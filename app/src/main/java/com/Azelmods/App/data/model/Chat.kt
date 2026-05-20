package com.Azelmods.App.data.model

data class Chat(
    val chatId: String = "",
    val participants: List<String> = emptyList(), // Legacy field
    val participantIds: List<String> = emptyList(), // New field
    val participantNames: Map<String, String> = emptyMap(),
    val participantPhotos: Map<String, String> = emptyMap(),
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),
    val lastMessageSenderId: String = "",
    val unreadCount: Map<String, Int> = emptyMap(),
    val isTyping: Map<String, Boolean> = emptyMap(), // New field
    val createdAt: Long = System.currentTimeMillis(),
    val chatType: ChatType = ChatType.PRIVATE,
    // Additional fields for UI compatibility
    val contactName: String = "",
    val contactPhotoUrl: String? = null,
    val isOnline: Boolean = false,
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val isArchived: Boolean = false,
    val lastMessageTimestamp: Long = lastMessageTime
) : Comparable<Chat> {
    override fun compareTo(other: Chat): Int {
        return lastMessageTimestamp.compareTo(other.lastMessageTimestamp)
    }
    
    /**
     * Gets the unread count for a specific user
     */
    fun getUnreadCount(userId: String): Int {
        return unreadCount[userId] ?: 0
    }
    
    /**
     * Gets the total unread count across all participants
     */
    fun getTotalUnreadCount(): Int {
        return unreadCount.values.sum()
    }
}

enum class ChatType {
    PRIVATE, GROUP
}
