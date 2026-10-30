package com.Azelmods.App.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.Azelmods.App.data.model.CallStatus
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 📞 Botón "Rechazar" de la notificación de llamada entrante.
 *
 * "Aceptar" ya no pasa por aquí: abre directamente la actividad (ver
 * [CallNotifications]), porque Android 12+ bloquea abrir actividades desde un
 * receptor lanzado por una notificación.
 */
class CallNotificationReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "CallNotificationReceiver"

        const val ACTION_DECLINE_CALL = "com.Azelmods.App.ACTION_DECLINE_CALL"
        const val EXTRA_CALL_ID       = "callId"

        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    override fun onReceive(context: Context, intent: Intent) {
        val callId = intent.getStringExtra(EXTRA_CALL_ID) ?: return
        if (intent.action != ACTION_DECLINE_CALL) return

        Log.d(TAG, "❌ Decline call: $callId")
        // Se retira YA: el usuario espera que el tono pare al pulsar.
        CallNotifications.cancelIncoming(context, callId)

        // goAsync mantiene vivo el proceso hasta que la escritura termina; sin él,
        // Android puede matar el receptor antes de que Firebase confirme.
        val pending = goAsync()
        scope.launch {
            try {
                withTimeoutOrNull(10_000L) {
                    FirebaseDatabase.getInstance().getReference("calls/$callId").updateChildren(
                        mapOf(
                            "status" to CallStatus.DECLINED.name,
                            "endTime" to ServerValue.TIMESTAMP,
                            "endedBy" to (FirebaseAuth.getInstance().currentUser?.uid ?: "")
                        )
                    ).await()
                }
                Log.d(TAG, "Call $callId declined successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to decline call $callId", e)
            } finally {
                pending.finish()
            }
        }
    }
}
