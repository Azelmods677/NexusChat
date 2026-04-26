package com.Azelmods.App.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val displayName: String = name, // Alias for compatibility
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val coverUrl: String? = null,
    val bio: String = "",
    val status: String = "Hey there! I'm using Nexus Chat",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val isPremium: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val fcmToken: String? = null,
    val messageCount: Int = 0,
    val filesShared: Int = 0
)
