package com.Azelmods.App.ui.navigation

import android.util.Log
import java.net.URLEncoder

/**
 * Centralized navigation routes for Nexus Chat
 * All route constants are defined here to avoid typos and ensure consistency
 */
object NavRoutes {
    // Auth
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    
    // Main
    const val HOME = "home"
    const val MAIN = "main"
    
    // Chats
    const val CHAT = "chat/{chatId}"
    const val NEW_CONVERSATION = "new_conversation"
    const val NEW_GROUP = "new_group"
    const val SEARCH = "search"
    
    // Stories
    const val STORIES = "stories"
    const val STORY_VIEWER = "story_viewer/{userId}"
    const val CREATE_STORY = "create_story"
    
    // Calls
    const val CALLS = "calls"
    const val INCOMING_CALL = "incoming_call/{callId}"
    const val ACTIVE_CALL = "active_call/{callId}"
    const val CALL_HISTORY = "call_history"
    
    // Profile
    const val PROFILE = "profile/{userId}"
    const val PROFILE_MAIN = "profile_main"
    const val PROFILE_VIEWER = "profile_viewer/{userId}"
    const val EDIT_PROFILE = "edit_profile"
    
    // Settings
    const val SETTINGS = "settings"
    const val SETTINGS_ACCOUNT = "settings_account"
    const val SETTINGS_PRIVACY = "settings_privacy"
    const val SETTINGS_SECURITY = "settings_security"
    const val SETTINGS_NOTIFICATIONS = "settings_notifications"
    const val SETTINGS_APPEARANCE = "settings_appearance"
    const val SETTINGS_STORAGE = "settings_storage"
    const val SETTINGS_DATA = "settings_data"
    const val SETTINGS_HELP = "settings_help"
    const val SETTINGS_ABOUT = "settings_about"
    const val PREMIUM = "premium"
    const val AI_FEATURES = "ai_features"
    const val AZEL_AI = "azel_ai"
    
    // Security & Advanced Features
    const val SECURITY = "security"
    const val TOR_CONTROL = "tor_control"
    const val TOR_BROWSER = "tor_browser"
    
    // Helper functions
    fun chatRoute(chatId: String) = "chat/$chatId"
    
    fun storyViewerRoute(userId: String): String {
        if (userId.isEmpty()) {
            Log.e("NavRoutes", "storyViewerRoute called with empty userId")
            return "story_viewer/error"
        }
        return try {
            val encoded = URLEncoder.encode(userId, "UTF-8")
            Log.d("NavRoutes", "storyViewerRoute - Original userId: $userId")
            Log.d("NavRoutes", "storyViewerRoute - Generated route: story_viewer/$encoded")
            "story_viewer/$encoded"
        } catch (e: Exception) {
            Log.e("NavRoutes", "Failed to encode userId: $userId", e)
            "story_viewer/$userId" // Fallback to unencoded
        }
    }
    
    fun incomingCallRoute(callId: String) = "incoming_call/$callId"
    fun activeCallRoute(callId: String) = "active_call/$callId"
    fun profileRoute(userId: String) = "profile/$userId"
    fun profileViewerRoute(userId: String) = "profile_viewer/$userId"
}
