package com.Azelmods.App.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.Azelmods.App.data.model.MessageStatus

@Composable
fun ReadReceiptIndicator(
    status: MessageStatus,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "receipt")

    // Animate color change when status changes to READ
    val targetColor = when (status) {
        MessageStatus.SENDING -> Color.Gray.copy(alpha = 0.5f)
        MessageStatus.SENT -> Color.Gray
        MessageStatus.DELIVERED -> Color.LightGray
        MessageStatus.READ -> Color(0xFF7B5CFA) // Nexus purple
        MessageStatus.FAILED -> Color.Red
    }

    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 400, easing = EaseInOutQuart),
        label = "receipt_color"
    )

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((-5).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (status) {
            MessageStatus.SENDING -> {
                // Spinning clock icon
                val rotation by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000, easing = LinearEasing)
                    ),
                    label = "sending_rotation"
                )
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = "Sending",
                    tint = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier
                        .size(13.dp)
                        .rotate(rotation)
                )
            }

            MessageStatus.SENT -> {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(300)) + scaleIn(tween(300))
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Sent",
                        tint = animatedColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }

            MessageStatus.DELIVERED, MessageStatus.READ -> {
                // Double checkmarks with stagger
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(tween(200)) + slideInHorizontally(tween(200))
                ) {
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = if (status == MessageStatus.READ) "Read" else "Delivered",
                        tint = animatedColor,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            MessageStatus.FAILED -> {
                Icon(
                    imageVector = Icons.Default.ErrorOutline,
                    contentDescription = "Failed",
                    tint = Color.Red,
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
