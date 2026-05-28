package com.Azelmods.App.ui.screens.call

import android.Manifest
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.model.CallType
import com.Azelmods.App.ui.components.VideoRenderer
import com.Azelmods.App.ui.components.safeClickable
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import kotlinx.coroutines.delay
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun ActiveCallScreen(
    contactId: String,
    callType: String,
    navController: NavController,
    viewModel: CallViewModel = hiltViewModel()
) {
    // Null safety check
    if (contactId.isEmpty()) {
        LaunchedEffect(Unit) {
            navController.popBackStack()
        }
        return
    }
    
    // Permission check
    val permissions = com.google.accompanist.permissions.rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.CAMERA
        )
    )
    
    LaunchedEffect(Unit) {
        if (!permissions.allPermissionsGranted) {
            permissions.launchMultiplePermissionRequest()
        }
    }
    
    // Show permission rationale if not granted
    if (!permissions.allPermissionsGranted) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Permisos Requeridos",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Necesitamos acceso al micrófono y cámara para realizar llamadas",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = { permissions.launchMultiplePermissionRequest() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Conceder Permisos")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(onClick = { navController.popBackStack() }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        }
        return
    }
    
    val contactState by viewModel.contactProfile.collectAsState()
    
    LaunchedEffect(contactId) {
        try {
            viewModel.loadContactProfileFromCall(contactId)
            viewModel.startCall(contactId, if (callType == "video") com.Azelmods.App.data.model.CallType.VIDEO else com.Azelmods.App.data.model.CallType.AUDIO)
        } catch (e: Exception) {
            android.util.Log.e("ActiveCallScreen", "Error initializing call: ${e.message}", e)
            navController.popBackStack()
        }
    }
    
    val contact = contactState
    var callDuration by remember { mutableStateOf(0) }
    val isAudioEnabled by viewModel.isAudioEnabled.collectAsState()
    val isVideoEnabled by viewModel.isVideoEnabled.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    var controlsVisible by remember { mutableStateOf(true) }
    val view = LocalView.current
    
    // WebRTC video tracks
    val localVideoTrack by viewModel.localVideoTrack.collectAsState()
    val remoteVideoTrack by viewModel.remoteVideoTrack.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    
    // Call duration timer
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
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeClickable { controlsVisible = !controlsVisible }
    ) {
        if (callType == "audio") {
            // Audio call - show waveform
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Avatar
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = contact?.name?.take(1)?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = contact?.name ?: "Unknown",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = formatDuration(callDuration),
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                // Audio waveform
                AudioWaveform()
            }
        } else {
            // Video call - show video placeholder
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Remote video (full screen)
                if (remoteVideoTrack != null) {
                    VideoRenderer(
                        videoTrack = remoteVideoTrack,
                        modifier = Modifier.fillMaxSize(),
                        mirror = false
                    )
                } else {
                    // Placeholder while connecting
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = when (connectionState) {
                                    org.webrtc.PeerConnection.PeerConnectionState.CONNECTING -> "Connecting..."
                                    org.webrtc.PeerConnection.PeerConnectionState.CONNECTED -> "Connected"
                                    org.webrtc.PeerConnection.PeerConnectionState.FAILED -> "Connection failed"
                                    else -> "Waiting..."
                                },
                                color = Color.White,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                
                // Local PiP video (top right corner)
                if (localVideoTrack != null && isVideoEnabled) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(width = 90.dp, height = 130.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        VideoRenderer(
                            videoTrack = localVideoTrack,
                            modifier = Modifier.fillMaxSize(),
                            mirror = true
                        )
                    }
                } else {
                    // Placeholder for local video when camera is off
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(width = 90.dp, height = 130.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.VideocamOff,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "You",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
        
        // Header
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = contact?.name ?: "Unknown",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Nexus Chat ${if (callType == "video") "Video" else "Audio"} Call",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    
                    Text(
                        text = formatDuration(callDuration),
                        color = Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }
        
        // Controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Control buttons grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    CallControlButton(
                        icon = if (isAudioEnabled) Icons.Default.Mic else Icons.Default.MicOff,
                        label = "Mute",
                        isActive = !isAudioEnabled,
                        onClick = { 
                            viewModel.toggleAudio()
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    )
                    
                    CallControlButton(
                        icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.AutoMirrored.Filled.VolumeDown,
                        label = "Speaker",
                        isActive = isSpeakerOn,
                        onClick = { 
                            viewModel.toggleSpeaker()
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (callType == "video") {
                        CallControlButton(
                            icon = if (isVideoEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                            label = "Camera",
                            isActive = !isVideoEnabled,
                            onClick = { 
                                viewModel.toggleVideo()
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        )
                        
                        CallControlButton(
                            icon = Icons.Default.FlipCameraAndroid,
                            label = "Flip",
                            isActive = false,
                            onClick = { 
                                viewModel.switchCamera()
                                view.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // End call button
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .shadow(12.dp, CircleShape)
                        .safeClickable {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                            viewModel.endCall()
                            navController.navigateUp()
                        },
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.error, MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                            )
                        ),
                        contentAlignment = Alignment.Center
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
        }
    }
}

@Composable
fun CallControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.safeClickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            shape = CircleShape,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 11.sp
        )
    }
}

@Composable
fun AudioWaveform() {
    Row(
        modifier = Modifier.fillMaxWidth(0.6f),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(7) { index ->
            val randomHeight = remember { (12..40).random().dp }
            
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(randomHeight)
                    .background(
                        Brush.verticalGradient(
                            listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.primary)
                        ),
                        RoundedCornerShape(2.dp)
                    )
            )
        }
    }
}

@Composable
fun CallViewModel(
    viewModel: com.Azelmods.App.ui.screens.call.CallViewModel = hiltViewModel()
): com.Azelmods.App.ui.screens.call.CallViewModel = viewModel

private fun formatDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%02d:%02d", minutes, secs)
    }
}
