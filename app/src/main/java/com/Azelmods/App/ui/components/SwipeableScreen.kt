package com.Azelmods.App.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

    // El gesto vive en el Box exterior y la traslación en uno interior:
    // si los indicadores compartieran el layer trasladado, se moverían
    // junto con el contenido en lugar de quedar fijos al borde.
    Box(
        modifier = Modifier
            .fillMaxSize()
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
        ) {
            content()
        }

        // Feedback visual del gesto: crece y se opaca a medida que el arrastre
        // se acerca al umbral, para que el usuario sepa cuándo va a disparar la acción.
        val progress = (abs(offsetX) / swipeThreshold).coerceIn(0f, 1f)
        when {
            isDragging && offsetX > 0 && onSwipeRight != null -> SwipeIndicator(
                direction = SwipeDirection.RIGHT,
                progress = progress,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 24.dp)
            )
            isDragging && offsetX < 0 && onSwipeLeft != null -> SwipeIndicator(
                direction = SwipeDirection.LEFT,
                progress = progress,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 24.dp)
            )
        }
    }
}

/**
 * 🔄 SWIPE INDICATOR
 * Visual feedback for swipe gestures
 *
 * BUG ORIGINAL: esta función era un cuerpo vacío con un TODO — se podía llamar
 * desde cualquier pantalla y no dibujaba nada. Ahora renderiza una flecha en
 * círculo cuya opacidad/escala siguen el progreso del gesto (0f..1f), y
 * SwipeableScreen la usa de verdad durante el arrastre.
 */
@Composable
fun SwipeIndicator(
    direction: SwipeDirection,
    progress: Float,
    modifier: Modifier = Modifier
) {
    if (direction == SwipeDirection.NONE) return

    val clamped = progress.coerceIn(0f, 1f)
    Surface(
        modifier = modifier
            .size(48.dp)
            .graphicsLayer {
                alpha = 0.3f + 0.7f * clamped
                val scale = 0.6f + 0.4f * clamped
                scaleX = scale
                scaleY = scale
            },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = if (direction == SwipeDirection.RIGHT) {
                    Icons.AutoMirrored.Filled.ArrowForward
                } else {
                    Icons.AutoMirrored.Filled.ArrowBack
                },
                contentDescription = null,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

enum class SwipeDirection {
    LEFT, RIGHT, NONE
}
