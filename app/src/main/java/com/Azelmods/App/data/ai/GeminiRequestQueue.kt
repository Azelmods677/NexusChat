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
        /**
         * Espaciado de 4,5 s entre peticiones: pensado para la capa gratuita de
         * Gemini (15 peticiones/minuto). Otros proveedores y los modelos locales
         * no lo necesitan; aplicárselo hacía que un equipo de agentes (≈10
         * consultas) esperase ~45 s de más sin motivo.
         */
        rateLimit: Boolean = true,
        block: suspend () -> T
    ): T {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return if (rateLimit) rateLimiter.executeWithRateLimit { block() } else block()
            } catch (e: kotlinx.coroutines.CancellationException) {
                // El usuario pulsó "Detener" o cerró la pantalla: nada que reintentar.
                throw e
            } catch (e: Exception) {
                lastException = e
                // Solo cuenta como límite de uso lo que de verdad lo es. Antes bastaba
                // con que el mensaje contuviera "rate" o "limit": un error de
                // "generate…" o de "max_tokens limit" se reintentaba 3 veces con
                // 20 s de espera antes de mostrar un fallo que era definitivo.
                val isRateLimit = isRateLimitError(e.message)

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

    private fun isRateLimitError(message: String?): Boolean {
        val msg = message?.lowercase() ?: return false
        return "429" in msg ||
            "quota" in msg ||
            "resource_exhausted" in msg ||
            "rate limit" in msg ||
            "rate_limit" in msg ||
            "too many requests" in msg
    }
}
