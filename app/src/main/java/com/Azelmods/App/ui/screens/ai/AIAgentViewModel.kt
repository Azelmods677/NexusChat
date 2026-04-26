package com.Azelmods.App.ui.screens.ai

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.agent.AgentToolParser
import com.Azelmods.App.data.api.OllamaApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AIAgentViewModel @Inject constructor(
    private val ollamaService: OllamaApiService,
    @ApplicationContext private val context: Context
) : ViewModel() {
    
    private val toolParser = AgentToolParser(context)
    private val _uiState = MutableStateFlow(AIAgentUiState())
    val uiState: StateFlow<AIAgentUiState> = _uiState.asStateFlow()
    
    init {
        checkConnection()
    }
    
    private fun checkConnection() {
        viewModelScope.launch {
            val ollamaAvailable = ollamaService.isServerAvailable()
            _uiState.update { it.copy(isConnected = ollamaAvailable) }
        }
    }
    
    fun updateInput(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }
    
    fun sendMessage() {
        val input = _uiState.value.inputText.trim()
        if (input.isBlank()) return
        
        val userMessage = AgentMessage(
            content = input,
            isUser = true,
            timestamp = getCurrentTime()
        )
        
        _uiState.update {
            it.copy(
                messages = it.messages + userMessage,
                inputText = "",
                isLoading = true
            )
        }
        
        sendToOllama(input)
    }
    
    private fun sendToOllama(prompt: String) {
        viewModelScope.launch {
            try {
                val conversationHistory = _uiState.value.messages
                    .filter { !it.isUser || it.content != prompt }
                    .map { com.Azelmods.App.data.api.ChatMessage(
                        role = if (it.isUser) "user" else "assistant",
                        content = it.content
                    )}
                
                // Add system prompt with tool instructions
                val systemMessage = com.Azelmods.App.data.api.ChatMessage(
                    role = "system",
                    content = toolParser.getSystemPrompt()
                )
                
                val responseBuilder = StringBuilder()
                
                ollamaService.chat(
                    model = "llama2",
                    messages = listOf(systemMessage) + conversationHistory + 
                              com.Azelmods.App.data.api.ChatMessage("user", prompt),
                    stream = true
                ).collect { chunk ->
                    responseBuilder.append(chunk)
                    
                    // Update UI with streaming response
                    _uiState.update { state ->
                        val messages = state.messages.toMutableList()
                        if (messages.lastOrNull()?.isUser == false) {
                            messages[messages.lastIndex] = messages.last().copy(
                                content = responseBuilder.toString()
                            )
                        } else {
                            messages.add(AgentMessage(
                                content = responseBuilder.toString(),
                                isUser = false,
                                timestamp = getCurrentTime()
                            ))
                        }
                        state.copy(messages = messages)
                    }
                }
                
                // Parse and execute tools after response is complete
                val finalResponse = responseBuilder.toString()
                val parsedResponse = toolParser.parseAndExecute(finalResponse)
                
                if (parsedResponse.hasTools) {
                    // Update message with tool execution results
                    _uiState.update { state ->
                        val messages = state.messages.toMutableList()
                        if (messages.lastOrNull()?.isUser == false) {
                            messages[messages.lastIndex] = messages.last().copy(
                                content = parsedResponse.modifiedText
                            )
                        }
                        state.copy(messages = messages)
                    }
                }
                
                _uiState.update { it.copy(isLoading = false) }
                
            } catch (e: Exception) {
                handleError("Ollama error: ${e.message}")
            }
        }
    }
    
    private fun handleError(error: String) {
        _uiState.update {
            it.copy(
                messages = it.messages + AgentMessage(
                    content = "❌ $error",
                    isUser = false,
                    timestamp = getCurrentTime()
                ),
                isLoading = false
            )
        }
    }
    
    fun clearChat() {
        _uiState.update { it.copy(messages = emptyList()) }
    }
    
    private fun getCurrentTime(): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
    }
}

data class AIAgentUiState(
    val messages: List<AgentMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val isConnected: Boolean = true
)

data class AgentMessage(
    val content: String,
    val isUser: Boolean,
    val timestamp: String
)
