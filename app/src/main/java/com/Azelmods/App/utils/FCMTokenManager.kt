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
     * Save FCM token to Firebase for the current user
     */
    suspend fun saveFCMToken(userId: String? = null): Result<String> = runCatching {
        val uid = userId ?: FirebaseAuth.getInstance().currentUser?.uid
            ?: throw Exception("User not authenticated")
        
        val token = FirebaseMessaging.getInstance().token.await()
        
        FirebaseDatabase.getInstance()
            .getReference("users/$uid/fcmTokens/${Build.MODEL}")
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
            .getReference("users/$uid/fcmTokens/${Build.MODEL}")
            .removeValue()
            .await()
        
        Log.d(TAG, "FCM token deleted for user: $uid")
        Unit
    }.onFailure { e ->
        Log.e(TAG, "Failed to delete FCM token", e)
    }
}
