package com.Azelmods.App.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Botones de la notificación de mensaje: responder y marcar como leído.
 *
 * Responder pasa por el mismo camino que escribir dentro del chat: antes la respuesta se
 * guardaba directamente en el servidor, sin cifrar.
 */
class MessageNotificationReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun databaseRepository(): RealtimeDatabaseRepository
    }

    companion object {
        private const val TAG = "MessageNotificationReceiver"

        const val ACTION_REPLY      = "com.Azelmods.App.ACTION_REPLY"
        const val ACTION_MARK_READ  = "com.Azelmods.App.ACTION_MARK_READ"
        const val EXTRA_CHAT_ID     = "chatId"
        const val KEY_TEXT_REPLY    = "key_text_reply"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val chatId = intent.getStringExtra(EXTRA_CHAT_ID) ?: return
        when (intent.action) {
            ACTION_REPLY -> handleReply(context, chatId, intent)
            ACTION_MARK_READ -> handleMarkAsRead(context, chatId)
        }
    }

    private fun handleReply(context: Context, chatId: String, intent: Intent) {
        val replyText = RemoteInput.getResultsFromIntent(intent)
            ?.getCharSequence(KEY_TEXT_REPLY)?.toString()?.trim()
        if (replyText.isNullOrBlank()) return

        val repository = EntryPointAccessors
            .fromApplication(context.applicationContext, Dependencies::class.java)
            .databaseRepository()
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.sendMessage(chatId, replyText)
                notificationManager(context).cancel(chatId.hashCode())
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo responder en $chatId", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun handleMarkAsRead(context: Context, chatId: String) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                val messagesRef = FirebaseDatabase.getInstance().getReference("chats/$chatId/messages")
                messagesRef.get().await().children.forEach { message ->
                    val messageId = message.key ?: return@forEach
                    if (message.child("senderId").getValue(String::class.java) != uid) {
                        messagesRef.child(messageId).child("readBy/$uid").setValue(true).await()
                    }
                }
                notificationManager(context).cancel(chatId.hashCode())
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo marcar como leído $chatId", e)
            } finally {
                pending.finish()
            }
        }
    }

    private fun notificationManager(context: Context) =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
}
