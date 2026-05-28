package com.Azelmods.App.ui.screens.settings

import com.Azelmods.App.data.manager.AppBackgroundManager
import com.Azelmods.App.data.model.BackgroundConfig
import com.Azelmods.App.data.model.BackgroundType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.preferences.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Shared ViewModel for all Settings screens
 * Provides real-time access to user preferences
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    val appBackgroundManager: AppBackgroundManager
) : ViewModel() {
    
    // Account Settings
    val displayName: StateFlow<String> = userPreferences.displayName
    val username: StateFlow<String> = userPreferences.username
    val bio: StateFlow<String> = userPreferences.bio
    val phoneNumber: StateFlow<String> = userPreferences.phoneNumber
    val email: StateFlow<String> = userPreferences.email
    
    // Privacy Settings
    val lastSeenEnabled: StateFlow<Boolean> = userPreferences.lastSeenEnabled
    val profilePhotoVisible: StateFlow<Boolean> = userPreferences.profilePhotoVisible
    val readReceiptsEnabled: StateFlow<Boolean> = userPreferences.readReceiptsEnabled
    val twoFactorEnabled: StateFlow<Boolean> = userPreferences.twoFactorEnabled
    
    // Notification Settings
    val notificationsEnabled: StateFlow<Boolean> = userPreferences.notificationsEnabled
    val soundEnabled: StateFlow<Boolean> = userPreferences.soundEnabled
    val vibrationEnabled: StateFlow<Boolean> = userPreferences.vibrationEnabled
    val messagePreview: StateFlow<Boolean> = userPreferences.messagePreview
    val groupNotifications: StateFlow<Boolean> = userPreferences.groupNotifications
    val notificationSound: StateFlow<String> = userPreferences.notificationSound
    val notificationSoundName: StateFlow<String> = userPreferences.notificationSoundName
    
    // Appearance Settings
    val darkModeEnabled: StateFlow<Boolean> = userPreferences.darkModeEnabled
    val accentColor: StateFlow<String> = userPreferences.accentColor
    val fontSize: StateFlow<String> = userPreferences.fontSize
    val wallpaperType: StateFlow<String> = userPreferences.wallpaperType
    val wallpaperValue: StateFlow<String> = userPreferences.wallpaperValue
    
    // Storage Settings
    val autoDownloadPhotos: StateFlow<Boolean> = userPreferences.autoDownloadPhotos
    val autoDownloadVideos: StateFlow<Boolean> = userPreferences.autoDownloadVideos
    val autoDownloadFiles: StateFlow<Boolean> = userPreferences.autoDownloadFiles
    
    // Account Update Functions
    fun updateDisplayName(name: String) {
        viewModelScope.launch {
            userPreferences.updateDisplayName(name)
        }
    }
    
    fun updateUsername(username: String) {
        viewModelScope.launch {
            userPreferences.updateUsername(username)
        }
    }
    
    fun updateBio(bio: String) {
        viewModelScope.launch {
            userPreferences.updateBio(bio)
        }
    }
    
    fun updatePhoneNumber(phone: String) {
        viewModelScope.launch {
            userPreferences.updatePhoneNumber(phone)
        }
    }
    
    fun updateEmail(email: String) {
        viewModelScope.launch {
            userPreferences.updateEmail(email)
        }
    }
    
    // Privacy Update Functions
    fun setLastSeenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setLastSeenEnabled(enabled)
        }
    }
    
    fun setProfilePhotoVisible(visible: Boolean) {
        viewModelScope.launch {
            userPreferences.setProfilePhotoVisible(visible)
        }
    }
    
    fun setReadReceiptsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setReadReceiptsEnabled(enabled)
        }
    }
    
    fun setTwoFactorEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setTwoFactorEnabled(enabled)
        }
    }
    
    // Notification Update Functions
    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNotificationsEnabled(enabled)
        }
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSoundEnabled(enabled)
        }
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setVibrationEnabled(enabled)
        }
    }
    
    fun setMessagePreview(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setMessagePreview(enabled)
        }
    }
    
    fun setGroupNotifications(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setGroupNotifications(enabled)
        }
    }
    
    fun setNotificationSound(uri: String, name: String) {
        viewModelScope.launch {
            userPreferences.setNotificationSound(uri, name)
        }
    }
    
    // Appearance Update Functions
    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setDarkModeEnabled(enabled)
        }
    }
    
    fun setAccentColor(color: String) {
        viewModelScope.launch {
            userPreferences.setAccentColor(color)
        }
    }
    
    fun setFontSize(size: String) {
        viewModelScope.launch {
            userPreferences.setFontSize(size)
        }
    }
    
    fun setWallpaper(type: String, value: String) {
        viewModelScope.launch {
            userPreferences.setWallpaper(type, value)
            
            // Sincronizar con el AppBackgroundManager para que se vea en toda la app
            when (type) {
                "image" -> appBackgroundManager.setImageBackground(value)
                "video" -> appBackgroundManager.setVideoBackground(value)
                "color" -> appBackgroundManager.setSolidColor(value)
                "default" -> appBackgroundManager.clearBackground()
                else -> appBackgroundManager.clearBackground()
            }
        }
    }

    fun setGradientWallpaper(colors: List<String>) {
        viewModelScope.launch {
            val colorsString = colors.joinToString(",")
            userPreferences.setWallpaper("gradient", colorsString)
            appBackgroundManager.setGradientBackground(colors)
        }
    }
    
    // Storage Update Functions
    fun setAutoDownloadPhotos(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoDownloadPhotos(enabled)
        }
    }
    
    fun setAutoDownloadVideos(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoDownloadVideos(enabled)
        }
    }
    
    fun setAutoDownloadFiles(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setAutoDownloadFiles(enabled)
        }
    }
    
    // Clear all data
    fun clearAllData() {
        viewModelScope.launch {
            userPreferences.clearAllData()
        }
    }
}
