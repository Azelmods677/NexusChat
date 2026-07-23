package com.Azelmods.App.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
// Color.kt — capa de COMPATIBILIDAD sobre el sistema canónico NexusTokens.
//
// Regla del Nexus Design System (Fase 5, unificación de identidad):
//   · Un color = UNA definición. El canon vive en NexusTokens.Color.
//   · Este archivo solo re-exporta alias con los nombres que las pantallas
//     ya usan, para no tocar 200+ archivos de UI.
//   · Los colores genuinamente propios de un dominio (terminal, diálogos,
//     paleta de avatares) sí se definen aquí porque no son roles del sistema.
//   · PROHIBIDO añadir aquí un Color(0xFF…) que duplique un rol existente.
// ═══════════════════════════════════════════════════════════════════════════

// ── Marca (alias del canon) ────────────────────────────────────────────────
val Purple = NexusTokens.Color.Primary          // Violeta de marca — ÚNICO en la app
val Teal = Color(0xFF00BFA6)                    // Verde-azulado (estado "delivered")
val Pink = NexusTokens.Color.Accent             // Rosa neón de acento

// ── Superficies oscuras (alias de la escalera canónica) ────────────────────
val DarkBackground = NexusTokens.Color.BgBase
val DarkSurface = NexusTokens.Color.BgSurface
val DarkSurfaceVariant = NexusTokens.Color.BgElevated
val DarkElevated = Color(0xFF2D2D44)            // Cuarto escalón: cards sobre cards
val DarkBorder = Color(0xFF3D3D5C)              // Bordes de inputs y divisores

// ── Tema claro (soportado) ─────────────────────────────────────────────────
val LightBackground = Color(0xFFFAFAFA)
val LightSurface = Color(0xFFFFFFFF)

// ── Semánticos (alias del canon) ───────────────────────────────────────────
val Success = NexusTokens.Color.Success
val Error = NexusTokens.Color.Error
val Warning = Color(0xFFFF9800)                 // Naranja de aviso (M3 no lo consume)
val Info = Color(0xFF2196F3)

// ── Estado de mensajes ─────────────────────────────────────────────────────
val MessageSent = Color(0xFF9E9E9E)
val MessageDelivered = Teal
val MessageRead = NexusTokens.Color.Primary

// ── Gradiente de Stories (identidad Nexus, 5 paradas) ──────────────────────
val StoryGradient = listOf(
    NexusTokens.Color.Accent,   // Rosa
    Color(0xFFFF8E53),          // Naranja
    Color(0xFFFFC837),          // Amarillo
    Teal,                       // Teal
    NexusTokens.Color.Primary   // Violeta
)

// ── Acentos consolidados (antes duplicados con hex casi iguales) ───────────
val ErrorRed = NexusTokens.Color.Error          // antes 0xFFEF4444
val AmberAccent = Color(0xFFF59E0B)             // Ámbar para warnings suaves
val NeonGreen = NexusTokens.Color.Online        // mismo hex que el canon
val EmeraldGreen = NexusTokens.Color.Online     // antes 0xFF10B981 (mismo rol)
val CyanAccent = NexusTokens.Color.Secondary    // mismo hex que el canon
val PurpleLight = NexusTokens.Color.PrimaryLight // antes 0xFF9B75FF
val PurpleBright = NexusTokens.Color.Primary    // antes 0xFF7B5CFA (mismo rol)
val GoldPremium = NexusTokens.Color.Gold        // mismo hex que el canon
val BlueAccent = Color(0xFF3B82F6)              // Azul (documentos, acciones 2ª)
val RosePink = Color(0xFFFC5C7D)                // Rosa intenso (gradientes)
val IndigoDeep = Color(0xFF2D1B69)              // Índigo (barra modo efímero)
val RedDeep = Color(0xFFCC0000)                 // Rojo intenso (destructivas)

// ── Gradiente de burbujas oscuras ──────────────────────────────────────────
val DarkBubbleDeep = Color(0xFF1E1E2E)
val DarkBubbleLight = Color(0xFF2A2A3E)
val DarkDeep = Color(0xFF0D0D1A)                // Fallback de video/wallpaper

// ── Dominio: Terminal / Editor (paleta propia, no son roles del sistema) ───
val TerminalGreen = Color(0xFF00FF41)
val TerminalBlack = Color(0xFF0A0A0A)
val TerminalGray = Color(0xFFCCCCCC)
val TerminalRed = Color(0xFFFF4444)
val TerminalAmber = Color(0xFFFFAA00)
val TerminalSurface = Color(0xFF111111)

// ── Dominio: diálogos oscuros (color picker) ───────────────────────────────
val DialogDark = Color(0xFF0F0F0F)
val DialogField = Color(0xFF1A1A1A)

// ── Paleta determinista para avatares sin foto (indexada por hash) ─────────
val CoralOrange = Color(0xFFFF8A65)
val SkyBlue = Color(0xFF4FC3F7)
val LavenderMist = Color(0xFFCE93D8)

val AvatarPalette = listOf(
    NexusTokens.Color.Primary, CyanAccent, Pink,
    NeonGreen, GoldPremium, CoralOrange,
    SkyBlue, LavenderMist
)
