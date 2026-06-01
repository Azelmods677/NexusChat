package com.Azelmods.App.data.ai

import android.util.Log
import kotlinx.coroutines.delay
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔄 GEMINI REQUEST QUEUE
 * Cola de requests con reintentos automáticos y backoff exponencial.
 * Detecta errores 429/quota y reintenta con espera creciente: 5s → 15s → 30s
 */
@Singleton
class GeminiRequestQueue @Inject constructor(
    private val rateLimiter: GeminiRateLimiter
) {

    companion object {
        private const val TAG = "GeminiRequestQueue"
    }

    // Reintentos automáticos con backoff exponencial
    suspend fun <T> enqueue(
        maxRetries: Int = 3,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return rateLimiter.executeWithRateLimit { block() }
            } catch (e: Exception) {
                lastException = e
                val isRateLimit = e.message?.let { msg ->
                    msg.contains("429") ||
                    msg.contains("quota") ||
                    msg.contains("RESOURCE_EXHAUSTED") ||
                    msg.contains("rate") ||
                    msg.contains("limit")
                } == true

                if (isRateLimit && attempt < maxRetries - 1) {
                    // Backoff: 5s, 15s, 30s
                    val waitMs = when (attempt) {
                        0 -> 5_000L
                        1 -> 15_000L
                        else -> 30_000L
                    }
                    Log.w(TAG, "⚠️ Rate limit detectado (intento ${attempt + 1}/$maxRetries). Esperando ${waitMs}ms...")
                    delay(waitMs)
                } else if (!isRateLimit) {
                    throw e
                }
            }
        }
        throw lastException ?: Exception("Max retries exceeded")
    }
}
