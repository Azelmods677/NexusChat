package com.Azelmods.App.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.Azelmods.App.MainActivity
import com.Azelmods.App.R

/**
 * Notificación de llamada entrante: un único sitio que la crea y la retira.
 *
 * Problemas que resuelve respecto a la versión anterior:
 *
 * - **"Aceptar" no hacía nada en Android 12+.** El botón lanzaba un
 *   BroadcastReceiver que a su vez abría la actividad; Android 12 prohíbe ese
 *   "trampolín" desde notificaciones y la actividad no se abría. Ahora "Aceptar"
 *   abre directamente [MainActivity].
 * - **Sonaba una sola vez y no se retiraba nunca.** El tono de canal suena una vez
 *   salvo que la notificación sea insistente; y nadie la cancelaba al contestar,
 *   rechazar o cuando el otro colgaba, así que quedaba "sonando" fija en la
 *   barra. Ahora es `FLAG_INSISTENT` (repite el tono como un teléfono), se
 *   retira desde [cancelIncoming] en todos esos casos y, como red de seguridad,
 *   caduca sola a los [RING_TIMEOUT_MS] aunque el push de fin no llegue.
 * - **Estilo de llamada del sistema.** `CallStyle` muestra los botones de
 *   contestar/colgar con el aspecto nativo y la notificación sube de prioridad.
 */
object CallNotifications {

    private const val TAG = "CallNotifications"

    const val TAG_INCOMING = "incoming_calls"
    const val TAG_MISSED = "missed_calls"

    /** Debe coincidir con el tiempo de timbre del emisor (CallViewModel). */
    const val RING_TIMEOUT_MS = 50_000L

    const val EXTRA_NAVIGATE_TO = "navigate_to"
    const val NAV_INCOMING_CALL = "incoming_call"
    const val NAV_ACCEPT_CALL = "accept_call"

    fun notificationId(callId: String): Int = callId.hashCode()

    fun showIncoming(
        context: Context,
        callId: String,
        callerName: String,
        callType: String,
        callerPhoto: Bitmap?
    ) {
        val isVideo = callType.equals("video", ignoreCase = true)
        val typeArg = if (isVideo) "video" else "audio"
        val requestBase = notificationId(callId)

        val fullScreenIntent = PendingIntent.getActivity(
            context, requestBase,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAVIGATE_TO, NAV_INCOMING_CALL)
                putExtra("callId", callId)
                putExtra("callType", typeArg)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Directo a la actividad: nada de BroadcastReceiver intermedio.
        val answerIntent = PendingIntent.getActivity(
            context, requestBase + 1,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_NAVIGATE_TO, NAV_ACCEPT_CALL)
                putExtra("callId", callId)
                putExtra("callType", typeArg)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Rechazar sí puede ser un broadcast: no abre ninguna actividad.
        val declineIntent = PendingIntent.getBroadcast(
            context, requestBase + 2,
            Intent(context, CallNotificationReceiver::class.java).apply {
                action = CallNotificationReceiver.ACTION_DECLINE_CALL
                putExtra(CallNotificationReceiver.EXTRA_CALL_ID, callId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val caller = Person.Builder()
            .setName(callerName)
            .setImportant(true)
            .apply { callerPhoto?.let { setIcon(IconCompat.createWithBitmap(it)) } }
            .build()

        val notification = NotificationCompat.Builder(context, NexusFirebaseMessagingService.CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(callerName)
            .setContentText(if (isVideo) "Videollamada entrante" else "Llamada de voz entrante")
            .setLargeIcon(callerPhoto)
            .setStyle(NotificationCompat.CallStyle.forIncomingCall(caller, declineIntent, answerIntent))
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setTimeoutAfter(RING_TIMEOUT_MS)
            .setVibrate(longArrayOf(0, 1000, 800, 1000, 800, 1000))
            .build()
            .apply { flags = flags or Notification.FLAG_INSISTENT }

        runCatching {
            manager(context).notify(TAG_INCOMING, notificationId(callId), notification)
        }.onFailure { Log.e(TAG, "No se pudo mostrar la llamada entrante", it) }
    }

    fun cancelIncoming(context: Context, callId: String?) {
        if (callId.isNullOrBlank()) return
        runCatching { manager(context).cancel(TAG_INCOMING, notificationId(callId)) }
    }

    private fun manager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
