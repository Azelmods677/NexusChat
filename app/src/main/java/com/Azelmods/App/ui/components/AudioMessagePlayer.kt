package com.Azelmods.App.ui.components

import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.IOException

@Composable
fun AudioMessagePlayer(
    audioUrl: String,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0) }
    var duration by remember { mutableStateOf(0) }
    var isError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    
    val mediaPlayer = remember(audioUrl) {
        MediaPlayer().apply {
            try {
                setDataSource(audioUrl)
                setOnPreparedListener { mp ->
                    duration = mp.duration
                    isLoading = false
                }
                setOnCompletionListener {
                    isPlaying = false
                    currentPosition = 0
                }
                setOnErrorListener { _, _, _ ->
                    isError = true
                    isLoading = false
                    true
                }
                prepareAsync()
            } catch (e: IOException) {
                isError = true
                isLoading = false
            }
        }
    }
    
    // Update progress while playing
    LaunchedEffect(isPlaying) {
        while (isPlaying && isActive) {
            currentPosition = mediaPlayer.currentPosition
            delay(100)
        }
    }
    
    // Cleanup
    DisposableEffect(audioUrl) {
        onDispose {
            try {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.stop()
                }
                mediaPlayer.release()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
    
    // Animated glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
    ) {
        // Glow background when playing
        if (isPlaying) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        androidx.compose.ui.graphics.Brush.horizontalGradient(
                            listOf(
                                accentColor.copy(alpha = glowAlpha * 0.3f),
                                accentColor.copy(alpha = glowAlpha * 0.1f)
                            )
                        ),
                        RoundedCornerShape(20.dp)
                    )
            )
        }
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    androidx.compose.ui.graphics.Brush.horizontalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.25f),
                            Color.Black.copy(alpha = 0.15f)
                        )
                    ),
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Play/Pause button with modern design
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer ring animation when playing
                if (isPlaying) {
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = EaseInOutSine),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "ring_scale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .scale(scale)
                            .background(
                                accentColor.copy(alpha = 0.2f),
                                CircleShape
                            )
                    )
                }
                
                // Main button
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.8f) + fadeIn()) togetherWith
                        (scaleOut(targetScale = 0.8f) + fadeOut())
                    },
                    label = "play_pause"
                ) { playing ->
                    IconButton(
                        onClick = {
                            if (isError || isLoading) return@IconButton
                            
                            if (playing) {
                                mediaPlayer.pause()
                                isPlaying = false
                            } else {
                                if (currentPosition >= duration && duration > 0) {
                                    mediaPlayer.seekTo(0)
                                    currentPosition = 0
                                }
                                mediaPlayer.start()
                                isPlaying = true
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                androidx.compose.ui.graphics.Brush.radialGradient(
                                    listOf(
                                        accentColor.copy(alpha = 0.9f),
                                        accentColor.copy(alpha = 0.7f)
                                    )
                                ),
                                CircleShape
                            )
                    ) {
                        Icon(
                            imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (playing) "Pause" else "Play",
                            tint = if (isError) Color(0xFFFF6B6B) else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                // Waveform visualization
                if (!isError && !isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(32.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(30) { index ->
                            val baseHeight = (8 + (index % 5) * 4).dp
                            val animatedHeight = if (isPlaying) {
                                val anim = rememberInfiniteTransition(label = "wave$index")
                                val height by anim.animateValue(
                                    initialValue = baseHeight,
                                    targetValue = baseHeight * 1.8f,
                                    typeConverter = Dp.VectorConverter,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(
                                            durationMillis = 300 + (index * 30),
                                            easing = EaseInOutSine
                                        ),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "height"
                                )
                                height
                            } else {
                                baseHeight
                            }
                            
                            val progress = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                            val barProgress = index.toFloat() / 30f
                            val isPassed = barProgress <= progress
                            
                            Box(
                                modifier = Modifier
                                    .width(2.dp)
                                    .height(animatedHeight)
                                    .background(
                                        if (isPassed) accentColor else Color.White.copy(alpha = 0.3f),
                                        RoundedCornerShape(1.dp)
                                    )
                            )
                        }
                    }
                } else {
                    // Loading or error state
                    Text(
                        text = if (isError) "❌ Error al cargar audio" 
                               else if (isLoading) "⏳ Cargando..." 
                               else "🎵 Mensaje de voz",
                        color = if (isError) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                // Time display with modern styling
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatDuration(currentPosition),
                        color = accentColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Duration badge
                    Box(
                        modifier = Modifier
                            .background(
                                Color.White.copy(alpha = 0.15f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = formatDuration(duration),
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

private fun formatDuration(milliseconds: Int): String {
    val seconds = milliseconds / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}
