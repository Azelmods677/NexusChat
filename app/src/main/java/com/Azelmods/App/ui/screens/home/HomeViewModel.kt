package com.Azelmods.App.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.data.model.ChatType
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

enum class ChatFilter { ALL, UNREAD, GROUPS }

// ── ViewModel ─────────────────────────────────────────────────────────────────

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val databaseRepository: RealtimeDatabaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

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

    // ── Action stubs ──────────────────────────────────────────────────────────

    fun togglePin(chatId: String) { /* TODO: persist pin state in DataStore   */
    }

    fun toggleMute(chatId: String) { /* TODO: persist mute state in DataStore  */
    }

    fun archiveChat(chatId: String) { /* TODO: mark chat as archived in Firebase */
    }

    fun deleteChat(chatId: String) { /* TODO: delete chat node from Firebase    */
    }

    // ── Core logic ────────────────────────────────────────────────────────────

    private fun loadChats() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            val userId = FirebaseAuth.getInstance().currentUser?.uid
            if (userId == null) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "User not authenticated"
                )
                return@launch
            }

            try {
                databaseRepository.getUserChats(userId).collect { rawChats ->
                    // All Firebase user-profile look-ups run on the IO dispatcher.
                    val enriched: List<Chat> = withContext(Dispatchers.IO) {
                        rawChats.mapNotNull { data ->
                            runCatching { buildChatFromMap(data, userId) }.getOrNull()
                        }
                    }

                    // Aggregate a single uid → name / photo map for the whole screen.
                    val allNames = mutableMapOf<String, String>()
                    val allPhotos = mutableMapOf<String, String>()
                    enriched.forEach { chat ->
                        allNames.putAll(chat.participantNames)
                        allPhotos.putAll(chat.participantPhotos)
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

        // Firebase may store members as  List<String>  or  Map<uid, Boolean>.
        val members: List<String> = when (val m = data["members"]) {
            is List<*> -> m.filterIsInstance<String>()
            is Map<*, *> -> m.keys.filterIsInstance<String>()
            else -> emptyList()
        }

        val chatType = if ((data["type"] as? String) == "group") {
            ChatType.GROUP
        } else {
            ChatType.PRIVATE
        }

        // Pick the first member that is not the current user as the "other" side.
        val otherUid = members.firstOrNull { it != currentUserId } ?: ""

        val otherUserData: Map<String, Any>? = if (otherUid.isNotBlank()) {
            databaseRepository.getUserById(otherUid)
        } else null

        // Prefer displayName, fall back to username, then "Unknown".
        val displayName = (otherUserData?.get("displayName") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: (otherUserData?.get("username") as? String)
                ?.takeIf { it.isNotBlank() }
            ?: "Unknown"

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
            unreadCount = emptyMap(),
            isTyping = emptyMap(),
            chatType = chatType
        )
    }

    private fun filterChats(
        chats: List<Chat>,
        query: String,
        filter: ChatFilter
    ): List<Chat> {
        var result = chats

        // Text search — match against participant display names or last message.
        if (query.isNotBlank()) {
            result = result.filter { chat ->
                chat.participantNames.values.any { name ->
                    name.contains(query, ignoreCase = true)
                } || chat.lastMessage.contains(query, ignoreCase = true)
            }
        }

        // Category filter.
        result = when (filter) {
            ChatFilter.ALL -> result
            ChatFilter.UNREAD -> {
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                result.filter { (it.unreadCount[uid] ?: 0) > 0 }
            }

            ChatFilter.GROUPS -> result.filter { it.chatType == ChatType.GROUP }
        }

        return result
    }

    override fun onCleared() {
        super.onCleared()
        // Mark the user offline when the ViewModel is torn down — best-effort.
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { databaseRepository.updatePresence(isOnline = false) }
        }
    }
}
