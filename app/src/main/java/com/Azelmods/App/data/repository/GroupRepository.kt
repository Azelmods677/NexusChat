package com.Azelmods.App.data.repository

import com.Azelmods.App.data.model.GroupSettings
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for group-related operations.
 * 
 * Handles:
 * - Group settings (welcome messages, etc.)
 * - Member management
 * - Group configuration
 */
@Singleton
class GroupRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val groupsRef = database.getReference("groups")
    private val groupSettingsRef = database.getReference("group_settings")
    
    /**
     * Gets group settings
     */
    suspend fun getGroupSettings(groupId: String): GroupSettings? {
        return try {
            val snapshot = groupSettingsRef.child(groupId).get().await()
            snapshot.getValue(GroupSettings::class.java)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Updates group settings
     */
    suspend fun updateGroupSettings(settings: GroupSettings): Result<Unit> {
        return try {
            groupSettingsRef.child(settings.groupId)
                .setValue(settings.copy(updatedAt = System.currentTimeMillis()))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Observes new members joining a group
     */
    fun observeNewMembers(groupId: String): Flow<String> = callbackFlow {
        val membersRef = groupsRef.child(groupId).child("members")
        
        val listener = object : ValueEventListener {
            private var isFirstLoad = true
            private val existingMembers = mutableSetOf<String>()
            
            override fun onDataChange(snapshot: DataSnapshot) {
                if (isFirstLoad) {
                    // Store existing members on first load
                    snapshot.children.forEach { child ->
                        existingMembers.add(child.key ?: "")
                    }
                    isFirstLoad = false
                } else {
                    // Detect new members
                    snapshot.children.forEach { child ->
                        val memberId = child.key ?: ""
                        if (memberId.isNotEmpty() && !existingMembers.contains(memberId)) {
                            existingMembers.add(memberId)
                            trySend(memberId)
                        }
                    }
                }
            }
            
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        
        membersRef.addValueEventListener(listener)
        
        awaitClose {
            membersRef.removeEventListener(listener)
        }
    }
    
    /**
     * Checks if user is admin of a group
     */
    suspend fun isUserAdmin(groupId: String, userId: String): Boolean {
        return try {
            val settings = getGroupSettings(groupId)
            settings?.adminIds?.contains(userId) == true
        } catch (e: Exception) {
            false
        }
    }
}
