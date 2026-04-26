package com.Azelmods.App.ui.screens.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.model.User
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ProfileViewModel - Manages profile screen state and operations.
 *
 * Features:
 * - Real-time user profile loading
 * - Real-time online status and last seen tracking
 * - Profile and cover photo uploads with crop parameters
 * - Profile editing (name, bio, status)
 * - Own profile vs other user profile detection
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val databaseRepository: RealtimeDatabaseRepository,
    private val storageRepository: StorageRepository
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private var presenceJob: Job? = null

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Loads user profile and starts observing real-time presence.
     *
     * @param userId The user ID to load profile for.
     */
    fun loadUserProfile(userId: String) {
        // Cancel previous presence observation if loading a different user
        if (_userId.value != userId) {
            presenceJob?.cancel()
            presenceJob = null
        }

        _userId.value = userId
        _state.value = _state.value.copy(isLoading = true, error = null)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        val isOwnProfile = userId == currentUserId

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch user data
                val userData = databaseRepository.getUserById(userId)

                if (userData != null) {
                    val user = User(
                        uid = userData["uid"] as? String ?: userId,
                        name = userData["name"] as? String ?: "",
                        displayName = userData["displayName"] as? String ?: userData["name"] as? String ?: "",
                        username = userData["username"] as? String ?: "",
                        email = userData["email"] as? String ?: "",
                        phone = userData["phone"] as? String ?: "",
                        photoUrl = userData["photoUrl"] as? String,
                        coverUrl = userData["coverUrl"] as? String,
                        bio = userData["bio"] as? String ?: "",
                        status = userData["status"] as? String ?: "Hey there! I'm using Nexus Chat",
                        isOnline = userData["isOnline"] as? Boolean ?: false,
                        lastSeen = (userData["lastSeen"] as? Long) ?: System.currentTimeMillis(),
                        isPremium = userData["isPremium"] as? Boolean ?: false,
                        createdAt = (userData["createdAt"] as? Long) ?: System.currentTimeMillis(),
                        fcmToken = userData["fcmToken"] as? String,
                        messageCount = (userData["messageCount"] as? Long)?.toInt() ?: 0,
                        filesShared = (userData["filesShared"] as? Long)?.toInt() ?: 0
                    )

                    _state.value = _state.value.copy(
                        user = user,
                        isLoading = false,
                        isOwnProfile = isOwnProfile,
                        isOnline = user.isOnline,
                        lastSeen = user.lastSeen
                    )

                    // Start observing real-time presence
                    startPresenceObservation(userId)
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "User not found"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user profile", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to load profile: ${e.message}"
                )
            }
        }
    }

    /**
     * Uploads a new profile photo with crop parameters.
     *
     * @param uri The image URI to upload.
     * @param scale The scale factor applied during cropping.
     * @param offsetX The horizontal offset applied during cropping.
     * @param offsetY The vertical offset applied during cropping.
     */
    fun uploadProfilePhoto(uri: Uri, scale: Float, offsetX: Float, offsetY: Float) {
        val currentUserId = _userId.value ?: return

        _state.value = _state.value.copy(isUploadingProfile = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Upload photo to Firebase Storage
                val photoUrl = storageRepository.uploadProfilePhoto(uri, currentUserId)

                // Update Firebase Realtime Database
                val updateData = mapOf(
                    "photoUrl" to photoUrl,
                    "updatedAt" to System.currentTimeMillis()
                )

                databaseRepository.updateUserProfile(currentUserId, updateData)
                    .catch { e ->
                        Log.e(TAG, "Error updating profile photo in database", e)
                        _state.value = _state.value.copy(
                            isUploadingProfile = false,
                            error = "Failed to update profile photo: ${e.message}"
                        )
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = {
                                // Update local state
                                val updatedUser = _state.value.user?.copy(photoUrl = photoUrl)
                                _state.value = _state.value.copy(
                                    user = updatedUser,
                                    isUploadingProfile = false,
                                    uploadSuccess = true
                                )
                                Log.d(TAG, "Profile photo uploaded successfully")
                            },
                            onFailure = { e ->
                                Log.e(TAG, "Error updating profile photo", e)
                                _state.value = _state.value.copy(
                                    isUploadingProfile = false,
                                    error = "Failed to update profile photo: ${e.message}"
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading profile photo", e)
                _state.value = _state.value.copy(
                    isUploadingProfile = false,
                    error = "Failed to upload profile photo: ${e.message}"
                )
            }
        }
    }

    /**
     * Uploads a new cover photo with crop parameters.
     *
     * @param uri The image URI to upload.
     * @param scale The scale factor applied during cropping.
     * @param offsetX The horizontal offset applied during cropping.
     * @param offsetY The vertical offset applied during cropping.
     */
    fun uploadCoverPhoto(uri: Uri, scale: Float, offsetX: Float, offsetY: Float) {
        val currentUserId = _userId.value ?: return

        _state.value = _state.value.copy(isUploadingCover = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Upload photo to Firebase Storage
                val coverUrl = storageRepository.uploadCoverPhoto(uri, currentUserId)

                // Update Firebase Realtime Database
                val updateData = mapOf(
                    "coverUrl" to coverUrl,
                    "updatedAt" to System.currentTimeMillis()
                )

                databaseRepository.updateUserProfile(currentUserId, updateData)
                    .catch { e ->
                        Log.e(TAG, "Error updating cover photo in database", e)
                        _state.value = _state.value.copy(
                            isUploadingCover = false,
                            error = "Failed to update cover photo: ${e.message}"
                        )
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = {
                                // Update local state
                                val updatedUser = _state.value.user?.copy(coverUrl = coverUrl)
                                _state.value = _state.value.copy(
                                    user = updatedUser,
                                    isUploadingCover = false,
                                    uploadSuccess = true
                                )
                                Log.d(TAG, "Cover photo uploaded successfully")
                            },
                            onFailure = { e ->
                                Log.e(TAG, "Error updating cover photo", e)
                                _state.value = _state.value.copy(
                                    isUploadingCover = false,
                                    error = "Failed to update cover photo: ${e.message}"
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading cover photo", e)
                _state.value = _state.value.copy(
                    isUploadingCover = false,
                    error = "Failed to upload cover photo: ${e.message}"
                )
            }
        }
    }

    /**
     * Saves profile changes (name, bio, status).
     *
     * @param name The new display name.
     * @param bio The new bio.
     * @param status The new status message.
     */
    fun saveProfile(name: String, bio: String, status: String) {
        val currentUserId = _userId.value ?: return

        _state.value = _state.value.copy(isLoading = true, error = null)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val updateData = mapOf(
                    "displayName" to name,
                    "bio" to bio,
                    "status" to status,
                    "updatedAt" to System.currentTimeMillis()
                )

                databaseRepository.updateUserProfile(currentUserId, updateData)
                    .catch { e ->
                        Log.e(TAG, "Error saving profile", e)
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Failed to save profile: ${e.message}"
                        )
                    }
                    .collect { result ->
                        result.fold(
                            onSuccess = {
                                // Update local state
                                val updatedUser = _state.value.user?.copy(
                                    displayName = name,
                                    bio = bio,
                                    status = status
                                )
                                _state.value = _state.value.copy(
                                    user = updatedUser,
                                    isLoading = false,
                                    uploadSuccess = true,
                                    showEditSheet = false
                                )
                                Log.d(TAG, "Profile saved successfully")
                            },
                            onFailure = { e ->
                                Log.e(TAG, "Error saving profile", e)
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = "Failed to save profile: ${e.message}"
                                )
                            }
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error saving profile", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Failed to save profile: ${e.message}"
                )
            }
        }
    }

    /**
     * Toggles the edit profile bottom sheet visibility.
     */
    fun toggleEditSheet() {
        _state.value = _state.value.copy(showEditSheet = !_state.value.showEditSheet)
    }

    /**
     * Clears the upload success flag.
     */
    fun clearUploadSuccess() {
        _state.value = _state.value.copy(uploadSuccess = false)
    }

    /**
     * Reloads the current user profile.
     */
    fun reloadProfile() {
        val currentUserId = _userId.value ?: return
        loadUserProfile(currentUserId)
    }

    /**
     * Builds a chat ID from two user IDs (sorted alphabetically).
     */
    fun buildChatId(otherUserId: String): String {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return otherUserId
        return listOf(currentUserId, otherUserId).sorted().joinToString("_")
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    /**
     * Starts observing real-time presence for the given user.
     */
    private fun startPresenceObservation(userId: String) {
        presenceJob?.cancel()

        presenceJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                databaseRepository.observeUserPresence(userId)
                    .catch { e ->
                        Log.e(TAG, "Error observing presence", e)
                    }
                    .collect { (isOnline, lastSeen) ->
                        _state.value = _state.value.copy(
                            isOnline = isOnline,
                            lastSeen = lastSeen
                        )
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Error in presence observation", e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        presenceJob?.cancel()
    }

    companion object {
        private const val TAG = "ProfileViewModel"
    }
}

/**
 * ProfileState - Represents the state of the profile screen.
 */
data class ProfileState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isOwnProfile: Boolean = true,
    val isUploadingProfile: Boolean = false,
    val isUploadingCover: Boolean = false,
    val uploadSuccess: Boolean = false,
    val error: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = 0L,
    val showEditSheet: Boolean = false
)
