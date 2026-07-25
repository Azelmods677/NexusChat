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
 * Creates and manages the "Azel Assistant" demo account for testing
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
        private const val DEMO_USER_ID = "demo_azel_assistant"
        private const val DEMO_USER_NAME = "Azel Assistant"
        private const val DEMO_USERNAME = "@azel"
        private const val DEMO_BIO = "I'm here to help you explore AzelGram features!"
        private const val DEMO_EMAIL = "azel.assistant@nexuschat.app"
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
            // `email` es obligatorio: la regla .validate de users/$uid exige
            // displayName + email. Sin él la escritura se rechaza.
            val demoUserData = mapOf(
                "uid" to DEMO_USER_ID,
                "name" to DEMO_USER_NAME,
                "username" to DEMO_USERNAME,
                "displayName" to DEMO_USER_NAME,
                "email" to DEMO_EMAIL,
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
            // No se relanza: si el perfil del bot ya existe (lo crea el primer usuario
            // que instala la app) la escritura se rechaza por regla, y antes esa
            // excepción abortaba el chat y los mensajes de bienvenida por completo.
            Log.w(TAG, "No se pudo crear el perfil demo (puede que ya exista): ${e.message}")
        }
    }
    
    /**
     * Create demo chat between current user and demo user
     */
    private suspend fun createDemoChat(currentUserId: String) {
        try {
            val uidA = currentUserId
            val uidB = DEMO_USER_ID
            val chatId = if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
            val timestamp = System.currentTimeMillis()
            
            val membersMap = mapOf(currentUserId to true, DEMO_USER_ID to true)
            
            val demoChatData = mapOf(
                "chatId" to chatId,
                "members" to membersMap,
                "lastMessage" to "Try exploring the features! 🚀",
                "lastMessageTime" to timestamp,
                "lastMessageSenderId" to DEMO_USER_ID,
                "createdAt" to timestamp,
                "isGroup" to false
            )
            
            // updateChildren y no setValue: setValue reemplaza el nodo completo y
            // borraría los mensajes si el chat ya existiera.
            database.reference
                .child("chats")
                .child(chatId)
                .updateChildren(demoChatData)
                .await()
            
            // Update userChats index for both users
            database.reference.child("userChats").child(currentUserId).child(chatId).setValue(true).await()
            database.reference.child("userChats").child(DEMO_USER_ID).child(chatId).setValue(true).await()
            
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
            val uidA = currentUserId
            val uidB = DEMO_USER_ID
            val chatId = if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
            // chats/{chatId}/messages, NO el nodo raiz "messages": la app lee de aqui.
            // Antes escribia en messages/{chatId}, que ninguna pantalla consulta y que
            // ademas las reglas rechazan (no existe, hereda .write: false del root),
            // asi que el chat de bienvenida siempre aparecia vacio.
            val messagesRef = database.reference
                .child("chats")
                .child(chatId)
                .child("messages")

            val baseTime = System.currentTimeMillis() - (5 * 60 * 1000) // 5 minutes ago

            // Todos los mensajes son DEL asistente. Antes habia uno falso atribuido al
            // usuario ("¡Genial! Voy a explorar la app"): poner palabras en boca de
            // alguien que no las escribio no es aceptable en produccion.
            // Bienvenida CORTA y conversacional: el asistente responde de verdad a lo
            // que escribas (ver DemoAssistant), así que aquí solo se abre la charla en
            // vez de soltar todo el manual de golpe.
            val welcomeTexts = listOf(
                "¡Hola! 👋 Soy Azel Assistant, tu guía de bienvenida en NexusChat.",
                "Este chat es real: los mensajes se guardan en la base de datos, igual " +
                    "que en cualquier conversación. Escríbeme y te respondo.",
                "Puedo enseñarte cada función. Prueba a escribirme una de estas palabras:\n" +
                    "• *cifrado* — cómo protejo tus mensajes\n" +
                    "• *Tor* — navegación anónima con Orbot\n" +
                    "• *IA* — conectar tu propio proveedor y modelo\n" +
                    "• *historias* · *llamadas* · *código*",
                "¿Empezamos? Escríbeme lo que quieras. 🚀"
            )

            welcomeTexts.forEachIndexed { index, body ->
                val messageId = "demo_msg_${index + 1}"
                // El esquema debe ser el MISMO que usa sendMessage() real: la app lee
                // `content`, no `text`, y `status`, no `isRead`. Con los nombres viejos
                // los mensajes salian en blanco aunque estuvieran en el sitio correcto.
                val message = mapOf(
                    "messageId" to messageId,
                    "senderId" to DEMO_USER_ID,
                    "content" to body,
                    "timestamp" to baseTime + (index * 10_000L),
                    "status" to "sent",
                    "isEncrypted" to false,
                    "reactions" to emptyMap<String, String>()
                )
                messagesRef.child(messageId).setValue(message).await()
            }

            // Que el ultimo mensaje sea el que la lista de chats muestra como preview.
            database.reference.child("chats").child(chatId).updateChildren(
                mapOf(
                    "lastMessage" to welcomeTexts.last(),
                    "lastMessageTime" to baseTime + ((welcomeTexts.size - 1) * 10_000L),
                    "lastMessageSenderId" to DEMO_USER_ID
                )
            ).await()

            Log.d(TAG, "Demo messages created: ${welcomeTexts.size} messages")

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
