package com.Azelmods.App.data.repository

import android.net.Uri
import android.util.Log
import com.Azelmods.App.BuildConfig
import com.Azelmods.App.data.local.CacheManager
import com.Azelmods.App.data.security.encryption.E2EECryptoService
import com.Azelmods.App.util.CrashlyticsLogger
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Suppress("UNCHECKED_CAST")

@Singleton
class RealtimeDatabaseRepository @Inject constructor(
    private val e2eeCryptoService: E2EECryptoService
) {
    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    fun getChatMessages(chatId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val messagesRef = database.child("chats").child(chatId).child("messages")
            .orderByChild("timestamp")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    val map = child.value as? Map<String, Any> ?: return@mapNotNull null
                    map.toMutableMap().apply { put("messageId", child.key ?: "") }
                }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    fun getChatMessagesPaginated(chatId: String, limit: Int): Flow<List<Map<String, Any>>> = callbackFlow {
        val messagesRef = database.child("chats").child(chatId).child("messages")
            .orderByChild("timestamp")
            .limitToLast(limit)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { child ->
                    val map = child.value as? Map<String, Any> ?: return@mapNotNull null
                    map.toMutableMap().apply { put("messageId", child.key ?: "") }
                }
                trySend(messages)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        messagesRef.addValueEventListener(listener)
        awaitClose { messagesRef.removeEventListener(listener) }
    }

    suspend fun loadMoreMessages(chatId: String, beforeTimestamp: Long, limit: Int): List<Map<String, Any>> {
        return try {
            val snapshot = database.child("chats").child(chatId).child("messages")
                .orderByChild("timestamp")
                .endBefore(beforeTimestamp.toDouble())
                .limitToLast(limit)
                .get().await()

            snapshot.children.mapNotNull { child ->
                val map = child.value as? Map<String, Any> ?: return@mapNotNull null
                map.toMutableMap().apply { put("messageId", child.key ?: "") }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun hasMoreMessages(chatId: String, beforeTimestamp: Long): Boolean {
        return try {
            val snapshot = database.child("chats").child(chatId).child("messages")
                .orderByChild("timestamp")
                .endBefore(beforeTimestamp.toDouble())
                .limitToLast(1)
                .get().await()
            snapshot.hasChildren()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun sendMessage(chatId: String, content: String, replyTo: String? = null) {
        android.util.Log.d("RealtimeDB", "📤 sendMessage called: chatId=$chatId, content='${content.take(50)}', replyTo=$replyTo")
        sendMessageInternal(chatId, content, replyTo)
        android.util.Log.d("RealtimeDB", "✅ sendMessage completed successfully")
    }

    suspend fun sendEphemeralMessage(
        chatId: String,
        content: String,
        replyTo: String? = null,
        isViewOnce: Boolean = false,
        selfDestructDuration: Long = 0
    ) {
        sendMessageInternal(
            chatId = chatId,
            content = content,
            replyTo = replyTo,
            isEphemeral = true,
            isViewOnce = isViewOnce,
            selfDestructDuration = selfDestructDuration
        )
    }

    private suspend fun sendMessageInternal(
        chatId: String,
        content: String,
        replyTo: String? = null,
        isEphemeral: Boolean = false,
        isViewOnce: Boolean = false,
        selfDestructDuration: Long = 0
    ) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        // ══════════════════════════════════════════════════════════════════
        // 🔧 FIX BUG #5 y #6: CREAR CHAT SI NO EXISTE Y ACTUALIZAR ÍNDICE
        // ══════════════════════════════════════════════════════════════════
        if (!chatSnapshot.exists()) {
            android.util.Log.d("RealtimeDB", "🆕 Chat $chatId no existe, creando...")
            
            // Extraer miembros del chatId (formato: "uid1_uid2" o "group_timestamp")
            val memberIds = if (chatId.startsWith("group_")) {
                // Para grupos, necesitamos los miembros del grupo
                // Por ahora, asumimos que el grupo ya fue creado con createGroup()
                listOf(currentUserId)
            } else {
                // Para chats 1:1, extraer los dos UIDs del chatId
                chatId.split("_").filter { it.isNotBlank() && it != currentUserId } + currentUserId
            }
            
            val membersMap = memberIds.distinct().associateWith { true }
            
            // Crear el nodo del chat en Firebase
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to membersMap,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to "",
                "isE2EE" to true // Habilitado por defecto
            )
            
            try {
                chatRef.setValue(chatData).await()
                android.util.Log.d("RealtimeDB", "✅ Chat $chatId creado exitosamente")
            } catch (e: Exception) {
                android.util.Log.e("RealtimeDB", "❌ Error creando chat: ${e.message}")
                throw e
            }
            
            // ══════════════════════════════════════════════════════════════════
            // 🔥 CRÍTICO: ACTUALIZAR ÍNDICE userChats PARA CADA MIEMBRO
            // Esto hace que el chat aparezca en la HomeScreen
            // ══════════════════════════════════════════════════════════════════
            try {
                updateUserChatsIndex(chatId, memberIds.distinct())
                android.util.Log.d("RealtimeDB", "✅ Índice userChats actualizado para ${memberIds.size} miembros")
            } catch (e: Exception) {
                android.util.Log.e("RealtimeDB", "❌ Error actualizando índice userChats: ${e.message}")
                // No lanzamos excepción aquí para que el mensaje se envíe de todos modos
            }
        }

        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        var displayContent = content
        var isEncrypted = false
        var encryptedPayload: String? = null
        val members = chatId.split("_").filter { it.isNotBlank() }
        
        if (members.size == 2) {
            val recipientId = members.firstOrNull { it != currentUserId }
            if (recipientId != null) {
                // La encriptación E2EE NUNCA debe bloquear el envío: si las claves
                // no están listas o el cifrado falla (sin claves del destinatario,
                // sin red, etc.) se envía en texto plano como fallback.
                try {
                    e2eeCryptoService.ensureLocalKeys()
                    when (val enc = e2eeCryptoService.encryptFor(recipientId, content)) {
                        is com.Azelmods.App.data.security.encryption.EncryptionResult.Success -> {
                            isEncrypted = true
                            encryptedPayload = android.util.Base64.encodeToString(
                                enc.ciphertext, android.util.Base64.NO_WRAP
                            )
                            displayContent = "🔒 Mensaje cifrado de extremo a extremo"
                        }
                        else -> {}
                    }
                } catch (e: Exception) {
                    android.util.Log.e("RealtimeDB", "E2EE falló, enviando en texto plano", e)
                }
            }
        }

        val messageData = hashMapOf<String, Any>(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to displayContent,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>(),
            "isEncrypted" to isEncrypted
        )
        if (encryptedPayload != null) {
            messageData["encryptedPayload"] = encryptedPayload
        }
        if (isEncrypted) {
            chatRef.child("isE2EE").setValue(true).await()
        }

        if (replyTo != null) {
            messageData["replyTo"] = replyTo
        }

        if (isEphemeral) {
            val destructAt = if (isViewOnce) 0L else if (selfDestructDuration > 0) System.currentTimeMillis() + (selfDestructDuration * 1000) else 0L
            messageData["isEphemeral"] = true
            messageData["isViewOnce"] = isViewOnce
            messageData["selfDestructDuration"] = selfDestructDuration
            messageData["selfDestructAt"] = destructAt
            messageData["viewedBy"] = emptyList<String>()
            if (!isEncrypted) {
                messageData["content"] = content
            }
        }

        chatRef.child("messages").child(messageId).setValue(messageData).await()

        val lastMessageText = if (isEphemeral) {
            when {
                isViewOnce -> "📷 Foto única"
                selfDestructDuration > 0 -> "🕐 Mensaje temporal"
                else -> content
            }
        } else {
            content
        }

        val lastMessageData = mapOf(
            "lastMessage" to lastMessageText,
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )
        chatRef.updateChildren(lastMessageData).await()

        sendFcmForMessage(
            chatId = chatId,
            senderId = currentUserId,
            content = if (isEphemeral) {
                if (isViewOnce) "📷 Foto única"
                else if (selfDestructDuration > 0) "🕐 Mensaje temporal"
                else content
            } else content
        )
    }

    fun getUserChats(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val userChatsIndexRef = database.child("userChats").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatIds = snapshot.children.mapNotNull { it.key }
                if (chatIds.isEmpty()) {
                    trySend(emptyList())
                    return
                }
                launch {
                    val chats = mutableListOf<Map<String, Any>>()
                    chatIds.forEach { chatId ->
                        try {
                            val chatSnapshot = database.child("chats").child(chatId).get().await()
                            if (chatSnapshot.exists()) {
                                val map = chatSnapshot.value as? Map<String, Any>
                                if (map != null) {
                                    chats.add(map.toMutableMap().apply { put("chatId", chatId) })
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("RealtimeDB", "Failed to fetch chat $chatId: ${e.message}")
                        }
                    }
                    trySend(chats)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        userChatsIndexRef.addValueEventListener(listener)
        awaitClose { userChatsIndexRef.removeEventListener(listener) }
    }

    suspend fun setChatBooleanField(chatId: String, field: String, value: Boolean) {
        database.child("chats").child(chatId).child(field).setValue(value).await()
    }

    suspend fun deleteChat(chatId: String) {
        database.child("chats").child(chatId).removeValue().await()
    }

    fun getUserCallHistory(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val callsRef = database.child("calls").orderByChild("callerId").equalTo(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val calls = snapshot.children.mapNotNull { it.value as? Map<String, Any> }
                trySend(calls)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        callsRef.addValueEventListener(listener)
        awaitClose { callsRef.removeEventListener(listener) }
    }

    fun getAllStories(): Flow<List<Map<String, Any>>> = callbackFlow {
        val storiesRef = database.child("stories")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val stories = snapshot.children.flatMap { userStories ->
                    userStories.children.mapNotNull { story ->
                        val map = story.value as? Map<String, Any> ?: return@mapNotNull null
                        val timestamp = map["timestamp"] as? Long ?: 0L
                        if (now - timestamp < 24 * 60 * 60 * 1000) {
                            map.toMutableMap().apply { put("storyId", story.key ?: "") }
                        } else null
                    }
                }
                trySend(stories)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        storiesRef.addValueEventListener(listener)
        awaitClose { storiesRef.removeEventListener(listener) }
    }

    /**
     * Lee SOLO las stories de un usuario concreto (`/stories/{userId}`) en lugar de
     * todo el árbol `/stories`. Esto evita el "Permission denied" al ver la story de
     * otra persona cuando las reglas de Firebase no permiten leer el nodo raíz de
     * stories, y es además más eficiente (menos datos y menos exigencia de permisos).
     */
    fun getStoriesForUser(userId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        if (userId.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val userStoriesRef = database.child("stories").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val now = System.currentTimeMillis()
                val stories = snapshot.children.mapNotNull { story ->
                    val map = story.value as? Map<String, Any> ?: return@mapNotNull null
                    val timestamp = map["timestamp"] as? Long ?: 0L
                    if (now - timestamp < 24 * 60 * 60 * 1000) {
                        map.toMutableMap().apply {
                            put("storyId", story.key ?: "")
                            // Garantiza que userId esté presente aunque el registro no lo guarde.
                            putIfAbsent("userId", userId)
                        }
                    } else null
                }
                trySend(stories)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        userStoriesRef.addValueEventListener(listener)
        awaitClose { userStoriesRef.removeEventListener(listener) }
    }

    suspend fun addReaction(chatId: String, messageId: String, reaction: String) {
        val userId = auth.currentUser?.uid ?: return
        database.child("chats").child(chatId).child("messages").child(messageId)
            .child("reactions").child(userId).setValue(reaction).await()
    }

    suspend fun createGroup(groupName: String, members: List<String>): String {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("Not logged in")
        val groupId = "group_${System.currentTimeMillis()}"
        val allMembers = (members + currentUserId).distinct()
        val membersMap = allMembers.associateWith { true }
        
        android.util.Log.d("RealtimeDB", "🆕 Creando grupo: $groupId con ${allMembers.size} miembros")
        
        val groupData = mapOf(
            "chatId" to groupId,
            "groupName" to groupName,
            "members" to membersMap,
            "createdAt" to ServerValue.TIMESTAMP,
            "createdBy" to currentUserId,
            "isGroup" to true,
            "type" to "group",
            "lastMessage" to "Grupo creado",
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId,
            "isE2EE" to false // Grupos no usan E2EE por defecto
        )
        
        try {
            // Crear el grupo en /chats
            database.child("chats").child(groupId).setValue(groupData).await()
            android.util.Log.d("RealtimeDB", "✅ Grupo $groupId creado en /chats")
            
            // ══════════════════════════════════════════════════════════════════
            // 🔥 CRÍTICO: ACTUALIZAR ÍNDICE userChats PARA CADA MIEMBRO
            // Sin esto, el grupo NO aparecerá en HomeScreen de ningún miembro
            // ══════════════════════════════════════════════════════════════════
            updateUserChatsIndex(groupId, allMembers)
            android.util.Log.d("RealtimeDB", "✅ Índice userChats actualizado para grupo $groupId")
            
            return groupId
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDB", "❌ Error creando grupo: ${e.message}", e)
            throw e
        }
    }

    suspend fun searchUserByUsername(username: String): Map<String, Any>? {
        val snapshot = database.child("users").orderByChild("username").equalTo(username).get().await()
        return snapshot.children.firstOrNull()?.value as? Map<String, Any>
    }

    suspend fun sendFriendRequest(targetUserId: String): String {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("Not logged in")
        val requestId = database.child("friendRequests").child(targetUserId).push().key ?: ""
        database.child("friendRequests").child(targetUserId).child(requestId).setValue(
            mapOf("from" to currentUserId, "timestamp" to ServerValue.TIMESTAMP, "status" to "pending")
        ).await()
        return requestId
    }

    suspend fun updateProfilePhoto(userId: String, photoUrl: String) {
        database.child("users").child(userId).child("profilePhotoUrl").setValue(photoUrl).await()
    }

    suspend fun createStory(mediaUrl: String, mediaType: String, isVideo: Boolean, caption: String? = null): String {
        val userId = auth.currentUser?.uid ?: throw Exception("Not logged in")
        val storyId = database.child("stories").child(userId).push().key ?: ""
        
        // Usar el mediaType que se pasa como parámetro directamente
        // Esto permite TEXT, IMAGE, VIDEO correctamente
        val storyData = mutableMapOf<String, Any>(
            "storyId" to storyId,
            "userId" to userId,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType.uppercase(), // Asegurar que esté en mayúsculas
            "type" to mediaType.uppercase(),
            "timestamp" to ServerValue.TIMESTAMP
        )
        if (!caption.isNullOrBlank()) {
            storyData["caption"] = caption
        }
        database.child("stories").child(userId).child(storyId).setValue(storyData).await()
        return storyId
    }

    suspend fun sendEphemeralMediaMessage(
        chatId: String,
        mediaUrl: String,
        mediaType: String,
        caption: String = "",
        isViewOnce: Boolean = false,
        selfDestructDuration: Long = 0
    ) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        if (!chatSnapshot.exists()) {
            val memberIds = chatId.split("_").filter { it.isNotBlank() }
            val membersMap = memberIds.associateWith { true }
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to membersMap,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            chatRef.setValue(chatData).await()
            updateUserChatsIndex(chatId, memberIds)
        }

        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        val destructAt = if (isViewOnce) 0L else if (selfDestructDuration > 0) System.currentTimeMillis() + (selfDestructDuration * 1000) else 0L

        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to caption,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>(),
            "isEphemeral" to true,
            "isViewOnce" to isViewOnce,
            "selfDestructDuration" to selfDestructDuration,
            "selfDestructAt" to destructAt,
            "viewedBy" to emptyList<String>()
        )

        chatRef.child("messages").child(messageId).setValue(messageData).await()

        val lastMessageText = when {
            isViewOnce -> "📷 Foto única"
            selfDestructDuration > 0 -> "🕐 Multimedia temporal"
            else -> when (mediaType) {
                "IMAGE" -> "📷 Foto"
                "VIDEO" -> "🎥 Video"
                "AUDIO" -> "🎤 Audio"
                else -> if (caption.isNotEmpty()) caption else "[$mediaType]"
            }
        }
        
        val lastMessageData = mapOf(
            "lastMessage" to lastMessageText,
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )
        chatRef.updateChildren(lastMessageData).await()

        sendFcmForMessage(
            chatId = chatId,
            senderId = currentUserId,
            content = if (isViewOnce) "📷 Foto única" 
                      else if (selfDestructDuration > 0) "🕐 Multimedia temporal"
                      else caption.ifEmpty { when (mediaType) {
                          "IMAGE" -> "📷 Foto"
                          "VIDEO" -> "🎥 Video"
                          "AUDIO" -> "🎤 Audio"
                          else -> mediaType
                      } },
            mediaType = mediaType
        )
    }

    suspend fun markEphemeralMessageViewed(chatId: String, messageId: String) {
        val userId = auth.currentUser?.uid ?: return
        val msgRef = database.child("chats").child(chatId).child("messages").child(messageId)
        val snapshot = msgRef.get().await()
        val isViewOnce = snapshot.child("isViewOnce").getValue(Boolean::class.java) ?: false
        
        if (isViewOnce) {
            msgRef.removeValue().await()
        } else {
            msgRef.child("viewedBy").push().setValue(userId).await()
        }
    }

    suspend fun cleanupExpiredEphemeralMessages() {
        val now = System.currentTimeMillis()
        val chatsSnapshot = database.child("chats").get().await()
        chatsSnapshot.children.forEach { chat ->
            val messagesSnapshot = chat.child("messages")
            messagesSnapshot.children.forEach { msg ->
                val isEphemeral = msg.child("isEphemeral").getValue(Boolean::class.java) ?: false
                val destructAt = msg.child("selfDestructAt").getValue(Long::class.java) ?: 0L
                if (isEphemeral && destructAt > 0 && now > destructAt) {
                    msg.ref.removeValue()
                }
            }
        }
    }

    suspend fun sendMediaMessage(
        chatId: String,
        mediaUrl: String,
        mediaType: String,
        caption: String = ""
    ) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")

        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        
        if (!chatSnapshot.exists()) {
            val memberIds = chatId.split("_").filter { it.isNotBlank() }
            val membersMap = memberIds.associateWith { true }
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to membersMap,
                "createdAt" to ServerValue.TIMESTAMP,
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            chatRef.setValue(chatData).await()
            updateUserChatsIndex(chatId, memberIds)
        }

        val messageId = chatRef.child("messages").push().key
            ?: throw Exception("Failed to generate message ID")

        val messageData = hashMapOf(
            "messageId" to messageId,
            "senderId" to currentUserId,
            "content" to caption,
            "mediaUrl" to mediaUrl,
            "mediaType" to mediaType,
            "timestamp" to ServerValue.TIMESTAMP,
            "status" to "sent",
            "reactions" to emptyMap<String, String>()
        )

        chatRef.child("messages").child(messageId).setValue(messageData).await()

        val lastMessageText = when (mediaType) {
            "IMAGE" -> "📷 Foto"
            "VIDEO" -> "🎥 Video"
            "AUDIO" -> "🎤 Audio"
            else -> if (caption.isNotEmpty()) caption else "[$mediaType]"
        }
        
        val lastMessageData = mapOf(
            "lastMessage" to lastMessageText,
            "lastMessageTime" to ServerValue.TIMESTAMP,
            "lastMessageSenderId" to currentUserId
        )
        chatRef.updateChildren(lastMessageData).await()

        sendFcmForMessage(
            chatId = chatId,
            senderId = currentUserId,
            content = caption.ifEmpty { when (mediaType) {
                "IMAGE" -> "📷 Foto"
                "VIDEO" -> "🎥 Video"
                "AUDIO" -> "🎤 Audio"
                "DOCUMENT" -> "📄 Documento"
                "LOCATION" -> "📍 Ubicación"
                "STICKER" -> "Sticker"
                else -> mediaType
            } },
            mediaType = mediaType
        )
    }

    suspend fun getUserById(userId: String): Map<String, Any>? {
        return database.child("users").child(userId).get().await().value as? Map<String, Any>
    }

    fun getUserProfile(userId: String): Flow<Result<Map<String, Any>>> = callbackFlow {
        val userRef = database.child("users").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val data = snapshot.value as? Map<String, Any>
                if (data != null) trySend(Result.success(data))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        userRef.addValueEventListener(listener)
        awaitClose { userRef.removeEventListener(listener) }
    }

    fun getAllUsers(): Flow<Result<List<Map<String, Any>>>> = callbackFlow {
        val usersRef = database.child("users")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val users = snapshot.children.mapNotNull { it.value as? Map<String, Any> }
                trySend(Result.success(users))
            }
            override fun onCancelled(error: DatabaseError) {
                trySend(Result.failure(error.toException()))
            }
        }
        usersRef.addValueEventListener(listener)
        awaitClose { usersRef.removeEventListener(listener) }
    }

    suspend fun updateUserProfile(userId: String, data: Map<String, Any>): Flow<Result<Unit>> = callbackFlow {
        database.child("users").child(userId).updateChildren(data)
            .addOnSuccessListener { trySend(Result.success(Unit)) }
            .addOnFailureListener { trySend(Result.failure(it)) }
        awaitClose()
    }

    suspend fun createCall(callData: Map<String, Any>): String {
        val callId = database.child("calls").push().key ?: throw Exception("Failed to generate call ID")
        database.child("calls").child(callId).setValue(callData).await()
        return callId
    }

    suspend fun getCallData(callId: String): Map<String, Any>? {
        return database.child("calls").child(callId).get().await().value as? Map<String, Any>
    }

    fun listenToCall(callId: String): Flow<Map<String, Any>?> = callbackFlow {
        val callRef = database.child("calls").child(callId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.value as? Map<String, Any>)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        callRef.addValueEventListener(listener)
        awaitClose { callRef.removeEventListener(listener) }
    }

    suspend fun updateCallStatus(callId: String, status: String) {
        database.child("calls").child(callId).child("status").setValue(status).await()
    }

    suspend fun setCallOffer(callId: String, offer: String) {
        database.child("calls").child(callId).child("offer").setValue(offer).await()
    }

    suspend fun setCallAnswer(callId: String, answer: String) {
        database.child("calls").child(callId).child("answer").setValue(answer).await()
    }

    suspend fun addIceCandidate(callId: String, candidate: Map<String, Any>) {
        database.child("calls").child(callId).child("iceCandidates").push().setValue(candidate).await()
    }

    fun listenToIceCandidates(callId: String): Flow<List<Map<String, Any>>> = callbackFlow {
        val ref = database.child("calls").child(callId).child("iceCandidates")
        val listener = object : ChildEventListener {
            val candidates = mutableListOf<Map<String, Any>>()
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                (snapshot.value as? Map<String, Any>)?.let {
                    candidates.add(it)
                    trySend(candidates.toList())
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addChildEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    suspend fun endCall(callId: String) {
        database.child("calls").child(callId).child("status").setValue("ENDED").await()
        database.child("calls").child(callId).child("endedAt").setValue(ServerValue.TIMESTAMP).await()
    }

    suspend fun uploadFile(uri: Uri, folder: String, fileName: String): String {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val storageRef = FirebaseStorage.getInstance().reference
        val fileRef = storageRef.child("$folder/$currentUserId/$fileName")
        fileRef.putFile(uri).await()
        return fileRef.downloadUrl.await().toString()
    }

    suspend fun sendImageMessage(chatId: String, imageUri: Uri, caption: String = "") {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val chatRef = database.child("chats").child(chatId)
        if (!chatRef.get().await().exists()) {
            val memberIds = chatId.split("_").filter { it.isNotBlank() }
            val membersMap = memberIds.associateWith { true }
            chatRef.setValue(mapOf("chatId" to chatId, "members" to membersMap, "createdAt" to ServerValue.TIMESTAMP)).await()
            updateUserChatsIndex(chatId, memberIds)
        }
        val fileName = "IMG_${System.currentTimeMillis()}.jpg"
        val imageUrl = uploadFile(imageUri, "images", fileName)
        val messageId = chatRef.child("messages").push().key ?: throw Exception("ID error")
        val messageData = hashMapOf(
            "messageId" to messageId, "senderId" to currentUserId, "content" to caption,
            "mediaType" to "IMAGE", "mediaUrl" to imageUrl, "timestamp" to ServerValue.TIMESTAMP, "status" to "sent"
        )
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        chatRef.updateChildren(mapOf("lastMessage" to (if (caption.isNotEmpty()) caption else "📷 Foto"), "lastMessageTime" to ServerValue.TIMESTAMP, "lastMessageSenderId" to currentUserId)).await()
        sendFcmForMessage(chatId, currentUserId, if (caption.isNotEmpty()) caption else "📷 Foto", "IMAGE")
    }

    suspend fun sendVideoMessage(chatId: String, videoUri: Uri, caption: String = "", duration: Long = 0) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val chatRef = database.child("chats").child(chatId)
        if (!chatRef.get().await().exists()) {
            val memberIds = chatId.split("_").filter { it.isNotBlank() }
            val membersMap = memberIds.associateWith { true }
            chatRef.setValue(mapOf("chatId" to chatId, "members" to membersMap, "createdAt" to ServerValue.TIMESTAMP)).await()
            updateUserChatsIndex(chatId, memberIds)
        }
        val fileName = "VID_${System.currentTimeMillis()}.mp4"
        val videoUrl = uploadFile(videoUri, "videos", fileName)
        val messageId = chatRef.child("messages").push().key ?: throw Exception("ID error")
        val messageData = hashMapOf(
            "messageId" to messageId, "senderId" to currentUserId, "content" to caption,
            "mediaType" to "VIDEO", "mediaUrl" to videoUrl, "duration" to duration, "timestamp" to ServerValue.TIMESTAMP, "status" to "sent"
        )
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        chatRef.updateChildren(mapOf("lastMessage" to (if (caption.isNotEmpty()) caption else "🎥 Video"), "lastMessageTime" to ServerValue.TIMESTAMP, "lastMessageSenderId" to currentUserId)).await()
        sendFcmForMessage(chatId, currentUserId, if (caption.isNotEmpty()) caption else "🎥 Video", "VIDEO")
    }

    suspend fun sendDocumentMessage(chatId: String, documentUri: Uri, fileName: String, fileSize: Long, mimeType: String) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val chatRef = database.child("chats").child(chatId)
        if (!chatRef.get().await().exists()) {
            val memberIds = chatId.split("_").filter { it.isNotBlank() }
            val membersMap = memberIds.associateWith { true }
            chatRef.setValue(mapOf("chatId" to chatId, "members" to membersMap, "createdAt" to ServerValue.TIMESTAMP)).await()
            updateUserChatsIndex(chatId, memberIds)
        }
        val documentUrl = uploadFile(documentUri, "documents", fileName)
        val messageId = chatRef.child("messages").push().key ?: throw Exception("ID error")
        val messageData = hashMapOf(
            "messageId" to messageId, "senderId" to currentUserId, "content" to fileName,
            "mediaType" to "DOCUMENT", "mediaUrl" to documentUrl, "fileName" to fileName, "fileSize" to fileSize, "mimeType" to mimeType,
            "timestamp" to ServerValue.TIMESTAMP, "status" to "sent"
        )
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        chatRef.updateChildren(mapOf("lastMessage" to "📄 $fileName", "lastMessageTime" to ServerValue.TIMESTAMP, "lastMessageSenderId" to currentUserId)).await()
        sendFcmForMessage(chatId, currentUserId, "📄 $fileName", "DOCUMENT")
    }

    suspend fun sendAudioMessage(chatId: String, audioUri: Uri, duration: Long) {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val chatRef = database.child("chats").child(chatId)
        if (!chatRef.get().await().exists()) {
            val memberIds = chatId.split("_").filter { it.isNotBlank() }
            val membersMap = memberIds.associateWith { true }
            chatRef.setValue(mapOf("chatId" to chatId, "members" to membersMap, "createdAt" to ServerValue.TIMESTAMP)).await()
            updateUserChatsIndex(chatId, memberIds)
        }
        val fileName = "AUD_${System.currentTimeMillis()}.m4a"
        val audioUrl = uploadFile(audioUri, "audio", fileName)
        val messageId = chatRef.child("messages").push().key ?: throw Exception("ID error")
        val messageData = hashMapOf(
            "messageId" to messageId, "senderId" to currentUserId, "content" to "",
            "mediaType" to "AUDIO", "mediaUrl" to audioUrl, "duration" to duration, "timestamp" to ServerValue.TIMESTAMP, "status" to "sent"
        )
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        chatRef.updateChildren(mapOf("lastMessage" to "🎤 Audio", "lastMessageTime" to ServerValue.TIMESTAMP, "lastMessageSenderId" to currentUserId)).await()
        sendFcmForMessage(chatId, currentUserId, "🎤 Audio", "AUDIO")
    }

    suspend fun sendLocationMessage(chatId: String, latitude: Double, longitude: Double, address: String = "") {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val chatRef = database.child("chats").child(chatId)
        if (!chatRef.get().await().exists()) {
            val memberIds = chatId.split("_").filter { it.isNotBlank() }
            val membersMap = memberIds.associateWith { true }
            chatRef.setValue(mapOf("chatId" to chatId, "members" to membersMap, "createdAt" to ServerValue.TIMESTAMP)).await()
            updateUserChatsIndex(chatId, memberIds)
        }
        val messageId = chatRef.child("messages").push().key ?: throw Exception("ID error")
        val messageData = hashMapOf(
            "messageId" to messageId, "senderId" to currentUserId, "content" to address,
            "mediaType" to "LOCATION", "latitude" to latitude, "longitude" to longitude, "timestamp" to ServerValue.TIMESTAMP, "status" to "sent"
        )
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        chatRef.updateChildren(mapOf("lastMessage" to "📍 Ubicación", "lastMessageTime" to ServerValue.TIMESTAMP, "lastMessageSenderId" to currentUserId)).await()
        sendFcmForMessage(chatId, currentUserId, "📍 Ubicación", "LOCATION")
    }

    suspend fun sendStickerMessage(chatId: String, stickerUrl: String, stickerPack: String = "") {
        val currentUserId = auth.currentUser?.uid ?: throw Exception("User not authenticated")
        val chatRef = database.child("chats").child(chatId)
        val chatSnapshot = chatRef.get().await()
        if (!chatSnapshot.exists()) {
            val memberIds = chatId.split("_").filter { it.isNotBlank() }
            val membersMap = memberIds.associateWith { true }
            chatRef.setValue(mapOf("chatId" to chatId, "members" to membersMap, "createdAt" to ServerValue.TIMESTAMP)).await()
            updateUserChatsIndex(chatId, memberIds)
        }
        val messageId = chatRef.child("messages").push().key ?: throw Exception("ID error")
        val messageData = hashMapOf(
            "messageId" to messageId, "senderId" to currentUserId, "content" to stickerUrl,
            "mediaType" to "STICKER", "stickerUrl" to stickerUrl, "stickerPack" to stickerPack, "timestamp" to ServerValue.TIMESTAMP, "status" to "sent"
        )
        chatRef.child("messages").child(messageId).setValue(messageData).await()
        chatRef.updateChildren(mapOf("lastMessage" to "Sticker", "lastMessageTime" to ServerValue.TIMESTAMP, "lastMessageSenderId" to currentUserId)).await()
        sendFcmForMessage(chatId, currentUserId, "Sticker", "STICKER")
    }

    suspend fun markStoryAsViewed(storyId: String) {
        val userId = auth.currentUser?.uid ?: return
        database.child("stories_views").child(storyId).child(userId).setValue(ServerValue.TIMESTAMP).await()
    }

    suspend fun sendStoryReply(storyId: String, recipientId: String, content: String) {
        val chatId = if (auth.currentUser!!.uid < recipientId) "${auth.currentUser!!.uid}_$recipientId" else "${recipientId}_${auth.currentUser!!.uid}"
        sendMessage(chatId, "Reaccionó a tu historia: $content")
    }

    suspend fun getStoryViewers(storyId: String): List<Map<String, Any>> {
        val snapshot = database.child("stories_views").child(storyId).get().await()
        return snapshot.children.mapNotNull { it.value as? Map<String, Any> }
    }

    suspend fun getChatMediaGallery(chatId: String): List<Map<String, Any>> {
        val snapshot = database.child("chats").child(chatId).child("messages").get().await()
        return snapshot.children.mapNotNull {
            val map = it.value as? Map<String, Any>
            if (map?.containsKey("mediaUrl") == true) map else null
        }
    }

    suspend fun updatePresence(isOnline: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val userPresenceRef = database.child("users").child(userId)
        val presenceMap = mapOf("online" to isOnline, "lastSeen" to ServerValue.TIMESTAMP)
        userPresenceRef.updateChildren(presenceMap).await()
        if (isOnline) userPresenceRef.child("online").onDisconnect().setValue(false)
        userPresenceRef.child("lastSeen").onDisconnect().setValue(ServerValue.TIMESTAMP)
    }

    fun observeUserPresence(userId: String): Flow<Pair<Boolean, Long>> = callbackFlow {
        val ref = database.child("users").child(userId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val online = snapshot.child("online").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("lastSeen").getValue(Long::class.java) ?: 0L
                trySend(Pair(online, lastSeen))
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    private suspend fun sendFcmForMessage(
        chatId: String,
        senderId: String,
        content: String,
        mediaType: String? = null,
        senderPhotoUrl: String? = null
    ) {
        if (!BuildConfig.FCM_ENABLED || BuildConfig.FCM_SERVER_KEY.isBlank()) {
            Log.d("RealtimeDB", "FCM disabled")
            return
        }

        try {
            val chatSnapshot = database.child("chats").child(chatId).child("members").get().await()
            val members = when (val value = chatSnapshot.value) {
                is Map<*, *> -> value.keys.filterIsInstance<String>()
                is List<*> -> value.filterIsInstance<String>() // Fallback retrocompatibilidad
                else -> emptyList()
            }
            val recipientId = members.firstOrNull { it != senderId } ?: return

            val userSnapshot = database.child("users").child(senderId).child("displayName").get().await()
            val senderName = userSnapshot.getValue(String::class.java) ?: "Usuario"

            val tokensSnapshot = database.child("users").child(recipientId).child("fcmTokens").get().await()
            val fcmTokens = tokensSnapshot.children.mapNotNull { it.getValue(String::class.java) }
            if (fcmTokens.isEmpty()) return

            val displayText = when (mediaType) {
                "IMAGE" -> "📷 Foto"
                "VIDEO" -> "🎥 Video"
                "AUDIO" -> "🎤 Mensaje de voz"
                "DOCUMENT" -> "📄 Documento"
                "LOCATION" -> "📍 Ubicación"
                "STICKER" -> "Sticker"
                else -> content
            }

            val dataPayload = com.Azelmods.App.service.FcmNotificationSender.buildMessagePayload(
                chatId = chatId, senderId = senderId, senderName = senderName,
                senderPhotoUrl = senderPhotoUrl, mediaType = mediaType, body = displayText
            )

            com.Azelmods.App.service.FcmNotificationSender.sendToMultipleTokens(
                fcmTokens, BuildConfig.FCM_SERVER_KEY, senderName, displayText, dataPayload
            )
        } catch (e: Exception) {
            Log.e("RealtimeDB", "FCM error: ${e.message}")
        }
    }

    fun observeTyping(chatId: String): Flow<Map<String, Boolean>> = callbackFlow {
        val typingRef = database.child("chats").child(chatId).child("typing")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val typingMap = snapshot.children.associate { it.key!! to (it.value as? Boolean ?: false) }
                trySend(typingMap)
            }
            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        typingRef.addValueEventListener(listener)
        awaitClose { typingRef.removeEventListener(listener) }
    }

    suspend fun setTypingStatus(chatId: String, isTyping: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        val typingRef = database.child("chats").child(chatId).child("typing").child(userId)
        if (isTyping) {
            typingRef.setValue(true).await()
            typingRef.onDisconnect().removeValue()
        } else {
            typingRef.removeValue().await()
        }
    }

    /**
     * 🔥 CRÍTICO: Helper function to maintain userChats index.
     * 
     * Escribe userChats/{uid}/{chatId} = true para cada miembro.
     * Esto permite listado eficiente de chats sin leer todo /chats.
     * 
     * **SIN ESTA FUNCIÓN, LOS CHATS NO APARECEN EN HOMESCREEN**
     * 
     * Estructura en Firebase:
     * /userChats
     *   /{userId1}
     *     /{chatId}: true
     *   /{userId2}
     *     /{chatId}: true
     * 
     * @param chatId El ID del chat (puede ser "uid1_uid2" o "group_timestamp")
     * @param memberIds Lista de UIDs de todos los miembros del chat
     */
    private suspend fun updateUserChatsIndex(chatId: String, memberIds: List<String>) {
        if (chatId.isBlank()) {
            android.util.Log.e("RealtimeDB", "❌ updateUserChatsIndex: chatId vacío")
            return
        }
        
        if (memberIds.isEmpty()) {
            android.util.Log.e("RealtimeDB", "❌ updateUserChatsIndex: memberIds vacío para chat $chatId")
            return
        }
        
        android.util.Log.d("RealtimeDB", "📝 Actualizando índice userChats para chat $chatId con ${memberIds.size} miembros: $memberIds")
        
        try {
            val updates = mutableMapOf<String, Any>()
            
            // Para cada miembro, crear entrada en userChats/{uid}/{chatId} = true
            memberIds.forEach { uid ->
                if (uid.isNotBlank()) {
                    updates["userChats/$uid/$chatId"] = true
                    android.util.Log.d("RealtimeDB", "  ✓ Agregando userChats/$uid/$chatId")
                }
            }
            
            if (updates.isEmpty()) {
                android.util.Log.e("RealtimeDB", "❌ No hay updates para aplicar en userChats")
                return
            }
            
            // Hacer update atómico de todos los índices a la vez
            database.updateChildren(updates).await()
            
            android.util.Log.d("RealtimeDB", "✅ Índice userChats actualizado exitosamente: ${updates.size} entradas escritas")
            
        } catch (e: Exception) {
            android.util.Log.e("RealtimeDB", "❌ Error fatal actualizando userChats index para $chatId: ${e.message}", e)
            // Re-lanzar la excepción para que el caller la maneje
            throw Exception("Failed to update userChats index: ${e.message}", e)
        }
    }
}
