package com.Azelmods.App.data.repository

import com.Azelmods.App.data.firebase.FirebaseManager
import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.util.Resource
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

interface ChatRepository {
    fun getChats(userId: String): Flow<Resource<List<Chat>>>
    fun getMessages(chatId: String): Flow<Resource<List<Message>>>
    suspend fun sendMessage(chatId: String, message: Message): Resource<Unit>
    suspend fun updateMessageStatus(chatId: String, messageId: String, status: MessageStatus): Resource<Unit>
    suspend fun addReaction(chatId: String, messageId: String, userId: String, emoji: String): Resource<Unit>
    suspend fun createChat(participants: List<String>): Resource<String>
    suspend fun setTypingStatus(chatId: String, userId: String, isTyping: Boolean): Resource<Unit>
    fun observeTypingStatus(chatId: String, userId: String): Flow<Boolean>
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
    
    override fun getMessages(chatId: String): Flow<Resource<List<Message>>> = callbackFlow {
        trySend(Resource.Loading())
        
        val messagesRef = firebaseManager.database.getReference("messages/$chatId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = mutableListOf<Message>()
                snapshot.children.forEach { messageSnapshot ->
                    val message = messageSnapshot.getValue(Message::class.java)
                    if (message != null) {
                        messages.add(message)
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
            val messageRef = firebaseManager.database.getReference("messages/$chatId").push()
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
                .getReference("messages/$chatId/$messageId/status")
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
                .getReference("messages/$chatId/$messageId/reactions/$userId")
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
            
            val chat = Chat(
                chatId = chatId,
                participants = participants,
                createdAt = System.currentTimeMillis()
            )
            
            chatRef.setValue(chat).await()
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
}
