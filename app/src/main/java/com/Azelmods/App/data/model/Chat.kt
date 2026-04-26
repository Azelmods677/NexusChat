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
    val chatType: ChatType = ChatType.PRIVATE
)

enum class ChatType {
    PRIVATE, GROUP
}
