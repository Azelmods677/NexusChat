package com.Azelmods.App.ui.screens.call

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.*
import com.Azelmods.App.utils.CallPermissionHelper
import com.Azelmods.App.utils.rememberAudioCallPermissionState
import com.Azelmods.App.utils.rememberVideoCallPermissionState
import kotlinx.coroutines.delay

/**
 * Call Screen for Audio/Video Calls
 * Handles Android 16 (API 36) permissions and foreground service requirements
 */
@Composable
fun CallScreen(
    navController: NavController,
    contactId: String,
    contactName: String,
    isVideoCall: Boolean = false
) {
    val context = LocalContext.current
    var isCallActive by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isVideoEnabled by remember { mutableStateOf(isVideoCall) }
    var showPermissionError by remember { mutableStateOf(false) }
    var callDuration by remember { mutableStateOf(0) }
    
    // Audio call permission handler
    val audioCallPermissionState = rememberAudioCallPermissionState(
        onPermissionsGranted = {
            // Start audio call
            CallPermissionHelper.startAudioCallService(context)
            isCallActive = true
        },
        onPermissionsDenied = {
            showPermissionError = true
        }
    )
    
    // Video call permission handler
    val videoCallPermissionState = rememberVideoCallPermissionState(
        onPermissionsGranted = {
            // Start video call
            CallPermissionHelper.startVideoCallService(context)
            isCallActive = true
        },
        onPermissionsDenied = {
            showPermissionError = true
        }
    )
    
    // Start call when screen opens
    LaunchedEffect(Unit) {
        if (isVideoCall) {
            videoCallPermissionState.launchPermissionRequest()
        } else {
            audioCallPermissionState.launchPermissionRequest()
        }
    }
    
    // Call duration timer (only runs when call is active)
    LaunchedEffect(isCallActive) {
        if (!isCallActive) return@LaunchedEffect
        while (true) {
            delay(1000)
            callDuration++
        }
    }
    
    // Stop service when leaving screen
    DisposableEffect(Unit) {
        onDispose {
            if (isCallActive) {
                CallPermissionHelper.stopCallService(context)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top section - Contact info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 64.dp)
            ) {
                // Avatar placeholder
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(Purple, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = contactName.firstOrNull()?.uppercase() ?: "?",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = contactName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (isCallActive) {
                        formatCallTimer(callDuration)
                    } else {
                        "Connecting..."
                    },
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
            
            // Bottom section - Call controls
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Control buttons row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Mute button
                    CallControlButton(
                        icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        label = if (isMuted) "Unmute" else "Mute",
                        backgroundColor = if (isMuted) Color.Red else DarkSurface,
                        onClick = { isMuted = !isMuted }
                    )
                    
                    // Speaker button
                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                        label = "Speaker",
                        backgroundColor = if (isSpeakerOn) Purple else DarkSurface,
                        onClick = { isSpeakerOn = !isSpeakerOn }
                    )
                    
                    // Video toggle (only for video calls)
                    if (isVideoCall) {
                        CallControlButton(
                            icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = "Video",
                            backgroundColor = if (isVideoEnabled) Purple else DarkSurface,
                            onClick = { isVideoEnabled = !isVideoEnabled }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // End call button
                FloatingActionButton(
                    onClick = {
                        CallPermissionHelper.stopCallService(context)
                        navController.popBackStack()
                    },
                    modifier = Modifier.size(72.dp),
                    containerColor = Color.Red,
                    contentColor = Color.White
                ) {
                    Icon(
                        Icons.Default.CallEnd,
                        contentDescription = "End Call",
                        modifier = Modifier.size(32.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "End Call",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }
        }
        
        // Permission error dialog
        if (showPermissionError) {
            AlertDialog(
                onDismissRequest = { 
                    showPermissionError = false
                    navController.popBackStack()
                },
                title = { Text("Permissions Required") },
                text = { 
                    Text(
                        if (isVideoCall) {
                            "Camera and microphone permissions are required for video calls."
                        } else {
                            "Microphone permission is required for audio calls."
                        }
                    )
                },
                confirmButton = {
                    TextButton(onClick = { 
                        showPermissionError = false
                        navController.popBackStack()
                    }) {
                        Text("OK")
                    }
                },
                containerColor = DarkSurface
            )
        }
    }
}

/**
 * Formats elapsed seconds into a human-readable call timer (MM:SS or HH:MM:SS).
 */
private fun formatCallTimer(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            containerColor = backgroundColor,
            contentColor = Color.White
        ) {
            Icon(
                icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}
