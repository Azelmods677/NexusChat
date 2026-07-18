package com.Azelmods.App.ui.screens.stories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StoryViewerState(
    val stories: List<StoryItemData> = emptyList(),
    val currentIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    val replySent: Boolean = false,
    val viewers: List<Map<String, Any>> = emptyList(),
    val deleted: Boolean = false,
    /** Emoji recién enviado (feedback en snackbar); null cuando no hay pendiente. */
    val reactionSent: String? = null,
    /** Reacciones propias de esta sesión: storyId → emoji (resalta el elegido). */
    val myReactions: Map<String, String> = emptyMap(),
    /** Reacciones de la historia propia abierta en el sheet: uid → emoji. */
    val reactions: Map<String, String> = emptyMap()
)

@HiltViewModel
class StoryViewerViewModel @Inject constructor(
    private val repository: RealtimeDatabaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StoryViewerState())
    val state: StateFlow<StoryViewerState> = _state.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    // ── Story loading ────────────────────────────────────────────────────────

    fun loadStoriesForUser(userId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid ?: run {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Not authenticated"
                    )
                    return@launch
                }

                repository.getStoriesForUser(userId).collect { storiesData ->
                    val userStories = mutableListOf<StoryItemData>()

                    for (storyMap in storiesData) {
                        val storyUserId = storyMap["userId"] as? String ?: userId

                        // Resolve user display info
                        val userData = runCatching { repository.getUserById(storyUserId) }.getOrNull()
                        val userName = userData?.get("displayName") as? String
                            ?: userData?.get("username") as? String
                            ?: userData?.get("name") as? String
                            ?: storyUserId
                        val userPhotoUrl = userData?.get("photoUrl") as? String

                        // Normalise timestamp fields that Firebase may return as Long or Double
                        val timestamp = when (val ts = storyMap["timestamp"]) {
                            is Long -> ts
                            is Double -> ts.toLong()
                            is String -> ts.toLongOrNull() ?: 0L
                            else -> 0L
                        }
                        val expiresAt = when (val e = storyMap["expiresAt"]) {
                            is Long -> e
                            is Double -> e.toLong()
                            is String -> e.toLongOrNull() ?: 0L
                            else -> 0L
                        }

                        // Vistas REALES desde stories_views/{storyId}: alimenta el
                        // conteo "👁 N" del dueño y el estado visto/no visto. Antes se
                        // leían de storyMap["views"] (nodo que nunca se llena) → 0/false.
                        val storyId = storyMap["storyId"] as? String ?: ""
                        val viewerIds: List<String> = runCatching {
                            repository.getStoryViewerIds(storyId)
                        }.getOrNull() ?: emptyList()

                        userStories.add(
                            StoryItemData(
                                storyId = storyId,
                                userId = storyUserId,
                                userName = userName,
                                userPhotoUrl = userPhotoUrl,
                                type = storyMap["type"] as? String ?: storyMap["mediaType"] as? String ?: "IMAGE",
                                mediaUrl = storyMap["mediaUrl"] as? String,
                                text = storyMap["text"] as? String,
                                caption = storyMap["caption"] as? String,
                                backgroundColor = storyMap["backgroundColor"] as? String,
                                timestamp = timestamp,
                                expiresAt = expiresAt,
                                views = viewerIds,
                                isViewed = viewerIds.contains(currentUserId)
                            )
                        )
                    }

                    val sorted = userStories.sortedBy { it.timestamp }

                    _state.value = _state.value.copy(
                        stories = sorted,
                        isLoading = false,
                        error = null
                    )

                    // Fire-and-forget: mark every unviewed story as viewed
                    sorted.forEach { story ->
                        if (!story.isViewed) {
                            launch(Dispatchers.IO) {
                                runCatching { repository.markStoryAsViewed(story.storyId) }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("StoryViewerVM", "Error cargando stories de $userId: ${e.message}", e)
                val msg = e.message ?: ""
                val friendly = if (msg.contains("Permission denied", ignoreCase = true) ||
                    msg.contains("permission_denied", ignoreCase = true)
                ) {
                    "No tienes permiso para ver estas historias. Revisa las reglas de Firebase o vuelve a intentarlo."
                } else {
                    msg.ifBlank { "No se pudieron cargar las historias" }
                }
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = friendly
                )
            }
        }
    }

    // ── Navigation ───────────────────────────────────────────────────────────

    fun nextStory() {
        val s = _state.value
        if (s.currentIndex < s.stories.size - 1) {
            _state.value = s.copy(currentIndex = s.currentIndex + 1)
        }
    }

    fun previousStory() {
        val s = _state.value
        if (s.currentIndex > 0) {
            _state.value = s.copy(currentIndex = s.currentIndex - 1)
        }
    }

    // ── Reply ────────────────────────────────────────────────────────────────

    fun sendReply(storyOwnerId: String, storyId: String, text: String) {
        if (text.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.sendStoryReply(storyOwnerId, storyId, text) }
                .onSuccess { _state.value = _state.value.copy(replySent = true) }
                .onFailure { _state.value = _state.value.copy(error = it.message) }
        }
    }

    fun clearReplySent() {
        _state.value = _state.value.copy(replySent = false)
    }

    // ── Reactions ────────────────────────────────────────────────────────────

    fun addReaction(storyOwnerId: String, storyId: String, emoji: String) {
        if (emoji.isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.addStoryReaction(storyOwnerId, storyId, emoji) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        reactionSent = emoji,
                        myReactions = _state.value.myReactions + (storyId to emoji)
                    )
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        error = "No se pudo enviar la reacción: ${it.message}"
                    )
                }
        }
    }

    fun clearReactionSent() {
        _state.value = _state.value.copy(reactionSent = null)
    }

    // ── Viewers ──────────────────────────────────────────────────────────────

    fun loadViewers(storyId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { repository.getStoryViewers(storyId) }
                .onSuccess { viewers ->
                    _state.value = _state.value.copy(viewers = viewers)
                }
                .onFailure { e ->
                    android.util.Log.e("StoryViewerVM", "Failed to load viewers: ${e.message}", e)
                }
            // El sheet de viewers solo se abre sobre la historia propia, por lo
            // que el dueño de las reacciones es siempre el usuario autenticado.
            val ownerId = auth.currentUser?.uid ?: return@launch
            runCatching { repository.getStoryReactions(ownerId, storyId) }
                .onSuccess { reactions ->
                    _state.value = _state.value.copy(reactions = reactions)
                }
                .onFailure { e ->
                    android.util.Log.e("StoryViewerVM", "Failed to load reactions: ${e.message}", e)
                }
        }
    }

    // ── Delete (own story) ─────────────────────────────────────────────────────

    /**
     * Elimina la historia indicada (solo si es del usuario actual; el repo lo
     * valida y las reglas también). Actualiza la lista local de inmediato y
     * marca `deleted = true` para que la pantalla cierre el visor.
     */
    fun deleteStory(story: StoryItemData) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                repository.deleteStory(story.storyId, story.userId, story.mediaUrl)
            }.onSuccess {
                val remaining = _state.value.stories.filterNot { it.storyId == story.storyId }
                val newIndex = _state.value.currentIndex
                    .coerceIn(0, (remaining.size - 1).coerceAtLeast(0))
                _state.value = _state.value.copy(
                    stories = remaining,
                    currentIndex = newIndex,
                    deleted = true
                )
            }.onFailure {
                _state.value = _state.value.copy(
                    error = it.message ?: "No se pudo eliminar la historia"
                )
            }
        }
    }

    fun clearDeleted() {
        _state.value = _state.value.copy(deleted = false)
    }
}
