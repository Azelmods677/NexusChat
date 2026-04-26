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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.Azelmods.App.data.preferences.UserPreferences

// Map accent color names to actual colors
fun getAccentColor(colorName: String): Color {
    return when (colorName) {
        "Purple" -> Purple
        "Blue" -> Color(0xFF3B82F6)
        "Green" -> Color(0xFF10B981)
        "Red" -> Color(0xFFEF4444)
        "Orange" -> Color(0xFFF97316)
        else -> Purple // Default
    }
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
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
