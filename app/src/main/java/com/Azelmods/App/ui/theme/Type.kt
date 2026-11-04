package com.Azelmods.App.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════
// Escala tipográfica.
//
// Familia del sistema: en una app de mensajería, el texto debe sentirse propio del
// teléfono (y SansSerif resuelve bien los emojis). Peso máximo SemiBold: el Bold y
// el Black por todas partes aplastan la jerarquía en vez de crearla. Interlineado
// explícito para que los mensajes largos se lean con comodidad.
// ═══════════════════════════════════════════════════════════════════════════
private val Sans = FontFamily.SansSerif

val Typography = Typography(
    // Números grandes: reloj, duración de llamada, porcentaje de batería.
    displayLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 52.sp, lineHeight = 58.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 40.sp, lineHeight = 46.sp, letterSpacing = (-0.25).sp),
    displaySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 32.sp, lineHeight = 38.sp),

    // Títulos de pantalla.
    headlineLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, lineHeight = 34.sp),
    headlineMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),

    // Barras superiores y nombres.
    titleLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),

    // Lectura.
    bodyLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 23.sp),
    bodyMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodySmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 17.sp, letterSpacing = 0.1.sp),

    // Botones, chips y metadatos.
    labelLarge = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall = TextStyle(fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.2.sp)
)
