package com.Azelmods.App.ui.screens.azelai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.util.CrashlyticsLogger
import com.Azelmods.App.data.api.AzelAIApiService
import com.Azelmods.App.data.model.AIMessage
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 AZEL AI VIEW MODEL 
 */
data class AzelAIState(
    val messages: List<AIMessage> = emptyList(),
    val isThinking: Boolean = false,
    val isStreaming: Boolean = false,
    val streamingContent: String = "",
    val error: String? = null,
    val stats: Map<String, Any> = emptyMap(),
    val currentChatId: String = "",
    val currentChatTitle: String = "",
    val selectedModel: String = AzelAIApiService.DEEPSEEK_R1_70B,
    val availableModels: List<com.Azelmods.App.data.api.AIModel> = emptyList(),
    val thinkingElapsedMs: Long = 0L
)

@HiltViewModel
class AzelAIViewModel @Inject constructor(
    private val repository: AzelAIRepository
) : ViewModel() {
    
    companion object {
        private const val TAG = "AzelAIViewModel"
    }
    
    private fun currentUserId(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    private var messagesCollectJob: Job? = null
    private var thinkingTimerJob: Job? = null
    
    private val _state = MutableStateFlow(AzelAIState())
    val state: StateFlow<AzelAIState> = _state.asStateFlow()
    
    private val _chatHistory = MutableStateFlow<List<ChatHistoryItem>>(emptyList())
    val chatHistory: StateFlow<List<ChatHistoryItem>> = _chatHistory.asStateFlow()
    
    init {
        if (currentUserId().isNotEmpty()) {
            Log.d(TAG, "🚀 Initializing AzelAIViewModel for user: ${currentUserId()}")
            loadAvailableModels()
            startNewChat()
            loadChatHistory()
            checkApiHealth()
        } else {
            Log.e(TAG, "❌ User not authenticated")
            _state.update { it.copy(error = "Usuario no autenticado. Por favor inicia sesión.") }
        }
    }
    
    /**
     * 📊 CARGAR MODELOS DISPONIBLES
     */
    private fun loadAvailableModels() {
        val models = repository.getAvailableModels()
        _state.update { it.copy(availableModels = models) }
        Log.d(TAG, "✅ Loaded ${models.size} available models")
    }
    
    /**
     * 🔄 CAMBIAR MODELO
     */
    fun selectModel(modelId: String) {
        _state.update { it.copy(selectedModel = modelId) }
        Log.d(TAG, "🔄 Model changed to: $modelId")
    }
    
    /**
     * 🔍 VERIFICAR SALUD DE LA API
     */
    private fun checkApiHealth() {
        viewModelScope.launch {
            try {
                val isHealthy = repository.checkApiHealth()
                Log.d(TAG, if (isHealthy) "✅ API is healthy" else "⚠️ API health check failed")                } catch (e: Exception) {
                CrashlyticsLogger.recordException(e, "ai", "loadModels")
                Log.e(TAG, "❌ API health check error", e)
            }
        }
    }
    
    /**
     * 📥 CARGAR MENSAJES DEL CHAT ACTUAL
     */
    private fun loadMessages() {
        val currentChatId = _state.value.currentChatId
        val userId = currentUserId()
        if (currentChatId.isEmpty() || userId.isEmpty()) return
        
        messagesCollectJob?.cancel()
        messagesCollectJob = viewModelScope.launch {
            repository.getMessageHistory("$userId/$currentChatId")
                .catch { e ->
                    CrashlyticsLogger.recordRecoverableError(e, "ai", "loadMessages", "Error al cargar mensajes")
                    Log.e(TAG, "❌ Error loading messages", e)
                    _state.update { it.copy(error = "Error al cargar mensajes: ${e.message}") }
                }
                .collect { messages ->
                    _state.update { it.copy(messages = messages) }
                    Log.d(TAG, "✅ Messages updated: ${messages.size}")
                }
        }
    }
    
    /**
     * 📜 CARGAR HISTORIAL DE CHATS
     */
    private fun loadChatHistory() {
        val userId = currentUserId()
        if (userId.isEmpty()) return
        viewModelScope.launch {
            try {
                val chats = repository.getChatHistory(userId)
                _chatHistory.update { chats }
                Log.d(TAG, "✅ Chat history loaded: ${chats.size}")
            } catch (e: Exception) {
                CrashlyticsLogger.recordRecoverableError(e, "ai", "loadChatHistory", "Error al cargar historial")
                Log.e(TAG, "❌ Error loading chat history", e)
            }
        }
    }
    
    /**
     * 📊 CARGAR ESTADÍSTICAS
     */
    private fun loadStats() {
        val userId = currentUserId()
        if (userId.isEmpty()) return
        viewModelScope.launch {
            try {
                val stats = repository.getChatStats(userId)
                _state.update { it.copy(stats = stats) }
                Log.d(TAG, "✅ Stats loaded: $stats")
            } catch (e: Exception) {
                CrashlyticsLogger.recordRecoverableError(e, "ai", "loadStats")
                Log.e(TAG, "❌ Error loading stats", e)
            }
        }
    }
    
    /**
     * 🆕 INICIAR NUEVO CHAT
     */
    fun startNewChat() {
        messagesCollectJob?.cancel()
        val newChatId = "chat_${System.currentTimeMillis()}"
        _state.update { 
            it.copy(
                currentChatId = newChatId,
                currentChatTitle = "Nuevo Chat",
                messages = emptyList(),
                streamingContent = "",
                isStreaming = false
            )
        }
        Log.d(TAG, "🆕 Started new chat: $newChatId")
    }
    
    /**
     * 📂 CARGAR CHAT ESPECÍFICO
     */
    fun loadChat(chatId: String) {
        messagesCollectJob?.cancel()
        _state.update { 
            it.copy(
                currentChatId = chatId,
                currentChatTitle = "Chat",
                messages = emptyList(),
                streamingContent = "",
                isStreaming = false
            )
        }
        loadMessages()
        Log.d(TAG, "📂 Loading chat: $chatId")
    }
    
    /**
     * 📝 ACTUALIZAR TÍTULO DEL CHAT
     */
    private fun updateChatTitle(firstMessage: String) {
        val title = firstMessage.take(50).let { 
            if (firstMessage.length > 50) "$it..." else it
        }
        _state.update { it.copy(currentChatTitle = title) }
    }
    
    /**
     * 💬 ENVIAR MENSAJE CON STREAMING
     */
    fun sendMessage(content: String, useStreaming: Boolean = true) {
        if (content.isBlank() || _state.value.isThinking || _state.value.isStreaming) {
            Log.w(TAG, "⚠️ Cannot send message: blank or already processing")
            return
        }
        
        viewModelScope.launch {
            try {
                val userId = currentUserId()
                if (userId.isEmpty()) {
                    throw Exception("Usuario no autenticado")
                }
                
                val currentChatId = _state.value.currentChatId
                val chatPath = "$userId/$currentChatId"
                
                // Actualizar título si es el primer mensaje
                if (_state.value.messages.isEmpty()) {
                    updateChatTitle(content)
                }
                
                // Agregar mensaje del usuario a la UI INMEDIATAMENTE
                val userMsg = AIMessage(
                    id = "user_${System.currentTimeMillis()}",
                    content = content,
                    role = "user",
                    timestamp = System.currentTimeMillis()
                )
                
                // Actualizar UI con mensaje del usuario
                val currentMessages = _state.value.messages.toMutableList()
                currentMessages.add(userMsg)
                _state.update { it.copy(messages = currentMessages) }
                
                // IMPORTANTE: Activar isThinking DESPUÉS de agregar el mensaje del usuario
                // para que el scroll automático funcione y el usuario vea el indicador
                kotlinx.coroutines.delay(100) // Pequeño delay para que la UI se actualice
                _state.update { it.copy(isThinking = true, isStreaming = useStreaming, streamingContent = "", error = null, thinkingElapsedMs = 0L) }
                
                // 🕐 Iniciar timer que cuenta el tiempo de espera (para indicador visual de timeout)
                thinkingTimerJob?.cancel()
                thinkingTimerJob = viewModelScope.launch {
                    while (_state.value.isThinking) {
                        kotlinx.coroutines.delay(250)
                        _state.update { it.copy(thinkingElapsedMs = it.thinkingElapsedMs + 250) }
                    }
                }
                Log.d(TAG, "💬 Sending message (streaming: $useStreaming): ${content.take(50)}...")
                
                // Intentar guardar en Firebase (opcional)
                try {
                    repository.saveMessage(chatPath, userMsg)
                    Log.d(TAG, "✅ User message saved to Firebase")
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Failed to save user message to Firebase: ${e.message}")
                    // Continuar sin guardar en Firebase
                }
                
                val history = _state.value.messages
                val selectedModel = _state.value.selectedModel
                
                Log.d(TAG, "🚀 Calling API with model: $selectedModel")
                
                if (useStreaming) {
                    var fullResponse = ""
                    // 🔧 Add 60-second timeout to prevent infinite loading
                    kotlinx.coroutines.withTimeout(60_000L) {
                        repository.sendMessageStream(content, history, selectedModel)
                            .collect { chunk ->
                                if (chunk.isNotEmpty()) {
                                    fullResponse += chunk
                                    _state.update { it.copy(streamingContent = fullResponse) }
                                    Log.d(TAG, "📥 Streaming chunk received: ${chunk.take(50)}...")
                                }
                            }
                    }
                    
                    Log.d(TAG, "✅ Streaming complete. Total length: ${fullResponse.length}")
                    
                    if (fullResponse.isNotEmpty()) {
                        val aiMsg = AIMessage(
                            id = "ai_${System.currentTimeMillis()}",
                            content = fullResponse,
                            role = "assistant",
                            timestamp = System.currentTimeMillis(),
                            tokens = fullResponse.split(" ").size,
                            model = selectedModel
                        )
                        
                        // Actualizar UI con respuesta de la IA
                        val updatedMessages = _state.value.messages.toMutableList()
                        updatedMessages.add(aiMsg)
                        _state.update { it.copy(messages = updatedMessages) }
                        
                        // Intentar guardar en Firebase (opcional)
                        try {
                            repository.saveMessage(chatPath, aiMsg)
                            Log.d(TAG, "✅ AI streaming response saved to Firebase")
                        } catch (e: Exception) {
                            Log.e(TAG, "⚠️ Failed to save AI response to Firebase: ${e.message}")
                            // Continuar sin guardar en Firebase
                        }
                    } else {
                        Log.w(TAG, "⚠️ Empty response from API")
                        throw Exception("La API no devolvió ninguna respuesta")
                    }
                } else {
                    // MODO NO-STREAMING
                    Log.d(TAG, "📡 Calling non-streaming API...")
                    val (aiResponse, tokens) = kotlinx.coroutines.withTimeout(60_000L) {
                        repository.sendMessage(content, history, selectedModel).getOrThrow()
                    }
                    Log.d(TAG, "✅ API response received: ${aiResponse.take(100)}... (tokens: $tokens)")
                    
                    val aiMsg = AIMessage(
                        id = "ai_${System.currentTimeMillis()}",
                        content = aiResponse,
                        role = "assistant",
                        timestamp = System.currentTimeMillis(),
                        tokens = tokens,
                        model = selectedModel
                    )
                    
                    // Actualizar UI con respuesta de la IA
                    val updatedMessages = _state.value.messages.toMutableList()
                    updatedMessages.add(aiMsg)
                    _state.update { it.copy(messages = updatedMessages) }
                    
                    // Intentar guardar en Firebase (opcional)
                    try {
                        repository.saveMessage(chatPath, aiMsg)
                        Log.d(TAG, "✅ AI response saved to Firebase (tokens: $tokens)")
                    } catch (e: Exception) {
                        Log.e(TAG, "⚠️ Failed to save AI response to Firebase: ${e.message}")
                        // Continuar sin guardar en Firebase
                    }
                }
                
                // Actualizar historial de chats (con try-catch)
                try {
                    repository.updateChatHistory(userId, currentChatId, _state.value.currentChatTitle, content)
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Failed to update chat history: ${e.message}")
                    // Continuar sin actualizar historial
                }
                
            } catch (e: Exception) {
                    CrashlyticsLogger.recordException(e, "ai", "sendMessage", "Error sending message")
                Log.e(TAG, "❌ Error sending message", e)
                val errorMessage = when {
                    e.message?.contains("no autenticado", ignoreCase = true) == true -> 
                        "Debes iniciar sesión para usar la IA"
                    e is java.net.UnknownHostException -> "Sin conexión a internet"
                    e is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                    e is java.net.ConnectException -> "No se pudo conectar al servidor"
                    e.message?.contains("401") == true -> "API Key inválida o expirada"
                    e.message?.contains("403") == true -> "Acceso denegado"
                    e.message?.contains("429") == true -> "Límite de solicitudes excedido"
                    e.message?.contains("500") == true || e.message?.contains("502") == true || e.message?.contains("503") == true -> 
                        "Servidor temporalmente no disponible"
                    else -> "Error: ${e.message ?: "Desconocido"}"
                }
                
                _state.update { it.copy(error = errorMessage) }
                
                // Agregar mensaje de error a la UI
                val errorMsg = AIMessage(
                    id = "error_${System.currentTimeMillis()}",
                    content = "❌ $errorMessage\n\nPor favor intenta de nuevo.",
                    role = "assistant",
                    timestamp = System.currentTimeMillis(),
                    error = true
                )
                
                val updatedMessages = _state.value.messages.toMutableList()
                updatedMessages.add(errorMsg)
                _state.update { it.copy(messages = updatedMessages) }
            } finally {
                thinkingTimerJob?.cancel()
                _state.update { it.copy(isThinking = false, isStreaming = false, streamingContent = "", thinkingElapsedMs = 0L) }
                Log.d(TAG, "🏁 Message sending complete")
            }
        }
    }
    
    /**
     * 🗑️ LIMPIAR HISTORIAL
     */
    fun clearHistory() {
        val userId = currentUserId()
        if (userId.isEmpty()) return
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Clearing all history")
                messagesCollectJob?.cancel()
                repository.clearHistory(userId)
                _state.update { it.copy(messages = emptyList(), stats = emptyMap()) }
                _chatHistory.update { emptyList() }
                startNewChat()
                Log.d(TAG, "✅ All history cleared")
            } catch (e: Exception) {
                CrashlyticsLogger.recordRecoverableError(e, "ai", "clearHistory", "Error clearing history")
                Log.e(TAG, "❌ Error clearing history", e)
                _state.update { it.copy(error = "Error al limpiar historial: ${e.message}") }
            }
        }
    }
    
    /**
     * ❌ LIMPIAR ERROR
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    /**
     * 🔄 REINTENTAR ÚLTIMO MENSAJE
     */
    fun retryLastMessage() {
        val lastUserMessage = _state.value.messages
            .lastOrNull { it.role == "user" && !it.error }
        
        if (lastUserMessage != null) {
            Log.d(TAG, "🔄 Retrying last message")
            sendMessage(lastUserMessage.content)
        }
    }
}
