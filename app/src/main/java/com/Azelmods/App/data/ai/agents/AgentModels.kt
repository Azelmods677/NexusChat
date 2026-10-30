package com.Azelmods.App.data.ai.agents

/** Herramientas que un agente puede usar. */
enum class AgentToolId(val id: String, val label: String, val emoji: String, val usage: String) {
    WEB_SEARCH(
        "web_search", "Búsqueda web", "🔎",
        "busca información actual en internet. input: la consulta, en pocas palabras."
    ),
    WIKIPEDIA(
        "wikipedia", "Wikipedia", "📚",
        "resumen enciclopédico de un tema. input: el nombre del tema."
    ),
    READ_URL(
        "read_url", "Leer página", "🌐",
        "descarga una página web y devuelve su texto. input: la URL completa (http/https; .onion solo con Tor activo)."
    ),
    CALCULATOR(
        "calculator", "Calculadora", "🧮",
        "calcula una expresión exacta. input: la expresión, p. ej. (1200*0.21)+15 o sqrt(2)."
    ),
    DATETIME(
        "datetime", "Fecha y hora", "🕒",
        "devuelve la fecha y hora actuales del dispositivo. input: vacío."
    );

    companion object {
        fun fromId(id: String): AgentToolId? {
            val key = id.trim().lowercase().replace('-', '_').replace(' ', '_')
            return entries.firstOrNull { it.id == key || it.name.lowercase() == key }
        }
    }
}

/** Un agente: identidad, estilo de trabajo y herramientas permitidas. */
data class AgentDefinition(
    val id: String,
    val name: String,
    val role: String,
    val emoji: String,
    /** Color ARGB del agente en la interfaz. */
    val color: Long,
    val instructions: String,
    val tools: Set<AgentToolId> = emptySet(),
    val maxSteps: Int = 4
)

/** Un equipo: coordinador + especialistas que trabajan en cadena. */
data class AgentTeam(
    val id: String,
    val name: String,
    val description: String,
    val emoji: String,
    /** Especialistas disponibles para el plan, en su orden natural. */
    val memberIds: List<String>,
    /** Agente que entrega la respuesta final (revisa y une el trabajo). */
    val finisherId: String
)

enum class AgentNodeStatus { WAITING, THINKING, USING_TOOL, DONE, ERROR }

/** Estado visible de un agente dentro de una ejecución. */
data class AgentNode(
    val agent: AgentDefinition,
    val status: AgentNodeStatus = AgentNodeStatus.WAITING,
    val activity: String = "En espera",
    val activeTool: AgentToolId? = null,
    val steps: Int = 0
)

/** Lo que va pasando, para el registro en vivo. */
sealed class AgentEvent {
    abstract val id: Long
    abstract val agentId: String

    data class Plan(override val id: Long, override val agentId: String, val steps: List<PlanStep>) : AgentEvent()
    data class Handoff(override val id: Long, override val agentId: String, val task: String) : AgentEvent()
    data class Thought(override val id: Long, override val agentId: String, val text: String) : AgentEvent()
    data class ToolCall(override val id: Long, override val agentId: String, val tool: AgentToolId?, val toolName: String, val input: String) : AgentEvent()
    data class ToolResult(override val id: Long, override val agentId: String, val tool: AgentToolId?, val output: String, val ok: Boolean) : AgentEvent()
    data class Output(override val id: Long, override val agentId: String, val text: String) : AgentEvent()
    data class Failure(override val id: Long, override val agentId: String, val message: String) : AgentEvent()
}

enum class AgentRunStatus { RUNNING, DONE, FAILED, CANCELLED }

data class AgentRunState(
    val runId: String,
    val goal: String,
    val title: String,
    val isTeam: Boolean,
    val nodes: List<AgentNode>,
    val events: List<AgentEvent> = emptyList(),
    val plan: List<PlanStep> = emptyList(),
    /** Índice del paso del plan en curso (-1 = planificando). */
    val currentStep: Int = -1,
    val status: AgentRunStatus = AgentRunStatus.RUNNING,
    val finalAnswer: String = "",
    val error: String? = null,
    val startedAt: Long = System.currentTimeMillis(),
    val finishedAt: Long? = null,
    val llmCalls: Int = 0,
    val toolCalls: Int = 0
) {
    val isRunning: Boolean get() = status == AgentRunStatus.RUNNING
}
