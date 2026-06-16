package com.Azelmods.App.ui.screens.azelai

/**
 * 🧭 AZEL AI ERROR MAPPER
 *
 * Mapeo centralizado de excepciones/errores de la IA → mensaje legible en español.
 *
 * Garantías (Property 5 de la spec fix-app-crashes):
 *  - SIEMPRE devuelve un mensaje no vacío.
 *  - NUNCA expone el stack trace técnico al usuario.
 *  - El RateLimit (429 / quota / RESOURCE_EXHAUSTED) se presenta como una condición
 *    temporal recuperable, no como un fallo de inicialización.
 *
 * Es una función pura (sin dependencias de Android) para poder testearla con
 * property-based testing.
 */
object AzelAIErrorMapper {

    /** Mensaje cuando no hay API key de Gemini configurada. */
    const val API_KEY_MISSING_MESSAGE =
        "Configura tu API key de Gemini en Ajustes › IA"

    /** Mensaje recuperable cuando se agotan los reintentos ante RateLimit. */
    const val RATE_LIMIT_MESSAGE =
        "El servicio de IA está temporalmente saturado. Reintenta en unos minutos."

    /** Mensaje por defecto: nunca permite un resultado vacío. */
    const val GENERIC_MESSAGE =
        "Ocurrió un error al contactar la IA. Reintenta en unos momentos."

    /**
     * ¿El mensaje corresponde a un RateLimit de Gemini?
     * Coincide con la detección de [com.Azelmods.App.data.ai.GeminiRequestQueue].
     */
    fun isRateLimit(message: String?): Boolean {
        val m = message ?: return false
        return m.contains("429") ||
            m.contains("quota", ignoreCase = true) ||
            m.contains("RESOURCE_EXHAUSTED", ignoreCase = true) ||
            m.contains("rate limit", ignoreCase = true) ||
            m.contains("límite de solicitudes", ignoreCase = true)
    }

    /**
     * Mapea una excepción a un mensaje en español comprensible para usuarios no técnicos.
     * Siempre devuelve un texto no vacío.
     */
    fun map(throwable: Throwable?): String {
        val message = throwable?.message?.trim().orEmpty()

        val mapped = when {
            throwable is kotlinx.coroutines.TimeoutCancellationException ->
                "La IA tardó demasiado en responder. Reintenta en unos momentos."

            // Falta la API key del usuario (marcador interno del servicio).
            message.contains("API_KEY_MISSING") -> API_KEY_MISSING_MESSAGE

            message.contains("no autenticado", ignoreCase = true) ->
                "Debes iniciar sesión para usar la IA."

            message.contains("SAFETY", ignoreCase = true) ||
                message.contains("blocked", ignoreCase = true) ||
                message.contains("filtered", ignoreCase = true) ->
                "El filtro de seguridad bloqueó la respuesta. Reformula tu pregunta e inténtalo de nuevo."

            // RateLimit: condición temporal recuperable (tras agotar reintentos de la cola).
            isRateLimit(message) -> RATE_LIMIT_MESSAGE

            throwable is java.net.UnknownHostException ->
                "Sin conexión a internet. Verifica tu red e inténtalo de nuevo."

            throwable is java.net.SocketTimeoutException ->
                "Tiempo de espera agotado. Reintenta en unos momentos."

            throwable is java.net.ConnectException ->
                "No se pudo conectar al servidor. Reintenta en unos momentos."

            message.contains("401") -> "La clave de acceso a la IA no es válida o expiró."

            message.contains("403") -> "Acceso denegado por el servicio de IA."

            message.contains("500") || message.contains("502") || message.contains("503") ->
                "El servicio de IA no está disponible temporalmente. Reintenta en unos minutos."

            else -> GENERIC_MESSAGE
        }

        // Salvaguarda final de la Property 5: jamás devolver un mensaje vacío.
        return mapped.ifBlank { GENERIC_MESSAGE }
    }

    /**
     * Etiqueta corta del tipo de error para logging con contexto (Requisito 9.5),
     * sin exponer el stack trace al usuario.
     */
    fun errorContext(throwable: Throwable?): String {
        if (throwable == null) return "type=null"
        val type = throwable.javaClass.simpleName
        val code = Regex("\\b(4\\d\\d|5\\d\\d)\\b").find(throwable.message ?: "")?.value
        val rate = isRateLimit(throwable.message)
        return buildString {
            append("type=").append(type)
            if (code != null) append(", code=").append(code)
            append(", rateLimited=").append(rate)
        }
    }
}
