package com.Azelmods.App.ui.components

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Draggable emoji component for stories
 * Allows user to drag emojis anywhere on the screen
 */
@Composable
fun DraggableEmoji(
    emoji: String,
    initialOffset: Offset = Offset.Zero,
    onPositionChange: (Offset) -> Unit = {},
    onRemove: () -> Unit = {},
    modifier: Modifier = Modifier,
    size: Int = 64
) {
    var offset by remember { mutableStateOf(initialOffset) }
    var scale by remember { mutableStateOf(1f) }
    
    Box(
        modifier = modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset = Offset(
                        x = (offset.x + dragAmount.x).coerceIn(
                            0f,
                            this.size.width.toFloat() - 100f
                        ),
                        y = (offset.y + dragAmount.y).coerceIn(
                            0f,
                            this.size.height.toFloat() - 100f
                        )
                    )
                    onPositionChange(offset)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onRemove()
                    }
                )
            }
    ) {
        Text(
            text = emoji,
            fontSize = size.sp
        )
    }
}

/**
 * Data class to store emoji overlay information
 */
data class EmojiOverlay(
    val emoji: String,
    val x: Float,
    val y: Float,
    val size: Int = 64,
    val id: String = java.util.UUID.randomUUID().toString()
)
