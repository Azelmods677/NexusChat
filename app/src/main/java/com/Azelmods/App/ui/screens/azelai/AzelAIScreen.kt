package com.Azelmods.App.ui.screens.azelai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Azelmods.App.data.model.AIMessage
import com.Azelmods.App.ui.theme.NexusTokens
import kotlinx.coroutines.delay
import java.util.*

// ── Paleta  Premium (Onyx & Slate) ───────────────────
private val BgDark = NexusTokens.Color.BgBase       
private val SurfaceDark = NexusTokens.Color.BgSurface  
private val AzelPurple = NexusTokens.Color.PrimaryLight   
private val AzelBlue = NexusTokens.Color.Secondary     
private val TextPrimary = NexusTokens.Color.TextPrimary
private val TextSecondary = NexusTokens.Color.TextSecondary
private val BorderColor = NexusTokens.Color.GlassBorder
private val UserBubble = NexusTokens.Color.BgElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzelAIScreen(
    onBack: () -> Unit,
    onOpenSettings: () -> Unit = {},
    viewModel: AzelAIViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    // 0 = chat, 1 = agentes
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable { mutableIntStateOf(0) }

    // Al volver de Ajustes → IA, la cabecera refleja el proveedor/modelo nuevo.
    androidx.lifecycle.compose.LifecycleEventEffect(androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
        viewModel.refreshProviderStatus()
    }
    
    var showClearDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showSidebar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(state.messages.size, state.streamingContent.length / 200) {
        val total = listState.layoutInfo.totalItemsCount
        if (state.messages.isNotEmpty() && total > 0) {
            listState.animateScrollToItem(total - 1)
        }
    }
    
    LaunchedEffect(state.error) {
        if (state.error != null) {
            delay(5000)
            viewModel.clearError()
        }
    }
    
    Scaffold(
        topBar = {
            AzelAITopBar(
                onBack = onBack,
                onClear = { showClearDialog = true },
                onStats = { showStatsDialog = true },
                onToggleSidebar = { showSidebar = !showSidebar },
                currentChatTitle = if (selectedTab == 1) "Agentes de IA" else state.currentChatTitle,
                providerName = state.providerName,
                providerModel = state.providerModel,
                isConfigured = state.isProviderConfigured,
                geminiModels = if (state.providerName.contains("Gemini")) state.availableModels else emptyList(),
                onSelectModel = viewModel::selectModel,
                onOpenSettings = onOpenSettings,
                selectedTab = selectedTab,
                onSelectTab = { selectedTab = it; showSidebar = false }
            )
        },
        containerColor = BgDark,
        contentWindowInsets = WindowInsets(0) // Edge-to-Edge: control manual de insets
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).navigationBarsPadding()) {
            if (selectedTab == 1) {
                com.Azelmods.App.ui.screens.azelai.agents.AgentsPanel(
                    geminiModel = state.selectedModel,
                    onOpenSettings = onOpenSettings,
                    onSendToChat = { goal, answer ->
                        viewModel.importAgentResult(goal, answer)
                        selectedTab = 0
                    }
                )
            } else
            Row(modifier = Modifier.fillMaxSize()) {
                AnimatedVisibility(
                    visible = showSidebar,
                    enter = slideInHorizontally { -it },
                    exit = slideOutHorizontally { -it }
                ) {
                    AzelAISidebar(
                        modifier = Modifier.width(280.dp),
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onChatSelect = { viewModel.loadChat(it); showSidebar = false },
                        onNewChat = { viewModel.startNewChat(); showSidebar = false },
                        viewModel = viewModel
                    )
                }
                
                        Column(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f)) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (state.messages.isEmpty()) {
                                item { AzelAIWelcome(isConfigured = state.isProviderConfigured, onOpenSettings = onOpenSettings) }
                                item {
                                    AzelAIQuickTools(
                                        onPick = { prompt -> inputText = prompt },
                                        onOpenAgents = { selectedTab = 1 }
                                    )
                                }
                            }
                            
                            items(
                                items = state.messages,
                                key = { it.id.ifBlank { it.timestamp.toString() } }
                            ) { message ->
                                AzelAIMessageBubble(message = message)
                            }
                            
                            if (state.isStreaming && state.streamingContent.isNotEmpty()) {
                                item {
                                    AzelAIMessageBubble(
                                        message = AIMessage(
                                            id = "streaming",
                                            content = state.streamingContent,
                                            role = "assistant",
                                            timestamp = System.currentTimeMillis()
                                        )
                                    )
                                }
                            } else if (state.isThinking) {
                                item {
                                    AzelAIThinkingBubble(
                                        elapsedMs = state.thinkingElapsedMs,
                                        timeoutMs = AzelAIViewModel.FIRST_TOKEN_TIMEOUT_MS
                                    )
                                }
                            }
                        }
                        
                        this@Column.AnimatedVisibility(
                            visible = state.error != null,
                            enter = fadeIn() + slideInVertically(),
                            exit = fadeOut() + slideOutVertically(),
                            modifier = Modifier.align(Alignment.TopCenter)
                        ) {
                            ErrorBanner(
                                state.error ?: "",
                                onClose = { viewModel.clearError() },
                                onRetry = {
                                    viewModel.clearError()
                                    viewModel.retryLastMessage()
                                }
                            )
                        }
                    }
                    
                    AzelAIInputBar(
                        value = inputText,
                        onValueChange = { inputText = it },
                        isThinking = state.isThinking || state.isStreaming,
                        onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText.trim())
                                inputText = ""
                            }
                        },
                        onStop = viewModel::stopGeneration
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        AzelConfirmDialog(
            title = "Limpiar Chat",
            desc = "¿Estás seguro de eliminar todo el historial de esta conversación?",
            onConfirm = { viewModel.clearHistory(); showClearDialog = false },
            onDismiss = { showClearDialog = false }
        )
    }
    
    if (showStatsDialog) {
        AzelStatsDialog(
            stats = state.stats,
            onDismiss = { showStatsDialog = false }
        )
    }
}

@Composable
fun AzelAITopBar(
    onBack: () -> Unit,
    onClear: () -> Unit,
    onStats: () -> Unit,
    onToggleSidebar: () -> Unit,
    currentChatTitle: String,
    providerName: String,
    providerModel: String,
    isConfigured: Boolean,
    geminiModels: List<com.Azelmods.App.data.api.AIModel>,
    onSelectModel: (String) -> Unit,
    onOpenSettings: () -> Unit,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit
) {
    var showModels by remember { mutableStateOf(false) }
    Surface(
        color = BgDark,
        border = BorderStroke(0.5.dp, BorderColor)
    ) {
        Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = TextPrimary)
                }
                if (selectedTab == 0) {
                    IconButton(onClick = onToggleSidebar) {
                        Icon(Icons.Default.Menu, "Conversaciones", tint = TextSecondary)
                    }
                }

                Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(
                        text = currentChatTitle.ifEmpty { "Azel IA" },
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // Estado REAL del proveedor (antes era un "API Key Activa" fijo en
                    // verde aunque no hubiera ninguna clave). Si es Gemini, tocarlo
                    // abre el selector de modelo; si falta configuración, los ajustes.
                    Box {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    when {
                                        !isConfigured -> onOpenSettings()
                                        geminiModels.isNotEmpty() -> showModels = true
                                        else -> onOpenSettings()
                                    }
                                }
                                .padding(vertical = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(7.dp).clip(CircleShape)
                                    .background(if (isConfigured) NexusTokens.Color.Online else NexusTokens.Color.Warning)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (isConfigured) {
                                    listOf(providerName, providerModel).filter { it.isNotBlank() }.joinToString(" · ")
                                        .ifBlank { "IA lista" }
                                } else "Sin configurar — toca para añadir tu clave",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (isConfigured && geminiModels.isNotEmpty()) {
                                Icon(Icons.Default.ArrowDropDown, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                        DropdownMenu(
                            expanded = showModels,
                            onDismissRequest = { showModels = false },
                            containerColor = SurfaceDark
                        ) {
                            geminiModels.forEach { model ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text(model.id, color = TextPrimary, fontSize = 14.sp)
                                            Text(model.description, color = TextSecondary, fontSize = 11.sp)
                                        }
                                    },
                                    onClick = {
                                        onSelectModel(model.id)
                                        showModels = false
                                    },
                                    trailingIcon = {
                                        if (model.id == providerModel) Icon(Icons.Default.Check, null, tint = AzelPurple)
                                    }
                                )
                            }
                        }
                    }
                }

                if (selectedTab == 0) {
                    IconButton(onClick = onStats) { Icon(Icons.Default.Insights, "Uso", tint = AzelPurple) }
                    IconButton(onClick = onClear) { Icon(Icons.Default.DeleteOutline, "Borrar historial", tint = TextSecondary) }
                } else {
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, "Ajustes de IA", tint = TextSecondary) }
                }
            }

            // Pestañas Chat / Agentes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 10.dp)
                    .clip(RoundedCornerShape(NexusTokens.Radius.pill))
                    .background(SurfaceDark)
                    .padding(3.dp)
            ) {
                listOf(
                    Triple(0, Icons.Default.ChatBubbleOutline, "Chat"),
                    Triple(1, Icons.Default.Hub, "Agentes")
                ).forEach { (index, icon, label) ->
                    val selected = selectedTab == index
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(NexusTokens.Radius.pill))
                            .background(
                                if (selected) Brush.linearGradient(listOf(AzelPurple.copy(alpha = 0.9f), AzelBlue.copy(alpha = 0.7f)))
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { onSelectTab(index) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(icon, null, tint = if (selected) Color.White else TextSecondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            label,
                            color = if (selected) Color.White else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AzelAIWelcome(isConfigured: Boolean = true, onOpenSettings: () -> Unit = {}) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(AzelPurple, AzelBlue))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(NexusTokens.IconSize.lg))
        }
        Spacer(Modifier.height(28.dp))
        Text(
            "¿En qué puedo ayudarte?",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )
        // Texto honesto: antes prometía que la IA "evitaba cualquier filtro ético",
        // lo cual era falso (se usan los filtros por defecto del proveedor) y es
        // motivo de rechazo en Google Play.
        Text(
            "Escribe, resume, traduce, programa o pregunta lo que necesites. " +
                "Usa tu propio proveedor de IA y tus conversaciones se guardan en tu cuenta.",
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 14.dp, start = 24.dp, end = 24.dp),
            lineHeight = 22.sp
        )
        if (!isConfigured) {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = AzelPurple),
                shape = RoundedCornerShape(NexusTokens.Radius.pill)
            ) {
                Icon(Icons.Default.Key, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Configurar mi IA")
            }
        }
    }
}

@Composable
fun AzelAIQuickTools(onPick: (String) -> Unit, onOpenAgents: () -> Unit) {
    // Antes cada atajo ENVIABA al instante una frase incompleta ("Mejora el estilo
    // de este texto:" sin texto). Ahora rellena el cuadro y el usuario completa.
    val items = listOf(
        Triple("✨", "Mejorar redacción", "Mejora el estilo y la claridad de este texto:\n\n"),
        Triple("🧠", "Explicar concepto", "Explícame de forma sencilla el concepto de "),
        Triple("💻", "Revisar código", "Revisa este código, encuentra errores y propón mejoras:\n\n"),
        Triple("🌍", "Traducir", "Traduce al inglés manteniendo el tono:\n\n")
    )

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (emoji, title, prompt) ->
                    Surface(
                        modifier = Modifier.weight(1f).padding(bottom = 12.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onPick(prompt) },
                        color = Color.Transparent,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(0.5.dp, BorderColor)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(emoji, fontSize = 20.sp)
                            Spacer(Modifier.height(12.dp))
                            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
        // Acceso directo a los agentes.
        Surface(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onOpenAgents),
            color = AzelPurple.copy(alpha = 0.10f),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(0.5.dp, AzelPurple.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🤖", fontSize = 22.sp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Agentes de IA", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Investigan, calculan y se reparten el trabajo. Míralos trabajar en vivo.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = AzelPurple)
            }
        }
    }
}

@Composable
fun AzelAIMessageBubble(message: AIMessage) {
    val isUser = message.role == "user"
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier.padding(top = 4.dp, end = 12.dp).size(32.dp)
                    .clip(CircleShape).background(AzelPurple.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = AzelPurple, modifier = Modifier.size(NexusTokens.IconSize.sm))
            }
        }
        
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            color = if (isUser) UserBubble else Color.Transparent,
            shape = RoundedCornerShape(18.dp),
            border = if (!isUser) null else BorderStroke(0.5.dp, BorderColor)
        ) {
            // Los mensajes del usuario van tal cual: lo que escribió es lo que
            // debe leer. Las respuestas del modelo pasan por el renderizador de
            // markdown, que es el formato en el que llegan.
            if (isUser) {
                SelectionContainer {
                    Text(
                        text = message.content,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        lineHeight = 24.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } else {
                SelectionContainer {
                    com.Azelmods.App.ui.components.MarkdownText(
                        markdown = message.content,
                        color = TextPrimary,
                        accent = AzelPurple,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun AzelAIInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isThinking: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit = {}
) {
    // Professional gradient animation for send button
    val infiniteTransition = rememberInfiniteTransition(label = "send_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth().imePadding().navigationBarsPadding(),
        color = BgDark,
        border = BorderStroke(0.5.dp, BorderColor),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Modern text input with subtle border
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, BorderColor.copy(alpha = 0.5f))
            ) {
                TextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { 
                        Text(
                            "Escribe tu consulta...", 
                            color = TextSecondary,
                            fontSize = 15.sp
                        ) 
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AzelPurple,
                        disabledContainerColor = Color.Transparent,
                        disabledTextColor = TextPrimary
                    ),
                    maxLines = 6,
                    enabled = true, // SIEMPRE HABILITADO
                    textStyle = LocalTextStyle.current.copy(fontSize = 15.sp)
                )
            }
            
            // Professional send button with gradient and glow
            Box(
                modifier = Modifier.size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                // Glow effect when active
                if (value.isNotBlank() && !isThinking) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(
                                        AzelPurple.copy(alpha = glowAlpha * 0.4f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }
                
                // Send button
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color = Color.Transparent,
                    onClick = {
                        when {
                            isThinking -> onStop()
                            value.isNotBlank() -> onSend()
                        }
                    }
                ) {
                    Box(
                        modifier = Modifier.background(
                            if (value.isNotBlank() && !isThinking) {
                                Brush.linearGradient(
                                    listOf(AzelPurple, AzelBlue)
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(SurfaceDark, SurfaceDark)
                                )
                            }
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isThinking) {
                            // Botón "Detener": corta la respuesta y conserva lo escrito.
                            Box(contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(34.dp),
                                    color = AzelPurple,
                                    strokeWidth = 2.dp
                                )
                                Icon(
                                    Icons.Default.Stop,
                                    contentDescription = "Detener",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar",
                                tint = if (value.isNotBlank()) Color.White else TextSecondary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorBanner(error: String, onClose: () -> Unit, onRetry: (() -> Unit)? = null) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = NexusTokens.Color.Error.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, NexusTokens.Color.Error),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = NexusTokens.Color.Error)
            Text(error, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
            if (onRetry != null) {
                IconButton(onClick = onRetry) { Icon(Icons.Default.Refresh, "Reintentar", tint = Color.White.copy(0.8f)) }
            }
            IconButton(onClick = onClose) { Icon(Icons.Default.Close, null, tint = Color.White.copy(0.6f)) }
        }
    }
}

@Composable
fun AzelAIThinkingBubble(
    elapsedMs: Long = 0L,
    timeoutMs: Long = 60_000L
) {
    val progress = (elapsedMs.toFloat() / timeoutMs.toFloat()).coerceIn(0f, 1f)
    val timeoutColor = when {
        progress < 0.7f -> AzelPurple
        progress < 0.9f -> NexusTokens.Color.Away // Naranja cuando se acerca
        else -> NexusTokens.Color.Error // Rojo cuando está por expirar
    }
    val elapsedSeconds = elapsedMs / 1000
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(AzelPurple.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = AzelPurple, modifier = Modifier.size(NexusTokens.IconSize.xs))
            }
            Spacer(Modifier.width(12.dp))
            repeat(3) { i ->
                val anim = rememberInfiniteTransition(label = "dots").animateFloat(
                    0.3f, 1f, infiniteRepeatable(tween(600, i * 200), RepeatMode.Reverse), label = "alpha"
                )
                Box(Modifier.size(8.dp).clip(CircleShape).background(AzelPurple.copy(anim.value)).padding(2.dp))
                Spacer(Modifier.width(4.dp))
            }
        }
        
        // ── Barra de progreso de timeout ────────────────
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 44.dp, end = 44.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.weight(1f).height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(BorderColor.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = progress)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(AzelPurple, timeoutColor)
                            )
                        )
                )
            }
            Text(
                text = "${elapsedSeconds}s",
                color = TextSecondary.copy(alpha = 0.7f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
        if (progress >= 0.85f) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = "⚠️ La respuesta está tardando más de lo esperado...",
                color = timeoutColor.copy(alpha = 0.8f),
                fontSize = 11.sp,
                modifier = Modifier.padding(start = 44.dp)
            )
        }
    }
}

@Composable
fun AzelConfirmDialog(title: String, desc: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text(title, color = TextPrimary) },
        text = { Text(desc, color = TextSecondary) },
        confirmButton = {
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = NexusTokens.Color.Error)) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar", color = AzelPurple) }
        }
    )
}

@Composable
fun AzelStatsDialog(stats: Map<String, Any>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceDark,
        title = { Text("Uso de la IA", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                StatItem("Mensajes totales", stats["messageCount"]?.toString() ?: "0")
                StatItem("Tokens procesados", stats["totalTokens"]?.toString() ?: "0")
                StatItem("Tipo de acceso", "Directo (API Key)")
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Cerrar") } }
    )
}

@Composable
fun StatItem(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 14.sp)
        Text(value, color = AzelBlue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun AzelAISidebar(
    modifier: Modifier = Modifier,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onChatSelect: (String) -> Unit,
    onNewChat: () -> Unit,
    viewModel: AzelAIViewModel
) {
    val history by viewModel.chatHistory.collectAsState()
    Surface(modifier = modifier.fillMaxHeight(), color = BgDark, border = BorderStroke(0.5.dp, BorderColor)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Chats", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                IconButton(onClick = onNewChat) { Icon(Icons.Default.Add, null, tint = AzelPurple) }
            }
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Buscar...", color = TextSecondary) },
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AzelPurple, unfocusedBorderColor = BorderColor)
            )
            Spacer(Modifier.height(16.dp))
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(history.filter { it.title.contains(searchQuery, true) }) { chat ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable { onChatSelect(chat.id) },
                        color = SurfaceDark,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(0.5.dp, BorderColor)
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(chat.title, color = TextPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(chat.lastMessage, color = TextSecondary, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

data class ChatHistoryItem(
    val id: String,
    val title: String,
    val lastMessage: String,
    val lastActivity: Long,
    val messageCount: Int
)
