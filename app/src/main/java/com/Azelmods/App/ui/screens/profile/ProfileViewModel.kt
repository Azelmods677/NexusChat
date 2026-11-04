package com.Azelmods.App.ui.screens.profile

import com.Azelmods.App.i18n.tr

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.tasks.await
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
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val databaseRepository: RealtimeDatabaseRepository,
    private val storageRepository: StorageRepository,
    private val premiumRepository: com.Azelmods.App.data.repository.PremiumRepository
) : ViewModel() {

    // ── State ─────────────────────────────────────────────────────────────────

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    // Convenience property for screens that expect userProfile
    val userProfile: StateFlow<User?> = MutableStateFlow<User?>(null).apply {
        viewModelScope.launch {
            _state.collect { state ->
                (this@apply as MutableStateFlow).value = state.user
            }
        }
    }

    private val _userId = MutableStateFlow<String?>(null)
    val userId: StateFlow<String?> = _userId.asStateFlow()

    private var presenceJob: Job? = null
    private var premiumJob: Job? = null

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

        if (_userId.value != userId || mediaListener == null) observeMedia(userId)
        if (_userId.value != userId || premiumJob == null) {
            premiumJob?.cancel()
            premiumJob = viewModelScope.launch {
                premiumRepository.observe(userId).collect { status ->
                    _state.value = _state.value.copy(premium = status)
                }
            }
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
                        coverVideoUrl = (userData["coverVideoUrl"] as? String)?.takeIf { it.isNotBlank() },
                        profileVideoUrl = (userData["profileVideoUrl"] as? String)?.takeIf { it.isNotBlank() },
                        profileColor = (userData["profileColor"] as? Number)?.toInt() ?: -1,
                        bio = userData["bio"] as? String ?: "",
                        status = userData["status"] as? String ?: tr("¡Hola! Estoy usando NexusChat"),
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
                                // Force recompose with new URL
                                val updatedUser = _state.value.user?.copy(photoUrl = photoUrl)
                                _state.value = _state.value.copy(
                                    user = updatedUser,
                                    isUploadingProfile = false,
                                    uploadSuccess = true
                                )
                                
                                // Refresh from database to ensure sync
                                viewModelScope.launch(Dispatchers.IO) {
                                    try {
                                        val refreshedData = databaseRepository.getUserById(currentUserId)
                                        refreshedData?.let { data ->
                                            val refreshedUser = User(
                                                uid = data["uid"] as? String ?: currentUserId,
                                                name = data["name"] as? String ?: "",
                                                displayName = data["displayName"] as? String ?: data["name"] as? String ?: "",
                                                username = data["username"] as? String ?: "",
                                                email = data["email"] as? String ?: "",
                                                phone = data["phone"] as? String ?: "",
                                                photoUrl = data["photoUrl"] as? String,
                                                coverUrl = data["coverUrl"] as? String,
                                                coverVideoUrl = (data["coverVideoUrl"] as? String)?.takeIf { it.isNotBlank() },
                                                bio = data["bio"] as? String ?: "",
                                                status = data["status"] as? String ?: tr("¡Hola! Estoy usando NexusChat"),
                                                isOnline = data["isOnline"] as? Boolean ?: false,
                                                lastSeen = (data["lastSeen"] as? Long) ?: System.currentTimeMillis(),
                                                isPremium = data["isPremium"] as? Boolean ?: false,
                                                createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
                                                fcmToken = data["fcmToken"] as? String,
                                                messageCount = (data["messageCount"] as? Long)?.toInt() ?: 0,
                                                filesShared = (data["filesShared"] as? Long)?.toInt() ?: 0
                                            )
                                            _state.value = _state.value.copy(user = refreshedUser)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error refreshing profile after upload", e)
                                    }
                                }
                                
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
        clearCoverVideo()
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
                                // Force recompose with new URL
                                val updatedUser = _state.value.user?.copy(coverUrl = coverUrl)
                                _state.value = _state.value.copy(
                                    user = updatedUser,
                                    isUploadingCover = false,
                                    uploadSuccess = true
                                )
                                
                                // Refresh from database to ensure sync
                                viewModelScope.launch(Dispatchers.IO) {
                                    try {
                                        val refreshedData = databaseRepository.getUserById(currentUserId)
                                        refreshedData?.let { data ->
                                            val refreshedUser = User(
                                                uid = data["uid"] as? String ?: currentUserId,
                                                name = data["name"] as? String ?: "",
                                                displayName = data["displayName"] as? String ?: data["name"] as? String ?: "",
                                                username = data["username"] as? String ?: "",
                                                email = data["email"] as? String ?: "",
                                                phone = data["phone"] as? String ?: "",
                                                photoUrl = data["photoUrl"] as? String,
                                                coverUrl = data["coverUrl"] as? String,
                                                coverVideoUrl = (data["coverVideoUrl"] as? String)?.takeIf { it.isNotBlank() },
                                                bio = data["bio"] as? String ?: "",
                                                status = data["status"] as? String ?: tr("¡Hola! Estoy usando NexusChat"),
                                                isOnline = data["isOnline"] as? Boolean ?: false,
                                                lastSeen = (data["lastSeen"] as? Long) ?: System.currentTimeMillis(),
                                                isPremium = data["isPremium"] as? Boolean ?: false,
                                                createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
                                                fcmToken = data["fcmToken"] as? String,
                                                messageCount = (data["messageCount"] as? Long)?.toInt() ?: 0,
                                                filesShared = (data["filesShared"] as? Long)?.toInt() ?: 0
                                            )
                                            _state.value = _state.value.copy(user = refreshedUser)
                                        }
                                    } catch (e: Exception) {
                                        Log.e(TAG, "Error refreshing profile after upload", e)
                                    }
                                }
                                
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

    // ── Fotos y vídeos del perfil ─────────────────────────────────────────────

    private var mediaListener: ValueEventListener? = null
    private var mediaRef: DatabaseReference? = null

    private fun userRef(userId: String) = FirebaseDatabase.getInstance().reference.child("users").child(userId)

    private fun observeMedia(userId: String) {
        mediaListener?.let { listener -> mediaRef?.removeEventListener(listener) }
        val ref = userRef(userId).child("profileMedia")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val items = snapshot.children.mapNotNull { child ->
                    val url = child.child("url").getValue(String::class.java) ?: return@mapNotNull null
                    ProfileMedia(
                        id = child.key.orEmpty(),
                        url = url,
                        thumbUrl = child.child("thumbUrl").getValue(String::class.java) ?: url,
                        isVideo = child.child("isVideo").getValue(Boolean::class.java) ?: false,
                        timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L
                    )
                }.sortedByDescending { it.timestamp }
                _state.value = _state.value.copy(media = items)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "No se pudieron cargar las fotos y vídeos: ${error.message}")
            }
        }
        ref.addValueEventListener(listener)
        mediaRef = ref
        mediaListener = listener
    }

    private fun ownUid(): String? = FirebaseAuth.getInstance().currentUser?.uid

    private fun isVideo(uri: Uri): Boolean = context.contentResolver.getType(uri)?.startsWith("video/") == true

    /** Primer fotograma como imagen: es lo que se ve en cuadrículas y listas. */
    private fun videoPoster(uri: Uri): Uri? = runCatching {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val frame = retriever.getFrameAtTime(0) ?: return null
            val file = java.io.File(context.cacheDir, "poster_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { frame.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, it) }
            Uri.fromFile(file)
        } finally {
            retriever.release()
        }
    }.getOrNull()

    private fun videoDurationMs(uri: Uri): Long = runCatching {
        val retriever = android.media.MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }.getOrDefault(0L)

    /** Añade una foto o un vídeo a la galería del perfil. */
    fun addProfileMedia(uri: Uri) {
        val uid = ownUid() ?: return
        if (!_state.value.premium.isActiveNow() && _state.value.media.size >= FREE_GALLERY_LIMIT) {
            _state.value = _state.value.copy(
                error = "Tu galería admite $FREE_GALLERY_LIMIT fotos o vídeos. Con Premium no tiene límite."
            )
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(isUploadingMedia = true, error = null)
            runCatching {
                val video = isVideo(uri)
                val url = storageRepository.uploadUserBackground(uri, uid, video)
                val thumbUrl = if (video) {
                    videoPoster(uri)?.let { storageRepository.uploadUserBackground(it, uid, false) } ?: url
                } else {
                    url
                }
                val item = mapOf(
                    "url" to url,
                    "thumbUrl" to thumbUrl,
                    "isVideo" to video,
                    "timestamp" to System.currentTimeMillis()
                )
                userRef(uid).child("profileMedia").push().setValue(item).await()
            }.onFailure {
                Log.e(TAG, "No se pudo subir a la galería", it)
                _state.value = _state.value.copy(error = "No se pudo subir: ${it.message}")
            }
            _state.value = _state.value.copy(isUploadingMedia = false)
        }
    }

    fun deleteProfileMedia(item: ProfileMedia) {
        val uid = ownUid() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                userRef(uid).child("profileMedia").child(item.id).removeValue().await()
                storageRepository.deleteFile(item.url)
                if (item.thumbUrl != item.url) storageRepository.deleteFile(item.thumbUrl)
            }.onFailure { Log.w(TAG, "No se pudo borrar del todo: ${it.message}") }
        }
    }

    /** Pone un vídeo corto como avatar. Su primer fotograma pasa a ser la foto en las listas. */
    fun uploadProfileVideo(uri: Uri) {
        val uid = ownUid() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (videoDurationMs(uri) > MAX_PROFILE_VIDEO_MS) {
                _state.value = _state.value.copy(error = "El vídeo de perfil puede durar hasta 30 segundos")
                return@launch
            }
            _state.value = _state.value.copy(isUploadingProfile = true, error = null)
            runCatching {
                val videoUrl = storageRepository.uploadUserBackground(uri, uid, true)
                val photoUrl = videoPoster(uri)?.let { storageRepository.uploadProfilePhoto(it, uid) }
                val changes = buildMap<String, Any> {
                    put("profileVideoUrl", videoUrl)
                    if (photoUrl != null) put("photoUrl", photoUrl)
                }
                userRef(uid).updateChildren(changes).await()
                val user = _state.value.user
                _state.value = _state.value.copy(
                    user = user?.copy(profileVideoUrl = videoUrl, photoUrl = photoUrl ?: user.photoUrl)
                )
            }.onFailure {
                Log.e(TAG, "No se pudo subir el vídeo de perfil", it)
                _state.value = _state.value.copy(error = "No se pudo subir el vídeo: ${it.message}")
            }
            _state.value = _state.value.copy(isUploadingProfile = false)
        }
    }

    fun removeProfileVideo() {
        val uid = ownUid() ?: return
        val old = _state.value.user?.profileVideoUrl
        _state.value = _state.value.copy(user = _state.value.user?.copy(profileVideoUrl = null))
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                userRef(uid).child("profileVideoUrl").removeValue().await()
                old?.let { storageRepository.deleteFile(it) }
            }.onFailure { Log.w(TAG, "No se pudo quitar el vídeo: ${it.message}") }
        }
    }

    /** Pone una foto o vídeo de la galería como principal, sin volver a subir nada. */
    fun setMainFromMedia(item: ProfileMedia) {
        val uid = ownUid() ?: return
        val video = item.url.takeIf { item.isVideo }
        val photo = if (item.isVideo) item.thumbUrl else item.url
        _state.value = _state.value.copy(
            user = _state.value.user?.copy(profileVideoUrl = video, photoUrl = photo)
        )
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                userRef(uid).updateChildren(mapOf("profileVideoUrl" to video, "photoUrl" to photo)).await()
            }.onFailure {
                Log.w(TAG, "No se pudo cambiar la foto principal: ${it.message}")
                _state.value = _state.value.copy(error = "No se pudo cambiar la foto principal")
            }
        }
    }

    /** La portada también puede ser un vídeo en bucle; su primer fotograma queda como imagen. */
    fun uploadCoverVideo(uri: Uri) {
        val uid = ownUid() ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (videoDurationMs(uri) > MAX_PROFILE_VIDEO_MS) {
                _state.value = _state.value.copy(error = "El vídeo de portada puede durar hasta 30 segundos")
                return@launch
            }
            _state.value = _state.value.copy(isUploadingProfile = true, error = null)
            runCatching {
                val old = _state.value.user?.coverVideoUrl
                val videoUrl = storageRepository.uploadUserBackground(uri, uid, true)
                val poster = videoPoster(uri)?.let { storageRepository.uploadCoverPhoto(it, uid) }
                val changes = buildMap<String, Any> {
                    put("coverVideoUrl", videoUrl)
                    if (poster != null) put("coverUrl", poster)
                }
                userRef(uid).updateChildren(changes).await()
                val user = _state.value.user
                _state.value = _state.value.copy(
                    user = user?.copy(coverVideoUrl = videoUrl, coverUrl = poster ?: user.coverUrl)
                )
                old?.let { runCatching { storageRepository.deleteFile(it) } }
            }.onFailure {
                Log.e(TAG, "No se pudo subir el vídeo de portada", it)
                _state.value = _state.value.copy(error = "No se pudo subir el vídeo: ${it.message}")
            }
            _state.value = _state.value.copy(isUploadingProfile = false)
        }
    }

    private fun clearCoverVideo() {
        val uid = ownUid() ?: return
        val old = _state.value.user?.coverVideoUrl ?: return
        _state.value = _state.value.copy(user = _state.value.user?.copy(coverVideoUrl = null))
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                userRef(uid).child("coverVideoUrl").removeValue().await()
                storageRepository.deleteFile(old)
            }.onFailure { Log.w(TAG, "No se pudo quitar el vídeo de portada: ${it.message}") }
        }
    }

    fun removeCover() {
        clearCoverVideo()
        val uid = ownUid() ?: return
        val old = _state.value.user?.coverUrl
        _state.value = _state.value.copy(user = _state.value.user?.copy(coverUrl = null))
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                userRef(uid).child("coverUrl").removeValue().await()
                old?.let { storageRepository.deleteFile(it) }
            }.onFailure { Log.w(TAG, "No se pudo quitar la portada: ${it.message}") }
        }
    }

    fun setProfileColor(index: Int) {
        val uid = ownUid() ?: return
        _state.value = _state.value.copy(user = _state.value.user?.copy(profileColor = index))
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { userRef(uid).child("profileColor").setValue(index).await() }
                .onFailure { Log.w(TAG, "No se pudo guardar el color: ${it.message}") }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
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
        mediaListener?.let { listener -> mediaRef?.removeEventListener(listener) }
    }

    companion object {
        private const val TAG = "ProfileViewModel"
        private const val MAX_PROFILE_VIDEO_MS = 31_000L
        private const val FREE_GALLERY_LIMIT = 10
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
    val showEditSheet: Boolean = false,
    val media: List<ProfileMedia> = emptyList(),
    val isUploadingMedia: Boolean = false,
    val premium: com.Azelmods.App.data.repository.PremiumStatus = com.Azelmods.App.data.repository.PremiumStatus()
)

/** Una foto o un vídeo de la galería del perfil. */
data class ProfileMedia(
    val id: String,
    val url: String,
    val thumbUrl: String,
    val isVideo: Boolean,
    val timestamp: Long
)
