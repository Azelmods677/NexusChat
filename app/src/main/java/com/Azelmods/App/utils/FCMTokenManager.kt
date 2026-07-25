package com.Azelmods.App.utils

import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FCMTokenManager {

    private const val TAG = "FCMTokenManager"

    /**
     * Ruta de los tokens de push.
     *
     * Vive FUERA de `/users` a propósito. La lista de contactos necesita leer la
     * colección `/users` entera, y en Firebase el permiso de lectura CASCADEA hacia
     * abajo: una regla más estricta en `users/$uid/fcmTokens` no puede revocar el
     * permiso concedido en el padre. Con los tokens en su propio nodo raíz, cada
     * usuario sigue siendo el único que puede leer y escribir los suyos.
     *
     * Las Cloud Functions los leen con el Admin SDK, que ignora las reglas.
     */
    fun tokenPath(uid: String): String = "fcmTokens/$uid/${Build.MODEL}"

    /**
     * Save FCM token to Firebase for the current user
     */
    suspend fun saveFCMToken(userId: String? = null): Result<String> = runCatching {
        val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: throw Exception("User not authenticated")

        val token = FirebaseMessaging.getInstance().token.await()

        FirebaseDatabase.getInstance()
            .getReference(tokenPath(uid))
            .setValue(token)
            .await()

        Log.d(TAG, "FCM token saved successfully for user: $uid")
        token
    }.onFailure { e ->
        Log.e(TAG, "Failed to save FCM token", e)
    }

    /**
     * Delete FCM token from Firebase (on logout)
     */
    suspend fun deleteFCMToken(userId: String? = null): Result<Unit> = runCatching {
        val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: throw Exception("User not authenticated")

        FirebaseDatabase.getInstance()
            .getReference(tokenPath(uid))
            .removeValue()
            .await()

        // Limpia también el token del esquema anterior (users/$uid/fcmTokens) para no
        // dejar residuos tras la migración; si ya no existe, no pasa nada.
        runCatching {
            FirebaseDatabase.getInstance()
                .getReference("users/$uid/fcmTokens/${Build.MODEL}")
                .removeValue()
                .await()
        }

        Log.d(TAG, "FCM token deleted for user: $uid")
        Unit
    }.onFailure { e ->
        Log.e(TAG, "Failed to delete FCM token", e)
    }
}
