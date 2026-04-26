package com.Azelmods.App.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.Azelmods.App.data.model.CallType
import com.Azelmods.App.ui.screens.call.CallViewModel
import com.Azelmods.App.ui.components.FullScreenImageViewer
import com.Azelmods.App.ui.theme.rememberThemeColor
import com.Azelmods.App.ui.theme.rememberThemeSecondaryColor
import com.google.firebase.auth.FirebaseAuth

/**
 * ProfileViewerScreen - Fullscreen profile viewer like Stories
 * 
 * Features:
 * - Fullscreen immersive view
 * - Swipe down to close
 * - Tap left/right to navigate (if viewing multiple profiles)
 * - Beautiful gradient backgrounds
 * - Quick actions (message, call, video)
 */
@Composable
fun ProfileViewerScreen(
    navController: NavController,
    userId: String,
    viewModel: ProfileViewModel = hiltViewModel(),
    callViewModel: CallViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isOwnProfile = userId == currentUserId
    val themeColor = rememberThemeColor()
    val themeSecondaryColor = rememberThemeSecondaryColor()
    
    var showFullscreenImage by remember { mutableStateOf(false) }
    
    val userState by viewModel.userProfile.collectAsState()
    
    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) {
            viewModel.loadUserProfile(userId)
        }
    }
    
    val user = userState
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { offset ->
                        // Tap anywhere to close
                        navController.popBackStack()
                    }
                )
            }
    ) {
        // Background gradient
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF1A1A2E),
                            Color(0xFF0F0F1A),
                            themeColor.copy(alpha = 0.3f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                    )
                )
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Avatar
            AnimatedFullscreenAvatar(
                name = user?.name ?: "?",
                photoUrl = user?.photoUrl,
                themeColor = themeColor,
                themeSecondaryColor = themeSecondaryColor,
                onClick = {
                    if (!user?.photoUrl.isNullOrBlank()) {
                        showFullscreenImage = true
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // User info
            Text(
                text = user?.name ?: "Unknown",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = user?.username ?: "@unknown",
                color = Color(0xFF00BFA6),
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Bio
            if (!user?.bio.isNullOrBlank()) {
                Text(
                    text = user?.bio ?: "",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Online status
            Surface(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                color = if (user?.isOnline == true) Color(0xFF10B981) else Color.Gray.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (user?.isOnline == true) {
                        val infiniteTransition = rememberInfiniteTransition(label = "online")
                        val scale by infiniteTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.3f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .scale(scale)
                        )
                    }
                    
                    Text(
                        text = if (user?.isOnline == true) "Online" else "Last seen recently",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Quick actions
            if (!isOwnProfile) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally)
                ) {
                    // Message
                    FloatingActionButton(
                        onClick = {
                            try {
                                navController.navigate("chat/$userId") {
                                    popUpTo("profile_viewer/$userId") { inclusive = true }
                                }
                            } catch (e: Exception) { }
                        },
                        containerColor = themeColor,
                        contentColor = Color.White
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Chat, "Message", modifier = Modifier.size(24.dp))
                    }
                    
                    // Call
                    FloatingActionButton(
                        onClick = {
                            try {
                                callViewModel.startCall(userId, CallType.AUDIO)
                                navController.navigate("active_call/$userId/audio")
                            } catch (e: Exception) { }
                        },
                        containerColor = Color(0xFF00BFA6),
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Phone, "Call", modifier = Modifier.size(24.dp))
                    }
                    
                    // Video
                    FloatingActionButton(
                        onClick = {
                            try {
                                callViewModel.startCall(userId, CallType.VIDEO)
                                navController.navigate("active_call/$userId/video")
                            } catch (e: Exception) { }
                        },
                        containerColor = Color(0xFFFF6B9D),
                        contentColor = Color.White
                    ) {
                        Icon(Icons.Default.Videocam, "Video", modifier = Modifier.size(24.dp))
                    }
                }
            } else {
                // Own profile - show edit button
                Button(
                    onClick = {
                        navController.popBackStack()
                        navController.navigate("profile/$userId")
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp)
                ) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile", fontSize = 16.sp)
                }
            }
        }
        
        // Close button at top
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }
        }
    }
    
    // Fullscreen image viewer
    if (showFullscreenImage && !user?.photoUrl.isNullOrBlank()) {
        FullScreenImageViewer(
            imageUrl = user?.photoUrl ?: "",
            senderName = user?.name ?: "Profile Photo",
            timestamp = "",
            onDismiss = { showFullscreenImage = false }
        )
    }
}

@Composable
fun AnimatedFullscreenAvatar(
    name: String,
    photoUrl: String?,
    themeColor: Color = Color(0xFF7C3AED),
    themeSecondaryColor: Color = Color(0xFF00BFA6),
    onClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.scale(scale)
    ) {
        // Outer rotating rainbow ring
        Box(
            modifier = Modifier
                .size(180.dp)
                .rotate(rotation)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF0000),
                            Color(0xFFFF7F00),
                            Color(0xFFFFFF00),
                            Color(0xFF00FF00),
                            Color(0xFF0000FF),
                            Color(0xFF4B0082),
                            Color(0xFF9400D3),
                            Color(0xFFFF0000)
                        )
                    ),
                    CircleShape
                )
        )
        
        // Middle ring
        Box(
            modifier = Modifier
                .size(170.dp)
                .background(Color.Black, CircleShape)
        )
        
        // Inner gradient ring
        Box(
            modifier = Modifier
                .size(165.dp)
                .background(
                    Brush.linearGradient(
                        listOf(
                            themeColor,
                            themeSecondaryColor
                        )
                    ),
                    CircleShape
                )
        )
        
        // Avatar
        Surface(
            modifier = Modifier
                .size(155.dp)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onClick() }
                    )
                },
            shape = CircleShape,
            color = themeColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (photoUrl != null && photoUrl.isNotBlank()) {
                    Image(
                        painter = rememberAsyncImagePainter(photoUrl),
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
