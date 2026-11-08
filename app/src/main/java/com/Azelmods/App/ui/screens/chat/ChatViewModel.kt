package com.Azelmods.App.ui.screens.chat

import com.Azelmods.App.i18n.tr

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Identidad de un mensaje dentro de la lista: es a la vez la clave de la `LazyColumn` y lo
 * que se usa para quitar repetidos. Al ser una sola definición, una lista pasada por
 * [mergeMessages] no puede provocar el crash "Key was already used".
 */
internal fun Message.listKey(): String =
    messageId.ifBlank { "fallback_${timestamp}_${senderId}_${content.hashCode()}_${mediaUrl.hashCode()}" }

/**
 * Une lo que llega en vivo con las páginas antiguas, sin repetidos y en orden. [live] va
 * primero porque su copia es la actual (estado de lectura, reacciones) y es la que debe ganar.
 */
internal fun mergeMessages(live: List<Message>, older: List<Message>): List<Message> =
    (live + older).distinctBy { it.listKey() }.sortedBy { it.timestamp }

/** Un miembro de un grupo, para saber quién escribe y poder mencionarlo. */
data class GroupChatMember(val uid: String, val name: String, val photoUrl: String?)

data class ChatState(
    val messages: List<Message> = emptyList(),
    val contact: User? = null,
    /** Solo en grupos: su nombre y sus miembros. */
    val groupName: String? = null,
    val groupPhoto: String? = null,
    val groupMembers: Map<String, GroupChatMember> = emptyMap(),
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
    /** Temporizador compartido del chat, en segundos. 0 = los mensajes no caducan. */
    val chatTimerSeconds: Long = 0L,
    /** El siguiente mensaje o archivo se envía como "una sola vista". */
    val viewOnceNext: Boolean = false,
    val showEphemeralPicker: Boolean = false,
    /** Reproducir vídeos dentro del chat sin tocarlos (ajuste de descarga automática). */
    val autoplayVideos: Boolean = false,
    // ── Translation ──
    val translatingMessageIds: Set<String> = emptySet(), // IDs currently being translated
    val translationError: String? = null,
    val translationNotice: String? = null,              // Aviso no-bloqueante (truncado / cuota baja)
    val translatedMessages: Map<String, String> = emptyMap(), // messageId -> translated text
    // ── Funciones de IA (todas opcionales, se activan en Ajustes → IA) ──
    val aiAvailable: Boolean = false,               // hay proveedor de IA configurado
    val smartRepliesEnabled: Boolean = false,
    val smartReplies: List<String> = emptyList(),
    val isLoadingSmartReplies: Boolean = false,
    val toneSuggestionsEnabled: Boolean = false,
    val isRewritingTone: Boolean = false,
    val summaryEnabled: Boolean = false,
    val isSummarizing: Boolean = false,
    val conversationSummary: String? = null,        // no null = mostrar el diálogo
    val autoTranslateEnabled: Boolean = false,
    val voiceDictationEnabled: Boolean = false,
    // ── Búsqueda dentro de la conversación ──
    val isSearchOpen: Boolean = false,
    val searchQuery: String = "",
    /** Ids de los mensajes que casan, del más reciente al más antiguo. */
    val searchResults: List<String> = emptyList(),
    /** Posición dentro de [searchResults] que se está mostrando. */
    val searchIndex: Int = 0,
    // ── Silencio de esta conversación (ajuste personal) ──
    val isMuted: Boolean = false
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
    private val userPreferences: com.Azelmods.App.data.preferences.UserPreferences,
    private val demoAccountManager: com.Azelmods.App.data.demo.DemoAccountManager,
    private val aiAssistService: com.Azelmods.App.data.ai.AiAssistService,
    private val aiFeaturePreferences: com.Azelmods.App.data.preferences.AiFeaturePreferences,
    private val photoEnhancer: com.Azelmods.App.utils.PhotoEnhancer,
    @dagger.hilt.android.qualifiers.ApplicationContext private val appContext: android.content.Context
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
        const val DEMO_BIO = "Estoy aquí para enseñarte lo que hace NexusChat"

        /**
         * Cuántos mensajes recientes traduce automáticamente al abrir un chat.
         * Sin este límite, entrar en una conversación larga dispararía cientos
         * de peticiones de traducción de golpe.
         */
        const val AUTO_TRANSLATE_WINDOW = 10

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

    /** Petición de respuestas sugeridas en vuelo; se cancela si llega otro mensaje. */
    private var smartRepliesJob: Job? = null

    init {
        observeAiFeatureFlags()
        viewModelScope.launch {
            userPreferences.autoDownloadVideos.collect { enabled ->
                _state.value = _state.value.copy(autoplayVideos = enabled)
            }
        }
    }
    
    /**
     * Load chat background configuration
     */
    /** Abre o cierra la hoja del temporizador. */
    fun toggleEphemeralPicker() {
        _state.value = _state.value.copy(showEphemeralPicker = !_state.value.showEphemeralPicker)
    }

    fun dismissEphemeralPicker() {
        _state.value = _state.value.copy(showEphemeralPicker = false)
    }

    /** Marca el siguiente envío como "una sola vista". */
    fun setViewOnceNext(enabled: Boolean) {
        _state.value = _state.value.copy(viewOnceNext = enabled, showEphemeralPicker = false)
    }

    /**
     * Cambia el temporizador del chat. Es compartido, como en Signal: lo que elige uno
     * vale para los dos y queda un aviso en la conversación para que nadie lo ignore.
     */
    fun setChatTimer(seconds: Long) {
        val chatId = currentChatId
        if (chatId.isBlank()) return
        _state.value = _state.value.copy(showEphemeralPicker = false)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val notice = if (seconds == 0L) tr("Mensajes temporales desactivados")
                else "Los mensajes nuevos desaparecerán ${timerLabel(seconds)} después de leerse"
                databaseRepository.setChatTimer(chatId, seconds, notice)
            }.onFailure {
                _state.value = _state.value.copy(error = "No se pudo cambiar el temporizador: ${it.message}")
            }
        }
    }

    /**
     * Vigila el chat abierto: refleja el temporizador común y, cada segundo, retira los
     * mensajes cuyo tiempo se ha acabado, en pantalla y en el servidor.
     *
     * La versión anterior leía el nodo `chats` completo, que las reglas de seguridad
     * niegan a cualquier cliente, así que no borraba nada nunca.
     */
    private fun startEphemeralCleanup(chatId: String) {
        ephemeralCleanupJob?.cancel()
        ephemeralCleanupJob = viewModelScope.launch(Dispatchers.IO) {
            launch {
                runCatching {
                    databaseRepository.observeChatTimer(chatId).collect { seconds ->
                        _state.value = _state.value.copy(chatTimerSeconds = seconds)
                    }
                }
            }
            while (true) {
                delay(1_000)
                val now = databaseRepository.serverNow()
                val expired = _state.value.messages.filter { it.expiresAt in 1..now }
                if (expired.isEmpty()) continue
                removeFromList(expired.map { it.messageId }.toSet())
                expired.forEach { deleteEphemeral(chatId, it) }
            }
        }
    }

    private val deletingEphemeralIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

    /**
     * Páginas antiguas ya traídas al subir por el chat. El listener en vivo solo cubre los
     * últimos 30 mensajes: si no se guardan aparte, cada mensaje nuevo o recibo de lectura
     * borraba de la pantalla todo lo que el usuario había cargado.
     */
    @Volatile private var olderMessages: List<Message> = emptyList()
    @Volatile private var lastLiveWindow: List<Message> = emptyList()
    private var loadMoreJob: Job? = null

    private fun removeFromList(ids: Set<String>) {
        olderMessages = olderMessages.filterNot { it.messageId in ids }
        lastLiveWindow = lastLiveWindow.filterNot { it.messageId in ids }
        _state.update { current -> current.copy(messages = current.messages.filterNot { it.messageId in ids }) }
    }

    private suspend fun deleteEphemeral(chatId: String, message: Message) {
        if (!deletingEphemeralIds.add(message.messageId)) return
        runCatching { databaseRepository.deleteEphemeralMessage(chatId, message.messageId, message.mediaUrl) }
            .onFailure {
                deletingEphemeralIds.remove(message.messageId)
                android.util.Log.w("ChatViewModel", "No se pudo borrar el mensaje temporal: ${it.message}")
            }
    }

    /** El destinatario ha visto un mensaje temporal: empieza su cuenta atrás. */
    fun markMessageViewed(message: Message) {
        if (!message.isEphemeral || currentChatId.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { databaseRepository.markEphemeralMessageViewed(currentChatId, message.messageId) }
        }
    }

    /** Se cerró un mensaje de una sola vista: desaparece para todos. */
    fun consumeViewOnce(message: Message) {
        val chatId = currentChatId
        if (chatId.isBlank()) return
        removeFromList(setOf(message.messageId))
        viewModelScope.launch(Dispatchers.IO) { deleteEphemeral(chatId, message) }
    }

    /**
     * Único camino de los archivos al chat: aplica el temporizador o la "una sola vista"
     * a fotos, vídeos, GIF y audio igual que al texto. Antes estos envíos ignoraban el
     * modo temporal, de modo que justo lo más delicado era lo que no desaparecía.
     */
    private suspend fun publishMedia(chatId: String, url: String, type: String, caption: String = "", extra: Map<String, Any> = emptyMap()) {
        val state = _state.value
        if (state.viewOnceNext || state.chatTimerSeconds > 0) {
            databaseRepository.sendEphemeralMediaMessage(
                chatId = chatId,
                mediaUrl = url,
                mediaType = type,
                caption = caption,
                isViewOnce = state.viewOnceNext,
                selfDestructDuration = state.chatTimerSeconds,
                extra = extra
            )
            if (state.viewOnceNext) _state.value = _state.value.copy(viewOnceNext = false)
        } else {
            databaseRepository.sendMediaMessage(chatId = chatId, mediaUrl = url, mediaType = type, caption = caption, extra = extra)
        }
    }

    /** GIF desde el teléfono, tal cual para conservar la animación. */
    fun sendGifMessage(uri: Uri, chatId: String) = uploadAndSend(chatId, "GIF") { target ->
        publishMedia(target, storageRepository.uploadChatGif(uri, target), "GIF")
    }

    /** Vídeo corto que se repite sin sonido dentro del chat, como los GIF de Telegram. */
    fun sendAnimationMessage(uri: Uri, chatId: String) = uploadAndSend(chatId, "animación") { target ->
        publishMedia(target, storageRepository.uploadChatVideo(uri, target), "GIF_VIDEO")
    }

    /**
     * GIF elegido en la búsqueda. Se descarga y se sube a nuestro almacenamiento: si el
     * mensaje apuntara a GIPHY, el teléfono del destinatario le revelaría su IP.
     */
    fun sendRemoteGif(gifUrl: String, chatId: String) = uploadAndSend(chatId, "GIF") { target ->
        val file = java.io.File(appContext.cacheDir, "gif_${System.currentTimeMillis()}.gif")
        try {
            okhttp3.OkHttpClient().newCall(okhttp3.Request.Builder().url(gifUrl).build()).execute().use { response ->
                if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                file.outputStream().use { out -> response.body?.byteStream()?.copyTo(out) }
            }
            publishMedia(target, storageRepository.uploadChatGif(Uri.fromFile(file), target), "GIF")
        } finally {
            file.delete()
        }
    }

    private fun uploadAndSend(chatId: String, what: String, block: suspend (String) -> Unit) {
        val target = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                block(target)
                _state.value = _state.value.copy(isUploading = false)
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "No se pudo enviar $what", e)
                _state.value = _state.value.copy(isUploading = false, error = "No se pudo enviar $what: ${e.message}")
            }
        }
    }

    /** Número → texto del temporizador, p. ej. 3600 → "1 hora". */
    private fun timerLabel(seconds: Long): String = when {
        seconds >= 604_800 -> tr("1 semana")
        seconds >= 86_400 -> tr("1 día")
        seconds >= 28_800 -> tr("8 horas")
        seconds >= 3_600 -> tr("1 hora")
        seconds >= 300 -> tr("5 minutos")
        else -> "$seconds segundos"
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

    private suspend fun loadGroupInfo(chatId: String) {
        runCatching {
            val chat = FirebaseDatabase.getInstance().reference.child("chats").child(chatId)
            val name = chat.child("groupName").get().await().getValue(String::class.java).orEmpty()
            val photo = chat.child("groupPhoto").get().await().getValue(String::class.java)?.ifBlank { null }
            val ids = chat.child("members").get().await().children.mapNotNull { it.key }
            val members = ids.associateWith { uid ->
                val user = runCatching { databaseRepository.getUserById(uid) }.getOrNull()
                GroupChatMember(
                    uid = uid,
                    name = (user?.get("displayName") as? String)?.ifBlank { null }
                        ?: (user?.get("name") as? String)?.ifBlank { null }
                        ?: tr("Usuario"),
                    photoUrl = (user?.get("photoUrl") as? String)?.ifBlank { null }
                )
            }
            _state.value = _state.value.copy(groupName = name, groupPhoto = photo, groupMembers = members)
        }.onFailure { android.util.Log.w("ChatViewModel", "No se pudo leer el grupo: ${it.message}") }
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
                    name = DEMO_USER_NAME.takeIf { isDemoContact }
                        ?: data["displayName"] as? String
                        ?: data["name"] as? String
                        ?: tr("Usuario"),
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
    /** El identificador real del chat abierto. */
    fun chatIdFor(passed: String): String = effectiveChatId(passed)

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
                com.Azelmods.App.service.ActiveChat.chatId = chatId

                // El chat de bienvenida se sembraba SOLO al abrirlo desde "Nueva
                // conversación". Entrando por la tarjeta Demo de la pantalla principal
                // el chat aparecía vacío. Sembrarlo aquí cubre las dos entradas
                // (initializeDemoAccount es idempotente: sale solo si ya se creó).
                if (isDemoChat(chatId)) {
                    runCatching { demoAccountManager.initializeDemoAccount(currentUserId) }
                        .onFailure {
                            android.util.Log.w("ChatViewModel", "No se pudo sembrar el chat demo: ${it.message}")
                        }
                }

                // Resolve the contact (peer) for this chat.
                val contact: User? = resolveContact(chatId, rawChatId, currentUserId)

                _state.value = _state.value.copy(
                    contact = contact,
                    isLoading = false
                )
                // En paralelo: los mensajes guardados no esperan a la red de los miembros.
                if (chatId.startsWith("group_")) viewModelScope.launch(Dispatchers.IO) { loadGroupInfo(chatId) }

                // ── OFFLINE CACHE: show cached messages immediately ──
                val cachedMessages = cacheManager.getCachedMessages(chatId)
                if (cachedMessages.isNotEmpty()) {
                    android.util.Log.d("ChatViewModel", "📦 Loaded ${cachedMessages.size} cached messages for $chatId")
                    _state.value = _state.value.copy(messages = cachedMessages.distinctBy { it.listKey() })
                }

                // Otra conversación: lo paginado de la anterior no pinta nada aquí.
                loadMoreJob?.cancel()
                olderMessages = emptyList()
                lastLiveWindow = emptyList()

                // Launch typing observer in its own coroutine so it doesn't block message collection
                observeTypingStatus(chatId)

                // Estado de silencio de esta conversación, para el menú.
                loadMuteState(chatId)

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
                        val peerId = peerIdOf(chatId, currentUserId)
                        val now = databaseRepository.serverNow()
                        val messages = messagesData.map { data -> mapMessage(data, chatId, peerId) }
                            .filter { message -> message.deletedFor[currentUserId] != true }
                            // Lo caducado no vuelve a aparecer aunque el borrado siga en camino.
                            .filterNot { it.expiresAt in 1..now }
                            .distinctBy { it.listKey() }

                        // ── SAVE TO ROOM CACHE ──
                        // Lo temporal no se guarda en el teléfono: sobreviviría al borrado.
                        try {
                            cacheManager.cacheMessages(messages.filterNot { it.isEphemeral || it.mediaType == "SYSTEM" })
                        } catch (e: Exception) {
                            android.util.Log.e("ChatViewModel", "Failed to cache messages", e)
                        }

                        // La ventana en vivo son los últimos 30: con cada mensaje nuevo, el
                        // más antiguo sale de ella. Sigue siendo historial, no un borrado.
                        val liveKeys = messages.mapTo(HashSet()) { it.listKey() }
                        val liveStart = messages.minOfOrNull { it.timestamp } ?: Long.MAX_VALUE
                        olderMessages = olderMessages + lastLiveWindow.filter {
                            it.timestamp < liveStart && it.listKey() !in liveKeys
                        }
                        lastLiveWindow = messages

                        val merged = mergeMessages(messages, olderMessages)
                            .filterNot { it.expiresAt in 1..now }

                        _state.update { current ->
                            current.copy(
                                messages = merged,
                                earliestMessageTimestamp = merged.firstOrNull()?.timestamp ?: 0L,
                                earliestMessageId = merged.firstOrNull()?.messageId.orEmpty()
                            )
                        }

                        // El recorrido del bot de bienvenida se reconstruye a partir
                        // de lo que ya hay escrito en el chat. `demoTurnIndex` es un
                        // campo del ViewModel y volvía a 0 cada vez que se entraba a
                        // la conversación, así que el asistente repetía el paso uno
                        // del tour una y otra vez: exactamente la sensación de "el
                        // bot se reinicia".
                        if (isDemoChat(chatId)) {
                            demoTurnIndex = messages.count { it.senderId == currentUserId }
                        }

                        // Funciones de IA que reaccionan a cada nuevo mensaje.
                        // Ambas comprueban por dentro si están activadas, así que
                        // en la configuración por defecto no hacen nada.
                        refreshSmartReplies()
                        autoTranslateIncoming()

                        // Recibos de lectura: marca los mensajes entrantes como
                        // "read" mientras el chat está abierto. Solo escribe deltas,
                        // así que no genera bucles de re-emisión.
                        viewModelScope.launch(Dispatchers.IO) {
                            runCatching { databaseRepository.markMessagesAsRead(chatId) }
                        }
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
     * Convierte un mensaje de la base de datos en el modelo de la app.
     *
     * Es el único sitio donde se hace. Antes había dos copias y ninguna leía los campos
     * de los mensajes temporales: el servidor los guardaba pero la app nunca se enteraba,
     * y por eso no había cuenta atrás ni "una sola vista", ni desaparecía nada.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun mapMessage(data: Map<String, Any>, chatId: String, peerId: String): Message {
        val isEncrypted = data["isEncrypted"] as? Boolean ?: false
        val payload = data["encryptedPayload"] as? String
        val groupKeyId = data["groupKeyId"] as? String
        val content = if (isEncrypted && groupKeyId != null && payload != null) {
            databaseRepository.decryptGroupMessage(chatId, groupKeyId, payload) ?: tr("🔒 No se pudo descifrar")
        } else {
            decryptedContent(
                rawContent = data["content"] as? String ?: "",
                isEncrypted = isEncrypted,
                payload = payload,
                peerId = peerId
            )
        }
        val timestamp = when (val raw = data["timestamp"]) {
            is Number -> raw.toLong()
            is String -> raw.toLongOrNull() ?: System.currentTimeMillis()
            else -> System.currentTimeMillis()
        }
        val viewedBy = when (val raw = data["viewedBy"]) {
            is Map<*, *> -> raw.keys.filterIsInstance<String>()
            is List<*> -> raw.filterIsInstance<String>()
            else -> emptyList()
        }
        return Message(
            messageId = data["messageId"] as? String ?: "",
            chatId = chatId,
            senderId = data["senderId"] as? String ?: "",
            senderName = data["senderName"] as? String ?: "",
            content = content,
            timestamp = timestamp,
            status = parseMessageStatus(data["status"]),
            replyTo = data["replyTo"] as? String,
            isVoice = data["isVoice"] as? Boolean ?: false,
            reactions = (data["reactions"] as? Map<String, String>) ?: emptyMap(),
            mediaUrl = data["mediaUrl"] as? String,
            mediaType = data["mediaType"] as? String,
            deletedFor = (data["deletedFor"] as? Map<String, Boolean>) ?: emptyMap(),
            deletedForEveryone = data["deletedForEveryone"] as? Boolean ?: false,
            edited = data["edited"] as? Boolean ?: false,
            editedAt = (data["editedAt"] as? Number)?.toLong() ?: 0L,
            forwardedFrom = data["forwardedFrom"] as? String,
            isEncrypted = isEncrypted,
            encryptedPayload = payload,
            latitude = (data["latitude"] as? Number)?.toDouble(),
            longitude = (data["longitude"] as? Number)?.toDouble(),
            isEphemeral = data["isEphemeral"] as? Boolean ?: false,
            isViewOnce = data["isViewOnce"] as? Boolean ?: false,
            selfDestructDuration = (data["selfDestructDuration"] as? Number)?.toLong() ?: 0L,
            viewedAt = (data["viewedAt"] as? Number)?.toLong() ?: 0L,
            expireAt = (data["expireAt"] as? Number)?.toLong() ?: 0L,
            selfDestructAt = (data["selfDestructAt"] as? Number)?.toLong() ?: 0L,
            viewedBy = viewedBy
        )
    }

    fun serverNow(): Long = databaseRepository.serverNow()

    /**
     * UID del otro extremo de un chat privado, o cadena vacía si es un grupo.
     *
     * Los chats 1:1 se nombran uniendo los dos uids ordenados con `_`
     * (ver [com.Azelmods.App.data.chat.ChatId]); los grupos usan un id push de
     * Firebase, que no contiene `_`, así que devuelven vacío y quedan fuera del
     * cifrado —igual que antes, porque el E2EE por pares no cubre grupos—.
     */
    private fun peerIdOf(chatId: String, currentUserId: String): String {
        val members = chatId.split("_").filter { it.isNotBlank() }
        if (members.size != 2) return ""
        return members.firstOrNull { it != currentUserId } ?: ""
    }

    /**
     * Descifra el contenido de un mensaje E2EE.
     *
     * Se descifra con el uid del **otro extremo del chat**, no con el del
     * remitente. El secreto ECDH es simétrico: la misma clave sirve para lo que
     * llega y para lo que uno mismo escribió. Antes se saltaba el descifrado
     * cuando el remitente era el usuario actual, así que el emisor veía
     * "🔒 Mensaje cifrado de extremo a extremo" —el marcador que se guarda en el
     * servidor en lugar del texto plano— en todos sus propios mensajes, y la
     * conversación resultaba ilegible por un lado y por el otro.
     */
    private suspend fun decryptedContent(
        rawContent: String,
        isEncrypted: Boolean,
        payload: String?,
        peerId: String
    ): String {
        if (!isEncrypted || payload.isNullOrBlank() || peerId.isBlank()) return rawContent
        return try {
            val bytes = Base64.decode(payload, Base64.NO_WRAP)
            when (val dec = decryptMessageUseCase(peerId, bytes, MessageType.WHISPER)) {
                is com.Azelmods.App.data.security.encryption.DecryptionResult.Success -> dec.plaintext
                else -> tr("🔒 No se pudo descifrar")
            }
        } catch (e: Exception) {
            android.util.Log.e("ChatViewModel", "Descifrado fallido", e)
            "🔒 Error de descifrado"
        }
    }

    private fun parseMessageStatus(raw: Any?): MessageStatus = when ((raw as? String)?.lowercase()) {
        "sending" -> MessageStatus.SENDING
        "sent" -> MessageStatus.SENT
        "delivered" -> MessageStatus.DELIVERED
        "read" -> MessageStatus.READ
        "failed" -> MessageStatus.FAILED
        else -> MessageStatus.SENT
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

    fun sendMessage(rawContent: String, chatId: String) {
        if (rawContent.isBlank()) return
        // Un comando (/dado, /calc 5+7...) se envía ya resuelto.
        val content = com.Azelmods.App.data.chat.ChatCommands.run(rawContent) ?: rawContent
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            try {
                android.util.Log.d("ChatViewModel", "📤 Sending message to chat: $targetChatId")
                
                val state = _state.value
                if (state.viewOnceNext || state.chatTimerSeconds > 0) {
                    // Sin vista previa: lo temporal no debe dejar rastro en ninguna web.
                    databaseRepository.sendEphemeralMessage(
                        chatId = targetChatId,
                        content = content,
                        replyTo = state.replyingTo?.messageId,
                        isViewOnce = state.viewOnceNext,
                        selfDestructDuration = state.chatTimerSeconds
                    )
                    if (state.viewOnceNext) _state.value = _state.value.copy(viewOnceNext = false)
                } else {
                    databaseRepository.sendMessage(
                        chatId = targetChatId,
                        content = LinkPreviews.attachTo(content),
                        replyTo = state.replyingTo?.messageId
                    )
                }

                android.util.Log.d("ChatViewModel", "✅ Message sent successfully")
                _state.value = _state.value.copy(replyingTo = null, error = null)

                // Chat de bienvenida: "Azel Assistant" responde mensaje a mensaje.
                if (isDemoChat(targetChatId) && state.chatTimerSeconds == 0L && !state.viewOnceNext) {
                    replyAsDemoAssistant(content, targetChatId)
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "❌ Error sending message: ${e.message}", e)
                
                // Detectar si es un error de red real o un error de Firebase
                val isNetworkError = e is java.net.UnknownHostException || 
                                     e is java.net.SocketTimeoutException ||
                                     e is java.io.IOException
                
                // Sesión ausente y permiso denegado son fallos distintos: mezclarlos
                // hacía que un problema de reglas de Firebase se reportara como
                // "vuelve a iniciar sesión", que nunca lo arreglaba.
                val isAuthError = e.message?.contains("not authenticated", ignoreCase = true) == true
                val isPermissionError = e.message?.contains("permission denied", ignoreCase = true) == true ||
                                        e.message?.contains("permission_denied", ignoreCase = true) == true

                if (isAuthError) {
                    android.util.Log.e("ChatViewModel", "🔐 No hay sesión activa")
                    _state.value = _state.value.copy(
                        replyingTo = null,
                        error = "Tu sesión expiró. Inicia sesión de nuevo."
                    )
                } else if (isPermissionError) {
                    android.util.Log.e("ChatViewModel", "🚫 Permiso denegado por las reglas de Firebase (no es la sesión)")
                    _state.value = _state.value.copy(
                        replyingTo = null,
                        error = "No se pudo enviar: sin permiso para escribir en este chat."
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
                                isEphemeral = _state.value.viewOnceNext || _state.value.chatTimerSeconds > 0,
                                isViewOnce = _state.value.viewOnceNext,
                                selfDestructDuration = _state.value.chatTimerSeconds
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
                        error = "Error al enviar: ${e.message ?: tr("Error desconocido")}"
                    )
                }
            }
        }
    }

    // Cuántos mensajes ha escrito el usuario al asistente demo (para avanzar el tour).
    private var demoTurnIndex = 0
    private var demoReplyJob: Job? = null

    /** `true` si el chat es el de bienvenida con Azel Assistant. */
    private fun isDemoChat(chatId: String): Boolean =
        chatId.contains(DEMO_USER_ID)

    /**
     * Hace que "Azel Assistant" responda al usuario **mensaje a mensaje** (no todo en
     * un bloque). Escribe cada respuesta como el bot con una breve pausa entre ellas;
     * la lista de mensajes se actualiza en tiempo real vía el listener de Firebase, así
     * que el usuario ve llegar las respuestas de una en una, como en un chat real.
     */
    private fun replyAsDemoAssistant(userText: String, chatId: String) {
        val replies = com.Azelmods.App.data.demo.DemoAssistant.repliesFor(userText, demoTurnIndex)
        demoTurnIndex++

        demoReplyJob?.cancel()
        demoReplyJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Pequeña espera inicial para que no llegue "instantáneo".
                delay(700)
                replies.forEachIndexed { index, reply ->
                    databaseRepository.sendMessageAs(
                        chatId = chatId,
                        senderId = DEMO_USER_ID,
                        content = reply
                    )
                    // Pausa entre mensajes proporcional a su longitud (efecto "escribiendo").
                    if (index < replies.lastIndex) {
                        delay((600L + reply.length * 18L).coerceAtMost(2200L))
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatViewModel", "Demo assistant reply failed: ${e.message}", e)
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
    fun sendImageMessage(imageUri: Uri, chatId: String, caption: String = "") {
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                // Realce opcional, y siempre en el propio dispositivo. Si falla,
                // `enhance` devuelve la Uri original, así que nunca bloquea el envío.
                val uriToUpload = if (aiFeaturePreferences.isPhotoEnhanceEnabled()) {
                    photoEnhancer.enhance(appContext, imageUri)
                } else {
                    imageUri
                }
                val imageUrl = storageRepository.uploadChatImage(uriToUpload, targetChatId)
                publishMedia(targetChatId, imageUrl, "IMAGE", caption)
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
    fun sendAudioMessage(audioUri: Uri, chatId: String, voice: Boolean = false) {
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                val audioUrl = storageRepository.uploadChatAudio(audioUri, targetChatId)
                publishMedia(targetChatId, audioUrl, "AUDIO", extra = if (voice) mapOf("isVoice" to true) else emptyMap())
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
    fun sendVideoMessage(videoUri: Uri, chatId: String, caption: String = "") {
        val targetChatId = effectiveChatId(chatId)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploading = true, error = null)
            try {
                val videoUrl = storageRepository.uploadChatVideo(videoUri, targetChatId)
                publishMedia(targetChatId, videoUrl, "VIDEO", caption)
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
                publishMedia(targetChatId, documentUrl, "DOCUMENT", fileName)
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
    /** Traduce lo que estás escribiendo antes de enviarlo, para responder en otro idioma. */
    fun translateDraft(text: String, onTranslated: (String) -> Unit) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val prefLang = userPreferences.translationLanguage.value
            val targetLang = if (prefLang == "auto" || prefLang.isBlank()) {
                java.util.Locale.getDefault().language.ifBlank { "es" }
            } else prefLang
            translationService.translate(text, targetLang = targetLang)
                .onSuccess { result -> withContext(Dispatchers.Main) { onTranslated(result.text) } }
                .onFailure { _state.value = _state.value.copy(translationError = it.message) }
        }
    }

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
    // ═══════════════════════════════════════════════════════════════════════
    //  FUNCIONES DE IA
    //
    //  Todas se apoyan en el proveedor que el usuario haya configurado en
    //  Ajustes → IA y todas están apagadas de fábrica: mandan texto del chat a
    //  un tercero, y eso no puede ocurrir sin que se pida. Si no hay proveedor,
    //  `aiAvailable` queda en false y la interfaz no muestra ninguna de estas
    //  opciones en lugar de enseñar botones que fallarían al pulsarlos.
    // ═══════════════════════════════════════════════════════════════════════

    /** Lee los interruptores y publica qué funciones debe mostrar la pantalla. */
    private fun observeAiFeatureFlags() {
        viewModelScope.launch {
            val available = aiAssistService.isAvailable()
            _state.value = _state.value.copy(aiAvailable = available)

            // El dictado usa el reconocedor del propio Android, así que no
            // depende de que haya proveedor de IA configurado.
            launch {
                aiFeaturePreferences.voiceTranscription.collect { enabled ->
                    _state.value = _state.value.copy(voiceDictationEnabled = enabled)
                }
            }

            if (!available) return@launch

            launch {
                aiFeaturePreferences.smartReplies.collect { enabled ->
                    _state.value = _state.value.copy(
                        smartRepliesEnabled = enabled,
                        smartReplies = if (enabled) _state.value.smartReplies else emptyList()
                    )
                    if (enabled) refreshSmartReplies()
                }
            }
            launch {
                aiFeaturePreferences.toneSuggestions.collect { enabled ->
                    _state.value = _state.value.copy(toneSuggestionsEnabled = enabled)
                }
            }
            launch {
                aiFeaturePreferences.conversationSummary.collect { enabled ->
                    _state.value = _state.value.copy(summaryEnabled = enabled)
                }
            }
            launch {
                aiFeaturePreferences.autoTranslate.collect { enabled ->
                    _state.value = _state.value.copy(autoTranslateEnabled = enabled)
                }
            }
        }
    }

    /**
     * Guion de la conversación para los prompts.
     *
     * Sólo se etiqueta quién habla ("YO"/"OTRO"), sin nombres reales: el modelo
     * no los necesita y no hay motivo para enviar identidades a un proveedor
     * externo.
     */
    private fun currentTranscript(maxMessages: Int = 20): String {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        return com.Azelmods.App.data.ai.AiAssistService.buildTranscript(
            messages = _state.value.messages
                .filter { !it.deletedForEveryone && it.content.isNotBlank() }
                .map { (it.senderId == me) to it.content },
            maxMessages = maxMessages
        )
    }

    /** Recalcula las tres respuestas sugeridas para el estado actual del chat. */
    fun refreshSmartReplies() {
        val state = _state.value
        if (!state.aiAvailable || !state.smartRepliesEnabled) return
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        // Sin sentido sugerir respuestas si el último mensaje es mío: no hay nada
        // a lo que contestar todavía.
        val last = state.messages.lastOrNull() ?: return
        if (last.senderId == me) {
            _state.value = state.copy(smartReplies = emptyList())
            return
        }

        smartRepliesJob?.cancel()
        smartRepliesJob = viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isLoadingSmartReplies = true)
            val result = aiAssistService.smartReplies(currentTranscript(maxMessages = 8))
            _state.value = _state.value.copy(
                isLoadingSmartReplies = false,
                smartReplies = result.getOrDefault(emptyList())
            )
        }
    }

    fun dismissSmartReplies() {
        _state.value = _state.value.copy(smartReplies = emptyList())
    }

    /** Resume la conversación y deja el texto en el estado para mostrarlo. */
    fun summarizeConversation() {
        if (!_state.value.aiAvailable) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isSummarizing = true, conversationSummary = null)
            val result = aiAssistService.summarize(currentTranscript(maxMessages = 40))
            _state.value = _state.value.copy(
                isSummarizing = false,
                conversationSummary = result.getOrElse { e ->
                    "No se pudo resumir: ${e.message ?: tr("error del proveedor de IA")}"
                }
            )
        }
    }

    fun dismissSummary() {
        _state.value = _state.value.copy(conversationSummary = null)
    }

    /**
     * Reescribe [draft] en otro tono y devuelve el resultado por [onResult] para
     * que la pantalla lo ponga en el campo de texto.
     *
     * El texto NO se envía: reescribir es una sugerencia, y decidir si se manda
     * sigue siendo del usuario.
     */
    fun rewriteDraftTone(
        draft: String,
        tone: com.Azelmods.App.data.ai.MessageTone,
        onResult: (String) -> Unit
    ) {
        if (!_state.value.aiAvailable || draft.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isRewritingTone = true)
            val result = aiAssistService.rewriteTone(draft, tone)
            _state.value = _state.value.copy(
                isRewritingTone = false,
                error = result.exceptionOrNull()?.let { "No se pudo reescribir: ${it.message}" }
            )
            result.getOrNull()?.let { rewritten ->
                withContext(Dispatchers.Main) { onResult(rewritten) }
            }
        }
    }

    /**
     * Traduce automáticamente los mensajes entrantes que aún no estén traducidos.
     *
     * Se limita a los últimos [AUTO_TRANSLATE_WINDOW] para no disparar una
     * ráfaga de peticiones al abrir un chat con años de historial.
     */
    private fun autoTranslateIncoming() {
        val state = _state.value
        if (!state.autoTranslateEnabled) return
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val pending = state.messages
            .takeLast(AUTO_TRANSLATE_WINDOW)
            .filter { it.senderId != me }
            .filter { it.content.isNotBlank() && !it.deletedForEveryone }
            .filterNot { it.messageId in state.translatedMessages }
            .filterNot { it.messageId in state.translatingMessageIds }

        pending.forEach { message -> translateMessage(message.messageId, message.content) }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BÚSQUEDA DENTRO DE LA CONVERSACIÓN
    //
    //  La entrada "Search" del menú existía desde hacía versiones con un
    //  `onClick` que solo cerraba el menú. Busca sobre los mensajes ya
    //  cargados; para ir más atrás, el propio scroll pagina y amplía el
    //  conjunto sobre el que se busca.
    // ═══════════════════════════════════════════════════════════════════════

    fun openSearch() {
        _state.value = _state.value.copy(isSearchOpen = true)
    }

    fun closeSearch() {
        _state.value = _state.value.copy(
            isSearchOpen = false,
            searchQuery = "",
            searchResults = emptyList(),
            searchIndex = 0
        )
    }

    fun onSearchQueryChange(query: String) {
        val state = _state.value
        if (query.isBlank()) {
            _state.value = state.copy(searchQuery = query, searchResults = emptyList(), searchIndex = 0)
            return
        }
        // Se busca también sobre la traducción visible: si el usuario está
        // leyendo un mensaje traducido, es ese texto el que recuerda.
        val resultados = state.messages
            .asReversed()
            .filter { message ->
                !message.deletedForEveryone && (
                    message.content.contains(query, ignoreCase = true) ||
                        state.translatedMessages[message.messageId]?.contains(query, ignoreCase = true) == true
                    )
            }
            .map { it.messageId }

        _state.value = state.copy(
            searchQuery = query,
            searchResults = resultados,
            searchIndex = 0
        )
    }

    /** Salta al siguiente resultado, dando la vuelta al llegar al final. */
    fun nextSearchResult() {
        val state = _state.value
        if (state.searchResults.isEmpty()) return
        _state.value = state.copy(searchIndex = (state.searchIndex + 1) % state.searchResults.size)
    }

    fun previousSearchResult() {
        val state = _state.value
        if (state.searchResults.isEmpty()) return
        val nuevo = if (state.searchIndex - 1 < 0) state.searchResults.lastIndex else state.searchIndex - 1
        _state.value = state.copy(searchIndex = nuevo)
    }

    /** Id del mensaje que hay que resaltar y hacia el que desplazarse. */
    fun currentSearchMessageId(): String? =
        _state.value.searchResults.getOrNull(_state.value.searchIndex)

    // ═══════════════════════════════════════════════════════════════════════
    //  SILENCIO DE ESTA CONVERSACIÓN
    // ═══════════════════════════════════════════════════════════════════════

    /** Lee el ajuste de silencio del chat abierto. */
    private fun loadMuteState(chatId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val settings = databaseRepository.getChatSettings(userId, chatId)
            _state.value = _state.value.copy(isMuted = settings.isCurrentlyMuted())
        }
    }

    /**
     * Entrega el aviso al llamador SIEMPRE en el hilo principal.
     *
     * Mismo motivo que en `HomeViewModel`: estas funciones trabajan en
     * [Dispatchers.IO] y la pantalla responde al callback con un `Toast`.
     * Mostrar un Toast fuera del hilo principal lanza
     * `RuntimeException: Can't toast on a thread that has not called
     * Looper.prepare()`. En Home ya se corrigió, pero aquí se quedó sin
     * alinear y por eso "Vaciar chat" cerraba la app desde dentro de la
     * conversación (desde la lista de chats no, porque ese camino es el de Home).
     */
    private suspend fun reportarEnMain(mensaje: String, onResult: (String) -> Unit) {
        withContext(Dispatchers.Main) { onResult(mensaje) }
    }

    /**
     * Silencia o reactiva la conversación abierta.
     *
     * @param durationMs cuánto dura el silencio si se está activando.
     */
    fun toggleMute(
        durationMs: Long = com.Azelmods.App.data.model.ChatSettings.MUTE_ALWAYS,
        onResult: (String) -> Unit = {}
    ) {
        val chatId = currentChatId
        if (chatId.isBlank()) return
        val silenciadoAhora = _state.value.isMuted
        viewModelScope.launch(Dispatchers.IO) {
            val muteUntil = when {
                silenciadoAhora -> null
                durationMs == com.Azelmods.App.data.model.ChatSettings.MUTE_ALWAYS ->
                    com.Azelmods.App.data.model.ChatSettings.MUTE_ALWAYS
                else -> System.currentTimeMillis() + durationMs
            }
            runCatching { databaseRepository.setChatMuted(chatId, muteUntil) }
                .onSuccess {
                    _state.value = _state.value.copy(isMuted = muteUntil != null)
                    reportarEnMain(
                        if (muteUntil == null) tr("Notificaciones reactivadas") else tr("Chat silenciado"),
                        onResult
                    )
                }
                .onFailure { reportarEnMain("No se pudo silenciar: ${it.message ?: "error"}", onResult) }
        }
    }

    /**
     * Vacía la conversación abierta: borra los mensajes en el servidor y en el
     * caché local.
     *
     * La entrada "Clear Chat" del menú del chat existía desde hacía versiones
     * pero su `onClick` sólo cerraba el menú, así que pulsarla no borraba nada.
     */
    fun clearChat(onResult: (String) -> Unit = {}) {
        val chatId = currentChatId
        if (chatId.isBlank()) {
            onResult("No hay conversación abierta")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                databaseRepository.clearChatMessages(chatId)
                cacheManager.clearChatMessages(chatId)
            }.onSuccess {
                olderMessages = emptyList()
                lastLiveWindow = emptyList()
                _state.value = _state.value.copy(
                    messages = emptyList(),
                    hasMoreMessages = false,
                    earliestMessageTimestamp = 0L,
                    earliestMessageId = ""
                )
                reportarEnMain("Chat vaciado", onResult)
            }.onFailure { e ->
                android.util.Log.e("ChatViewModel", "No se pudo vaciar el chat $chatId", e)
                reportarEnMain("No se pudo vaciar: ${e.message ?: tr("error desconocido")}", onResult)
            }
        }
    }

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
                
                val original = _state.value.messages.firstOrNull { it.messageId == messageId }
                databaseRepository.editMessage(
                    chatId = currentChatId,
                    messageId = messageId,
                    newContent = newContent,
                    wasEncrypted = original?.isEncrypted == true
                )

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
        val chatId = currentChatId
        // El freno va aquí, antes de lanzar nada: comprobar un `isLoadingMore` que se activa
        // dentro de la corrutina dejaba pasar dos cargas seguidas de la misma página, y sus
        // mensajes acababan repetidos en la lista (crash "Key was already used").
        if (loadMoreJob?.isActive == true || !state.hasMoreMessages ||
            state.earliestMessageTimestamp <= 0L || chatId.isBlank()
        ) return

        _state.update { it.copy(isLoadingMore = true) }
        loadMoreJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                val page = databaseRepository.loadMoreMessages(
                    chatId = chatId,
                    beforeTimestamp = state.earliestMessageTimestamp,
                    limit = 30
                )

                if (page.isEmpty()) {
                    _state.update { it.copy(isLoadingMore = false, hasMoreMessages = false) }
                    return@launch
                }

                // Se calcula sobre los datos crudos: una página entera de mensajes borrados
                // para mí también tiene que hacer avanzar la paginación.
                val oldestInPage = page.minOf {
                    (it["timestamp"] as? Number)?.toLong() ?: state.earliestMessageTimestamp
                }
                val hasMore = databaseRepository.hasMoreMessages(
                    chatId = chatId,
                    beforeTimestamp = oldestInPage
                )

                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                val peerId = peerIdOf(chatId, currentUserId)
                val now = databaseRepository.serverNow()
                val newMessages = page.map { data -> mapMessage(data, chatId, peerId) }
                    .filter { message -> message.deletedFor[currentUserId] != true }
                    .filterNot { it.expiresAt in 1..now }

                olderMessages = (newMessages + olderMessages).distinctBy { it.listKey() }
                val merged = mergeMessages(lastLiveWindow, olderMessages)

                _state.update { current ->
                    current.copy(
                        messages = merged,
                        earliestMessageTimestamp = oldestInPage,
                        earliestMessageId = merged.firstOrNull()?.messageId.orEmpty(),
                        isLoadingMore = false,
                        hasMoreMessages = hasMore
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                _state.update { it.copy(isLoadingMore = false) }
                throw e
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        error = "Error al cargar más mensajes: ${e.message}"
                    )
                }
            }
        }
    }
}
