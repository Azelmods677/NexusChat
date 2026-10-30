package com.Azelmods.App.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.Azelmods.App.R
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.webrtc.CallAudioRouter
import com.Azelmods.App.webrtc.WebRTCManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.PeerConnection
import javax.inject.Inject

/**
 * Servicio en primer plano de la llamada en curso.
 *
 * Mantiene vivo el micrófono (y la cámara) con la app en segundo plano y muestra
 * la notificación "llamada en curso". Correcciones respecto a la versión anterior:
 *
 * - **"Colgar" de la notificación no colgaba.** Solo paraba el servicio: la
 *   PeerConnection seguía abierta, el otro seguía escuchando y la llamada nunca
 *   se cerraba en la base de datos. Ahora cierra la llamada de verdad.
 * - **"Silenciar" de la notificación no silenciaba.** Solo cambiaba su propio
 *   icono. Ahora actúa sobre la pista de audio compartida de [WebRTCManager], la
 *   misma que usa el botón de la pantalla, y ambos se ven el uno al otro.
 * - **El cronómetro contaba desde que empezaba a sonar.** Ahora muestra
 *   "Llamando…" / "Conectando…" y cuenta desde que el medio conecta.
 * - **Tocar la notificación mataba la llamada.** Abría la app con
 *   `FLAG_ACTIVITY_CLEAR_TASK`, que destruía la pantalla de la llamada (y con
 *   ella la conexión). Ahora trae la tarea existente al frente.
 */
@AndroidEntryPoint
class CallService : Service() {

    @Inject lateinit var webRTCManager: WebRTCManager
    @Inject lateinit var databaseRepository: RealtimeDatabaseRepository
    @Inject lateinit var audioRouter: CallAudioRouter

    private val TAG = "CallService"
    private val CHANNEL_ID = "call_service_channel"
    private val NOTIFICATION_ID = 1001

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var observeJob: Job? = null
    private var timerJob: Job? = null
    private var connectedAtElapsed: Long = 0L
    private var callId = ""
    private var callType = "audio"
    private var contactName = "Contacto"

    companion object {
        const val ACTION_START_CALL = "ACTION_START_CALL"
        /** Lo envía la app al colgar: solo hay que parar el servicio. */
        const val ACTION_END_CALL = "ACTION_END_CALL"
        /** Botón "Colgar" de la notificación: hay que cerrar la llamada entera. */
        const val ACTION_HANGUP = "ACTION_HANGUP"
        const val ACTION_TOGGLE_MUTE = "ACTION_TOGGLE_MUTE"
        const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        const val EXTRA_CALL_TYPE = "EXTRA_CALL_TYPE"
        const val EXTRA_CONTACT_NAME = "EXTRA_CONTACT_NAME"
        const val CALL_TYPE_AUDIO = "audio"
        const val CALL_TYPE_VIDEO = "video"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "CallService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL -> {
                callId = intent.getStringExtra(EXTRA_CALL_ID) ?: ""
                callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: CALL_TYPE_AUDIO
                contactName = intent.getStringExtra(EXTRA_CONTACT_NAME)?.takeIf { it.isNotBlank() } ?: "Contacto"
                startForegroundService()
            }
            ACTION_END_CALL -> stopEverything()
            ACTION_HANGUP -> hangUpFromNotification()
            ACTION_TOGGLE_MUTE -> webRTCManager.toggleMic()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        // ── Android 14+ (API 34+): starting a microphone/camera FGS REQUIRES the
        // matching runtime permission to be granted at this exact moment, otherwise
        // the system throws SecurityException and crashes the app. Guard against it. ──
        val hasMic = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val hasCamera = androidx.core.content.ContextCompat.checkSelfPermission(
            this, android.Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasMic || (callType == CALL_TYPE_VIDEO && !hasCamera)) {
            Log.e(TAG, "Missing runtime permissions for call FGS (mic=$hasMic, cam=$hasCamera). Aborting service.")
            stopSelf()
            return
        }

        val notification = buildNotification(statusText = "Llamando…", muted = false)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Only request camera type when we actually hold the camera permission.
                val foregroundServiceType = when {
                    callType == CALL_TYPE_VIDEO && hasCamera ->
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                    else ->
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                            ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                }
                startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // ForegroundServiceStartNotAllowedException / SecurityException, etc.
            Log.e(TAG, "Failed to start foreground service for call: ${e.message}", e)
            stopSelf()
            return
        }

        Log.d(TAG, "Foreground service started for call: $callId")
        observeCall()
    }

    /**
     * Reconstruye la notificación cuando cambia el estado de la conexión o del
     * micrófono, y arranca el cronómetro la primera vez que el medio conecta.
     */
    private fun observeCall() {
        observeJob?.cancel()
        connectedAtElapsed = 0L
        observeJob = scope.launch {
            combine(webRTCManager.connectionState, webRTCManager.micEnabled) { state, mic -> state to mic }
                .collect { (state, micEnabled) ->
                    if (state == PeerConnection.PeerConnectionState.CONNECTED && connectedAtElapsed == 0L) {
                        connectedAtElapsed = SystemClock.elapsedRealtime()
                        startTimer()
                    }
                    if (connectedAtElapsed == 0L || state != PeerConnection.PeerConnectionState.CONNECTED) {
                        notify(buildNotification(statusFor(state), muted = !micEnabled))
                    }
                }
        }
    }

    private fun statusFor(state: PeerConnection.PeerConnectionState?): String = when (state) {
        PeerConnection.PeerConnectionState.CONNECTED -> formatDuration(SystemClock.elapsedRealtime() - connectedAtElapsed)
        PeerConnection.PeerConnectionState.DISCONNECTED -> "Reconectando…"
        PeerConnection.PeerConnectionState.FAILED -> "Conexión perdida"
        PeerConnection.PeerConnectionState.CONNECTING -> "Conectando…"
        else -> "Llamando…"
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                if (webRTCManager.connectionState.value == PeerConnection.PeerConnectionState.CONNECTED) {
                    val elapsed = formatDuration(SystemClock.elapsedRealtime() - connectedAtElapsed)
                    notify(buildNotification(elapsed, muted = !webRTCManager.micEnabled.value))
                }
                delay(1000L)
            }
        }
    }

    private fun hangUpFromNotification() {
        val id = callId
        val durationSec = if (connectedAtElapsed > 0L) {
            (SystemClock.elapsedRealtime() - connectedAtElapsed) / 1000
        } else null
        // Primero se corta el medio (lo que el usuario espera al pulsar) y luego se
        // escribe el fin en la base de datos; la pantalla de la llamada lo detecta y
        // se cierra sola.
        webRTCManager.cleanup()
        audioRouter.stop()
        scope.launch {
            if (id.isNotBlank()) {
                withTimeoutOrNull(10_000L) {
                    runCatching { databaseRepository.endCall(id, durationSec = durationSec) }
                        .onFailure { Log.w(TAG, "No se pudo cerrar la llamada $id: ${it.message}") }
                }
            }
            stopEverything()
        }
    }

    private fun stopEverything() {
        observeJob?.cancel()
        timerJob?.cancel()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun notify(notification: Notification) {
        runCatching { getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification) }
    }

    /**
     * Formats elapsed milliseconds into HH:MM:SS or MM:SS.
     */
    private fun formatDuration(elapsedMs: Long): String {
        val totalSeconds = (elapsedMs / 1000).coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(java.util.Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(java.util.Locale.ROOT, "%02d:%02d", minutes, seconds)
        }
    }

    private fun buildNotification(statusText: String, muted: Boolean): Notification {
        // Trae al frente la tarea que ya existe, como el icono del launcher: NO se
        // limpia la pila (eso destruía la pantalla de la llamada).
        val openIntent = (packageManager.getLaunchIntentForPackage(packageName) ?: Intent()).apply {
            setPackage(null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val hangupPendingIntent = PendingIntent.getService(
            this, 1,
            Intent(this, CallService::class.java).apply { action = ACTION_HANGUP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mutePendingIntent = PendingIntent.getService(
            this, 2,
            Intent(this, CallService::class.java).apply { action = ACTION_TOGGLE_MUTE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isVideo = callType == CALL_TYPE_VIDEO
        val person = Person.Builder().setName(contactName).setImportant(true).build()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(contactName)
            .setContentText(
                (if (isVideo) "Videollamada" else "Llamada de voz") + " · " + statusText +
                    if (muted) " · micrófono silenciado" else ""
            )
            .setStyle(NotificationCompat.CallStyle.forOngoingCall(person, hangupPendingIntent))
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .addAction(
                if (muted) android.R.drawable.ic_lock_silent_mode_off else android.R.drawable.ic_lock_silent_mode,
                if (muted) "Activar micrófono" else "Silenciar",
                mutePendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Llamada en curso",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Muestra la llamada activa y permite colgar o silenciar"
            setSound(null, null)
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
        Log.d(TAG, "CallService destroyed")
    }
}
