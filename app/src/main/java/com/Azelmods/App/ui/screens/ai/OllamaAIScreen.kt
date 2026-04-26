package com.Azelmods.App.ui.screens.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.ai.PromptCategory
import com.Azelmods.App.ui.components.safeClickable
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OllamaAIScreen(
    navController: NavController,
    viewModel: OllamaAIViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showSettings by remember { mutableStateOf(false) }
    
    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("🤖 Azel IA", color = Color.White)
                        Text(
                            text = if (state.isConnected) "🟢 Conectado" else "🔴 Desconectado",
                            fontSize = 12.sp,
                            color = if (state.isConnected) Color(0xFF10B981) else Color(0xFFEF4444)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    // Model selector
                    Box {
                        IconButton(onClick = { showSettings = !showSettings }) {
                            Icon(Icons.Default.Settings, null, tint = Color.White)
                        }
                        
                        DropdownMenu(
                            expanded = showSettings,
                            onDismissRequest = { showSettings = false },
                            modifier = Modifier.background(Color(0xFF1A1A2E))
                        ) {
                            Text(
                                "Modelo Actual: ${state.currentModel}",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                            
                            state.availableModels.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model, color = Color.White) },
                                    onClick = {
                                        viewModel.selectModel(model)
                                        showSettings = false
                                    },
                                    leadingIcon = {
                                        if (model == state.currentModel) {
                                            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                )
                            }
                            
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
                            
                            DropdownMenuItem(
                                text = { Text("Configurar Servidor", color = Color.White) },
                                onClick = {
                                    showSettings = false
                                    // TODO: Show server config dialog
                                },
                                leadingIcon = { Icon(Icons.Default.Cloud, null, tint = Color.White) }
                            )
                            
                            DropdownMenuItem(
                                text = { Text("Limpiar Chat", color = Color(0xFFEF4444)) },
                                onClick = {
                                    viewModel.clearChat()
                                    showSettings = false
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Messages list
                Box(modifier = Modifier.weight(1f)) {
                    if (state.messages.isEmpty()) {
                        // Empty state with categories
                        EmptyAIState(
                            onPromptClick = { prompt ->
                                viewModel.usePrompt(prompt)
                            },
                            onCategoryClick = { category ->
                                viewModel.selectCategory(category)
                            }
                        )
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(state.messages) { message ->
                                AIMessageBubble(message)
                            }
                            
                            // Typing indicator
                            if (state.isGenerating) {
                                item {
                                    AITypingIndicator()
                                }
                            }
                        }
                    }
                }
                
                // Input area
                AIInputArea(
                    message = state.currentInput,
                    onMessageChange = { viewModel.updateInput(it) },
                    onSend = {
                        scope.launch {
                            viewModel.sendMessage()
                            listState.animateScrollToItem(state.messages.size)
                        }
                    },
                    isGenerating = state.isGenerating,
                    isConnected = state.isConnected
                )
            }
            
            // Category prompts overlay - OUTSIDE Column, INSIDE Box
            AnimatedVisibility(
                visible = state.selectedCategory != null,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                state.selectedCategory?.let { category ->
                    CategoryPromptsSheet(
                        category = category,
                        onPromptClick = { prompt ->
                            viewModel.usePrompt(prompt)
                            viewModel.selectCategory(null)
                        },
                        onDismiss = { viewModel.selectCategory(null) }
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyAIState(
    onPromptClick: (String) -> Unit,
    onCategoryClick: (PromptCategory) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated icon
        val infiniteTransition = rememberInfiniteTransition(label = "ai_pulse")
        val scaleAnim by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000),
                repeatMode = RepeatMode.Reverse
            ),
            label = "scale"
        )
        
        Surface(
            modifier = Modifier
                .size(120.dp)
                .scale(scaleAnim),
            shape = CircleShape,
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.radialGradient(
                        listOf(
                            Color(0xFFEF4444),
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.secondary
                        )
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "🔓 Azel IA Sin Censura",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Hacking • Exploits • Programación Avanzada",
            fontSize = 14.sp,
            color = Color(0xFFEF4444),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Categorías de prompts
        Text(
            text = "CATEGORÍAS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Grid de categorías
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val categories = PromptCategory.values().toList()
            categories.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { category ->
                        CategoryChip(
                            category = category,
                            onClick = { onCategoryClick(category) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty spaces
                    repeat(3 - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun QuickPromptChip(text: String) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp
        )
    }
}

@Composable
fun CategoryChip(
    category: PromptCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.safeClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = category.icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = category.displayName.split(" ").first(),
                fontSize = 10.sp,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}

@Composable
fun AIMessageBubble(message: AIMessage) {
    val isUser = message.role == "user"
    
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        Surface(
            modifier = Modifier.widthIn(max = 320.dp),
            shape = if (isUser) {
                RoundedCornerShape(20.dp, 20.dp, 4.dp, 20.dp)
            } else {
                RoundedCornerShape(20.dp, 20.dp, 20.dp, 4.dp)
            },
            color = if (isUser) MaterialTheme.colorScheme.primary else Color(0xFF1A1A2E),
            shadowElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(14.dp)
            ) {
                // Role indicator
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Azel IA",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                // Message content - selectable
                SelectionContainer {
                    Text(
                        text = message.content,
                        color = Color.White,
                        fontSize = 15.sp,
                        lineHeight = 20.sp
                    )
                }
                
                // Timestamp
                Text(
                    text = message.timestamp,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AITypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF1A1A2E)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "offset"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(y = offsetY.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun AIInputArea(
    message: String,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit,
    isGenerating: Boolean,
    isConnected: Boolean
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1A2E),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            OutlinedTextField(
                value = message,
                onValueChange = onMessageChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (isConnected) "Pregunta sin censura..." else "Servidor desconectado",
                        color = Color.Gray
                    )
                },
                enabled = isConnected && !isGenerating,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    disabledTextColor = Color.Gray,
                    focusedContainerColor = Color(0xFF2D2D44),
                    unfocusedContainerColor = Color(0xFF2D2D44),
                    disabledContainerColor = Color(0xFF2D2D44),
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = Color.Transparent
                ),
                maxLines = 5
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Send button
            AnimatedVisibility(
                visible = message.isNotEmpty() && !isGenerating && isConnected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .size(48.dp)
                        .safeClickable(onClick = onSend),
                    shape = CircleShape,
                    color = Color.Transparent,
                    shadowElevation = 8.dp
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            // Loading indicator
            if (isGenerating) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp
                    )
                }
            }
        }
    }
}

// Data classes
data class AIMessage(
    val role: String,
    val content: String,
    val timestamp: String
)

@Composable
fun CategoryPromptsSheet(
    category: PromptCategory,
    onPromptClick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F0F1A).copy(alpha = 0.95f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = category.icon,
                        fontSize = 32.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = category.displayName,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${com.Azelmods.App.data.ai.UncensoredPrompts.getPromptsByCategory(category).size} prompts disponibles",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, null, tint = Color.White)
                }
            }
            
            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f))
            
            // Prompts list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(com.Azelmods.App.data.ai.UncensoredPrompts.getPromptsByCategory(category)) { prompt ->
                    PromptCard(
                        prompt = prompt,
                        onClick = { onPromptClick(prompt) }
                    )
                }
            }
        }
    }
}

@Composable
fun PromptCard(
    prompt: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = prompt,
                color = Color.White,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
