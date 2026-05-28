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

// ─────────────────────────────────────────────────────────────────────────────
// Data models  (shared across stories package — do NOT redefine elsewhere)
// ─────────────────────────────────────────────────────────────────────────────

data class StoryItemData(
    val storyId: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String?,
    val type: String,           // "TEXT" | "IMAGE" | "VIDEO"
    val mediaUrl: String?,
    val text: String?,
    val caption: String?,
    val backgroundColor: String?,
    val timestamp: Long,
    val expiresAt: Long,
    val views: List<String>,
    val isViewed: Boolean = false
)

data class StoriesState(
    val stories: List<StoryItemData> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val currentUserPhotoUrl: String? = null   // profile photo of the logged-in user
)

// ─────────────────────────────────────────────────────────────────────────────
// ViewModel
// ─────────────────────────────────────────────────────────────────────────────

@HiltViewModel
class StoriesViewModel @Inject constructor(
    private val repository: RealtimeDatabaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StoriesState())
    val state: StateFlow<StoriesState> = _state.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        loadStories()
    }

    private fun loadStories() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val currentUserId = auth.currentUser?.uid ?: run {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }

                // ── Load the current user's profile photo first ───────────────
                val currentUserPhotoUrl = runCatching {
                    repository.getUserById(currentUserId)?.get("photoUrl") as? String
                }.getOrNull()

                _state.value = _state.value.copy(currentUserPhotoUrl = currentUserPhotoUrl)

                // ── Stream all stories from Firebase ─────────────────────────
                repository.getAllStories().collect { storiesData ->
                    val allStories = mutableListOf<StoryItemData>()

                    for (storyMap in storiesData) {
                        try {
                            val userId = storyMap["userId"] as? String
                            if (userId.isNullOrBlank()) continue

                            // Resolve display name + photo for each story author
                            val userData = runCatching {
                                repository.getUserById(userId)
                            }.getOrNull()

                            val userName = userData?.get("displayName") as? String
                                ?: userData?.get("username") as? String
                                ?: userData?.get("name") as? String
                                ?: userId

                            val userPhotoUrl = userData?.get("photoUrl") as? String

                            // Normalise timestamp fields (Firebase may return Long or Double)
                            val timestamp = when (val ts = storyMap["timestamp"]) {
                                is Long -> ts
                                is Double -> ts.toLong()
                                is String -> ts.toLongOrNull() ?: 0L
                                else -> 0L
                            }

                            val expiresAt = when (val exp = storyMap["expiresAt"]) {
                                is Long -> exp
                                is Double -> exp.toLong()
                                is String -> exp.toLongOrNull() ?: 0L
                                else -> 0L
                            }

                            // Views can be a Map<uid, true> or a plain List
                            val viewsList: List<String> = when (val v = storyMap["views"]) {
                                is Map<*, *> -> v.keys.mapNotNull { it as? String }
                                is List<*> -> v.mapNotNull { it as? String }
                                else -> emptyList()
                            }

                            allStories.add(
                                StoryItemData(
                                    storyId = storyMap["storyId"] as? String ?: "",
                                    userId = userId,
                                    userName = userName,
                                    userPhotoUrl = userPhotoUrl,
                                    type = storyMap["type"] as? String ?: storyMap["mediaType"] as? String ?: "IMAGE",
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
                        } catch (e: Exception) {
                            android.util.Log.e(
                                "StoriesViewModel",
                                "Error processing story: ${e.message}",
                                e
                            )
                        }
                    }

                    // Group by user (WhatsApp / Telegram style) — one bubble per user.
                    // The representative story is the most recent; the ring is coloured
                    // only when at least one story from that user is still unread.
                    val groupedStories = allStories
                        .groupBy { it.userId }
                        .map { (_, userStories) ->
                            val representative = userStories.maxBy { it.timestamp }
                            val hasUnviewed = userStories.any { !it.isViewed }
                            representative.copy(isViewed = !hasUnviewed)
                        }
                        .sortedByDescending { it.timestamp }

                    _state.value = _state.value.copy(
                        stories = groupedStories,
                        isLoading = false,
                        error = null
                    )
                }

            } catch (e: Exception) {
                android.util.Log.e("StoriesViewModel", "Error loading stories: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
