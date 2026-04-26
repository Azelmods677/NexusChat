package com.Azelmods.App.domain.usecase

import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.data.repository.GroupRepository
import com.Azelmods.App.data.repository.MessageRepository
import com.Azelmods.App.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Use case for sending automatic welcome messages when new members join a group.
 * 
 * Features:
 * - Checks if welcome is enabled for the group
 * - Formats message with new member's name
 * - Sends as system message
 */
class SendWelcomeMessageUseCase @Inject constructor(
    private val groupRepository: GroupRepository,
    private val messageRepository: MessageRepository,
    private val userRepository: UserRepository,
    private val auth: FirebaseAuth
) {
    /**
     * Sends welcome message to a new group member
     * 
     * @param groupId The group ID
     * @param newMemberId The new member's user ID
     */
    suspend operator fun invoke(groupId: String, newMemberId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Get group settings
            val settings = groupRepository.getGroupSettings(groupId)
            
            // Check if welcome is enabled
            if (settings == null || !settings.welcomeEnabled) {
                return@withContext Result.success(Unit)
            }
            
            // Get new member's name
            val newMember = userRepository.getUserById(newMemberId)
            val memberName = newMember?.name ?: "New Member"
            
            // Format welcome message
            val welcomeText = settings.formatWelcomeMessage(memberName)
            
            // Create system message
            val message = Message(
                messageId = "",
                senderId = "system",
                senderName = "System",
                receiverId = groupId,
                content = welcomeText,
                timestamp = System.currentTimeMillis(),
                status = MessageStatus.SENT,
                isGroup = true,
                mediaType = null,
                mediaUrl = null
            )
            
            // Send message
            messageRepository.sendMessage(message)
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
