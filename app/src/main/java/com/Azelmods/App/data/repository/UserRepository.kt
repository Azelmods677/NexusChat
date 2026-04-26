package com.Azelmods.App.data.repository

import com.Azelmods.App.data.firebase.FirebaseManager
import com.Azelmods.App.data.model.User
import com.Azelmods.App.util.Resource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface UserRepository {
    suspend fun getUserById(userId: String): Resource<User>
    suspend fun updateProfile(user: User): Resource<Unit>
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Resource<Unit>
    suspend fun searchUsers(query: String): Resource<List<User>>
    fun observeUserStatus(userId: String): Flow<Boolean>
}

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : UserRepository {
    
    override suspend fun getUserById(userId: String): Resource<User> {
        return try {
            val snapshot = firebaseManager.database
                .getReference("users/$userId")
                .get()
                .await()
            
            val user = snapshot.getValue(User::class.java)
            if (user != null) {
                Resource.Success(user)
            } else {
                Resource.Error("User not found")
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to get user")
        }
    }
    
    override suspend fun updateProfile(user: User): Resource<Unit> {
        return try {
            firebaseManager.database
                .getReference("users/${user.uid}")
                .setValue(user)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update profile")
        }
    }
    
    override suspend fun updateOnlineStatus(userId: String, isOnline: Boolean): Resource<Unit> {
        return try {
            val userRef = firebaseManager.database.getReference("users/$userId")
            userRef.child("isOnline").setValue(isOnline).await()
            
            if (!isOnline) {
                userRef.child("lastSeen").setValue(System.currentTimeMillis()).await()
            }
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update online status")
        }
    }
    
    override suspend fun searchUsers(query: String): Resource<List<User>> {
        return try {
            val snapshot = firebaseManager.database
                .getReference("users")
                .orderByChild("displayName")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .get()
                .await()
            
            val users = mutableListOf<User>()
            snapshot.children.forEach { userSnapshot ->
                val user = userSnapshot.getValue(User::class.java)
                if (user != null) {
                    users.add(user)
                }
            }
            
            Resource.Success(users)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Search failed")
        }
    }
    
    override fun observeUserStatus(userId: String): Flow<Boolean> = callbackFlow {
        val userRef = firebaseManager.database.getReference("users/$userId/isOnline")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.getValue(Boolean::class.java) ?: false
                trySend(isOnline)
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(false)
            }
        }
        
        userRef.addValueEventListener(listener)
        
        awaitClose { userRef.removeEventListener(listener) }
    }
}
