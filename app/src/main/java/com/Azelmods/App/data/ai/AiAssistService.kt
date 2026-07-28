package com.Azelmods.App.data.ai

import com.Azelmods.App.data.api.AzelAIApiService
import com.Azelmods.App.data.api.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

/** Tono al que reescribir un borrador. */
enum class MessageTone(val label: String, val instruction: String) {
    FRIENDLY("Cercano", "cálido y cercano, como se le escribe a un amigo"),
    FORMAL("Formal", "formal y profesional, sin coloquialismos"),
    SHORT("Más corto", "lo más breve posible sin perder el sentido"),
    KIND("Más amable", "amable y conciliador, suavizando cualquier aspereza")
}

/**
 * Tareas puntuales de IA sobre una conversación: respuestas sugeridas, resumen,
 * cambio de tono y traducción.
 *
 * Se apoya en [AzelAIApiService], que ya resuelve el proveedor configurado por el
 * usuario (Gemini o cualquiera compatible con OpenAI) y aplica el limitador de
 * peticiones. Aquí sólo viven los *prompts* y el post-procesado, que es lo que
 * distingue una sugerencia útil de un párrafo de relleno.
 *
 * ## Por qué cada función devuelve `Result`
 *
 * Todas estas funciones son opcionales para el usuario: si no hay clave, si el
 * proveedor falla o si devuelve algo inesperado, la conversación tiene que
 * seguir funcionando igual. Ninguna lanza.
 */
@Singleton
class AiAssistService @Inject constructor(
    private val api: AzelAIApiService,
    private val keyStore: AiKeyStore
) {

    /** `false` si no hay proveedor de IA configurado; la UI se oculta entonces. */
    fun isAvailable(): Boolean = runCatching { keyStore.isProviderUsable() }.getOrDefault(false)

    /**
     * Ejecuta [block] con un tope de tiempo DURO.
     *
     * Aunque el cliente HTTP ya tiene sus timeouts, la cola reintenta con backoff
     * (5s + 15s) ante límites de cuota, así que en el peor caso una sola llamada
     * podía tardar minutos y la interfaz se veía "colgada" resumiendo o traduciendo.
     * Con [withTimeoutOrNull] la operación SIEMPRE termina: o devuelve el resultado,
     * o falla con un mensaje claro que la UI puede mostrar y reintentar.
     *
     * Se usa `withTimeoutOrNull` (y no `withTimeout` dentro de `runCatching`) para no
     * tragarnos la cancelación del padre: si el usuario sale de la pantalla, la
     * corrutina se cancela limpiamente en vez de convertirse en un `Result.failure`.
     */
    private suspend fun <T> withAiTimeout(block: suspend () -> T): Result<T> {
        val outcome = withTimeoutOrNull(AI_TIMEOUT_MS) { runCatching { block() } }
        return outcome ?: Result.failure(
            Exception("La IA tardó demasiado en responder. Revisa tu conexión o inténtalo de nuevo.")
        )
    }

    /**
     * Tres respuestas cortas para contestar al último mensaje.
     *
     * Se piden en líneas separadas y no en JSON a propósito: los modelos
     * pequeños que la gente conecta por OpenRouter u Ollama fallan a menudo al
     * cerrar el JSON, y una lista de líneas es trivial de recuperar aunque la
     * respuesta venga sucia.
     */
    suspend fun smartReplies(transcript: String): Result<List<String>> = withContext(Dispatchers.IO) {
        if (transcript.isBlank()) return@withContext Result.success(emptyList())
        withAiTimeout {
            val response = api.chatCompletion(
                messages = listOf(
                    Message(
                        role = "user",
                        content = """
                            Eres un asistente que sugiere respuestas rápidas en un chat.
                            Lee la conversación y propón EXACTAMENTE 3 respuestas que podría
                            enviar "YO" ahora mismo.

                            Reglas:
                            - Una por línea, sin numerar, sin comillas, sin viñetas.
                            - Máximo 8 palabras cada una.
                            - En el MISMO idioma de la conversación.
                            - Que sean distintas entre sí: una afirmativa, una negativa o
                              dudosa, y una que haga avanzar la conversación.
                            - No expliques nada. Devuelve sólo las 3 líneas.

                            Conversación:
                            $transcript
                        """.trimIndent()
                    )
                ),
                temperature = 0.7f,
                maxTokens = 120
            )
            response.content
                .lineSequence()
                .map { it.trim().removePrefix("-").removePrefix("•").trim().trim('"') }
                // Un modelo desobediente a veces numera igual; se limpia el "1." inicial.
                .map { it.replace(Regex("""^\d+[.)]\s*"""), "") }
                .filter { it.isNotBlank() && it.length <= 60 }
                .take(3)
                .toList()
        }
    }

    /** Resumen en viñetas de la conversación. */
    suspend fun summarize(transcript: String): Result<String> = withContext(Dispatchers.IO) {
        if (transcript.isBlank()) return@withContext Result.failure(Exception("No hay mensajes que resumir"))
        withAiTimeout {
            api.chatCompletion(
                messages = listOf(
                    Message(
                        role = "user",
                        content = """
                            Resume esta conversación de chat en el MISMO idioma en que está escrita.

                            Formato:
                            - 3 a 5 viñetas con lo importante que se dijo.
                            - Si quedó algo pendiente o acordado, una última línea que empiece
                              por "Pendiente:".
                            - Nada de introducciones ni despedidas.

                            Conversación:
                            $transcript
                        """.trimIndent()
                    )
                ),
                temperature = 0.3f,
                maxTokens = 400
            ).content.trim()
        }
    }

    /** Reescribe [draft] en el [tone] indicado, conservando el idioma original. */
    suspend fun rewriteTone(draft: String, tone: MessageTone): Result<String> = withContext(Dispatchers.IO) {
        if (draft.isBlank()) return@withContext Result.failure(Exception("No hay nada que reescribir"))
        withAiTimeout {
            api.chatCompletion(
                messages = listOf(
                    Message(
                        role = "user",
                        content = """
                            Reescribe el siguiente mensaje en un tono ${tone.instruction}.

                            Reglas:
                            - Mantén el idioma original y el significado.
                            - Devuelve SÓLO el mensaje reescrito, sin comillas ni explicaciones.
                            - No añadas información que no esté en el original.

                            Mensaje:
                            $draft
                        """.trimIndent()
                    )
                ),
                temperature = 0.5f,
                maxTokens = 400
            ).content.trim().trim('"')
        }
    }

    /**
     * Traducción con el modelo del usuario.
     *
     * Es notablemente mejor que la API gratuita de memoria de traducción que usa
     * [com.Azelmods.App.data.translation.TranslationService] como respaldo,
     * sobre todo con jerga, emoji y mensajes cortos sin contexto, que es
     * justamente lo que se escribe en un chat.
     */
    suspend fun translate(text: String, targetLanguageName: String): Result<String> = withContext(Dispatchers.IO) {
        if (text.isBlank()) return@withContext Result.failure(Exception("Texto vacío"))
        withAiTimeout {
            api.chatCompletion(
                messages = listOf(
                    Message(
                        role = "user",
                        content = """
                            Traduce el siguiente mensaje de chat a $targetLanguageName.

                            Reglas:
                            - Devuelve SÓLO la traducción, sin comillas ni notas.
                            - Conserva emoji, menciones (@usuario), enlaces y formato.
                            - Si ya está en $targetLanguageName, devuélvelo tal cual.
                            - Es lenguaje coloquial de chat: tradúcelo como hablaría una
                              persona, no de forma literal.

                            Mensaje:
                            $text
                        """.trimIndent()
                    )
                ),
                temperature = 0.2f,
                maxTokens = 600
            ).content.trim().trim('"')
        }
    }

    companion object {
        /** Tope duro por operación de IA. Más allá, se falla con un mensaje claro. */
        private const val AI_TIMEOUT_MS = 45_000L

        /**
         * Convierte los últimos mensajes en un guion legible para el modelo.
         *
         * Se etiquetan como "YO" y "OTRO" en vez de con los nombres reales: al
         * modelo no le hace falta saber con quién habla el usuario, y así no se
         * envían nombres de personas a un proveedor externo sin necesidad.
         */
        fun buildTranscript(
            messages: List<Pair<Boolean, String>>,
            maxMessages: Int = 20
        ): String = messages
            .takeLast(maxMessages)
            .filter { it.second.isNotBlank() }
            .joinToString("\n") { (isMine, content) ->
                val who = if (isMine) "YO" else "OTRO"
                "$who: ${content.take(500)}"
            }
    }
}
