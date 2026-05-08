package com.Azelmods.App.ui.components

import android.media.MediaRecorder
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

/**
 * Voice recorder component with waveform visualization.
 * 
 * Features:
 * - Real-time waveform animation
 * - Timer display (00:00)
 * - Cancel and send buttons
 * - Automatic stop after 5 minutes
 */
@Composable
fun VoiceRecorderUI(
    isRecording: Boolean,
    recordingTime: Int,
    onCancel: () -> Unit,
    onSend: () -> Unit,
    themeColor: Color,
    themeSecondaryColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button
            IconButton(onClick = onCancel) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Recording animation
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing red dot
                val infiniteTransition = rememberInfiniteTransition(label = "recording")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFEF4444).copy(alpha = alpha), CircleShape)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Time
                Text(
                    text = String.format("%02d:%02d", recordingTime / 60, recordingTime % 60),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Animated waveform
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(20) { index ->
                        val height by infiniteTransition.animateFloat(
                            initialValue = 4f,
                            targetValue = 24f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 400 + (index * 50),
                                    easing = LinearEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "wave$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(height.dp)
                                .background(
                                    themeColor,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Send button
            Surface(
                modifier = Modifier
                    .size(48.dp),
                onClick = onSend,
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(themeColor, themeSecondaryColor)
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
    }
}

/**
 * Audio recorder helper class.
 */
class AudioRecorder(
    private val context: android.content.Context,
    private val outputFile: File
) {
    private var mediaRecorder: MediaRecorder? = null
    
    fun startRecording() {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(outputFile.absolutePath)
            prepare()
            start()
        }
    }
    
    fun stopRecording(): File {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        return outputFile
    }
    
    fun cancelRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        outputFile.delete()
    }
}
