package com.Azelmods.App.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.botDataStore: DataStore<Preferences> by preferencesDataStore(name = "bot_preferences")

/**
 * Bot preferences for internal bot features
 * 
 * Features:
 * - Auto respuesta privado/grupos
 * - Modo Fantasma
 * - Mensajes en masa
 * - Traductor entrada
 * - Quoted personalizado
 * - Mencionar todos
 */
@Singleton
class BotPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.botDataStore
    
    companion object {
        private val KEY_AUTO_REPLY_PRIVATE = booleanPreferencesKey("auto_reply_private")
        private val KEY_AUTO_REPLY_PRIVATE_MESSAGE = stringPreferencesKey("auto_reply_private_message")
        private val KEY_AUTO_REPLY_GROUPS = booleanPreferencesKey("auto_reply_groups")
        private val KEY_AUTO_REPLY_GROUPS_MESSAGE = stringPreferencesKey("auto_reply_groups_message")
        private val KEY_GHOST_MODE = booleanPreferencesKey("ghost_mode")
        private val KEY_MASS_MESSAGES = booleanPreferencesKey("mass_messages")
        private val KEY_TRANSLATOR = booleanPreferencesKey("translator")
        private val KEY_TRANSLATOR_LANGUAGE = stringPreferencesKey("translator_language")
        private val KEY_CUSTOM_QUOTED = booleanPreferencesKey("custom_quoted")
        private val KEY_MENTION_ALL = booleanPreferencesKey("mention_all")
    }
    
    // Auto reply private
    val autoReplyPrivate: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_REPLY_PRIVATE] ?: false
    }
    
    suspend fun setAutoReplyPrivate(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_REPLY_PRIVATE] = enabled
        }
    }
    
    val autoReplyPrivateMessage: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_REPLY_PRIVATE_MESSAGE] ?: "Estoy ocupado, te responderé pronto."
    }
    
    suspend fun setAutoReplyPrivateMessage(message: String) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_REPLY_PRIVATE_MESSAGE] = message
        }
    }
    
    // Auto reply groups
    val autoReplyGroups: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_REPLY_GROUPS] ?: false
    }
    
    suspend fun setAutoReplyGroups(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_REPLY_GROUPS] = enabled
        }
    }
    
    val autoReplyGroupsMessage: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_AUTO_REPLY_GROUPS_MESSAGE] ?: "No disponible en este momento."
    }
    
    suspend fun setAutoReplyGroupsMessage(message: String) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_REPLY_GROUPS_MESSAGE] = message
        }
    }
    
    // Ghost mode
    val ghostMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_GHOST_MODE] ?: false
    }
    
    suspend fun setGhostMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_GHOST_MODE] = enabled
        }
    }
    
    // Mass messages
    val massMessages: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_MASS_MESSAGES] ?: false
    }
    
    suspend fun setMassMessages(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_MASS_MESSAGES] = enabled
        }
    }
    
    // Translator
    val translator: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_TRANSLATOR] ?: false
    }
    
    suspend fun setTranslator(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_TRANSLATOR] = enabled
        }
    }
    
    val translatorLanguage: Flow<String> = dataStore.data.map { prefs ->
        prefs[KEY_TRANSLATOR_LANGUAGE] ?: "es"
    }
    
    suspend fun setTranslatorLanguage(language: String) {
        dataStore.edit { prefs ->
            prefs[KEY_TRANSLATOR_LANGUAGE] = language
        }
    }
    
    // Custom quoted
    val customQuoted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_CUSTOM_QUOTED] ?: false
    }
    
    suspend fun setCustomQuoted(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_CUSTOM_QUOTED] = enabled
        }
    }
    
    // Mention all
    val mentionAll: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_MENTION_ALL] ?: false
    }
    
    suspend fun setMentionAll(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_MENTION_ALL] = enabled
        }
    }
}
