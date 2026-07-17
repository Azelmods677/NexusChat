package com.Azelmods.App.ui.theme

import androidx.compose.ui.graphics.Color

// Primary Colors
val Purple = Color(0xFF7C3AED)      // Primary brand color
val Teal = Color(0xFF00BFA6)        // Secondary accent
val Pink = Color(0xFFFF6B9D)        // Tertiary accent

// Dark Theme (Default)
val DarkBackground = Color(0xFF0F0F1A)  // Main background
val DarkSurface = Color(0xFF1A1A2E)     // Card/surface background
val DarkSurfaceVariant = Color(0xFF252538)

// Light Theme (Supported)
val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)

// Semantic Colors
val Success = Color(0xFF4CAF50)
val Error = Color(0xFFF44336)
val Warning = Color(0xFFFF9800)
val Info = Color(0xFF2196F3)

// Message Status Colors
val MessageSent = Color(0xFF9E9E9E)
val MessageDelivered = Color(0xFF00BFA6)
val MessageRead = Color(0xFF7C3AED)

// Story Gradient
val StoryGradient = listOf(
    Color(0xFFFF6B9D),  // Pink
    Color(0xFFFF8E53),  // Orange
    Color(0xFFFFC837),  // Yellow
    Color(0xFF00BFA6),  // Teal
    Color(0xFF7C3AED)   // Purple
)

// ── Tokens agregados al centralizar los Color(0xFF…) hardcodeados (Fase 4) ──
// Regla del refactor: cada token conserva EXACTAMENTE el hex que reemplaza,
// así la migración es un cambio de arquitectura puro, sin regresión visual.
// La consolidación de matices casi-iguales (p.ej. ErrorRed vs Error) es un
// segundo paso deliberadamente separado.
val DarkElevated = Color(0xFF2D2D44)    // Superficie elevada (cards sobre cards)
val DarkBorder = Color(0xFF3D3D5C)      // Bordes de inputs y divisores
val ErrorRed = Color(0xFFEF4444)        // Rojo de error usado por las pantallas
val AmberAccent = Color(0xFFF59E0B)     // Ámbar para warnings suaves/destacados
val NeonGreen = Color(0xFF00E676)       // Verde neón (badges, estado online)
val EmeraldGreen = Color(0xFF10B981)    // Verde esmeralda (indicadores online)
val CyanAccent = Color(0xFF00D4FF)      // Cyan eléctrico (acentos secundarios)
val PurpleLight = Color(0xFF9B75FF)     // Violeta claro (acentos/gradientes)
val PurpleBright = Color(0xFF7B5CFA)    // Violeta brillante (gradientes)
val TerminalGreen = Color(0xFF00FF41)   // Verde fósforo (pantalla de terminal)
val TerminalBlack = Color(0xFF0A0A0A)   // Fondo casi negro (terminal/editor)
val GoldPremium = Color(0xFFFFD700)     // Dorado premium
val BlueAccent = Color(0xFF3B82F6)      // Azul (documentos, acciones secundarias)
val DarkBubbleDeep = Color(0xFF1E1E2E)  // Gradiente oscuro: extremo profundo
val DarkBubbleLight = Color(0xFF2A2A3E) // Gradiente oscuro: extremo claro
val DarkDeep = Color(0xFF0D0D1A)        // Fondo profundo (fallback de video/wallpaper)
val RosePink = Color(0xFFFC5C7D)        // Rosa intenso (gradientes de acento)
