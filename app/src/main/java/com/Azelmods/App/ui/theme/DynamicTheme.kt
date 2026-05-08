package com.Azelmods.App.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Dynamic Theme System - 15 Predefined Themes
 * Allows users to change app accent color
 */
object AppTheme {
    // 1. Purple Theme (Default)
    val PurplePrimary = Color(0xFF7C3AED)
    val PurpleSecondary = Color(0xFF5B21B6)
    val PurpleTertiary = Color(0xFF3B0764)
    
    // 2. Blue Theme
    val BluePrimary = Color(0xFF3B82F6)
    val BlueSecondary = Color(0xFF2563EB)
    val BlueTertiary = Color(0xFF1E40AF)
    
    // 3. Green Theme
    val GreenPrimary = Color(0xFF10B981)
    val GreenSecondary = Color(0xFF059669)
    val GreenTertiary = Color(0xFF047857)
    
    // 4. Red Theme
    val RedPrimary = Color(0xFFEF4444)
    val RedSecondary = Color(0xFFDC2626)
    val RedTertiary = Color(0xFFB91C1C)
    
    // 5. Pink Theme
    val PinkPrimary = Color(0xFFEC4899)
    val PinkSecondary = Color(0xFFDB2777)
    val PinkTertiary = Color(0xFFBE185D)
    
    // 6. Orange Theme
    val OrangePrimary = Color(0xFFF97316)
    val OrangeSecondary = Color(0xFFEA580C)
    val OrangeTertiary = Color(0xFFC2410C)
    
    // 7. Cyan Theme
    val CyanPrimary = Color(0xFF06B6D4)
    val CyanSecondary = Color(0xFF0891B2)
    val CyanTertiary = Color(0xFF0E7490)
    
    // 8. Toxic Theme (Verde Neón)
    val ToxicPrimary = Color(0xFF00FF00)
    val ToxicSecondary = Color(0xFF00CC00)
    val ToxicTertiary = Color(0xFF009900)
    
    // 9. Dark Theme
    val DarkPrimary = Color(0xFF1F2937)
    val DarkSecondary = Color(0xFF111827)
    val DarkTertiary = Color(0xFF0F172A)
    
    // 10. Gold Theme
    val GoldPrimary = Color(0xFFFBBF24)
    val GoldSecondary = Color(0xFFF59E0B)
    val GoldTertiary = Color(0xFFD97706)
    
    // 11. Toxico Red Theme
    val ToxicoRedPrimary = Color(0xFFFF0000)
    val ToxicoRedSecondary = Color(0xFFCC0000)
    val ToxicoRedTertiary = Color(0xFF990000)
    
    // 12. Perverso Theme
    val PerversoPrimary = Color(0xFFCC0000)
    val PerversoSecondary = Color(0xFF990000)
    val PerversoTertiary = Color(0xFF660000)
    
    // 13. Crimson Dark Theme
    val CrimsonDarkPrimary = Color(0xFF8B0000)
    val CrimsonDarkSecondary = Color(0xFF660000)
    val CrimsonDarkTertiary = Color(0xFF4D0000)
    
    // 14. Neon Red Theme
    val NeonRedPrimary = Color(0xFFFF1744)
    val NeonRedSecondary = Color(0xFFD50000)
    val NeonRedTertiary = Color(0xFFB71C1C)
    
    // 15. Blood Moon Theme
    val BloodMoonPrimary = Color(0xFFB71C1C)
    val BloodMoonSecondary = Color(0xFF8B0000)
    val BloodMoonTertiary = Color(0xFF660000)
    
    fun getPrimaryColor(theme: String): Color {
        return when (theme.uppercase()) {
            "PURPLE", "MORADO" -> PurplePrimary
            "BLUE", "AZUL" -> BluePrimary
            "GREEN", "VERDE" -> GreenPrimary
            "RED", "ROJO" -> RedPrimary
            "PINK", "ROSA" -> PinkPrimary
            "ORANGE", "NARANJA" -> OrangePrimary
            "CYAN", "CIAN" -> CyanPrimary
            "TOXIC", "TÓXICO" -> ToxicPrimary
            "DARK", "OSCURO" -> DarkPrimary
            "GOLD", "DORADO" -> GoldPrimary
            "TOXICO_RED" -> ToxicoRedPrimary
            "PERVERSO" -> PerversoPrimary
            "CRIMSON_DARK", "CRIMSONDARK" -> CrimsonDarkPrimary
            "NEON_RED", "NEONRED" -> NeonRedPrimary
            "BLOOD_MOON", "BLOODMOON" -> BloodMoonPrimary
            else -> PurplePrimary // Default fallback
        }
    }
    
    fun getSecondaryColor(theme: String): Color {
        return when (theme.uppercase()) {
            "PURPLE", "MORADO" -> PurpleSecondary
            "BLUE", "AZUL" -> BlueSecondary
            "GREEN", "VERDE" -> GreenSecondary
            "RED", "ROJO" -> RedSecondary
            "PINK", "ROSA" -> PinkSecondary
            "ORANGE", "NARANJA" -> OrangeSecondary
            "CYAN", "CIAN" -> CyanSecondary
            "TOXIC", "TÓXICO" -> ToxicSecondary
            "DARK", "OSCURO" -> DarkSecondary
            "GOLD", "DORADO" -> GoldSecondary
            "TOXICO_RED" -> ToxicoRedSecondary
            "PERVERSO" -> PerversoSecondary
            "CRIMSON_DARK", "CRIMSONDARK" -> CrimsonDarkSecondary
            "NEON_RED", "NEONRED" -> NeonRedSecondary
            "BLOOD_MOON", "BLOODMOON" -> BloodMoonSecondary
            else -> PurpleSecondary // Default fallback
        }
    }
    
    fun getTertiaryColor(theme: String): Color {
        return when (theme.uppercase()) {
            "PURPLE", "MORADO" -> PurpleTertiary
            "BLUE", "AZUL" -> BlueTertiary
            "GREEN", "VERDE" -> GreenTertiary
            "RED", "ROJO" -> RedTertiary
            "PINK", "ROSA" -> PinkTertiary
            "ORANGE", "NARANJA" -> OrangeTertiary
            "CYAN", "CIAN" -> CyanTertiary
            "TOXIC", "TÓXICO" -> ToxicTertiary
            "DARK", "OSCURO" -> DarkTertiary
            "GOLD", "DORADO" -> GoldTertiary
            "TOXICO_RED" -> ToxicoRedTertiary
            "PERVERSO" -> PerversoTertiary
            "CRIMSON_DARK", "CRIMSONDARK" -> CrimsonDarkTertiary
            "NEON_RED", "NEONRED" -> NeonRedTertiary
            "BLOOD_MOON", "BLOODMOON" -> BloodMoonTertiary
            else -> PurpleTertiary // Default fallback
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
