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
    val showEphemeralPicker: Boolean = false,      // Show duration picker dropdown
    // ── Translation ──
    val translatingMessageIds: Set<String> = emptySet(), // IDs currently being translated
    val translationError: String? = null,
    val translationNotice: String? = null,              // Aviso no-bloqueante (truncado / cuota baja)
    val translatedMessages: Map<String, String> = emptyMap() // messageId -> translated text
)

@Suppress("UNCHECKED_CAST")
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val databaseRepository: RealtimeDatabaseRepository,
    private val backgroundRepository: com.Azelmods.App.data.repository.ChatBackgroundRepository,
    private val decryptMessageUseCase: DecryptMessageUseCase,
    private val cacheManager: CacheManager,
    private val translationService: com.Azelmods.App.data.translation.TranslationService,
    private val userPreferences: com.Azelmods.App.data.preferences.UserPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    
    private val _chatBackground = MutableStateFlow(com.Azelmods.App.data.model.BackgroundConfig())
    val chatBackground: StateFlow<com.Azelmods.App.data.model.BackgroundConfig> = _chatBackground.asStateFlow()

    private companion object {
        // Valores por defecto del contacto demo (Azel Assistant), usados como
        // fallback cuando Firebase no devuelve datos para el Demo Chat.
        const val DEMO_USER_ID = "demo_azel_assistant"
        const val DEMO_USER_NAME = "Azel Assistant"
        const val DEMO_USERNAME = "@azel"
        const val DEMO_BIO = "I'm here to help you explore AzelGram features!"

        // Umbral (en palabras restantes) a partir del cual se avisa que la
        // cuota diaria gratuita de traducción está por agotarse.
        const val LOW_QUOTA_THRESHOLD = 150
    }

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
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val state = _state.value
                databaseRepository.sendEphemeralMediaMessage(
                    chatId = targetChatId,
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
    /**
     * Returns true when [rawId] already is a canonical chatId that includes
     * [currentUserId] as one of its underscore-separated participants.
     * Firebase UIDs never contain underscores, so boundary checks are safe.
     */
    private fun isCanonicalForUser(rawId: String, currentUserId: String): Boolean {
        if (currentUserId.isBlank()) return false
        return rawId == currentUserId ||
            rawId.startsWith("${currentUserId}_") ||
            rawId.endsWith("_$currentUserId") ||
            rawId.contains("_${currentUserId}_")
    }

    /**
     * Normalizes a raw navigation argument into the canonical chatId used for
     * storage. Group chats keep their id; an already-canonical id is returned
     * as-is; a bare peer UID is combined with the current user (sorted) so both
     * participants always resolve to the same chat node.
     */
    private fun resolveCanonicalChatId(rawId: String, currentUserId: String): String {
        if (rawId.isBlank()) return rawId
        if (rawId.startsWith("group_")) return rawId
        if (isCanonicalForUser(rawId, currentUserId)) return rawId
        return listOf(currentUserId, rawId).sorted().joinToString("_")
    }

    /**
     * Derives the peer (other participant) UID from a canonical 1:1 chatId.
     */
    private fun derivePeerId(canonicalId: String, currentUserId: String): String? {
        if (canonicalId.startsWith("group_")) return null
        val peer = when {
            canonicalId.startsWith("${currentUserId}_") ->
                canonicalId.removePrefix("${currentUserId}_")
            canonicalId.endsWith("_$currentUserId") ->
                canonicalId.removeSuffix("_$currentUserId")
            canonicalId.contains("_${currentUserId}_") ->
                canonicalId.replace("_${currentUserId}_", "_")
            else -> canonicalId
        }
        return peer.takeIf { it.isNotBlank() && it != currentUserId }
    }

    /**
     * Resolves the contact for a chat. Prefers the chat's `members` node (covers
     * legacy / group structures) and falls back to deriving the peer from the
     * canonical chatId, then fetches the real user profile (name + photo).
     */
    private suspend fun resolveContact(
        canonicalChatId: String,
        rawChatId: String,
        currentUserId: String
    ): User? {
        if (canonicalChatId.startsWith("group_")) return null

        val otherUid: String? = try {
            val membersSnapshot = FirebaseDatabase.getInstance().reference
                .child("chats")
                .child(canonicalChatId)
                .child("members")
                .get()
                .await()

            val uids = when (val value = membersSnapshot.value) {
                is List<*> -> value.filterIsInstance<String>()
                is Map<*, *> -> value.keys.filterIsInstance<String>()
                else -> emptyList()
            }
            uids.firstOrNull { it != currentUserId }
                ?: derivePeerId(canonicalChatId, currentUserId)
                ?: rawChatId.takeIf { it.isNotBlank() && it != currentUserId }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed reading members, deriving peer", e)
            derivePeerId(canonicalChatId, currentUserId)
                ?: rawChatId.takeIf { it.isNotBlank() && it != currentUserId }
        }

        val uid = otherUid ?: return null

        val isDemoContact = uid == DEMO_USER_ID

        return try {
            databaseRepository.getUserById(uid)?.let { data ->
                User(
                    uid = data["uid"] as? String ?: uid,
                    name = data["displayName"] as? String
                        ?: data["name"] as? String
                        ?: if (isDemoContact) DEMO_USER_NAME else "Usuario",
                    username = data["username"] as? String ?: "",
                    email = data["email"] as? String ?: "",
                    photoUrl = data["photoUrl"] as? String,
                    bio = data["bio"] as? String ?: "",
                    isOnline = data["isOnline"] as? Boolean ?: false,
                    lastSeen = data["lastSeen"] as? Long ?: 0L
                )
            } ?: defaultContactFor(uid, isDemoContact)
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Failed to fetch contact $uid", e)
            defaultContactFor(uid, isDemoContact)
        }
    }

    /**
     * Construye un [User] por defecto cuando Firebase no devuelve datos del
     * contacto. Para el contacto demo usa los valores de Azel Assistant para
     * que el Demo Chat nunca crashee por datos ausentes (Requisito 5.5).
     */
    private fun defaultContactFor(uid: String, isDemoContact: Boolean): User =
        if (isDemoContact) {
            User(
                uid = DEMO_USER_ID,
                name = DEMO_USER_NAME,
                username = DEMO_USERNAME,
                bio = DEMO_BIO,
                isOnline = true
            )
        } else {
            User(uid = uid, name = "Usuario")
        }

    /**
     * Returns the canonical chatId resolved during [loadChat]. Falls back to the
     * passed value only if a chat hasn't been loaded yet.
     */
    private fun effectiveChatId(passed: String): String =
        currentChatId.ifBlank { passed }

    fun loadChat(rawChatId: String) {
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

                // ── Normalize the incoming id into a canonical chatId ──
                // Navigation may pass either a full canonical chatId ("uidA_uidB")
                // or just the peer's UID. We resolve both cases here so the chat,
                // its members and its messages always live under the same node.
                val chatId = resolveCanonicalChatId(rawChatId, currentUserId)
                currentChatId = chatId

                // Resolve the contact (peer) for this chat.
                val contact: User? = resolveContact(chatId, rawChatId, currentUserId)

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
                  try {
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
                                try {
                                    val bytes = Base64.decode(payload, Base64.NO_WRAP)
                                    when (val dec = decryptMessageUseCase(senderId, bytes, MessageType.WHISPER)) {
                                        is com.Azelmods.App.data.security.encryption.DecryptionResult.Success ->
                                            content = dec.plaintext
                                        else -> content = "🔒 No se pudo descifrar"
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("ChatViewModel", "Decryption crash prevented", e)
                                    content = "🔒 Error de descifrado"
                                }
                            }
                            
                            val timestampRaw = data["timestamp"]
                            val timestampVal = when (timestampRaw) {
                                is Long -> timestampRaw
                                is Double -> timestampRaw.toLong()
                                is String -> timestampRaw.toLongOrNull() ?: System.currentTimeMillis()
                                else -> System.currentTimeMillis()
                            }
                            
                            Message(
                                messageId = data["messageId"] as? String ?: "",
                                chatId = chatId,
                                senderId = senderId,
                                senderName = data["senderName"] as? String ?: "",
                                content = content,
                                timestamp = timestampVal,
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
                        }.distinctBy { it.messageId.ifBlank { "${it.timestamp}_${it.senderId}" } }

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
                  } catch (ex: Exception) {
                    // El Flow de mensajes puede cerrarse con excepción (p. ej. permiso
                    // denegado al leer el nodo del chat) o fallar al mapear. Lo manejamos
                    // aquí para que NUNCA crashee la app: se degrada a los mensajes en caché
                    // (si los hay) sin cerrar la pantalla.
                    android.util.Log.e("ChatViewModel", "Recolección de mensajes falló (manejado, sin crash)", ex)
                    _state.value = _state.value.copy(isLoading = false)
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

    fun refreshChat() {
        if (currentChatId.isNotBlank()) {
            loadChat(currentChatId)
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
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("ChatViewModel", "📤 Sending message to chat: $targetChatId")
                
                val state = _state.value
                if (state.isEphemeralMode) {
                    // Send as ephemeral message
                    databaseRepository.sendEphemeralMessage(
                        chatId = targetChatId,
                        content = content,
                        replyTo = state.replyingTo?.messageId,
                        isViewOnce = state.ephemeralDuration == 0L,
                        selfDestructDuration = state.ephemeralDuration
                    )
                } else {
                    databaseRepository.sendMessage(
                        chatId = targetChatId,
                        content = content,
                        replyTo = state.replyingTo?.messageId
                    )
                }
                
                android.util.Log.d("ChatViewModel", "✅ Message sent successfully")
                _state.value = _state.value.copy(replyingTo = null, error = null)
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "❌ Error sending message: ${e.message}", e)
                
                // Detectar si es un error de red real o un error de Firebase
                val isNetworkError = e is java.net.UnknownHostException || 
                                     e is java.net.SocketTimeoutException ||
                                     e is java.io.IOException
                
                val isAuthError = e.message?.contains("not authenticated", ignoreCase = true) == true ||
                                  e.message?.contains("permission denied", ignoreCase = true) == true
                
                if (isAuthError) {
                    // Error de autenticación - no es problema de red
                    android.util.Log.e("ChatViewModel", "🔐 Authentication error - user may not be logged in")
                    _state.value = _state.value.copy(
                        replyingTo = null,
                        error = "Error de autenticación. Por favor, inicia sesión de nuevo."
                    )
                } else if (isNetworkError) {
                    // Error de red real - guardar en cola offline
                    android.util.Log.w("ChatViewModel", "📡 Network error - saving to offline queue")
                    try {
                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        cacheManager.database.pendingMessageDao().insert(
                            com.Azelmods.App.data.local.entity.PendingMessageEntity(
                                chatId = targetChatId,
                                content = content,
                                senderId = currentUserId,
                                replyTo = _state.value.replyingTo?.messageId,
                                isEphemeral = _state.value.isEphemeralMode,
                                isViewOnce = _state.value.ephemeralDuration == 0L,
                                selfDestructDuration = _state.value.ephemeralDuration
                            )
                        )
                        _state.value = _state.value.copy(
                            replyingTo = null,
                            error = "Mensaje guardado. Se enviará cuando haya conexión."
                        )
                    } catch (e2: Exception) {
                        android.util.Log.e("ChatViewModel", "❌ Error saving to offline queue: ${e2.message}", e2)
                        _state.value = _state.value.copy(
                            replyingTo = null,
                            error = "Error al guardar mensaje: ${e2.message}"
                        )
                    }
                } else {
                    // Otro tipo de error (Firebase, permisos, etc.)
                    android.util.Log.e("ChatViewModel", "⚠️ Firebase error: ${e.message}")
                    _state.value = _state.value.copy(
                        replyingTo = null,
                        error = "Error al enviar: ${e.message ?: "Error desconocido"}"
                    )
                }
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
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                val imageUrl = storageRepository.uploadChatImage(imageUri, targetChatId)
                databaseRepository.sendMediaMessage(
                    chatId = targetChatId,
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
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                val audioUrl = storageRepository.uploadChatAudio(audioUri, targetChatId)
                databaseRepository.sendMediaMessage(
                    chatId = targetChatId,
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
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                val videoUrl = storageRepository.uploadChatVideo(videoUri, targetChatId)
                databaseRepository.sendMediaMessage(
                    chatId = targetChatId,
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
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                val documentUrl = storageRepository.uploadChatDocument(documentUri, targetChatId, fileName)
                databaseRepository.sendMediaMessage(
                    chatId = targetChatId,
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
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                databaseRepository.sendLocationMessage(targetChatId, latitude, longitude, address)
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
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                databaseRepository.sendStickerMessage(targetChatId, sticker, "")
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
     * Translate a message into the preferred language using [TranslationService].
     * Toggles: if a translation already exists for the message, it is removed.
     */
    fun translateMessage(messageId: String, text: String) {
        if (messageId.isBlank() || text.isBlank()) return
        // Toggle off if already translated
        if (_state.value.translatedMessages.containsKey(messageId)) {
            _state.value = _state.value.copy(
                translatedMessages = _state.value.translatedMessages - messageId,
                translationError = null
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                translatingMessageIds = _state.value.translatingMessageIds + messageId,
                translationError = null
            )
            try {
                val prefLang = userPreferences.translationLanguage.value
                val targetLang = if (prefLang == "auto" || prefLang.isBlank()) {
                    java.util.Locale.getDefault().language.ifBlank { "es" }
                } else prefLang
                val result = translationService.translate(text, targetLang = targetLang)
                result.onSuccess { translation ->
                    // Avisos preventivos: antes el truncado a 500 chars era silencioso y
                    // el límite de cuota solo se descubría cuando la API ya fallaba.
                    val notices = buildList {
                        if (translation.wasTruncated) {
                            add("Mensaje largo: solo se tradujeron los primeros " +
                                "${com.Azelmods.App.data.translation.TranslationService.MAX_CHARS} caracteres.")
                        }
                        if (translation.remainingWords <= LOW_QUOTA_THRESHOLD) {
                            add("Cuota gratuita de hoy casi agotada: quedan ~${translation.remainingWords} " +
                                "palabras de ${com.Azelmods.App.data.translation.TranslationQuotaTracker.DAILY_WORD_LIMIT}.")
                        }
                    }
                    _state.value = _state.value.copy(
                        translatedMessages = _state.value.translatedMessages + (messageId to translation.text),
                        translatingMessageIds = _state.value.translatingMessageIds - messageId,
                        translationNotice = notices.joinToString(" ").ifBlank { null }
                    )
                }.onFailure { e ->
                    _state.value = _state.value.copy(
                        translationError = "No se pudo traducir: ${e.message}",
                        translatingMessageIds = _state.value.translatingMessageIds - messageId
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    translationError = "No se pudo traducir: ${e.message}",
                    translatingMessageIds = _state.value.translatingMessageIds - messageId
                )
            }
        }
    }
    
    fun clearTranslationError() {
        _state.value = _state.value.copy(translationError = null)
    }

    fun clearTranslationNotice() {
        _state.value = _state.value.copy(translationNotice = null)
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
                        try {
                            val bytes = Base64.decode(payload, Base64.NO_WRAP)
                            when (val dec = decryptMessageUseCase(senderId, bytes, MessageType.WHISPER)) {
                                is com.Azelmods.App.data.security.encryption.DecryptionResult.Success ->
                                    content = dec.plaintext
                                else -> content = "🔒 No se pudo descifrar"
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ChatViewModel", "Decryption crash prevented in loadMore", e)
                            content = "🔒 Error de descifrado"
                        }
                    }
                    val timestampRaw = data["timestamp"]
                    val timestampVal = when (timestampRaw) {
                        is Long -> timestampRaw
                        is Double -> timestampRaw.toLong()
                        is String -> timestampRaw.toLongOrNull() ?: System.currentTimeMillis()
                        else -> System.currentTimeMillis()
                    }
                    Message(
                        messageId = data["messageId"] as? String ?: "",
                        chatId = currentChatId,
                        senderId = senderId,
                        senderName = data["senderName"] as? String ?: "",
                        content = content,
                        timestamp = timestampVal,
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
