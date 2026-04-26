package com.Azelmods.App.domain.usecase.auth

import com.Azelmods.App.data.model.User
import com.Azelmods.App.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class GoogleLoginUseCase @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    suspend operator fun invoke(idToken: String): Resource<User> {
        return try {
            require(idToken.isNotBlank()) { "ID token cannot be blank" }
            
            // Sign in with Google credential
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return Resource.Error("Authentication failed")
            
            // Check if user exists in Realtime Database
            val userRef = database.reference.child("users").child(firebaseUser.uid)
            val snapshot = userRef.get().await()
            
            // If user doesn't exist, create profile
            if (!snapshot.exists()) {
                val userData = mapOf(
                    "uid" to firebaseUser.uid,
                    "displayName" to (firebaseUser.displayName ?: "User"),
                    "username" to "@${firebaseUser.email?.substringBefore("@") ?: firebaseUser.uid.take(8)}",
                    "email" to (firebaseUser.email ?: ""),
                    "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                    "phoneNumber" to (firebaseUser.phoneNumber ?: ""),
                    "bio" to "Hey there! I'm using Nexus Chat",
                    "createdAt" to ServerValue.TIMESTAMP,
                    "isOnline" to true
                )
                
                userRef.setValue(userData).await()
                
                val newUser = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "User",
                    displayName = firebaseUser.displayName ?: "User",
                    username = "@${firebaseUser.email?.substringBefore("@") ?: firebaseUser.uid.take(8)}",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    bio = "Hey there! I'm using Nexus Chat",
                    status = "Hey there! I'm using Nexus Chat",
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis()
                )
                
                Resource.Success(newUser)
            } else {
                // Load existing user
                @Suppress("UNCHECKED_CAST")
                val userData = snapshot.value as? Map<String, Any> ?: return Resource.Error("Failed to load user data")
                
                // Update online status
                userRef.child("isOnline").setValue(true).await()
                
                val user = User(
                    uid = userData["uid"] as? String ?: firebaseUser.uid,
                    name = userData["displayName"] as? String ?: userData["name"] as? String ?: "User",
                    displayName = userData["displayName"] as? String ?: userData["name"] as? String ?: "User",
                    username = userData["username"] as? String ?: "@user",
                    email = userData["email"] as? String ?: "",
                    photoUrl = userData["photoUrl"] as? String,
                    bio = userData["bio"] as? String ?: "",
                    status = userData["status"] as? String ?: "Hey there! I'm using Nexus Chat",
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    createdAt = (userData["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                )
                
                Resource.Success(user)
            }
        } catch (e: Exception) {
            android.util.Log.e("GoogleLoginUseCase", "Error: ${e.message}", e)
            Resource.Error(e.message ?: "Google Sign-In failed")
        }
    }
}
