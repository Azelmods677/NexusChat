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
import androidx.compose.runtime.DisposableEffect
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

    /** Destino pedido por una notificación; Compose lo consume y navega. */
    data class CallNavRequest(val route: String)

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

    // Lazy: TorService solo se crea si el usuario dejó activado el modo anónimo.
    @Inject
    lateinit var torService: dagger.Lazy<com.Azelmods.App.data.security.tor.TorService>

    @Inject
    lateinit var torPreferences: com.Azelmods.App.data.security.tor.TorPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before super.onCreate()
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        
        // ✅ Enable edge-to-edge (modern approach - no systemuicontroller)
        enableEdgeToEdge()
        
        // ── Modo anónimo: si el usuario lo dejó activado, se restaura YA, antes de
        // que la app haga peticiones. Antes solo se restauraba al abrir la pantalla
        // de seguridad y, hasta entonces, todo salía sin Tor. ──
        if (torPreferences.isAnonymousModeEnabled()) {
            runCatching { torService.get().restoreIfEnabled(torPreferences) }
                .onFailure { Log.e(TAG, "No se pudo restaurar el modo anónimo", it) }
        }

        // ── Handle incoming-call navigation coming from a notification ──
        handleCallIntent(intent)
        handleDeepLink(intent)
        
        // Fix ACTION_HOVER_EXIT crash
        window.decorView.setOnHoverListener { view, motionEvent -> true }
        
        // ✅ Set user online when app starts
        lifecycleScope.launch {
            runCatching { databaseRepository.updatePresence(isOnline = true) }
        }

        // ✅ Guardar/actualizar el token FCM del usuario actual.
        // CAUSA RAÍZ (fix): NADIE llamaba a saveFCMToken(), así que
        // users/$uid/fcmTokens quedaba vacío y la Cloud Function no podía enviar
        // push de mensajes NI de llamadas entrantes → las llamadas no "sonaban"
        // en el otro dispositivo y las notificaciones de mensajes no llegaban.
        lifecycleScope.launch {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) {
                runCatching { com.Azelmods.App.utils.FCMTokenManager.saveFCMToken() }
            }
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

                            // ── Destinos pedidos por notificaciones (llamadas, mensajes…) ──
                            val pendingCall by callNavRequest
                            LaunchedEffect(pendingCall) {
                                pendingCall?.let { req ->
                                    try {
                                        navController.navigate(req.route) { launchSingleTop = true }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Failed to navigate to ${req.route}: ${e.message}", e)
                                    }
                                    callNavRequest.value = null
                                }
                            }

                            // La pantalla de llamada puede verse sobre la pantalla de
                            // bloqueo y encenderla, pero SOLO mientras se está en ella:
                            // activarlo para toda la actividad dejaría la app entera
                            // accesible con el teléfono bloqueado.
                            DisposableEffect(navController) {
                                val listener = androidx.navigation.NavController.OnDestinationChangedListener { _, destination, _ ->
                                    val route = destination.route.orEmpty()
                                    val inCall = route.startsWith("incoming_call") || route.startsWith("active_call")
                                    setShowWhenLocked(inCall)
                                    setTurnScreenOn(inCall)
                                }
                                navController.addOnDestinationChangedListener(listener)
                                onDispose { navController.removeOnDestinationChangedListener(listener) }
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
     * Traduce los extras de las notificaciones (llamada entrante, contestar desde
     * la notificación, mensaje, llamada perdida…) a una ruta de navegación.
     *
     * Antes solo se atendía "incoming_call": tocar la notificación de un mensaje
     * o de una llamada perdida abría la app en la pantalla de inicio sin más.
     */
    private fun handleCallIntent(intent: Intent?) {
        try {
            val navTo = intent?.getStringExtra("navigate_to") ?: return
            val callType = when (intent.getStringExtra("callType")?.lowercase()) {
                "video" -> "video"
                else -> "audio"
            }
            val route = when (navTo) {
                com.Azelmods.App.service.CallNotifications.NAV_INCOMING_CALL -> {
                    val callId = intent.getStringExtra("callId")?.takeIf { it.isNotBlank() } ?: return
                    "incoming_call/$callId/$callType"
                }
                com.Azelmods.App.service.CallNotifications.NAV_ACCEPT_CALL -> {
                    val callId = intent.getStringExtra("callId")?.takeIf { it.isNotBlank() } ?: return
                    com.Azelmods.App.service.CallNotifications.cancelIncoming(this, callId)
                    "active_call/$callId/$callType?isCaller=false"
                }
                "chat" -> {
                    val peer = intent.getStringExtra("senderId")?.takeIf { it.isNotBlank() } ?: return
                    "chat/$peer"
                }
                "call_back" -> {
                    val contactId = intent.getStringExtra("contactId")?.takeIf { it.isNotBlank() } ?: return
                    "active_call/$contactId/audio?isCaller=true"
                }
                "calls" -> "calls"
                "azel_ai" -> "azel_ai"
                else -> return
            }
            // Consumido: si la actividad se recrea no se vuelve a navegar.
            intent.removeExtra("navigate_to")
            callNavRequest.value = CallNavRequest(route)
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
