package com.Azelmods.App.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OllamaApiService @Inject constructor(
    private val torDnsResolver: com.Azelmods.App.data.security.tor.TorDnsResolver,
    private val baseUrl: String = "http://127.0.0.1:11434"
) {

    companion object {
        private const val TAG = "OllamaApiService"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        // 🔒 DNS resolver con soporte Tor (evita DNS leaks cuando Tor está activo)
        .dns(torDnsResolver)
        .build()

    private val apiRoot = baseUrl.trimEnd('/')

    fun chat(
        model: String,
        messages: List<ChatMessage>,
        temperature: Double,
        stream: Boolean
    ): Flow<String> = flow {
        val messagesArray = JSONArray()
        messages.forEach { msg ->
            messagesArray.put(JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
            })
        }
        val body = JSONObject().apply {
            put("model", model)
            put("messages", messagesArray)
            put("stream", stream)
            put("options", JSONObject().apply { put("temperature", temperature) })
        }
        val request = Request.Builder()
            .url("$apiRoot/api/chat")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (!response.isSuccessful) {
            throw Exception("Ollama local error: ${response.code}")
        }
        val responseBody = response.body?.string() ?: throw Exception("Respuesta vacía de Ollama local")
        val json = JSONObject(responseBody)
        val content = json.optJSONObject("message")?.optString("content")
            ?: throw Exception("Respuesta inválida de Ollama local")
        emit(content)
    }.flowOn(Dispatchers.IO)

    fun generate(
        model: String,
        prompt: String,
        systemPrompt: String,
        temperature: Double,
        stream: Boolean
    ): Flow<String> = flow {
        val body = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
            put("system", systemPrompt)
            put("stream", stream)
            put("options", JSONObject().apply { put("temperature", temperature) })
        }
        val request = Request.Builder()
            .url("$apiRoot/api/generate")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        if (!response.isSuccessful) {
            throw Exception("Ollama generate error: ${response.code}")
        }
        val responseBody = response.body?.string() ?: throw Exception("Respuesta vacía de Ollama local")
        val content = JSONObject(responseBody).optString("response")
        if (content.isBlank()) throw Exception("Respuesta inválida de Ollama local")
        emit(content)
    }.flowOn(Dispatchers.IO)

    suspend fun listModels(): List<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$apiRoot/api/tags").get().build()
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()
            val json = JSONObject(response.body?.string() ?: return@withContext emptyList())
            val models = json.optJSONArray("models") ?: return@withContext emptyList()
            (0 until models.length()).mapNotNull { i ->
                models.optJSONObject(i)?.optString("name")?.takeIf { it.isNotBlank() }
            }
        } catch (e: Exception) {
            Log.e(TAG, "listModels failed", e)
            emptyList()
        }
    }

    suspend fun isServerAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url("$apiRoot/api/tags").get().build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }
}
