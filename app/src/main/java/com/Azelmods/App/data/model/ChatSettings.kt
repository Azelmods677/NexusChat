package com.Azelmods.App.data.model

/**
 * Chat settings model for pin, mute, and archive functionality.
 */
data class ChatSettings(
    val chatId: String = "",
    val isPinned: Boolean = false,
    val isMuted: Boolean = false,
    val muteUntil: Long = 0L, // Timestamp when mute expires
    val isArchived: Boolean = false,
    val unreadCount: Int = 0,
    val lastMessageTime: Long = 0L,
    val pinnedAt: Long = 0L
) {
    /**
     * Checks if chat is currently muted
     */
    fun isCurrentlyMuted(): Boolean {
        if (!isMuted) return false
        if (muteUntil == Long.MAX_VALUE) return true // Always muted
        return System.currentTimeMillis() < muteUntil
    }
    
    companion object {
        const val MUTE_1_HOUR = 3600000L
        const val MUTE_8_HOURS = 28800000L
        const val MUTE_1_WEEK = 604800000L
        const val MUTE_ALWAYS = Long.MAX_VALUE
    }
}
