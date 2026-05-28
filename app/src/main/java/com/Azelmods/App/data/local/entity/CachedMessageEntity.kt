package com.Azelmods.App.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class CachedMessageEntity(
    @PrimaryKey
    @ColumnInfo(name = "message_id")
    val messageId: String,

    @ColumnInfo(name = "chat_id")
    val chatId: String,

    @ColumnInfo(name = "sender_id")
    val senderId: String,

    @ColumnInfo(name = "sender_name")
    val senderName: String = "",

    @ColumnInfo(name = "content")
    val content: String = "",

    @ColumnInfo(name = "is_encrypted")
    val isEncrypted: Boolean = false,

    @ColumnInfo(name = "encrypted_payload")
    val encryptedPayload: String? = null,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = 0,

    @ColumnInfo(name = "status")
    val status: String = "SENT",

    @ColumnInfo(name = "is_edited")
    val isEdited: Boolean = false,

    @ColumnInfo(name = "edited_at")
    val editedAt: Long = 0,

    @ColumnInfo(name = "reply_to")
    val replyTo: String? = null,

    /** JSON-encoded map of userId → emoji */
    @ColumnInfo(name = "reactions_json")
    val reactionsJson: String? = null,

    @ColumnInfo(name = "media_url")
    val mediaUrl: String? = null,

    @ColumnInfo(name = "media_type")
    val mediaType: String? = null,

    /** JSON-encoded map of userId → deleted status */
    @ColumnInfo(name = "deleted_for_json")
    val deletedForJson: String? = null,

    @ColumnInfo(name = "deleted_for_everyone")
    val deletedForEveryone: Boolean = false,

    @ColumnInfo(name = "forwarded_from")
    val forwardedFrom: String? = null
)
