package com.Azelmods.App.ui.screens.azelai.agents

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Azelmods.App.data.ai.agents.AgentCatalog
import com.Azelmods.App.data.ai.agents.AgentDefinition
import com.Azelmods.App.data.ai.agents.AgentEvent
import com.Azelmods.App.data.ai.agents.AgentNode
import com.Azelmods.App.data.ai.agents.AgentNodeStatus
import com.Azelmods.App.data.ai.agents.AgentRunState
import com.Azelmods.App.data.ai.agents.AgentRunStatus
import com.Azelmods.App.data.ai.agents.AgentTeam
import com.Azelmods.App.data.ai.agents.PlanStep
import com.Azelmods.App.ui.components.MarkdownText
import com.Azelmods.App.ui.theme.NexusTokens
import kotlinx.coroutines.delay

private val BgDark = NexusTokens.Color.BgBase
private val Surface1 = NexusTokens.Color.BgSurface
private val Surface2 = NexusTokens.Color.BgElevated
private val TextPrimary = NexusTokens.Color.TextPrimary
private val TextSecondary = NexusTokens.Color.TextSecondary
private val TextMuted = NexusTokens.Color.TextMuted
private val Border = NexusTokens.Color.GlassBorder
private val Accent = NexusTokens.Color.PrimaryLight

/**
 * Pestaña "Agentes" de Azel IA.
 *
 * Sin ejecución en curso muestra la configuración (agente o equipo, objetivo,
 * ejemplos e historial). Con una ejecución, el "espacio de trabajo": los agentes
 * como nodos animados que se iluminan al pensar o usar herramientas, el plan del
 * equipo, el registro en vivo de lo que hacen y el resultado final.
 */
@Composable
fun AgentsPanel(
    geminiModel: String,
    onOpenSettings: () -> Unit,
    onSendToChat: (goal: String, answer: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AgentsViewModel = hiltViewModel()
) {
    val ui by viewModel.ui.collectAsState()
    val run by viewModel.run.collectAsState()

    LaunchedEffect(Unit) { viewModel.refreshProvider() }

    AnimatedContent(
        targetState = run == null,
        transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
        label = "agents_mode",
        modifier = modifier.fillMaxSize()
    ) { showSetup ->
        if (showSetup) {
            AgentSetup(
                ui = ui,
                onTeamMode = viewModel::setTeamMode,
                onSelectAgent = viewModel::selectAgent,
                onSelectTeam = viewModel::selectTeam,
                onStart = { goal -> viewModel.start(goal, geminiModel) },
                onOpenSettings = onOpenSettings,
                onOpenHistory = viewModel::openFromHistory
            )
        } else {
            run?.let { current ->
                AgentWorkspace(
                    run = current,
                    onCancel = viewModel::cancel,
                    onClose = viewModel::close,
                    onRetry = {
                        val goal = current.goal
                        viewModel.close()
                        viewModel.start(goal, geminiModel)
                    },
                    onSendToChat = { onSendToChat(current.goal, current.finalAnswer) }
                )
            }
        }
    }
}

// ╔══════════════════════════════════════════════════════════════════════╗
// ║  CONFIGURACIÓN                                                        ║
// ╚══════════════════════════════════════════════════════════════════════╝

@Composable
private fun AgentSetup(
    ui: AgentsUiState,
    onTeamMode: (Boolean) -> Unit,
    onSelectAgent: (String) -> Unit,
    onSelectTeam: (String) -> Unit,
    onStart: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: (AgentRunSummary) -> Unit
) {
    var goal by rememberSaveable { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column {
                Text(
                    "Agentes de IA",
                    color = TextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Dales un objetivo: buscan en la web, calculan, se reparten el trabajo y te entregan " +
                        "el resultado. Puedes ver cada paso mientras trabajan.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }
        }

        if (!ui.isProviderConfigured) {
            item { ProviderBanner(onOpenSettings) }
        }

        item {
            ModeSwitch(isTeam = ui.isTeamMode, onChange = onTeamMode)
        }

        if (ui.isTeamMode) {
            items(AgentCatalog.teams, key = { it.id }) { team ->
                TeamCard(team = team, selected = team.id == ui.selectedTeamId, onClick = { onSelectTeam(team.id) })
            }
        } else {
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(AgentCatalog.singles, key = { it.id }) { agent ->
                        AgentCard(agent = agent, selected = agent.id == ui.selectedAgentId, onClick = { onSelectAgent(agent.id) })
                    }
                }
            }
        }

        item {
            GoalInput(
                goal = goal,
                onGoalChange = { goal = it },
                enabled = ui.isProviderConfigured,
                onStart = {
                    if (goal.isNotBlank()) {
                        onStart(goal)
                        goal = ""
                    }
                }
            )
        }

        item {
          Column {
            Text("Ideas para probar", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgentCatalog.examples.forEach { example ->
                    SuggestionChip(
                        onClick = { goal = example },
                        label = { Text(example, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 260.dp)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Surface1,
                            labelColor = TextSecondary
                        ),
                        border = BorderStroke(0.5.dp, Border)
                    )
                }
            }
          }
        }

        if (ui.history.isNotEmpty()) {
            item {
                Text(
                    "Recientes",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(ui.history, key = { it.runId }) { summary ->
                HistoryRow(summary = summary, onClick = { onOpenHistory(summary) })
            }
        }
    }
}

@Composable
private fun ProviderBanner(onOpenSettings: () -> Unit) {
    Surface(
        color = NexusTokens.Color.Warning.copy(alpha = 0.12f),
        shape = RoundedCornerShape(NexusTokens.Radius.lg),
        border = BorderStroke(1.dp, NexusTokens.Color.Warning.copy(alpha = 0.5f))
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Key, null, tint = NexusTokens.Color.Warning)
            Spacer(Modifier.width(12.dp))
            Text(
                "Los agentes usan tu proveedor de IA. Configura una clave o un modelo local para empezar.",
                color = TextPrimary,
                fontSize = 13.sp,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenSettings) { Text("Configurar", color = NexusTokens.Color.Warning) }
        }
    }
}

@Composable
private fun ModeSwitch(isTeam: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusTokens.Radius.pill))
            .background(Surface1)
            .border(0.5.dp, Border, RoundedCornerShape(NexusTokens.Radius.pill))
            .padding(4.dp)
    ) {
        listOf(false to "🤖  Un agente", true to "👥  Un equipo").forEach { (team, label) ->
            val selected = team == isTeam
            val bg by animateColorAsState(
                if (selected) NexusTokens.Color.Primary else Color.Transparent,
                label = "mode_bg"
            )
            Box(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(NexusTokens.Radius.pill))
                    .background(bg)
                    .clickable { onChange(team) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    color = if (selected) Color.White else TextSecondary,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun AgentCard(agent: AgentDefinition, selected: Boolean, onClick: () -> Unit) {
    val color = Color(agent.color)
    Surface(
        modifier = Modifier
            .width(158.dp)
            .clickable(onClick = onClick),
        color = if (selected) color.copy(alpha = 0.12f) else Surface1,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(if (selected) 1.5.dp else 0.5.dp, if (selected) color else Border)
    ) {
        Column(Modifier.padding(14.dp)) {
            AgentAvatar(agent = agent, size = 48)
            Spacer(Modifier.height(12.dp))
            Text(agent.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                agent.role,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = 2,
                minLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            Spacer(Modifier.height(10.dp))
            Text(
                if (agent.tools.isEmpty()) "Solo razonamiento" else agent.tools.joinToString("  ") { it.emoji },
                color = TextMuted,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun TeamCard(team: AgentTeam, selected: Boolean, onClick: () -> Unit) {
    val members = listOf(AgentCatalog.COORDINATOR) +
        team.memberIds.mapNotNull { AgentCatalog.agent(it) } +
        listOfNotNull(AgentCatalog.agent(team.finisherId))
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (selected) NexusTokens.Color.Primary.copy(alpha = 0.12f) else Surface1,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(if (selected) 1.5.dp else 0.5.dp, if (selected) Accent else Border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(team.emoji, fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Text(team.name, color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                if (selected) Icon(Icons.Default.CheckCircle, null, tint = Accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(6.dp))
            Text(team.description, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically
            ) {
                members.forEachIndexed { i, agent ->
                    if (i > 0) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            null,
                            tint = TextMuted,
                            modifier = Modifier.padding(horizontal = 4.dp).size(14.dp)
                        )
                    }
                    AgentAvatar(agent = agent, size = 32)
                }
            }
        }
    }
}

@Composable
private fun GoalInput(goal: String, onGoalChange: (String) -> Unit, enabled: Boolean, onStart: () -> Unit) {
    Surface(
        color = Surface1,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(0.5.dp, Border)
    ) {
        Column(Modifier.padding(12.dp)) {
            TextField(
                value = goal,
                onValueChange = onGoalChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("¿Qué quieres que consigan?", color = TextMuted) },
                minLines = 2,
                maxLines = 6,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Accent
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                Button(
                    onClick = onStart,
                    enabled = enabled && goal.isNotBlank(),
                    shape = RoundedCornerShape(NexusTokens.Radius.pill),
                    colors = ButtonDefaults.buttonColors(containerColor = NexusTokens.Color.Primary)
                ) {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Poner a trabajar")
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(summary: AgentRunSummary, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Surface1,
        shape = RoundedCornerShape(NexusTokens.Radius.lg),
        border = BorderStroke(0.5.dp, Border)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(summary.emojis.ifBlank { "🤖" }, fontSize = 16.sp, maxLines = 1)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(summary.goal, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "${summary.title} · ${formatElapsed(summary.durationMs)}",
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                if (summary.succeeded) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                null,
                tint = if (summary.succeeded) NexusTokens.Color.Success else NexusTokens.Color.Error,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ╔══════════════════════════════════════════════════════════════════════╗
// ║  ESPACIO DE TRABAJO (agentes en vivo)                                 ║
// ╚══════════════════════════════════════════════════════════════════════╝

@Composable
private fun AgentWorkspace(
    run: AgentRunState,
    onCancel: () -> Unit,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onSendToChat: () -> Unit
) {
    val listState = rememberLazyListState()
    val agentsById = remember(run.nodes) { run.nodes.associate { it.agent.id to it.agent } }

    // Sigue el trabajo: al llegar un evento nuevo, baja hasta él.
    LaunchedEffect(run.events.size, run.status) {
        val total = listState.layoutInfo.totalItemsCount
        if (total > 0) listState.animateScrollToItem(total - 1)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            RunHeader(run = run, onCancel = onCancel, onClose = onClose)
        }
        if (run.nodes.isNotEmpty()) {
            item(key = "stage") { AgentStage(nodes = run.nodes) }
        }
        if (run.plan.isNotEmpty()) {
            item(key = "plan") { PlanCard(plan = run.plan, currentStep = run.currentStep, agentsById = agentsById, running = run.isRunning) }
        }
        if (run.events.isNotEmpty()) {
            item(key = "log_title") {
                Text("Actividad", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            items(run.events, key = { it.id }) { event ->
                EventRow(event = event, agent = agentsById[event.agentId], modifier = Modifier.animateItem())
            }
        }
        if (run.isRunning) {
            item(key = "working") { WorkingIndicator(run) }
        }
        when (run.status) {
            AgentRunStatus.DONE -> item(key = "result") {
                ResultCard(answer = run.finalAnswer, animate = run.nodes.isNotEmpty(), onSendToChat = onSendToChat, onNew = onClose)
            }
            AgentRunStatus.FAILED -> item(key = "failed") {
                StatusMessage(
                    icon = Icons.Default.ErrorOutline,
                    color = NexusTokens.Color.Error,
                    title = "No se pudo completar",
                    text = run.error ?: run.finalAnswer.ifBlank { "Error desconocido" },
                    primaryLabel = "Reintentar",
                    onPrimary = onRetry,
                    onSecondary = onClose
                )
            }
            AgentRunStatus.CANCELLED -> item(key = "cancelled") {
                StatusMessage(
                    icon = Icons.Default.StopCircle,
                    color = NexusTokens.Color.Warning,
                    title = "Detenido",
                    text = "Paraste la ejecución. Lo que hicieron los agentes sigue arriba.",
                    primaryLabel = "Volver a lanzar",
                    onPrimary = onRetry,
                    onSecondary = onClose
                )
            }
            AgentRunStatus.RUNNING -> Unit
        }
    }
}

@Composable
private fun RunHeader(run: AgentRunState, onCancel: () -> Unit, onClose: () -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(run.isRunning) {
        while (run.isRunning) {
            now = System.currentTimeMillis()
            delay(500)
        }
    }
    val elapsed = (run.finishedAt ?: now) - run.startedAt

    Surface(
        color = Surface1,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(0.5.dp, Border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Close, "Cerrar", tint = TextSecondary)
                }
                Spacer(Modifier.width(6.dp))
                Text(run.title, color = TextSecondary, fontSize = 13.sp, modifier = Modifier.weight(1f))
                StatusPill(run.status, elapsed)
            }
            Spacer(Modifier.height(10.dp))
            Text(run.goal, color = TextPrimary, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, lineHeight = 23.sp)
            if (run.nodes.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "🧠 ${run.llmCalls} consultas · 🔧 ${run.toolCalls} herramientas",
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (run.isRunning) {
                        OutlinedButton(
                            onClick = onCancel,
                            shape = RoundedCornerShape(NexusTokens.Radius.pill),
                            border = BorderStroke(1.dp, NexusTokens.Color.Error.copy(alpha = 0.6f)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Stop, null, tint = NexusTokens.Color.Error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Detener", color = NexusTokens.Color.Error, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: AgentRunStatus, elapsedMs: Long) {
    val (label, color) = when (status) {
        AgentRunStatus.RUNNING -> "Trabajando · ${formatElapsed(elapsedMs)}" to NexusTokens.Color.Secondary
        AgentRunStatus.DONE -> "Completado · ${formatElapsed(elapsedMs)}" to NexusTokens.Color.Success
        AgentRunStatus.FAILED -> "Error" to NexusTokens.Color.Error
        AgentRunStatus.CANCELLED -> "Detenido" to NexusTokens.Color.Warning
    }
    Row(
        Modifier
            .clip(RoundedCornerShape(NexusTokens.Radius.pill))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (status == AgentRunStatus.RUNNING) {
            val t = rememberInfiniteTransition(label = "dot")
            val a by t.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "a")
            Box(Modifier.size(7.dp).alpha(a).clip(CircleShape).background(color))
            Spacer(Modifier.width(6.dp))
        }
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

/** Los agentes en fila, unidos por conectores; el que trabaja brilla. */
@Composable
private fun AgentStage(nodes: List<AgentNode>) {
    Surface(
        color = Surface1,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(0.5.dp, Border)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 18.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = if (nodes.size == 1) Arrangement.Center else Arrangement.Start
        ) {
            nodes.forEachIndexed { index, node ->
                if (index > 0) {
                    val active = node.status == AgentNodeStatus.THINKING || node.status == AgentNodeStatus.USING_TOOL
                    val reached = node.status != AgentNodeStatus.WAITING
                    Connector(active = active, reached = reached, color = Color(node.agent.color))
                }
                AgentNodeView(node = node, wide = nodes.size == 1)
            }
        }
    }
}

@Composable
private fun Connector(active: Boolean, reached: Boolean, color: Color) {
    val t = rememberInfiniteTransition(label = "flow")
    val phase by t.animateFloat(0f, 24f, infiniteRepeatable(tween(700, easing = LinearEasing)), label = "phase")
    Canvas(
        Modifier
            .padding(top = 33.dp)
            .width(28.dp)
            .height(2.dp)
    ) {
        drawLine(
            color = when {
                active -> color
                reached -> color.copy(alpha = 0.55f)
                else -> Color.White.copy(alpha = 0.15f)
            },
            start = Offset(0f, size.height / 2),
            end = Offset(size.width, size.height / 2),
            strokeWidth = 4f,
            cap = StrokeCap.Round,
            pathEffect = if (active) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), -phase) else null
        )
    }
}

@Composable
private fun AgentNodeView(node: AgentNode, wide: Boolean) {
    val color = Color(node.agent.color)
    val working = node.status == AgentNodeStatus.THINKING || node.status == AgentNodeStatus.USING_TOOL
    val t = rememberInfiniteTransition(label = "node")
    val sweep by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1_200, easing = LinearEasing)), label = "sweep")
    val pulse by t.animateFloat(1f, 1.08f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "pulse")
    val bounce by t.animateFloat(0f, -5f, infiniteRepeatable(tween(420), RepeatMode.Reverse), label = "bounce")
    val alpha by animateFloatAsState(if (node.status == AgentNodeStatus.WAITING) 0.45f else 1f, label = "alpha")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(if (wide) 220.dp else 86.dp)
            .alpha(alpha)
    ) {
        Box(Modifier.size(68.dp), contentAlignment = Alignment.Center) {
            // Anillo: gira mientras piensa; fijo y del color de estado al acabar.
            Canvas(Modifier.fillMaxSize().rotate(if (working) sweep else 0f)) {
                val stroke = Stroke(width = 5f, cap = StrokeCap.Round)
                when (node.status) {
                    AgentNodeStatus.THINKING, AgentNodeStatus.USING_TOOL -> drawArc(
                        brush = Brush.sweepGradient(listOf(Color.Transparent, color, color)),
                        startAngle = 0f, sweepAngle = 300f, useCenter = false, style = stroke
                    )
                    AgentNodeStatus.DONE -> drawCircle(NexusTokens.Color.Success, style = stroke)
                    AgentNodeStatus.ERROR -> drawCircle(NexusTokens.Color.Error, style = stroke)
                    AgentNodeStatus.WAITING -> drawCircle(Color.White.copy(alpha = 0.18f), style = stroke)
                }
            }
            Box(Modifier.scale(if (working) pulse else 1f)) {
                AgentAvatar(agent = node.agent, size = 54)
            }
            // Insignia: herramienta en uso (rebota) o resultado.
            val badge = when {
                node.status == AgentNodeStatus.USING_TOOL -> node.activeTool?.emoji
                node.status == AgentNodeStatus.DONE -> "✓"
                node.status == AgentNodeStatus.ERROR -> "!"
                else -> null
            }
            if (badge != null) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .offset(y = if (node.status == AgentNodeStatus.USING_TOOL) bounce.dp else 0.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(
                            when (node.status) {
                                AgentNodeStatus.DONE -> NexusTokens.Color.Success
                                AgentNodeStatus.ERROR -> NexusTokens.Color.Error
                                else -> Surface2
                            }
                        )
                        .border(1.5.dp, Surface1, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(badge, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(node.agent.name, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        AnimatedContent(
            targetState = node.activity,
            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(150)) },
            label = "activity"
        ) { activity ->
            Text(
                activity,
                color = if (working) color else TextMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
fun AgentAvatar(agent: AgentDefinition, size: Int) {
    val color = Color(agent.color)
    Box(
        Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(Brush.linearGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0.12f))))
            .border(1.dp, color.copy(alpha = 0.6f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(agent.emoji, fontSize = (size * 0.48f).sp)
    }
}

@Composable
private fun PlanCard(plan: List<PlanStep>, currentStep: Int, agentsById: Map<String, AgentDefinition>, running: Boolean) {
    Surface(
        color = Surface1,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(0.5.dp, Border)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("📋 Plan del coordinador", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(10.dp))
            plan.forEachIndexed { i, step ->
                val agent = agentsById[step.agentId] ?: AgentCatalog.agent(step.agentId)
                val state = when {
                    i < currentStep || (!running && currentStep >= plan.size) -> 2
                    i == currentStep && running -> 1
                    else -> 0
                }
                Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                        when (state) {
                            2 -> Icon(Icons.Default.CheckCircle, null, tint = NexusTokens.Color.Success, modifier = Modifier.size(18.dp))
                            1 -> CircularProgressIndicator(Modifier.size(15.dp), strokeWidth = 2.dp, color = Color(agent?.color ?: 0xFFAB9EFF))
                            else -> Text("${i + 1}", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "${agent?.emoji ?: "🤖"} ${agent?.name ?: step.agentId}",
                            color = if (state == 1) Color(agent?.color ?: 0xFFAB9EFF) else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(step.task, color = if (state == 0) TextMuted else TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: AgentEvent, agent: AgentDefinition?, modifier: Modifier = Modifier) {
    val color = agent?.let { Color(it.color) } ?: Accent
    Row(modifier.fillMaxWidth()) {
        // Raíl del agente: punto de color + línea, como una línea de tiempo.
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(28.dp)) {
            Text(agent?.emoji ?: "🤖", fontSize = 15.sp)
            Box(
                Modifier
                    .padding(top = 4.dp)
                    .width(2.dp)
                    .heightIn(min = 12.dp)
                    .fillMaxHeight()
                    .background(color.copy(alpha = 0.35f))
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
            when (event) {
                is AgentEvent.Thought -> Text(
                    "💭 ${event.text}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    lineHeight = 18.sp
                )
                is AgentEvent.Handoff -> Text(
                    buildString {
                        append(agent?.name ?: "Agente").append(" recibe: ").append(event.task)
                    },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
                is AgentEvent.Plan -> Text(
                    "Planificó ${event.steps.size} pasos para el equipo",
                    color = TextPrimary,
                    fontSize = 13.sp
                )
                is AgentEvent.ToolCall -> Row(
                    Modifier
                        .clip(RoundedCornerShape(NexusTokens.Radius.md))
                        .background(color.copy(alpha = 0.10f))
                        .border(0.5.dp, color.copy(alpha = 0.4f), RoundedCornerShape(NexusTokens.Radius.md))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(event.tool?.emoji ?: "🔧", fontSize = 13.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${event.tool?.label ?: event.toolName}: ",
                        color = color,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        event.input.ifBlank { "—" },
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                is AgentEvent.ToolResult -> ExpandableBlock(
                    title = if (event.ok) "✅ Resultado de ${event.tool?.label ?: "la herramienta"}" else "⚠️ ${event.tool?.label ?: "Herramienta"} sin resultado",
                    body = event.output,
                    accent = if (event.ok) NexusTokens.Color.Success else NexusTokens.Color.Warning,
                    monospace = true
                )
                is AgentEvent.Output -> ExpandableBlock(
                    title = "📝 ${agent?.name ?: "Agente"} entregó su parte (${wordCount(event.text)} palabras)",
                    body = event.text,
                    accent = color,
                    markdown = true
                )
                is AgentEvent.Failure -> Text(
                    "⚠️ ${event.message}",
                    color = NexusTokens.Color.Error,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ExpandableBlock(
    title: String,
    body: String,
    accent: Color,
    monospace: Boolean = false,
    markdown: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    Surface(
        color = Surface1,
        shape = RoundedCornerShape(NexusTokens.Radius.md),
        border = BorderStroke(0.5.dp, Border),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded }
    ) {
        Column(Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    if (expanded) "Contraer" else "Ver más",
                    tint = TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
            if (!expanded) {
                Text(
                    body.lines().filter { it.isNotBlank() }.take(2).joinToString(" · "),
                    color = TextMuted,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            AnimatedVisibility(visible = expanded, enter = expandVertically(), exit = shrinkVertically()) {
                SelectionContainer {
                    if (markdown) {
                        MarkdownText(markdown = body, color = TextPrimary, accent = accent, modifier = Modifier.padding(top = 6.dp))
                    } else {
                        Text(
                            body,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
                            lineHeight = 17.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkingIndicator(run: AgentRunState) {
    val active = run.nodes.firstOrNull {
        it.status == AgentNodeStatus.THINKING || it.status == AgentNodeStatus.USING_TOOL
    }
    Row(Modifier.padding(start = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            strokeWidth = 2.dp,
            color = active?.let { Color(it.agent.color) } ?: Accent
        )
        Spacer(Modifier.width(10.dp))
        Text(
            active?.let { "${it.agent.name}: ${it.activity}" } ?: "Preparando…",
            color = TextSecondary,
            fontSize = 13.sp
        )
    }
}

@Composable
private fun ResultCard(answer: String, animate: Boolean, onSendToChat: () -> Unit, onNew: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    // Efecto máquina de escribir: el resultado "aparece" en ~2 s, no de golpe.
    var visibleChars by remember(answer) { mutableIntStateOf(if (animate) 0 else answer.length) }
    LaunchedEffect(answer) {
        if (!animate) return@LaunchedEffect
        val step = (answer.length / 90).coerceAtLeast(4)
        while (visibleChars < answer.length) {
            visibleChars = (visibleChars + step).coerceAtMost(answer.length)
            delay(22)
        }
    }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1_500)
            copied = false
        }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(Brush.linearGradient(NexusTokens.Gradient.AzelAI))
            .padding(1.5.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(BgDark)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Resultado", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Spacer(Modifier.height(10.dp))
            SelectionContainer {
                MarkdownText(
                    markdown = answer.take(visibleChars),
                    color = TextPrimary,
                    accent = Accent
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AssistChip(
                    onClick = {
                        clipboard.setText(AnnotatedString(answer))
                        copied = true
                    },
                    label = { Text(if (copied) "Copiado" else "Copiar") },
                    leadingIcon = { Icon(if (copied) Icons.Default.Check else Icons.Default.ContentCopy, null, Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, answer)
                        }
                        context.startActivity(Intent.createChooser(share, "Compartir resultado"))
                    },
                    label = { Text("Compartir") },
                    leadingIcon = { Icon(Icons.Default.Share, null, Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onSendToChat,
                    label = { Text("Seguir en el chat") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(16.dp)) }
                )
                AssistChip(
                    onClick = onNew,
                    label = { Text("Nueva tarea") },
                    leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) }
                )
            }
        }
    }
}

@Composable
private fun StatusMessage(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    title: String,
    text: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onSecondary: () -> Unit
) {
    Surface(
        color = color.copy(alpha = 0.10f),
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = color)
                Spacer(Modifier.width(8.dp))
                Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(6.dp))
            Text(text, color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onPrimary,
                    colors = ButtonDefaults.buttonColors(containerColor = NexusTokens.Color.Primary),
                    shape = RoundedCornerShape(NexusTokens.Radius.pill)
                ) { Text(primaryLabel) }
                TextButton(onClick = onSecondary) { Text("Nueva tarea", color = TextSecondary) }
            }
        }
    }
}

private fun wordCount(text: String): Int = text.split(Regex("\\s+")).count { it.isNotBlank() }

private fun formatElapsed(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0)
    return if (s >= 60) "${s / 60}:${(s % 60).toString().padStart(2, '0')} min" else "$s s"
}
