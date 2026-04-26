package com.Azelmods.App

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.rememberNavController
import com.Azelmods.App.data.preferences.UserPreferences
import com.Azelmods.App.ui.navigation.NavGraph
import com.Azelmods.App.ui.theme.NexusChatTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var userPreferences: UserPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // ✅ Enable edge-to-edge (modern approach - no systemuicontroller)
        enableEdgeToEdge()
        
        // Fix ACTION_HOVER_EXIT crash
        window.decorView.setOnHoverListener { _, _ -> true }
        
        setContent {
            NexusChatTheme(userPreferences = userPreferences) {
                // Wrap entire app with AppBackground
                val appBackgroundManager = remember { 
                    com.Azelmods.App.data.manager.AppBackgroundManager(this)
                }
                
                com.Azelmods.App.ui.components.AppBackground(
                    backgroundManager = appBackgroundManager,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent // Transparent to show background
                    ) {
                        val navController = rememberNavController()
                        NavGraph(navController = navController)
                    }
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // Fix ACTION_HOVER_EXIT crash on older Android versions
        window.decorView.setOnHoverListener { _, _ -> true }
    }
}
