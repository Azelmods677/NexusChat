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
import com.Azelmods.App.domain.usecase.DecryptMessageUseCase
import com.Azelmods.App.data.security.encryption.DecryptionResult
import com.Azelmods.App.data.security.encryption.MessageType
import com.Azelmods.App.i18n.tr
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit
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

    @Inject
    lateinit var decryptMessage: DecryptMessageUseCase

    @Inject
    lateinit var groupCrypto: com.Azelmods.App.data.security.encryption.GroupCrypto

    companion object {
        private const val TAG = "NexusFCM"
        private const val NETWORK_WAIT_SECONDS = 4L
        private const val DECRYPT_WAIT_MS = 4_000L

        // ── Notification Channels ─────────────────────────────
        const val CHANNEL_MESSAGES     = "nexus_messages"
        const val CHANNEL_CALLS        = "nexus_calls"
        const val CHANNEL_MISSED_CALLS = "nexus_missed_calls"
        const val CHANNEL_STORIES      = "nexus_stories"
        const val CHANNEL_AI           = "nexus_ai"
        const val CHANNEL_UPDATES      = "nexus_updates"
        private const val UPDATE_NOTIFICATION_ID = 1001

        // ── Notification IDs ──────────────────────────────────
        private const val NOTIFICATION_GROUP_MESSAGES = "nexus_group_messages"
        private const val SUMMARY_NOTIFICATION_ID = 1000

        // ── RemoteInput key ────────────────────────────────────
        const val KEY_TEXT_REPLY = "key_text_reply"

        // ── Actions ────────────────────────────────────────────
        private const val ACTION_REPLY      = "com.Azelmods.App.ACTION_REPLY"
        private const val ACTION_MARK_READ   = "com.Azelmods.App.ACTION_MARK_READ"
    }

    // ── Token nuevo → guardar en Firebase ───────────────────────
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")

        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Ruta centralizada en FCMTokenManager: los tokens viven fuera de /users
        // para que la lectura de la colección de usuarios no los exponga.
        FirebaseDatabase.getInstance()
            .getReference(com.Azelmods.App.utils.FCMTokenManager.tokenPath(uid))
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
            // Silenciar un chat no impedía nada: el ajuste se guardaba y ningún
            // sitio lo consultaba, así que las notificaciones seguían llegando
            // igual. La comprobación va aquí, en el cliente, porque el silencio
            // es una preferencia privada de cada usuario que el servidor no
            // tiene por qué conocer para decidir a quién enviar el push.
            //
            // Las llamadas NO se filtran a propósito: silenciar una
            // conversación silencia sus mensajes, no impide que te llamen.
            "message"       -> withChatNotMuted(data) { showMessageNotification(data) }
            "story"         -> showStoryNotification(data)
            "ai"            -> showAINotification(data)
            "app_update"    -> showUpdateNotification(data)
            "secret"        -> showPrivateNotice(tr("Chat secreto"), tr("Tienes un mensaje secreto"))
            "channel"       -> showPrivateNotice(data["title"] ?: tr("Canales"), data["body"].orEmpty())
            else            -> withChatNotMuted(data) { showMessageNotification(data) }
        }
    }

    /**
     * Ejecuta [onAllowed] salvo que el usuario haya silenciado ese chat.
     *
     * Ante cualquier duda —sin sesión, sin chatId, fallo de red al leer el
     * ajuste— se notifica. Perder un mensaje por un error de lectura es peor
     * que una notificación de más en un chat silenciado.
     */
    private fun withChatNotMuted(data: Map<String, String>, onAllowed: () -> Unit) {
        val chatId = data["chatId"]
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (chatId.isNullOrBlank() || uid.isNullOrBlank()) {
            onAllowed()
            return
        }

        // Este método ya corre en un hilo de fondo: esperar aquí permite descargar la foto y
        // descifrar después, cosa que en un callback del hilo principal fallaría.
        val silenced = runCatching {
            val snapshot = Tasks.await(
                FirebaseDatabase.getInstance().getReference("chat_settings/$uid/$chatId").get(),
                NETWORK_WAIT_SECONDS, TimeUnit.SECONDS
            )
            com.Azelmods.App.data.model.ChatSettings(
                chatId = chatId,
                isMuted = snapshot.child("isMuted").getValue(Boolean::class.java) ?: false,
                muteUntil = snapshot.child("muteUntil").getValue(Long::class.java) ?: 0L
            ).isCurrentlyMuted()
        }.getOrDefault(false)
        if (!silenced) onAllowed()
    }

    /**
     * El texto real del aviso. Un mensaje cifrado llega como texto cifrado: el servidor no
     * puede leerlo, así que se descifra aquí, con las mismas claves que usa el chat.
     */
    private fun messageText(data: Map<String, String>, chatId: String, senderId: String): String {
        val body = data["body"] ?: data["message"] ?: ""
        // Rótulo que enviaban las versiones antiguas del servidor.
        val fallback = if (body.isBlank() || body.startsWith("🔒")) tr("Nuevo mensaje") else body
        if (data["isEncrypted"] != "true") return fallback

        val payload = data["encryptedPayload"]?.takeIf { it.isNotBlank() } ?: runCatching {
            val messageId = data["messageId"].orEmpty()
            Tasks.await(
                FirebaseDatabase.getInstance()
                    .getReference("chats/$chatId/messages/$messageId/encryptedPayload").get(),
                NETWORK_WAIT_SECONDS, TimeUnit.SECONDS
            ).getValue(String::class.java)
        }.getOrNull() ?: return fallback

        data["groupKeyId"]?.let { keyId ->
            return runCatching {
                runBlocking { withTimeoutOrNull(DECRYPT_WAIT_MS) { groupCrypto.decrypt(chatId, keyId, payload) } }
            }.getOrNull() ?: fallback
        }
        return runCatching {
            val bytes = android.util.Base64.decode(payload, android.util.Base64.NO_WRAP)
            runBlocking {
                withTimeoutOrNull(DECRYPT_WAIT_MS) { decryptMessage(senderId, bytes, MessageType.WHISPER) }
            }
        }.getOrNull().let { (it as? DecryptionResult.Success)?.plaintext } ?: fallback
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  INCOMING CALL NOTIFICATION  (como WhatsApp/Telegram)   ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun showIncomingCallNotification(data: Map<String, String>) {
        val callId      = data["callId"] ?: return
        val callerName  = data["callerName"]?.takeIf { it.isNotBlank() } ?: "Anónimo"
        val callType    = data["callType"] ?: "AUDIO"

        // onMessageReceived ya corre en un hilo de fondo: descargar la foto aquí
        // no bloquea la interfaz.
        CallNotifications.showIncoming(
            context = this,
            callId = callId,
            callerName = callerName,
            callType = callType,
            callerPhoto = loadContactPhoto(data["callerPhotoUrl"])
        )
        Log.d(TAG, "📞 Incoming call notification shown for: $callerName")
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  MISSED CALL NOTIFICATION                               ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun showMissedCallNotification(data: Map<String, String>) {
        val callId = data["callId"] ?: ""
        // El emisor colgó o se agotó el tiempo: el tono de la llamada entrante debe
        // callar YA, esté o no la app abierta.
        CallNotifications.cancelIncoming(this, callId)

        // Mismo tag e id que usa la app cuando detecta la llamada perdida con la
        // pantalla abierta: si llegan las dos vías, la segunda reemplaza a la
        // primera en vez de duplicarla.
        NotificationHelper.showMissedCallNotification(
            context = this,
            callId = callId,
            callerName = data["callerName"]?.takeIf { it.isNotBlank() } ?: "Anónimo",
            callerPhotoUrl = data["callerPhotoUrl"],
            callerId = data["callerId"]
        )
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  MESSAGE NOTIFICATION  (MessagingStyle + Quick Reply)   ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun showMessageNotification(data: Map<String, String>) {
        val chatId       = data["chatId"] ?: return
        val senderId     = data["senderId"] ?: return
        // Un chat oculto no debe delatarse en la barra de notificaciones.
        com.Azelmods.App.data.chat.ChatOrganizer.init(applicationContext)
        val hiddenChat   = com.Azelmods.App.data.chat.ChatOrganizer.isHidden(chatId)
        val senderName   = if (hiddenChat) "NexusChat" else data["senderName"] ?: "Anónimo"
        // Quien está leyendo ese chat ya está viendo el mensaje.
        if (ActiveChat.isShowing(chatId)) return
        val body         = messageText(data, chatId, senderId).let { com.Azelmods.App.ui.screens.chat.pollPreview(it) ?: it }
        val senderPhoto  = if (hiddenChat) null else data["senderPhotoUrl"]
        val mediaType    = data["mediaType"]

        // ── Determinar texto a mostrar según el tipo de media ──
        val displayText = if (hiddenChat) tr("Mensaje nuevo") else when (mediaType) {
            "IMAGE"    -> "📷 Foto"
            "VIDEO"    -> "🎥 Video"
            "AUDIO"    -> "🎤 Mensaje de voz"
            "DOCUMENT" -> "📄 Documento"
            "LOCATION" -> "📍 Ubicación"
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
        // Los mensajes seguidos del mismo chat se acumulan en vez de reemplazarse.
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val previous = manager.activeNotifications.firstOrNull { it.id == chatId.hashCode() }?.notification
        val messagingStyle = (previous?.let {
            NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(it)
        } ?: NotificationCompat.MessagingStyle(Person.Builder().setName(tr("Tú")).build()))
            .setConversationTitle(data["groupName"]?.takeIf { it.isNotBlank() } ?: senderName)
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
        val replyLabel = "Responder"
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
            "Marcar como leído",
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
            .setColor(androidx.core.content.ContextCompat.getColor(this, com.Azelmods.App.R.color.notification_accent))
            .apply {
                if (isSoundEnabled) setSound(customSoundUri)
                if (!isVibrationEnabled) setVibrate(longArrayOf(0))
            }
            .build()

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
                .setSummaryText("Mensajes nuevos"))
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
        val title = data["title"] ?: "Nueva historia"
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

    /** Aviso simple, que abre la app. Un chat secreto nunca muestra de quién ni qué dice. */
    private fun showPrivateNotice(title: String, body: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val open = PendingIntent.getActivity(
            this, title.hashCode(),
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(
            title.hashCode(),
            NotificationCompat.Builder(this, CHANNEL_MESSAGES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setContentIntent(open)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        )
    }

    // ╔══════════════════════════════════════════════════════════╗
    // ║  APP UPDATE                                             ║
    // ╚══════════════════════════════════════════════════════════╝
    private fun showUpdateNotification(data: Map<String, String>) {
        val version = data["versionName"] ?: return
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            android.app.NotificationChannel(
                CHANNEL_UPDATES, tr("Actualizaciones"), NotificationManager.IMPORTANCE_DEFAULT
            )
        )
        val open = PendingIntent.getActivity(
            this, version.hashCode(),
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        manager.notify(
            UPDATE_NOTIFICATION_ID,
            NotificationCompat.Builder(this, CHANNEL_UPDATES)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(tr("NexusChat %s ya está aquí", version))
                .setContentText(tr("Toca para actualizar"))
                .setContentIntent(open)
                .setAutoCancel(true)
                .build()
        )
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
