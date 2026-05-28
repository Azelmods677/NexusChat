package com.Azelmods.App.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 💬 Message Notification Receiver
 *
 * Maneja las acciones de los botones en la notificación de mensaje:
 * - Reply: envía la respuesta a través de Firebase Realtime Database
 * - Mark as Read: marca los mensajes como leídos y cancela la notificación
 */
class MessageNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "MessageNotificationReceiver"

        const val ACTION_REPLY      = "com.Azelmods.App.ACTION_REPLY"
        const val ACTION_MARK_READ  = "com.Azelmods.App.ACTION_MARK_READ"
        const val EXTRA_CHAT_ID     = "chatId"
        const val EXTRA_SENDER_ID   = "senderId"
        const val KEY_TEXT_REPLY    = "key_text_reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: return

        when (intent.action) {
            ACTION_REPLY -> {
                val senderId = intent.getStringExtra(EXTRA_SENDER_ID) ?: return
                handleReply(context, chatId, senderId, intent)
            }
            ACTION_MARK_READ -> {
                handleMarkAsRead(context, chatId)
            }
        }
    }

    private fun handleReply(context: Context, chatId: String, senderId: String, intent: Intent) {
        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        val replyText = remoteInput?.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim()

        if (replyText.isNullOrBlank()) {
            Log.w(TAG, "Empty reply, ignoring")
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                    Log.e(TAG, "User not authenticated, cannot reply")
                    return@launch
                }

                val messageRef = FirebaseDatabase.getInstance()
                    .getReference("chats/$chatId/messages")
                    .push()

                val messageData = mapOf(
                    "messageId" to (messageRef.key ?: ""),
                    "senderId" to uid,
                    "receiverId" to senderId,
                    "content" to replyText,
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "SENT"
                )

                messageRef.setValue(messageData).await()

                // Actualizar último mensaje del chat
                val chatRef = FirebaseDatabase.getInstance()
                    .getReference("chats/$chatId")
                chatRef.child("lastMessage").setValue(replyText).await()
                chatRef.child("lastMessageTime").setValue(System.currentTimeMillis()).await()
                chatRef.child("lastMessageSenderId").setValue(uid).await()

                // Cerrar la notificación después de responder
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(chatId.hashCode())

                Log.d(TAG, "✅ Reply sent to $chatId: $replyText")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send reply to $chatId", e)
            }
        }
    }

    private fun handleMarkAsRead(context: Context, chatId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch

                // Marcar mensajes como leídos (actualizar todos los mensajes no leídos en este chat)
                val messagesRef = FirebaseDatabase.getInstance()
                    .getReference("chats/$chatId/messages")

                val snapshot = messagesRef.get().await()
                snapshot.children.forEach { msgSnapshot ->
                    val msgId = msgSnapshot.key ?: return@forEach
                    val senderId = msgSnapshot.child("senderId").getValue(String::class.java)
                    // Solo marcar como leídos los mensajes de OTROS usuarios (no los propios)
                    if (senderId != uid) {
                        messagesRef.child(msgId).child("readBy/$uid").setValue(true).await()
                    }
                }

                // Cerrar la notificación
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(chatId.hashCode())

                Log.d(TAG, "✅ Messages marked as read in $chatId")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark messages as read in $chatId", e)
            }
        }
    }
}
