package com.Azelmods.App.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Azelmods.App.ui.theme.GoldPremium
import com.Azelmods.App.ui.theme.EmeraldGreen
import com.Azelmods.App.ui.theme.ErrorRed
import com.Azelmods.App.ui.theme.DarkElevated
import com.Azelmods.App.ui.theme.DarkBubbleLight
import com.Azelmods.App.ui.theme.DarkBubbleDeep
import com.Azelmods.App.ui.theme.BlueAccent

/**
 * Modern Message Input Bar - Professional, animated, and unique design
 * 
 * Features:
 * - Single emoji picker button (functional)
 * - Sticker support
 * - Voice recording
 * - Attachment menu
 * - Smooth animations
 * - Professional gradient design
 * - Telegram/WhatsApp inspired but unique
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernMessageInputBar(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendMessage: () -> Unit,
    onEmojiClick: () -> Unit,
    onStickerClick: () -> Unit,
    onAttachmentClick: () -> Unit,
    onVoiceRecordStart: () -> Unit,
    onVoiceRecordStop: () -> Unit,
    isRecording: Boolean = false,
    themeColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }
    
    // Animaciones
    val elevation by animateDpAsState(
        targetValue = if (isFocused) 8.dp else 2.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "elevation"
    )
    
    val borderAlpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.3f,
        animationSpec = tween(300),
        label = "border_alpha"
    )
    
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp),
        shadowElevation = elevation,
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            DarkBubbleDeep,
                            DarkBubbleLight
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    width = 1.5.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            themeColor.copy(alpha = borderAlpha * 0.6f),
                            themeColor.copy(alpha = borderAlpha * 0.3f),
                            themeColor.copy(alpha = borderAlpha * 0.6f)
                        )
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Emoji Button
                AnimatedVisibility(
                    visible = !isRecording,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    ModernIconButton(
                        icon = Icons.Default.EmojiEmotions,
                        contentDescription = "Emoji",
                        onClick = onEmojiClick,
                        tint = GoldPremium
                    )
                }
                
                // Text Input
                AnimatedVisibility(
                    visible = !isRecording,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 120.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(DarkElevated)
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        BasicTextField(
                            value = messageText,
                            onValueChange = onMessageChange,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                lineHeight = 20.sp
                            ),
                            maxLines = 5,
                            decorationBox = { innerTextField ->
                                if (messageText.isEmpty()) {
                                    Text(
                                        "Message...",
                                        color = Color.Gray.copy(alpha = 0.5f),
                                        fontSize = 15.sp
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }
                }
                
                // Recording indicator
                AnimatedVisibility(
                    visible = isRecording,
                    enter = expandHorizontally() + fadeIn(),
                    exit = shrinkHorizontally() + fadeOut()
                ) {
                    RecordingIndicator(
                        modifier = Modifier.weight(1f)
                    )
                }
                
                // Action Buttons Row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Sticker Button
                    AnimatedVisibility(
                        visible = messageText.isEmpty() && !isRecording,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        ModernIconButton(
                            icon = Icons.AutoMirrored.Filled.StickyNote2,
                            contentDescription = "Stickers",
                            onClick = onStickerClick,
                            tint = EmeraldGreen
                        )
                    }
                    
                    // Attachment Button
                    AnimatedVisibility(
                        visible = messageText.isEmpty() && !isRecording,
                        enter = scaleIn() + fadeIn(),
                        exit = scaleOut() + fadeOut()
                    ) {
                        ModernIconButton(
                            icon = Icons.Default.AttachFile,
                            contentDescription = "Attach",
                            onClick = onAttachmentClick,
                            tint = BlueAccent
                        )
                    }
                    
                    // Send or Voice Button
                    AnimatedContent(
                        targetState = messageText.isNotEmpty(),
                        label = "send_voice_button",
                        transitionSpec = {
                            (scaleIn() + fadeIn()) togetherWith (scaleOut() + fadeOut())
                        }
                    ) { hasText ->
                        if (hasText) {
                            SendButton(
                                onClick = onSendMessage,
                                themeColor = themeColor
                            )
                        } else {
                            VoiceButton(
                                isRecording = isRecording,
                                onRecordStart = onVoiceRecordStart,
                                onRecordStop = onVoiceRecordStop,
                                themeColor = themeColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModernIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.85f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "button_scale"
    )
    
    IconButton(
        onClick = {
            isPressed = true
            onClick()
        },
        modifier = modifier
            .size(40.dp)
            .scale(scale)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
    }
    
    LaunchedEffect(isPressed) {
        if (isPressed) {
            kotlinx.coroutines.delay(100)
            isPressed = false
        }
    }
}

@Composable
private fun SendButton(
    onClick: () -> Unit,
    themeColor: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "send_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Box(
        modifier = Modifier.size(44.dp),
        contentAlignment = Alignment.Center
    ) {
        // Glow effect
        Box(
            modifier = Modifier
                .size(44.dp)
                .scale(1.2f)
                .background(
                    themeColor.copy(alpha = glowAlpha * 0.3f),
                    CircleShape
                )
        )
        
        // Button
        Surface(
            onClick = onClick,
            modifier = Modifier.size(44.dp),
            shape = CircleShape,
            color = themeColor,
            shadowElevation = 4.dp
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceButton(
    isRecording: Boolean,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    themeColor: Color
) {
    val scale by animateFloatAsState(
        targetValue = if (isRecording) 1.1f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "voice_scale"
    )
    
    Surface(
        onClick = { if (isRecording) onRecordStop() else onRecordStart() },
        modifier = Modifier
            .size(44.dp)
            .scale(scale),
        shape = CircleShape,
        color = if (isRecording) ErrorRed else BlueAccent,
        shadowElevation = if (isRecording) 6.dp else 2.dp
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                contentDescription = if (isRecording) "Stop" else "Record",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun RecordingIndicator(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "recording")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    
    Row(
        modifier = modifier
            .height(40.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(DarkElevated)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(ErrorRed.copy(alpha = alpha), CircleShape)
        )
        
        Text(
            text = "Recording...",
            color = Color.White,
            fontSize = 15.sp
        )
    }
}
