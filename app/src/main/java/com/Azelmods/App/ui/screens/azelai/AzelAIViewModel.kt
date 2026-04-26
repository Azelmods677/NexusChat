package com.Azelmods.App.ui.screens.azelai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.model.AIMessage
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AzelAIState(
    val messages: List<AIMessage> = emptyList(),
    val isThinking: Boolean = false,
    val error: String? = null,
    val stats: Map<String, Any> = emptyMap()
)

class AzelAIViewModel : ViewModel() {
    
    companion object {
        private const val TAG = "AzelAIViewModel"
    }
    
    private val repository = AzelAIRepository()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    private val _state = MutableStateFlow(AzelAIState())
    val state: StateFlow<AzelAIState> = _state.asStateFlow()
    
    init {
        if (userId.isNotEmpty()) {
            loadMessages()
            loadStats()
        } else {
            Log.e(TAG, "User not authenticated")
        }
    }
    
    private fun loadMessages() {
        viewModelScope.launch {
            repository.getMessageHistory(userId)
                .catch { e ->
                    Log.e(TAG, "Error loading messages", e)
                    _state.update { it.copy(error = "Error al cargar mensajes: ${e.message}") }
                }
                .collect { messages ->
                    _state.update { it.copy(messages = messages) }
                    Log.d(TAG, "Messages updated: ${messages.size}")
                }
        }
    }
    
    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = repository.getChatStats(userId)
                _state.update { it.copy(stats = stats) }
                Log.d(TAG, "Stats loaded: $stats")
            } catch (e: Exception) {
                Log.e(TAG, "Error loading stats", e)
            }
        }
    }
    
    /**
     * Enviar mensaje a la IA
     */
    fun sendMessage(content: String) {
        if (content.isBlank() || _state.value.isThinking) {
            Log.w(TAG, "Cannot send message: blank or already thinking")
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isThinking = true, error = null) }
            Log.d(TAG, "Sending message: $content")
            
            try {
                // Guardar mensaje del usuario
                val userMsg = AIMessage(
                    content = content,
                    role = "user",
                    timestamp = System.currentTimeMillis()
                )
                repository.saveMessage(userId, userMsg)
                Log.d(TAG, "User message saved")
                
                // Llamar a la API
                val history = _state.value.messages
                val (aiResponse, tokens) = repository.sendMessage(content, history).getOrThrow()
                
                Log.d(TAG, "AI response received: ${aiResponse.take(100)}...")
                Log.d(TAG, "Tokens used: $tokens")
                
                // Guardar respuesta de la IA
                val aiMsg = AIMessage(
                    content = aiResponse,
                    role = "assistant",
                    timestamp = System.currentTimeMillis(),
                    tokens = tokens,
                    model = "llama3.3:70b"
                )
                repository.saveMessage(userId, aiMsg)
                Log.d(TAG, "AI message saved")
                
                // Recargar estadísticas
                loadStats()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
                val errorMessage = when {
                    e.message?.contains("401") == true -> "Error de autenticación con la API"
                    e.message?.contains("429") == true -> "Límite de solicitudes excedido. Intenta más tarde"
                    e.message?.contains("timeout") == true -> "Tiempo de espera agotado. Intenta de nuevo"
                    else -> "Error: ${e.message}"
                }
                
                _state.update { it.copy(error = errorMessage) }
                
                // Guardar mensaje de error
                val errorMsg = AIMessage(
                    content = "❌ $errorMessage\n\nPor favor intenta de nuevo.",
                    role = "assistant",
                    timestamp = System.currentTimeMillis(),
                    error = true
                )
                repository.saveMessage(userId, errorMsg)
            } finally {
                _state.update { it.copy(isThinking = false) }
            }
        }
    }
    
    /**
     * Limpiar historial de conversación
     */
    fun clearHistory() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Clearing history")
                repository.clearHistory(userId)
                _state.update { it.copy(messages = emptyList(), stats = emptyMap()) }
                Log.d(TAG, "History cleared successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Error clearing history", e)
                _state.update { it.copy(error = "Error al limpiar historial: ${e.message}") }
            }
        }
    }
    
    /**
     * Limpiar mensaje de error
     */
    fun clearError() {
        _state.update { it.copy(error = null) }
    }
    
    /**
     * Reenviar último mensaje (en caso de error)
     */
    fun retryLastMessage() {
        val lastUserMessage = _state.value.messages
            .lastOrNull { it.role == "user" && !it.error }
        
        if (lastUserMessage != null) {
            Log.d(TAG, "Retrying last message: ${lastUserMessage.content}")
            sendMessage(lastUserMessage.content)
        }
    }
}
