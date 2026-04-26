package com.Azelmods.App.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseManager @Inject constructor(
    val auth: FirebaseAuth,
    val database: FirebaseDatabase,
    val storage: FirebaseStorage,
    val messaging: FirebaseMessaging
) {
    fun getCurrentUserId(): String? = auth.currentUser?.uid
    
    fun isUserAuthenticated(): Boolean = auth.currentUser != null
}
