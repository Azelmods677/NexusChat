package com.Azelmods.App.ui.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.Azelmods.App.data.security.tor.TorService
import com.Azelmods.App.ui.screens.ai.AIFeaturesScreenRedesigned
import com.Azelmods.App.ui.screens.auth.LoginScreen
import com.Azelmods.App.ui.screens.auth.RegisterScreen
import com.Azelmods.App.ui.screens.call.ActiveCallScreen
import com.Azelmods.App.ui.screens.call.IncomingCallScreen
import com.Azelmods.App.ui.screens.calls.CallsScreen
import com.Azelmods.App.ui.screens.chat.ChatScreen
import com.Azelmods.App.ui.screens.conversation.NewConversationScreen
import com.Azelmods.App.ui.screens.home.SearchScreen
import com.Azelmods.App.ui.screens.main.MainScreen
import com.Azelmods.App.ui.screens.premium.PremiumScreen
import com.Azelmods.App.ui.screens.profile.EditProfileScreen
import com.Azelmods.App.ui.screens.profile.ProfileScreen
import com.Azelmods.App.ui.screens.profile.ProfileViewerScreen
import com.Azelmods.App.ui.screens.settings.*
import com.Azelmods.App.ui.screens.splash.SplashScreen
import com.Azelmods.App.ui.screens.stories.CreateStoryScreen
import com.Azelmods.App.ui.screens.stories.StoryViewerScreen
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.net.URLDecoder

/**
 * Hilt EntryPoint for accessing TorService in Composables
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface TorServiceEntryPoint {
    fun torService(): TorService
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String = Screen.Splash.route
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth
        composable(Screen.Splash.route) {
            SplashScreen(navController = navController)
        }
        
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        
        composable(Screen.Register.route) {
            RegisterScreen(navController = navController)
        }
        
        // Main
        composable(Screen.Home.route) {
            MainScreen(navController = navController)
        }
        
        // Chats
        composable(
            route = Screen.Chat.route,
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            ChatScreen(contactId = chatId, navController = navController)
        }
        
        // Media Gallery
        composable(
            route = "media_gallery/{chatId}",
            arguments = listOf(navArgument("chatId") { type = NavType.StringType })
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId") ?: ""
            val chatViewModel: com.Azelmods.App.ui.screens.chat.ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
            val state by chatViewModel.state.collectAsState()
            
            LaunchedEffect(chatId) {
                chatViewModel.loadChat(chatId)
            }
            
            com.Azelmods.App.ui.screens.chat.MediaGalleryScreen(
                chatId = chatId,
                navController = navController,
                messages = state.messages
            )
        }
        
        composable(Screen.NewConversation.route) {
            NewConversationScreen(navController = navController)
        }
        
        // QR Scanner
        composable("qr_scanner") {
            com.Azelmods.App.ui.screens.conversation.QRScannerScreen(
                navController = navController,
                onQRScanned = { uid, username, name ->
                    // Navigate to chat with scanned user
                    navController.navigate("chat/$uid") {
                        popUpTo("qr_scanner") { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Search.route) {
            SearchScreen(navController = navController)
        }
        
        // Stories
        composable(
            route = Screen.StoryViewer.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val rawUserId = backStackEntry.arguments?.getString("userId") ?: ""
            val userId = try {
                // NavController should auto-decode, but we'll ensure it's decoded
                if (rawUserId.contains("%")) {
                    URLDecoder.decode(rawUserId, "UTF-8")
                } else {
                    rawUserId
                }
            } catch (e: Exception) {
                Log.e("NavGraph", "Failed to decode userId: $rawUserId", e)
                rawUserId
            }
            Log.d("NavGraph", "StoryViewer - Extracted userId: $userId")
            if (userId.isEmpty()) {
                Log.w("NavGraph", "StoryViewerScreen opened with empty userId")
            }
            StoryViewerScreen(navController = navController, userId = userId)
        }
        
        composable(Screen.CreateStory.route) {
            CreateStoryScreen(navController = navController)
        }
        
        // Calls
        composable(Screen.Calls.route) {
            CallsScreen(navController = navController)
        }
        
        composable(
            route = "incoming_call/{callId}/{callType}",
            arguments = listOf(
                navArgument("callId") { 
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("callType") { 
                    type = NavType.StringType
                    defaultValue = "audio"
                }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            val callType = backStackEntry.arguments?.getString("callType") ?: "audio"
            IncomingCallScreen(
                contactId = callId,
                callType = callType,
                navController = navController
            )
        }
        
        composable(
            route = "active_call/{callId}/{callType}",
            arguments = listOf(
                navArgument("callId") { 
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("callType") { 
                    type = NavType.StringType
                    defaultValue = "audio"
                }
            )
        ) { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId") ?: ""
            val callType = backStackEntry.arguments?.getString("callType") ?: "audio"
            ActiveCallScreen(
                contactId = callId,
                callType = callType,
                navController = navController
            )
        }
        
        // Profile
        composable(
            route = Screen.Profile.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileScreen(userId = userId, navController = navController)
        }
        
        composable(Screen.EditProfile.route) {
            EditProfileScreen(navController = navController)
        }
        
        // Profile Viewer - Fullscreen like Stories
        composable(
            route = Screen.ProfileViewer.route,
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            ProfileViewerScreen(navController = navController, userId = userId)
        }
        
        // Settings
        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }
        
        composable(Screen.SettingsAccount.route) {
            AccountSettingsScreen(navController = navController)
        }
        
        composable(Screen.SettingsPrivacy.route) {
            PrivacySecurityScreen(navController = navController)
        }
        
        composable(Screen.SettingsNotifications.route) {
            NotificationsScreen(navController = navController)
        }
        
        composable(Screen.SettingsAppearance.route) {
            AppearanceScreen(navController = navController)
        }
        
        composable("font_size") {
            com.Azelmods.App.ui.screens.settings.FontSizeScreen(navController = navController)
        }
        
        composable("wallpaper") {
            com.Azelmods.App.ui.screens.settings.WallpaperScreen(navController = navController)
        }
        
        composable("device_info") {
            com.Azelmods.App.ui.screens.settings.DeviceInfoScreen(navController = navController)
        }
        
        // Tutorial screens
        composable(
            route = "tutorial/{tutorialId}",
            arguments = listOf(navArgument("tutorialId") { type = NavType.StringType })
        ) { backStackEntry ->
            val tutorialId = backStackEntry.arguments?.getString("tutorialId") ?: ""
            com.Azelmods.App.ui.screens.tutorial.TutorialScreen(
                navController = navController,
                tutorialId = tutorialId
            )
        }
        
        composable(Screen.SettingsStorage.route) {
            StorageDataScreen(navController = navController)
        }
        
        composable(Screen.SettingsHelp.route) {
            HelpSupportScreen(navController = navController)
        }
        
        composable(Screen.SettingsAbout.route) {
            AboutScreen(navController = navController)
        }
        
        composable(Screen.Premium.route) {
            PremiumScreen(navController = navController)
        }
        
        composable(Screen.AiFeatures.route) {
            AIFeaturesScreenRedesigned(navController = navController)
        }
        
        // Azel IA - Advanced AI chat
        composable("azel_ai") {
            com.Azelmods.App.ui.screens.azelai.AzelAIScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Photo Viewer - Full screen image viewer with zoom
        composable(
            route = "photo_viewer?url={url}",
            arguments = listOf(navArgument("url") { type = NavType.StringType })
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("url") ?: ""
            com.Azelmods.App.ui.screens.viewer.PhotoViewerScreen(
                imageUrl = encodedUrl,
                navController = navController
            )
        }
        
        // Image Crop - Crop and position photos before upload
        composable(
            route = "image_crop?uri={uri}&type={type}",
            arguments = listOf(
                navArgument("uri") { type = NavType.StringType },
                navArgument("type") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("uri") ?: ""
            val photoType = backStackEntry.arguments?.getString("type") ?: "profile"
            com.Azelmods.App.ui.screens.viewer.ImageCropScreen(
                imageUri = encodedUri,
                photoType = photoType,
                navController = navController
            )
        }
        
        // Azel IA - Chatbot sin censura
        composable(Screen.AzelAI.route) {
            com.Azelmods.App.ui.screens.azelai.AzelAIScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Security & Advanced Features
        composable(Screen.Security.route) {
            com.Azelmods.App.ui.screens.security.SecurityScreen(
                navController = navController
            )
        }
        
        composable(Screen.TorControl.route) {
            com.Azelmods.App.ui.screens.security.TorControlScreen(
                navController = navController
            )
        }
        
        // Tor Browser - Anonymous browsing with .onion support (Embedded Tor)
        composable(Screen.TorBrowser.route) {
            val context = androidx.compose.ui.platform.LocalContext.current
            val torService = remember { 
                // Get TorService from Hilt dependency injection
                dagger.hilt.android.EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    TorServiceEntryPoint::class.java
                ).torService()
            }
            com.Azelmods.App.ui.screens.security.TorBrowserScreenNew(
                navController = navController,
                torService = torService
            )
        }
        
       
        // Ollama Chat
        composable("ai_agent") {
            com.Azelmods.App.ui.screens.ai.AIAgentScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        
        // Terminal
        composable("terminal") {
            com.Azelmods.App.ui.screens.terminal.TerminalScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // CyberSec Toolkit
        composable("cybersec") {
            com.Azelmods.App.ui.screens.cybersec.CyberSecScreen(
                onBack = { navController.popBackStack() }
            )
        }
        
        // Background Picker
        composable(
            route = "background_picker?chatId={chatId}",
            arguments = listOf(
                navArgument("chatId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val chatId = backStackEntry.arguments?.getString("chatId")
            com.Azelmods.App.ui.screens.background.BackgroundPickerScreen(
                navController = navController,
                chatId = chatId
            )
        }
        
        // Mod Screens
        composable("mod_home") {
            com.Azelmods.App.ui.screens.home.ModHomeScreen(
                navController = navController
            )
        }
        
        composable("internal_bot") {
            com.Azelmods.App.ui.screens.bot.InternalBotScreen(
                navController = navController
            )
        }
    }
}
