package com.Azelmods.App.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.Azelmods.App.data.manager.AIManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.aiDataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_preferences")

/**
 * 🧠 AI PREFERENCES - Configuración de la IA
 */
@Singleton
class AIPreferences @Inject constructor(
    private val context: Context
) {
    
    companion object {
        // Proveedor de IA
        private val AI_PROVIDER = stringPreferencesKey("ai_provider")
        private val AI_MODEL = stringPreferencesKey("ai_model")
        
        // Configuración de generación
        private val TEMPERATURE = doublePreferencesKey("temperature")
        private val MAX_TOKENS = intPreferencesKey("max_tokens")
        private val TOP_P = doublePreferencesKey("top_p")
        private val FREQUENCY_PENALTY = doublePreferencesKey("frequency_penalty")
        private val PRESENCE_PENALTY = doublePreferencesKey("presence_penalty")
        
        // Configuración de comportamiento
        private val AGGRESSIVE_MODE = booleanPreferencesKey("aggressive_mode")
        private val AUTO_FALLBACK = booleanPreferencesKey("auto_fallback")
        private val STREAM_RESPONSES = booleanPreferencesKey("stream_responses")
        private val SAVE_HISTORY = booleanPreferencesKey("save_history")
        
        // API Keys personalizadas
        private val CUSTOM_OPENCODE_KEY = stringPreferencesKey("custom_opencode_key")
        private val CUSTOM_OLLAMA_URL = stringPreferencesKey("custom_ollama_url")
        
        // Estadísticas
        private val TOTAL_MESSAGES = intPreferencesKey("total_messages")
        private val TOTAL_TOKENS = intPreferencesKey("total_tokens")
        private val FAVORITE_MODEL = stringPreferencesKey("favorite_model")
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // PROVEEDOR Y MODELO
    // ═══════════════════════════════════════════════════════════════════
    
    val aiProvider: Flow<String> = context.aiDataStore.data.map { preferences ->
        preferences[AI_PROVIDER] ?: AIManager.AIProvider.OLLAMA_CLOUD.name
    }
    
    suspend fun setAIProvider(provider: AIManager.AIProvider) {
        context.aiDataStore.edit { preferences ->
            preferences[AI_PROVIDER] = provider.name
        }
    }
    
    val aiModel: Flow<String> = context.aiDataStore.data.map { preferences ->
        preferences[AI_MODEL] ?: com.Azelmods.App.data.api.AzelAIApiService.DEEPSEEK_R1_70B
    }
    
    suspend fun setAIModel(model: String) {
        context.aiDataStore.edit { preferences ->
            preferences[AI_MODEL] = model
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE GENERACIÓN
    // ═══════════════════════════════════════════════════════════════════
    
    val temperature: Flow<Double> = context.aiDataStore.data.map { preferences ->
        preferences[TEMPERATURE] ?: 0.9
    }
    
    suspend fun setTemperature(temp: Double) {
        context.aiDataStore.edit { preferences ->
            preferences[TEMPERATURE] = temp.coerceIn(0.0, 2.0)
        }
    }
    
    val maxTokens: Flow<Int> = context.aiDataStore.data.map { preferences ->
        preferences[MAX_TOKENS] ?: 8192
    }
    
    suspend fun setMaxTokens(tokens: Int) {
        context.aiDataStore.edit { preferences ->
            preferences[MAX_TOKENS] = tokens.coerceIn(256, 32768)
        }
    }
    
    val topP: Flow<Double> = context.aiDataStore.data.map { preferences ->
        preferences[TOP_P] ?: 0.95
    }
    
    suspend fun setTopP(topP: Double) {
        context.aiDataStore.edit { preferences ->
            preferences[TOP_P] = topP.coerceIn(0.0, 1.0)
        }
    }
    
    val frequencyPenalty: Flow<Double> = context.aiDataStore.data.map { preferences ->
        preferences[FREQUENCY_PENALTY] ?: 0.1
    }
    
    suspend fun setFrequencyPenalty(penalty: Double) {
        context.aiDataStore.edit { preferences ->
            preferences[FREQUENCY_PENALTY] = penalty.coerceIn(-2.0, 2.0)
        }
    }
    
    val presencePenalty: Flow<Double> = context.aiDataStore.data.map { preferences ->
        preferences[PRESENCE_PENALTY] ?: 0.1
    }
    
    suspend fun setPresencePenalty(penalty: Double) {
        context.aiDataStore.edit { preferences ->
            preferences[PRESENCE_PENALTY] = penalty.coerceIn(-2.0, 2.0)
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CONFIGURACIÓN DE COMPORTAMIENTO
    // ═══════════════════════════════════════════════════════════════════
    
    val aggressiveMode: Flow<Boolean> = context.aiDataStore.data.map { preferences ->
        preferences[AGGRESSIVE_MODE] ?: true
    }
    
    suspend fun setAggressiveMode(enabled: Boolean) {
        context.aiDataStore.edit { preferences ->
            preferences[AGGRESSIVE_MODE] = enabled
        }
    }
    
    val autoFallback: Flow<Boolean> = context.aiDataStore.data.map { preferences ->
        preferences[AUTO_FALLBACK] ?: true
    }
    
    suspend fun setAutoFallback(enabled: Boolean) {
        context.aiDataStore.edit { preferences ->
            preferences[AUTO_FALLBACK] = enabled
        }
    }
    
    val streamResponses: Flow<Boolean> = context.aiDataStore.data.map { preferences ->
        preferences[STREAM_RESPONSES] ?: false
    }
    
    suspend fun setStreamResponses(enabled: Boolean) {
        context.aiDataStore.edit { preferences ->
            preferences[STREAM_RESPONSES] = enabled
        }
    }
    
    val saveHistory: Flow<Boolean> = context.aiDataStore.data.map { preferences ->
        preferences[SAVE_HISTORY] ?: true
    }
    
    suspend fun setSaveHistory(enabled: Boolean) {
        context.aiDataStore.edit { preferences ->
            preferences[SAVE_HISTORY] = enabled
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // API KEYS PERSONALIZADAS
    // ═══════════════════════════════════════════════════════════════════
    
    val customOllamaUrl: Flow<String> = context.aiDataStore.data.map { preferences ->
        preferences[CUSTOM_OLLAMA_URL] ?: "http://localhost:11434"
    }
    
    suspend fun setCustomOllamaUrl(url: String) {
        context.aiDataStore.edit { preferences ->
            preferences[CUSTOM_OLLAMA_URL] = url
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // ESTADÍSTICAS
    // ═══════════════════════════════════════════════════════════════════
    
    val totalMessages: Flow<Int> = context.aiDataStore.data.map { preferences ->
        preferences[TOTAL_MESSAGES] ?: 0
    }
    
    suspend fun incrementMessageCount() {
        context.aiDataStore.edit { preferences ->
            val current = preferences[TOTAL_MESSAGES] ?: 0
            preferences[TOTAL_MESSAGES] = current + 1
        }
    }
    
    val totalTokens: Flow<Int> = context.aiDataStore.data.map { preferences ->
        preferences[TOTAL_TOKENS] ?: 0
    }
    
    suspend fun addTokens(tokens: Int) {
        context.aiDataStore.edit { preferences ->
            val current = preferences[TOTAL_TOKENS] ?: 0
            preferences[TOTAL_TOKENS] = current + tokens
        }
    }
    
    val favoriteModel: Flow<String> = context.aiDataStore.data.map { preferences ->
        preferences[FAVORITE_MODEL] ?: "gpt-4-turbo-2024-04-09"
    }
    
    suspend fun setFavoriteModel(model: String) {
        context.aiDataStore.edit { preferences ->
            preferences[FAVORITE_MODEL] = model
        }
    }
    
    // ═══════════════════════════════════════════════════════════════════
    // CONFIGURACIONES PREDEFINIDAS
    // ═══════════════════════════════════════════════════════════════════
    
    /**
     * 🔥 MODO ULTRA AGRESIVO - Máxima creatividad y libertad
     */
    suspend fun setUltraAggressiveMode() {
        context.aiDataStore.edit { preferences ->
            preferences[TEMPERATURE] = 0.95
            preferences[MAX_TOKENS] = 8192
            preferences[TOP_P] = 0.95
            preferences[FREQUENCY_PENALTY] = 0.2
            preferences[PRESENCE_PENALTY] = 0.2
            preferences[AGGRESSIVE_MODE] = true
        }
    }
    
    /**
     * ⚖️ MODO BALANCEADO - Equilibrio entre creatividad y precisión
     */
    suspend fun setBalancedMode() {
        context.aiDataStore.edit { preferences ->
            preferences[TEMPERATURE] = 0.8
            preferences[MAX_TOKENS] = 4096
            preferences[TOP_P] = 0.9
            preferences[FREQUENCY_PENALTY] = 0.1
            preferences[PRESENCE_PENALTY] = 0.1
            preferences[AGGRESSIVE_MODE] = true
        }
    }
    
    /**
     * 🎯 MODO PRECISO - Máxima precisión y consistencia
     */
    suspend fun setPreciseMode() {
        context.aiDataStore.edit { preferences ->
            preferences[TEMPERATURE] = 0.3
            preferences[MAX_TOKENS] = 2048
            preferences[TOP_P] = 0.8
            preferences[FREQUENCY_PENALTY] = 0.0
            preferences[PRESENCE_PENALTY] = 0.0
            preferences[AGGRESSIVE_MODE] = false
        }
    }
    
    /**
     * 🔄 RESETEAR A VALORES POR DEFECTO
     */
    suspend fun resetToDefaults() {
        context.aiDataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    /**
     * 📊 OBTENER TODAS LAS CONFIGURACIONES
     */
    fun getAllSettings(): Flow<Map<String, Any>> = context.aiDataStore.data.map { preferences ->
        mapOf(
            "ai_provider" to (preferences[AI_PROVIDER] ?: AIManager.AIProvider.OLLAMA_CLOUD.name),
            "ai_model" to (preferences[AI_MODEL] ?: com.Azelmods.App.data.api.AzelAIApiService.DEEPSEEK_R1_70B),
            "temperature" to (preferences[TEMPERATURE] ?: 0.9),
            "max_tokens" to (preferences[MAX_TOKENS] ?: 8192),
            "top_p" to (preferences[TOP_P] ?: 0.95),
            "frequency_penalty" to (preferences[FREQUENCY_PENALTY] ?: 0.1),
            "presence_penalty" to (preferences[PRESENCE_PENALTY] ?: 0.1),
            "aggressive_mode" to (preferences[AGGRESSIVE_MODE] ?: true),
            "auto_fallback" to (preferences[AUTO_FALLBACK] ?: true),
            "stream_responses" to (preferences[STREAM_RESPONSES] ?: false),
            "save_history" to (preferences[SAVE_HISTORY] ?: true),
            "total_messages" to (preferences[TOTAL_MESSAGES] ?: 0),
            "total_tokens" to (preferences[TOTAL_TOKENS] ?: 0),
            "favorite_model" to (preferences[FAVORITE_MODEL] ?: com.Azelmods.App.data.api.AzelAIApiService.DEEPSEEK_R1_70B)
        )
    }
}