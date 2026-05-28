package com.Azelmods.App.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class CachedUserEntity(
    @PrimaryKey
    @ColumnInfo(name = "uid")
    val uid: String,

    @ColumnInfo(name = "name")
    val name: String = "",

    @ColumnInfo(name = "display_name")
    val displayName: String = "",

    @ColumnInfo(name = "username")
    val username: String = "",

    @ColumnInfo(name = "email")
    val email: String = "",

    @ColumnInfo(name = "phone")
    val phone: String = "",

    @ColumnInfo(name = "photo_url")
    val photoUrl: String? = null,

    @ColumnInfo(name = "cover_url")
    val coverUrl: String? = null,

    @ColumnInfo(name = "bio")
    val bio: String = "",

    @ColumnInfo(name = "status_text")
    val statusText: String = "Hey there! I'm using Nexus Chat",

    @ColumnInfo(name = "is_online")
    val isOnline: Boolean = false,

    @ColumnInfo(name = "last_seen")
    val lastSeen: Long = 0
)
