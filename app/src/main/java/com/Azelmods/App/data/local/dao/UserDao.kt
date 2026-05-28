package com.Azelmods.App.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.Azelmods.App.data.local.entity.CachedUserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    /** Observe all cached users. */
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<CachedUserEntity>>

    /** Single shot — get all cached users. */
    @Query("SELECT * FROM users")
    suspend fun getAllUsersOnce(): List<CachedUserEntity>

    @Query("SELECT * FROM users WHERE uid = :userId LIMIT 1")
    suspend fun getUserById(userId: String): CachedUserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: CachedUserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<CachedUserEntity>)

    @Query("DELETE FROM users")
    suspend fun deleteAllUsers()

    @Query("DELETE FROM users WHERE uid = :userId")
    suspend fun deleteUserById(userId: String)
}
