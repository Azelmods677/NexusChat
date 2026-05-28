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
import com.Azelmods.App.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CallService : Service() {

    private val TAG = "CallService"
    private val CHANNEL_ID = "call_service_channel"
    private val NOTIFICATION_ID = 1001

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var timerJob: Job? = null
    private var callStartElapsed: Long = 0L
    private var callId = ""
    private var callType = "audio"
    private var contactName = "Unknown"
    private var isMuted = false

    companion object {
        const val ACTION_START_CALL = "ACTION_START_CALL"
        const val ACTION_END_CALL = "ACTION_END_CALL"
        const val ACTION_TOGGLE_MUTE = "ACTION_TOGGLE_MUTE"
        const val EXTRA_CALL_ID = "EXTRA_CALL_ID"
        const val EXTRA_CALL_TYPE = "EXTRA_CALL_TYPE"
        const val EXTRA_CONTACT_NAME = "EXTRA_CONTACT_NAME"
        const val EXTRA_IS_MUTED = "EXTRA_IS_MUTED"
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
                callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: "audio"
                contactName = intent.getStringExtra(EXTRA_CONTACT_NAME) ?: "Unknown"
                startForegroundService()
            }
            ACTION_END_CALL -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_TOGGLE_MUTE -> {
                isMuted = intent.getBooleanExtra(EXTRA_IS_MUTED, !isMuted)
                Log.d(TAG, "Mute toggled: $isMuted")
                // Update notification with new mute state
                val elapsedMs = SystemClock.elapsedRealtime() - callStartElapsed
                val formatted = formatDuration(elapsedMs)
                val notification = buildNotification(formatted)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
        return START_NOT_STICKY
    }

    private fun startForegroundService() {
        // Record start time using elapsedRealtime (immune to system clock changes)
        callStartElapsed = SystemClock.elapsedRealtime()

        val notification = buildNotification(formatDuration(0))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val foregroundServiceType = when (callType) {
                "video" -> ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                else -> ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE or
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
            }
            startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        Log.d(TAG, "Foreground service started for call: $callId")
        startTimer()
    }

    /**
     * Starts a coroutine that updates the notification every second
     * with the elapsed call duration.
     */
    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (true) {
                delay(1000L)
                val elapsedMs = SystemClock.elapsedRealtime() - callStartElapsed
                val formatted = formatDuration(elapsedMs)
                Log.d(TAG, "Call timer: $formatted")

                val notification = buildNotification(formatted)
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, notification)
            }
        }
    }

    /**
     * Formats elapsed milliseconds into HH:MM:SS or MM:SS.
     */
    private fun formatDuration(elapsedMs: Long): String {
        val totalSeconds = elapsedMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    private fun buildNotification(timerText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val endCallIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_END_CALL
        }
        val endCallPendingIntent = PendingIntent.getService(
            this, 1, endCallIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val muteIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_TOGGLE_MUTE
            putExtra(EXTRA_IS_MUTED, !isMuted)
        }
        val mutePendingIntent = PendingIntent.getService(
            this, 2, muteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callTypeIcon = if (callType == "video") {
            android.R.drawable.ic_menu_camera
        } else {
            android.R.drawable.ic_menu_call
        }

        val callTypeText = if (callType == "video") "Video call" else "Audio call"
        val muteIcon = if (isMuted) android.R.drawable.ic_lock_silent_mode else android.R.drawable.ic_btn_speak_now
        val muteLabel = if (isMuted) "Unmute" else "Mute"

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("$callTypeText · $timerText")
            .setContentText("Talking with $contactName")
            .setSmallIcon(callTypeIcon)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .addAction(
                muteIcon,
                muteLabel,
                mutePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "End Call",
                endCallPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Call Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notification for ongoing calls with timer"
                setSound(null, null)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
        Log.d(TAG, "CallService destroyed")
    }
}
