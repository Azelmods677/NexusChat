package com.Azelmods.App.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Dynamic Theme System
 * Allows users to change app accent color
 */
object AppTheme {
    // Purple Theme (Default)
    val PurplePrimary = Color(0xFF7C3AED)
    val PurpleSecondary = Color(0xFF5B21B6)
    val PurpleTertiary = Color(0xFF3B0764)
    
    // Blue Theme
    val BluePrimary = Color(0xFF3B82F6)
    val BlueSecondary = Color(0xFF2563EB)
    val BlueTertiary = Color(0xFF1E40AF)
    
    // Green Theme
    val GreenPrimary = Color(0xFF10B981)
    val GreenSecondary = Color(0xFF059669)
    val GreenTertiary = Color(0xFF047857)
    
    // Red Theme
    val RedPrimary = Color(0xFFEF4444)
    val RedSecondary = Color(0xFFDC2626)
    val RedTertiary = Color(0xFFB91C1C)
    
    // Orange Theme
    val OrangePrimary = Color(0xFFF97316)
    val OrangeSecondary = Color(0xFFEA580C)
    val OrangeTertiary = Color(0xFFC2410C)
    
    fun getPrimaryColor(theme: String): Color {
        return when (theme) {
            "Purple" -> PurplePrimary
            "Blue" -> BluePrimary
            "Green" -> GreenPrimary
            "Red" -> RedPrimary
            "Orange" -> OrangePrimary
            else -> PurplePrimary
        }
    }
    
    fun getSecondaryColor(theme: String): Color {
        return when (theme) {
            "Purple" -> PurpleSecondary
            "Blue" -> BlueSecondary
            "Green" -> GreenSecondary
            "Red" -> RedSecondary
            "Orange" -> OrangeSecondary
            else -> PurpleSecondary
        }
    }
    
    fun getTertiaryColor(theme: String): Color {
        return when (theme) {
            "Purple" -> PurpleTertiary
            "Blue" -> BlueTertiary
            "Green" -> GreenTertiary
            "Red" -> RedTertiary
            "Orange" -> OrangeTertiary
            else -> PurpleTertiary
        }
    }
}

// Composable to get current theme color
@Composable
fun rememberThemeColor(): Color {
    val viewModel: com.Azelmods.App.ui.screens.settings.SettingsViewModel = hiltViewModel()
    val accentColor by viewModel.accentColor.collectAsState(initial = "Purple")
    return AppTheme.getPrimaryColor(accentColor)
}

@Composable
fun rememberThemeSecondaryColor(): Color {
    val viewModel: com.Azelmods.App.ui.screens.settings.SettingsViewModel = hiltViewModel()
    val accentColor by viewModel.accentColor.collectAsState(initial = "Purple")
    return AppTheme.getSecondaryColor(accentColor)
}
