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
    val viewers: List<Map<String, Any>> = emptyList()
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

                repository.getAllStories().collect { storiesData ->
                    val userStories = mutableListOf<StoryItemData>()

                    for (storyMap in storiesData) {
                        val storyUserId = storyMap["userId"] as? String ?: continue
                        if (storyUserId != userId) continue

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

                        // Views can be a Map<uid, true> or a plain List
                        val viewsList: List<String> = when (val v = storyMap["views"]) {
                            is Map<*, *> -> v.keys.mapNotNull { it as? String }
                            is List<*> -> v.mapNotNull { it as? String }
                            else -> emptyList()
                        }

                        userStories.add(
                            StoryItemData(
                                storyId = storyMap["storyId"] as? String ?: "",
                                userId = storyUserId,
                                userName = userName,
                                userPhotoUrl = userPhotoUrl,
                                type = storyMap["type"] as? String ?: "IMAGE",
                                mediaUrl = storyMap["mediaUrl"] as? String,
                                text = storyMap["text"] as? String,
                                caption = storyMap["caption"] as? String,
                                backgroundColor = storyMap["backgroundColor"] as? String,
                                timestamp = timestamp,
                                expiresAt = expiresAt,
                                views = viewsList,
                                isViewed = viewsList.contains(currentUserId)
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
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
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
        }
    }
}
