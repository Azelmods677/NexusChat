package com.Azelmods.App.ui.screens.profile
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

data class EditProfileState(
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val databaseRepository: RealtimeDatabaseRepository,
    private val storageRepository: StorageRepository,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    init {
        loadUserProfile()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                val userId = auth.currentUser?.uid
                if (userId.isNullOrBlank()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "User not authenticated"
                    )
                    return@launch
                }

                databaseRepository.getUserProfile(userId).collect { result ->
                    result.onSuccess { user ->
                        _state.value = _state.value.copy(
                            displayName = user["displayName"] as? String ?: "",
                            username = user["username"] as? String ?: "",
                            bio = user["bio"] as? String ?: "",
                            phoneNumber = user["phoneNumber"] as? String ?: "",
                            email = user["email"] as? String ?: "",
                            photoUrl = user["photoUrl"] as? String,
                            isLoading = false
                        )
                    }.onFailure { exception ->
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load profile"
                        )
                    }
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    fun updateDisplayName(name: String) {
        _state.value = _state.value.copy(displayName = name)
    }

    fun updateUsername(username: String) {
        val cleanUsername = username.lowercase().replace(" ", "")
        _state.value = _state.value.copy(username = cleanUsername)
    }

    fun updateBio(bio: String) {
        if (bio.length <= 150) {
            _state.value = _state.value.copy(bio = bio)
        }
    }

    fun updatePhoneNumber(phone: String) {
        _state.value = _state.value.copy(phoneNumber = phone)
    }

    fun updateEmail(email: String) {
        _state.value = _state.value.copy(email = email)
    }

    fun uploadProfilePhoto(uri: Uri) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            try {
                // Get userId at the exact moment of upload
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId.isNullOrBlank()) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "Por favor inicia sesión nuevamente" // Please login again
                    )
                    return@launch
                }

                // Upload photo to Storage
                val photoUrl = storageRepository.uploadProfilePhoto(uri, userId)
                
                // Update state with new photo URL
                _state.value = _state.value.copy(
                    photoUrl = photoUrl,
                    isSaving = false,
                    successMessage = "Foto subida exitosamente" // Photo uploaded successfully
                )
                
                // Update Realtime Database with new photo URL
                saveProfile()
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = "Error al subir la foto: ${e.message}" // Error uploading photo
                )
            }
        }
    }

    fun removeProfilePhoto() {
        _state.value = _state.value.copy(photoUrl = null)
    }

    fun saveProfile() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            
            try {
                // Get userId at the exact moment of save
                val userId = FirebaseAuth.getInstance().currentUser?.uid
                if (userId.isNullOrBlank()) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "Por favor inicia sesión nuevamente" // Please login again
                    )
                    return@launch
                }

                if (_state.value.displayName.isBlank()) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "El nombre no puede estar vacío" // Display name cannot be empty
                    )
                    return@launch
                }

                if (_state.value.username.isBlank()) {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = "El nombre de usuario no puede estar vacío" // Username cannot be empty
                    )
                    return@launch
                }

                val profileData = mapOf(
                    "displayName" to _state.value.displayName,
                    "username" to _state.value.username,
                    "bio" to _state.value.bio,
                    "phoneNumber" to _state.value.phoneNumber,
                    "email" to _state.value.email,
                    "photoUrl" to (_state.value.photoUrl ?: ""),
                    "updatedAt" to System.currentTimeMillis()
                )

                databaseRepository.updateUserProfile(userId, profileData).collect { result ->
                    result.onSuccess {
                        _state.value = _state.value.copy(
                            isSaving = false,
                            successMessage = "Perfil actualizado exitosamente" // Profile updated successfully
                        )
                    }.onFailure { exception ->
                        _state.value = _state.value.copy(
                            isSaving = false,
                            error = "Error al guardar el perfil: ${exception.message}" // Error saving profile
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isSaving = false,
                    error = "Error al guardar el perfil: ${e.message}" // Error saving profile
                )
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _state.value = _state.value.copy(successMessage = null)
    }
}
