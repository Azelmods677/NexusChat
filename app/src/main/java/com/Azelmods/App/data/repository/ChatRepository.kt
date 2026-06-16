package com.Azelmods.App.data.repository

import com.Azelmods.App.data.firebase.FirebaseManager
import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.util.Resource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRepository {
    fun getChats(userId: String): Flow<Resource<List<Chat>>>
    fun getMessages(chatId: String, currentUserId: String): Flow<Resource<List<Message>>>
    suspend fun sendMessage(chatId: String, message: Message): Resource<Unit>
    suspend fun updateMessageStatus(chatId: String, messageId: String, status: MessageStatus): Resource<Unit>
    suspend fun addReaction(chatId: String, messageId: String, userId: String, emoji: String): Resource<Unit>
    suspend fun createChat(participants: List<String>): Resource<String>
    suspend fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean): Resource<Unit>
    fun observeTypingStatus(chatId: String, userId: String): Flow<Boolean>
    suspend fun deleteMessage(chatId: String, messageId: String, userId: String, deleteForEveryone: Boolean): Resource<Unit>
    suspend fun editMessage(chatId: String, messageId: String, newContent: String, userId: String): Resource<Unit>
}

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val firebaseManager: FirebaseManager
) : ChatRepository {
    
    override fun getChats(userId: String): Flow<Resource<List<Chat>>> = callbackFlow {
        trySend(Resource.Loading())
        
        val chatsRef = firebaseManager.database.getReference("chats")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chats = mutableListOf<Chat>()
                snapshot.children.forEach { chatSnapshot ->
                    val chat = chatSnapshot.getValue(Chat::class.java)
                    if (chat != null && chat.participants.contains(userId)) {
                        chats.add(chat)
                    }
                }
                // Sort by last message time
                chats.sortByDescending { it.lastMessageTime }
                trySend(Resource.Success(chats))
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Resource.Error(error.message))
            }
        }
        
        chatsRef.addValueEventListener(listener)
        
        awaitClose { chatsRef.removeEventListener(listener) }
    }
    
    override fun getMessages(chatId: String, currentUserId: String): Flow<Resource<List<Message>>> = callbackFlow {
        trySend(Resource.Loading())
        
        val messagesRef = firebaseManager.database.getReference("chats/$chatId/messages")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                snapshot.children.forEach { messageSnapshot ->
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        // Filter out messages deleted for this user
                        val isDeletedForUser = message.deletedFor[currentUserId] == true
                        if (!isDeletedForUser) {
                            messages.add(message)
                        }
                    }
                }
                // Sort by timestamp
                messages.sortBy { it.timestamp }
                trySend(Resource.Success(messages))
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(Resource.Error(error.message))
            }
        }
        
        messagesRef.addValueEventListener(listener)
        
        awaitClose { messagesRef.removeEventListener(listener) }
    }
    
    override suspend fun sendMessage(chatId: String, message: Message): Resource<Unit> {
        return try {
            val messageRef = firebaseManager.database.getReference("chats/$chatId/messages").push()
            val messageWithId = message.copy(messageId = messageRef.key ?: "")
            
            messageRef.setValue(messageWithId).await()
            
            // Update chat's last message
            val chatRef = firebaseManager.database.getReference("chats/$chatId")
            chatRef.child("lastMessage").setValue(message.content).await()
            chatRef.child("lastMessageTime").setValue(message.timestamp).await()
            chatRef.child("lastMessageSenderId").setValue(message.senderId).await()
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to send message")
        }
    }
    
    override suspend fun updateMessageStatus(
        chatId: String,
        messageId: String,
        status: MessageStatus
    ): Resource<Unit> {
        return try {
            firebaseManager.database
                .getReference("chats/$chatId/messages/$messageId/status")
                .setValue(status.name)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to update message status")
        }
    }
    
    override suspend fun addReaction(
        chatId: String,
        messageId: String,
        userId: String,
        emoji: String
    ): Resource<Unit> {
        return try {
            firebaseManager.database
                .getReference("chats/$chatId/messages/$messageId/reactions/$userId")
                .setValue(emoji)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to add reaction")
        }
    }
    
    override suspend fun createChat(participants: List<String>): Resource<String> {
        return try {
            val chatRef = firebaseManager.database.getReference("chats").push()
            val chatId = chatRef.key ?: return Resource.Error("Failed to create chat")
            
            val membersMap = participants.associateWith { true }
            val chatData = mapOf(
                "chatId" to chatId,
                "members" to membersMap,
                "createdAt" to System.currentTimeMillis(),
                "lastMessage" to "",
                "lastMessageTime" to ServerValue.TIMESTAMP,
                "lastMessageSenderId" to ""
            )
            
            chatRef.setValue(chatData).await()
            
            // Update userChats index for all participants
            participants.forEach { uid ->
                firebaseManager.database.getReference("userChats")
                    .child(uid).child(chatId).setValue(true).await()
            }
            
            Resource.Success(chatId)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to create chat")
        }
    }
    
    override suspend fun setTypingStatus(
        chatId: String,
        userId: String,
        isTyping: Boolean
    ): Resource<Unit> {
        return try {
            firebaseManager.database
                .getReference("typing/$chatId/$userId")
                .setValue(isTyping)
                .await()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to set typing status")
        }
    }
    
    override fun observeTypingStatus(chatId: String, userId: String): Flow<Boolean> = callbackFlow {
        val typingRef = firebaseManager.database.getReference("typing/$chatId/$userId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isTyping = snapshot.getValue(Boolean::class.java) ?: false
                trySend(isTyping)
            }
            
            override fun onCancelled(error: DatabaseError) {
                trySend(false)
            }
        }
        
        typingRef.addValueEventListener(listener)
        
        awaitClose { typingRef.removeEventListener(listener) }
    }
    
    override suspend fun deleteMessage(
        chatId: String,
        messageId: String,
        userId: String,
        deleteForEveryone: Boolean
    ): Resource<Unit> {
        return try {
            if (deleteForEveryone) {
                // Check if user is sender
                val messageSnapshot = firebaseManager.database
                    .getReference("chats/$chatId/messages/$messageId")
                    .get()
                    .await()
                
                val message = messageSnapshot.getValue(Message::class.java)
                    ?: return Resource.Error("Message not found")
                
                if (message.senderId != userId) {
                    return Resource.Error("Only sender can delete for everyone")
                }
                
                // Check 48-hour time window
                val currentTime = System.currentTimeMillis()
                val timeDiff = currentTime - message.timestamp
                val fortyEightHours = 48 * 60 * 60 * 1000L
                
                if (timeDiff > fortyEightHours) {
                    return Resource.Error("Delete for everyone expired (48 hours)")
                }
                
                // Mark as deleted for everyone and clear content
                val updates = mapOf(
                    "deletedForEveryone" to true,
                    "content" to "",
                    "mediaUrl" to null,
                    "mediaType" to null
                )
                firebaseManager.database
                    .getReference("chats/$chatId/messages/$messageId")
                    .updateChildren(updates)
                    .await()
                
                // Update chat's lastMessage if this was the last message
                val chatSnapshot = firebaseManager.database
                    .getReference("chats/$chatId")
                    .get()
                    .await()
                
                val chat = chatSnapshot.getValue(Chat::class.java)
                if (chat != null && chat.lastMessageTime == message.timestamp) {
                    // Get previous message
                    val messagesSnapshot = firebaseManager.database
                        .getReference("chats/$chatId/messages")
                        .orderByChild("timestamp")
                        .limitToLast(2)
                        .get()
                        .await()
                    
                    val messages = mutableListOf<Message>()
                    messagesSnapshot.children.forEach { msgSnapshot ->
                        val msg = msgSnapshot.getValue(Message::class.java)
                        if (msg != null && msg.messageId != messageId) {
                            messages.add(msg)
                        }
                    }
                    
                    if (messages.isNotEmpty()) {
                        val previousMessage = messages.last()
                        val lastMessageText = if (previousMessage.deletedForEveryone) {
                            "Este mensaje fue eliminado"
                        } else {
                            previousMessage.content
                        }
                        firebaseManager.database
                            .getReference("chats/$chatId/lastMessage")
                            .setValue(lastMessageText)
                            .await()
                        firebaseManager.database
                            .getReference("chats/$chatId/lastMessageTime")
                            .setValue(previousMessage.timestamp)
                            .await()
                    } else {
                        firebaseManager.database
                            .getReference("chats/$chatId/lastMessage")
                            .setValue("")
                            .await()
                    }
                }
            } else {
                // Mark as deleted only for this user
                firebaseManager.database
                    .getReference("chats/$chatId/messages/$messageId/deletedFor/$userId")
                    .setValue(true)
                    .await()
            }
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to delete message")
        }
    }
    
    override suspend fun editMessage(
        chatId: String,
        messageId: String,
        newContent: String,
        userId: String
    ): Resource<Unit> {
        return try {
            // Get message to validate
            val messageSnapshot = firebaseManager.database
                .getReference("chats/$chatId/messages/$messageId")
                .get()
                .await()
            
            val message = messageSnapshot.getValue(Message::class.java)
                ?: return Resource.Error("Message not found")
            
            // Validate user is sender
            if (message.senderId != userId) {
                return Resource.Error("Only sender can edit message")
            }
            
            // Validate message type (only text messages can be edited)
            if (message.mediaType != null) {
                return Resource.Error("Cannot edit media messages")
            }
            
            // Validate edit time window (15 minutes)
            val currentTime = System.currentTimeMillis()
            val timeDiff = currentTime - message.timestamp
            val fifteenMinutes = 15 * 60 * 1000L
            
            if (timeDiff > fifteenMinutes) {
                return Resource.Error("Edit time expired (15 minutes)")
            }
            
            // Update message
            val updates = mapOf(
                "content" to newContent,
                "edited" to true,
                "editedAt" to currentTime
            )
            firebaseManager.database
                .getReference("chats/$chatId/messages/$messageId")
                .updateChildren(updates)
                .await()
            
            // Update chat's lastMessage if this was the last message
            val chatSnapshot = firebaseManager.database
                .getReference("chats/$chatId")
                .get()
                .await()
            
            val chat = chatSnapshot.getValue(Chat::class.java)
            if (chat != null && chat.lastMessageTime == message.timestamp) {
                firebaseManager.database
                    .getReference("chats/$chatId/lastMessage")
                    .setValue(newContent)
                    .await()
            }
            
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Failed to edit message")
        }
    }
}
