package com.Azelmods.App.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🌐 OpenCode API Service — Cloud AI Provider
 * Uses OPENCODE_API_KEY from BuildConfig (local.properties)
 */
@Singleton
class OpenCodeApiService @Inject constructor(
    private val torDnsResolver: com.Azelmods.App.data.security.tor.TorDnsResolver
) {

    companion object {
        private const val TAG = "OpenCodeApiService"

        private val API_KEY = com.Azelmods.App.BuildConfig.OPENCODE_API_KEY.trim()
        private const val BASE_URL = "https://api.opencode.ai/v1"

        // Available models
        const val GPT_4_TURBO = "gpt-4-turbo"
        const val GPT_4O = "gpt-4o"
        const val GPT_4O_MINI = "gpt-4o-mini"
        const val CLAUDE_3_5_SONNET = "claude-3-5-sonnet-20241022"
        const val CLAUDE_3_HAIKU = "claude-3-haiku-20240307"

        private const val DEFAULT_MODEL = GPT_4_TURBO
        private const val MAX_TOKENS = 8192
        private const val TEMPERATURE = 0.9f
        private const val TOP_P = 0.95f
    }

    init {
        if (API_KEY.isBlank() || API_KEY == "YOUR_API_KEY") {
            Log.w(TAG, "OPENCODE_API_KEY no configurada en local.properties. OpenCode no estará disponible.")
        } else {
            Log.d(TAG, "OpenCode Cloud configurado (key presente)")
        }
    }

    /** Returns true if a real API key is set. */
    fun isConfigured(): Boolean =
        API_KEY.isNotBlank() && API_KEY != "YOUR_API_KEY"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("Authorization", "Bearer $API_KEY")
                .header("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }
        .retryOnConnectionFailure(true)
        // 🔒 DNS resolver con soporte Tor (evita DNS leaks cuando Tor está activo)
        .dns(torDnsResolver)
        .build()

    /**
     * 🚀 Streaming chat completion via SSE
     */
    fun chatCompletionStream(
        model: String = DEFAULT_MODEL,
        messages: List<Message>,
        temperature: Float = TEMPERATURE,
        maxTokens: Int = MAX_TOKENS
    ): Flow<StreamResponse> = callbackFlow {
        val closed = AtomicBoolean(false)

        try {
            val messagesArray = JSONArray()
            messages.forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("temperature", temperature.toDouble())
                put("max_tokens", maxTokens)
                put("top_p", TOP_P.toDouble())
                put("stream", true)
            }

            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .addHeader("Accept", "text/event-stream")
                .build()

            Log.d(TAG, "🚀 Starting OpenCode stream: $model")

            val eventSource = EventSources.createFactory(client).newEventSource(
                request = request,
                listener = object : EventSourceListener() {
                    override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                        Log.d(TAG, "✅ OpenCode stream opened")
                    }

                    override fun onEvent(
                        eventSource: EventSource,
                        id: String?,
                        type: String?,
                        data: String
                    ) {
                        if (closed.get()) return
                        try {
                            if (data.trim() == "[DONE]") {
                                if (closed.compareAndSet(false, true)) {
                                    trySend(StreamResponse.Done)
                                    runCatching { close() }
                                }
                                return
                            }
                            if (data.isBlank()) return

                            val json = JSONObject(data)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val delta = choices.getJSONObject(0).optJSONObject("delta")
                                val content = delta?.optString("content")
                                if (!content.isNullOrEmpty() && !closed.get()) {
                                    trySend(StreamResponse.Content(content))
                                }
                                val finishReason = choices.getJSONObject(0).optString("finish_reason")
                                if ((finishReason == "stop" || finishReason == "length") &&
                                    closed.compareAndSet(false, true)
                                ) {
                                    trySend(StreamResponse.Done)
                                    runCatching { close() }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "❌ Error parsing OpenCode event", e)
                            if (closed.compareAndSet(false, true)) {
                                trySend(StreamResponse.Error("Error parsing: ${e.message}"))
                                runCatching { close() }
                            }
                        }
                    }

                    override fun onFailure(
                        eventSource: EventSource,
                        t: Throwable?,
                        response: okhttp3.Response?
                    ) {
                        if (closed.get()) return
                        val errorMsg = when (response?.code) {
                            401 -> "🔑 OpenCode API Key inválida"
                            403 -> "🚫 Acceso denegado a OpenCode API"
                            429 -> "⏱️ Límite de OpenCode excedido"
                            500, 502, 503 -> "🔧 OpenCode servidor no disponible"
                            else -> "❌ OpenCode error: ${t?.message ?: response?.code ?: "desconocido"}"
                        }
                        Log.e(TAG, "❌ OpenCode stream failed: $errorMsg", t)
                        if (closed.compareAndSet(false, true)) {
                            trySend(StreamResponse.Error(errorMsg))
                            runCatching { close() }
                        }
                    }

                    override fun onClosed(eventSource: EventSource) {
                        if (closed.compareAndSet(false, true)) {
                            runCatching { close() }
                        }
                    }
                }
            )

            awaitClose {
                closed.set(true)
                eventSource.cancel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ OpenCode stream setup error", e)
            if (closed.compareAndSet(false, true)) {
                trySend(StreamResponse.Error("Error: ${e.message?.take(200) ?: "desconocido"}"))
                runCatching { close() }
            }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 💬 Non-streaming chat completion (fallback)
     */
    suspend fun chatCompletion(
        model: String = DEFAULT_MODEL,
        messages: List<Message>,
        temperature: Float = TEMPERATURE,
        maxTokens: Int = MAX_TOKENS
    ): ChatResponse = withContext(Dispatchers.IO) {
        try {
            val messagesArray = JSONArray()
            messages.forEach { msg ->
                messagesArray.put(JSONObject().apply {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }

            val requestBody = JSONObject().apply {
                put("model", model)
                put("messages", messagesArray)
                put("temperature", temperature.toDouble())
                put("max_tokens", maxTokens)
                put("top_p", TOP_P.toDouble())
                put("stream", false)
            }

            val request = Request.Builder()
                .url("$BASE_URL/chat/completions")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                val msg = when (response.code) {
                    401 -> "🔑 API Key inválida"
                    403 -> "🚫 Acceso denegado"
                    429 -> "⏱️ Límite excedido"
                    500, 502, 503 -> "🔧 Servidor no disponible"
                    else -> "Error ${response.code}: ${errorBody.take(200)}"
                }
                throw Exception(msg)
            }

            val body = response.body?.string() ?: throw Exception("Respuesta vacía")
            val json = JSONObject(body)
            val choices = json.getJSONArray("choices")
            if (choices.length() == 0) throw Exception("Sin respuestas")
            val content = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
            val usage = json.optJSONObject("usage")
            val totalTokens = usage?.optInt("total_tokens", 0) ?: 0

            ChatResponse(
                content = content,
                tokens = totalTokens,
                model = model
            )
        } catch (e: Exception) {
            Log.e(TAG, "❌ OpenCode chat error", e)
            throw e
        }
    }

    /** Check if the API is reachable. */
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured()) return@withContext false
        try {
            val request = Request.Builder()
                .url("$BASE_URL/models")
                .get()
                .build()
            client.newCall(request).execute().isSuccessful
        } catch (e: Exception) {
            false
        }
    }

    /** Get available model list. */
    fun getAvailableModels(): List<AIModel> = listOf(
        AIModel(GPT_4_TURBO, "OpenCode GPT-4 Turbo", "Rápido y potente", 0),
        AIModel(GPT_4O, "OpenCode GPT-4o", "Multimodal avanzado", 0),
        AIModel(GPT_4O_MINI, "OpenCode GPT-4o Mini", "Económico y rápido", 0),
        AIModel(CLAUDE_3_5_SONNET, "Claude 3.5 Sonnet", "Excelente para código", 0),
        AIModel(CLAUDE_3_HAIKU, "Claude 3 Haiku", "Ultra rápido", 0)
    )
}
