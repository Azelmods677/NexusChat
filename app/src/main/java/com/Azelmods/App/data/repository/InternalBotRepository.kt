package com.Azelmods.App.data.repository

import com.Azelmods.App.data.preferences.BotPreferences
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for internal bot features
 */
@Singleton
class InternalBotRepository @Inject constructor(
    private val botPreferences: BotPreferences
) {
    // Auto reply private
    fun getAutoReplyPrivate(): Flow<Boolean> = botPreferences.autoReplyPrivate
    suspend fun setAutoReplyPrivate(enabled: Boolean) = botPreferences.setAutoReplyPrivate(enabled)
    
    fun getAutoReplyPrivateMessage(): Flow<String> = botPreferences.autoReplyPrivateMessage
    suspend fun setAutoReplyPrivateMessage(message: String) = botPreferences.setAutoReplyPrivateMessage(message)
    
    // Auto reply groups
    fun getAutoReplyGroups(): Flow<Boolean> = botPreferences.autoReplyGroups
    suspend fun setAutoReplyGroups(enabled: Boolean) = botPreferences.setAutoReplyGroups(enabled)
    
    fun getAutoReplyGroupsMessage(): Flow<String> = botPreferences.autoReplyGroupsMessage
    suspend fun setAutoReplyGroupsMessage(message: String) = botPreferences.setAutoReplyGroupsMessage(message)
    
    // Ghost mode
    fun getGhostMode(): Flow<Boolean> = botPreferences.ghostMode
    suspend fun setGhostMode(enabled: Boolean) = botPreferences.setGhostMode(enabled)
    
    // Mass messages
    fun getMassMessages(): Flow<Boolean> = botPreferences.massMessages
    suspend fun setMassMessages(enabled: Boolean) = botPreferences.setMassMessages(enabled)
    
    // Translator
    fun getTranslator(): Flow<Boolean> = botPreferences.translator
    suspend fun setTranslator(enabled: Boolean) = botPreferences.setTranslator(enabled)
    
    fun getTranslatorLanguage(): Flow<String> = botPreferences.translatorLanguage
    suspend fun setTranslatorLanguage(language: String) = botPreferences.setTranslatorLanguage(language)
    
    // Custom quoted
    fun getCustomQuoted(): Flow<Boolean> = botPreferences.customQuoted
    suspend fun setCustomQuoted(enabled: Boolean) = botPreferences.setCustomQuoted(enabled)
    
    // Mention all
    fun getMentionAll(): Flow<Boolean> = botPreferences.mentionAll
    suspend fun setMentionAll(enabled: Boolean) = botPreferences.setMentionAll(enabled)
}
