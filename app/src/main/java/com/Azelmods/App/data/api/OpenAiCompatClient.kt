package com.Azelmods.App.data.api

import android.util.Log
import okhttp3.Call
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
            val content = message?.stringOrEmpty("content").orEmpty()
            if (content.isBlank()) throw Exception("La respuesta del proveedor venía vacía")

            val totalTokens = json.optJSONObject("usage")?.optInt("total_tokens", 0) ?: 0
            return Result(content = content, totalTokens = totalTokens)
        }
    }

    /**
     * Igual que [chatCompletion] pero con `stream: true`: entrega la respuesta a
     * trozos en [onDelta] según la genera el modelo (Server-Sent Events, el mismo
     * formato en OpenAI, OpenRouter, DeepSeek, Groq, Mistral, Ollama…).
     *
     * Antes los proveedores OpenAI-compatibles se pedían sin streaming y la
     * respuesta aparecía de golpe tras esperar a que terminara entera: con
     * respuestas largas o modelos locales eran decenas de segundos con la pantalla
     * "pensando" sin dar señales de vida.
     *
     * Si el servidor ignora `stream` y contesta un JSON normal, se entrega entero
     * en un solo trozo. [onCall] recibe la llamada HTTP para poder cancelarla
     * (botón "Detener").
     *
     * @return tokens totales si el proveedor los informa; 0 si no.
     */
    fun chatCompletionStream(
        client: OkHttpClient,
        baseUrl: String,
        apiKey: String,
        model: String,
        systemPrompt: String,
        messages: List<Pair<String, String>>,
        temperature: Float,
        maxTokens: Int,
        topP: Float,
        onCall: (Call) -> Unit = {},
        onDelta: (String) -> Unit
    ): Int {
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
            put("stream", true)
        }.toString()

        val requestBuilder = Request.Builder()
            .url("$baseUrl/chat/completions")
            .addHeader("Accept", "text/event-stream")
            .post(body.toRequestBody("application/json".toMediaType()))
        if (apiKey.isNotBlank()) requestBuilder.addHeader("Authorization", "Bearer $apiKey")

        val call = client.newCall(requestBuilder.build())
        onCall(call)

        call.execute().use { response ->
            if (!response.isSuccessful) {
                val detail = extractErrorMessage(response.body?.string())
                Log.e(TAG, "HTTP ${response.code} de $baseUrl (stream) — $detail")
                throw Exception("Error del proveedor (${response.code}): $detail")
            }
            val responseBody = response.body ?: throw Exception("Respuesta vacía del proveedor")

            val contentType = response.header("Content-Type").orEmpty()
            if (!contentType.contains("event-stream", ignoreCase = true)) {
                // El servidor no hace streaming: respuesta completa en JSON.
                val raw = responseBody.string()
                val json = JSONObject(raw)
                val choices = json.optJSONArray("choices")
                val content = choices?.optJSONObject(0)?.optJSONObject("message")
                    ?.stringOrEmpty("content").orEmpty()
                if (content.isBlank()) throw Exception("La respuesta del proveedor venía vacía")
                onDelta(content)
                return json.optJSONObject("usage")?.optInt("total_tokens", 0) ?: 0
            }

            val source = responseBody.source()
            var totalTokens = 0
            var gotContent = false
            while (true) {
                val line = source.readUtf8Line() ?: break
                if (!line.startsWith("data:")) continue
                val data = line.removePrefix("data:").trim()
                if (data.isEmpty()) continue
                if (data == "[DONE]") break

                val json = runCatching { JSONObject(data) }.getOrNull() ?: continue
                json.optJSONObject("error")?.let { error ->
                    throw Exception("Error del proveedor: ${error.stringOrEmpty("message").ifBlank { "desconocido" }}")
                }
                val choice = json.optJSONArray("choices")?.optJSONObject(0)
                val text = choice?.optJSONObject("delta")?.stringOrEmpty("content").orEmpty()
                if (text.isNotEmpty()) {
                    gotContent = true
                    onDelta(text)
                }
                json.optJSONObject("usage")?.let { totalTokens = it.optInt("total_tokens", totalTokens) }
            }
            if (!gotContent) throw Exception("La respuesta del proveedor venía vacía")
            return totalTokens
        }
    }

    /**
     * Pide al proveedor su catálogo real de modelos (`GET {baseUrl}/models`).
     *
     * Es lo que permite que un modelo recién publicado (una versión nueva de DeepSeek,
     * GLM, Qwen o Kimi) aparezca en la app SIN actualizar la app: la lista no está
     * hardcodeada, la contesta el propio proveedor.
     *
     * Devuelve los ids ordenados. Lanza excepción con el motivo si falla, para que la
     * UI pueda explicar por qué (clave inválida, endpoint mal escrito, sin red…).
     */
    fun listModels(
        client: OkHttpClient,
        baseUrl: String,
        apiKey: String
    ): List<String> {
        require(baseUrl.isNotBlank()) { "La URL base del proveedor está vacía" }

        val requestBuilder = Request.Builder()
            .url("$baseUrl/models")
            .get()

        if (apiKey.isNotBlank()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        client.newCall(requestBuilder.build()).execute().use { response ->
            val raw = response.body?.string()

            if (!response.isSuccessful) {
                val detail = extractErrorMessage(raw)
                Log.e(TAG, "HTTP ${response.code} al listar modelos de $baseUrl — $detail")
                throw Exception("No se pudo obtener la lista (${response.code}): $detail")
            }
            if (raw.isNullOrBlank()) throw Exception("El proveedor devolvió una lista vacía")

            val json = JSONObject(raw)
            // Formato OpenAI: {"data":[{"id":"..."}]}. Algunos servidores devuelven
            // directamente un array, así que se contemplan ambos.
            val data: JSONArray = json.optJSONArray("data")
                ?: runCatching { JSONArray(raw) }.getOrNull()
                ?: throw Exception("Formato de respuesta no reconocido")

            val ids = (0 until data.length()).mapNotNull { i ->
                when (val entry = data.opt(i)) {
                    is JSONObject -> entry.optString("id").takeIf { it.isNotBlank() }
                    is String -> entry.takeIf { it.isNotBlank() }
                    else -> null
                }
            }

            if (ids.isEmpty()) throw Exception("El proveedor no devolvió ningún modelo")
            return ids.distinct().sorted()
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

    /**
     * `optString` de Android devuelve el texto "null" cuando el valor es JSON
     * null, y varios proveedores mandan `"content": null` en el primer trozo del
     * stream o en modelos de razonamiento. Sin esto, "null" acababa pegado al
     * principio de la respuesta.
     */
    private fun JSONObject.stringOrEmpty(key: String): String =
        if (isNull(key)) "" else optString(key, "")
}
