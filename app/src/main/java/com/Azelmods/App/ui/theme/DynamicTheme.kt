package com.Azelmods.App.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.Azelmods.App.ui.screens.settings.SettingsViewModel

/**
 * Dynamic Theme System — 15 Predefined Themes
 *
 * Uses a single [THEME_MAP] to avoid the three-way when-expression
 * duplication that existed before (getPrimaryColor / getSecondaryColor /
 * getTertiaryColor each had an identical when block).
 */
object AppTheme {

    // ── Theme colour triplet records ──────────────────────────────────────
    data class ThemeColors(
        val primary: Color,
        val secondary: Color,
        val tertiary: Color
    )

    // ── 15 built-in themes — lookup once, reuse everywhere ────────────────
    private val THEME_MAP: Map<String, ThemeColors> = mapOf(
        "PURPLE"        to ThemeColors(Color(0xFF7C3AED), Color(0xFF5B21B6), Color(0xFF3B0764)),
        "BLUE"          to ThemeColors(Color(0xFF3B82F6), Color(0xFF2563EB), Color(0xFF1E40AF)),
        "GREEN"         to ThemeColors(Color(0xFF10B981), Color(0xFF059669), Color(0xFF047857)),
        "RED"           to ThemeColors(Color(0xFFEF4444), Color(0xFFDC2626), Color(0xFFB91C1C)),
        "PINK"          to ThemeColors(Color(0xFFEC4899), Color(0xFFDB2777), Color(0xFFBE185D)),
        "ORANGE"        to ThemeColors(Color(0xFFF97316), Color(0xFFEA580C), Color(0xFFC2410C)),
        "CYAN"          to ThemeColors(Color(0xFF06B6D4), Color(0xFF0891B2), Color(0xFF0E7490)),
        "TOXIC"         to ThemeColors(Color(0xFF00FF00), Color(0xFF00CC00), Color(0xFF009900)),
        "DARK"          to ThemeColors(Color(0xFF1F2937), Color(0xFF111827), Color(0xFF0F172A)),
        "GOLD"          to ThemeColors(Color(0xFFFBBF24), Color(0xFFF59E0B), Color(0xFFD97706)),
        "TOXICO_RED"    to ThemeColors(Color(0xFFFF0000), Color(0xFFCC0000), Color(0xFF990000)),
        "PERVERSO"      to ThemeColors(Color(0xFFCC0000), Color(0xFF990000), Color(0xFF660000)),
        "CRIMSON_DARK"  to ThemeColors(Color(0xFF8B0000), Color(0xFF660000), Color(0xFF4D0000)),
        "NEON_RED"      to ThemeColors(Color(0xFFFF1744), Color(0xFFD50000), Color(0xFFB71C1C)),
        "BLOOD_MOON"    to ThemeColors(Color(0xFFB71C1C), Color(0xFF8B0000), Color(0xFF660000))
    )

    // Also indexed by Spanish aliases (no extra storage — just aliases)
    private val ALIAS_MAP: Map<String, String> = mapOf(
        "MORADO"     to "PURPLE",
        "AZUL"       to "BLUE",
        "VERDE"      to "GREEN",
        "ROJO"       to "RED",
        "ROSA"       to "PINK",
        "NARANJA"    to "ORANGE",
        "CIAN"       to "CYAN",
        "TÓXICO"     to "TOXIC",
        "OSCURO"     to "DARK",
        "DORADO"     to "GOLD",
        "CRIMSONDARK" to "CRIMSON_DARK",
        "NEONRED"    to "NEON_RED",
        "BLOODMOON"  to "BLOOD_MOON"
    )

    /** Normalise a user-supplied theme name to its canonical key. */
    private fun normalise(name: String): String =
        ALIAS_MAP[name.uppercase()] ?: name.uppercase()

    /** Look up [ThemeColors] for a theme name, falling back to PURPLE. */
    private fun lookup(name: String): ThemeColors =
        THEME_MAP[normalise(name)] ?: THEME_MAP.getValue("PURPLE")

    // ── Public helpers (replacing the old triplet of when-expressions) ────
    fun getPrimaryColor(theme: String): Color  = lookup(theme).primary
    fun getSecondaryColor(theme: String): Color = lookup(theme).secondary
    fun getTertiaryColor(theme: String): Color  = lookup(theme).tertiary
    fun getThemeColors(theme: String): ThemeColors = lookup(theme)
}

// ── Composable helpers used by screens ─────────────────────────────────────

@Composable
fun rememberThemeColor(): Color {
    val viewModel: SettingsViewModel = hiltViewModel()
    val accentColor by viewModel.accentColor.collectAsState(initial = "Purple")
    return AppTheme.getPrimaryColor(accentColor)
}

@Composable
fun rememberThemeSecondaryColor(): Color {
    val viewModel: SettingsViewModel = hiltViewModel()
    val accentColor by viewModel.accentColor.collectAsState(initial = "Purple")
    return AppTheme.getSecondaryColor(accentColor)
}
