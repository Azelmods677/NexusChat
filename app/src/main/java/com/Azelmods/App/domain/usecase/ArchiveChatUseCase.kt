package com.Azelmods.App.domain.usecase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for archiving/unarchiving chats.
 * 
 * Archived chats:
 * - Hidden from main chat list
 * - Can be accessed from archived section
 * - Automatically unarchived when new message arrives
 */
class ArchiveChatUseCase @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    /**
     * Archives or unarchives a chat
     * 
     * @param chatId The chat ID to archive/unarchive
     * @param archive True to archive, false to unarchive
     * @return Result with success or error
     */
    suspend operator fun invoke(chatId: String, archive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userId = auth.currentUser?.uid ?: return@withContext Result.failure(
                Exception("User not authenticated")
            )
            
            database.getReference("chat_settings")
                .child(userId)
                .child(chatId)
                .updateChildren(
                    mapOf(
                        "isArchived" to archive
                    )
                ).await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
