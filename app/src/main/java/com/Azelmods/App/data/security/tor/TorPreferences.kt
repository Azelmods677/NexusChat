package com.Azelmods.App.data.security.tor

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages persistence of Tor settings using SharedPreferences
 * 
 * Handles saving and restoring:
 * - Anonymous Mode enabled/disabled state
 * - Bridge configuration (obfs4 bridge addresses)
 * 
 * Requirements: 23.1, 23.2, 23.3, 23.4
 */
@Singleton
class TorPreferences @Inject constructor(
    context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val TAG = "TorPreferences"
        private const val PREFS_NAME = "tor_preferences"
        private const val KEY_ANONYMOUS_MODE_ENABLED = "anonymous_mode_enabled"
        private const val KEY_BRIDGE_ADDRESSES = "bridge_addresses"
        private const val KEY_USE_BRIDGES = "use_bridges"
        private const val BRIDGE_SEPARATOR = "|||"
    }
    
    /**
     * Saves Anonymous Mode preference
     * Requirement 23.1: Save Anonymous Mode preference to SharedPreferences
     */
    fun setAnonymousModeEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_ANONYMOUS_MODE_ENABLED, enabled)
            .apply()
        Log.d(TAG, "Saved Anonymous Mode state: $enabled")
    }
    
    /**
     * Gets Anonymous Mode preference
     * Requirement 23.3: Restore the previous Anonymous Mode state
     */
    fun isAnonymousModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_ANONYMOUS_MODE_ENABLED, false)
    }
    
    /**
     * Saves bridge configuration
     * Requirement 23.2: Save bridge configuration to SharedPreferences
     */
    fun saveBridgeConfiguration(bridges: List<String>) {
        val bridgesString = bridges.joinToString(BRIDGE_SEPARATOR)
        prefs.edit()
            .putString(KEY_BRIDGE_ADDRESSES, bridgesString)
            .putBoolean(KEY_USE_BRIDGES, bridges.isNotEmpty())
            .apply()
        Log.d(TAG, "Saved ${bridges.size} bridge address(es)")
    }
    
    /**
     * Gets saved bridge addresses
     * Requirement 23.3: Restore bridge configuration
     */
    fun getBridgeAddresses(): List<String> {
        val bridgesString = prefs.getString(KEY_BRIDGE_ADDRESSES, "") ?: ""
        return if (bridgesString.isEmpty()) {
            emptyList()
        } else {
            bridgesString.split(BRIDGE_SEPARATOR).filter { it.isNotBlank() }
        }
    }
    
    /**
     * Checks if bridges are configured
     */
    fun areBridgesConfigured(): Boolean {
        return prefs.getBoolean(KEY_USE_BRIDGES, false)
    }
    
    /**
     * Clears all Tor preferences
     */
    fun clearAll() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Cleared all Tor preferences")
    }
}
