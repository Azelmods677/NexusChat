package com.Azelmods.App.ui.screens.chat

import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.local.CacheManager
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.data.model.User
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.data.repository.StorageRepository
import com.Azelmods.App.data.security.encryption.MessageType
import com.Azelmods.App.domain.usecase.DecryptMessageUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ChatState(
    val messages: List<Message> = emptyList(),
    val contact: User? = null,
    val isTyping: Boolean = false,
    val typingUserName: String? = null,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,         // Loading older messages (pagination)
    val hasMoreMessages: Boolean = true,        // Whether there are more messages to load
    val isUploading: Boolean = false,
    val error: String? = null,
    val replyingTo: Message? = null,
    val editingMessage: Message? = null,
    // ── Pagination tracking ──
    val earliestMessageTimestamp: Long = 0L,    // Timestamp of the earliest loaded message
    val earliestMessageId: String = "",          // ID of the earliest loaded message
    // ── Ephemeral / Self-Destructing Messages ──
    val isEphemeralMode: Boolean = false,          // Toggle for ephemeral sending mode
    val ephemeralDuration: Long = 0L,              // Selected duration in seconds (0 = view once)
    val showEphemeralPicker: Boolean = false       // Show duration picker dropdown
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val databaseRepository: RealtimeDatabaseRepository,
    private val backgroundRepository: com.Azelmods.App.data.repository.ChatBackgroundRepository,
    private val decryptMessageUseCase: DecryptMessageUseCase,
    private val cacheManager: CacheManager
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    private val _chatBackground = MutableStateFlow(com.Azelmods.App.data.model.BackgroundConfig())
    val chatBackground: StateFlow<com.Azelmods.App.data.model.BackgroundConfig> = _chatBackground.asStateFlow()

    /**
     * Holds the full chatId once loadChat is called.
     * Used by addReaction and other methods that don't take chatId as a parameter.
     */
    private var currentChatId: String = ""

    private var lastTypingStatus: Boolean = false
    private var typingDebounceJob: Job? = null
    private var typingObserverJob: Job? = null
    
    private var ephemeralCleanupJob: Job? = null
    private var messagesCollectionJob: Job? = null
    
    /**
     * Load chat background configuration
     */
    /**
     * Toggle ephemeral mode on/off
     */
    fun toggleEphemeralMode() {
        _state.value = _state.value.copy(
            isEphemeralMode = !_state.value.isEphemeralMode,
            showEphemeralPicker = false
        )
    }

    /**
     * Set ephemeral duration and enable ephemeral mode
     */
    fun setEphemeralDuration(durationSeconds: Long) {
        _state.value = _state.value.copy(
            isEphemeralMode = true,
            ephemeralDuration = durationSeconds,
            showEphemeralPicker = false
        )
    }

    /**
     * Toggle the ephemeral duration picker
     */
    fun toggleEphemeralPicker() {
        _state.value = _state.value.copy(showEphemeralPicker = !_state.value.showEphemeralPicker)
    }

    /**
     * Dismiss the ephemeral duration picker
     */
    fun dismissEphemeralPicker() {
        _state.value = _state.value.copy(showEphemeralPicker = false)
    }

    /**
     * Start periodic cleanup of expired ephemeral messages
     */
    private fun startEphemeralCleanup(chatId: String) {
        ephemeralCleanupJob?.cancel()
        ephemeralCleanupJob = viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(30_000) // Check every 30 seconds
                try {
                    databaseRepository.cleanupExpiredEphemeralMessages()
                } catch (e: Exception) {
                    // Silently ignore cleanup errors
                }
            }
        }
    }

    /**
     * Mark a message as viewed (for ephemeral/view-once tracking)
     */
    fun markMessageViewed(message: Message) {
        if (!message.isEphemeral || currentChatId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                databaseRepository.markEphemeralMessageViewed(currentChatId, message.messageId)
            } catch (e: Exception) {
                // Silently ignore
            }
        }
    }

    /**
     * Send ephemeral media message (view once photo/video)
     */
    fun sendEphemeralMediaMessage(mediaUrl: String, mediaType: String, chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = _state.value
                databaseRepository.sendEphemeralMediaMessage(
                    chatId = chatId,
                    mediaUrl = mediaUrl,
                    mediaType = mediaType,
                    caption = "",
                    isViewOnce = state.ephemeralDuration == 0L,
                    selfDestructDuration = state.ephemeralDuration
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Error al enviar: ${e.message}")
            }
        }
    }

    fun loadChatBackground(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                backgroundRepository.loadBackground(chatId)
                backgroundRepository.getBackground(chatId).collect { config ->
                    _chatBackground.value = config
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Load a chat session.
     * [chatId] is the FULL chatId — never prepend "chat_".
     * Resolves the other participant by reading chats/{chatId}/members,
     * finding the UID that is NOT the current user, then fetching from users/{uid}.
     */
    fun loadChat(chatId: String) {
        currentChatId = chatId
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoading = true)

            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Usuario no autenticado"
                    )
                    return@launch
                }

                // Resolve the "other" participant from chats/{chatId}/members (list of UIDs)
                val membersSnapshot = FirebaseDatabase.getInstance().reference
                    .child("chats")
                    .child(chatId)
                    .child("members")
                    .get()
                    .await()

                val otherUid = membersSnapshot.children
                    .mapNotNull { it.getValue(String::class.java) }
                    .firstOrNull { it != currentUserId }

                val contact: User? = if (otherUid != null) {
                    databaseRepository.getUserById(otherUid)?.let { data ->
                        User(
                            uid = data["uid"] as? String ?: otherUid,
                            name = data["displayName"] as? String
                                ?: data["name"] as? String
                                ?: "Usuario",
                            username = data["username"] as? String ?: "",
                            email = data["email"] as? String ?: "",
                            photoUrl = data["photoUrl"] as? String,
                            bio = data["bio"] as? String ?: "",
                            isOnline = data["isOnline"] as? Boolean ?: false,
                            lastSeen = data["lastSeen"] as? Long ?: 0L
                        )
                    }
                } else null

                _state.value = _state.value.copy(
                    contact = contact,
                    isLoading = false
                )

                // ── OFFLINE CACHE: show cached messages immediately ──
                val cachedMessages = cacheManager.getCachedMessages(chatId)
                if (cachedMessages.isNotEmpty()) {
                    android.util.Log.d("ChatViewModel", "📦 Loaded ${cachedMessages.size} cached messages for $chatId")
                    _state.value = _state.value.copy(messages = cachedMessages)
                }

                // Launch typing observer in its own coroutine so it doesn't block message collection
                observeTypingStatus(chatId)

                // Start periodic cleanup of expired ephemeral messages
                startEphemeralCleanup(chatId)

                // Paginated message collection in real-time (suspends until cancelled)
                messagesCollectionJob?.cancel()
                messagesCollectionJob = viewModelScope.launch(Dispatchers.IO) {
                    databaseRepository.getChatMessagesPaginated(
                        chatId = chatId,
                        limit = 30
                    ).collect { messagesData ->
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        val messages = messagesData.map { data ->
                            val senderId = data["senderId"] as? String ?: ""
                            var content = data["content"] as? String ?: ""
                            val isEncrypted = data["isEncrypted"] as? Boolean ?: false
                            val payload = data["encryptedPayload"] as? String
                            if (isEncrypted && !payload.isNullOrBlank() && senderId != currentUserId) {
                                val bytes = Base64.decode(payload, Base64.NO_WRAP)
                                when (val dec = decryptMessageUseCase(senderId, bytes, MessageType.WHISPER)) {
                                    is com.Azelmods.App.data.security.encryption.DecryptionResult.Success ->
                                        content = dec.plaintext
                                    else -> content = "🔒 No se pudo descifrar"
                                }
                            }
                            Message(
                                messageId = data["messageId"] as? String ?: "",
                                chatId = chatId,
                                senderId = senderId,
                                senderName = data["senderName"] as? String ?: "",
                                content = content,
                                timestamp = data["timestamp"] as? Long ?: 0L,
                                status = MessageStatus.SENT,
                                replyTo = data["replyTo"] as? String,
                                reactions = (data["reactions"] as? Map<String, String>) ?: emptyMap(),
                                mediaUrl = data["mediaUrl"] as? String,
                                mediaType = data["mediaType"] as? String,
                                deletedFor = (data["deletedFor"] as? Map<String, Boolean>) ?: emptyMap(),
                                deletedForEveryone = data["deletedForEveryone"] as? Boolean ?: false,
                                edited = data["edited"] as? Boolean ?: false,
                                editedAt = data["editedAt"] as? Long ?: 0L,
                                forwardedFrom = data["forwardedFrom"] as? String,
                                isEncrypted = isEncrypted,
                                encryptedPayload = payload
                            )
                        }.filter { message ->
                            message.deletedFor[currentUserId] != true
                        }

                        // ── SAVE TO ROOM CACHE ──
                        try {
                            cacheManager.cacheMessages(messages)
                        } catch (e: Exception) {
                            android.util.Log.e("ChatViewModel", "Failed to cache messages", e)
                        }

                        // Update earliest message timestamp for pagination
                        val earliestTimestamp = messages.minOfOrNull { it.timestamp } ?: 0L
                        val earliestId = messages.minByOrNull { it.timestamp }?.messageId ?: ""

                        _state.value = _state.value.copy(
                            messages = messages,
                            earliestMessageTimestamp = earliestTimestamp,
                            earliestMessageId = earliestId,
                            hasMoreMessages = true
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    /**
     * Write the current user's typing status to typing/{chatId}/{userId}.
     * Debounced: only writes if the value actually changes.
     * If [isTyping] is true, auto-clears after 5 s.
     * Cancels previous debounce job on each call.
     */
    fun setTypingStatus(chatId: String, isTyping: Boolean) {
        if (lastTypingStatus == isTyping) return
        lastTypingStatus = isTyping
        typingDebounceJob?.cancel()
        typingDebounceJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                databaseRepository.setTypingStatus(chatId, isTyping)
                if (isTyping) {
                    // Auto-clear typing after 5 seconds if not explicitly cleared
                    delay(5_000)
                    databaseRepository.setTypingStatus(chatId, false)
                    lastTypingStatus = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Observe typing/{chatId} and update [ChatState.isTyping] and [ChatState.typingUserName]
     * whenever ANY other user is currently typing.
     */
    private fun observeTypingStatus(chatId: String) {
        typingObserverJob?.cancel()
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        typingObserverJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                databaseRepository.observeTyping(chatId).collect { typingMap ->
                    val anyOtherTyping = typingMap.any { (uid, isTyping) ->
                        uid != currentUserId && isTyping
                    }
                    val typingUserName = if (anyOtherTyping) _state.value.contact?.name else null
                    _state.value = _state.value.copy(
                        isTyping = anyOtherTyping,
                        typingUserName = typingUserName
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun sendMessage(content: String, chatId: String) {
        if (content.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = _state.value
                if (state.isEphemeralMode) {
                    // Send as ephemeral message
                    databaseRepository.sendEphemeralMessage(
                        chatId = chatId,
                        content = content,
                        replyTo = state.replyingTo?.messageId,
                        isViewOnce = state.ephemeralDuration == 0L,
                        selfDestructDuration = state.ephemeralDuration
                    )
                } else {
                    databaseRepository.sendMessage(
                        chatId = chatId,
                        content = content,
                        replyTo = state.replyingTo?.messageId
                    )
                }
                _state.value = _state.value.copy(replyingTo = null)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    error = "Error al enviar mensaje: ${e.message}"
                )
            }
        }
    }

    fun setReplyingTo(message: Message?) {
        _state.value = _state.value.copy(replyingTo = message)
    }

    /**
     * Add a reaction to a message.
     * Uses [currentChatId] which is set by [loadChat].
     */
    fun addReaction(messageId: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (currentChatId.isBlank()) return@launch
                databaseRepository.addReaction(currentChatId, messageId, emoji)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Send an image message.
     */
    fun sendImageMessage(imageUri: Uri, chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                val imageUrl = storageRepository.uploadChatImage(imageUri, chatId)
                databaseRepository.sendMediaMessage(
                    chatId = chatId,
                    mediaUrl = imageUrl,
                    mediaType = "IMAGE",
                    caption = ""
                )
                _state.value = _state.value.copy(isUploading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isUploading = false,
                    error = "Error al enviar imagen: ${e.message}"
                )
            }
        }
    }

    /**
     * Send an audio message.
     */
    fun sendAudioMessage(audioUri: Uri, chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                val audioUrl = storageRepository.uploadChatAudio(audioUri, chatId)
                databaseRepository.sendMediaMessage(
                    chatId = chatId,
                    mediaUrl = audioUrl,
                    mediaType = "AUDIO",
                    caption = ""
                )
                _state.value = _state.value.copy(isUploading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isUploading = false,
                    error = "Error al enviar audio: ${e.message}"
                )
            }
        }
    }

    /**
     * Send a video message.
     */
    fun sendVideoMessage(videoUri: Uri, chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                val videoUrl = storageRepository.uploadChatVideo(videoUri, chatId)
                databaseRepository.sendMediaMessage(
                    chatId = chatId,
                    mediaUrl = videoUrl,
                    mediaType = "VIDEO",
                    caption = ""
                )
                _state.value = _state.value.copy(isUploading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isUploading = false,
                    error = "Error al enviar video: ${e.message}"
                )
            }
        }
    }

    /**
     * Send a document message uploaded to Firebase Storage.
     */
    fun sendDocumentMessage(documentUri: Uri, chatId: String, fileName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                val documentUrl = storageRepository.uploadChatDocument(documentUri, chatId, fileName)
                databaseRepository.sendMediaMessage(
                    chatId = chatId,
                    mediaUrl = documentUrl,
                    mediaType = "DOCUMENT",
                    caption = fileName
                )
                _state.value = _state.value.copy(isUploading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isUploading = false,
                    error = "Error al enviar documento: ${e.message}"
                )
            }
        }
    }

    /**
     * Send a location message.
     */
    fun sendLocationMessage(
        latitude: Double,
        longitude: Double,
        address: String,
        chatId: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                databaseRepository.sendLocationMessage(chatId, latitude, longitude, address)
                _state.value = _state.value.copy(isUploading = false)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isUploading = false,
                    error = "Error al enviar ubicación: ${e.message}"
                )
            }
        }
    }

    /**
     * Send a sticker message.
     */
    fun sendStickerMessage(sticker: String, chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                databaseRepository.sendStickerMessage(chatId, sticker, "")
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    error = "Error al enviar sticker: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    /**
     * Delete a message (REAL implementation)
     */
    fun deleteMessage(message: Message, forEveryone: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                if (currentChatId.isBlank()) return@launch
                
                if (forEveryone) {
                    // Mark as deleted for everyone and clear content
                    val updates = mapOf(
                        "deletedForEveryone" to true,
                        "content" to "",
                        "mediaUrl" to null,
                        "mediaType" to null
                    )
                    FirebaseDatabase.getInstance().reference
                        .child("chats")
                        .child(currentChatId)
                        .child("messages")
                        .child(message.messageId)
                        .updateChildren(updates)
                        .await()
                    
                    // Update chat's lastMessage if this was the last message
                    val chatSnapshot = FirebaseDatabase.getInstance().reference
                        .child("chats")
                        .child(currentChatId)
                        .get()
                        .await()
                    
                    val lastMessageTime = chatSnapshot.child("lastMessageTime").getValue(Long::class.java)
                    if (lastMessageTime == message.timestamp) {
                        // This was the last message, update to previous message or "Este mensaje fue eliminado"
                        FirebaseDatabase.getInstance().reference
                            .child("chats")
                            .child(currentChatId)
                            .child("lastMessage")
                            .setValue("Este mensaje fue eliminado")
                            .await()
                    }
                } else {
                    // Mark as deleted only for this user
                    FirebaseDatabase.getInstance().reference
                        .child("chats")
                        .child(currentChatId)
                        .child("messages")
                        .child(message.messageId)
                        .child("deletedFor")
                        .child(currentUserId)
                        .setValue(true)
                        .await()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    error = "Error al eliminar mensaje: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Edit a message (REAL implementation)
     */
    fun editMessage(messageId: String, newContent: String) {
        if (newContent.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (currentChatId.isBlank()) return@launch
                
                val updates = mapOf(
                    "content" to newContent,
                    "edited" to true,
                    "editedAt" to System.currentTimeMillis()
                )
                
                FirebaseDatabase.getInstance().reference
                    .child("chats")
                    .child(currentChatId)
                    .child("messages")
                    .child(messageId)
                    .updateChildren(updates)
                    .await()
                
                _state.value = _state.value.copy(editingMessage = null)
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    error = "Error al editar mensaje: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Set message to edit
     */
    fun setEditingMessage(message: Message?) {
        _state.value = _state.value.copy(editingMessage = message)
    }

    /**
     * 🆕 Load MORE older messages (pagination — scroll-up).
     * Fetches 30 messages before the earliest known timestamp and prepends them.
     */
    fun loadMoreMessages() {
        val state = _state.value
        if (state.isLoadingMore || !state.hasMoreMessages || state.earliestMessageTimestamp <= 0L || currentChatId.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoadingMore = true)
            try {
                val olderMessages = databaseRepository.loadMoreMessages(
                    chatId = currentChatId,
                    beforeTimestamp = state.earliestMessageTimestamp,
                    limit = 30
                )

                if (olderMessages.isEmpty()) {
                    _state.value = _state.value.copy(isLoadingMore = false, hasMoreMessages = false)
                    return@launch
                }

                // Check if there are even older messages
                val oldestNew = olderMessages.firstOrNull()?.get("timestamp") as? Long ?: state.earliestMessageTimestamp
                val hasMore = databaseRepository.hasMoreMessages(
                    chatId = currentChatId,
                    beforeTimestamp = oldestNew
                )

                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val newMessages = olderMessages.map { data ->
                    val senderId = data["senderId"] as? String ?: ""
                    var content = data["content"] as? String ?: ""
                    val isEncrypted = data["isEncrypted"] as? Boolean ?: false
                    val payload = data["encryptedPayload"] as? String
                    if (isEncrypted && !payload.isNullOrBlank() && senderId != currentUserId) {
                        val bytes = Base64.decode(payload, Base64.NO_WRAP)
                        when (val dec = decryptMessageUseCase(senderId, bytes, MessageType.WHISPER)) {
                            is com.Azelmods.App.data.security.encryption.DecryptionResult.Success ->
                                content = dec.plaintext
                            else -> content = "🔒 No se pudo descifrar"
                        }
                    }
                    Message(
                        messageId = data["messageId"] as? String ?: "",
                        chatId = currentChatId,
                        senderId = senderId,
                        senderName = data["senderName"] as? String ?: "",
                        content = content,
                        timestamp = data["timestamp"] as? Long ?: 0L,
                        status = MessageStatus.SENT,
                        replyTo = data["replyTo"] as? String,
                        reactions = (data["reactions"] as? Map<String, String>) ?: emptyMap(),
                        mediaUrl = data["mediaUrl"] as? String,
                        mediaType = data["mediaType"] as? String,
                        deletedFor = (data["deletedFor"] as? Map<String, Boolean>) ?: emptyMap(),
                        deletedForEveryone = data["deletedForEveryone"] as? Boolean ?: false,
                        edited = data["edited"] as? Boolean ?: false,
                        editedAt = data["editedAt"] as? Long ?: 0L,
                        forwardedFrom = data["forwardedFrom"] as? String,
                        isEncrypted = isEncrypted,
                        encryptedPayload = payload
                    )
                }.filter { message ->
                    message.deletedFor[currentUserId] != true
                }

                val earliestTimestamp = newMessages.minOfOrNull { it.timestamp } ?: state.earliestMessageTimestamp
                val earliestId = newMessages.minByOrNull { it.timestamp }?.messageId ?: state.earliestMessageId

                _state.value = _state.value.copy(
                    messages = newMessages + _state.value.messages,
                    earliestMessageTimestamp = earliestTimestamp,
                    earliestMessageId = earliestId,
                    isLoadingMore = false,
                    hasMoreMessages = hasMore
                )
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isLoadingMore = false,
                    error = "Error al cargar más mensajes: ${e.message}"
                )
            }
        }
    }
}
