package com.Azelmods.App.ui.screens.stories

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreateStoryState(
    val isUploading: Boolean = false,
    val uploadSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class CreateStoryViewModel @Inject constructor(
    private val storageRepository: StorageRepository,
    private val databaseRepository: RealtimeDatabaseRepository,
    private val userRepository: com.Azelmods.App.data.repository.UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _state = MutableStateFlow(CreateStoryState())
    val state: StateFlow<CreateStoryState> = _state.asStateFlow()
    
    /**
     * Upload IMAGE story to Firebase Storage and save to Realtime Database
     * Pattern: stories/{userId}/{timestamp}.jpg
     */
    fun uploadImageStory(imageUri: Uri, caption: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, error = null, uploadSuccess = false)
            
            try {
                // Get userId at the exact moment of upload
                val userId = auth.currentUser?.uid
                if (userId.isNullOrBlank()) {
                    _state.value = _state.value.copy(
                        isUploading = false,
                        error = "Por favor inicia sesión nuevamente"
                    )
                    return@launch
                }
                
                // Step 1: Upload to Storage FIRST and AWAIT completion
                val imageUrl = storageRepository.uploadStory(imageUri, userId)
                
                // Step 1.5: Get user name
                val userResult = userRepository.getUserById(userId)
                val userName = if (userResult is com.Azelmods.App.util.Resource.Success) {
                    userResult.data?.name ?: "Unknown"
                } else {
                    "Unknown"
                }
                
                // Step 2: ONLY after upload succeeds, save to Realtime Database
                val storyData = mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "type" to "IMAGE",
                    "mediaUrl" to imageUrl,
                    "caption" to caption,
                    "timestamp" to System.currentTimeMillis(),
                    "expiresAt" to (System.currentTimeMillis() + (24 * 60 * 60 * 1000)),
                    "views" to emptyList<String>()
                )
                
                // Step 3: Await database write completion
                databaseRepository.createStory(storyData).collect { result ->
                    result.onSuccess {
                        _state.value = _state.value.copy(
                            isUploading = false,
                            uploadSuccess = true
                        )
                    }.onFailure { exception ->
                        _state.value = _state.value.copy(
                            isUploading = false,
                            error = "Error al guardar la historia: ${exception.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isUploading = false,
                    error = "Error al subir la historia: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Upload VIDEO story to Firebase Storage and save to Realtime Database
     * Pattern: stories/{userId}/{timestamp}.mp4
     */
    fun uploadVideoStory(videoUri: Uri, caption: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, error = null, uploadSuccess = false)
            
            try {
                val userId = auth.currentUser?.uid
                if (userId.isNullOrBlank()) {
                    _state.value = _state.value.copy(
                        isUploading = false,
                        error = "Por favor inicia sesión nuevamente"
                    )
                    return@launch
                }
                
                // Step 1: Upload video to Storage FIRST and AWAIT completion
                val videoUrl = storageRepository.uploadStoryVideo(videoUri, userId)
                
                // Step 1.5: Get user name
                val userResult = userRepository.getUserById(userId)
                val userName = if (userResult is com.Azelmods.App.util.Resource.Success) {
                    userResult.data?.name ?: "Unknown"
                } else {
                    "Unknown"
                }
                
                // Step 2: ONLY after upload succeeds, save to Realtime Database
                val storyData = mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "type" to "VIDEO",
                    "mediaUrl" to videoUrl,
                    "caption" to caption,
                    "timestamp" to System.currentTimeMillis(),
                    "expiresAt" to (System.currentTimeMillis() + (24 * 60 * 60 * 1000)),
                    "views" to emptyList<String>()
                )
                
                // Step 3: Await database write completion
                databaseRepository.createStory(storyData).collect { result ->
                    result.onSuccess {
                        _state.value = _state.value.copy(
                            isUploading = false,
                            uploadSuccess = true
                        )
                    }.onFailure { exception ->
                        _state.value = _state.value.copy(
                            isUploading = false,
                            error = "Error al guardar la historia: ${exception.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isUploading = false,
                    error = "Error al subir el video: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Create TEXT story (no upload needed, direct to database)
     */
    fun createTextStory(text: String, backgroundColor: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isUploading = true, error = null, uploadSuccess = false)
            
            try {
                val userId = auth.currentUser?.uid
                if (userId.isNullOrBlank()) {
                    _state.value = _state.value.copy(
                        isUploading = false,
                        error = "Por favor inicia sesión nuevamente"
                    )
                    return@launch
                }
                
                // TEXT stories skip Storage entirely
                // Get user name
                val userResult = userRepository.getUserById(userId)
                val userName = if (userResult is com.Azelmods.App.util.Resource.Success) {
                    userResult.data?.name ?: "Unknown"
                } else {
                    "Unknown"
                }
                
                val storyData = mapOf(
                    "userId" to userId,
                    "userName" to userName,
                    "type" to "TEXT",
                    "text" to text,
                    "backgroundColor" to backgroundColor,
                    "timestamp" to System.currentTimeMillis(),
                    "expiresAt" to (System.currentTimeMillis() + (24 * 60 * 60 * 1000)),
                    "views" to emptyList<String>()
                )
                
                databaseRepository.createStory(storyData).collect { result ->
                    result.onSuccess {
                        _state.value = _state.value.copy(
                            isUploading = false,
                            uploadSuccess = true
                        )
                    }.onFailure { exception ->
                        _state.value = _state.value.copy(
                            isUploading = false,
                            error = "Error al crear la historia: ${exception.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isUploading = false,
                    error = "Error al crear la historia: ${e.message}"
                )
            }
        }
    }
    
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun clearSuccess() {
        _state.value = _state.value.copy(uploadSuccess = false)
    }
}
