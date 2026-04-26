package com.Azelmods.App.ui.screens.about

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.components.safeClickable
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreenRedesigned(
    navController: NavController
) {
    var visible by remember { mutableStateOf(false) }
    var countriesCount by remember { mutableStateOf(0) }
    var usersCount by remember { mutableStateOf(0) }
    var ratingCount by remember { mutableStateOf(0f) }
    val view = LocalView.current
    
    LaunchedEffect(Unit) {
        visible = true
        // Animate numbers
        repeat(150) {
            delay(10)
            countriesCount = it + 1
        }
    }
    
    LaunchedEffect(Unit) {
        delay(200)
        repeat(20) {
            delay(50)
            usersCount = (it + 1) * 100000
        }
    }
    
    LaunchedEffect(Unit) {
        delay(400)
        repeat(48) {
            delay(20)
            ratingCount = (it + 1) * 0.1f
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            
            // Animated app icon
            AnimatedAppIcon(visible = visible)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // App name with slide in animation
            AnimatedVisibility(
                visible = visible,
                enter = slideInVertically { it } + fadeIn()
            ) {
                Text(
                    text = "Nexus Chat",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Version with fade in
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 200))
            ) {
                Text(
                    text = "Version 1.0.0",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Build info
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 400))
            ) {
                Text(
                    text = "Build 100 · Made with ❤️",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(animationSpec = tween(durationMillis = 600, delayMillis = 600))
            ) {
                Text(
                    text = "A modern messenger with AI-powered features",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Stats row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    icon = "🌍",
                    value = "$countriesCount+",
                    label = "Countries"
                )
                
                StatCard(
                    icon = "👥",
                    value = "${usersCount / 1000000}M+",
                    label = "Users"
                )
                
                StatCard(
                    icon = "⭐",
                    value = String.format("%.1f", ratingCount),
                    label = "Rating"
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Links section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1A2E)
            ) {
                Column {
                    LinkItem(
                        icon = Icons.Default.Description,
                        title = "Terms of Service",
                        subtitle = "nexus-chat.com/terms",
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    )
                    
                    HorizontalDivider(color = Color(0xFF2D2D44))
                    
                    LinkItem(
                        icon = Icons.Default.PrivacyTip,
                        title = "Privacy Policy",
                        subtitle = "nexus-chat.com/privacy",
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    )
                    
                    HorizontalDivider(color = Color(0xFF2D2D44))
                    
                    LinkItem(
                        icon = Icons.Default.Code,
                        title = "Licenses",
                        subtitle = "Open source licenses",
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    )
                    
                    HorizontalDivider(color = Color(0xFF2D2D44))
                    
                    LinkItem(
                        icon = Icons.Default.VideoLibrary,
                        title = "YouTube",
                        subtitle = "Azel Mods 『𝕳𝖆𝖈𝖐𝕻𝖚𝖗𝖌𝖆𝖙𝖔𝖗𝖞』",
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    )
                    
                    HorizontalDivider(color = Color(0xFF2D2D44))
                    
                    LinkItem(
                        icon = Icons.AutoMirrored.Filled.Send,
                        title = "Telegram",
                        subtitle = "t.me/AzelModsx67779",
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    )
                    
                    HorizontalDivider(color = Color(0xFF2D2D44))
                    
                    LinkItem(
                        icon = Icons.Default.MusicNote,
                        title = "TikTok",
                        subtitle = "@azelmods",
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    )
                    
                    HorizontalDivider(color = Color(0xFF2D2D44))
                    
                    LinkItem(
                        icon = Icons.Default.Code,
                        title = "GitHub",
                        subtitle = "github.com/AzelMods677/Nexus-Chat",
                        onClick = {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun AnimatedAppIcon(visible: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "icon")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )
    
    Box(
        modifier = Modifier.offset(y = offsetY.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .size(80.dp)
                .blur(12.dp)
                .background(Color(0xFF7C3AED), RoundedCornerShape(20.dp))
        )
        
        // Icon
        Surface(
            modifier = Modifier
                .size(80.dp)
                .shadow(8.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier.background(
                    Brush.linearGradient(
                        listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
                    )
                ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Chat,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun StatCard(
    icon: String,
    value: String,
    label: String
) {
    Surface(
        modifier = Modifier.width(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = icon,
                fontSize = 32.sp
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = value,
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun LinkItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(Color(0xFF7C3AED), Color(0xFF5B21B6))
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 15.sp
                )
                Text(
                    text = subtitle,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
