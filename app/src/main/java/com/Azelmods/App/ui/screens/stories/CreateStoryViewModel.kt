package com.Azelmods.App.ui.screens.stories

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.data.repository.StorageRepository
import com.Azelmods.App.utils.StoryExporter
import com.Azelmods.App.utils.StoryVideoComposer
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    private val storageRepository: StorageRepository,
    private val databaseRepository: RealtimeDatabaseRepository,
    private val userRepository: com.Azelmods.App.data.repository.UserRepository,
    private val auth: FirebaseAuth
) : ViewModel() {
    
    private val _state = MutableStateFlow(CreateStoryState())
    val state: StateFlow<CreateStoryState> = _state.asStateFlow()
    
    /**
     * Upload a COMPOSED IMAGE story: the editor captures its editable area (photo +
     * text / stickers / emojis) into [composedBitmap] using a Compose GraphicsLayer,
     * so the overlays are already burned into the pixels. We persist that bitmap to a
     * temp file and upload it instead of the original media.
     *
     * If anything goes wrong while saving the composed bitmap we fall back to
     * uploading [originalUri] so the user never ends up unable to publish.
     */
    fun uploadComposedImageStory(composedBitmap: Bitmap, caption: String, originalUri: Uri?) {
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

                // Persist the composited bitmap (overlays already rendered) to a temp file.
                val uploadUri: Uri = try {
                    StoryExporter.saveComposedStory(context, composedBitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fall back to the original media if the composite could not be saved.
                    originalUri ?: throw e
                }

                val imageUrl = storageRepository.uploadStory(uploadUri, userId)

                databaseRepository.createStory(
                    mediaUrl = imageUrl,
                    mediaType = "IMAGE",
                    isVideo = false,
                    caption = caption
                )

                _state.value = _state.value.copy(
                    isUploading = false,
                    uploadSuccess = true
                )
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
                
                // Step 2: ONLY after upload succeeds, save to Realtime Database
                databaseRepository.createStory(
                    mediaUrl = imageUrl,
                    mediaType = "IMAGE",
                    isVideo = false,
                    caption = caption
                )

                _state.value = _state.value.copy(
                    isUploading = false,
                    uploadSuccess = true
                )
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
     * Upload a COMPOSED VIDEO story: [overlayBitmap] is the editor's editable area
     * captured into a transparent bitmap (text / stickers / emojis already rasterized,
     * transparent where the video shows through). We burn it into every video frame with
     * [StoryVideoComposer] and upload the resulting MP4 instead of the original clip.
     *
     * If compositing fails for any reason we fall back to uploading [originalUri] so the
     * user can always publish (overlays will be missing in that fallback case only).
     */
    fun uploadComposedVideoStory(overlayBitmap: Bitmap, caption: String, originalUri: Uri) {
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

                val uploadUri: Uri = try {
                    StoryVideoComposer.composeVideoWithOverlay(context, originalUri, overlayBitmap)
                } catch (e: Exception) {
                    e.printStackTrace()
                    // Fall back to the original clip if burning the overlays failed.
                    originalUri
                }

                val videoUrl = storageRepository.uploadStoryVideo(uploadUri, userId)

                databaseRepository.createStory(
                    mediaUrl = videoUrl,
                    mediaType = "VIDEO",
                    isVideo = true,
                    caption = caption
                )

                _state.value = _state.value.copy(
                    isUploading = false,
                    uploadSuccess = true
                )
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
                
                // Step 2: ONLY after upload succeeds, save to Realtime Database
                databaseRepository.createStory(
                    mediaUrl = videoUrl,
                    mediaType = "VIDEO",
                    isVideo = true,
                    caption = caption
                )

                _state.value = _state.value.copy(
                    isUploading = false,
                    uploadSuccess = true
                )
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
                
                // TEXT stories skip Storage entirely. Se persisten los campos
                // `text` y `backgroundColor` que el visor realmente lee (antes se
                // metía el texto en mediaUrl y se descartaba el color → texto en blanco).
                databaseRepository.createStory(
                    mediaUrl = text,
                    mediaType = "TEXT",
                    isVideo = false,
                    text = text,
                    backgroundColor = backgroundColor
                )

                _state.value = _state.value.copy(
                    isUploading = false,
                    uploadSuccess = true
                )
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
