package com.Azelmods.App.ui.screens.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.ai.PromptCategory
import com.Azelmods.App.data.ai.UncensoredPrompts
import com.Azelmods.App.data.api.ChatMessage
import com.Azelmods.App.data.api.OllamaApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class OllamaAIViewModel @Inject constructor() : ViewModel() {
    
    private val _state = MutableStateFlow(OllamaAIState())
    val state: StateFlow<OllamaAIState> = _state.asStateFlow()
    
    private var ollamaService: OllamaApiService? = null
    
    init {
        // Initialize with Ollama Cloud or local server
        initializeOllama("http://localhost:11434") // Cambiar a tu Ollama Cloud URL
        checkConnection()
        loadAvailableModels()
    }
    
    fun initializeOllama(serverUrl: String) {
        ollamaService = OllamaApiService(serverUrl)
        _state.update { it.copy(serverUrl = serverUrl) }
        checkConnection()
    }
    
    private fun checkConnection() {
        viewModelScope.launch {
            val isAvailable = ollamaService?.isServerAvailable() ?: false
            _state.update { it.copy(isConnected = isAvailable) }
        }
    }
    
    private fun loadAvailableModels() {
        viewModelScope.launch {
            val models = ollamaService?.listModels() ?: emptyList()
            _state.update {
                it.copy(
                    availableModels = if (models.isEmpty()) {
                        // Modelos recomendados para hacking sin censura
                        listOf(
                            "llama2-uncensored",
                            "mistral-uncensored", 
                            "wizard-vicuna-uncensored",
                            "dolphin-mixtral",
                            "codellama",
                            "deepseek-coder"
                        )
                    } else {
                        models
                    },
                    currentModel = models.firstOrNull() ?: "llama2-uncensored"
                )
            }
        }
    }
    
    fun selectModel(model: String) {
        _state.update { it.copy(currentModel = model) }
    }
    
    fun updateInput(text: String) {
        _state.update { it.copy(currentInput = text) }
    }
    
    fun selectCategory(category: PromptCategory?) {
        _state.update { it.copy(selectedCategory = category) }
    }
    
    fun usePrompt(prompt: String) {
        _state.update { it.copy(currentInput = prompt) }
    }
    
    fun sendMessage() {
        val currentInput = _state.value.currentInput.trim()
        if (currentInput.isEmpty() || _state.value.isGenerating) return
        
        // Add user message
        val userMessage = AIMessage(
            role = "user",
            content = currentInput,
            timestamp = getCurrentTime()
        )
        
        _state.update {
            it.copy(
                messages = it.messages + userMessage,
                currentInput = "",
                isGenerating = true
            )
        }
        
        // Generate response with UNCENSORED system prompt
        viewModelScope.launch {
            try {
                // Preparar historial con system prompt sin censura
                val chatHistory = mutableListOf(
                    ChatMessage(
                        role = "system",
                        content = UncensoredPrompts.UNCENSORED_SYSTEM
                    )
                )
                
                // Agregar mensajes previos
                chatHistory.addAll(
                    _state.value.messages.map {
                        ChatMessage(
                            role = it.role,
                            content = it.content
                        )
                    }
                )
                
                val responseBuilder = StringBuilder()
                
                ollamaService?.chat(
                    model = _state.value.currentModel,
                    messages = chatHistory,
                    temperature = 0.9, // Alta creatividad para respuestas sin censura
                    stream = true
                )?.collect { chunk ->
                    responseBuilder.append(chunk)
                    
                    // Update message in real-time
                    _state.update { currentState ->
                        val messages = currentState.messages.toMutableList()
                        
                        // Remove previous message if exists
                        if (messages.lastOrNull()?.role == "assistant") {
                            messages.removeLast()
                        }
                        
                        // Add updated message
                        messages.add(
                            AIMessage(
                                role = "assistant",
                                content = responseBuilder.toString(),
                                timestamp = getCurrentTime()
                            )
                        )
                        
                        currentState.copy(messages = messages)
                    }
                }
                
            } catch (e: Exception) {
                // Add error message
                val errorMessage = AIMessage(
                    role = "assistant",
                    content = "❌ Error: ${e.message ?: "No se pudo conectar con Ollama Cloud"}\n\n" +
                            "💡 Verifica:\n" +
                            "1. Ollama Cloud está corriendo\n" +
                            "2. La URL del servidor es correcta\n" +
                            "3. El modelo está descargado",
                    timestamp = getCurrentTime()
                )
                
                _state.update {
                    it.copy(messages = it.messages + errorMessage)
                }
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }
    
    fun clearChat() {
        _state.update {
            it.copy(
                messages = emptyList(),
                currentInput = ""
            )
        }
    }
    
    private fun getCurrentTime(): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }
}

data class OllamaAIState(
    val messages: List<AIMessage> = emptyList(),
    val currentInput: String = "",
    val isGenerating: Boolean = false,
    val isConnected: Boolean = false,
    val currentModel: String = "llama2-uncensored",
    val availableModels: List<String> = listOf("llama2-uncensored", "mistral-uncensored", "wizard-vicuna-uncensored"),
    val serverUrl: String = "http://localhost:11434",
    val selectedCategory: PromptCategory? = null
)
