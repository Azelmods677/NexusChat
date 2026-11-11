package com.Azelmods.App.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val ROTATE_MS = 10_000
private const val CIRCLE_MS = 2_000
private const val DASH_STEP = 12f
private const val DASH_SWEEP = 8f
private const val LEAD_GAP = 48f

/**
 * Tu foto dentro de un aro vivo: el aro da una vuelta cada 10 s mientras su arco crece y se
 * recoge cada 2 s, dejando una estela de segmentos que se afinan. Al tocarla se encoge un instante.
 */
@Composable
fun AnimatedRingAvatar(
    name: String,
    photoUrl: String?,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val accent = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "aroFoto")
    val degrees by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(ROTATE_MS, easing = LinearEasing)),
        label = "giro"
    )
    // 0..1 el arco crece; 1..2 se recoge.
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(CIRCLE_MS * 2, easing = LinearEasing)),
        label = "arco"
    )
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.875f else 1f, tween(200), label = "pulsar")

    Box(
        modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                else Modifier
            )
            .drawBehind {
                val width = (this.size.minDimension * 0.05f).coerceAtLeast(1.5.dp.toPx())
                val inset = width / 2f
                val topLeft = Offset(inset, inset)
                val arcSize = Size(this.size.width - width, this.size.height - width)
                val brush = Brush.linearGradient(listOf(accent, Color.White))
                val stroke = Stroke(width = width, cap = StrokeCap.Round)
                // Frenado al final, como un DecelerateInterpolator.
                fun eased(t: Float) = 1f - (1f - t) * (1f - t)
                rotate(degrees + DASH_STEP) {
                    if (phase < 1f) {
                        val sweep = 360f * eased(phase)
                        val solid = (sweep - LEAD_GAP).coerceAtLeast(0f)
                        if (solid > 0f) drawArc(brush, 0f, solid, false, topLeft, arcSize, style = stroke)
                        var dash = 0
                        while (dash <= 4 && solid + DASH_STEP * dash <= sweep) {
                            drawArc(brush, solid + DASH_STEP * dash, DASH_SWEEP - dash, false, topLeft, arcSize, style = stroke)
                            dash++
                        }
                    } else {
                        val start = 360f * eased(phase - 1f)
                        drawArc(brush, start, 360f - start, false, topLeft, arcSize, style = stroke)
                        var position = start
                        var dashSweep = DASH_SWEEP
                        while (position > DASH_STEP && dashSweep > 0.4f) {
                            dashSweep -= 0.2f
                            position -= DASH_STEP
                            drawArc(brush, position, dashSweep, false, topLeft, arcSize, style = stroke)
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        UserAvatar(name = name, photoUrl = photoUrl, size = size * 0.78f)
    }
}
