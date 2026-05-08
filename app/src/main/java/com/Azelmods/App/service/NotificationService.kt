package com.Azelmods.App.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import androidx.core.graphics.drawable.IconCompat
import com.Azelmods.App.MainActivity
import com.Azelmods.App.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint

/**
 * Firebase Cloud Messaging service for handling notifications.
 * 
 * Features:
 * - Grouped notifications by conversation
 * - Quick reply from notification shade
 * - Mark as read action
 * - Sender avatar display
 */
@AndroidEntryPoint
class NotificationService : FirebaseMessagingService() {
    
    companion object {
        private const val CHANNEL_ID = "nexus_chat_messages"
        private const val CHANNEL_NAME = "Messages"
        internal const val KEY_TEXT_REPLY = "key_text_reply"
        private const val ACTION_REPLY = "com.Azelmods.App.ACTION_REPLY"
        private const val ACTION_MARK_READ = "com.Azelmods.App.ACTION_MARK_READ"
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Extract notification data
        val data = remoteMessage.data
        val chatId = data["chatId"] ?: return
        val senderId = data["senderId"] ?: return
        val senderName = data["senderName"] ?: "Unknown"
        val message = data["message"] ?: ""
        val senderPhotoUrl = data["senderPhotoUrl"]
        
        showNotification(chatId, senderId, senderName, message, senderPhotoUrl)
    }
    
    private fun showNotification(
        chatId: String,
        senderId: String,
        senderName: String,
        message: String,
        senderPhotoUrl: String?
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create person for sender
        val sender = Person.Builder()
            .setName(senderName)
            .setKey(senderId)
            .apply {
                // TODO: Load sender avatar from URL
                // setIcon(IconCompat.createWithBitmap(bitmap))
            }
            .build()
        
        // Create messaging style notification
        val messagingStyle = NotificationCompat.MessagingStyle(sender)
            .setConversationTitle(senderName)
            .addMessage(message, System.currentTimeMillis(), sender)
        
        // Open chat intent
        val openChatIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("chatId", chatId)
        }
        val openChatPendingIntent = PendingIntent.getActivity(
            this,
            chatId.hashCode(),
            openChatIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Quick reply action
        val replyLabel = "Reply"
        val remoteInput = RemoteInput.Builder(KEY_TEXT_REPLY)
            .setLabel(replyLabel)
            .build()
        
        val replyIntent = Intent(this, NotificationReplyReceiver::class.java).apply {
            putExtra("chatId", chatId)
            putExtra("senderId", senderId)
        }
        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            chatId.hashCode() + 1,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            replyLabel,
            replyPendingIntent
        )
            .addRemoteInput(remoteInput)
            .build()
        
        // Mark as read action
        val markReadIntent = Intent(this, NotificationMarkReadReceiver::class.java).apply {
            putExtra("chatId", chatId)
        }
        val markReadPendingIntent = PendingIntent.getBroadcast(
            this,
            chatId.hashCode() + 2,
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_notification,
            "Mark as read",
            markReadPendingIntent
        ).build()
        
        // Build notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(messagingStyle)
            .setContentIntent(openChatPendingIntent)
            .setAutoCancel(true)
            .setGroup(chatId) // Group by conversation
            .addAction(replyAction)
            .addAction(markReadAction)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        
        notificationManager.notify(chatId.hashCode(), notification)
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages"
                enableVibration(true)
                enableLights(true)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: Update FCM token in Firebase Realtime Database
    }
}

/**
 * Broadcast receiver for quick reply action
 */
class NotificationReplyReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra("chatId") ?: return
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(NotificationService.KEY_TEXT_REPLY)?.toString()
        
        if (replyText != null) {
            // TODO: Send message via repository
        }
    }
}

/**
 * Broadcast receiver for mark as read action
 */
class NotificationMarkReadReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra("chatId") ?: return
        
        // TODO: Mark messages as read via repository
        
        // Cancel notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(chatId.hashCode())
    }
}
