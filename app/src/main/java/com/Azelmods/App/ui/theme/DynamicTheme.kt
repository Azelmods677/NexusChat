package com.Azelmods.App.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import com.Azelmods.App.ui.screens.settings.SettingsViewModel

/**
 * Dynamic Theme System — 25 temas curados.
 *
 * Fuente ÚNICA de los acentos de la app. [SWATCHES] define el orden, el id
 * (clave persistida), el nombre visible y el triplete de colores (primary +
 * variantes para gradientes). Tanto el selector de la UI ([ACCENT_SWATCHES])
 * como la resolución de color ([getPrimaryColor]/[getThemeColors]) leen de aquí,
 * evitando listas duplicadas y "drift" entre pantallas.
 */
object AppTheme {

    /** Triplete de colores de un tema (para gradientes y variantes). */
    data class ThemeColors(
        val primary: Color,
        val secondary: Color,
        val tertiary: Color
    )

    /** Un acento seleccionable: id persistido + nombre visible + colores. */
    data class AccentSwatch(
        val id: String,            // clave canónica (se guarda en preferencias)
        val displayName: String,   // etiqueta para la UI
        val colors: ThemeColors
    ) {
        val color: Color get() = colors.primary
    }

    private fun c(hex: Long) = Color(hex)

    // ── 25 temas curados (hue-ordered, vibrantes y distintos) ──────────────
    val ACCENT_SWATCHES: List<AccentSwatch> = listOf(
        // PURPLE es el acento por defecto: su primary ES el violeta de marca
        // (NexusTokens.Color.Primary). Así el tema por defecto y la identidad
        // estática de la app usan exactamente el mismo violeta.
        AccentSwatch("PURPLE",     "Púrpura",         ThemeColors(c(0xFF7C6FE0), c(0xFF651FFF), c(0xFF4A148C))),
        AccentSwatch("VIOLET",     "Violeta",         ThemeColors(c(0xFF9D4EDD), c(0xFF7B2CBF), c(0xFF5A189A))),
        AccentSwatch("LAVENDER",   "Lavanda",         ThemeColors(c(0xFFB388FF), c(0xFF9575CD), c(0xFF673AB7))),
        AccentSwatch("INDIGO",     "Índigo",          ThemeColors(c(0xFF5C6BC0), c(0xFF3949AB), c(0xFF1A237E))),
        AccentSwatch("BLUE",       "Azul",            ThemeColors(c(0xFF2979FF), c(0xFF2962FF), c(0xFF0D47A1))),
        AccentSwatch("SKY",        "Cielo",           ThemeColors(c(0xFF40C4FF), c(0xFF00B0FF), c(0xFF0277BD))),
        AccentSwatch("CYAN",       "Cian",            ThemeColors(c(0xFF00E5FF), c(0xFF00B8D4), c(0xFF00838F))),
        AccentSwatch("AQUA",       "Aguamarina",      ThemeColors(c(0xFF1DE9B6), c(0xFF00BFA5), c(0xFF00897B))),
        AccentSwatch("TEAL",       "Teal",            ThemeColors(c(0xFF26A69A), c(0xFF00897B), c(0xFF004D40))),
        AccentSwatch("MINT",       "Menta",           ThemeColors(c(0xFF64FFDA), c(0xFF1DE9B6), c(0xFF00BFA5))),
        AccentSwatch("EMERALD",    "Esmeralda",       ThemeColors(c(0xFF00E676), c(0xFF00C853), c(0xFF1B5E20))),
        AccentSwatch("GREEN",      "Verde Lima",      ThemeColors(c(0xFF76FF03), c(0xFF64DD17), c(0xFF33691E))),
        AccentSwatch("LIME",       "Lima",            ThemeColors(c(0xFFC6FF00), c(0xFFAEEA00), c(0xFF827717))),
        AccentSwatch("YELLOW",     "Amarillo",        ThemeColors(c(0xFFFFEA00), c(0xFFFFD600), c(0xFFF9A825))),
        AccentSwatch("GOLD",       "Oro",             ThemeColors(c(0xFFFFD54F), c(0xFFFFC107), c(0xFFFF8F00))),
        AccentSwatch("AMBER",      "Ámbar",           ThemeColors(c(0xFFFFAB00), c(0xFFFF8F00), c(0xFFFF6F00))),
        AccentSwatch("ORANGE",     "Naranja",         ThemeColors(c(0xFFFF9100), c(0xFFFF6D00), c(0xFFE65100))),
        AccentSwatch("CORAL",      "Coral",           ThemeColors(c(0xFFFF7043), c(0xFFFF5722), c(0xFFD84315))),
        AccentSwatch("RED",        "Rojo",            ThemeColors(c(0xFFFF1744), c(0xFFD50000), c(0xFFB71C1C))),
        AccentSwatch("CRIMSON",    "Carmesí",         ThemeColors(c(0xFFEF233C), c(0xFFD90429), c(0xFF8D0801))),
        AccentSwatch("ROSE",       "Rosa",            ThemeColors(c(0xFFFF4081), c(0xFFF50057), c(0xFFC51162))),
        AccentSwatch("PINK",       "Rosa Neón",       ThemeColors(c(0xFFFF80AB), c(0xFFFF4081), c(0xFFC2185B))),
        AccentSwatch("MAGENTA",    "Magenta",         ThemeColors(c(0xFFE040FB), c(0xFFD500F9), c(0xFFAA00FF))),
        AccentSwatch("SLATE",      "Pizarra",         ThemeColors(c(0xFF90A4AE), c(0xFF607D8B), c(0xFF37474F))),
        AccentSwatch("BLOOD_MOON", "Luna de Sangre",  ThemeColors(c(0xFFFF5252), c(0xFFC62828), c(0xFF7F0000)))
    )

    // ── Lookup por id (mayúsculas) ────────────────────────────────────────
    private val THEME_MAP: Map<String, ThemeColors> =
        ACCENT_SWATCHES.associate { it.id to it.colors }

    // Alias (nombres antiguos / español) → clave canónica. Preserva las
    // preferencias ya guardadas por usuarios de la versión anterior.
    private val ALIAS_MAP: Map<String, String> = mapOf(
        // Español
        "MORADO" to "PURPLE",
        "VIOLETA" to "VIOLET",
        "LAVANDA" to "LAVENDER",
        "ÍNDIGO" to "INDIGO",
        "INDIGO" to "INDIGO",
        "AZUL" to "BLUE",
        "CIELO" to "SKY",
        "CIAN" to "CYAN",
        "AGUAMARINA" to "AQUA",
        "MENTA" to "MINT",
        "ESMERALDA" to "EMERALD",
        "VERDE" to "GREEN",
        "LIMA" to "LIME",
        "AMARILLO" to "YELLOW",
        "ORO" to "GOLD",
        "ÁMBAR" to "AMBER",
        "AMBAR" to "AMBER",
        "NARANJA" to "ORANGE",
        "ROJO" to "RED",
        "CARMESÍ" to "CRIMSON",
        "CARMESI" to "CRIMSON",
        "ROSA" to "ROSE",
        "PIZARRA" to "SLATE",
        "LUNA DE SANGRE" to "BLOOD_MOON",
        // Nombres legacy de la versión de 15 temas
        "TOXIC" to "GREEN",
        "TÓXICO" to "GREEN",
        "DARK" to "SLATE",
        "OSCURO" to "SLATE",
        "TOXICO_RED" to "RED",
        "PERVERSO" to "CRIMSON",
        "CRIMSON_DARK" to "CRIMSON",
        "CRIMSONDARK" to "CRIMSON",
        "NEON_RED" to "RED",
        "NEONRED" to "RED",
        "BLOODMOON" to "BLOOD_MOON"
    )

    /** Normaliza un nombre a su clave canónica. */
    private fun normalise(name: String): String {
        val upper = name.trim().uppercase()
        return when {
            THEME_MAP.containsKey(upper) -> upper
            ALIAS_MAP.containsKey(upper) -> ALIAS_MAP.getValue(upper)
            else -> upper
        }
    }

    /** Look up [ThemeColors] for a theme name, falling back to PURPLE. */
    private fun lookup(name: String): ThemeColors =
        THEME_MAP[normalise(name)] ?: THEME_MAP.getValue("PURPLE")

    // ── Public helpers ────────────────────────────────────────────────────
    fun getPrimaryColor(theme: String): Color = lookup(theme).primary
    fun getSecondaryColor(theme: String): Color = lookup(theme).secondary
    fun getTertiaryColor(theme: String): Color = lookup(theme).tertiary
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
