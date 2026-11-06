package com.Azelmods.App.ui.theme

import androidx.compose.ui.graphics.Color

// ═══════════════════════════════════════════════════════════════════════════
// Color.kt — capa de compatibilidad sobre NexusTokens.Color.
//
// Los nombres antiguos ("Purple", "NeonGreen", "CyanAccent"…) siguen existiendo
// para no tocar cada pantalla a la vez, pero ya apuntan a la paleta neutra: ningún
// color neón sobrevive detrás de un alias. El código nuevo debe usar NexusTokens o
// MaterialTheme.colorScheme.
// ═══════════════════════════════════════════════════════════════════════════

// ── Marca ──────────────────────────────────────────────────────────────────
val Purple get() = NexusTokens.Color.Primary
val Teal get() = NexusTokens.Color.Primary
val Pink = NexusTokens.Color.Accent

// ── Superficies oscuras ────────────────────────────────────────────────────
val DarkBackground = NexusTokens.Color.BgBase
val DarkSurface = NexusTokens.Color.BgSurface
val DarkSurfaceVariant = NexusTokens.Color.BgElevated
val DarkElevated = NexusTokens.Color.BgHigh
val DarkBorder = NexusTokens.Color.Hairline

// ── Tema claro ─────────────────────────────────────────────────────────────
val LightBackground = Color(0xFFF6F7F8)
val LightSurface = Color(0xFFFFFFFF)

// ── Semánticos ─────────────────────────────────────────────────────────────
val Success = NexusTokens.Color.Success
val Error = NexusTokens.Color.Error
val Warning = NexusTokens.Color.Warning
val Info = NexusTokens.Color.Secondary

// ── Estado de mensajes ─────────────────────────────────────────────────────
val MessageSent = NexusTokens.Color.TextMuted
val MessageDelivered = NexusTokens.Color.TextSecondary
val MessageRead = NexusTokens.Color.ReadReceipt

// ── Anillo de historias: un solo tono, no un arcoíris ──────────────────────
val StoryGradient: List<Color> get() = listOf(
    NexusTokens.Color.Primary,
    NexusTokens.Color.Primary
)

// ── Acentos heredados ──────────────────────────────────────────────────────
val ErrorRed = NexusTokens.Color.Error
val AmberAccent = NexusTokens.Color.Gold
val NeonGreen = NexusTokens.Color.Online
val EmeraldGreen = NexusTokens.Color.Online
val CyanAccent = NexusTokens.Color.Secondary
val PurpleLight get() = NexusTokens.Color.PrimaryLight
val PurpleBright get() = NexusTokens.Color.Primary
val GoldPremium = NexusTokens.Color.Gold
val BlueAccent = NexusTokens.Color.Secondary
val RosePink = NexusTokens.Color.Accent
val IndigoDeep = NexusTokens.Color.BgElevated
val RedDeep = Color(0xFFB23A33)

// ── Burbujas ───────────────────────────────────────────────────────────────
val DarkBubbleDeep = NexusTokens.Color.BgElevated
val DarkBubbleLight = NexusTokens.Color.BgElevated
val DarkDeep = NexusTokens.Color.BgBase

// ── Dominio: terminal y editor ─────────────────────────────────────────────
val TerminalGreen = Color(0xFF8BD5A0)
val TerminalBlack = Color(0xFF0D0F11)
val TerminalGray = Color(0xFFC9CDD1)
val TerminalRed = Color(0xFFE57F78)
val TerminalAmber = Color(0xFFE0B765)
val TerminalSurface = Color(0xFF15181B)

// ── Dominio: diálogos oscuros (selector de color) ──────────────────────────
val DialogDark = NexusTokens.Color.BgSurface
val DialogField = NexusTokens.Color.BgElevated

// ── Avatares sin foto: tonos apagados, deterministas por nombre ────────────
val CoralOrange = Color(0xFFB9745F)
val SkyBlue = Color(0xFF5B8DB0)
val LavenderMist = Color(0xFF8C7BA8)

val AvatarPalette = listOf(
    Color(0xFF4F8F7E),
    Color(0xFF5B8DB0),
    Color(0xFF8C7BA8),
    Color(0xFFA9708A),
    Color(0xFFB9745F),
    Color(0xFFA98E55),
    Color(0xFF7A8F5A),
    Color(0xFF6C7A89)
)
