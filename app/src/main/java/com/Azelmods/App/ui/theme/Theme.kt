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

private fun createDarkColorScheme(accentColor: Color) = darkColorScheme(
    primary = accentColor,
    secondary = accentColor.copy(alpha = 0.7f),
    tertiary = accentColor.copy(alpha = 0.5f),
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    primaryContainer = accentColor.copy(alpha = 0.3f),
    onPrimaryContainer = accentColor,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    error = Error
)

private fun createLightColorScheme(accentColor: Color) = lightColorScheme(
    primary = accentColor,
    secondary = accentColor.copy(alpha = 0.7f),
    tertiary = accentColor.copy(alpha = 0.5f),
    background = LightBackground,
    surface = LightSurface,
    primaryContainer = accentColor.copy(alpha = 0.2f),
    onPrimaryContainer = accentColor,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color.Black,
    onSurface = Color.Black,
    error = Error
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
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes
    ) {
        CompositionLocalProvider(LocalDensity provides scaledDensity, content = content)
    }
}
