package com.Azelmods.App.domain.usecase

import com.Azelmods.App.data.model.ChatSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for muting/unmuting chats.
 * 
 * Mute durations:
 * - 1 hour
 * - 8 hours
 * - 1 week
 * - Always
 */
class MuteChatUseCase @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    /**
     * Mutes a chat for specified duration
     * 
     * @param chatId The chat ID to mute
     * @param duration Duration in milliseconds (use ChatSettings constants)
     * @return Result with success or error
     */
    suspend operator fun invoke(chatId: String, duration: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            val muteUntil = if (duration == ChatSettings.MUTE_ALWAYS) {
                ChatSettings.MUTE_ALWAYS
            } else {
                System.currentTimeMillis() + duration
            }
            
            database.getReference("chat_settings")
                .child(userId)
                .child(chatId)
                .updateChildren(
                    mapOf(
                        "isMuted" to true,
                        "muteUntil" to muteUntil
                    )
                ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Unmutes a chat
     */
    suspend fun unmute(chatId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            database.getReference("chat_settings")
                .child(userId)
                .child(chatId)
                .updateChildren(
                    mapOf(
                        "isMuted" to false,
                        "muteUntil" to 0L
                    )
                ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
