package com.Azelmods.App.data.ai.agents

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

/**
 * Lo que un agente decide en cada paso.
 *
 * El protocolo es JSON en texto plano y no el "tool calling" nativo de cada
 * proveedor a propósito: así funciona IGUAL con Gemini, OpenAI, OpenRouter,
 * DeepSeek, Groq o un modelo local de Ollama, que no comparten formato de
 * herramientas (y varios modelos locales no lo soportan).
 */
sealed class AgentDecision {
    /** Quiere usar una herramienta y esperar su resultado. */
    data class UseTool(val thought: String, val tool: String, val input: String) : AgentDecision()

    /** Ha terminado: [answer] es la respuesta en Markdown. */
    data class Final(val thought: String, val answer: String) : AgentDecision()
}

/** Un paso del plan del coordinador: qué agente hace qué. */
data class PlanStep(val agentId: String, val task: String)

/**
 * Interpreta las respuestas de los modelos.
 *
 * Los modelos no siempre devuelven JSON limpio: lo envuelven en ```json, añaden
 * una frase antes, o directamente contestan en prosa. Este parser es tolerante a
 * propósito: si no encuentra un objeto válido, toma el texto como respuesta
 * final en vez de dar error, porque una respuesta útil mal formateada es mejor
 * que ninguna.
 */
object AgentProtocol {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseDecision(raw: String): AgentDecision {
        val text = raw.trim()
        val obj = extractJsonObject(text)?.let { parseObject(it) }

        if (obj != null) {
            val thought = obj.string("thought") ?: obj.string("pensamiento").orEmpty()
            val final = obj.string("final") ?: obj.string("answer") ?: obj.string("respuesta")
            val action = obj.string("action") ?: obj.string("tool") ?: obj.string("accion")
            val input = obj.string("input") ?: obj.string("action_input") ?: obj.string("query").orEmpty()

            if (!action.isNullOrBlank() && !action.equals("final", ignoreCase = true) &&
                !action.equals("none", ignoreCase = true)
            ) {
                return AgentDecision.UseTool(thought, action.trim(), input.trim())
            }
            if (final != null) return AgentDecision.Final(thought, final.trim())
            // Objeto sin "final" ni "action": su pensamiento es lo único útil.
            if (thought.isNotBlank()) return AgentDecision.Final("", thought.trim())
        }

        // JSON roto pero con un "final" reconocible (p. ej. saltos de línea sin
        // escapar dentro de la cadena): se rescata su contenido.
        salvageFinal(text)?.let { return AgentDecision.Final("", it) }

        return AgentDecision.Final("", stripCodeFence(text))
    }

    /**
     * Plan del coordinador: `{"plan":[{"agent":"investigador","task":"..."}]}`
     * (también acepta el array suelto). Ignora agentes que no estén en [validIds].
     */
    fun parsePlan(raw: String, validIds: Set<String>, maxSteps: Int = 4): List<PlanStep> {
        val text = stripCodeFence(raw.trim())
        // Primero el formato pedido ({"plan":[…]}); si el primer objeto no trae
        // plan (p. ej. el modelo devolvió el array suelto y el primer "{" es el
        // de su primer paso), se busca el array.
        val obj = extractJsonObject(text)?.let { parseObject(it) }
        val fromObject = (obj?.get("plan") ?: obj?.get("steps") ?: obj?.get("pasos")) as? JsonArray
        val array: JsonArray = fromObject
            ?: extractJsonArray(text)?.let { runCatching { json.parseToJsonElement(it) as? JsonArray }.getOrNull() }
            ?: return emptyList()

        return array.mapNotNull { item ->
            val o = item as? JsonObject ?: return@mapNotNull null
            val agent = (o.string("agent") ?: o.string("agente") ?: o.string("id"))?.trim()?.lowercase()
            val task = (o.string("task") ?: o.string("tarea")).orEmpty().trim()
            if (agent == null || task.isBlank()) return@mapNotNull null
            val id = validIds.firstOrNull { it == agent || agent.startsWith(it) || it.startsWith(agent) }
                ?: return@mapNotNull null
            PlanStep(id, task)
        }.take(maxSteps)
    }

    /** El campo "thought" de una respuesta JSON, si lo hay (p. ej. el del plan). */
    fun extractThought(raw: String): String =
        extractJsonObject(raw.trim())?.let { parseObject(it) }?.string("thought")?.trim().orEmpty()

    /** Primer objeto `{…}` equilibrado del texto, respetando cadenas y escapes. */
    fun extractJsonObject(text: String): String? = extractBalanced(stripCodeFence(text), '{', '}')

    private fun extractJsonArray(text: String): String? = extractBalanced(stripCodeFence(text), '[', ']')

    private fun extractBalanced(text: String, open: Char, close: Char): String? {
        var start = text.indexOf(open)
        while (start >= 0) {
            var depth = 0
            var inString = false
            var escaped = false
            for (i in start until text.length) {
                val c = text[i]
                if (inString) {
                    when {
                        escaped -> escaped = false
                        c == '\\' -> escaped = true
                        c == '"' -> inString = false
                    }
                    continue
                }
                when (c) {
                    '"' -> inString = true
                    open -> depth++
                    close -> {
                        depth--
                        if (depth == 0) return text.substring(start, i + 1)
                    }
                }
            }
            start = text.indexOf(open, start + 1)
        }
        return null
    }

    private fun parseObject(candidate: String): JsonObject? =
        runCatching { json.parseToJsonElement(candidate) as? JsonObject }.getOrNull()
            // Saltos de línea literales dentro de las cadenas: JSON no los admite,
            // pero es el fallo más común de los modelos pequeños.
            ?: runCatching { json.parseToJsonElement(escapeRawNewlinesInStrings(candidate)) as? JsonObject }.getOrNull()

    private fun escapeRawNewlinesInStrings(s: String): String {
        val sb = StringBuilder(s.length + 16)
        var inString = false
        var escaped = false
        for (c in s) {
            if (inString) {
                when {
                    escaped -> { escaped = false; sb.append(c) }
                    c == '\\' -> { escaped = true; sb.append(c) }
                    c == '"' -> { inString = false; sb.append(c) }
                    c == '\n' -> sb.append("\\n")
                    c == '\r' -> Unit
                    c == '\t' -> sb.append("\\t")
                    else -> sb.append(c)
                }
            } else {
                if (c == '"') inString = true
                sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun salvageFinal(text: String): String? {
        val match = Regex("\"final\"\\s*:\\s*\"").find(text) ?: return null
        val startIdx = match.range.last + 1
        val endIdx = text.lastIndexOf('"')
        if (endIdx <= startIdx) return null
        return text.substring(startIdx, endIdx)
            .replace("\\n", "\n")
            .replace("\\\"", "\"")
            .replace("\\t", "\t")
            .trim()
            .takeIf { it.isNotEmpty() }
    }

    fun stripCodeFence(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("```")) return trimmed
        val firstNewline = trimmed.indexOf('\n')
        if (firstNewline < 0) return trimmed.trim('`').trim()
        val body = trimmed.substring(firstNewline + 1)
        return body.substringBeforeLast("```").trim()
    }

    private fun JsonObject.string(key: String): String? {
        val value = this[key] ?: return null
        return when (value) {
            is JsonPrimitive -> value.contentOrNull
            // Algunos modelos mandan el input como objeto ({"query": "..."}).
            is JsonObject -> value.values.firstOrNull()?.let { (it as? JsonPrimitive)?.contentOrNull } ?: value.toString()
            is JsonArray -> value.joinToString("\n") { (it as? JsonPrimitive)?.contentOrNull ?: it.toString() }
        }
    }
}
