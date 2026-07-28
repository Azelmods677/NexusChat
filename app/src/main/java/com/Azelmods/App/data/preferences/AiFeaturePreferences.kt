package com.Azelmods.App.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiFeatureDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_feature_preferences"
)

/**
 * Interruptores de las funciones de IA sobre los chats.
 *
 * Todas nacen **apagadas**. Cada una envía texto de la conversación al proveedor
 * de IA que el usuario haya configurado, y eso no puede pasar por defecto en una
 * app que presume de cifrado de extremo a extremo: el usuario tiene que
 * encenderlas a sabiendas. La pantalla de ajustes lo dice con esas palabras.
 */
@Singleton
class AiFeaturePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.aiFeatureDataStore

    private companion object {
        val KEY_SMART_REPLIES = booleanPreferencesKey("smart_replies")
        val KEY_AUTO_TRANSLATE = booleanPreferencesKey("auto_translate_incoming")
        val KEY_SUMMARY = booleanPreferencesKey("conversation_summary")
        val KEY_TONE = booleanPreferencesKey("tone_suggestions")
        val KEY_PHOTO_ENHANCE = booleanPreferencesKey("photo_enhance")
        val KEY_VOICE_TRANSCRIPTION = booleanPreferencesKey("voice_transcription")
    }

    /** Sugerencias de respuesta rápida encima del campo de texto. */
    val smartReplies: Flow<Boolean> = dataStore.data.map { it[KEY_SMART_REPLIES] ?: false }

    /** Traducción automática de los mensajes que llegan. */
    val autoTranslate: Flow<Boolean> = dataStore.data.map { it[KEY_AUTO_TRANSLATE] ?: false }

    /** Opción de resumir la conversación en el menú del chat. */
    val conversationSummary: Flow<Boolean> = dataStore.data.map { it[KEY_SUMMARY] ?: false }

    /** Botón para reescribir el borrador en otro tono. */
    val toneSuggestions: Flow<Boolean> = dataStore.data.map { it[KEY_TONE] ?: false }

    /** Realce automático al enviar fotos (local, sin subir nada a ningún sitio). */
    val photoEnhance: Flow<Boolean> = dataStore.data.map { it[KEY_PHOTO_ENHANCE] ?: false }

    /** Dictado por voz en el campo de texto (reconocimiento del propio dispositivo). */
    val voiceTranscription: Flow<Boolean> = dataStore.data.map { it[KEY_VOICE_TRANSCRIPTION] ?: false }

    suspend fun setSmartReplies(enabled: Boolean) = set(KEY_SMART_REPLIES, enabled)
    suspend fun setAutoTranslate(enabled: Boolean) = set(KEY_AUTO_TRANSLATE, enabled)
    suspend fun setConversationSummary(enabled: Boolean) = set(KEY_SUMMARY, enabled)
    suspend fun setToneSuggestions(enabled: Boolean) = set(KEY_TONE, enabled)
    suspend fun setPhotoEnhance(enabled: Boolean) = set(KEY_PHOTO_ENHANCE, enabled)
    suspend fun setVoiceTranscription(enabled: Boolean) = set(KEY_VOICE_TRANSCRIPTION, enabled)

    /** Lectura puntual, para los sitios donde no interesa suscribirse al flujo. */
    suspend fun isSmartRepliesEnabled(): Boolean = smartReplies.first()
    suspend fun isAutoTranslateEnabled(): Boolean = autoTranslate.first()
    suspend fun isPhotoEnhanceEnabled(): Boolean = photoEnhance.first()

    private suspend fun set(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { it[key] = value }
    }
}
