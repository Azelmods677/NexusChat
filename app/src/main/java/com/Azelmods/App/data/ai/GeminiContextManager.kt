package com.Azelmods.App.data.ai

import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 📦 GEMINI CONTEXT MANAGER
 * Gestiona la ventana de contexto para optimizar el uso de tokens.
 * - Trim del historial a N mensajes (ventana deslizante)
 * - Estimación de tokens
 * - Verificación de seguridad antes de enviar
 *
 * NO modifica prompts existentes, solo limita el historial enviado.
 */
@Singleton
class GeminiContextManager @Inject constructor() {

    companion object {
        private const val TAG = "GeminiContextManager"
        private const val DEFAULT_MAX_MESSAGES = 16
        private const val SAFE_TOKEN_LIMIT = 900_000 // Margen seguro bajo 1M TPM
    }

    /**
     * Ventana deslizante: solo últimos N mensajes.
     * Mantiene los prompts intactos, solo limita el historial enviado.
     * Siempre conserva el primer mensaje (system/context) si existe.
     */
    fun <T> trimHistory(
        fullHistory: List<T>,
        maxMessages: Int = DEFAULT_MAX_MESSAGES
    ): List<T> {
        if (fullHistory.size <= maxMessages) return fullHistory

        // Siempre conservar el primer mensaje (system/context)
        val first = fullHistory.first()
        val recent = fullHistory.takeLast(maxMessages - 1)

        Log.d(TAG, "📦 Historial trimmed: ${fullHistory.size} → ${maxMessages} mensajes")
        return listOf(first) + recent
    }

    /**
     * Estima tokens de forma simple (4 chars ≈ 1 token)
     */
    fun estimateTokens(text: String): Int = (text.length / 4).coerceAtLeast(1)

    /**
     * Verifica si el historial es seguro para enviar (bajo el límite TPM)
     */
    fun isSafeToSend(history: List<Map<String, Any>>): Boolean {
        val totalChars = history.sumOf { msg ->
            msg["content"]?.toString()?.length ?: 0
        }
        val estimatedTokens = estimateTokens(totalChars.toString()) * history.size
        val safe = estimatedTokens < SAFE_TOKEN_LIMIT
        if (!safe) {
            Log.w(TAG, "⚠️ Historial demasiado grande: ~$estimatedTokens tokens estimados")
        }
        return safe
    }
}
