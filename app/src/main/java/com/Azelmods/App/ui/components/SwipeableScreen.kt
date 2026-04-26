package com.Azelmods.App.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/**
 * 📱 SWIPEABLE SCREEN COMPONENT
 * Permite navegación con gestos de deslizamiento horizontal
 * Similar a WhatsApp/Telegram para mejor UX móvil
 */
@Composable
fun SwipeableScreen(
    onSwipeLeft: (() -> Unit)? = null,
    onSwipeRight: (() -> Unit)? = null,
    swipeThreshold: Float = 100f,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var offsetX by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    
    // Animated offset for smooth return
    val animatedOffsetX by animateFloatAsState(
        targetValue = if (isDragging) offsetX else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "offset"
    )
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                translationX = animatedOffsetX
                // Add slight scale effect for better feedback
                val scale = 1f - (abs(animatedOffsetX) / 3000f)
                scaleX = scale.coerceIn(0.95f, 1f)
                scaleY = scale.coerceIn(0.95f, 1f)
            }
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        detectHorizontalDragGestures(
                            onDragStart = {
                                isDragging = true
                            },
                            onDragEnd = {
                                isDragging = false
                                
                                // Check if swipe threshold was reached
                                if (abs(offsetX) > swipeThreshold) {
                                    if (offsetX > 0 && onSwipeRight != null) {
                                        onSwipeRight()
                                    } else if (offsetX < 0 && onSwipeLeft != null) {
                                        onSwipeLeft()
                                    }
                                }
                                
                                offsetX = 0f
                            },
                            onDragCancel = {
                                isDragging = false
                                offsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                
                                // Limit drag distance
                                val maxDrag = with(density) { 150.dp.toPx() }
                                offsetX = (offsetX + dragAmount).coerceIn(-maxDrag, maxDrag)
                            }
                        )
                    }
                } else {
                    Modifier
                }
            )
    ) {
        content()
    }
}

/**
 * 🔄 SWIPE INDICATOR
 * Visual feedback for swipe gestures
 */
@Composable
fun SwipeIndicator(
    direction: SwipeDirection,
    progress: Float,
    modifier: Modifier = Modifier
) {
    // TODO: Add visual indicator (arrow, icon, etc.)
    // This can be used to show swipe hints to users
}

enum class SwipeDirection {
    LEFT, RIGHT, NONE
}
