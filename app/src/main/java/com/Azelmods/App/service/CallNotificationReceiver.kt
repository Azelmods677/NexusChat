package com.Azelmods.App.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.Azelmods.App.MainActivity
import com.Azelmods.App.data.model.CallStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * 📞 Call Notification Receiver
 *
 * Maneja las acciones de los botones en la notificación de llamada entrante:
 * - Accept: abre la pantalla de la llamada (IncomingCall)
 * - Decline: marca la llamada como DECLINED en Firebase y cierra la notificación
 */
class CallNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallNotificationReceiver"

        const val ACTION_ACCEPT_CALL  = "com.Azelmods.App.ACTION_ACCEPT_CALL"
        const val ACTION_DECLINE_CALL = "com.Azelmods.App.ACTION_DECLINE_CALL"
        const val EXTRA_CALL_ID       = "callId"
        const val EXTRA_CALL_TYPE     = "callType"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return
        val callType = intent.getStringExtra(EXTRA_CALL_TYPE) ?: "audio"

        when (intent.action) {
            ACTION_ACCEPT_CALL -> {
                Log.d(TAG, "✅ Accept call: $callId")
                acceptCall(context, callId, callType)
            }
            ACTION_DECLINE_CALL -> {
                Log.d(TAG, "❌ Decline call: $callId")
                declineCall(context, callId)
            }
        }
    }

    private fun acceptCall(context: Context, callId: String, callType: String) {
        // Abrir la app directamente en la pantalla IncomingCall
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "incoming_call")
            putExtra("callId", callId)
            putExtra("callType", callType)
        }
        context.startActivity(openIntent)

        // Cerrar la notificación de llamada entrante
        cancelNotification(context, callId)
    }

    private fun declineCall(context: Context, callId: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Marcar la llamada como DECLINED en Firebase
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val callRef = FirebaseDatabase.getInstance().getReference("calls/$callId")

                callRef.child("status").setValue(CallStatus.DECLINED.name).await()
                callRef.child("endTime").setValue(System.currentTimeMillis()).await()

                Log.d(TAG, "Call $callId declined successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decline call $callId", e)
            }

            // Cerrar la notificación
            cancelNotification(context, callId)
        }
    }

    private fun cancelNotification(context: Context, callId: String) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel("incoming_calls", callId.hashCode())
    }
}
