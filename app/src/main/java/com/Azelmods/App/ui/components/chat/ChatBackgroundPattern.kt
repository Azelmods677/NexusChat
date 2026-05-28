package com.Azelmods.App.ui.components.chat

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.cos
import kotlin.math.sin

/**
 * ChatBackgroundDoodles — Fondo sutil tipo doodle para la pantalla de chat.
 *
 * Similar a los fondos de WhatsApp/Telegram: patrones pequeños y
 * repetitivos con opacidad muy baja que no distraen del contenido.
 *
 * @param doodleColor Color de los doodles (por defecto blanco muy tenue)
 * @param alpha Opacidad general del patrón (0.01-0.04 recomendado)
 * @param parallaxOffset Offset para efecto parallax al hacer scroll
 */
@Composable
fun ChatBackgroundDoodles(
    doodleColor: Color = Color.White,
    alpha: Float = 0.025f,
    parallaxOffset: Float = 0f,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.coerceIn(0f, 0.1f) }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val shift = parallaxOffset * 0.3f

            // ── Doodle set 1: Small diagonal crosses ──
            val crossSpacing = 140f
            var x = 20f + shift % crossSpacing
            while (x < w) {
                var y = 20f
                while (y < h) {
                    drawSmallCross(doodleColor, x, y, 6f)
                    y += crossSpacing
                }
                x += crossSpacing
            }

            // ── Doodle set 2: Tiny circles ──
            val circleSpacing = 100f
            x = 10f + shift % circleSpacing
            while (x < w) {
                var y = 10f
                while (y < h) {
                    drawCircle(
                        color = doodleColor,
                        radius = 2.5f,
                        center = Offset(x, y)
                    )
                    y += circleSpacing
                }
                x += circleSpacing
            }

            // ── Doodle set 3: Small dots grid ──
            val dotSpacing = 50f
            x = 25f + shift % (dotSpacing * 2)
            while (x < w) {
                var y = 25f
                while (y < h) {
                    drawCircle(
                        color = doodleColor,
                        radius = 1f,
                        center = Offset(x, y)
                    )
                    y += dotSpacing * 2
                }
                x += dotSpacing * 2
            }

            // ── Doodle set 4: Wavy lines ──
            val waveSpacing = 180f
            x = 0f
            var waveIdx = 0
            while (x < w) {
                val yBase = (waveIdx % 7) * waveSpacing * 0.5f + 60f
                if (yBase < h) {
                    drawWavyLine(doodleColor, x, yBase, 40f)
                }
                x += waveSpacing
                waveIdx++
            }

            // ── Doodle set 5: Arrow tips ──
            val arrowSpacing = 200f
            x = 50f + shift % arrowSpacing
            var arrowIdx = 0
            while (x < w) {
                val yPos = (arrowIdx % 5) * arrowSpacing * 0.5f + 100f
                if (yPos < h) {
                    drawSmallArrow(doodleColor, x, yPos, 8f)
                }
                x += arrowSpacing
                arrowIdx++
            }
        }
    }
}

private fun DrawScope.drawSmallCross(color: Color, cx: Float, cy: Float, size: Float) {
    val half = size / 2f
    drawLine(color, Offset(cx - half, cy), Offset(cx + half, cy), strokeWidth = 1.2f)
    drawLine(color, Offset(cx, cy - half), Offset(cx, cy + half), strokeWidth = 1.2f)
}

private fun DrawScope.drawWavyLine(color: Color, startX: Float, startY: Float, length: Float) {
    val path = Path()
    path.moveTo(startX, startY)
    var px = startX
    var py = startY
    val segments = 8
    for (i in 1..segments) {
        val t = i.toFloat() / segments
        val nx = startX + length * t
        val ny = startY + sin(t * Math.PI.toFloat() * 4) * 4f
        path.lineTo(nx, ny)
        px = nx
        py = ny
    }
    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
}

private fun DrawScope.drawSmallArrow(color: Color, cx: Float, cy: Float, size: Float) {
    // Simple arrow: ^ shape
    val half = size / 2f
    val path = Path().apply {
        moveTo(cx, cy - half)
        lineTo(cx - half, cy + half * 0.3f)
        lineTo(cx, cy + half * 0.1f)
        lineTo(cx + half, cy + half * 0.3f)
        close()
    }
    drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f))
}
