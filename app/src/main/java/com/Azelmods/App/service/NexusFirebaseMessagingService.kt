package com.Azelmods.App.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.Azelmods.App.MainActivity
import com.Azelmods.App.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class NexusFirebaseMessagingService : FirebaseMessagingService() {
    
    companion object {
        private const val TAG = "NexusFCM"
        const val CHANNEL_MESSAGES = "nexus_messages"
        const val CHANNEL_STORIES  = "nexus_stories"
        const val CHANNEL_AI       = "nexus_ai"
    }
    
    // ── Token nuevo → guardar en Firebase ─────────────────
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseDatabase.getInstance()
                .getReference("users/$uid/fcmTokens/${Build.MODEL}")
                .setValue(token)
                .addOnSuccessListener {
                    Log.d(TAG, "FCM token saved to Firebase")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to save FCM token", e)
                }
        }
    }
    
    // ── Notificación recibida ──────────────────────────────
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")
        
        val data    = message.data
        val type    = data["type"] ?: "message"
        val title   = data["title"] ?: message.notification?.title ?: "Nexus"
        val body    = data["body"]  ?: message.notification?.body  ?: ""
        val chatId  = data["chatId"]
        val senderId = data["senderId"]
        
        Log.d(TAG, "Notification type: $type, title: $title")
        
        when (type) {
            "message" -> showMessageNotification(title, body, chatId, senderId)
            "story"   -> showStoryNotification(title, body)
            "ai"      -> showAINotification(title, body)
            else      -> showMessageNotification(title, body, chatId, senderId)
        }
    }
    
    // ── Notificación de mensaje ────────────────────────────
    private fun showMessageNotification(
        title: String, body: String,
        chatId: String?, senderId: String?
    ) {
        createChannels()
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigate_to", "chat")
            putExtra("chatId", chatId)
            putExtra("senderId", senderId)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        
        val notification = NotificationCompat.Builder(this, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setSound(sound)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()
        
        val manager = getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }
    
    // ── Notificación de story ──────────────────────────────
    private fun showStoryNotification(title: String, body: String) {
        createChannels()
        val notification = NotificationCompat.Builder(this, CHANNEL_STORIES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
    
    // ── Notificación de Azel IA ───────────────────────────
    private fun showAINotification(title: String, body: String) {
        createChannels()
        val notification = NotificationCompat.Builder(this, CHANNEL_AI)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
    
    // ── Crear canales (Android 8+) ─────────────────────────
    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE)
            as NotificationManager
        
        listOf(
            Triple(CHANNEL_MESSAGES, "Mensajes",
                   NotificationManager.IMPORTANCE_HIGH),
            Triple(CHANNEL_STORIES,  "Historias",
                   NotificationManager.IMPORTANCE_DEFAULT),
            Triple(CHANNEL_AI,       "Azel IA",
                   NotificationManager.IMPORTANCE_DEFAULT)
        ).forEach { (id, name, importance) ->
            if (manager.getNotificationChannel(id) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(id, name, importance)
                )
            }
        }
    }
}
