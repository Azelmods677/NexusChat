package com.Azelmods.App.data.demo

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Demo Account Manager
 * Creates and manages the "Kiro Assistant" demo account for testing
 */
@Singleton
class DemoAccountManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: FirebaseDatabase
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("demo_prefs", Context.MODE_PRIVATE)
    
    companion object {
        private const val TAG = "DemoAccountManager"
        private const val DEMO_ACCOUNT_CREATED_KEY = "demo_account_created"
        private const val DEMO_USER_ID = "demo_kiro_assistant"
        private const val DEMO_USER_NAME = "Kiro Assistant"
        private const val DEMO_USERNAME = "@kiro"
        private const val DEMO_BIO = "I'm here to help you explore NexusChat features!"
    }
    
    /**
     * Initialize demo account for the current user
     * Creates demo user, chat, and messages if not already created
     */
    suspend fun initializeDemoAccount(currentUserId: String) {
        try {
            // Check if demo account already created
            if (prefs.getBoolean(DEMO_ACCOUNT_CREATED_KEY, false)) {
                Log.d(TAG, "Demo account already created, skipping initialization")
                return
            }
            
            Log.d(TAG, "Initializing demo account for user: $currentUserId")
            
            // Create demo user in Firebase
            createDemoUser()
            
            // Create demo chat
            createDemoChat(currentUserId)
            
            // Create demo messages
            createDemoMessages(currentUserId)
            
            // Mark as created
            prefs.edit().putBoolean(DEMO_ACCOUNT_CREATED_KEY, true).apply()
            
            Log.d(TAG, "Demo account initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing demo account: ${e.message}", e)
        }
    }
    
    /**
     * Create demo user in Firebase
     */
    private suspend fun createDemoUser() {
        try {
            val demoUserData = mapOf(
                "uid" to DEMO_USER_ID,
                "name" to DEMO_USER_NAME,
                "username" to DEMO_USERNAME,
                "displayName" to DEMO_USER_NAME,
                "bio" to DEMO_BIO,
                "isOnline" to true,
                "lastSeen" to ServerValue.TIMESTAMP,
                "photoUrl" to "",
                "coverUrl" to "",
                "createdAt" to ServerValue.TIMESTAMP
            )
            
            database.reference
                .child("users")
                .child(DEMO_USER_ID)
                .setValue(demoUserData)
                .await()
            
            Log.d(TAG, "Demo user created: $DEMO_USER_ID")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating demo user: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Create demo chat between current user and demo user
     */
    private suspend fun createDemoChat(currentUserId: String) {
        try {
            val chatId = "${currentUserId}_${DEMO_USER_ID}"
            val timestamp = System.currentTimeMillis()
            
            val demoChatData = mapOf(
                "chatId" to chatId,
                "participants" to listOf(currentUserId, DEMO_USER_ID),
                "lastMessage" to "Try exploring the features! 🚀",
                "lastMessageTime" to timestamp,
                "lastMessageSenderId" to DEMO_USER_ID,
                "createdAt" to timestamp,
                "isGroup" to false
            )
            
            database.reference
                .child("chats")
                .child(chatId)
                .setValue(demoChatData)
                .await()
            
            Log.d(TAG, "Demo chat created: $chatId")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating demo chat: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Create demo messages in the chat
     */
    private suspend fun createDemoMessages(currentUserId: String) {
        try {
            val chatId = "${currentUserId}_${DEMO_USER_ID}"
            val messagesRef = database.reference.child("messages").child(chatId)
            
            val baseTime = System.currentTimeMillis() - (5 * 60 * 1000) // 5 minutes ago
            
            // Message 1: Welcome from Kiro
            val message1 = mapOf(
                "messageId" to "demo_msg_1",
                "senderId" to DEMO_USER_ID,
                "senderName" to DEMO_USER_NAME,
                "text" to "¡Hola! 👋 Soy Kiro Assistant, tu guía en NexusChat.",
                "timestamp" to baseTime,
                "type" to "TEXT",
                "isRead" to false
            )
            messagesRef.child("demo_msg_1").setValue(message1).await()
            
            // Message 2: Introduction
            val message2 = mapOf(
                "messageId" to "demo_msg_2",
                "senderId" to DEMO_USER_ID,
                "senderName" to DEMO_USER_NAME,
                "text" to "Estoy aquí para ayudarte a explorar todas las funciones de la app.",
                "timestamp" to baseTime + 10000,
                "type" to "TEXT",
                "isRead" to false
            )
            messagesRef.child("demo_msg_2").setValue(message2).await()
            
            // Message 3: Feature exploration prompt
            val message3 = mapOf(
                "messageId" to "demo_msg_3",
                "senderId" to DEMO_USER_ID,
                "senderName" to DEMO_USER_NAME,
                "text" to "Puedes probar:\n• Enviar mensajes de texto\n• Compartir fotos y videos\n• Hacer llamadas de voz/video\n• Crear historias\n• Navegar de forma anónima con Tor",
                "timestamp" to baseTime + 20000,
                "type" to "TEXT",
                "isRead" to false
            )
            messagesRef.child("demo_msg_3").setValue(message3).await()
            
            // Message 4: Sample user reply
            val message4 = mapOf(
                "messageId" to "demo_msg_4",
                "senderId" to currentUserId,
                "senderName" to "Tú",
                "text" to "¡Genial! Voy a explorar la app 😊",
                "timestamp" to baseTime + 30000,
                "type" to "TEXT",
                "isRead" to true
            )
            messagesRef.child("demo_msg_4").setValue(message4).await()
            
            // Message 5: Follow-up from Kiro
            val message5 = mapOf(
                "messageId" to "demo_msg_5",
                "senderId" to DEMO_USER_ID,
                "senderName" to DEMO_USER_NAME,
                "text" to "¡Perfecto! Si necesitas ayuda, siempre puedes volver a este chat. 💬",
                "timestamp" to baseTime + 40000,
                "type" to "TEXT",
                "isRead" to false
            )
            messagesRef.child("demo_msg_5").setValue(message5).await()
            
            // Message 6: Security tip
            val message6 = mapOf(
                "messageId" to "demo_msg_6",
                "senderId" to DEMO_USER_ID,
                "senderName" to DEMO_USER_NAME,
                "text" to "💡 Tip: Activa el modo anónimo en Configuración > Seguridad para navegar con Tor.",
                "timestamp" to baseTime + 50000,
                "type" to "TEXT",
                "isRead" to false
            )
            messagesRef.child("demo_msg_6").setValue(message6).await()
            
            // Message 7: Final message
            val message7 = mapOf(
                "messageId" to "demo_msg_7",
                "senderId" to DEMO_USER_ID,
                "senderName" to DEMO_USER_NAME,
                "text" to "¡Disfruta explorando NexusChat! 🚀",
                "timestamp" to baseTime + 60000,
                "type" to "TEXT",
                "isRead" to false
            )
            messagesRef.child("demo_msg_7").setValue(message7).await()
            
            Log.d(TAG, "Demo messages created: 7 messages")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating demo messages: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * Reset demo account (for testing)
     */
    fun resetDemoAccount() {
        prefs.edit().putBoolean(DEMO_ACCOUNT_CREATED_KEY, false).apply()
        Log.d(TAG, "Demo account reset")
    }
}
