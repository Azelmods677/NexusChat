package com.Azelmods.App.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import androidx.core.net.toUri
import com.Azelmods.App.MainActivity
import com.Azelmods.App.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.Azelmods.App.data.preferences.UserPreferences
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import java.net.HttpURLConnection
import java.net.URL

/**
 * 🔔 Nexus Chat Notifications — Sistema de notificaciones premium
 *
 * Canales de notificación:
 * - nexus_messages   → mensajes de chat (Importance HIGH, con sonido y vibración)
 * - nexus_calls      → llamadas entrantes (Importance HIGH, ringtone largo)
 * - nexus_missed_calls → llamadas perdidas (Importance DEFAULT)
 * - nexus_stories    → historias nuevas (Importance DEFAULT)
 * - nexus_ai         → respuestas de Azel IA (Importance DEFAULT)
 */
@AndroidEntryPoint
class NexusFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userPreferences: UserPreferences

    companion object {
        private const val TAG = "NexusFCM"

        // ── Notification Channels ─────────────────────────────
        const val CHANNEL_MESSAGES     = "nexus_messages"
        const val CHANNEL_CALLS        = "nexus_calls"
        const val CHANNEL_MISSED_CALLS = "nexus_missed_calls"
        const val CHANNEL_STORIES      = "nexus_stories"
        const val CHANNEL_AI           = "nexus_ai"

        // ── Notification IDs ──────────────────────────────────
        private const val NOTIFICATION_GROUP_MESSAGES = "nexus_group_messages"
        private const val SUMMARY_NOTIFICATION_ID = 1000

        // ── RemoteInput key ────────────────────────────────────
        const val KEY_TEXT_REPLY = "key_text_reply"

        // ── Actions ────────────────────────────────────────────
        private const val ACTION_REPLY      = "com.Azelmods.App.ACTION_REPLY"
        private const val ACTION_ACCEPT_CALL = "com.Azelmods.App.ACTION_ACCEPT_CALL"
        private const val ACTION_DECLINE_CALL = "com.Azelmods.App.ACTION_DECLINE_CALL"
        private const val ACTION_MARK_READ   = "com.Azelmods.App.ACTION_MARK_READ"
    }

    // ── Token nuevo → guardar en Firebase ───────────────────────
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        FirebaseDatabase.getInstance()
            .getReference("users/$uid/fcmTokens/${Build.MODEL}")
            .setValue(token)
            .addOnSuccessListener { Log.d(TAG, "FCM token saved") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to save FCM token", e) }
    }

    // ── Notificación recibida ──────────────────────────────────
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        val data = message.data
        val type = data["type"] ?: "message"

        when (type) {
            "incoming_call" -> showIncomingCallNotification(data)
            "missed_call"   -> showMissedCallNotification(data)
            "message"       -> showMessageNotification(data)
            "story"         -> showStoryNotification(data)
            "ai"            -> showAINotification(data)
            else            -> showMessageNotification(data)
        }
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  INCOMING CALL NOTIFICATION  (como WhatsApp/Telegram)   ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun showIncomingCallNotification(data: Map<String, String>) {
        val callId      = data["callId"] ?: return
        val callerName  = data["callerName"] ?: "Anónimo"
        val callerPhoto = data["callerPhotoUrl"]
        val callType    = data["callType"] ?: "AUDIO"

        val callTypeLabel = if (callType == "VIDEO") "Video call" else "Audio call"

        // ── Full screen intent (abre IncomingCallScreen) ──
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "incoming_call")
            putExtra("callId", callId)
            putExtra("callType", callType.lowercase())
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, callId.hashCode(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── Accept action ──
        val acceptIntent = Intent(this, CallNotificationReceiver::class.java).apply {
            action = ACTION_ACCEPT_CALL
            putExtra("callId", callId)
            putExtra("callType", callType.lowercase())
        }
        val acceptPendingIntent = PendingIntent.getBroadcast(
            this, callId.hashCode() + 1, acceptIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // ── Decline action ──
        val declineIntent = Intent(this, CallNotificationReceiver::class.java).apply {
            action = ACTION_DECLINE_CALL
            putExtra("callId", callId)
        }
        val declinePendingIntent = PendingIntent.getBroadcast(
            this, callId.hashCode() + 2, declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // ── Call ringtone ──
        val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

        // ── Build notification ──
        val notification = NotificationCompat.Builder(this, CHANNEL_CALLS)
            .setSmallIcon(if (callType == "VIDEO") android.R.drawable.ic_menu_camera else android.R.drawable.ic_menu_call)
            .setContentTitle(callerName)
            .setContentText("Incoming $callTypeLabel…")
            .setSubText("Nexus Chat")
            .setLargeIcon(loadContactPhoto(callerPhoto))
            .setFullScreenIntent(fullScreenPendingIntent, true) // 👈 muestra como llamada entrante (full screen)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSound(ringtoneUri)
            .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000)) // patrón de llamada
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Decline",
                declinePendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_call,
                "Accept",
                acceptPendingIntent
            )
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // ID único basado en callId para poder actualizar/eliminar después
        manager.notify("incoming_calls", callId.hashCode(), notification)

        Log.d(TAG, "📞 Incoming call notification shown for: $callerName")
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  MISSED CALL NOTIFICATION                               ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun showMissedCallNotification(data: Map<String, String>) {
        val callerName = data["callerName"] ?: "Anónimo"
        val callerPhoto = data["callerPhotoUrl"]
        val callId = data["callId"] ?: ""

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "calls")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, callId.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MISSED_CALLS)
            .setSmallIcon(android.R.drawable.ic_menu_call)
            .setContentTitle("Missed call")
            .setContentText("From $callerName")
            .setSubText("Nexus Chat")
            .setLargeIcon(loadContactPhoto(callerPhoto))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify("missed_calls", callId.hashCode(), notification)
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  MESSAGE NOTIFICATION  (MessagingStyle + Quick Reply)   ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun showMessageNotification(data: Map<String, String>) {
        val chatId       = data["chatId"] ?: return
        val senderId     = data["senderId"] ?: return
        val senderName   = data["senderName"] ?: "Anónimo"
        val body         = data["body"] ?: data["message"] ?: ""
        val senderPhoto  = data["senderPhotoUrl"]
        val mediaType    = data["mediaType"]

        // ── Determinar texto a mostrar según el tipo de media ──
        val displayText = when (mediaType) {
            "IMAGE"    -> "📷 Photo"
            "VIDEO"    -> "🎥 Video"
            "AUDIO"    -> "🎤 Voice message"
            "DOCUMENT" -> "📄 Document"
            "LOCATION" -> "📍 Location"
            "STICKER"  -> "Sticker"
            else       -> body
        }

        // ── Person del remitente ──
        val sender = Person.Builder()
            .setName(senderName)
            .setKey(senderId)
            .apply {
                val avatar = loadContactPhoto(senderPhoto)
                if (avatar != null) {
                    setIcon(IconCompat.createWithBitmap(avatar))
                }
            }
            .build()

        // ── MessagingStyle (como WhatsApp/Telegram) ──
        val messagingStyle = NotificationCompat.MessagingStyle(sender)
            .setConversationTitle(senderName)
            .addMessage(displayText, System.currentTimeMillis(), sender)

        // ── Abrir chat ──
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "chat")
            putExtra("chatId", chatId)
            putExtra("senderId", senderId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            this, chatId.hashCode(), openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ── Quick Reply (RemoteInput) ──
        val replyLabel = "Reply"
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()

        val replyIntent = Intent(this, MessageNotificationReceiver::class.java).apply {
            action = ACTION_REPLY
            putExtra("chatId", chatId)
            putExtra("senderId", senderId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this, chatId.hashCode() + 10, replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            replyLabel,
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()

        // ── Mark as Read action ──
        val markReadIntent = Intent(this, MessageNotificationReceiver::class.java).apply {
            action = ACTION_MARK_READ
            putExtra("chatId", chatId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            this, chatId.hashCode() + 20, markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "Mark as read",
            markReadPendingIntent
        ).build()

        // ── Sound from preferences ──
        val customSoundUri = userPreferences.notificationSound.value.let {
            if (it.isNotEmpty()) it.toUri() else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        val isSoundEnabled = userPreferences.soundEnabled.value
        val isVibrationEnabled = userPreferences.vibrationEnabled.value

        // ── Build notification ──
        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(messagingStyle)
            .setContentIntent(openPendingIntent)
            .setAutoCancel(true)
            .setGroup(NOTIFICATION_GROUP_MESSAGES) // Agrupación por grupo de mensajes
            .setGroupSummary(false)
            .addAction(replyAction)
            .addAction(markReadAction)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setShortcutId(chatId)
            .setColor(0xFF7B5CFA.toInt())
            .apply {
                if (isSoundEnabled) setSound(customSoundUri)
                if (!isVibrationEnabled) setVibrate(longArrayOf(0))
            }
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Usar el chatId como ID para agrupar notificaciones del mismo chat
        // (Android agrupa automáticamente por ID si usamos el mismo)
        manager.notify(chatId.hashCode(), notification)

        // ── Summary notification (para agrupación estilo WhatsApp) ──
        showMessageSummary(manager)

        Log.d(TAG, "💬 Message notification shown from: $senderName")
    }

    /**
     * Muestra una notificación de resumen para el grupo de mensajes.
     * Así las notificaciones se agrupan como en WhatsApp.
     */
    private fun showMessageSummary(manager: NotificationManager) {
        val summary = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(NotificationCompat.InboxStyle()
                .setSummaryText("New messages"))
            .setGroup(NOTIFICATION_GROUP_MESSAGES)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()
        manager.notify(SUMMARY_NOTIFICATION_ID, summary)
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  STORY NOTIFICATION                                     ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun showStoryNotification(data: Map<String, String>) {
        val title = data["title"] ?: "New Story"
        val body  = data["body"]  ?: ""

        val notification = NotificationCompat.Builder(this, CHANNEL_STORIES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  AI NOTIFICATION                                        ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun showAINotification(data: Map<String, String>) {
        val title = data["title"] ?: "Azel AI"
        val body  = data["body"]  ?: ""

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "azel_ai")
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_AI)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  HELPERS                                                ║
    // ╚══════════════════════════════════════════════════════════╝

    /**
     * Carga la foto de perfil del contacto como Bitmap para la notificación.
     * Se ejecuta en segundo plano para no bloquear.
     */
    private fun loadContactPhoto(photoUrl: String?): Bitmap? {
        if (photoUrl.isNullOrBlank()) return null
        return try {
            val url = URL(photoUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connectTimeout = 3000
            connection.readTimeout = 3000
            connection.connect()
            val input = connection.inputStream
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to load contact photo", e)
            null
        }
    }

}
