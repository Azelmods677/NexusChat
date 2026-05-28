package com.Azelmods.App.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.cos
import kotlin.math.sin

/**
 * Parse a hex color string (#RRGGBB or #AARRGGBB) to Compose Color.
 * Falls back to [fallback] on any parse error.
 */
fun parseHexColor(hex: String, fallback: Color = Color(0xFF0D0D1A)): Color {
    return try {
        val clean = hex.removePrefix("#")
        val colorLong = clean.toLong(16)
        if (clean.length <= 6) Color(colorLong or 0xFF000000.toLong()) else Color(colorLong)
    } catch (_: Exception) {
        fallback
    }
}

/**
 * Convert gradient colors + angle into a Compose [Brush.linearGradient].
 * Returns null when fewer than 2 colors.
 */
fun linearGradientBrush(
    gradientColors: List<String>,
    gradientAngle: Int = 135
): Brush? {
    if (gradientColors.size < 2) return null
    val colors = gradientColors.map { parseHexColor(it) }
    val angleRad = Math.toRadians(gradientAngle.toDouble())
    return Brush.linearGradient(
        colors = colors,
        start = Offset(
            x = (0.5f - cos(angleRad).toFloat() * 0.5f) * 1000f,
            y = (0.5f - sin(angleRad).toFloat() * 0.5f) * 1000f
        ),
        end = Offset(
            x = (0.5f + cos(angleRad).toFloat() * 0.5f) * 1000f,
            y = (0.5f + sin(angleRad).toFloat() * 0.5f) * 1000f
        )
    )
}

/**
 * Pre-built background brushes for common states.
 */
object BackgroundBrushes {
    val DEFAULT_DARK = Color(0xFF0D0D1A)
    val DEFAULT_SURFACE = Color(0xFF1A1A2E)
    val DEFAULT_SURFACE_VARIANT = Color(0xFF252538)
}
