package com.Azelmods.App.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen(NavRoutes.SPLASH)
    object Login : Screen(NavRoutes.LOGIN)
    object Register : Screen(NavRoutes.REGISTER)
    object Home : Screen(NavRoutes.HOME)
    object Main : Screen(NavRoutes.MAIN)
    
    object Chat : Screen(NavRoutes.CHAT) {
        fun createRoute(chatId: String) = NavRoutes.chatRoute(chatId)
    }
    object NewConversation : Screen(NavRoutes.NEW_CONVERSATION)
    object NewGroup : Screen(NavRoutes.NEW_GROUP)
    object Search : Screen(NavRoutes.SEARCH)
    
    object Stories : Screen(NavRoutes.STORIES)
    object StoryViewer : Screen(NavRoutes.STORY_VIEWER) {
        fun createRoute(userId: String) = NavRoutes.storyViewerRoute(userId)
    }
    object CreateStory : Screen(NavRoutes.CREATE_STORY)
    
    object Calls : Screen(NavRoutes.CALLS)
    object IncomingCall : Screen(NavRoutes.INCOMING_CALL) {
        fun createRoute(callId: String) = NavRoutes.incomingCallRoute(callId)
    }
    object ActiveCall : Screen(NavRoutes.ACTIVE_CALL) {
        fun createRoute(callId: String) = NavRoutes.activeCallRoute(callId)
    }
    object CallHistory : Screen(NavRoutes.CALL_HISTORY)
    
    object Profile : Screen(NavRoutes.PROFILE) {
        fun createRoute(userId: String) = NavRoutes.profileRoute(userId)
    }
    object ProfileMain : Screen(NavRoutes.PROFILE_MAIN)
    object ProfileViewer : Screen(NavRoutes.PROFILE_VIEWER) {
        fun createRoute(userId: String) = NavRoutes.profileViewerRoute(userId)
    }
    object EditProfile : Screen(NavRoutes.EDIT_PROFILE)
    
    object Settings : Screen(NavRoutes.SETTINGS)
    object SettingsAccount : Screen(NavRoutes.SETTINGS_ACCOUNT)
    object SettingsPrivacy : Screen(NavRoutes.SETTINGS_PRIVACY)
    object SettingsSecurity : Screen(NavRoutes.SETTINGS_SECURITY)
    object SettingsNotifications : Screen(NavRoutes.SETTINGS_NOTIFICATIONS)
    object SettingsAppearance : Screen(NavRoutes.SETTINGS_APPEARANCE)
    object SettingsStorage : Screen(NavRoutes.SETTINGS_STORAGE)
    object SettingsData : Screen(NavRoutes.SETTINGS_DATA)
    object SettingsHelp : Screen(NavRoutes.SETTINGS_HELP)
    object SettingsAbout : Screen(NavRoutes.SETTINGS_ABOUT)
    object Premium : Screen(NavRoutes.PREMIUM)
    object AiFeatures : Screen(NavRoutes.AI_FEATURES)
    object AzelAI : Screen(NavRoutes.AZEL_AI)
    
    // Security & Advanced Features
    object Security : Screen(NavRoutes.SECURITY)
    object TorControl : Screen(NavRoutes.TOR_CONTROL)
    object TorBrowser : Screen(NavRoutes.TOR_BROWSER)
    
    // Background Picker
    object BackgroundPicker : Screen("background_picker?chatId={chatId}") {
        fun createRoute(chatId: String? = null) = if (chatId != null) {
            "background_picker?chatId=$chatId"
        } else {
            "background_picker"
        }
    }
    
    // Mod Screens
    object ModHome : Screen("mod_home")
    object InternalBot : Screen("internal_bot")
}
