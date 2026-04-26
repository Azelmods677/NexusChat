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
    val callType: String, // "AUDIO" or "VIDEO"
    val status: String, // "CALLING", "ACCEPTED", "ENDED", "MISSED"
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
                
                // TODO: Implement call history loading from Firebase
                // For now, show empty list (no demo data)
                _state.value = _state.value.copy(
                    calls = emptyList(),
                    isLoading = false
                )
                
            } catch (e: Exception) {
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
