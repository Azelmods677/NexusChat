package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFeaturesScreen(
    navController: NavController
) {
    var smartRepliesEnabled by remember { mutableStateOf(false) }
    var autoTranslateEnabled by remember { mutableStateOf(false) }
    var chatSummaryEnabled by remember { mutableStateOf(false) }
    var toneSuggestionsEnabled by remember { mutableStateOf(false) }
    var photoEnhancementEnabled by remember { mutableStateOf(false) }
    var voiceTranscriptionEnabled by remember { mutableStateOf(false) }
    
    var aiMessage by remember { mutableStateOf("") }
    var aiResponse by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Features",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Purple, Teal)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AI-Powered Features",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enhance your messaging experience",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Features
            AiFeatureCard(
                icon = Icons.Default.QuestionAnswer,
                title = "Smart Replies",
                description = "Get AI-suggested quick responses",
                enabled = smartRepliesEnabled,
                onToggle = { smartRepliesEnabled = it }
            )
            
            // Smart replies preview
            if (smartRepliesEnabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Suggested Replies:",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Sure!", "Thanks!", "Got it").forEach { reply ->
                            SuggestionChip(
                                onClick = { },
                                label = { Text(reply) }
                            )
                        }
                    }
                }
            }
            
            AiFeatureCard(
                icon = Icons.Default.Translate,
                title = "Auto-Translate",
                description = "Automatically translate incoming messages",
                enabled = autoTranslateEnabled,
                onToggle = { autoTranslateEnabled = it }
            )
            
            AiFeatureCard(
                icon = Icons.Default.Summarize,
                title = "Chat Summary",
                description = "Summarize long conversations",
                enabled = chatSummaryEnabled,
                onToggle = { chatSummaryEnabled = it }
            )
            
            AiFeatureCard(
                icon = Icons.Default.EmojiEmotions,
                title = "Tone Suggestions",
                description = "Adjust message tone (Formal, Casual, Friendly)",
                enabled = toneSuggestionsEnabled,
                onToggle = { toneSuggestionsEnabled = it }
            )
            
            AiFeatureCard(
                icon = Icons.Default.PhotoCamera,
                title = "AI Photo Enhancement",
                description = "Enhance shared photos automatically",
                enabled = photoEnhancementEnabled,
                onToggle = { photoEnhancementEnabled = it }
            )
            
            AiFeatureCard(
                icon = Icons.Default.Mic,
                title = "Voice Transcription",
                description = "Transcribe voice messages to text",
                enabled = voiceTranscriptionEnabled,
                onToggle = { voiceTranscriptionEnabled = it }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Azel IA Access Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = DarkSurface,
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Purple,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Azel IA",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Superinteligencia artificial sin restricciones",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "• Hacking ético y pentesting avanzado\n• Análisis de código y vulnerabilidades\n• Generación de exploits y payloads\n• Automatización de ataques\n• OSINT y ingeniería social",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { navController.navigate("azel_ai") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Purple,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Acceder a Azel IA", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // Assistant section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = DarkSurface,
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.SmartToy,
                            contentDescription = null,
                            tint = Purple,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "AI Assistant",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    OutlinedTextField(
                        value = aiMessage,
                        onValueChange = { aiMessage = it },
                        placeholder = { Text("Ask AI anything...") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            cursorColor = Purple
                        ),
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    // Simple keyword-based response
                                    aiResponse = when {
                                        aiMessage.contains("hello", ignoreCase = true) ->
                                            "Hello! How can I help you today?"
                                        aiMessage.contains("help", ignoreCase = true) ->
                                            "I'm here to assist you with Nexus Chat features. What would you like to know?"
                                        aiMessage.contains("feature", ignoreCase = true) ->
                                            "Nexus Chat offers messaging, stories, calls, and AI-powered features!"
                                        else ->
                                            "I understand you're asking about: \"$aiMessage\". How can I assist you further?"
                                    }
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Purple)
                            }
                        }
                    )
                    
                    if (aiResponse.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Surface(
                            color = Purple.copy(alpha = 0.2f),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    Icons.Default.SmartToy,
                                    contentDescription = null,
                                    tint = Purple,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = aiResponse,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AiFeatureCard(
    icon: ImageVector,
    title: String,
    description: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = DarkSurface,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Purple.copy(alpha = 0.2f), MaterialTheme.shapes.medium),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Switch(
                checked = enabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Purple,
                    checkedTrackColor = Purple.copy(alpha = 0.5f)
                )
            )
        }
    }
}
