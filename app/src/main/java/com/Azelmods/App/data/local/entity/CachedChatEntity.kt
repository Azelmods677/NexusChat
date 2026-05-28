package com.Azelmods.App.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class CachedChatEntity(
    @PrimaryKey
    @ColumnInfo(name = "chat_id")
    val chatId: String,

    @ColumnInfo(name = "last_message")
    val lastMessage: String = "",

    @ColumnInfo(name = "last_message_time")
    val lastMessageTime: Long = 0,

    @ColumnInfo(name = "last_message_sender_id")
    val lastMessageSenderId: String = "",

    /** JSON-encoded list of participant UIDs */
    @ColumnInfo(name = "participant_ids_json")
    val participantIdsJson: String = "[]",

    /** JSON-encoded map of uid → displayName */
    @ColumnInfo(name = "participant_names_json")
    val participantNamesJson: String = "{}",

    /** JSON-encoded map of uid → photoUrl */
    @ColumnInfo(name = "participant_photos_json")
    val participantPhotosJson: String = "{}",

    @ColumnInfo(name = "chat_type")
    val chatType: String = "PRIVATE",

    @ColumnInfo(name = "is_pinned")
    val isPinned: Boolean = false,

    @ColumnInfo(name = "is_muted")
    val isMuted: Boolean = false,

    @ColumnInfo(name = "is_archived")
    val isArchived: Boolean = false,

    @ColumnInfo(name = "is_e2ee")
    val isE2EE: Boolean = true,

    @ColumnInfo(name = "unread_count")
    val unreadCount: Int = 0
)
