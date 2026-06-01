package com.Azelmods.App.data.ai

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentLinkedQueue
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🛡️ GEMINI RATE LIMITER
 * Controla la velocidad de requests a la API de Gemini Free Tier.
 * Free tier: 15 RPM → mínimo 4.5s entre requests para margen seguro.
 */
@Singleton
class GeminiRateLimiter @Inject constructor() {

    companion object {
        private const val TAG = "GeminiRateLimiter"
    }

    private val mutex = Mutex()
    private val requestTimestamps = ConcurrentLinkedQueue<Long>()

    // Free tier: 15 RPM → 1 request cada 4 segundos como máximo seguro
    private val minDelayMs = 4_500L
    private var lastRequestTime = 0L

    suspend fun <T> executeWithRateLimit(block: suspend () -> T): T {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val elapsed = now - lastRequestTime

            if (lastRequestTime > 0 && elapsed < minDelayMs) {
                val waitTime = minDelayMs - elapsed
                Log.d(TAG, "⏳ Rate limit: esperando ${waitTime}ms antes del próximo request")
                delay(waitTime)
            }

            lastRequestTime = System.currentTimeMillis()
        }
        return block()
    }
}
