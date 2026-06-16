package com.Azelmods.App.data.api
import android.util.Log
import com.Azelmods.App.BuildConfig
import com.Azelmods.App.data.ai.AiKeyStore
import com.Azelmods.App.data.ai.GeminiRequestQueue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import javax.inject.Inject
import javax.inject.Singleton
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

/**
 * 🚀 AZEL AI API SERVICE - GEMINI INTEGRATION
 * Servicio para la API de Gemini con streaming real, cola con backoff y rate limiting.
 * La API key se resuelve en tiempo de ejecución: primero la del usuario (AiKeyStore),
 * con fallback a BuildConfig.GEMINI_API_KEY (leída desde local.properties).
 */
@Singleton
class AzelAIApiService @Inject constructor(
    private val torDnsResolver: com.Azelmods.App.data.security.tor.TorDnsResolver,
    private val requestQueue: GeminiRequestQueue,
    private val keyStore: AiKeyStore
) {
    
    companion object {
        private const val TAG = "AzelAIApiService"

        /** Marcador interno usado cuando no hay API key configurada (mapeado a mensaje legible). */
        const val API_KEY_MISSING = "API_KEY_MISSING"

        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        
        // Modelos disponibles en Gemini
        const val GEMINI_3_1_PRO_PREVIEW = "gemini-3.1-pro-preview"
        const val GEMINI_2_5_FLASH = "gemini-2.5-flash"
        const val GEMINI_2_0_FLASH = "gemini-2.0-flash"
        const val GEMINI_1_5_PRO = "gemini-1.5-pro"
        const val GEMINI_1_5_FLASH = "gemini-1.5-flash"
        
        // Mantener nombres antiguos mapeados a Gemini para compatibilidad
        const val DEEPSEEK_R1_70B = GEMINI_2_5_FLASH
        const val DEEPSEEK_R1_32B = GEMINI_2_5_FLASH
        const val DEEPSEEK_R1_14B = GEMINI_2_0_FLASH
        const val DEEPSEEK_R1_7B = GEMINI_1_5_FLASH
        const val LLAMA_3_3_70B = GEMINI_1_5_PRO
        const val LLAMA_3_2_90B = GEMINI_1_5_PRO
        const val LLAMA_3_2_3B = GEMINI_1_5_FLASH
        const val LLAMA_3_1_8B = GEMINI_1_5_FLASH
        const val QWEN_2_5_72B = GEMINI_2_0_FLASH
        const val MISTRAL_LARGE_3 = GEMINI_2_0_FLASH
        const val CODELLAMA_70B = GEMINI_1_5_PRO
        const val DOLPHIN_MIXTRAL = GEMINI_2_0_FLASH
        const val GPT_OSS_120B_CLOUD = GEMINI_3_1_PRO_PREVIEW
        
        // Configuración por defecto
        private const val DEFAULT_MODEL = GEMINI_2_5_FLASH
        private const val MAX_TOKENS = 1024
        private const val TEMPERATURE = 0.7f
        private const val TOP_P = 0.95f
        private const val MAX_CONTEXT_MESSAGES = 8
    }
    
    init {
        Log.d(TAG, "Gemini API configurado: $BASE_URL")
    }

    /**
     * 🔑 Resuelve la API key activa.
     *  1. Clave introducida por el usuario y guardada de forma segura (AiKeyStore).
     *  2. Fallback a BuildConfig.GEMINI_API_KEY (desde local.properties).
     * Devuelve cadena vacía si no hay ninguna configurada.
     */
    private fun resolveApiKey(): String {
        val userKey = keyStore.getApiKey()?.trim().orEmpty()
        if (userKey.isNotEmpty()) return userKey
        return BuildConfig.GEMINI_API_KEY.trim()
    }

    /** `true` si hay una API key disponible (de usuario o de BuildConfig). */
    fun hasApiKey(): Boolean = resolveApiKey().isNotEmpty()

    /** Devuelve la API key activa o lanza una excepción reconocible si falta. */
    private fun requireApiKey(): String {
        val key = resolveApiKey()
        if (key.isEmpty()) throw IllegalStateException(API_KEY_MISSING)
        return key
    }
    
    private val client by lazy {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .retryOnConnectionFailure(true)
            .dns(torDnsResolver)
        
        Log.d(TAG, "📡 Using connection for Gemini API")
        
        try {
            val trustManagerFactory = javax.net.ssl.TrustManagerFactory.getInstance(
                javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
            ).apply { init(null as java.security.KeyStore?) }
            val sslContext = javax.net.ssl.SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagerFactory.trustManagers, java.security.SecureRandom())
            builder.sslSocketFactory(sslContext.socketFactory, trustManagerFactory.trustManagers[0] as javax.net.ssl.X509TrustManager)
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Failed to configure custom SSL socket factory", e)
        }
        
        builder.build()
    }
    
    /**
     * 🧭 SYSTEM PROMPT NEUTRAL
     * Asistente general capaz y útil. Responde con normalidad a usos legítimos,
     * en el idioma del usuario (español o inglés). Sin técnicas de evasión.
     */
    private fun getSystemPrompt(): String = """
You are Azel IA, a capable and helpful general-purpose assistant integrated into a messaging app.

Guidelines:
- Be accurate, clear, and concise. Provide complete, practical answers.
- Reply in the user's language (Spanish or English), matching their tone.
- Help with everyday tasks: writing and improving text, explaining concepts, summarizing,
  translating, answering questions, and assisting with programming and software topics.
- For code, prefer correct, readable, well-structured examples and explain the reasoning.
- For security questions, focus on defensive best practices, secure coding, and how to
  protect systems and data.
- When you are unsure, say so honestly instead of inventing facts.
- Format answers with Markdown when it improves readability (lists, code blocks, headings).
    """.trimIndent()
    
    /**
     * 🚀 CHAT COMPLETION CON STREAMING REAL (GEMINI FORMAT)
     */
    fun chatCompletionStream(
        model: String = DEFAULT_MODEL,
        messages: List<Message>,
        temperature: Float = TEMPERATURE,
        maxTokens: Int = MAX_TOKENS,
        topP: Float = TOP_P,
        frequencyPenalty: Float = 0f, // No aplica directo en Gemini general config
        presencePenalty: Float = 0f   // No aplica directo en Gemini general config
    ): Flow<StreamResponse> = callbackFlow {
        val channel: SendChannel<StreamResponse> = this
        val activeSource = java.util.concurrent.atomic.AtomicReference<EventSource?>(null)

        try {
            // 🛡️ Encaminar TODA la solicitud de streaming por la cola:
            //   - GeminiRateLimiter aplica el espaciado mínimo entre requests
            //   - GeminiRequestQueue reintenta con backoff exponencial ante RateLimit (429/quota)
            // Cada reintento abre un nuevo EventSource a Gemini dentro del bloque encolado.
            requestQueue.enqueue {
                streamGenerateContentOnce(
                    model = model,
                    messages = messages,
                    temperature = temperature,
                    maxTokens = maxTokens,
                    topP = topP,
                    channel = channel,
                    sourceRef = activeSource
                )
            }
            // El intento terminó correctamente (Done ya fue emitido por streamGenerateContentOnce).
            runCatching { close() }
        } catch (e: Exception) {
            // Reintentos agotados o error no recuperable: propagar como Error para que
            // el ViewModel lo mapee a un mensaje legible/recuperable.
            Log.e(TAG, "❌ Streaming failed (type=${e.javaClass.simpleName}): ${e.message}", e)
            runCatching { channel.trySend(StreamResponse.Error("Error: ${e.message}")) }
            runCatching { close() }
        }

        awaitClose {
            Log.d(TAG, "🛑 Closing event source")
            runCatching { activeSource.getAndSet(null)?.cancel() }
        }
    }.flowOn(Dispatchers.IO)

    /**
     * 🔁 UN INTENTO DE STREAMING (llamado por requestQueue.enqueue)
     *
     * Suspende hasta que el stream termina (Done) o falla. Si Gemini responde con un
     * RateLimit (429 / quota / RESOURCE_EXHAUSTED) antes de emitir contenido, lanza una
     * excepción reconocible por GeminiRequestQueue para que ésta reintente con backoff.
     * Si ya se recibió contenido, no se reintenta y el error se propaga al flujo.
     */
    private suspend fun streamGenerateContentOnce(
        model: String,
        messages: List<Message>,
        temperature: Float,
        maxTokens: Int,
        topP: Float,
        channel: SendChannel<StreamResponse>,
        sourceRef: java.util.concurrent.atomic.AtomicReference<EventSource?>
    ): Unit = suspendCancellableCoroutine { cont ->
        val finished = AtomicBoolean(false)
        val receivedContent = AtomicBoolean(false)

        Log.d(TAG, "🚀 Starting streaming chat completion with Gemini model: $model")

        val requestBodyString = buildRequestBody(
            messages = messages,
            temperature = temperature,
            maxTokens = maxTokens,
            topP = topP
        ).toString()

        val url = "$BASE_URL/$model:streamGenerateContent?alt=sse&key=${requireApiKey()}"

        val request = Request.Builder()
            .url(url)
            .post(requestBodyString.toRequestBody("application/json".toMediaType()))
            .addHeader("Accept", "text/event-stream")
            .build()

        val eventSource = EventSources.createFactory(client).newEventSource(
            request = request,
            listener = object : EventSourceListener() {
                override fun onOpen(eventSource: EventSource, response: okhttp3.Response) {
                    Log.d(TAG, "✅ Stream opened successfully")
                }

                override fun onEvent(
                    eventSource: EventSource,
                    id: String?,
                    type: String?,
                    data: String
                ) {
                    if (finished.get()) return

                    try {
                        if (data.isBlank()) return

                        val json = try {
                            JSONObject(data)
                        } catch (e: org.json.JSONException) {
                            Log.e(TAG, "❌ Invalid JSON received: ${data.take(100)}", e)
                            return
                        }

                        val candidates = json.optJSONArray("candidates")
                        if (candidates != null && candidates.length() > 0) {
                            val candidate = candidates.getJSONObject(0)
                            val content = candidate.optJSONObject("content")
                            val parts = content?.optJSONArray("parts")

                            if (parts != null && parts.length() > 0) {
                                val text = parts.getJSONObject(0).optString("text")
                                if (text.isNotEmpty()) {
                                    receivedContent.set(true)
                                    channel.trySend(StreamResponse.Content(text))
                                }
                            }

                            val finishReason = candidate.optString("finishReason", "")
                            if (finishReason.isNotEmpty() && finishReason != "STOP" &&
                                finished.compareAndSet(false, true)
                            ) {
                                Log.d(TAG, "✅ Stream finished with reason: $finishReason")
                                channel.trySend(StreamResponse.Done)
                                if (cont.isActive) cont.resume(Unit)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "❌ Error parsing stream event", e)
                    }
                }

                override fun onFailure(
                    eventSource: EventSource,
                    t: Throwable?,
                    response: okhttp3.Response?
                ) {
                    if (!finished.compareAndSet(false, true)) return

                    val code = response?.code
                    val detail = t?.message ?: ""
                    val rateLimited = code == 429 ||
                        detail.contains("429") ||
                        detail.contains("quota", ignoreCase = true) ||
                        detail.contains("RESOURCE_EXHAUSTED", ignoreCase = true)

                    Log.e(TAG, "❌ Stream failed (code=$code, rateLimited=$rateLimited): $detail", t)

                    // Sólo dejamos que la cola reintente si aún no emitimos contenido;
                    // reintentar a mitad de un stream produciría respuestas duplicadas.
                    if (rateLimited && !receivedContent.get()) {
                        if (cont.isActive) {
                            cont.resumeWithException(
                                Exception("429 RESOURCE_EXHAUSTED: rate limit en streaming (code=$code)")
                            )
                        }
                    } else {
                        val errorMsg = "❌ Error de conexión con Gemini: ${detail.ifBlank { code?.toString() ?: "desconocido" }}"
                        channel.trySend(StreamResponse.Error(errorMsg))
                        if (cont.isActive) cont.resume(Unit)
                    }
                }

                override fun onClosed(eventSource: EventSource) {
                    Log.d(TAG, "🔒 Stream closed")
                    if (finished.compareAndSet(false, true)) {
                        channel.trySend(StreamResponse.Done)
                        if (cont.isActive) cont.resume(Unit)
                    }
                }
            }
        )

        sourceRef.set(eventSource)

        cont.invokeOnCancellation {
            finished.set(true)
            runCatching { eventSource.cancel() }
        }
    }
    
    /**
     * 💬 CHAT COMPLETION SIN STREAMING (FALLBACK)
     */
    suspend fun chatCompletion(
        model: String = DEFAULT_MODEL,
        messages: List<Message>,
        temperature: Float = TEMPERATURE,
        maxTokens: Int = MAX_TOKENS,
        topP: Float = TOP_P,
        frequencyPenalty: Float = 0f,
        presencePenalty: Float = 0f
    ): ChatResponse = withContext(Dispatchers.IO) {
        // 🛡️ Wrapping con rate limiter + retry automático
        requestQueue.enqueue {
            chatCompletionInternal(model, messages, temperature, maxTokens, topP)
        }
    }
    
    /**
     * 💬 IMPLEMENTACIÓN INTERNA DE CHAT COMPLETION (llamada por requestQueue)
     */
    private suspend fun chatCompletionInternal(
        model: String,
        messages: List<Message>,
        temperature: Float,
        maxTokens: Int,
        topP: Float
    ): ChatResponse {
        try {
            Log.d(TAG, "🚀 Starting non-streaming chat completion with Gemini: $model")
            
            val requestBodyString = buildRequestBody(
                messages = messages,
                temperature = temperature,
                maxTokens = maxTokens,
                topP = topP
            ).toString()
            
            val url = "$BASE_URL/$model:generateContent?key=${requireApiKey()}"
            
            val request = Request.Builder()
                .url(url)
                .post(requestBodyString.toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: "Sin detalles"
                Log.e(TAG, "❌ API Error: ${response.code} - Body: $errorBody")
                throw Exception("Error de Gemini API (${response.code})")
            }
            
            val responseBody = response.body?.string()
            if (responseBody.isNullOrBlank()) {
                throw Exception("Respuesta vacía del servidor")
            }
            
            val jsonResponse = JSONObject(responseBody)
            val candidates = jsonResponse.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                throw Exception("Sin respuesta de Gemini")
            }
            
            val firstChoice = candidates.getJSONObject(0)
            val contentObj = firstChoice.getJSONObject("content")
            val parts = contentObj.getJSONArray("parts")
            val content = parts.getJSONObject(0).getString("text")
            
            val usage = jsonResponse.optJSONObject("usageMetadata")
            val totalTokens = usage?.optInt("totalTokenCount", 0) ?: 0
            
            Log.d(TAG, "✅ Response received, tokens: $totalTokens")
            
            return ChatResponse(
                content = content,
                tokens = totalTokens,
                model = model
            )
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error in chat completion", e)
            throw e
        }
    }  // end chatCompletionInternal
    
    /**
     * 🔧 CONSTRUIR REQUEST BODY PARA GEMINI
     */
    private fun buildRequestBody(
        messages: List<Message>,
        temperature: Float,
        maxTokens: Int,
        topP: Float
    ): JSONObject {
        val requestBody = JSONObject()
        
        // System instruction (neutral assistant)
        val systemInstruction = JSONObject().apply {
            put("role", "system")
            put("parts", JSONArray().put(JSONObject().apply { put("text", getSystemPrompt()) }))
        }
        requestBody.put("systemInstruction", systemInstruction)
        
        // Contents
        val contentsArray = JSONArray()
        messages.takeLast(MAX_CONTEXT_MESSAGES).forEach { msg ->
            // Gemini uses "user" and "model" roles
            val geminiRole = if (msg.role == "assistant") "model" else if (msg.role == "system") "user" else msg.role
            contentsArray.put(JSONObject().apply {
                put("role", geminiRole)
                put("parts", JSONArray().put(JSONObject().apply { put("text", msg.content) }))
            })
        }
        requestBody.put("contents", contentsArray)
        
        // Nota: No se envían safetySettings personalizados. Se respetan los filtros de
        // seguridad por defecto del proveedor (Gemini). No se intenta desactivarlos.
        
        // Generation Config
        val generationConfig = JSONObject().apply {
            put("temperature", temperature.toDouble())
            put("topP", topP.toDouble())
            put("maxOutputTokens", maxTokens)
        }
        requestBody.put("generationConfig", generationConfig)
        
        return requestBody
    }
    
    /**
     * 📊 OBTENER MODELOS DISPONIBLES EN GEMINI
     */
    fun getAvailableModels(): List<AIModel> = listOf(
        AIModel(GEMINI_3_1_PRO_PREVIEW, description = "✨ Gemini 3.1 Pro (Preview) - Inteligencia Suprema", parameters = 0),
        AIModel(GEMINI_2_5_FLASH, description = "⚡ Gemini 2.5 Flash - Muy Rápido y Eficiente (Recomendado)", parameters = 0),
        AIModel(GEMINI_2_0_FLASH, description = "⚡ Gemini 2.0 Flash - Rápido y capaz", parameters = 0),
        AIModel(GEMINI_1_5_PRO, description = "🚀 Modelo clásico más avanzado", parameters = 0),
        AIModel(GEMINI_1_5_FLASH, description = "💡 Modelo eficiente para tareas rápidas", parameters = 0)
    )
    
    /**
     * 🔍 VERIFICAR SALUD DE LA API GEMINI
     */
    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val key = resolveApiKey()
            if (key.isEmpty()) {
                Log.w(TAG, "Health check omitido: no hay API key configurada")
                return@withContext false
            }
            val url = "$BASE_URL/gemini-1.5-flash?key=$key"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Health check failed", e)
            false
        }
    }
}

/**
 * 📦 DATA CLASSES
 */
data class Message(
    val role: String, // "user", "assistant" (mapped internally to "model")
    val content: String
)

data class ChatResponse(
    val content: String,
    val tokens: Int,
    val model: String
)

sealed class StreamResponse {
    data class Content(val text: String) : StreamResponse()
    data class Error(val message: String) : StreamResponse()
    object Done : StreamResponse()
}

data class AIModel(
    val id: String,
    val name: String = "Azel IA", 
    val description: String,
    val parameters: Int
)
