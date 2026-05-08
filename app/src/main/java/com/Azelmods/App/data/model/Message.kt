package com.Azelmods.App.data.model

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "", // Added for compatibility
    val content: String = "",
    val timestamp: Long = 0,
    val status: MessageStatus = MessageStatus.SENT,
    val isEdited: Boolean = false,
    val isGroup: Boolean = false, // Added for compatibility
    val replyTo: String? = null,
    val reactions: Map<String, String> = emptyMap(), // userId to emoji
    val mediaUrl: String? = null, // URL for image/video/audio
    val mediaType: String? = null, // "IMAGE", "VIDEO", "AUDIO"
    val deletedFor: Map<String, Boolean> = emptyMap(), // userId to deleted status
    val deletedForEveryone: Boolean = false, // NEW: true if deleted for everyone
    val edited: Boolean = false,
    val editedAt: Long = 0,
    val forwardedFrom: String? = null // Name of original sender if forwarded
)

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
