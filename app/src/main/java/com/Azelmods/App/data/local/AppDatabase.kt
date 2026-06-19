package com.Azelmods.App.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.Azelmods.App.data.local.dao.ChatDao
import com.Azelmods.App.data.local.dao.MessageDao
import com.Azelmods.App.data.local.dao.PendingMessageDao
import com.Azelmods.App.data.local.dao.UserDao
import com.Azelmods.App.data.local.entity.CachedChatEntity
import com.Azelmods.App.data.local.entity.CachedMessageEntity
import com.Azelmods.App.data.local.entity.CachedUserEntity
import com.Azelmods.App.data.local.entity.PendingMessageEntity

@Database(
    entities = [
        CachedMessageEntity::class,
        CachedChatEntity::class,
        CachedUserEntity::class,
        PendingMessageEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao
    abstract fun chatDao(): ChatDao
    abstract fun userDao(): UserDao
    abstract fun pendingMessageDao(): PendingMessageDao

    companion object {
        private const val DB_NAME = "nexuschat_cache.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
