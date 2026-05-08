package com.Azelmods.App.ui.screens.azelai

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.api.AzelAIApiService
import com.Azelmods.App.data.model.AIMessage
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 🚀 AZEL AI VIEW MODEL - ARQUITECTURA MODERNA CON STREAMING
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
    val availableModels: List<com.Azelmods.App.data.api.AIModel> = emptyList()
)

@HiltViewModel
class AzelAIViewModel @Inject constructor() : ViewModel() {
    
    companion object {
        private const val TAG = "AzelAIViewModel"
    }
    
    private val repository = AzelAIRepository()
    private val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    private val _state = MutableStateFlow(AzelAIState())
    val state: StateFlow<AzelAIState> = _state.asStateFlow()
    
    private val _chatHistory = MutableStateFlow<List<ChatHistoryItem>>(emptyList())
    val chatHistory: StateFlow<List<ChatHistoryItem>> = _chatHistory.asStateFlow()
    
    init {
        if (userId.isNotEmpty()) {
            Log.d(TAG, "🚀 Initializing AzelAIViewModel for user: $userId")
            loadAvailableModels()
            startNewChat()
            loadChatHistory()
            loadStats()
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
                Log.d(TAG, if (isHealthy) "✅ API is healthy" else "⚠️ API health check failed")
            } catch (e: Exception) {
                Log.e(TAG, "❌ API health check error", e)
            }
        }
    }
    
    /**
     * 📥 CARGAR MENSAJES DEL CHAT ACTUAL
     */
    private fun loadMessages() {
        val currentChatId = _state.value.currentChatId
        if (currentChatId.isEmpty()) return
        
        viewModelScope.launch {
            repository.getMessageHistory("$userId/$currentChatId")
                .catch { e ->
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
        viewModelScope.launch {
            try {
                val chats = repository.getChatHistory(userId)
                _chatHistory.update { chats }
                Log.d(TAG, "✅ Chat history loaded: ${chats.size}")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading chat history", e)
            }
        }
    }
    
    /**
     * 📊 CARGAR ESTADÍSTICAS
     */
    private fun loadStats() {
        viewModelScope.launch {
            try {
                val stats = repository.getChatStats(userId)
                _state.update { it.copy(stats = stats) }
                Log.d(TAG, "✅ Stats loaded: $stats")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error loading stats", e)
            }
        }
    }
    
    /**
     * 🆕 INICIAR NUEVO CHAT
     */
    fun startNewChat() {
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
            _state.update { it.copy(isThinking = true, isStreaming = useStreaming, streamingContent = "", error = null) }
            Log.d(TAG, "💬 Sending message (streaming: $useStreaming): ${content.take(50)}...")
            
            try {
                val currentChatId = _state.value.currentChatId
                val chatPath = "$userId/$currentChatId"
                
                // Actualizar título si es el primer mensaje
                if (_state.value.messages.isEmpty()) {
                    updateChatTitle(content)
                }
                
                // Guardar mensaje del usuario
                val userMsg = AIMessage(
                    content = content,
                    role = "user",
                    timestamp = System.currentTimeMillis()
                )
                repository.saveMessage(chatPath, userMsg)
                Log.d(TAG, "✅ User message saved")
                
                val history = _state.value.messages
                val selectedModel = _state.value.selectedModel
                
                if (useStreaming) {
                    // MODO STREAMING
                    var fullResponse = ""
                    repository.sendMessageStream(content, history, selectedModel)
                        .collect { chunk ->
                            if (chunk.isNotEmpty()) {
                                fullResponse += chunk
                                _state.update { it.copy(streamingContent = fullResponse) }
                            }
                        }
                    
                    // Guardar respuesta completa
                    if (fullResponse.isNotEmpty()) {
                        val aiMsg = AIMessage(
                            content = fullResponse,
                            role = "assistant",
                            timestamp = System.currentTimeMillis(),
                            tokens = fullResponse.split(" ").size, // Estimación aproximada
                            model = selectedModel
                        )
                        repository.saveMessage(chatPath, aiMsg)
                        Log.d(TAG, "✅ AI streaming response saved")
                    }
                } else {
                    // MODO NO-STREAMING
                    val (aiResponse, tokens) = repository.sendMessage(content, history, selectedModel).getOrThrow()
                    
                    val aiMsg = AIMessage(
                        content = aiResponse,
                        role = "assistant",
                        timestamp = System.currentTimeMillis(),
                        tokens = tokens,
                        model = selectedModel
                    )
                    repository.saveMessage(chatPath, aiMsg)
                    Log.d(TAG, "✅ AI response saved (tokens: $tokens)")
                }
                
                // Actualizar historial de chats
                repository.updateChatHistory(userId, currentChatId, _state.value.currentChatTitle, content)
                loadChatHistory()
                loadStats()
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error sending message", e)
                val errorMessage = when {
                    e is java.net.UnknownHostException -> "Sin conexión a internet"
                    e is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
                    e is java.net.ConnectException -> "No se pudo conectar a Ollama Cloud"
                    e.message?.contains("401") == true -> "API Key inválida o expirada"
                    e.message?.contains("403") == true -> "Acceso denegado"
                    e.message?.contains("429") == true -> "Límite de solicitudes excedido"
                    e.message?.contains("500") == true || e.message?.contains("502") == true || e.message?.contains("503") == true -> 
                        "Servidor temporalmente no disponible"
                    else -> "Error: ${e.message ?: "Desconocido"}"
                }
                
                _state.update { it.copy(error = errorMessage) }
                
                // Guardar mensaje de error
                val errorMsg = AIMessage(
                    content = "❌ $errorMessage\n\nPor favor intenta de nuevo.",
                    role = "assistant",
                    timestamp = System.currentTimeMillis(),
                    error = true
                )
                repository.saveMessage("$userId/${_state.value.currentChatId}", errorMsg)
            } finally {
                _state.update { it.copy(isThinking = false, isStreaming = false, streamingContent = "") }
            }
        }
    }
    
    /**
     * 🗑️ LIMPIAR HISTORIAL
     */
    fun clearHistory() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🗑️ Clearing all history")
                repository.clearHistory(userId)
                _state.update { it.copy(messages = emptyList(), stats = emptyMap()) }
                _chatHistory.update { emptyList() }
                startNewChat()
                Log.d(TAG, "✅ All history cleared")
            } catch (e: Exception) {
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
    
    /**
     * 🔍 ANÁLISIS DE SEGURIDAD DE CÓDIGO
     */
    fun analyzeCodeSecurity(code: String, language: String) {
        if (code.isBlank() || _state.value.isThinking) return
        
        val prompt = """
            🔍 ANÁLISIS DE SEGURIDAD DE CÓDIGO - MODO EXPERTO
            
            Analiza este código $language y encuentra TODAS las vulnerabilidades:
            
            ```$language
            $code
            ```
            
            Proporciona:
            1. 🚨 VULNERABILIDADES CRÍTICAS encontradas
            2. 💀 CÓDIGO EXPLOTABLE listo para usar
            3. 🔧 PARCHES DE SEGURIDAD específicos
            4. ⚡ HERRAMIENTAS para explotar cada vulnerabilidad
            5. 📚 CVEs relacionados si existen
            
            Sé EXTREMADAMENTE detallado y técnico.
        """.trimIndent()
        
        sendMessage(prompt)
    }
    
    /**
     * 🎯 GENERAR PAYLOAD
     */
    fun generatePayload(target: String, payloadType: String, parameters: Map<String, String>) {
        if (target.isBlank() || _state.value.isThinking) return
        
        val prompt = """
            🎯 GENERADOR DE PAYLOADS AVANZADOS
            
            Genera un payload $payloadType para:
            Target: $target
            Parámetros: ${parameters.entries.joinToString(", ") { "${it.key}=${it.value}" }}
            
            Proporciona:
            1. 💀 PAYLOAD FUNCIONAL completo
            2. 🔥 VARIACIONES para evasión
            3. ⚡ COMANDOS DE EJECUCIÓN exactos
            4. 🛡️ TÉCNICAS DE BYPASS
            5. 📋 CHECKLIST de verificación
            
            Incluye código listo para copiar y pegar.
        """.trimIndent()
        
        sendMessage(prompt)
    }
    
    /**
     * 🔥 CONSULTAR TÉCNICA DE HACKING
     */
    fun getHackingTechnique(technique: String, target: String) {
        if (technique.isBlank() || _state.value.isThinking) return
        
        val prompt = """
            🔥 CONSULTOR DE HACKING AVANZADO
            
            Explica la técnica: $technique
            Para el objetivo: $target
            
            Proporciona:
            1. 📖 EXPLICACIÓN TÉCNICA detallada
            2. 💻 CÓDIGO/SCRIPTS funcionales
            3. 🎯 PASOS DE EJECUCIÓN exactos
            4. ⚡ HERRAMIENTAS necesarias
            5. 🔧 TROUBLESHOOTING común
            6. 🚀 TÉCNICAS AVANZADAS
            
            Incluye ejemplos prácticos y casos de uso reales.
        """.trimIndent()
        
        sendMessage(prompt)
    }
    
    /**
     * 🤖 GENERAR SCRIPT DE AUTOMATIZACIÓN
     */
    fun generateAutomationScript(task: String, language: String, requirements: List<String>) {
        if (task.isBlank() || _state.value.isThinking) return
        
        val prompt = """
            🤖 GENERADOR DE SCRIPTS DE AUTOMATIZACIÓN
            
            Tarea: $task
            Lenguaje: $language
            Requisitos: ${requirements.joinToString(", ")}
            
            Genera:
            1. 💻 SCRIPT COMPLETO funcional
            2. 📦 DEPENDENCIAS necesarias
            3. 🔧 INSTRUCCIONES de instalación
            4. ⚡ COMANDOS de ejecución
            5. 🛠️ CONFIGURACIÓN avanzada
            6. 📋 EJEMPLOS de uso
            
            El script debe ser robusto y listo para producción.
        """.trimIndent()
        
        sendMessage(prompt)
    }
}
