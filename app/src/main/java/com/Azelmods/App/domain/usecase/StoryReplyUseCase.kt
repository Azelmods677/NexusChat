package com.Azelmods.App.domain.usecase

import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for replying to stories.
 * 
 * Features:
 * - Quick reply from story viewer
 * - Creates or opens existing chat with story owner
 * - Sends message with story reference
 */
class StoryReplyUseCase @Inject constructor(
    private val databaseRepository: RealtimeDatabaseRepository,
    private val auth: FirebaseAuth
) {
    /**
     * Sends a reply to a story
     * 
     * @param storyOwnerId The user ID of the story owner
     * @param storyId The story ID being replied to
     * @param replyText The reply message text
     * @return Result with chat ID or error
     */
    suspend operator fun invoke(
        storyOwnerId: String,
        storyId: String,
        replyText: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val currentUserId = auth.currentUser?.uid ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            // Build chat ID (sorted alphabetically)
            val chatId = listOf(currentUserId, storyOwnerId).sorted().joinToString("_")
            
            // Send message with story reference
            databaseRepository.sendMessage(
                chatId = chatId,
                content = replyText,
                replyTo = "story:$storyId"
            )
            
            Result.success(chatId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
