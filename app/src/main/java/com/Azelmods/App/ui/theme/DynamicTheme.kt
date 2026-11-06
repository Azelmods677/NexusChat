package com.Azelmods.App.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Azelmods.App.ui.screens.settings.SettingsViewModel

/**
 * Acentos de la app: una lista corta y curada.
 *
 * Fuente ÚNICA de los acentos de la app. [SWATCHES] define el orden, el id
 * (clave persistida), el nombre visible y el triplete de colores (primary +
 * variantes para gradientes). Tanto el selector de la UI ([ACCENT_SWATCHES])
 * como la resolución de color ([getPrimaryColor]/[getThemeColors]) leen de aquí,
 * evitando listas duplicadas y "drift" entre pantallas.
 */
object AppTheme {

    private val CUSTOM_HEX = Regex("^#[0-9a-fA-F]{6}$")

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

    /**
     * Un acento con sus tres roles. `secondary` coincide con `primary`: el código que
     * aún dibuja `linearGradient(primary, secondary)` obtiene un color sólido.
     */
    private fun accent(id: String, name: String, hex: Long): AccentSwatch {
        val color = Color(hex)
        return AccentSwatch(id, name, ThemeColors(color, color, lerp(color, Color.Black, 0.45f)))
    }

    // ── Acentos curados: saturación contenida, todos legibles sobre grafito ──
    val ACCENT_SWATCHES: List<AccentSwatch> = listOf(
        accent("JADE", "Jade", 0xFF2FB38E),
        accent("PETROL", "Petróleo", 0xFF2E9CAE),
        accent("COBALT", "Cobalto", 0xFF4F8AE8),
        accent("INDIGO", "Índigo", 0xFF6D78D6),
        accent("PLUM", "Ciruela", 0xFF9A6BBF),
        accent("ROSE", "Rosa", 0xFFD0648A),
        accent("RED", "Rojo", 0xFFD9534F),
        accent("CORAL", "Coral", 0xFFE07A5F),
        accent("AMBER", "Ámbar", 0xFFD9A441),
        accent("OLIVE", "Oliva", 0xFF8FA35A),
        accent("SLATE", "Pizarra", 0xFF8796A5),
        accent("GRAPHITE", "Grafito", 0xFFC2C7CC),
        // Más vivos
        accent("ELECTRIC", "Azul eléctrico", 0xFF0077FF),
        accent("ROYAL", "Azul rey", 0xFF2F5BEA),
        accent("NAVY", "Marino", 0xFF476599),
        accent("SKYBLUE", "Celeste", 0xFF38B6FF),
        accent("TURQUOISE", "Turquesa", 0xFF14C8C8),
        accent("AQUAMARINE", "Aguamarina", 0xFF2DD4BF),
        accent("EMERALD_GREEN", "Esmeralda", 0xFF10B981),
        accent("TRUE_GREEN", "Verde", 0xFF22C55E),
        accent("LIME_GREEN", "Lima", 0xFF84CC16),
        accent("LEMON", "Limón", 0xFFD4D62B),
        accent("SUN", "Amarillo", 0xFFFACC15),
        accent("TANGERINE", "Mandarina", 0xFFF59E0B),
        accent("TRUE_ORANGE", "Naranja", 0xFFF97316),
        accent("TERRACOTTA", "Terracota", 0xFFC2613F),
        accent("BROWN", "Chocolate", 0xFF9C6B4A),
        accent("VIVID_RED", "Rojo vivo", 0xFFEF2B2D),
        accent("BLOOD", "Sangre", 0xFFBF293F),
        accent("WINE", "Vino", 0xFFA74563),
        accent("HOT_PINK", "Fucsia", 0xFFEC4899),
        accent("BUBBLEGUM", "Chicle", 0xFFF472B6),
        accent("NEON_MAGENTA", "Magenta", 0xFFD946EF),
        accent("AMETHYST", "Amatista", 0xFFA855F7),
        accent("TRUE_VIOLET", "Violeta", 0xFF8B5CF6),
        accent("LILAC", "Lila", 0xFFB39DDB)
    )

    // ── Lookup por id (mayúsculas) ────────────────────────────────────────
    private val THEME_MAP: Map<String, ThemeColors> =
        ACCENT_SWATCHES.associate { it.id to it.colors }

    // Ids de versiones anteriores → acento actual más cercano, para que una preferencia
    // guardada siga siendo válida. PURPLE era el valor por defecto: pasa a Jade.
    private val ALIAS_MAP: Map<String, String> = mapOf(
        "PURPLE" to "JADE", "MORADO" to "JADE",
        "VIOLET" to "PLUM", "VIOLETA" to "PLUM",
        "LAVENDER" to "PLUM", "LAVANDA" to "PLUM",
        "MAGENTA" to "PLUM",
        "ÍNDIGO" to "INDIGO",
        "BLUE" to "COBALT", "AZUL" to "COBALT",
        "SKY" to "COBALT", "CIELO" to "COBALT",
        "CYAN" to "PETROL", "CIAN" to "PETROL",
        "AQUA" to "PETROL", "AGUAMARINA" to "PETROL",
        "TEAL" to "PETROL",
        "MINT" to "JADE", "MENTA" to "JADE",
        "EMERALD" to "JADE", "ESMERALDA" to "JADE",
        "GREEN" to "OLIVE", "VERDE" to "OLIVE",
        "LIME" to "OLIVE", "LIMA" to "OLIVE",
        "YELLOW" to "AMBER", "AMARILLO" to "AMBER",
        "GOLD" to "AMBER", "ORO" to "AMBER",
        "ÁMBAR" to "AMBER", "AMBAR" to "AMBER",
        "ORANGE" to "CORAL", "NARANJA" to "CORAL",
        "ROJO" to "RED",
        "CRIMSON" to "RED", "CARMESÍ" to "RED", "CARMESI" to "RED",
        "ROSA" to "ROSE", "PINK" to "ROSE",
        "PIZARRA" to "SLATE",
        "BLOOD_MOON" to "RED", "LUNA DE SANGRE" to "RED", "BLOODMOON" to "RED",
        "TOXIC" to "OLIVE", "TÓXICO" to "OLIVE",
        "DARK" to "SLATE", "OSCURO" to "SLATE",
        "TOXICO_RED" to "RED", "PERVERSO" to "RED",
        "CRIMSON_DARK" to "RED", "CRIMSONDARK" to "RED",
        "NEON_RED" to "RED", "NEONRED" to "RED"
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

    /** Un color libre elegido por el usuario se guarda como "#RRGGBB". */
    fun isCustom(name: String): Boolean = CUSTOM_HEX.matches(name.trim())

    private fun customColors(name: String): ThemeColors {
        val color = Color(0xFF000000 or name.trim().removePrefix("#").toLong(16))
        return ThemeColors(color, color, lerp(color, Color.Black, 0.45f))
    }

    /** Busca los colores de un acento; si no se reconoce, Jade. */
    private fun lookup(name: String): ThemeColors = when {
        isCustom(name) -> customColors(name)
        else -> THEME_MAP[normalise(name)] ?: THEME_MAP.getValue("JADE")
    }

    /** Id canónico de un valor guardado, para marcar el acento activo en el selector. */
    fun canonicalId(name: String): String = when {
        isCustom(name) -> name.trim().uppercase()
        else -> normalise(name).takeIf { it in THEME_MAP } ?: "JADE"
    }

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
