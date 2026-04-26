package com.Azelmods.App.domain.usecase

import com.Azelmods.App.data.model.ChatSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for pinning/unpinning chats.
 * 
 * Rules:
 * - Maximum 3 pinned chats per user
 * - Pinned chats appear at top of chat list
 */
class PinChatUseCase @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    /**
     * Pins or unpins a chat
     * 
     * @param chatId The chat ID to pin/unpin
     * @param pin True to pin, false to unpin
     * @return Result with success or error
     */
    suspend operator fun invoke(chatId: String, pin: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            val settingsRef = database.getReference("chat_settings")
                .child(userId)
                .child(chatId)
            
            if (pin) {
                // Check if user already has 3 pinned chats
                val pinnedChatsSnapshot = database.getReference("chat_settings")
                    .child(userId)
                    .orderByChild("isPinned")
                    .equalTo(true)
                    .get()
                    .await()
                
                val pinnedCount = pinnedChatsSnapshot.childrenCount
                if (pinnedCount >= 3) {
                    return@withContext Result.failure(
                        Exception("Maximum 3 pinned chats allowed")
                    )
                }
                
                // Pin the chat
                settingsRef.updateChildren(
                    mapOf(
                        "isPinned" to true,
                        "pinnedAt" to System.currentTimeMillis()
                    )
                ).await()
            } else {
                // Unpin the chat
                settingsRef.updateChildren(
                    mapOf(
                        "isPinned" to false,
                        "pinnedAt" to 0L
                    )
                ).await()
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
