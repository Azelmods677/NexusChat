package com.Azelmods.App.data.preferences

import android.content.Context
import android.content.SharedPreferences
import com.Azelmods.App.data.tutorials.AppFeature
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tutorial Preferences
 * 
 * Tracks which tutorials the user has seen
 */
@Singleton
class TutorialPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "tutorial_prefs",
        Context.MODE_PRIVATE
    )
    
    /**
     * Check if user has seen a specific tutorial
     */
    fun hasSeenTutorial(feature: AppFeature): Boolean {
        return prefs.getBoolean("tutorial_${feature.name}", false)
    }
    
    /**
     * Mark tutorial as seen
     */
    fun markTutorialAsSeen(feature: AppFeature) {
        prefs.edit().putBoolean("tutorial_${feature.name}", true).apply()
    }
    
    /**
     * Reset all tutorials (for testing or user request)
     */
    fun resetAllTutorials() {
        prefs.edit().clear().apply()
    }
    
    /**
     * Check if this is first time opening the app
     */
    fun isFirstTimeUser(): Boolean {
        return !prefs.getBoolean("app_opened_before", false)
    }
    
    /**
     * Mark app as opened
     */
    fun markAppAsOpened() {
        prefs.edit().putBoolean("app_opened_before", true).apply()
    }
}
