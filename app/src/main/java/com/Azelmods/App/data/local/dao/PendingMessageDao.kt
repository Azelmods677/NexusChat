package com.Azelmods.App.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.Azelmods.App.data.local.entity.PendingMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingMessageDao {
    @Insert
    suspend fun insert(pendingMessage: PendingMessageEntity): Long

    @Query("SELECT * FROM pending_messages ORDER BY timestamp ASC")
    fun getAll(): Flow<List<PendingMessageEntity>>

    @Query("SELECT * FROM pending_messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    suspend fun getByChatId(chatId: String): List<PendingMessageEntity>

    @Query("SELECT COUNT(*) FROM pending_messages")
    suspend fun count(): Int

    @Delete
    suspend fun delete(pendingMessage: PendingMessageEntity)

    @Query("DELETE FROM pending_messages WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_messages")
    suspend fun deleteAll()
}
