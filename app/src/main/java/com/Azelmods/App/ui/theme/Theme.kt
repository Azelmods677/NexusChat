package com.Azelmods.App.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.Azelmods.App.data.preferences.UserPreferences
import com.Azelmods.App.data.preferences.ThemePreferences

// Map accent color names to actual colors - 15 themes support
fun getAccentColor(colorName: String): Color {
    return when (colorName.uppercase()) {
        "PURPLE", "MORADO" -> Purple
        "BLUE", "AZUL" -> Color(0xFF3B82F6)
        "GREEN", "VERDE" -> Color(0xFF10B981)
        "RED", "ROJO" -> Color(0xFFEF4444)
        "PINK", "ROSA" -> Color(0xFFEC4899)
        "ORANGE", "NARANJA" -> Color(0xFFF97316)
        "CYAN", "CIAN" -> Color(0xFF06B6D4)
        "TOXIC", "TÓXICO" -> Color(0xFF00FF00)
        "DARK", "OSCURO" -> Color(0xFF1F2937)
        "GOLD", "DORADO" -> Color(0xFFFBBF24)
        "TOXICO_RED" -> Color(0xFFFF0000)
        "PERVERSO" -> Color(0xFFCC0000)
        "CRIMSON_DARK", "CRIMSONDARK" -> Color(0xFF8B0000)
        "NEON_RED", "NEONRED" -> Color(0xFFFF1744)
        "BLOOD_MOON", "BLOODMOON" -> Color(0xFFB71C1C)
        else -> Purple // Default fallback
    }
}

// Get typography based on font size preference
@Composable
fun getTypographyForSize(sizeName: String): androidx.compose.material3.Typography {
    val scaleFactor = when (sizeName.uppercase()) {
        "SMALL", "PEQUEÑO" -> 0.85f
        "MEDIUM", "MEDIANO" -> 1.0f
        "LARGE", "GRANDE" -> 1.15f
        "EXTRA_LARGE", "EXTRAGRANDE" -> 1.3f
        else -> 1.0f
    }
    
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
    
    // Get user's font size preference
    val fontSizeName by userPreferences?.fontSize?.collectAsState() ?: androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf("Medium") }
    val typography = getTypographyForSize(fontSizeName)
    
    val colorScheme = when {
        // Dynamic color for Android 12+ (Material You) - only if explicitly enabled
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Use custom dark/light theme with user's accent color
        darkTheme -> createDarkColorScheme(accentColor)
        else -> createLightColorScheme(accentColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = Shapes,
        content = content
    )
}
