package com.Azelmods.App.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for adding emoji reactions to stories.
 * 
 * Features:
 * - Quick emoji reaction while viewing story
 * - Stores reaction in story reactions node
 * - One reaction per user per story
 */
class AddStoryReactionUseCase @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    /**
     * Adds an emoji reaction to a story
     * 
     * @param storyId The story ID to react to
     * @param emoji The emoji reaction
     * @return Result with success or error
     */
    suspend operator fun invoke(storyId: String, emoji: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            // Store reaction in stories/{storyId}/reactions/{userId}
            database.getReference("stories")
                .child(storyId)
                .child("reactions")
                .child(userId)
                .setValue(
                    mapOf(
                        "emoji" to emoji,
                        "timestamp" to System.currentTimeMillis()
                    )
                ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
