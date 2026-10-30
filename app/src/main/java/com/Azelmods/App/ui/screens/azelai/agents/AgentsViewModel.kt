package com.Azelmods.App.ui.screens.azelai.agents

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.ai.agents.AgentCatalog
import com.Azelmods.App.data.ai.agents.AgentDefinition
import com.Azelmods.App.data.ai.agents.AgentOrchestrator
import com.Azelmods.App.data.ai.agents.AgentRunState
import com.Azelmods.App.data.ai.agents.AgentRunStatus
import com.Azelmods.App.data.ai.agents.AgentTeam
import com.Azelmods.App.data.api.AzelAIApiService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/** Ejecución guardada en el historial (lo que se ve en "Recientes"). */
data class AgentRunSummary(
    val runId: String,
    val title: String,
    val goal: String,
    val emojis: String,
    val finalAnswer: String,
    val startedAt: Long,
    val durationMs: Long,
    val succeeded: Boolean
)

data class AgentsUiState(
    val isTeamMode: Boolean = false,
    val selectedAgentId: String = AgentCatalog.singles.first().id,
    val selectedTeamId: String = AgentCatalog.teams.first().id,
    val isProviderConfigured: Boolean = true,
    val providerLabel: String = "",
    val history: List<AgentRunSummary> = emptyList()
)

@HiltViewModel
class AgentsViewModel @Inject constructor(
    private val orchestrator: AgentOrchestrator,
    private val apiService: AzelAIApiService
) : ViewModel() {

    private companion object {
        const val TAG = "AgentsViewModel"
        const val HISTORY_LIMIT = 20
    }

    private val _ui = MutableStateFlow(AgentsUiState())
    val ui: StateFlow<AgentsUiState> = _ui.asStateFlow()

    /** Ejecución en pantalla (null = pantalla de configuración). */
    private val _run = MutableStateFlow<AgentRunState?>(null)
    val run: StateFlow<AgentRunState?> = _run.asStateFlow()

    private var runJob: Job? = null

    init {
        refreshProvider()
        loadHistory()
    }

    fun refreshProvider() {
        val (name, model) = apiService.activeProviderSummary()
        _ui.update { it.copy(isProviderConfigured = apiService.isConfigured(), providerLabel = "$name · $model") }
    }

    fun setTeamMode(team: Boolean) = _ui.update { it.copy(isTeamMode = team) }
    fun selectAgent(id: String) = _ui.update { it.copy(selectedAgentId = id) }
    fun selectTeam(id: String) = _ui.update { it.copy(selectedTeamId = id) }

    fun start(goal: String, geminiModel: String) {
        val cleanGoal = goal.trim()
        if (cleanGoal.isEmpty() || _run.value?.isRunning == true) return
        refreshProvider()

        val ui = _ui.value
        val runId = "run_${System.currentTimeMillis()}"
        val (state, block) = if (ui.isTeamMode) {
            val team = AgentCatalog.teams.firstOrNull { it.id == ui.selectedTeamId } ?: AgentCatalog.teams.first()
            val nodes = teamRoster(team)
            val flow = MutableStateFlow(
                orchestrator.newRunState(runId, cleanGoal, nodes, isTeam = true, title = "${team.emoji} ${team.name}")
            )
            flow to suspend { orchestrator.runTeam(team, cleanGoal, geminiModel, flow) }
        } else {
            val agent = AgentCatalog.agent(ui.selectedAgentId) ?: AgentCatalog.singles.first()
            val flow = MutableStateFlow(
                orchestrator.newRunState(runId, cleanGoal, listOf(agent), isTeam = false, title = "${agent.emoji} ${agent.name}")
            )
            flow to suspend { orchestrator.runSingle(agent, cleanGoal, geminiModel, flow) }
        }

        _run.value = state.value
        runJob = viewModelScope.launch {
            // El orquestador escribe en su propio StateFlow; aquí se refleja en la
            // pantalla a la vez que trabaja.
            val mirror = launch { state.collect { _run.value = it } }
            try {
                if (!apiService.isConfigured()) {
                    throw IllegalStateException("No hay ninguna IA configurada. Añade tu clave o un modelo local en Ajustes → IA.")
                }
                block()
            } catch (e: CancellationException) {
                state.update {
                    it.copy(status = AgentRunStatus.CANCELLED, finishedAt = System.currentTimeMillis())
                }
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "La ejecución de agentes falló", e)
                state.update {
                    it.copy(
                        status = AgentRunStatus.FAILED,
                        error = friendlyError(e),
                        finishedAt = System.currentTimeMillis()
                    )
                }
            } finally {
                _run.value = state.value
                mirror.cancel()
                saveToHistory(state.value)
            }
        }
    }

    /** Detiene la ejecución en curso; lo hecho hasta ahora sigue en pantalla. */
    fun cancel() {
        runJob?.cancel()
        _run.update { current ->
            current?.takeIf { it.isRunning }?.copy(
                status = AgentRunStatus.CANCELLED,
                finishedAt = System.currentTimeMillis()
            ) ?: current
        }
    }

    /** Vuelve a la pantalla de configuración. */
    fun close() {
        if (_run.value?.isRunning == true) cancel()
        _run.value = null
    }

    /** Abre una ejecución del historial (solo lectura). */
    fun openFromHistory(summary: AgentRunSummary) {
        if (_run.value?.isRunning == true) return
        _run.value = AgentRunState(
            runId = summary.runId,
            goal = summary.goal,
            title = summary.title,
            isTeam = false,
            nodes = emptyList(),
            status = if (summary.succeeded) AgentRunStatus.DONE else AgentRunStatus.FAILED,
            finalAnswer = summary.finalAnswer,
            startedAt = summary.startedAt,
            finishedAt = summary.startedAt + summary.durationMs
        )
    }

    private fun teamRoster(team: AgentTeam): List<AgentDefinition> =
        (listOf(AgentCatalog.COORDINATOR) +
            team.memberIds.mapNotNull { AgentCatalog.agent(it) } +
            listOfNotNull(AgentCatalog.agent(team.finisherId))).distinctBy { it.id }

    private fun friendlyError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg == AzelAIApiService.API_KEY_MISSING ->
                "No hay ninguna IA configurada. Añade tu clave o un modelo local en Ajustes → IA."
            e is java.net.UnknownHostException -> "Sin conexión a internet."
            msg.contains("401") -> "La API key no es válida."
            msg.contains("429") -> "Límite de uso del proveedor alcanzado. Espera un momento y reinténtalo."
            else -> msg.ifBlank { "Error desconocido" }
        }
    }

    // ── Historial en Firebase (aiChats/{uid}/agentRuns) ───────────────────

    private fun historyRef() = FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
        FirebaseDatabase.getInstance().getReference("aiChats/$uid/agentRuns")
    }

    private fun loadHistory() {
        val ref = historyRef() ?: return
        viewModelScope.launch {
            try {
                val snapshot = ref.orderByChild("startedAt").limitToLast(HISTORY_LIMIT).get().await()
                val items = snapshot.children.mapNotNull { child ->
                    val goal = child.child("goal").getValue(String::class.java) ?: return@mapNotNull null
                    AgentRunSummary(
                        runId = child.key ?: return@mapNotNull null,
                        title = child.child("title").getValue(String::class.java).orEmpty(),
                        goal = goal,
                        emojis = child.child("emojis").getValue(String::class.java).orEmpty(),
                        finalAnswer = child.child("finalAnswer").getValue(String::class.java).orEmpty(),
                        startedAt = child.child("startedAt").getValue(Long::class.java) ?: 0L,
                        durationMs = child.child("durationMs").getValue(Long::class.java) ?: 0L,
                        succeeded = child.child("succeeded").getValue(Boolean::class.java) ?: false
                    )
                }.sortedByDescending { it.startedAt }
                _ui.update { it.copy(history = items) }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo cargar el historial de agentes: ${e.message}")
            }
        }
    }

    private fun saveToHistory(state: AgentRunState) {
        if (state.nodes.isEmpty()) return // abierta desde el historial: ya está guardada
        val ref = historyRef() ?: return
        val succeeded = state.status == AgentRunStatus.DONE && state.finalAnswer.isNotBlank()
        if (!succeeded && state.events.isEmpty()) return
        val summary = AgentRunSummary(
            runId = state.runId,
            title = state.title,
            goal = state.goal,
            emojis = state.nodes.joinToString("") { it.agent.emoji },
            finalAnswer = state.finalAnswer.ifBlank { state.error.orEmpty() },
            startedAt = state.startedAt,
            durationMs = (state.finishedAt ?: System.currentTimeMillis()) - state.startedAt,
            succeeded = succeeded
        )
        _ui.update { ui -> ui.copy(history = (listOf(summary) + ui.history.filter { it.runId != summary.runId }).take(HISTORY_LIMIT)) }
        // Ámbito propio: el guardado debe terminar aunque se cierre la pantalla.
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching {
                ref.child(state.runId).setValue(
                    mapOf(
                        "title" to summary.title,
                        "goal" to summary.goal.take(2_000),
                        "emojis" to summary.emojis,
                        "finalAnswer" to summary.finalAnswer.take(30_000),
                        "startedAt" to summary.startedAt,
                        "durationMs" to summary.durationMs,
                        "succeeded" to summary.succeeded,
                        "llmCalls" to state.llmCalls,
                        "toolCalls" to state.toolCalls
                    )
                ).await()
            }.onFailure { Log.w(TAG, "No se pudo guardar la ejecución: ${it.message}") }
        }
    }
}
