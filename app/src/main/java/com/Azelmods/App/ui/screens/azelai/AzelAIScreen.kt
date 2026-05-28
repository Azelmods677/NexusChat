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
import kotlinx.coroutines.delay
import java.util.*

// ── Paleta  Premium (Onyx & Slate) ───────────────────
private val BgDark = Color(0xFF0D0F12)       
private val SurfaceDark = Color(0xFF1A1D21)  
private val AzelPurple = Color(0xFFAB7FEF)   
private val AzelBlue = Color(0xFF63B3ED)     
private val TextPrimary = Color(0xFFE2E8F0)
private val TextSecondary = Color(0xFF94A3B8)
private val BorderColor = Color(0xFF2D3748)
private val UserBubble = Color(0xFF2D3748)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzelAIScreen(
    onBack: () -> Unit,
    viewModel: AzelAIViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    
    var showClearDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    var showSidebar by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
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
                currentChatTitle = state.currentChatTitle,
                isConnected = true // SIEMPRE CONECTADO
            )
        },
        containerColor = BgDark,
        contentWindowInsets = WindowInsets(0) // Edge-to-Edge: control manual de insets
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding).navigationBarsPadding()) {
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
                                item { AzelAIWelcome() }
                                item { AzelAIQuickTools(viewModel) }
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
                                        timeoutMs = 60_000L
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
                            ErrorBanner(state.error ?: "", onClose = { viewModel.clearError() })
                        }
                    }
                    
                    AzelAIInputBar(
                        value = inputText,
                        onValueChange = { inputText = it },
                        isThinking = state.isThinking,
                        onSend = {
                            if (inputText.isNotBlank()) {
                                viewModel.sendMessage(inputText.trim())
                                inputText = ""
                            }
                        }
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
    isConnected: Boolean
) {
    Surface(
        color = BgDark,
        border = BorderStroke(0.5.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleSidebar) {
                Icon(Icons.Default.Menu, "Menu", tint = TextSecondary)
            }
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = TextPrimary)
            }
            
            Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                Text(
                    text = currentChatTitle.ifEmpty { "Azel IA" },
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(7.dp).clip(CircleShape)
                            .background(Color(0xFF48BB78)) // SIEMPRE VERDE
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "API Key Activa",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            
            IconButton(onClick = onStats) { Icon(Icons.Default.AutoAwesome, null, tint = AzelPurple) }
            IconButton(onClick = onClear) { Icon(Icons.Default.DeleteOutline, null, tint = TextSecondary) }
        }
    }
}

@Composable
fun AzelAIWelcome() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 60.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(64.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(AzelPurple, AzelBlue))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.AutoAwesome, null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "¿En qué puedo ayudarte?",
            color = TextPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            letterSpacing = (-0.5).sp
        )
        Text(
            "Azel IA esta desarollada para evitar cualquier tipo de filtro etico  por lo cual puede preguntar con confianza",
            color = TextSecondary,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 16.dp, start = 24.dp, end = 24.dp),
            lineHeight = 22.sp
        )
    }
}

@Composable
fun AzelAIQuickTools(viewModel: AzelAIViewModel) {
    val items = listOf(
        Triple("✨", "Mejorar redacción", "Mejora el estilo y la claridad de este texto:"),
        Triple("🧠", "Explicar concepto", "Explícame de forma sencilla y profunda el concepto de:"),
        Triple("💻", "Optimizar código", "Analiza y optimiza este código para mejorar su eficiencia:"),
        Triple("🛡️", "Análisis de seguridad", "Realiza un análisis exhaustivo de seguridad sobre:")
    )
    
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                rowItems.forEach { (emoji, title, prompt) ->
                    Surface(
                        modifier = Modifier.weight(1f).padding(bottom = 12.dp)
                            .clickable { viewModel.sendMessage(prompt) },
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
                Icon(Icons.Outlined.AutoAwesome, null, tint = AzelPurple, modifier = Modifier.size(18.dp))
            }
        }
        
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            color = if (isUser) UserBubble else Color.Transparent,
            shape = RoundedCornerShape(18.dp),
            border = if (!isUser) null else BorderStroke(0.5.dp, BorderColor)
        ) {
            SelectionContainer {
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    fontFamily = if (message.content.contains("```")) FontFamily.Monospace else FontFamily.Default,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    }
}

@Composable
fun AzelAIInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isThinking: Boolean,
    onSend: () -> Unit
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
                        if (value.isNotBlank() && !isThinking) {
                            onSend()
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
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.5.dp
                            )
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
fun ErrorBanner(error: String, onClose: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        color = Color(0xFF2D1212),
        border = BorderStroke(1.dp, Color(0xFFF56565)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, null, tint = Color(0xFFF56565))
            Text(error, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f).padding(horizontal = 8.dp))
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
        progress < 0.9f -> Color(0xFFED8936) // Naranja cuando se acerca
        else -> Color(0xFFF56565) // Rojo cuando está por expirar
    }
    val elapsedSeconds = elapsedMs / 1000
    
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(AzelPurple.copy(0.1f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.AutoAwesome, null, tint = AzelPurple, modifier = Modifier.size(16.dp))
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
            Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF56565))) {
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
