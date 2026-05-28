package com.Azelmods.App

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.Azelmods.App.data.preferences.UserPreferences
import com.Azelmods.App.ui.components.VideoWallpaper
import com.Azelmods.App.ui.navigation.NavGraph
import com.Azelmods.App.ui.theme.NexusChatTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    companion object {
        private const val TAG = "MainActivity"
    }

    @Inject
    lateinit var userPreferences: UserPreferences
    
    @Inject
    lateinit var appBackgroundManager: com.Azelmods.App.data.manager.AppBackgroundManager
    
    @Inject
    lateinit var databaseRepository: com.Azelmods.App.data.repository.RealtimeDatabaseRepository
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // ✅ Enable edge-to-edge (modern approach - no systemuicontroller)
        enableEdgeToEdge()
        
        // Fix ACTION_HOVER_EXIT crash
        window.decorView.setOnHoverListener { view, motionEvent -> true }
        
        // ✅ Set user online when app starts
        lifecycleScope.launch {
            runCatching { databaseRepository.updatePresence(isOnline = true) }
        }
        
        setContent {
            NexusChatTheme(userPreferences = userPreferences) {
                
                // ── Runtime permission: POST_NOTIFICATIONS (Android 13+) ──
                val context = LocalContext.current
                val notificationPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    Log.d(TAG, "POST_NOTIFICATIONS permission: ${if (isGranted) "GRANTED" else "DENIED"}")
                }
                
                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
                val wallpaperType by userPreferences.wallpaperType.collectAsState()
                val wallpaperValue by userPreferences.wallpaperValue.collectAsState()

                com.Azelmods.App.ui.components.AppBackground(
                    backgroundManager = appBackgroundManager
                ) {
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
        window.decorView.setOnHoverListener { view, motionEvent -> true }
        
        // ✅ Set user online when app resumes
        lifecycleScope.launch {
            runCatching { databaseRepository.updatePresence(isOnline = true) }
        }
    }
    
    override fun onDestroy() {
        // Clean up hover listener BEFORE super.onDestroy (window may detach)
        window.decorView.setOnHoverListener(null)
        super.onDestroy()
    }
    
    override fun onPause() {
        super.onPause()
        // ✅ Set user offline when app goes to background
        lifecycleScope.launch {
            runCatching { databaseRepository.updatePresence(isOnline = false) }
        }
    }
}
