package com.Azelmods.App.data.repository

import com.Azelmods.App.data.firebase.FirebaseManager
import com.Azelmods.App.data.model.User
import com.Azelmods.App.util.Resource
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface AuthRepository {
    suspend fun login(email: String, password: String): Resource<User>
    suspend fun register(email: String, password: String, displayName: String): Resource<User>
    suspend fun loginWithGoogle(idToken: String): Resource<User>
    suspend fun logout(): Resource<Unit>
    fun getCurrentUser(): User?
    fun isUserAuthenticated(): Boolean
}

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : AuthRepository {
    
    override suspend fun login(email: String, password: String): Resource<User> {
        return try {
            val result = firebaseManager.auth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                // Fetch user data from Realtime Database
                val userSnapshot = firebaseManager.database
                    .getReference("users/${firebaseUser.uid}")
                    .get()
                    .await()
                
                val user = userSnapshot.getValue(User::class.java)
                
                if (user != null) {
                    Resource.Success(user)
                } else {
                    Resource.Error("User data not found")
                }
            } else {
                Resource.Error("Authentication failed")
            }
        } catch (e: FirebaseAuthException) {
            Resource.Error(e.message ?: "Login failed")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }
    
    override suspend fun register(
        email: String,
        password: String,
        displayName: String
    ): Resource<User> {
        return try {
            val result = firebaseManager.auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                // Create user document in Realtime Database
                val user = User(
                    uid = firebaseUser.uid,
                    displayName = displayName,
                    email = email,
                    username = email.substringBefore("@"),
                    createdAt = System.currentTimeMillis()
                )
                
                firebaseManager.database
                    .getReference("users/${firebaseUser.uid}")
                    .setValue(user)
                    .await()
                
                Resource.Success(user)
            } else {
                Resource.Error("Registration failed")
            }
        } catch (e: FirebaseAuthException) {
            Resource.Error(e.message ?: "Registration failed")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }
    
    override suspend fun loginWithGoogle(idToken: String): Resource<User> {
        return try {
            val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
            val result = firebaseManager.auth.signInWithCredential(credential).await()
            val firebaseUser = result.user
            
            if (firebaseUser != null) {
                // Check if user exists in database
                val userSnapshot = firebaseManager.database
                    .getReference("users/${firebaseUser.uid}")
                    .get()
                    .await()
                
                val user = if (userSnapshot.exists()) {
                    userSnapshot.getValue(User::class.java)!!
                } else {
                    // Create new user in database
                    val newUser = User(
                        uid = firebaseUser.uid,
                        displayName = firebaseUser.displayName ?: "",
                        email = firebaseUser.email ?: "",
                        username = firebaseUser.email?.substringBefore("@") ?: "",
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        createdAt = System.currentTimeMillis()
                    )
                    
                    firebaseManager.database
                        .getReference("users/${firebaseUser.uid}")
                        .setValue(newUser)
                        .await()
                    
                    newUser
                }
                
                Resource.Success(user)
            } else {
                Resource.Error("Google Sign-In failed")
            }
        } catch (e: FirebaseAuthException) {
            Resource.Error(e.message ?: "Google Sign-In failed")
        } catch (e: Exception) {
            Resource.Error(e.message ?: "An error occurred")
        }
    }
    
    override suspend fun logout(): Resource<Unit> {
        return try {
            firebaseManager.auth.signOut()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Logout failed")
        }
    }
    
    override fun getCurrentUser(): User? {
        val firebaseUser = firebaseManager.auth.currentUser
        return if (firebaseUser != null) {
            User(
                uid = firebaseUser.uid,
                displayName = firebaseUser.displayName ?: "",
                email = firebaseUser.email ?: "",
                photoUrl = firebaseUser.photoUrl?.toString()
            )
        } else {
            null
        }
    }
    
    override fun isUserAuthenticated(): Boolean {
        return firebaseManager.isUserAuthenticated()
    }
}
