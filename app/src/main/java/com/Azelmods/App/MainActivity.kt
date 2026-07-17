package com.Azelmods.App

import android.Manifest
import android.content.Intent
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
import androidx.compose.runtime.mutableStateOf
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

    /**
     * Holds a pending call-screen navigation request coming from a notification
     * full-screen intent (incoming call). Compose observes this and navigates.
     */
    private val callNavRequest = mutableStateOf<CallNavRequest?>(null)

    data class CallNavRequest(
        val target: String,   // "incoming_call"
        val callId: String,
        val callType: String  // "audio" / "video"
    )

    @Inject
    lateinit var userPreferences: UserPreferences
    
    @Inject
    lateinit var appBackgroundManager: com.Azelmods.App.data.manager.AppBackgroundManager
    
    @Inject
    lateinit var databaseRepository: com.Azelmods.App.data.repository.RealtimeDatabaseRepository
    
    @Inject
    lateinit var sessionManager: com.Azelmods.App.data.session.SessionManager
    
    @Inject
    lateinit var appLockManager: com.Azelmods.App.data.security.AppLockManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // ✅ Enable edge-to-edge (modern approach - no systemuicontroller)
        enableEdgeToEdge()
        
        // ── Handle incoming-call navigation coming from a notification ──
        handleCallIntent(intent)
        handleDeepLink(intent)
        
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

                // Arranque seguro de la persistencia de Firebase: si la app
                // sobrevive la ventana de arranque sin crashear, marcamos el
                // inicio como sano para que el próximo arranque active la
                // persistencia con normalidad (ver FirebasePersistenceGuard).
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(6000)
                    com.Azelmods.App.data.firebase.FirebasePersistenceGuard.markHealthy(context)
                }

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
                
                // ── Session refresh automático cada 50 minutos ──
                LaunchedEffect(Unit) {
                    while (true) {
                        kotlinx.coroutines.delay(com.Azelmods.App.data.session.SessionManager.REFRESH_INTERVAL_MS)
                        sessionManager.refreshSession()
                    }
                }
                
                val wallpaperType by userPreferences.wallpaperType.collectAsState()
                val wallpaperValue by userPreferences.wallpaperValue.collectAsState()
                
                // ── App Lock State ──
                val isAppLocked by appLockManager.isLocked.collectAsState(initial = false)
                val showLockScreen = remember { mutableStateOf(false) }
                
                // Verificar si debe bloquearse al iniciar
                LaunchedEffect(Unit) {
                    if (appLockManager.shouldLockOnResume()) {
                        appLockManager.lock()
                        showLockScreen.value = true
                    }
                }
                
                // Sincronizar estado de bloqueo con UI
                LaunchedEffect(isAppLocked) {
                    showLockScreen.value = isAppLocked
                }

                com.Azelmods.App.ui.components.AppBackground(
                    backgroundManager = appBackgroundManager
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = Color.Transparent
                    ) {
                        if (showLockScreen.value) {
                            // Mostrar pantalla de bloqueo
                            com.Azelmods.App.ui.screens.security.AppLockScreen(
                                onUnlocked = {
                                    showLockScreen.value = false
                                }
                            )
                        } else {
                            // Contenido normal de la app
                            val navController = rememberNavController()
                            NavGraph(navController = navController)

                            // ── Handle deep links (nexuschat://chat/{id} or nexuschat://profile/{id}) ──
                            val pendingDeepLink by deepLinkNavRequest
                            LaunchedEffect(pendingDeepLink) {
                                pendingDeepLink?.let { req ->
                                    try {
                                        when (req.target) {
                                            "chat" -> {
                                                navController.navigate("chat/${req.id}")
                                            }
                                            "profile" -> {
                                                navController.navigate("profile/${req.id}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to navigate deep link: ${e.message}", e)
                                    }
                                    deepLinkNavRequest.value = null
                                }
                            }

                            // ── Navigate to the incoming-call screen when launched from a
                            //    call notification's full-screen intent. ──
                            val pendingCall by callNavRequest
                            LaunchedEffect(pendingCall) {
                                pendingCall?.let { req ->
                                    if (req.callId.isNotBlank()) {
                                        try {
                                            navController.navigate(
                                                "${req.target}/${req.callId}/${req.callType}"
                                            )
                                        } catch (e: Exception) {
                                            Log.e(TAG, "Failed to navigate to call screen: ${e.message}", e)
                                        }
                                    }
                                    callNavRequest.value = null
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleCallIntent(intent)
    }

    /**
     * Extracts incoming-call navigation extras (set by the FCM service / call
     * notification full-screen intent) and stores them so Compose can navigate.
     */
    private fun handleCallIntent(intent: Intent?) {
        try {
            val navTo = intent?.getStringExtra("navigate_to") ?: return
            if (navTo == "incoming_call") {
                val callId = intent.getStringExtra("callId") ?: return
                if (callId.isBlank()) return
                val callType = intent.getStringExtra("callType") ?: "audio"
                callNavRequest.value = CallNavRequest(
                    target = "incoming_call",
                    callId = callId,
                    callType = callType
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse call intent: ${e.message}", e)
        }
    }

    private fun handleDeepLink(intent: Intent?) {
        try {
            val data = intent?.data ?: return
            val host = data.host ?: return
            val path = data.pathSegments?.firstOrNull() ?: return
            when (host) {
                "chat" -> {
                    deepLinkNavRequest.value = DeepLinkNavRequest("chat", path)
                }
                "profile" -> {
                    deepLinkNavRequest.value = DeepLinkNavRequest("profile", path)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse deep link: ${e.message}", e)
        }
    }

    data class DeepLinkNavRequest(
        val target: String,
        val id: String
    )
    
    private val deepLinkNavRequest = mutableStateOf<DeepLinkNavRequest?>(null)

    override fun onResume() {        super.onResume()
        // Fix ACTION_HOVER_EXIT crash on older Android versions
        window.decorView.setOnHoverListener { view, motionEvent -> true }
        
        // ✅ Set user online when app resumes
        lifecycleScope.launch {
            runCatching { databaseRepository.updatePresence(isOnline = true) }
        }
        
        // ✅ Verificar si debe bloquearse la app
        lifecycleScope.launch {
            if (appLockManager.shouldLockOnResume()) {
                appLockManager.lock()
            }
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
        
        // ✅ Actualizar timestamp para auto-bloqueo
        appLockManager.updateLastActiveTime()
    }
}
