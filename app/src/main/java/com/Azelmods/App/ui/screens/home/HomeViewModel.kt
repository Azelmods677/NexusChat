package com.Azelmods.App.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.local.CacheManager
import com.Azelmods.App.data.local.toChat
import com.Azelmods.App.data.manager.AppBackgroundManager
import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.data.model.ChatType
import com.Azelmods.App.data.model.User
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

// ── State ─────────────────────────────────────────────────────────────────────

data class HomeState(
    val chats: List<Chat> = emptyList(),
    val filteredChats: List<Chat> = emptyList(),
    val searchQuery: String = "",
    val selectedFilter: ChatFilter = ChatFilter.ALL,
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Global uid → displayName lookup aggregated across all loaded chats. */
    val participantNames: Map<String, String> = emptyMap(),
    /** Global uid → photoUrl lookup aggregated across all loaded chats. */
    val participantPhotos: Map<String, String> = emptyMap()
)

enum class ChatFilter { ALL, UNREAD, GROUPS, ARCHIVED }

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val databaseRepository: RealtimeDatabaseRepository,
    private val cacheManager: CacheManager,
    private val backgroundManager: AppBackgroundManager
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    val backgroundConfig = backgroundManager.backgroundConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        loadChats()
        // Mark the current user as online — best-effort, failures are swallowed.
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { databaseRepository.updatePresence(isOnline = true) }
        }
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Pull-to-refresh or manual retry entry point. */
    fun refreshChats() = loadChats()

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(
            searchQuery = query,
            filteredChats = filterChats(
                chats = _state.value.chats,
                query = query,
                filter = _state.value.selectedFilter
            )
        )
    }

    fun onFilterChange(filter: ChatFilter) {
        _state.value = _state.value.copy(
            selectedFilter = filter,
            filteredChats = filterChats(
                chats = _state.value.chats,
                query = _state.value.searchQuery,
                filter = filter
            )
        )
    }

    fun togglePin(chatId: String) {
        val chat = _state.value.chats.find { it.chatId == chatId } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                databaseRepository.setChatBooleanField(chatId, "isPinned", !chat.isPinned)
            }
        }
    }

    fun toggleMute(chatId: String) {
        val chat = _state.value.chats.find { it.chatId == chatId } ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                databaseRepository.setChatBooleanField(chatId, "isMuted", !chat.isMuted)
            }
        }
    }

    fun archiveChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                databaseRepository.setChatBooleanField(chatId, "isArchived", true)
            }
        }
    }

    fun deleteChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                databaseRepository.deleteChat(chatId)
            }
        }
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    private fun loadChats() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                android.util.Log.e("HomeVM", "❌ User not authenticated")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "User not authenticated"
                )
                return@launch
            }
            
            android.util.Log.d("HomeVM", "✅ Loading chats for user: $userId")

            try {
                // ── OFFLINE CACHE: show cached chats immediately ──
                val cachedEntities = cacheManager.getCachedChats()
                if (cachedEntities.isNotEmpty()) {
                    val cachedChats = cachedEntities.map { it.toChat() }
                    android.util.Log.d("HomeVM", "📦 Loaded ${cachedChats.size} cached chats")
                    // Aggregate participant info from cache
                    val allNames = mutableMapOf<String, String>()
                    val allPhotos = mutableMapOf<String, String>()
                    cachedChats.forEach { chat ->
                        allNames.putAll(chat.participantNames)
                        allPhotos.putAll(chat.participantPhotos)
                    }
                    _state.value = _state.value.copy(
                        chats = cachedChats,
                        filteredChats = filterChats(
                            chats = cachedChats,
                            query = _state.value.searchQuery,
                            filter = _state.value.selectedFilter
                        ),
                        participantNames = allNames,
                        participantPhotos = allPhotos,
                        isLoading = false
                    )
                } else {
                    android.util.Log.d("HomeVM", "📭 No cached chats found")
                }

                databaseRepository.getUserChats(userId).collect { rawChats ->
                    android.util.Log.d("HomeVM", "🔄 Received ${rawChats.size} chats from Firebase")
                    
                    if (rawChats.isEmpty()) {
                        android.util.Log.w("HomeVM", "⚠️ No chats found in Firebase for user $userId")
                        _state.value = _state.value.copy(
                            chats = emptyList(),
                            filteredChats = emptyList(),
                            isLoading = false,
                            error = null // No es error, simplemente no hay chats
                        )
                        return@collect
                    }
                    
                    // All Firebase user-profile look-ups run on the IO dispatcher.
                    val enriched: List<Chat> = withContext(Dispatchers.IO) {
                        rawChats.mapNotNull { data ->
                            runCatching { 
                                val chat = buildChatFromMap(data, userId)
                                android.util.Log.d("HomeVM", "✅ Built chat: ${chat.chatId}, type: ${chat.chatType}, participants: ${chat.participants.size}")
                                chat
                            }.getOrElse { e ->
                                android.util.Log.e("HomeVM", "❌ Failed to build chat: ${e.message}", e)
                                null
                            }
                        }
                    }
                    
                    android.util.Log.d("HomeVM", "📊 Successfully enriched ${enriched.size} chats")

                    // Aggregate a single uid → name / photo map for the whole screen.
                    val allNames = mutableMapOf<String, String>()
                    val allPhotos = mutableMapOf<String, String>()
                    enriched.forEach { chat ->
                        allNames.putAll(chat.participantNames)
                        allPhotos.putAll(chat.participantPhotos)
                    }

                    // ── SAVE CHATS TO ROOM CACHE ──
                    try {
                        cacheManager.cacheChats(enriched)
                        android.util.Log.d("HomeVM", "💾 Cached ${enriched.size} chats successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("HomeVM", "❌ Failed to cache chats: ${e.message}", e)
                    }

                    // ── CACHE USER PROFILES too ──
                    try {
                        enriched.forEach { chat ->
                            chat.participantNames.entries.forEach { (uid, name) ->
                                val photoUrl = chat.participantPhotos[uid]
                                cacheManager.cacheUser(
                                    User(uid = uid, name = name, displayName = name, photoUrl = photoUrl)
                                )
                            }
                        }
                        android.util.Log.d("HomeVM", "💾 Cached user profiles successfully")
                    } catch (e: Exception) {
                        android.util.Log.e("HomeVM", "❌ Failed to cache user profiles: ${e.message}", e)
                    }

                    _state.value = _state.value.copy(
                        chats = enriched,
                        filteredChats = filterChats(
                            chats = enriched,
                            query = _state.value.searchQuery,
                            filter = _state.value.selectedFilter
                        ),
                        participantNames = allNames,
                        participantPhotos = allPhotos,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "❌ Critical error loading chats: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load chats"
                )
            }
        }
    }

    /**
     * Constructs an enriched [Chat] from a raw Firebase map.
     *
     * The "other" participant (the one that is NOT [currentUserId]) is fetched
     * via [RealtimeDatabaseRepository.getUserById] and their display-name /
     * photo URL are injected into the [Chat] object's participant maps.
     *
     * For group chats the first non-self member is used as the preview contact.
     */
    @Suppress("UNCHECKED_CAST")
    private suspend fun buildChatFromMap(
        data: Map<String, Any>,
        currentUserId: String
    ): Chat {
        val chatId = data["chatId"] as? String ?: ""
        android.util.Log.d("HomeVM", "🔨 Building chat: $chatId")

        val members: List<String> = when {
            data["members"] is List<*> -> (data["members"] as List<*>).filterIsInstance<String>()
            data["members"] is Map<*, *> -> (data["members"] as Map<*, *>).keys.filterIsInstance<String>()
            data["participants"] is List<*> -> (data["participants"] as List<*>).filterIsInstance<String>()
            else -> emptyList()
        }
        
        android.util.Log.d("HomeVM", "👥 Chat $chatId has ${members.size} members: $members")

        val isPinned = data["isPinned"] as? Boolean ?: false
        val isMuted = data["isMuted"] as? Boolean ?: false
        val isArchived = data["isArchived"] as? Boolean ?: false

        val chatType = if ((data["type"] as? String) == "group" || 
                           (data["isGroup"] as? Boolean) == true ||
                           chatId.startsWith("group_")) {
            android.util.Log.d("HomeVM", "📁 Chat $chatId is GROUP")
            ChatType.GROUP
        } else {
            android.util.Log.d("HomeVM", "💬 Chat $chatId is PRIVATE")
            ChatType.PRIVATE
        }

        // Pick the first member that is not the current user as the "other" side.
        val otherUid = members.firstOrNull { it != currentUserId } ?: ""
        android.util.Log.d("HomeVM", "👤 Other user ID: $otherUid")

        val otherUserData: Map<String, Any>? = if (otherUid.isNotBlank()) {
            try {
                databaseRepository.getUserById(otherUid).also {
                    android.util.Log.d("HomeVM", "✅ Fetched user data for $otherUid: ${it?.get("displayName")}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeVM", "❌ Failed to fetch user $otherUid: ${e.message}")
                null
            }
        } else {
            android.util.Log.w("HomeVM", "⚠️ No other user found for chat $chatId")
            null
        }

        // Real online presence of the other participant (snapshot; refreshes on each
        // getUserChats emission). Never hardcoded.
        val otherIsOnline = otherUserData?.get("isOnline") as? Boolean ?: false

        // Real per-user unread counters from the chat node, if present.
        val unreadMap: Map<String, Int> = when (val raw = data["unreadCount"]) {
            is Map<*, *> -> raw.entries.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val count = (v as? Number)?.toInt() ?: return@mapNotNull null
                key to count
            }.toMap()
            else -> emptyMap()
        }

        // Real typing state from the chat node, if present.
        val typingMap: Map<String, Boolean> = when (val raw = data["isTyping"]) {
            is Map<*, *> -> raw.entries.mapNotNull { (k, v) ->
                val key = k as? String ?: return@mapNotNull null
                val typing = v as? Boolean ?: return@mapNotNull null
                key to typing
            }.toMap()
            else -> emptyMap()
        }

        // For groups, use groupName if available
        val displayName = if (chatType == ChatType.GROUP) {
            (data["groupName"] as? String)?.takeIf { it.isNotBlank() }
                ?: "Grupo ${members.size} miembros"
        } else {
            // Prefer displayName, fall back to username, then "Anónimo".
            (otherUserData?.get("displayName") as? String)
                ?.takeIf { it.isNotBlank() }
                ?: (otherUserData?.get("username") as? String)
                    ?.takeIf { it.isNotBlank() }
                ?: "Anónimo"
        }

        // Prefer photoUrl, fall back to profilePhotoUrl.
        val photoUrl = (otherUserData?.get("photoUrl") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: (otherUserData?.get("profilePhotoUrl") as? String)
                ?.takeIf { it.isNotBlank() }
            ?: ""

        val participantNames = if (otherUid.isNotBlank()) mapOf(otherUid to displayName) else emptyMap()
        val participantPhotos = if (otherUid.isNotBlank() && photoUrl.isNotBlank()) {
            mapOf(otherUid to photoUrl)
        } else emptyMap()

        // Firebase returns numeric fields as Long, Int, or Double — normalise to Long.
        val lastMessageTime: Long = when (val t = data["lastMessageTime"]) {
            is Long -> t
            is Number -> t.toLong()
            else -> 0L
        }

        return Chat(
            chatId = chatId,
            participants = members,
            participantIds = members,
            participantNames = participantNames,
            participantPhotos = participantPhotos,
            lastMessage = data["lastMessage"] as? String ?: "",
            lastMessageTime = lastMessageTime,
            lastMessageSenderId = data["lastMessageSenderId"] as? String ?: "",
            unreadCount = unreadMap,
            isTyping = typingMap,
            chatType = chatType,
            contactName = displayName,
            contactPhotoUrl = photoUrl.takeIf { it.isNotBlank() },
            isOnline = otherIsOnline,
            isPinned = isPinned,
            isMuted = isMuted,
            isArchived = isArchived,
            isE2EE = data["isE2EE"] as? Boolean ?: true
        ).also {
            android.util.Log.d("HomeVM", "✅ Successfully built chat: ${it.chatId}, name: ${it.contactName}")
        }
    }

    private fun filterChats(
        chats: List<Chat>,
        query: String,
        filter: ChatFilter
    ): List<Chat> {
        var result = when (filter) {
            ChatFilter.ARCHIVED -> chats.filter { it.isArchived }
            else -> chats.filter { !it.isArchived }
        }

        // Text search — match against participant display names or last message.
        if (query.isNotBlank()) {
            result = result.filter { chat ->
                chat.participantNames.values.any { name ->
                    name.contains(query, ignoreCase = true)
                } || chat.lastMessage.contains(query, ignoreCase = true)
            }
        }

        result = when (filter) {
            ChatFilter.ALL, ChatFilter.ARCHIVED -> result
            ChatFilter.UNREAD -> {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                result.filter { (it.unreadCount[uid] ?: 0) > 0 }
            }
            ChatFilter.GROUPS -> result.filter { it.chatType == ChatType.GROUP }
        }

        return result.sortedWith(
            compareByDescending<Chat> { it.isPinned }
                .thenByDescending { it.lastMessageTime }
        )
    }

    override fun onCleared() {
        super.onCleared()
        // Mark the user offline when the ViewModel is torn down — best-effort.
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { databaseRepository.updatePresence(isOnline = false) }
        }
    }
}
