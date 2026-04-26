package com.Azelmods.App.ui.screens.azelai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Azelmods.App.data.model.AIMessage
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── Colores temáticos Azel IA ─────────────────────────
private val AzelPurple = Color(0xFF7C3AED)
private val AzelPurpleLight = Color(0xFFEDE9FE)
private val AzelPurpleDark = Color(0xFF4C1D95)
private val AzelGradient = listOf(Color(0xFF0F0F1A), Color(0xFF1A1A2E), Color(0xFF2D2A52))
private val BubbleAI = Color(0xFF1E1B4B)
private val BubbleUser = Color(0xFF7C3AED)
private val TextOnDark = Color(0xFFF5F3FF)
private val TextOnPurple = Color.White
private val CodeBg = Color(0xFF0D1117)
private val CodeBorder = Color(0xFF30363D)

// ─────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AzelAIScreen(
    onBack: () -> Unit,
    viewModel: AzelAIViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showClearDialog by remember { mutableStateOf(false) }
    var showStatsDialog by remember { mutableStateOf(false) }
    
    // Auto-scroll al nuevo mensaje
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.lastIndex)
        }
    }
    
    // Limpiar error automáticamente
    LaunchedEffect(state.error) {
        if (state.error != null) {
            kotlinx.coroutines.delay(5000)
            viewModel.clearError()
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(AzelGradient))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            
            // ── TopBar ──────────────────────────────────
            AzelAITopBar(
                onBack = onBack,
                onClear = { showClearDialog = true },
                onStats = { showStatsDialog = true },
                stats = state.stats
            )
            
            // ── Lista de mensajes ────────────────────────
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(
                    start = 12.dp, end = 12.dp,
                    top = 8.dp, bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pantalla bienvenida si no hay mensajes
                if (state.messages.isEmpty()) {
                    item { AzelAIWelcome() }
                }
                
                items(
                    items = state.messages,
                    key = { it.id.ifBlank { it.timestamp.toString() } }
                ) { message ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(initialOffsetY = { 40 }) + fadeIn()
                    ) {
                        AzelAIMessageBubble(message = message)
                    }
                }
                
                // Indicador "pensando"
                if (state.isThinking) {
                    item { AzelAIThinkingBubble() }
                }
            }
            
            // ── Error banner ─────────────────────────────
            AnimatedVisibility(visible = state.error != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = Color(0xFFEF4444),
                    onClick = { viewModel.clearError() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = state.error ?: "",
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.Close, null, tint = Color.White, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            // ── Input bar ───────────────────────────────
            AzelAIInputBar(
                value = inputText,
                onValueChange = { inputText = it },
                isThinking = state.isThinking,
                onSend = {
                    val msg = inputText.trim()
                    if (msg.isNotEmpty()) {
                        viewModel.sendMessage(msg)
                        inputText = ""
                        scope.launch {
                            kotlinx.coroutines.delay(300)
                            if (state.messages.isNotEmpty())
                                listState.animateScrollToItem(state.messages.lastIndex)
                        }
                    }
                },
                suggestions = listOf(
                    "🔐 Explica SQL injection",
                    "💻 Código Python para...",
                    "🌐 Cómo funciona HTTPS",
                    "🛡️ Técnicas de pentesting",
                    "🔧 Script bash para...",
                    "📡 Análisis de red con Wireshark"
                ),
                onSuggestionClick = { inputText = it },
                isConnected = true // SIEMPRE conectado - sin verificación de servidor
            )
        }
        
        // ── Diálogo limpiar historial ────────────────────
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                containerColor = Color(0xFF1E1B4B),
                title = {
                    Text("Limpiar historial", color = TextOnDark, fontWeight = FontWeight.Bold)
                },
                text = {
                    Text(
                        "¿Eliminar toda la conversación con Azel IA?\n\nEsta acción no se puede deshacer.",
                        color = TextOnDark.copy(alpha = 0.8f)
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.clearHistory()
                        showClearDialog = false
                    }) {
                        Text("Eliminar", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("Cancelar", color = AzelPurple)
                    }
                }
            )
        }
        
        // ── Diálogo estadísticas ─────────────────────────
        if (showStatsDialog) {
            AlertDialog(
                onDismissRequest = { showStatsDialog = false },
                containerColor = Color(0xFF1E1B4B),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Analytics, null, tint = AzelPurple, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Estadísticas", color = TextOnDark, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatRow("Mensajes totales", state.stats["messageCount"]?.toString() ?: "0")
                        StatRow("Tokens usados", state.stats["totalTokens"]?.toString() ?: "0")
                        val lastActivity = state.stats["lastActivity"] as? Long ?: 0L
                        if (lastActivity > 0) {
                            val date = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                                .format(Date(lastActivity))
                            StatRow("Última actividad", date)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showStatsDialog = false }) {
                        Text("Cerrar", color = AzelPurple)
                    }
                }
            )
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextOnDark.copy(alpha = 0.7f), fontSize = 14.sp)
        Text(value, color = AzelPurple, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

// ── TopBar ────────────────────────────────────────────────
@Composable
fun AzelAITopBar(
    onBack: () -> Unit,
    onClear: () -> Unit,
    onStats: () -> Unit,
    stats: Map<String, Any>
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.Black.copy(alpha = 0.4f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
            }
            
            // Avatar IA animado
            val pulse = rememberInfiniteTransition(label = "pulse")
            val scale by pulse.animateFloat(
                initialValue = 1f, targetValue = 1.08f, label = "scale",
                animationSpec = infiniteRepeatable(
                    tween(1200, easing = EaseInOut),
                    RepeatMode.Reverse
                )
            )
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFA78BFA), AzelPurple, AzelPurpleDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome, null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(Modifier.width(10.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Azel IA",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)) // Verde - siempre en línea
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "En línea • ${stats["messageCount"] ?: 0} mensajes",
                        color = Color(0xFF86EFAC),
                        fontSize = 11.sp
                    )
                }
            }
            
            IconButton(onClick = onStats) {
                Icon(Icons.Default.Analytics, null, tint = Color.White.copy(alpha = 0.7f))
            }
            
            IconButton(onClick = onClear) {
                Icon(Icons.Default.DeleteOutline, null, tint = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

// ── Pantalla bienvenida ────────────────────────────────────
@Composable
fun AzelAIWelcome() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Icono grande animado
        val rotation = rememberInfiniteTransition(label = "rot")
        val angle by rotation.animateFloat(
            0f, 360f, label = "angle",
            animationSpec = infiniteRepeatable(tween(8000, easing = LinearEasing))
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .graphicsLayer { rotationZ = angle }
                .clip(CircleShape)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            AzelPurple, Color(0xFF06B6D4),
                            Color(0xFF8B5CF6), Color(0xFFEC4899),
                            AzelPurple
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E1B4B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome, null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
        
        Spacer(Modifier.height(24.dp))
        Text(
            "Hola, soy Azel IA",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Asistente técnico avanzado sin restricciones",
            color = AzelPurple,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Experto en:\n• Hacking ético y pentesting\n• Programación avanzada\n• Seguridad informática\n• Análisis de vulnerabilidades\n• Desarrollo de exploits",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

// ── Bubble de mensaje ────────────────────────────────────────
@Composable
fun AzelAIMessageBubble(message: AIMessage) {
    val isUser = message.role == "user"
    val time = SimpleDateFormat("HH:mm", Locale.getDefault())
        .format(Date(message.timestamp))
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        // Avatar IA
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFA78BFA), AzelPurpleDark)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.AutoAwesome, null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        
        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            Surface(
                modifier = Modifier.widthIn(max = 300.dp),
                shape = RoundedCornerShape(
                    topStart = 20.dp, topEnd = 20.dp,
                    bottomStart = if (isUser) 20.dp else 6.dp,
                    bottomEnd = if (isUser) 6.dp else 20.dp
                ),
                color = if (isUser)
                    AzelPurple
                else
                    BubbleAI,
                shadowElevation = 2.dp
            ) {
                SelectionContainer {
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = message.content,
                            color = if (message.error) Color(0xFFFCA5A5) else TextOnPurple,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            fontFamily = if (message.content.contains("```")) FontFamily.Monospace else FontFamily.Default
                        )
                        
                        // Mostrar tokens si es respuesta de IA
                        if (!isUser && message.tokens > 0) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${message.tokens} tokens",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                time,
                color = Color.White.copy(alpha = 0.35f),
                fontSize = 10.sp
            )
        }
        
        if (isUser) Spacer(Modifier.width(8.dp))
    }
}

// ── Bubble "pensando" animado ────────────────────────────────
@Composable
fun AzelAIThinkingBubble() {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(Color(0xFFA78BFA), AzelPurpleDark)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.AutoAwesome, null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp),
            color = BubbleAI,
            shadowElevation = 2.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                (0..2).forEach { i ->
                    val alpha by transition.animateFloat(
                        0.3f, 1f, label = "dot$i",
                        animationSpec = infiniteRepeatable(
                            tween(600, delayMillis = i * 200, easing = EaseInOut),
                            RepeatMode.Reverse
                        )
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFA78BFA).copy(alpha = alpha))
                    )
                }
            }
        }
    }
}

// ── Input Bar ────────────────────────────────────────────────
@Composable
fun AzelAIInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isThinking: Boolean,
    onSend: () -> Unit,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit,
    isConnected: Boolean = true // Parámetro con valor por defecto
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.5f))
            .navigationBarsPadding()
    ) {
        // Chips de sugerencias
        AnimatedVisibility(visible = value.isEmpty() && !isThinking) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestions) { suggestion ->
                    SuggestionChip(
                        onClick = { onSuggestionClick(suggestion) },
                        label = {
                            Text(
                                suggestion,
                                fontSize = 12.sp,
                                maxLines = 1
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color.White.copy(alpha = 0.1f),
                            labelColor = Color.White.copy(alpha = 0.8f)
                        ),
                        border = SuggestionChipDefaults.suggestionChipBorder(
                            enabled = true,
                            borderColor = AzelPurple.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "Pregunta sobre hacking, código, seguridad...",
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 14.sp
                    )
                },
                enabled = !isThinking, // Solo deshabilitado cuando está pensando
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AzelPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = AzelPurple,
                    focusedContainerColor = Color.White.copy(alpha = 0.08f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.05f),
                    disabledContainerColor = Color.White.copy(alpha = 0.03f),
                    disabledBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                maxLines = 5
            )
            
            Spacer(Modifier.width(12.dp))
            
            // Botón enviar
            FloatingActionButton(
                onClick = onSend,
                modifier = Modifier.size(52.dp),
                containerColor = if (value.isNotBlank() && !isThinking)
                    AzelPurple
                else
                    Color.Gray.copy(0.3f),
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 4.dp,
                    pressedElevation = 8.dp
                )
            ) {
                if (isThinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
