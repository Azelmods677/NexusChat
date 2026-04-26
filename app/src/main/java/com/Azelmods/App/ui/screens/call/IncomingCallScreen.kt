package com.Azelmods.App.ui.screens.call

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.model.CallType
import com.Azelmods.App.ui.components.safeClickable
import kotlinx.coroutines.delay

@Composable
fun IncomingCallScreen(
    contactId: String,
    callType: String, // "audio" or "video"
    navController: NavController,
    viewModel: CallViewModel = hiltViewModel()
) {
    val contactState by viewModel.contactProfile.collectAsState()
    
    LaunchedEffect(contactId) {
        viewModel.loadContactProfile(contactId)
    }
    
    val contact = contactState
    val view = LocalView.current
    
    // Haptic feedback loop
    LaunchedEffect(Unit) {
        while (true) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
            delay(2000)
        }
    }
    
    // Animated gradient background
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "angle"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(
                        x = 1000f * kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat(),
                        y = 1000f * kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat()
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // Call type text
            Text(
                text = if (callType == "video") "Incoming Video Call" else "Incoming Audio Call",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Contact name
            Text(
                text = contact?.name ?: "Unknown",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Animated avatar with rings
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                // Three expanding rings
                repeat(3) { index ->
                    val ringTransition = rememberInfiniteTransition(label = "ring$index")
                    val scale by ringTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, delayMillis = index * 200),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "scale"
                    )
                    val alpha by ringTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, delayMillis = index * 200),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "alpha"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale)
                            .alpha(alpha)
                            .background(
                                Brush.linearGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                                ),
                                CircleShape
                            )
                    )
                }
                
                // Pulsing avatar
                val pulseTransition = rememberInfiniteTransition(label = "pulse")
                val pulseScale by pulseTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.06f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulse"
                )
                
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(pulseScale),
                    shape = CircleShape,
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = Modifier.background(
                            Brush.linearGradient(
                                listOf(
                                    Color(contact?.name?.hashCode() ?: 0 or 0xFF000000.toInt()),
                                    Color((contact?.name?.hashCode() ?: 0 shl 8) or 0xFF000000.toInt())
                                )
                            )
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = contact?.name?.take(1)?.uppercase() ?: "?",
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Side actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SideActionButton(
                    icon = Icons.AutoMirrored.Filled.Message,
                    label = "Message",
                    onClick = { }
                )
                
                SideActionButton(
                    icon = Icons.Default.Alarm,
                    label = "Remind me",
                    onClick = { }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Main action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Decline button
                val declineScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "decline"
                )
                
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(declineScale)
                        .safeClickable {
                            viewModel.endCall()
                            navController.navigateUp()
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.error
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CallEnd,
                            contentDescription = "Decline",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                
                // Accept button
                val acceptScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "accept"
                )
                
                Surface(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(acceptScale)
                        .safeClickable {
                            view.performHapticFeedback(android.view.HapticFeedbackConstants.CONFIRM)
                            try {
                                // Accept call with WebRTC
                                val callId = "call_${contactId}_${System.currentTimeMillis()}"
                                val type = if (callType == "video") CallType.VIDEO else CallType.AUDIO
                                viewModel.acceptCall(callId, type)
                                
                                navController.navigate("active_call/$contactId/$callType") {
                                    popUpTo("incoming_call/$contactId/$callType") { inclusive = true }
                                }
                            } catch (e: Exception) { }
                        },
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Accept",
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
fun SideActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.safeClickable(onClick = onClick)
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}
