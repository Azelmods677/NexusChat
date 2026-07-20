package com.Azelmods.App.ui.screens.azelai

import android.util.Log
import com.Azelmods.App.data.ai.GeminiContextManager
import com.Azelmods.App.data.api.AzelAIApiService
import com.Azelmods.App.data.api.Message
import com.Azelmods.App.data.api.StreamResponse
import com.Azelmods.App.data.model.AIMessage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🚀 AZEL AI REPOSITORY - REPOSITORIO AVANZADO CON STREAMING
 * Gestiona la comunicación con la API y Firebase
 */
@Singleton
class AzelAIRepository @Inject constructor(
    private val apiService: AzelAIApiService,
    private val contextManager: GeminiContextManager
) {
    
    companion object {
        private const val TAG = "AzelAIRepository"
    }
    
    private fun buildApiMessages(
        conversationHistory: List<AIMessage>,
        userMessage: String
    ): List<Message> {
        val maxHistory = 10 // Mantener solo los últimos 10 mensajes para optimizar tokens
        
        val filteredHistory = conversationHistory
            .filter { !it.isLoading && !it.error && it.role != "system" }
            
        // Usar GeminiContextManager para trim seguro del historial
        val trimmedHistory = contextManager.trimHistory(filteredHistory, maxHistory)

        val messages = trimmedHistory
            .map { msg -> 
                // Optimizamos tokens truncando respuestas largas de la IA, pero respetando los prompts del usuario
                val optimizedContent = if (msg.role == "assistant" && msg.content.length > 1500) {
                    msg.content.take(1500) + "\n\n[...Texto truncado para ahorrar tokens...]"
                } else {
                    msg.content
                }
                Message(msg.role, optimizedContent)
            }
            .toMutableList()
            
        val lastIsDuplicateUser = messages.lastOrNull()?.let {
            it.role == "user" && it.content == userMessage
        } == true
        if (!lastIsDuplicateUser) {
            messages.add(Message("user", userMessage))
        }
        return messages
    }
    private val database = FirebaseDatabase.getInstance()
    private val auth = FirebaseAuth.getInstance()
    
    /**
     * 🔥 ENVIAR MENSAJE CON STREAMING EN TIEMPO REAL
     */
    fun sendMessageStream(
        userMessage: String,
        conversationHistory: List<AIMessage>,
        model: String = AzelAIApiService.GEMINI_2_5_FLASH
    ): Flow<String> {
        val messages = buildApiMessages(conversationHistory, userMessage)
        
        return apiService.chatCompletionStream(
            model = model,
            messages = messages
        ).map { response ->
            when (response) {
                is StreamResponse.Content -> response.text
                is StreamResponse.Error -> throw Exception(response.message)
                is StreamResponse.Done -> ""
            }
        }
    }
    
    /**
     * 💬 ENVIAR MENSAJE SIN STREAMING (FALLBACK)
     */
    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<AIMessage>,
        model: String = AzelAIApiService.GEMINI_2_5_FLASH
    ): Result<Pair<String, Int>> = runCatching {
        val messages = buildApiMessages(conversationHistory, userMessage)
        
        val response = apiService.chatCompletion(
            model = model,
            messages = messages
        )
        
        Log.d(TAG, "✅ Response received: ${response.content.take(100)}...")
        Pair(response.content, response.tokens)
    }
    
    /**
     * 📊 OBTENER MODELOS DISPONIBLES
     */
    fun getAvailableModels() = apiService.getAvailableModels()
    
    /**
     * 🔍 VERIFICAR SALUD DE LA API
     */
    suspend fun checkApiHealth() = apiService.checkHealth()
    
    /**
     * Obtener historial en tiempo real desde Firebase
     */
    fun getMessageHistory(chatPath: String): Flow<List<AIMessage>> = callbackFlow {
        val ref = database.getReference("aiChats/$chatPath/messages")
            .orderByChild("timestamp")
            .limitToLast(100)
        
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    try {
                        child.getValue(AIMessage::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing message", e)
                        null
                    }
                }.sortedBy { it.timestamp }
                
                Log.d(TAG, "Loaded ${messages.size} messages from Firebase")
                trySend(messages)
            }
            
            override fun onCancelled(error: DatabaseError) {
                Log.e(TAG, "Firebase listener cancelled", error.toException())
                close(error.toException())
            }
        }
        
        ref.addValueEventListener(listener)
        
        awaitClose {
            ref.removeEventListener(listener)
            Log.d(TAG, "Firebase listener removed")
        }
    }
    
    /**
     * Guardar mensaje en Firebase (historial persistente)
     */
    suspend fun saveMessage(chatPath: String, message: AIMessage) {
        try {
            val userId = auth.currentUser?.uid
            if (userId == null) {
                Log.e(TAG, "❌ User not authenticated, cannot save message")
                return
            }
            
            val ref = database.getReference("aiChats/$chatPath/messages").push()
            val msgWithId = message.copy(id = ref.key ?: "")
            ref.setValue(msgWithId).await()
            
            // Actualizar lastActivity y estadísticas
            val updates = mapOf(
                "aiChats/$chatPath/lastActivity" to System.currentTimeMillis(),
                "aiChats/$chatPath/messageCount" to com.google.firebase.database.ServerValue.increment(1),
                "aiChats/$chatPath/totalTokens" to com.google.firebase.database.ServerValue.increment(message.tokens.toLong())
            )
            database.reference.updateChildren(updates).await()
            
            Log.d(TAG, "Message saved to Firebase: ${message.id}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving message to Firebase", e)
            throw e
        }
    }
    
    /**
     * Obtener historial de chats
     */
    suspend fun getChatHistory(userId: String): List<com.Azelmods.App.ui.screens.azelai.ChatHistoryItem> {
        return try {
            val snapshot = database.getReference("aiChats/$userId").get().await()
            val chats = mutableListOf<com.Azelmods.App.ui.screens.azelai.ChatHistoryItem>()
            
            snapshot.children.forEach { chatSnapshot ->
                val chatId = chatSnapshot.key ?: return@forEach
                if (chatId.startsWith("chat_")) {
                    val title = chatSnapshot.child("title").getValue(String::class.java) ?: "Chat sin título"
                    val lastMessage = chatSnapshot.child("lastMessage").getValue(String::class.java) ?: ""
                    val lastActivity = chatSnapshot.child("lastActivity").getValue(Long::class.java) ?: 0L
                    val messageCount = chatSnapshot.child("messageCount").getValue(Int::class.java) ?: 0
                    
                    chats.add(
                        com.Azelmods.App.ui.screens.azelai.ChatHistoryItem(
                            id = chatId,
                            title = title,
                            lastMessage = lastMessage,
                            lastActivity = lastActivity,
                            messageCount = messageCount
                        )
                    )
                }
            }
            
            chats.sortedByDescending { it.lastActivity }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat history", e)
            emptyList()
        }
    }
    
    /**
     * Actualizar historial de chat
     */
    suspend fun updateChatHistory(userId: String, chatId: String, title: String, lastMessage: String) {
        try {
            val updates = mapOf(
                "aiChats/$userId/$chatId/title" to title,
                "aiChats/$userId/$chatId/lastMessage" to lastMessage.take(100),
                "aiChats/$userId/$chatId/lastActivity" to System.currentTimeMillis()
            )
            database.reference.updateChildren(updates).await()
            Log.d(TAG, "Chat history updated: $chatId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating chat history", e)
        }
    }
    
    /**
     * Limpiar historial de conversación
     */
    suspend fun clearHistory(userId: String) {
        try {
            database.getReference("aiChats/$userId").removeValue().await()
            Log.d(TAG, "All history cleared for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing history", e)
            throw e
        }
    }
    
    suspend fun getChatStats(userId: String): Map<String, Any> {
        return try {
            val snapshot = database.getReference("aiChats/$userId").get().await()
            var messageCount = 0
            var totalTokens = 0
            var lastActivity = 0L
            snapshot.children.forEach { chatSnapshot ->
                val chatId = chatSnapshot.key ?: return@forEach
                if (!chatId.startsWith("chat_")) return@forEach
                messageCount += chatSnapshot.child("messageCount").getValue(Int::class.java) ?: 0
                totalTokens += chatSnapshot.child("totalTokens").getValue(Int::class.java) ?: 0
                val activity = chatSnapshot.child("lastActivity").getValue(Long::class.java) ?: 0L
                if (activity > lastActivity) lastActivity = activity
            }
            mapOf(
                "messageCount" to messageCount,
                "totalTokens" to totalTokens,
                "lastActivity" to lastActivity
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat stats", e)
            emptyMap()
        }
    }
}
