package com.Azelmods.App.data.repository

import com.Azelmods.App.data.model.BackgroundConfig
import com.Azelmods.App.data.model.BackgroundType
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for per-chat background management
 * 
 * Features:
 * - Save/load background per chat from Firebase
 * - Local cache with StateFlow
 * - Offline support with Room (TODO: add Room integration)
 */
@Singleton
class ChatBackgroundRepository @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val backgroundCache = mutableMapOf<String, MutableStateFlow<BackgroundConfig>>()
    
    /**
     * Get background configuration for a chat
     */
    fun getBackground(chatId: String): Flow<BackgroundConfig> {
        return backgroundCache.getOrPut(chatId) {
            MutableStateFlow(BackgroundConfig(type = BackgroundType.DEFAULT))
        }
    }
    
    /**
     * Save background configuration for a chat
     */
    suspend fun saveBackground(chatId: String, config: BackgroundConfig) = withContext(Dispatchers.IO) {
        try {
            // Save to Firebase
            val ref = database.getReference("chats/$chatId/settings/background")
            val data = mapOf(
                "type" to config.type.name,
                "colorHex" to config.colorHex,
                "imageUri" to config.imageUri,
                "videoUri" to config.videoUri,
                "gradientColors" to config.gradientColors,
                "gradientAngle" to config.gradientAngle,
                "blurRadius" to config.blurRadius,
                "overlayAlpha" to config.overlayAlpha
            )
            ref.setValue(data).await()
            
            // Update cache
            backgroundCache.getOrPut(chatId) {
                MutableStateFlow(config)
            }.value = config
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Load background from Firebase
     */
    suspend fun loadBackground(chatId: String) = withContext(Dispatchers.IO) {
        try {
            val ref = database.getReference("chats/$chatId/settings/background")
            val snapshot = ref.get().await()
            
            if (snapshot.exists()) {
                val type = snapshot.child("type").getValue(String::class.java)
                    ?.let { BackgroundType.valueOf(it) } ?: BackgroundType.DEFAULT
                
                val config = BackgroundConfig(
                    type = type,
                    colorHex = snapshot.child("colorHex").getValue(String::class.java),
                    imageUri = snapshot.child("imageUri").getValue(String::class.java),
                    videoUri = snapshot.child("videoUri").getValue(String::class.java),
                    gradientColors = (snapshot.child("gradientColors").value as? List<*>)
                        ?.mapNotNull { it as? String } ?: emptyList(),
                    gradientAngle = snapshot.child("gradientAngle").getValue(Int::class.java) ?: 135,
                    blurRadius = snapshot.child("blurRadius").getValue(Float::class.java) ?: 10f,
                    overlayAlpha = snapshot.child("overlayAlpha").getValue(Float::class.java) ?: 0.4f
                )
                
                // Update cache
                backgroundCache.getOrPut(chatId) {
                    MutableStateFlow(config)
                }.value = config
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Delete background configuration (reset to DEFAULT)
     */
    suspend fun deleteBackground(chatId: String) = withContext(Dispatchers.IO) {
        try {
            val ref = database.getReference("chats/$chatId/settings/background")
            ref.removeValue().await()
            
            // Update cache to DEFAULT
            backgroundCache.getOrPut(chatId) {
                MutableStateFlow(BackgroundConfig(type = BackgroundType.DEFAULT))
            }.value = BackgroundConfig(type = BackgroundType.DEFAULT)
            
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
