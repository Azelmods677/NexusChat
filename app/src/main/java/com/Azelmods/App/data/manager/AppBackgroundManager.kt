package com.Azelmods.App.data.manager

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.Azelmods.App.data.model.BackgroundConfig
import com.Azelmods.App.data.model.BackgroundType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.backgroundDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_background")

/**
 * Manages global app background state
 * 
 * Features:
 * - Persisted in DataStore
 * - StateFlow for reactive updates
 * - Supports all background types
 */
@Singleton
class AppBackgroundManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.backgroundDataStore
    
    companion object {
        private val KEY_TYPE = stringPreferencesKey("background_type")
        private val KEY_COLOR_HEX = stringPreferencesKey("color_hex")
        private val KEY_IMAGE_URI = stringPreferencesKey("image_uri")
        private val KEY_VIDEO_URI = stringPreferencesKey("video_uri")
        private val KEY_GRADIENT_COLORS = stringPreferencesKey("gradient_colors")
        private val KEY_GRADIENT_ANGLE = intPreferencesKey("gradient_angle")
        private val KEY_BLUR_RADIUS = floatPreferencesKey("blur_radius")
        private val KEY_OVERLAY_ALPHA = floatPreferencesKey("overlay_alpha")
    }
    
    /**
     * Current background configuration as Flow
     */
    val backgroundConfig: Flow<BackgroundConfig> = dataStore.data.map { prefs ->
        BackgroundConfig(
            type = prefs[KEY_TYPE]?.let { BackgroundType.valueOf(it) } ?: BackgroundType.NONE,
            colorHex = prefs[KEY_COLOR_HEX],
            imageUri = prefs[KEY_IMAGE_URI],
            videoUri = prefs[KEY_VIDEO_URI],
            gradientColors = prefs[KEY_GRADIENT_COLORS]?.split(",") ?: emptyList(),
            gradientAngle = prefs[KEY_GRADIENT_ANGLE] ?: 135,
            blurRadius = prefs[KEY_BLUR_RADIUS] ?: 10f,
            overlayAlpha = prefs[KEY_OVERLAY_ALPHA] ?: 0.4f
        )
    }
    
    /**
     * Save background configuration
     */
    suspend fun saveBackground(config: BackgroundConfig) {
        dataStore.edit { prefs ->
            prefs[KEY_TYPE] = config.type.name
            config.colorHex?.let { prefs[KEY_COLOR_HEX] = it }
            config.imageUri?.let { prefs[KEY_IMAGE_URI] = it }
            config.videoUri?.let { prefs[KEY_VIDEO_URI] = it }
            if (config.gradientColors.isNotEmpty()) {
                prefs[KEY_GRADIENT_COLORS] = config.gradientColors.joinToString(",")
            }
            prefs[KEY_GRADIENT_ANGLE] = config.gradientAngle
            prefs[KEY_BLUR_RADIUS] = config.blurRadius
            prefs[KEY_OVERLAY_ALPHA] = config.overlayAlpha
        }
    }
    
    /**
     * Clear background (set to NONE)
     */
    suspend fun clearBackground() {
        saveBackground(BackgroundConfig(type = BackgroundType.NONE))
    }
    
    /**
     * Set solid color background
     */
    suspend fun setSolidColor(colorHex: String, overlayAlpha: Float = 0.4f) {
        saveBackground(
            BackgroundConfig(
                type = BackgroundType.SOLID_COLOR,
                colorHex = colorHex,
                overlayAlpha = overlayAlpha
            )
        )
    }
    
    /**
     * Set image background
     */
    suspend fun setImageBackground(imageUri: String, overlayAlpha: Float = 0.4f) {
        saveBackground(
            BackgroundConfig(
                type = BackgroundType.IMAGE,
                imageUri = imageUri,
                overlayAlpha = overlayAlpha
            )
        )
    }
    
    /**
     * Set video background
     */
    suspend fun setVideoBackground(videoUri: String, overlayAlpha: Float = 0.4f) {
        saveBackground(
            BackgroundConfig(
                type = BackgroundType.VIDEO,
                videoUri = videoUri,
                overlayAlpha = overlayAlpha
            )
        )
    }
    
    /**
     * Set gradient background
     */
    suspend fun setGradientBackground(
        colors: List<String>,
        angle: Int = 135,
        overlayAlpha: Float = 0.4f
    ) {
        saveBackground(
            BackgroundConfig(
                type = BackgroundType.GRADIENT,
                gradientColors = colors,
                gradientAngle = angle,
                overlayAlpha = overlayAlpha
            )
        )
    }
}
