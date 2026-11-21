package com.Azelmods.App.ui.screens.welcome

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Efectos anime de Nexa: corazones, brillos y los signos "?" y "!". */
internal fun DrawScope.nexaHeart(c: Offset, r: Float, color: Color) {
    val p = Path().apply {
        moveTo(c.x, c.y + r * 0.9f)
        cubicTo(c.x - r * 1.6f, c.y - r * 0.2f, c.x - r * 0.7f, c.y - r * 1.3f, c.x, c.y - r * 0.4f)
        cubicTo(c.x + r * 0.7f, c.y - r * 1.3f, c.x + r * 1.6f, c.y - r * 0.2f, c.x, c.y + r * 0.9f)
        close()
    }
    drawPath(p, color)
}

internal fun DrawScope.nexaSparkle(c: Offset, r: Float, color: Color) {
    if (r <= 0.5f) return
    val p = Path().apply {
        moveTo(c.x, c.y - r); quadraticTo(c.x, c.y, c.x + r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y + r); quadraticTo(c.x, c.y, c.x - r, c.y)
        quadraticTo(c.x, c.y, c.x, c.y - r); close()
    }
    drawPath(p, color)
    drawCircle(Color.White.copy(alpha = 0.8f), r * 0.18f, c)
}

/** Signo manga ("?" o "!") dibujado a mano, sin depender de una fuente. */
internal fun DrawScope.nexaMark(which: String, at: Offset, h: Float) {
    val col = Color(0xFF9B6BFF)
    val w = h * 0.12f
    if (which == "!") {
        drawLine(col, Offset(at.x, at.y - h / 2f), Offset(at.x, at.y + h * 0.18f), strokeWidth = w * 1.4f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        drawCircle(col, w * 0.85f, Offset(at.x, at.y + h * 0.45f))
    } else {
        val p = Path().apply {
            moveTo(at.x - h * 0.22f, at.y - h * 0.22f)
            cubicTo(at.x - h * 0.22f, at.y - h * 0.55f, at.x + h * 0.26f, at.y - h * 0.55f, at.x + h * 0.24f, at.y - h * 0.22f)
            cubicTo(at.x + h * 0.22f, at.y, at.x, at.y + h * 0.02f, at.x, at.y + h * 0.2f)
        }
        drawPath(p, col, style = Stroke(w * 1.3f, cap = androidx.compose.ui.graphics.StrokeCap.Round))
        drawCircle(col, w * 0.85f, Offset(at.x, at.y + h * 0.45f))
    }
}
