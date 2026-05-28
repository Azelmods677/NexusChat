package com.Azelmods.App.data.model

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "", // Added for compatibility
    val content: String = "",
    val isEncrypted: Boolean = false,
    val encryptedPayload: String? = null,
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
    val forwardedFrom: String? = null, // Name of original sender if forwarded
    // ── Ephemeral / Self-Destructing Messages ──
    val isEphemeral: Boolean = false,      // true if this message is temporary
    val isViewOnce: Boolean = false,       // true for "view once" media messages
    val selfDestructDuration: Long = 0,    // seconds until self-destruct after being viewed (0 = view once)
    val selfDestructAt: Long = 0,          // absolute timestamp when message should be deleted from DB
    val viewedBy: List<String> = emptyList() // users who have viewed this message (for view-once)
)

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
