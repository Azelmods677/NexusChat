package com.Azelmods.App.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages theme and color customization preferences.
 *
 * Features:
 * - Custom primary/secondary colors
 * - Predefined theme presets
 * - Chat background customization
 * - Message bubble styles
 */
@Singleton
class ThemePreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "theme_prefs",
        Context.MODE_PRIVATE
    )

    companion object {
        private const val KEY_PRIMARY_COLOR = "primary_color"
        private const val KEY_SECONDARY_COLOR = "secondary_color"
        private const val KEY_THEME_PRESET = "theme_preset"
        private const val KEY_CHAT_BACKGROUND = "chat_background"
        private const val KEY_MESSAGE_STYLE = "message_style"
        private const val KEY_TAB_ORDER = "tab_order"
        private const val KEY_VIDEO_WALLPAPER_URI = "video_wallpaper_uri"
        
        // Default colors
        private val DEFAULT_PRIMARY = Color(0xFF7C3AED)
        private val DEFAULT_SECONDARY = Color(0xFF5B21B6)
        
        // Default tab order: Chats, Stories, Calls, Profile
        private const val DEFAULT_TAB_ORDER = "0,1,2,3"
    }

    /**
     * Gets the primary theme color
     */
    fun getPrimaryColor(): Color {
        val colorInt = prefs.getInt(KEY_PRIMARY_COLOR, DEFAULT_PRIMARY.toArgb())
        return Color(colorInt)
    }

    /**
     * Sets the primary theme color
     */
    fun setPrimaryColor(color: Color) {
        prefs.edit().putInt(KEY_PRIMARY_COLOR, color.toArgb()).apply()
    }

    /**
     * Gets the secondary theme color
     */
    fun getSecondaryColor(): Color {
        val colorInt = prefs.getInt(KEY_SECONDARY_COLOR, DEFAULT_SECONDARY.toArgb())
        return Color(colorInt)
    }

    /**
     * Sets the secondary theme color
     */
    fun setSecondaryColor(color: Color) {
        prefs.edit().putInt(KEY_SECONDARY_COLOR, color.toArgb()).apply()
    }

    /**
     * Gets the current theme preset
     */
    fun getThemePreset(): ThemePreset {
        val presetName = prefs.getString(KEY_THEME_PRESET, ThemePreset.PURPLE.name)
        return ThemePreset.valueOf(presetName ?: ThemePreset.PURPLE.name)
    }

    /**
     * Sets a theme preset
     */
    fun setThemePreset(preset: ThemePreset) {
        prefs.edit().putString(KEY_THEME_PRESET, preset.name).apply()
        setPrimaryColor(preset.primaryColor)
        setSecondaryColor(preset.secondaryColor)
    }
    
    /**
     * Gets background color from current theme preset
     */
    fun getBackgroundColor(): Color {
        val preset = getThemePreset()
        return preset.backgroundColor
    }
    
    /**
     * Gets surface color from current theme preset
     */
    fun getSurfaceColor(): Color {
        val preset = getThemePreset()
        return preset.surfaceColor
    }

    /**
     * Gets chat background type
     */
    fun getChatBackground(): ChatBackground {
        val bgName = prefs.getString(KEY_CHAT_BACKGROUND, ChatBackground.DEFAULT.name)
        return ChatBackground.valueOf(bgName ?: ChatBackground.DEFAULT.name)
    }

    /**
     * Sets chat background type
     */
    fun setChatBackground(background: ChatBackground) {
        prefs.edit().putString(KEY_CHAT_BACKGROUND, background.name).apply()
    }

    /**
     * Gets message bubble style
     */
    fun getMessageStyle(): MessageStyle {
        val styleName = prefs.getString(KEY_MESSAGE_STYLE, MessageStyle.CARD_3D.name)
        return MessageStyle.valueOf(styleName ?: MessageStyle.CARD_3D.name)
    }

    /**
     * Sets message bubble style
     */
    fun setMessageStyle(style: MessageStyle) {
        prefs.edit().putString(KEY_MESSAGE_STYLE, style.name).apply()
    }
    
    /**
     * Gets the custom tab order
     * Returns list of indices representing the order
     */
    fun getTabOrder(): List<Int> {
        val orderString = prefs.getString(KEY_TAB_ORDER, DEFAULT_TAB_ORDER) ?: DEFAULT_TAB_ORDER
        return orderString.split(",").mapNotNull { it.toIntOrNull() }
    }
    
    /**
     * Sets the custom tab order
     */
    fun setTabOrder(order: List<Int>) {
        val orderString = order.joinToString(",")
        prefs.edit().putString(KEY_TAB_ORDER, orderString).apply()
    }
    
    /**
     * Gets the video wallpaper URI
     */
    fun getVideoWallpaperUri(): String? {
        return prefs.getString(KEY_VIDEO_WALLPAPER_URI, null)
    }
    
    /**
     * Sets the video wallpaper URI
     */
    fun setVideoWallpaperUri(uri: String?) {
        prefs.edit().putString(KEY_VIDEO_WALLPAPER_URI, uri).apply()
    }
}

/**
 * Predefined theme presets
 */
enum class ThemePreset(
    val displayName: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val backgroundColor: Color = Color(0xFF0F0F1A),
    val surfaceColor: Color = Color(0xFF1A1A2E)
) {
    PURPLE("Morado", Color(0xFF7C3AED), Color(0xFF5B21B6)),
    BLUE("Azul", Color(0xFF3B82F6), Color(0xFF1E40AF)),
    GREEN("Verde", Color(0xFF10B981), Color(0xFF059669)),
    RED("Rojo", Color(0xFFEF4444), Color(0xFFDC2626)),
    PINK("Rosa", Color(0xFFEC4899), Color(0xFFDB2777)),
    ORANGE("Naranja", Color(0xFFF97316), Color(0xFFEA580C)),
    CYAN("Cian", Color(0xFF06B6D4), Color(0xFF0891B2)),
    TOXIC("Tóxico", Color(0xFF00FF00), Color(0xFF00CC00)), // Verde neón
    DARK("Oscuro", Color(0xFF1F2937), Color(0xFF111827)),
    GOLD("Dorado", Color(0xFFFBBF24), Color(0xFFF59E0B)),
    
    // ✨ NEW MOD THEMES - Dark aesthetic with red variants
    TOXICO_RED("Tóxico", Color(0xFFFF0000), Color(0xFFCC0000), Color(0xFF0A0A0A), Color(0xFF111111)),
    PERVERSO("Perverso", Color(0xFFCC0000), Color(0xFF990000), Color(0xFF080808), Color(0xFF0F0F0F)),
    CRIMSON_DARK("Crimson Dark", Color(0xFF8B0000), Color(0xFF660000), Color(0xFF050505), Color(0xFF0D0D0D)),
    NEON_RED("Neon Red", Color(0xFFFF1744), Color(0xFFD50000), Color(0xFF0A0A0A), Color(0xFF121212)),
    BLOOD_MOON("Blood Moon", Color(0xFFB71C1C), Color(0xFF8B0000), Color(0xFF060606), Color(0xFF100808))
}

/**
 * Chat background types
 */
enum class ChatBackground(val displayName: String) {
    DEFAULT("Por defecto"),
    SOLID_DARK("Oscuro sólido"),
    GRADIENT("Degradado"),
    VIDEO("Video de galería"),
    IMAGE("Imagen de galería")
}

/**
 * Message bubble styles
 */
enum class MessageStyle(val displayName: String) {
    CARD_3D("Carta 3D"),
    FLAT("Plano"),
    ROUNDED("Redondeado"),
    BUBBLE("Burbuja")
}
