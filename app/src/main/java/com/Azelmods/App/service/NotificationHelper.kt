package com.Azelmods.App.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.Azelmods.App.MainActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 📞 NotificationHelper — Utilidad para mostrar notificaciones desde cualquier parte de la app.
 *
 * También expone un contador de llamadas perdidas para mostrar un badge
 * en el tab de Calls dentro de MainScreen.
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"

    // ── Contador de llamadas perdidas (para badge en MainScreen) ──
    private val _missedCallCount = MutableStateFlow(0)
    val missedCallCount: StateFlow<Int> = _missedCallCount.asStateFlow()

    /** Reincia el contador (llamar desde CallsScreen al abrir). */
    fun resetMissedCallCount() {
        _missedCallCount.value = 0
    }

    /**
     * Muestra una notificación de "Llamada perdida de [callerName]" con un botón
     * "Call back" que abre la pantalla de llamada al contacto.
     *
     * @param context       Context de la app (ApplicationContext funciona).
     * @param callId        ID de la llamada en Firebase.
     * @param callerName    Nombre de la persona que llamaba.
     * @param callerPhotoUrl URL opcional de la foto de perfil.
     * @param callerId      ID del usuario que llamó (para poder devolver la llamada).
     */
    fun showMissedCallNotification(
        context: Context,
        callId: String,
        callerName: String,
        callerPhotoUrl: String? = null,
        callerId: String? = null
    ) {
        Log.d(TAG, "Showing missed call notification for: $callerName (call: $callId)")

        // ── Open calls history ──
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "calls")
        }
        val pendingIntent = PendingIntent.getActivity(
            context, callId.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── Call back action ──
        val callBackPendingIntent = if (!callerId.isNullOrBlank()) {
            val callBackIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("navigate_to", "call_back")
                putExtra("contactId", callerId)
                putExtra("contactName", callerName)
            }
            PendingIntent.getActivity(
                context, callId.hashCode() + 100, callBackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        } else null

        // Load bitmap for large icon (same approach as NexusFirebaseMessagingService)
        val largeIcon = if (!callerPhotoUrl.isNullOrBlank()) {
            try {
                val url = java.net.URL(callerPhotoUrl)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.doInput = true
                conn.connectTimeout = 3000
                conn.readTimeout = 3000
                conn.connect()
                val input = conn.inputStream
                android.graphics.BitmapFactory.decodeStream(input)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load caller photo for notification", e)
                null
            }
        } else null

        val notificationBuilder = NotificationCompat.Builder(context, NexusFirebaseMessagingService.CHANNEL_MISSED_CALLS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Missed call")
            .setContentText("From $callerName")
            .setSubText("Nexus Chat")
            .setLargeIcon(largeIcon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)

        // Add "Call back" button if we have the caller's ID
        if (callBackPendingIntent != null) {
            notificationBuilder.addAction(
                android.R.drawable.ic_menu_call,
                "Call back",
                callBackPendingIntent
            )
        }

        // Ensure the channel exists (safe to call multiple times)
        ensureMissedCallsChannelExists(context)

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify("missed_calls", callId.hashCode(), notificationBuilder.build())

        // Increment missed call counter for badge
        _missedCallCount.value = _missedCallCount.value + 1
    }

    /**
     * Crea el canal de llamadas perdidas si aún no existe.
     * Llamar desde cualquier lugar antes de notificar.
     */
    private fun ensureMissedCallsChannelExists(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(NexusFirebaseMessagingService.CHANNEL_MISSED_CALLS) == null) {
            val channel = NotificationChannel(
                NexusFirebaseMessagingService.CHANNEL_MISSED_CALLS,
                "Missed Calls",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Missed call alerts"
                setShowBadge(true)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
