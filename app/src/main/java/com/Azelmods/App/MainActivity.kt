package com.Azelmods.App

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
    
    @Inject
    lateinit var databaseRepository: com.Azelmods.App.data.repository.RealtimeDatabaseRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // ✅ Enable edge-to-edge (modern approach - no systemuicontroller)
        enableEdgeToEdge()
        
        // Fix ACTION_HOVER_EXIT crash
        window.decorView.setOnHoverListener { _, _ -> true }
        
        // ✅ Set user online when app starts
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { databaseRepository.updatePresence(isOnline = true) }
        }
        
        setContent {
            NexusChatTheme(userPreferences = userPreferences) {
                val wallpaperType by userPreferences.wallpaperType.collectAsState()
                val wallpaperValue by userPreferences.wallpaperValue.collectAsState()
                
                Box(modifier = Modifier.fillMaxSize()) {
                    // Layer 1: Wallpaper background
                    when (wallpaperType) {
                        "image" -> {
                            wallpaperValue.takeIf { it.isNotEmpty() }?.let { uri ->
                                coil3.compose.AsyncImage(
                                    model = uri,
                                    contentDescription = null,
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    alpha = 0.35f
                                )
                            }
                        }
                        "color" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(wallpaperValue.toLongOrNull() ?: 0xFF0D0D1AL))
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF0D0D1A))
                            )
                        }
                    }
                    
                    // Layer 2: App content
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
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
        
        // ✅ Set user online when app resumes
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { databaseRepository.updatePresence(isOnline = true) }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // ✅ Set user offline when app goes to background
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            runCatching { databaseRepository.updatePresence(isOnline = false) }
        }
    }
}
