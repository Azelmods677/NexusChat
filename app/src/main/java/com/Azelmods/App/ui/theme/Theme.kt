package com.Azelmods.App.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.Azelmods.App.data.preferences.UserPreferences
import com.Azelmods.App.data.preferences.ThemePreferences

// Map accent color names to actual colors - delegates to AppTheme
fun getAccentColor(colorName: String): Color =
    AppTheme.getPrimaryColor(colorName)

/**
 * Factor de escala del tamaño de fuente elegido por el usuario.
 *
 * Es la ÚNICA fuente de verdad del escalado: se usa para escalar la densidad de
 * fuente de toda la app (ver [NexusChatTheme]). Antes solo se escalaba la
 * `Typography` de Material, pero casi todos los `Text(...)` de la app usan
 * `fontSize = XX.sp` a pelo y no leen de la Typography, así que "Muy grande" no
 * agrandaba nada visible (mensajes del chat, tarjetas de la home, etc.).
 *
 * Se aceptan todas las variantes que puede guardar FontSizeScreen (ES/EN, con o
 * sin guion bajo) para tolerar valores heredados.
 */
fun fontScaleFor(sizeName: String): Float =
    when (sizeName.uppercase().replace(" ", "").replace("_", "")) {
        "SMALL", "PEQUEÑO", "PEQUENO" -> 0.85f
        "MEDIUM", "MEDIANO", "NORMAL" -> 1.0f
        "LARGE", "GRANDE" -> 1.15f
        "EXTRALARGE", "XLARGE", "EXTRAGRANDE", "MUYGRANDE" -> 1.3f
        else -> 1.0f
    }

// Get typography based on font size preference
@Composable
fun getTypographyForSize(sizeName: String): androidx.compose.material3.Typography {
    val scaleFactor = fontScaleFor(sizeName)

    return androidx.compose.material3.Typography(
        displayLarge = Typography.displayLarge.copy(fontSize = Typography.displayLarge.fontSize * scaleFactor),
        displayMedium = Typography.displayMedium.copy(fontSize = Typography.displayMedium.fontSize * scaleFactor),
        displaySmall = Typography.displaySmall.copy(fontSize = Typography.displaySmall.fontSize * scaleFactor),
        headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize * scaleFactor),
        headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize * scaleFactor),
        headlineSmall = Typography.headlineSmall.copy(fontSize = Typography.headlineSmall.fontSize * scaleFactor),
        titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize * scaleFactor),
        titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize * scaleFactor),
        titleSmall = Typography.titleSmall.copy(fontSize = Typography.titleSmall.fontSize * scaleFactor),
        bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize * scaleFactor),
        bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize * scaleFactor),
        bodySmall = Typography.bodySmall.copy(fontSize = Typography.bodySmall.fontSize * scaleFactor),
        labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize * scaleFactor),
        labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize * scaleFactor),
        labelSmall = Typography.labelSmall.copy(fontSize = Typography.labelSmall.fontSize * scaleFactor)
    )
}

/**
 * Texto legible sobre un color: oscuro sobre acentos claros, blanco sobre los oscuros.
 * Con un `onPrimary` fijo en blanco, acentos como Jade o Ámbar no pasaban contraste.
 */
private fun readableOn(background: Color): Color =
    if (background.luminance() > 0.34f) Color(0xFF0B0F0D) else Color.White

/**
 * Esquema oscuro completo. Antes `secondary`/`tertiary` eran el acento con alfa: los
 * contenedores de Material salían translúcidos y cambiaban según lo que hubiera detrás.
 */
private fun createDarkColorScheme(accentColor: Color) = darkColorScheme(
    primary = accentColor,
    onPrimary = readableOn(accentColor),
    primaryContainer = lerp(NexusTokens.Color.BgSurface, accentColor, 0.28f),
    onPrimaryContainer = lerp(accentColor, Color.White, 0.65f),
    inversePrimary = lerp(accentColor, Color.Black, 0.35f),
    secondary = NexusTokens.Color.Secondary,
    onSecondary = Color(0xFF0E1215),
    secondaryContainer = NexusTokens.Color.BgHigh,
    onSecondaryContainer = NexusTokens.Color.TextPrimary,
    tertiary = NexusTokens.Color.Gold,
    onTertiary = Color(0xFF1A1405),
    tertiaryContainer = NexusTokens.Color.BgHigh,
    onTertiaryContainer = NexusTokens.Color.TextPrimary,
    background = NexusTokens.Color.BgBase,
    onBackground = NexusTokens.Color.TextPrimary,
    surface = NexusTokens.Color.BgBase,
    onSurface = NexusTokens.Color.TextPrimary,
    surfaceVariant = NexusTokens.Color.BgElevated,
    onSurfaceVariant = NexusTokens.Color.TextSecondary,
    surfaceTint = Color.Transparent,
    surfaceDim = NexusTokens.Color.BgDeep,
    surfaceBright = NexusTokens.Color.BgHigh,
    surfaceContainerLowest = NexusTokens.Color.BgDeep,
    surfaceContainerLow = NexusTokens.Color.BgBase,
    surfaceContainer = NexusTokens.Color.BgSurface,
    surfaceContainerHigh = NexusTokens.Color.BgElevated,
    surfaceContainerHighest = NexusTokens.Color.BgHigh,
    outline = Color(0xFF3A4046),
    outlineVariant = NexusTokens.Color.Hairline,
    error = NexusTokens.Color.Error,
    onError = Color.White,
    errorContainer = Color(0xFF3A1B19),
    onErrorContainer = Color(0xFFF4B8B3),
    scrim = Color.Black
)

private fun createLightColorScheme(accentColor: Color) = lightColorScheme(
    primary = lerp(accentColor, Color.Black, 0.18f),
    onPrimary = Color.White,
    primaryContainer = lerp(Color.White, accentColor, 0.18f),
    onPrimaryContainer = lerp(accentColor, Color.Black, 0.55f),
    secondary = Color(0xFF55636F),
    onSecondary = Color.White,
    tertiary = Color(0xFF8A6A1F),
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF14171A),
    surface = LightSurface,
    onSurface = Color(0xFF14171A),
    surfaceVariant = Color(0xFFEDEFF1),
    onSurfaceVariant = Color(0xFF50575E),
    surfaceTint = Color.Transparent,
    outline = Color(0xFFC3C8CD),
    outlineVariant = Color(0xFFE3E6E9),
    error = NexusTokens.Color.Error,
    onError = Color.White
)

@Composable
fun NexusChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default - use user's accent color
    userPreferences: UserPreferences? = null,
    content: @Composable () -> Unit
) {
    // Get user's accent color preference
    val accentColorName by userPreferences?.accentColor?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Purple") }
    val accentColor = getAccentColor(accentColorName)
    
    // Get user's font size preference.
    // FIX "el tamaño de letra no hace efecto": antes solo se escalaba la Typography de
    // Material, pero la inmensa mayoría de los `Text(...)` de la app fijan `fontSize` en
    // sp a mano y no leen de la Typography, así que no cambiaban de tamaño. Ahora se
    // escala la DENSIDAD DE FUENTE del árbol entero: todo `.sp` —de Typography o a
    // mano— se agranda o encoge por igual. Se multiplica por el fontScale del sistema
    // para respetar también la accesibilidad del dispositivo.
    val fontSizeName by userPreferences?.fontSize?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Medium") }
    val fontScale = fontScaleFor(fontSizeName)
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity.density, baseDensity.fontScale, fontScale) {
        Density(density = baseDensity.density, fontScale = baseDensity.fontScale * fontScale)
    }

    // Dark mode preference (en caliente).
    // FIX: antes NexusChatTheme ignoraba la preferencia y usaba siempre
    // isSystemInDarkTheme(), por lo que el switch "Dark Mode" de AppearanceScreen
    // no tenía efecto real (era un toggle decorativo). Ahora, si hay UserPreferences,
    // la preferencia del usuario manda y el cambio recompone el árbol al instante.
    val darkModeEnabled by userPreferences?.darkModeEnabled?.collectAsState()
        ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(darkTheme) }

    // Lo leen NexusTokens.Color.Primary y compañía, en toda la app.
    if (AccentState.current != accentColor) AccentState.current = accentColor

    val colorScheme = when {
        // Dynamic color for Android 12+ (Material You) - only if explicitly enabled
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkModeEnabled) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Use custom dark/light theme with user's accent color
        darkModeEnabled -> createDarkColorScheme(accentColor)
        else -> createLightColorScheme(accentColor)
    }

    // La Typography se pasa SIN escalar: el escalado lo hace la densidad de fuente de
    // abajo, para no aplicar el factor dos veces sobre el texto que sí usa Typography.
    val fontStyle by com.Azelmods.App.ui.components.ChatLook.fontStyle.collectAsState()
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typographyWith(fontStyle),
        shapes = Shapes
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity, content = content)
    }
}

/** La misma escala de texto con otra familia de letra: solo cambia el aspecto, no los tamaños. */
private fun typographyWith(style: com.Azelmods.App.ui.components.AppFontStyle): androidx.compose.material3.Typography {
    if (style == com.Azelmods.App.ui.components.AppFontStyle.SANS) return Typography
    // Fuentes que ya trae Android: funcionan sin descargar nada. Las que no existan en un
    // teléfono concreto caen en la normal.
    val family = style.resource?.let {
        androidx.compose.ui.text.font.FontFamily(androidx.compose.ui.text.font.Font(it))
    } ?: androidx.compose.ui.text.font.FontFamily(
        android.graphics.Typeface.create(style.systemName, android.graphics.Typeface.NORMAL)
    )
    return Typography.copy(
        displayLarge = Typography.displayLarge.copy(fontFamily = family),
        displayMedium = Typography.displayMedium.copy(fontFamily = family),
        displaySmall = Typography.displaySmall.copy(fontFamily = family),
        headlineLarge = Typography.headlineLarge.copy(fontFamily = family),
        headlineMedium = Typography.headlineMedium.copy(fontFamily = family),
        headlineSmall = Typography.headlineSmall.copy(fontFamily = family),
        titleLarge = Typography.titleLarge.copy(fontFamily = family),
        titleMedium = Typography.titleMedium.copy(fontFamily = family),
        titleSmall = Typography.titleSmall.copy(fontFamily = family),
        bodyLarge = Typography.bodyLarge.copy(fontFamily = family),
        bodyMedium = Typography.bodyMedium.copy(fontFamily = family),
        bodySmall = Typography.bodySmall.copy(fontFamily = family),
        labelLarge = Typography.labelLarge.copy(fontFamily = family),
        labelMedium = Typography.labelMedium.copy(fontFamily = family),
        labelSmall = Typography.labelSmall.copy(fontFamily = family)
    )
}
