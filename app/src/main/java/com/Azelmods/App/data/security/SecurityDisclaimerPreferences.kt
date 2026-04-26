package com.Azelmods.App.data.security

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages security disclaimer acceptance persistence
 * 
 * Requirements: 25.1, 25.4, 25.5
 */
@Singleton
class SecurityDisclaimerPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "security_disclaimer_prefs",
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val KEY_DISCLAIMER_ACCEPTED = "disclaimer_accepted"
        private const val KEY_ACCEPTANCE_TIMESTAMP = "acceptance_timestamp"
    }
    
    /**
     * Checks if user has accepted the security disclaimer
     */
    fun hasAcceptedDisclaimer(): Boolean {
        return prefs.getBoolean(KEY_DISCLAIMER_ACCEPTED, false)
    }
    
    /**
     * Saves disclaimer acceptance
     */
    fun setDisclaimerAccepted(accepted: Boolean) {
        prefs.edit()
            .putBoolean(KEY_DISCLAIMER_ACCEPTED, accepted)
            .putLong(KEY_ACCEPTANCE_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }
    
    /**
     * Gets the timestamp when disclaimer was accepted
     */
    fun getAcceptanceTimestamp(): Long {
        return prefs.getLong(KEY_ACCEPTANCE_TIMESTAMP, 0L)
    }
    
    /**
     * Clears disclaimer acceptance (for testing or reset)
     */
    fun clearAcceptance() {
        prefs.edit()
            .remove(KEY_DISCLAIMER_ACCEPTED)
            .remove(KEY_ACCEPTANCE_TIMESTAMP)
            .apply()
    }
}
