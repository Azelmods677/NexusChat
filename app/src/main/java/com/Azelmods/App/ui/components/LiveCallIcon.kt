package com.Azelmods.App.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Movimiento propio sobre los iconos de llamada: ondas que salen del teléfono o un aro que late en la
 * cámara. Se dibuja encima del icono y no recibe toques. Se apaga desde el estudio de diseño.
 */
@Composable
fun CallIconMotion(video: Boolean, modifier: Modifier = Modifier) {
    val enabled by ChatLook.animatedCallIcons.collectAsState()
    if (!enabled) return
    val color = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "iconoLlamada")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = LinearEasing)),
        label = "faseLlamada"
    )
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val stroke = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        if (video) {
            val lens = Offset(center.x - 2.dp.toPx(), center.y)
            val grow = (phase / 0.7f).coerceAtMost(1f)
            drawCircle(
                color = color.copy(alpha = (1f - grow) * 0.9f),
                radius = 5.dp.toPx() + grow * 11.dp.toPx(),
                center = lens,
                style = stroke
            )
        } else {
            val origin = Offset(center.x - 3.dp.toPx(), center.y + 3.dp.toPx())
            repeat(2) { wave ->
                val shown = ((phase - wave * 0.22f) / 0.5f).coerceIn(0f, 1f)
                val alpha = if (shown < 0.5f) shown * 2f else (1f - shown) * 2f
                val radius = (11 + wave * 4).dp.toPx()
                drawArc(
                    color = color.copy(alpha = alpha.coerceIn(0f, 1f)),
                    startAngle = -80f,
                    sweepAngle = 70f,
                    useCenter = false,
                    topLeft = Offset(origin.x - radius, origin.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = stroke
                )
            }
        }
    }
}
