package com.Azelmods.App.data.model

enum class StoryType {
    TEXT,
    IMAGE,
    VIDEO
}

data class Story(
    val storyId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userPhoto: String? = null,
    val type: String = "IMAGE", // TEXT, IMAGE, VIDEO
    val mediaUrl: String? = null, // null for TEXT stories
    val text: String? = null,
    val caption: String? = null,
    val backgroundColor: String? = null, // For TEXT stories
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000), // 24 hours
    val views: List<String> = emptyList(),
    val viewCount: Int = 0
)
