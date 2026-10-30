package com.Azelmods.App.data.ai.agents

import android.util.Log
import com.Azelmods.App.data.api.AzelAIApiService
import com.Azelmods.App.data.api.Message
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Motor de los agentes de IA.
 *
 * Cada agente trabaja en bucle "pensar → usar herramienta → observar" (patrón
 * ReAct) hasta dar su respuesta final. En modo equipo, un coordinador reparte
 * tareas entre especialistas, cada uno recibe el trabajo de los anteriores y un
 * revisor entrega el resultado.
 *
 * Todo lo que ocurre se publica en un [MutableStateFlow] de [AgentRunState]: la
 * interfaz lo pinta en vivo (qué agente trabaja, qué herramienta usa, qué
 * encontró), que es lo que permite ver a los agentes trabajar.
 *
 * Usa el proveedor de IA que el usuario tenga configurado (Gemini, OpenAI,
 * OpenRouter, DeepSeek, Ollama…): el protocolo es JSON en texto, no el formato
 * de herramientas propio de cada uno.
 */
@Singleton
class AgentOrchestrator @Inject constructor(
    private val api: AzelAIApiService,
    private val toolbox: AgentToolbox
) {
    companion object {
        private const val TAG = "AgentOrchestrator"
        private const val CONTEXT_CHARS_PER_AGENT = 3_000
        private const val OLD_OBSERVATION_CHARS = 400
    }

    private val eventIds = AtomicLong(0)

    fun newRunState(runId: String, goal: String, agents: List<AgentDefinition>, isTeam: Boolean, title: String) =
        AgentRunState(
            runId = runId,
            goal = goal,
            title = title,
            isTeam = isTeam,
            nodes = agents.map { AgentNode(it) }
        )

    /** Un agente en solitario. Devuelve su respuesta. */
    suspend fun runSingle(
        agent: AgentDefinition,
        goal: String,
        model: String,
        state: MutableStateFlow<AgentRunState>
    ): String {
        val rec = Recorder(state)
        val answer = runAgent(agent, goal, context = "", model = model, rec = rec)
        rec.finish(answer)
        return answer
    }

    /** Un equipo: plan del coordinador → especialistas en cadena → revisor. */
    suspend fun runTeam(
        team: AgentTeam,
        goal: String,
        model: String,
        state: MutableStateFlow<AgentRunState>
    ): String {
        val rec = Recorder(state)
        val members = team.memberIds.mapNotNull { AgentCatalog.agent(it) }
        val finisher = AgentCatalog.agent(team.finisherId) ?: AgentCatalog.REVIEWER

        // ── 1. Plan ──
        val plan = makePlan(members, goal, model, rec).ifEmpty {
            // El coordinador no devolvió un plan utilizable: orden natural del equipo.
            members.map { PlanStep(it.id, goal) }
        }
        rec.event { AgentEvent.Plan(it, AgentCatalog.COORDINATOR.id, plan) }
        state.update { it.copy(plan = plan) }
        rec.node(AgentCatalog.COORDINATOR.id) {
            copy(status = AgentNodeStatus.DONE, activity = "Plan de ${plan.size} pasos")
        }

        // ── 2. Especialistas ──
        val outputs = mutableListOf<Pair<AgentDefinition, String>>()
        plan.forEachIndexed { index, step ->
            val agent = AgentCatalog.agent(step.agentId) ?: return@forEachIndexed
            state.update { it.copy(currentStep = index) }
            rec.event { AgentEvent.Handoff(it, agent.id, step.task) }
            val output = runAgent(agent, step.task, buildTeamContext(goal, outputs), model, rec)
            outputs += agent to output
        }

        // ── 3. Revisión y entrega ──
        state.update { it.copy(currentStep = plan.size) }
        val finalTask = "Objetivo original del usuario: $goal\n\n" +
            "Revisa el trabajo del equipo, corrige lo que haga falta y entrega la respuesta final completa."
        rec.event { AgentEvent.Handoff(it, finisher.id, "Revisar y entregar el resultado final") }
        val answer = runAgent(finisher, finalTask, buildTeamContext(goal, outputs), model, rec)
        rec.finish(answer)
        return answer
    }

    // ── Planificación ─────────────────────────────────────────────────────

    private suspend fun makePlan(
        members: List<AgentDefinition>,
        goal: String,
        model: String,
        rec: Recorder
    ): List<PlanStep> {
        val coordinator = AgentCatalog.COORDINATOR
        rec.node(coordinator.id) { copy(status = AgentNodeStatus.THINKING, activity = "Planificando…", steps = 1) }

        val roster = members.joinToString("\n") { a ->
            val tools = if (a.tools.isEmpty()) "sin herramientas" else a.tools.joinToString { it.id }
            "- ${a.id}: ${a.role} ($tools)"
        }
        val system = coordinator.instructions + "\n\n" +
            "Especialistas disponibles:\n$roster\n\n" +
            "Responde SOLO con un objeto JSON, sin texto alrededor:\n" +
            "{\"thought\":\"por qué este plan\",\"plan\":[{\"agent\":\"<id>\",\"task\":\"<tarea concreta>\"}]}\n" +
            "Reglas: entre 1 y 4 pasos en orden lógico; usa solo los ids de la lista; cada tarea debe " +
            "ser concreta y aprovechar lo que devuelven los pasos anteriores; no incluyas al revisor, " +
            "él entrega el resultado al final. Escribe las tareas en el idioma del usuario."

        return try {
            val reply = callModel(system, "Objetivo: $goal", model)
            rec.llmCall()
            val thought = AgentProtocol.extractThought(reply)
            if (thought.isNotBlank()) rec.event { AgentEvent.Thought(it, coordinator.id, thought) }
            AgentProtocol.parsePlan(reply, members.map { it.id }.toSet())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "El coordinador falló, se usa el orden por defecto: ${e.message}")
            rec.event { AgentEvent.Failure(it, coordinator.id, "No pude planificar (${e.message}); sigo el orden del equipo.") }
            emptyList()
        }
    }

    private fun buildTeamContext(goal: String, outputs: List<Pair<AgentDefinition, String>>): String {
        if (outputs.isEmpty()) return "Objetivo general del equipo: $goal"
        return buildString {
            append("Objetivo general del equipo: ").append(goal).append("\n\nTrabajo previo del equipo:\n")
            outputs.forEach { (agent, text) ->
                append("\n### ").append(agent.emoji).append(' ').append(agent.name).append('\n')
                append(text.take(CONTEXT_CHARS_PER_AGENT))
                if (text.length > CONTEXT_CHARS_PER_AGENT) append("\n[…recortado…]")
                append('\n')
            }
        }
    }

    // ── Bucle de un agente ────────────────────────────────────────────────

    private suspend fun runAgent(
        agent: AgentDefinition,
        task: String,
        context: String,
        model: String,
        rec: Recorder
    ): String {
        val scratch = mutableListOf<String>()
        val maxSteps = agent.maxSteps.coerceAtLeast(1)

        try {
            for (step in 1..maxSteps + 1) {
                val forceFinal = step > maxSteps || agent.tools.isEmpty()
                rec.node(agent.id) {
                    copy(
                        status = AgentNodeStatus.THINKING,
                        activity = when {
                            step == 1 -> "Analizando la tarea…"
                            forceFinal -> "Redactando la respuesta…"
                            else -> "Pensando con lo encontrado…"
                        },
                        activeTool = null,
                        steps = step
                    )
                }

                val reply = callModel(
                    systemPrompt = buildSystemPrompt(agent, forceFinal),
                    userPrompt = buildUserPrompt(task, context, scratch, forceFinal),
                    model = model
                )
                rec.llmCall()

                when (val decision = AgentProtocol.parseDecision(reply)) {
                    is AgentDecision.Final -> {
                        if (decision.thought.isNotBlank()) rec.event { AgentEvent.Thought(it, agent.id, decision.thought) }
                        val answer = decision.answer.ifBlank { "No tengo nada que añadir." }
                        rec.event { AgentEvent.Output(it, agent.id, answer) }
                        rec.node(agent.id) { copy(status = AgentNodeStatus.DONE, activity = "Listo", activeTool = null) }
                        return answer
                    }
                    is AgentDecision.UseTool -> {
                        if (decision.thought.isNotBlank()) rec.event { AgentEvent.Thought(it, agent.id, decision.thought) }
                        if (forceFinal) {
                            // Ya no puede usar herramientas y aun así lo intenta: se le
                            // vuelve a pedir la respuesta con lo que tiene.
                            scratch += "Intentaste usar ${decision.tool} pero ya no quedan herramientas: responde ya."
                            if (step > maxSteps) break
                            continue
                        }
                        val observation = useTool(agent, decision, rec)
                        scratch += "Paso $step\nPensamiento: ${decision.thought}\n" +
                            "Acción: ${decision.tool}(\"${decision.input}\")\nResultado:\n$observation"
                    }
                }
            }
        } catch (e: CancellationException) {
            rec.node(agent.id) { copy(status = AgentNodeStatus.ERROR, activity = "Detenido", activeTool = null) }
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "El agente ${agent.id} falló", e)
            rec.node(agent.id) { copy(status = AgentNodeStatus.ERROR, activity = "Error", activeTool = null) }
            rec.event { AgentEvent.Failure(it, agent.id, e.message ?: "Error desconocido") }
            throw e
        }

        // Agotó los intentos sin respuesta final limpia: se entrega lo que reunió.
        val fallback = "No pude cerrar una respuesta completa. Esto es lo que encontré:\n\n" +
            scratch.joinToString("\n\n") { it.take(800) }
        rec.event { AgentEvent.Output(it, agent.id, fallback) }
        rec.node(agent.id) { copy(status = AgentNodeStatus.DONE, activity = "Terminado con avisos", activeTool = null) }
        return fallback
    }

    private suspend fun useTool(agent: AgentDefinition, decision: AgentDecision.UseTool, rec: Recorder): String {
        val toolId = AgentToolId.fromId(decision.tool)
        rec.event { AgentEvent.ToolCall(it, agent.id, toolId, decision.tool, decision.input) }

        if (toolId == null || toolId !in agent.tools) {
            val available = agent.tools.joinToString { it.id }.ifBlank { "ninguna" }
            val msg = "La herramienta «${decision.tool}» no está disponible. Disponibles: $available."
            rec.event { AgentEvent.ToolResult(it, agent.id, toolId, msg, ok = false) }
            return msg
        }

        rec.node(agent.id) {
            copy(status = AgentNodeStatus.USING_TOOL, activity = toolActivity(toolId, decision.input), activeTool = toolId)
        }
        val outcome = toolbox.run(toolId, decision.input)
        rec.toolCall()
        rec.event { AgentEvent.ToolResult(it, agent.id, toolId, outcome.output, outcome.ok) }
        return outcome.output
    }

    private fun toolActivity(tool: AgentToolId, input: String): String {
        val shortInput = input.take(40).let { if (input.length > 40) "$it…" else it }
        return when (tool) {
            AgentToolId.WEB_SEARCH -> "Buscando «$shortInput»"
            AgentToolId.WIKIPEDIA -> "Consultando Wikipedia"
            AgentToolId.READ_URL -> "Leyendo la página"
            AgentToolId.CALCULATOR -> "Calculando"
            AgentToolId.DATETIME -> "Mirando la fecha"
        }
    }

    // ── Prompts ───────────────────────────────────────────────────────────

    private fun buildSystemPrompt(agent: AgentDefinition, forceFinal: Boolean): String = buildString {
        append("Eres «").append(agent.name).append("», un agente de IA dentro de NexusChat. ")
        append(agent.instructions).append("\n\n")
        if (forceFinal || agent.tools.isEmpty()) {
            append("No uses herramientas: responde ya con lo que sabes y lo que te han pasado.\n")
            append("Responde SOLO con un objeto JSON, sin texto fuera de él:\n")
            append("{\"thought\":\"una frase sobre cómo lo has resuelto\",\"final\":\"tu respuesta completa en Markdown\"}\n")
        } else {
            append("Herramientas disponibles:\n")
            agent.tools.forEach { append("- ").append(it.id).append(": ").append(it.usage).append('\n') }
            append("\nResponde SIEMPRE con UN ÚNICO objeto JSON, sin texto fuera de él:\n")
            append("- Para usar una herramienta: {\"thought\":\"qué necesitas y por qué (una frase)\",\"action\":\"<herramienta>\",\"input\":\"<entrada>\"}\n")
            append("- Para terminar: {\"thought\":\"una frase\",\"final\":\"tu respuesta completa en Markdown\"}\n")
            append("Usa una herramienta por paso y como mucho ").append(agent.maxSteps).append(" en total. ")
            append("Si ya sabes la respuesta con seguridad, termina sin usar herramientas.\n")
        }
        append("\nReglas: no inventes datos ni fuentes; si una herramienta falla o no encuentra nada, dilo y ")
        append("sigue con lo que tengas; responde en el idioma del usuario. Dentro del JSON escapa las comillas ")
        append("y usa \\n para los saltos de línea.")
    }

    private fun buildUserPrompt(task: String, context: String, scratch: List<String>, forceFinal: Boolean): String =
        buildString {
            append("Tarea: ").append(task).append('\n')
            if (context.isNotBlank()) append('\n').append(context).append('\n')
            if (scratch.isNotEmpty()) {
                append("\nLo que has hecho hasta ahora:\n")
                scratch.forEachIndexed { i, entry ->
                    // Las observaciones antiguas se resumen para no desbordar el contexto.
                    val keepFull = i >= scratch.size - 3
                    append(if (keepFull) entry else entry.take(OLD_OBSERVATION_CHARS) + " […]").append("\n\n")
                }
            }
            append(if (forceFinal) "\nDa ya tu respuesta final en el JSON indicado." else "\nDecide tu siguiente paso.")
        }

    private suspend fun callModel(systemPrompt: String, userPrompt: String, model: String): String =
        api.chatCompletion(
            model = model,
            messages = listOf(Message("user", userPrompt)),
            temperature = 0.4f,
            maxTokens = 4096,
            systemPrompt = systemPrompt
        ).content

    // ── Publicación del estado ────────────────────────────────────────────

    private inner class Recorder(private val state: MutableStateFlow<AgentRunState>) {
        fun event(build: (Long) -> AgentEvent) {
            val e = build(eventIds.incrementAndGet())
            state.update { it.copy(events = it.events + e) }
        }

        fun node(agentId: String, change: AgentNode.() -> AgentNode) {
            state.update { s ->
                s.copy(nodes = s.nodes.map { if (it.agent.id == agentId) it.change() else it })
            }
        }

        fun llmCall() = state.update { it.copy(llmCalls = it.llmCalls + 1) }
        fun toolCall() = state.update { it.copy(toolCalls = it.toolCalls + 1) }

        fun finish(answer: String) = state.update {
            it.copy(status = AgentRunStatus.DONE, finalAnswer = answer, finishedAt = System.currentTimeMillis())
        }
    }
}
