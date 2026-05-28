package com.Azelmods.App.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.Azelmods.App.data.local.entity.CachedMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MessageDao {

    /** Observe all messages for a chat, ordered by timestamp ascending. */
    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp ASC")
    fun getMessages(chatId: String): Flow<List<CachedMessageEntity>>

    /** Single shot — get all cached messages for a chat. */
    @Query("SELECT * FROM messages WHERE chat_id = :chatId ORDER BY timestamp ASC")
    suspend fun getMessagesOnce(chatId: String): List<CachedMessageEntity>

    @Query("SELECT * FROM messages WHERE message_id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): CachedMessageEntity?

    /** Insert or replace a single message. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CachedMessageEntity)

    /** Insert or replace a batch of messages. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessageEntity>)

    /** Delete all messages belonging to a specific chat. */
    @Query("DELETE FROM messages WHERE chat_id = :chatId")
    suspend fun deleteMessagesByChat(chatId: String)

    /** Delete all messages (e.g. on logout). */
    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    /** Get the timestamp of the most recent cached message for a chat. */
    @Query("SELECT COALESCE(MAX(timestamp), 0) FROM messages WHERE chat_id = :chatId")
    suspend fun getLastMessageTimestamp(chatId: String): Long
}
