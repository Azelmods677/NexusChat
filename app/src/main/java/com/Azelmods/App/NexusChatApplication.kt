package com.Azelmods.App

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.util.DebugLogger
import com.Azelmods.App.BuildConfig
import com.Azelmods.App.data.demo.DemoAccountManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okio.Path.Companion.toOkioPath
import javax.inject.Inject

@HiltAndroidApp
class NexusChatApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var demoAccountManager: DemoAccountManager

    /**
     * Application-wide coroutine scope backed by [Dispatchers.IO].
     *
     * Using IO instead of Main ensures that background work (Firebase calls,
     * demo-account seeding, etc.) never competes with the UI thread.
     * [SupervisorJob] prevents a failure in one child coroutine from
     * cancelling sibling coroutines.
     */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        
        // ══════════════════════════════════════════════════════════════
        // INSTALL GLOBAL ERROR HANDLER — ENTERPRISE GRADE 2026
        // ══════════════════════════════════════════════════════════════
        com.Azelmods.App.util.NexusCrashHandler.install(this)

        // Firebase is initialised automatically by its ContentProvider before
        // Application.onCreate() is called. If you want explicit, ordered
        // initialisation via the App Startup library, register
        // com.Azelmods.App.startup.FirebaseInitializer in AndroidManifest.xml
        // and remove the Firebase ContentProvider (tools:node="remove").

        // Crear canales de notificación al inicio (Android 8+)
        createNotificationChannels()

        initializeDemoAccount()
    }

    // ── Demo account bootstrap ────────────────────────────────────────────────

    /**
     * Seeds the demo account once a signed-in Firebase user is available.
     *
     * If the user is already authenticated at launch we call [DemoAccountManager]
     * immediately. Otherwise we register a one-shot [FirebaseAuth.AuthStateListener]
     * that removes itself as soon as the user signs in, preventing a memory leak
     * from a permanently-retained listener.
     */
    private fun initializeDemoAccount() {
        applicationScope.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    demoAccountManager.initializeDemoAccount(currentUser.uid)
                } else {
                    registerOneTimeDemoListener()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error during demo account initialisation", e)
            }
        }
    }

    /**
     * Registers a [FirebaseAuth.AuthStateListener] that self-removes after the
     * first sign-in event, avoiding a permanent strong reference to the listener.
     */
    private fun registerOneTimeDemoListener() {
        // Declared as a nullable var so the lambda can capture and clear it.
        var authListener: FirebaseAuth.AuthStateListener? = null

        authListener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser ?: return@AuthStateListener

            // Remove the listener immediately — we only need to act once.
            authListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
            authListener = null

            applicationScope.launch {
                try {
                    demoAccountManager.initializeDemoAccount(user.uid)
                } catch (e: Exception) {
                    Log.e(TAG, "Error initialising demo account after sign-in", e)
                }
            }
        }

        FirebaseAuth.getInstance().addAuthStateListener(requireNotNull(authListener) { "authListener no debería ser null" })
    }

    // ── Coil image loader ─────────────────────────────────────────────────────

    /**
     * Provides the application-wide [ImageLoader] used by Coil.
     *
     * Memory cache  : 25 % of available heap (Coil default heuristic).
     * Disk cache    : capped at 50 MB — generous enough for chat thumbnails
     *                 while staying well within typical device storage budgets.
     * DebugLogger   : enabled only in debug builds to avoid log spam in release.
     */
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder()
                    .maxSizePercent(context, percent = 0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache").toOkioPath())
                    .maxSizeBytes(50L * 1024L * 1024L) // 50 MB
                    .build()
            }
            .crossfade(true)
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }

    // ── Notification Channels (creados en App.onCreate para estar siempre disponibles) ──

    /**
     * Crea todos los canales de notificación que usa la app.
     * Se llama desde [onCreate] y es seguro llamarlo múltiples veces (es idempotente).
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = getSystemService(NotificationManager::class.java)

        val messageChannel = NotificationChannel(
            "nexus_messages", "Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "New messages from your chats"
            enableVibration(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
        }

        val callChannel = NotificationChannel(
            "nexus_calls", "Incoming Calls",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming call notifications"
            enableVibration(true)
            enableLights(true)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
            )
            setShowBadge(true)
        }

        val missedCallChannel = NotificationChannel(
            "nexus_missed_calls", "Missed Calls",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Missed call alerts"
            setShowBadge(true)
        }

        val storyChannel = NotificationChannel(
            "nexus_stories", "Stories",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "New stories from your contacts"
        }

        val aiChannel = NotificationChannel(
            "nexus_ai", "Azel AI",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Azel AI responses"
        }

        listOf(messageChannel, callChannel, missedCallChannel, storyChannel, aiChannel)
            .forEach { channel ->
                if (manager.getNotificationChannel(channel.id) == null) {
                    manager.createNotificationChannel(channel)
                }
            }

        Log.d(TAG, "✅ Notification channels created")
    }

    // ── Companion ─────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "NexusChatApplication"
    }
}
