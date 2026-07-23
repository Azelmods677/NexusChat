package com.Azelmods.App.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ═══════════════════════════════════════════════════════════════════════════
// Nexus Typography — escala completa de Material 3 (15 estilos).
//
// Familia: SansSerif en TODOS los estilos. FontFamily.Default puede romper el
// render de emojis en algunos dispositivos; SansSerif hace fallback correcto a
// las fuentes de emoji del sistema. Antes solo 6 estilos estaban definidos y
// los 9 restantes caían al default de Compose (familia distinta → pantallas
// tipográficamente inconsistentes). Ahora los 15 comparten familia y criterio.
//
// Criterio de pesos Nexus (Principio A5): la jerarquía se marca con peso
// dentro de cada nivel (SemiBold para énfasis, Normal para lectura), no
// multiplicando tamaños. Tamaños = escala estándar M3 para no desplazar
// ningún layout existente.
//
// Regla de uso: 10–12sp (labelSmall/bodySmall) SOLO para metadatos
// (timestamps, captions, badges). El cuerpo de lectura es bodyLarge/Medium.
// ═══════════════════════════════════════════════════════════════════════════
val Typography = Typography(
    // ── Display: números/hero (llamadas, splash) ──
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp
    ),

    // ── Headline: títulos de pantalla ──
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp
    ),

    // ── Title: cabeceras de sección, nombres en top bars ──
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp
    ),

    // ── Body: lectura (mensajes, descripciones) ──
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp
    ),

    // ── Label: botones, chips, metadatos ──
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp
    )
)
