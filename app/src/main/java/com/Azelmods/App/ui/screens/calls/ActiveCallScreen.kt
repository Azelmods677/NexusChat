package com.Azelmods.App.ui.screens.calls

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ActiveCallScreen(
    navController: NavController,
    callId: String
) {
    var callDuration by remember { mutableStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isVideoOn by remember { mutableStateOf(true) }
    var controlsVisible by remember { mutableStateOf(true) }
    
    // Call timer
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            callDuration++
        }
    }
    
    // Auto-hide controls
    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(4000)
            controlsVisible = false
        }
    }
    
    // Waveform animation
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val wave1Height by infiniteTransition.animateFloat(
        initialValue = 20f,
        targetValue = 60f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wave1"
    )
    
    val wave2Height by infiniteTransition.animateFloat(
        initialValue = 30f,
        targetValue = 50f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(100)
        ),
        label = "wave2"
    )
    
    val wave3Height by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 70f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(200)
        ),
        label = "wave3"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable { controlsVisible = !controlsVisible }
    ) {
        // Blurred background (placeholder)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Purple.copy(alpha = 0.3f))
                .blur(20.dp)
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top section - caller info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.height(48.dp))
                
                // Avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(Purple),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        color = Color.White,
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "John Doe",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Call timer
                Text(
                    text = formatDuration(callDuration),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 18.sp
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Audio waveform
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(5) { index ->
                        val height = when (index) {
                            0, 4 -> wave1Height
                            1, 3 -> wave2Height
                            else -> wave3Height
                        }
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(height.dp)
                                .background(Teal, CircleShape)
                        )
                    }
                }
            }
            
            // Controls
            if (controlsVisible) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Control grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallControl(
                            icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                            label = "Mute",
                            isActive = isMuted,
                            onClick = { isMuted = !isMuted }
                        )
                        
                        CallControl(
                            icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                            label = "Speaker",
                            isActive = isSpeakerOn,
                            onClick = { isSpeakerOn = !isSpeakerOn }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        CallControl(
                            icon = if (isVideoOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = "Video",
                            isActive = !isVideoOn,
                            onClick = { isVideoOn = !isVideoOn }
                        )
                        
                        CallControl(
                            icon = Icons.Default.FlipCameraAndroid,
                            label = "Flip",
                            isActive = false,
                            onClick = { }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // End call button
                    FloatingActionButton(
                        onClick = { navController.popBackStack() },
                        containerColor = Color.Red,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "End Call",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun CallControl(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    if (isActive) Color.Red.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = if (isActive) Color.Red else Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
