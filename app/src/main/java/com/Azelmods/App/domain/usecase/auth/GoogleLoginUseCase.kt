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
                // Derive a friendly name: displayName > email prefix > short uid
                val derivedName = firebaseUser.displayName?.trim()?.takeIf { it.isNotBlank() }
                    ?: firebaseUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
                        ?.replaceFirstChar { it.uppercaseChar() }
                    ?: "User ${firebaseUser.uid.take(4)}"
                val userData = mapOf(
                    "uid" to firebaseUser.uid,
                    "displayName" to derivedName,
                    // Ni el correo ni el teléfono van al perfil público, y el @usuario no se
                    // saca del correo: cualquiera podía leerlos. Van al índice privado.
                    "username" to "@user${firebaseUser.uid.take(8).lowercase()}",
                    "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                    "bio" to "¡Hola! Estoy usando NexusChat",
                    "createdAt" to ServerValue.TIMESTAMP,
                    "isOnline" to true
                )
                
                userRef.setValue(userData).await()
                com.Azelmods.App.data.profile.ContactIndex.register(firebaseUser.phoneNumber, firebaseUser.email)
                
                val newUser = User(
                    uid = firebaseUser.uid,
                    name = derivedName,
                    displayName = derivedName,
                    username = "@${firebaseUser.email?.substringBefore("@") ?: firebaseUser.uid.take(8)}",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString(),
                    bio = "¡Hola! Estoy usando NexusChat",
                    status = "¡Hola! Estoy usando NexusChat",
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
                    name = userData["displayName"] as? String ?: userData["name"] as? String
                        ?: firebaseUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() }
                        ?: "User ${firebaseUser.uid.take(4)}",
                    displayName = userData["displayName"] as? String ?: userData["name"] as? String
                        ?: firebaseUser.email?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() }
                        ?: "User ${firebaseUser.uid.take(4)}",
                    username = userData["username"] as? String ?: "@user",
                    email = userData["email"] as? String ?: "",
                    photoUrl = userData["photoUrl"] as? String,
                    bio = userData["bio"] as? String ?: "",
                    status = userData["status"] as? String ?: "¡Hola! Estoy usando NexusChat",
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
