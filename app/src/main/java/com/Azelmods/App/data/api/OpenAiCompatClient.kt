package com.Azelmods.App.data.api

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/**
 * Cliente para cualquier endpoint que hable el dialecto de OpenAI
 * (`POST {baseUrl}/chat/completions`).
 *
 * Un solo cliente sirve para OpenAI, OpenRouter, DeepSeek, Mistral, Groq, Ollama,
 * LM Studio, vLLM y llama.cpp, porque todos implementan el mismo contrato. Lo único
 * que cambia entre ellos es la URL base, el nombre del modelo y si hace falta clave.
 */
object OpenAiCompatClient {

    private const val TAG = "OpenAiCompat"

    /** Texto de la respuesta y tokens consumidos, si el proveedor los informa. */
    data class Result(val content: String, val totalTokens: Int)

    /**
     * @param systemPrompt instrucción de sistema; se envía como primer mensaje con
     *   rol `system`, que es como lo espera este dialecto (Gemini usa otro campo).
     * @param messages pares (rol, contenido) ya recortados por el llamador. El rol
     *   debe ser `user` o `assistant`.
     * @param apiKey puede ir vacía en servidores locales; en ese caso no se manda
     *   la cabecera Authorization, porque algunos servidores rechazan un Bearer vacío.
     */
    fun chatCompletion(
        client: OkHttpClient,
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        temperature: Float,
        maxTokens: Int,
        topP: Float
    ): Result {
        require(baseUrl.isNotBlank()) { "La URL base del proveedor está vacía" }
        require(model.isNotBlank()) { "No hay modelo configurado para este proveedor" }

        val messagesArray = JSONArray().apply {
            if (systemPrompt.isNotBlank()) {
                put(JSONObject().put("role", "system").put("content", systemPrompt))
            }
            messages.forEach { (role, content) ->
                put(JSONObject().put("role", role).put("content", content))
            }
        }

        val body = JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            put("temperature", temperature.toDouble())
            put("max_tokens", maxTokens)
            put("top_p", topP.toDouble())
            put("stream", false)
        }.toString()

        val requestBuilder = Request.Builder()
            .url("$baseUrl/chat/completions")
            .post(body.toRequestBody("application/json".toMediaType()))

        // Servidores locales suelen no tener auth; un "Bearer " vacío hace que
        // algunos devuelvan 401 en vez de ignorarlo.
        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val raw = response.body?.string()

            if (!response.isSuccessful) {
                // El mensaje del proveedor es lo único que permite distinguir
                // "modelo inexistente" de "clave inválida" o "sin saldo".
                val detail = extractErrorMessage(raw)
                Log.e(TAG, "HTTP ${response.code} de $baseUrl — $detail")
                throw Exception("Error del proveedor (${response.code}): $detail")
            }

            if (raw.isNullOrBlank()) throw Exception("Respuesta vacía del proveedor")

            val json = JSONObject(raw)
            val choices = json.optJSONArray("choices")
            if (choices == null || choices.length() == 0) {
                throw Exception("El proveedor no devolvió ninguna respuesta")
            }

            val message = choices.getJSONObject(0).optJSONObject("message")
            val content = message?.optString("content").orEmpty()
            if (content.isBlank()) throw Exception("La respuesta del proveedor venía vacía")

            val totalTokens = json.optJSONObject("usage")?.optInt("total_tokens", 0) ?: 0
            return Result(content = content, totalTokens = totalTokens)
        }
    }

    /**
     * Extrae el texto de error del cuerpo. Los proveedores no coinciden en el formato:
     * unos usan `{"error":{"message":...}}`, otros `{"error":"..."}` y algunos texto
     * plano, así que se prueban en orden y se cae al cuerpo tal cual.
     */
    private fun extractErrorMessage(raw: String?): String {
        if (raw.isNullOrBlank()) return "sin detalles"
        return runCatching {
            val json = JSONObject(raw)
            json.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                ?: json.optString("error").takeIf { it.isNotBlank() }
                ?: json.optString("message").takeIf { it.isNotBlank() }
                ?: raw.take(200)
        }.getOrElse { raw.take(200) }
    }
}
