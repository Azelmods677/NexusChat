package com.Azelmods.App.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_messages")
data class PendingMessageEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val chatId: String,
    val content: String,
    val senderId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: String = "text",
    val replyTo: String? = null,
    val isEphemeral: Boolean = false,
    val isViewOnce: Boolean = false,
    val selfDestructDuration: Long = 0
)
