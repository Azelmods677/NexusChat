package com.Azelmods.App.data.model

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val content: String = "",
    val timestamp: Long = 0,
    val status: MessageStatus = MessageStatus.SENT,
    val isEdited: Boolean = false,
    val replyTo: String? = null,
    val reactions: Map<String, String> = emptyMap(), // userId to emoji
    val mediaUrl: String? = null, // URL for image/video/audio
    val mediaType: String? = null // "IMAGE", "VIDEO", "AUDIO"
)

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
