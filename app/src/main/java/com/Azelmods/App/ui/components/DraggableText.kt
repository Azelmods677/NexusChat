package com.Azelmods.App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * Draggable text component for stories
 * Allows user to drag text anywhere on the screen
 */
@Composable
fun DraggableText(
    text: String,
    initialOffset: Offset = Offset.Zero,
    onPositionChange: (Offset) -> Unit = {},
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    backgroundColor: Color = Color.Black.copy(alpha = 0.6f),
    fontSize: Int = 32
) {
    var offset by remember { mutableStateOf(initialOffset) }
    
    Box(
        modifier = modifier
            .offset { IntOffset(offset.x.roundToInt(), offset.y.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offset = Offset(
                        x = (offset.x + dragAmount.x).coerceIn(
                            0f,
                            size.width.toFloat() - 200f // Approximate text width
                        ),
                        y = (offset.y + dragAmount.y).coerceIn(
                            0f,
                            size.height.toFloat() - 100f // Approximate text height
                        )
                    )
                    onPositionChange(offset)
                }
            }
            .background(backgroundColor, RoundedCornerShape(16.dp))
            .padding(20.dp)
    ) {
        Text(
            text = text,
            fontSize = fontSize.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Data class to store text overlay information
 */
data class TextOverlay(
    val text: String,
    val x: Float,
    val y: Float,
    val color: String = "#FFFFFF",
    val backgroundColor: String = "#000000",
    val fontSize: Int = 32
)
