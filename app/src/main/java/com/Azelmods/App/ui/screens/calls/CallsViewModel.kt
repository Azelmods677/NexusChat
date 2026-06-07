package com.Azelmods.App.ui.screens.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallHistoryItem(
    val callId: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String?,
    val callType: String,
    val status: String,
    val startTime: Long,
    val endTime: Long?,
    val duration: String?,
    val isIncoming: Boolean
)

data class CallsState(
    val calls: List<CallHistoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class CallsViewModel @Inject constructor(
    private val repository: RealtimeDatabaseRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CallsState())
    val state: StateFlow<CallsState> = _state.asStateFlow()

    private val auth = FirebaseAuth.getInstance()

    init {
        loadCallHistory()
    }

    private fun loadCallHistory() {
        viewModelScope.launch {
            try {
                val currentUserId = auth.currentUser?.uid
                if (currentUserId == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }

                repository.getUserCallHistory(currentUserId).collect { rawCalls ->
                    val items = rawCalls.mapNotNull { data ->
                        buildCallHistoryItem(data, currentUserId)
                    }
                    _state.value = _state.value.copy(
                        calls = items,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun buildCallHistoryItem(
        data: Map<String, Any>,
        currentUserId: String
    ): CallHistoryItem? {
        val callId = data["callId"] as? String ?: return null
        val callerId = data["callerId"] as? String ?: ""
        val receiverId = data["receiverId"] as? String ?: ""
        val otherUserId = if (callerId == currentUserId) receiverId else callerId
        if (otherUserId.isBlank()) return null

        val otherUser = repository.getUserById(otherUserId)
        val userName = (otherUser?.get("displayName") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: (otherUser?.get("username") as? String)
            ?: "Anónimo"
        val photoUrl = (otherUser?.get("photoUrl") as? String)
            ?.takeIf { it.isNotBlank() }
            ?: (otherUser?.get("profilePhotoUrl") as? String)

        val startTime = when (val t = data["startTime"]) {
            is Long -> t
            is Number -> t.toLong()
            else -> System.currentTimeMillis()
        }
        val endTime = when (val t = data["endTime"]) {
            is Long -> t
            is Number -> t.toLong()
            else -> null
        }
        val durationMs = if (endTime != null && endTime > startTime) endTime - startTime else 0L
        val duration = if (durationMs > 0) {
            val sec = durationMs / 1000
            val min = sec / 60
            val rem = sec % 60
            if (min > 0) "${min}m ${rem}s" else "${rem}s"
        } else null

        return CallHistoryItem(
            callId = callId,
            userId = otherUserId,
            userName = userName,
            userPhotoUrl = photoUrl,
            callType = data["callType"] as? String ?: "AUDIO",
            status = data["status"] as? String ?: "ENDED",
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            isIncoming = receiverId == currentUserId
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
