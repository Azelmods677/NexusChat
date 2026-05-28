package com.Azelmods.App.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.Azelmods.App.data.local.entity.CachedChatEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {

    /** Observe all cached chats, ordered by last message time descending. */
    @Query("SELECT * FROM chats ORDER BY last_message_time DESC")
    fun getChats(): Flow<List<CachedChatEntity>>

    /** Single shot — get all cached chats. */
    @Query("SELECT * FROM chats ORDER BY last_message_time DESC")
    suspend fun getChatsOnce(): List<CachedChatEntity>

    @Query("SELECT * FROM chats WHERE chat_id = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): CachedChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: CachedChatEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<CachedChatEntity>)

    @Query("DELETE FROM chats")
    suspend fun deleteAllChats()

    @Query("DELETE FROM chats WHERE chat_id = :chatId")
    suspend fun deleteChatById(chatId: String)
}
