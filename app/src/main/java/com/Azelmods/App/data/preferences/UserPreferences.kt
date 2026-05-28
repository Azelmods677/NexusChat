package com.Azelmods.App.data.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * User Preferences Manager
 * Manages all app settings and preferences with real-time updates
 */
@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    // Account Settings
    private val _displayName = MutableStateFlow(prefs.getString(KEY_DISPLAY_NAME, "User") ?: "User")
    val displayName: StateFlow<String> = _displayName.asStateFlow()
    
    private val _username = MutableStateFlow(prefs.getString(KEY_USERNAME, "user") ?: "user")
    val username: StateFlow<String> = _username.asStateFlow()
    
    private val _bio = MutableStateFlow(prefs.getString(KEY_BIO, "Hey there! I'm using Nexus Chat") ?: "Hey there! I'm using Nexus Chat")
    val bio: StateFlow<String> = _bio.asStateFlow()
    
    private val _phoneNumber = MutableStateFlow(prefs.getString(KEY_PHONE, "") ?: "")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()
    
    private val _email = MutableStateFlow(prefs.getString(KEY_EMAIL, "") ?: "")
    val email: StateFlow<String> = _email.asStateFlow()
    
    // Privacy Settings
    private val _lastSeenEnabled = MutableStateFlow(prefs.getBoolean(KEY_LAST_SEEN, true))
    val lastSeenEnabled: StateFlow<Boolean> = _lastSeenEnabled.asStateFlow()
    
    private val _profilePhotoVisible = MutableStateFlow(prefs.getBoolean(KEY_PROFILE_PHOTO, true))
    val profilePhotoVisible: StateFlow<Boolean> = _profilePhotoVisible.asStateFlow()
    
    private val _readReceiptsEnabled = MutableStateFlow(prefs.getBoolean(KEY_READ_RECEIPTS, true))
    val readReceiptsEnabled: StateFlow<Boolean> = _readReceiptsEnabled.asStateFlow()
    
    private val _twoFactorEnabled = MutableStateFlow(prefs.getBoolean(KEY_TWO_FACTOR, false))
    val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()
    
    // Notification Settings
    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()
    
    private val _soundEnabled = MutableStateFlow(prefs.getBoolean(KEY_SOUND, true))
    val soundEnabled: StateFlow<Boolean> = _soundEnabled.asStateFlow()
    
    private val _vibrationEnabled = MutableStateFlow(prefs.getBoolean(KEY_VIBRATION, true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()
    
    private val _messagePreview = MutableStateFlow(prefs.getBoolean(KEY_MESSAGE_PREVIEW, true))
    val messagePreview: StateFlow<Boolean> = _messagePreview.asStateFlow()
    
    private val _groupNotifications = MutableStateFlow(prefs.getBoolean(KEY_GROUP_NOTIFICATIONS, true))
    val groupNotifications: StateFlow<Boolean> = _groupNotifications.asStateFlow()
    
    private val _notificationSound = MutableStateFlow(prefs.getString(KEY_NOTIFICATION_SOUND, "") ?: "")
    val notificationSound: StateFlow<String> = _notificationSound.asStateFlow()
    
    private val _notificationSoundName = MutableStateFlow(prefs.getString(KEY_NOTIFICATION_SOUND_NAME, "Default") ?: "Default")
    val notificationSoundName: StateFlow<String> = _notificationSoundName.asStateFlow()
    
    // Appearance Settings
    private val _darkModeEnabled = MutableStateFlow(prefs.getBoolean(KEY_DARK_MODE, true))
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()
    
    private val _accentColor = MutableStateFlow(prefs.getString(KEY_ACCENT_COLOR, "Purple") ?: "Purple")
    val accentColor: StateFlow<String> = _accentColor.asStateFlow()
    
    private val _fontSize = MutableStateFlow(prefs.getString(KEY_FONT_SIZE, "Medium") ?: "Medium")
    val fontSize: StateFlow<String> = _fontSize.asStateFlow()
    
    private val _wallpaperType = MutableStateFlow(prefs.getString(KEY_WALLPAPER_TYPE, "default") ?: "default")
    val wallpaperType: StateFlow<String> = _wallpaperType.asStateFlow()
    
    private val _wallpaperValue = MutableStateFlow(prefs.getString(KEY_WALLPAPER_VALUE, "") ?: "")
    val wallpaperValue: StateFlow<String> = _wallpaperValue.asStateFlow()
    
    // Storage Settings
    private val _autoDownloadPhotos = MutableStateFlow(prefs.getBoolean(KEY_AUTO_DOWNLOAD_PHOTOS, true))
    val autoDownloadPhotos: StateFlow<Boolean> = _autoDownloadPhotos.asStateFlow()
    
    private val _autoDownloadVideos = MutableStateFlow(prefs.getBoolean(KEY_AUTO_DOWNLOAD_VIDEOS, false))
    val autoDownloadVideos: StateFlow<Boolean> = _autoDownloadVideos.asStateFlow()
    
    private val _autoDownloadFiles = MutableStateFlow(prefs.getBoolean(KEY_AUTO_DOWNLOAD_FILES, false))
    val autoDownloadFiles: StateFlow<Boolean> = _autoDownloadFiles.asStateFlow()
    
    // Account Settings Update Functions
    fun updateDisplayName(name: String) {
        prefs.edit { putString(KEY_DISPLAY_NAME, name) }
        _displayName.value = name
    }
    
    fun updateUsername(username: String) {
        prefs.edit { putString(KEY_USERNAME, username) }
        _username.value = username
    }
    
    fun updateBio(bio: String) {
        prefs.edit { putString(KEY_BIO, bio) }
        _bio.value = bio
    }
    
    fun updatePhoneNumber(phone: String) {
        prefs.edit { putString(KEY_PHONE, phone) }
        _phoneNumber.value = phone
    }
    
    fun updateEmail(email: String) {
        prefs.edit { putString(KEY_EMAIL, email) }
        _email.value = email
    }
    
    // Privacy Settings Update Functions
    fun setLastSeenEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_LAST_SEEN, enabled) }
        _lastSeenEnabled.value = enabled
    }
    
    fun setProfilePhotoVisible(visible: Boolean) {
        prefs.edit { putBoolean(KEY_PROFILE_PHOTO, visible) }
        _profilePhotoVisible.value = visible
    }
    
    fun setReadReceiptsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_READ_RECEIPTS, enabled) }
        _readReceiptsEnabled.value = enabled
    }
    
    fun setTwoFactorEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_TWO_FACTOR, enabled) }
        _twoFactorEnabled.value = enabled
    }
    
    // Notification Settings Update Functions
    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_NOTIFICATIONS, enabled) }
        _notificationsEnabled.value = enabled
    }
    
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_SOUND, enabled) }
        _soundEnabled.value = enabled
    }
    
    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_VIBRATION, enabled) }
        _vibrationEnabled.value = enabled
    }
    
    fun setMessagePreview(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_MESSAGE_PREVIEW, enabled) }
        _messagePreview.value = enabled
    }
    
    fun setGroupNotifications(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_GROUP_NOTIFICATIONS, enabled) }
        _groupNotifications.value = enabled
    }
    
    fun setNotificationSound(uri: String, name: String) {
        prefs.edit {
            putString(KEY_NOTIFICATION_SOUND, uri)
            putString(KEY_NOTIFICATION_SOUND_NAME, name)
        }
        _notificationSound.value = uri
        _notificationSoundName.value = name
    }
    
    // Appearance Settings Update Functions
    fun setDarkModeEnabled(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }
        _darkModeEnabled.value = enabled
    }
    
    fun setAccentColor(color: String) {
        prefs.edit { putString(KEY_ACCENT_COLOR, color) }
        _accentColor.value = color
    }
    
    fun setFontSize(size: String) {
        prefs.edit { putString(KEY_FONT_SIZE, size) }
        _fontSize.value = size
    }
    
    fun setWallpaper(type: String, value: String) {
        prefs.edit {
            putString(KEY_WALLPAPER_TYPE, type)
            putString(KEY_WALLPAPER_VALUE, value)
        }
        _wallpaperType.value = type
        _wallpaperValue.value = value
    }
    
    // Storage Settings Update Functions
    fun setAutoDownloadPhotos(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_DOWNLOAD_PHOTOS, enabled) }
        _autoDownloadPhotos.value = enabled
    }
    
    fun setAutoDownloadVideos(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_DOWNLOAD_VIDEOS, enabled) }
        _autoDownloadVideos.value = enabled
    }
    
    fun setAutoDownloadFiles(enabled: Boolean) {
        prefs.edit { putBoolean(KEY_AUTO_DOWNLOAD_FILES, enabled) }
        _autoDownloadFiles.value = enabled
    }
    
    // Clear all data
    fun clearAllData() {
        prefs.edit { clear() }
        // Reset to defaults
        _displayName.value = "User"
        _username.value = "user"
        _bio.value = "Hey there! I'm using Nexus Chat"
        _lastSeenEnabled.value = true
        _notificationsEnabled.value = true
        _darkModeEnabled.value = true
        // ... reset all other values
    }
    
    companion object {
        private const val PREFS_NAME = "nexus_chat_preferences"
        
        // Account Keys
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_USERNAME = "username"
        private const val KEY_BIO = "bio"
        private const val KEY_PHONE = "phone_number"
        private const val KEY_EMAIL = "email"
        
        // Privacy Keys
        private const val KEY_LAST_SEEN = "last_seen_enabled"
        private const val KEY_PROFILE_PHOTO = "profile_photo_visible"
        private const val KEY_READ_RECEIPTS = "read_receipts_enabled"
        private const val KEY_TWO_FACTOR = "two_factor_enabled"
        
        // Notification Keys
        private const val KEY_NOTIFICATIONS = "notifications_enabled"
        private const val KEY_SOUND = "sound_enabled"
        private const val KEY_VIBRATION = "vibration_enabled"
        private const val KEY_MESSAGE_PREVIEW = "message_preview"
        private const val KEY_GROUP_NOTIFICATIONS = "group_notifications"
        private const val KEY_NOTIFICATION_SOUND = "notification_sound"
        private const val KEY_NOTIFICATION_SOUND_NAME = "notification_sound_name"
        
        // Appearance Keys
        private const val KEY_DARK_MODE = "dark_mode_enabled"
        private const val KEY_ACCENT_COLOR = "accent_color"
        private const val KEY_FONT_SIZE = "font_size"
        private const val KEY_WALLPAPER_TYPE = "wallpaper_type"
        private const val KEY_WALLPAPER_VALUE = "wallpaper_value"
        
        // Storage Keys
        private const val KEY_AUTO_DOWNLOAD_PHOTOS = "auto_download_photos"
        private const val KEY_AUTO_DOWNLOAD_VIDEOS = "auto_download_videos"
        private const val KEY_AUTO_DOWNLOAD_FILES = "auto_download_files"
    }
}
