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
import kotlinx.coroutines.flow.produceIn
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
    val selectedModel: String = AzelAIApiService.GEMINI_2_5_FLASH,
    val availableModels: List<com.Azelmods.App.data.api.AIModel> = emptyList(),
    val thinkingElapsedMs: Long = 0L,
    /** Proveedor y modelo reales (no un "API Key activa" fijo). */
    val providerName: String = "",
    val providerModel: String = "",
    val isProviderConfigured: Boolean = true
)

@HiltViewModel
class AzelAIViewModel @Inject constructor(
    private val repository: AzelAIRepository
) : ViewModel() {
    
    private fun currentUserId(): String =
        FirebaseAuth.getInstance().currentUser?.uid ?: ""
    
    private var messagesCollectJob: Job? = null
    private var thinkingTimerJob: Job? = null
    private var sendJob: Job? = null
    
    private val _state = MutableStateFlow(AzelAIState())
    val state: StateFlow<AzelAIState> = _state.asStateFlow()
    
    private val _chatHistory = MutableStateFlow<List<ChatHistoryItem>>(emptyList())
    val chatHistory: StateFlow<List<ChatHistoryItem>> = _chatHistory.asStateFlow()
    
    init {
        if (currentUserId().isNotEmpty()) {
            Log.d(TAG, "🚀 Initializing AzelAIViewModel for user: ${currentUserId()}")
            loadAvailableModels()
            refreshProviderStatus()
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
        refreshProviderStatus()
        Log.d(TAG, "🔄 Model changed to: $modelId")
    }

    /**
     * Relee proveedor, modelo y si hay clave. La pantalla lo llama al volver de
     * los ajustes de IA para que la cabecera refleje el cambio al momento.
     */
    fun refreshProviderStatus() {
        val (name, model) = repository.activeProviderSummary(_state.value.selectedModel)
        _state.update {
            it.copy(
                providerName = name,
                providerModel = model,
                isProviderConfigured = repository.isProviderConfigured()
            )
        }
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
     *
     * Plazos: antes había un tope FIJO de 60 s para toda la respuesta, así que una
     * respuesta larga que seguía llegando se cortaba a la mitad con "Tiempo de
     * espera agotado". Ahora el límite es de inactividad: [FIRST_TOKEN_TIMEOUT_MS]
     * para el primer trozo y [IDLE_TIMEOUT_MS] entre trozos; mientras el modelo
     * siga escribiendo, la respuesta no se corta.
     */
    fun sendMessage(content: String, useStreaming: Boolean = true) {
        if (content.isBlank() || _state.value.isThinking || _state.value.isStreaming) {
            Log.w(TAG, "⚠️ Cannot send message: blank or already processing")
            return
        }

        sendJob = viewModelScope.launch {
            val partial = StringBuilder()
            try {
                val userId = currentUserId()
                if (userId.isEmpty()) {
                    throw Exception("Usuario no autenticado")
                }
                if (!repository.isProviderConfigured()) {
                    throw IllegalStateException(com.Azelmods.App.data.api.AzelAIApiService.API_KEY_MISSING)
                }

                val currentChatId = _state.value.currentChatId
                val chatPath = "$userId/$currentChatId"

                if (_state.value.messages.isEmpty()) {
                    updateChatTitle(content)
                }

                val userMsg = AIMessage(
                    id = "user_${System.currentTimeMillis()}",
                    content = content,
                    role = "user",
                    timestamp = System.currentTimeMillis()
                )
                _state.update { it.copy(messages = it.messages + userMsg) }

                // isThinking DESPUÉS de añadir el mensaje para que el scroll automático
                // llegue al final y se vea el indicador.
                kotlinx.coroutines.delay(100)
                _state.update { it.copy(isThinking = true, isStreaming = useStreaming, streamingContent = "", error = null, thinkingElapsedMs = 0L) }

                thinkingTimerJob?.cancel()
                thinkingTimerJob = viewModelScope.launch {
                    while (_state.value.isThinking) {
                        kotlinx.coroutines.delay(250)
                        _state.update { it.copy(thinkingElapsedMs = it.thinkingElapsedMs + 250) }
                    }
                }

                try {
                    repository.saveMessage(chatPath, userMsg)
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Failed to save user message to Firebase: ${e.message}")
                }

                val history = _state.value.messages
                val selectedModel = _state.value.selectedModel
                var tokens = 0

                if (useStreaming) {
                    var firstChunk = true
                    repository.sendMessageStream(content, history, selectedModel)
                        .timeoutBetweenChunks(FIRST_TOKEN_TIMEOUT_MS, IDLE_TIMEOUT_MS) { firstChunk }
                        .collect { chunk ->
                            if (chunk.isNotEmpty()) {
                                firstChunk = false
                                partial.append(chunk)
                                _state.update { it.copy(streamingContent = partial.toString()) }
                            }
                        }
                } else {
                    val (aiResponse, usedTokens) = kotlinx.coroutines.withTimeoutOrNull(FIRST_TOKEN_TIMEOUT_MS) {
                        repository.sendMessage(content, history, selectedModel).getOrThrow()
                    } ?: throw AiResponseTimeoutException()
                    partial.append(aiResponse)
                    tokens = usedTokens
                }

                val fullResponse = partial.toString()
                if (fullResponse.isBlank()) throw Exception("La IA no devolvió ninguna respuesta")

                val aiMsg = AIMessage(
                    id = "ai_${System.currentTimeMillis()}",
                    content = fullResponse,
                    role = "assistant",
                    timestamp = System.currentTimeMillis(),
                    tokens = if (tokens > 0) tokens else fullResponse.split(" ").size,
                    model = _state.value.providerModel.ifBlank { selectedModel }
                )
                _state.update { it.copy(messages = it.messages + aiMsg) }

                try {
                    repository.saveMessage(chatPath, aiMsg)
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Failed to save AI response to Firebase: ${e.message}")
                }
                try {
                    repository.updateChatHistory(userId, currentChatId, _state.value.currentChatTitle, content)
                    loadChatHistory()
                } catch (e: Exception) {
                    Log.e(TAG, "⚠️ Failed to update chat history: ${e.message}")
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                // "Detener": se conserva lo que ya había escrito la IA.
                if (partial.isNotEmpty()) {
                    val stopped = AIMessage(
                        id = "ai_${System.currentTimeMillis()}",
                        content = partial.toString() + "\n\n_(respuesta detenida)_",
                        role = "assistant",
                        timestamp = System.currentTimeMillis()
                    )
                    _state.update { it.copy(messages = it.messages + stopped) }
                }
                throw e
            } catch (e: Exception) {
                CrashlyticsLogger.recordException(e, "ai", "sendMessage", "Error sending message")
                Log.e(TAG, "❌ Error sending message", e)
                showSendError(mapError(e))
            } finally {
                thinkingTimerJob?.cancel()
                _state.update { it.copy(isThinking = false, isStreaming = false, streamingContent = "", thinkingElapsedMs = 0L) }
            }
        }
    }

    /**
     * Trae el resultado de los agentes a la conversación actual, para poder
     * seguir preguntando sobre él en el chat ("Seguir en el chat").
     */
    fun importAgentResult(goal: String, answer: String) {
        if (answer.isBlank()) return
        val userId = currentUserId()
        val now = System.currentTimeMillis()
        val userMsg = AIMessage(id = "user_$now", content = "🤖 Tarea para los agentes: $goal", role = "user", timestamp = now)
        val aiMsg = AIMessage(id = "ai_${now + 1}", content = answer, role = "assistant", timestamp = now + 1)
        if (_state.value.messages.isEmpty()) updateChatTitle(goal)
        _state.update { it.copy(messages = it.messages + userMsg + aiMsg) }
        if (userId.isEmpty()) return
        val chatId = _state.value.currentChatId
        viewModelScope.launch {
            runCatching {
                repository.saveMessage("$userId/$chatId", userMsg)
                repository.saveMessage("$userId/$chatId", aiMsg)
                repository.updateChatHistory(userId, chatId, _state.value.currentChatTitle, goal)
                loadChatHistory()
            }.onFailure { Log.w(TAG, "No se pudo guardar el resultado de los agentes: ${it.message}") }
        }
    }

    /** Corta la respuesta en curso (botón "Detener"). */
    fun stopGeneration() {
        sendJob?.cancel()
    }

    private fun showSendError(errorMessage: String) {
        val errorMsg = AIMessage(
            id = "error_${System.currentTimeMillis()}",
            content = "❌ $errorMessage",
            role = "assistant",
            timestamp = System.currentTimeMillis(),
            error = true
        )
        _state.update { it.copy(error = errorMessage, messages = it.messages + errorMsg) }
    }

    private fun mapError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            e is AiResponseTimeoutException -> e.message.orEmpty()
            msg == com.Azelmods.App.data.api.AzelAIApiService.API_KEY_MISSING ->
                "No hay ninguna IA configurada. Añade tu clave o un modelo local en Ajustes → IA."
            msg.contains("no autenticado", ignoreCase = true) -> "Debes iniciar sesión para usar la IA"
            e is java.net.UnknownHostException -> "Sin conexión a internet"
            e is java.net.SocketTimeoutException -> "Tiempo de espera agotado"
            e is java.net.ConnectException -> "No se pudo conectar con el servidor de IA"
            msg.contains("401") -> "API key inválida o caducada"
            msg.contains("403") -> "Acceso denegado por el proveedor"
            msg.contains("404") -> "El modelo elegido no existe en este proveedor"
            msg.contains("429") -> "Límite de uso del proveedor alcanzado. Espera un momento."
            msg.contains("500") || msg.contains("502") || msg.contains("503") ->
                "El proveedor de IA no está disponible ahora mismo"
            else -> msg.ifBlank { "Error desconocido" }
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
     *
     * Quita el mensaje de error y la pregunta que lo provocó antes de reenviarla:
     * antes la pregunta se añadía otra vez y el historial quedaba duplicado.
     */
    fun retryLastMessage() {
        val messages = _state.value.messages
        val lastUserIndex = messages.indexOfLast { it.role == "user" && !it.error }
        if (lastUserIndex < 0) return
        val lastUserMessage = messages[lastUserIndex]
        Log.d(TAG, "🔄 Retrying last message")
        _state.update { it.copy(messages = messages.take(lastUserIndex), error = null) }
        sendMessage(lastUserMessage.content)
    }

    companion object {
        private const val TAG = "AzelAIViewModel"
        /** Margen para el primer trozo: modelos grandes y locales tardan en arrancar. */
        const val FIRST_TOKEN_TIMEOUT_MS = 90_000L
        /** Silencio máximo entre trozos una vez que la respuesta ha empezado. */
        const val IDLE_TIMEOUT_MS = 45_000L
    }
}

/** La IA se quedó callada más tiempo del permitido. */
class AiResponseTimeoutException :
    Exception("La IA no respondió a tiempo. Comprueba tu conexión o prueba otro modelo.")

/**
 * Lanza [AiResponseTimeoutException] si el flujo pasa más de [firstMs] sin emitir
 * nada al principio, o más de [idleMs] entre dos trozos.
 *
 * Es una excepción normal y no la `TimeoutCancellationException` de `withTimeout`
 * a propósito: esa es una cancelación y, al cruzar el canal, se confundiría con
 * que el usuario pulsó "Detener".
 */
@OptIn(kotlinx.coroutines.FlowPreview::class)
internal fun <T> kotlinx.coroutines.flow.Flow<T>.timeoutBetweenChunks(
    firstMs: Long,
    idleMs: Long,
    isFirst: () -> Boolean
): kotlinx.coroutines.flow.Flow<T> = kotlinx.coroutines.flow.channelFlow {
    val upstream = this@timeoutBetweenChunks.produceIn(this)
    while (true) {
        val limit = if (isFirst()) firstMs else idleMs
        val result = kotlinx.coroutines.withTimeoutOrNull(limit) { upstream.receiveCatching() }
            ?: throw AiResponseTimeoutException()
        if (result.isClosed) {
            result.exceptionOrNull()?.let { throw it }
            break
        }
        send(result.getOrThrow())
    }
}
