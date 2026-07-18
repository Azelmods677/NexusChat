package com.Azelmods.App.ui.screens.background

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.manager.AppBackgroundManager
import com.Azelmods.App.data.model.BackgroundConfig
import com.Azelmods.App.data.model.BackgroundType
import com.Azelmods.App.data.repository.ChatBackgroundRepository
import com.Azelmods.App.data.repository.StorageRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class BackgroundScope {
    APP, CHAT
}

/**
 * ViewModel for background picker screen
 * 
 * Manages:
 * - Background configuration state
 * - App-wide or per-chat scope
 * - Apply changes to AppBackgroundManager or ChatBackgroundRepository
 */
@HiltViewModel
class BackgroundPickerViewModel @Inject constructor(
    private val appBackgroundManager: AppBackgroundManager,
    private val chatBackgroundRepository: ChatBackgroundRepository,
    private val storageRepository: StorageRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val chatId: String? = savedStateHandle.get<String>("chatId")

    val scope: BackgroundScope = if (chatId != null) BackgroundScope.CHAT else BackgroundScope.APP

    private val _selectedConfig = MutableStateFlow(BackgroundConfig())
    val selectedConfig: StateFlow<BackgroundConfig> = _selectedConfig.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }
    
    init {
        loadCurrentBackground()
    }
    
    private fun loadCurrentBackground() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when (scope) {
                    BackgroundScope.APP -> {
                        appBackgroundManager.backgroundConfig.collect { config ->
                            _selectedConfig.value = config
                            _isLoading.value = false
                        }
                    }
                    BackgroundScope.CHAT -> {
                        chatId?.let { id ->
                            chatBackgroundRepository.loadBackground(id)
                            chatBackgroundRepository.getBackground(id).collect { config ->
                                _selectedConfig.value = config
                                _isLoading.value = false
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _isLoading.value = false
            }
        }
    }
    
    fun updateConfig(config: BackgroundConfig) {
        _selectedConfig.value = config
    }
    
    fun setType(type: BackgroundType) {
        _selectedConfig.value = _selectedConfig.value.copy(type = type)
    }
    
    fun setSolidColor(colorHex: String) {
        _selectedConfig.value = _selectedConfig.value.copy(
            type = BackgroundType.SOLID_COLOR,
            colorHex = colorHex
        )
    }
    
    fun pickImage(uri: Uri) {
        _selectedConfig.value = _selectedConfig.value.copy(
            type = BackgroundType.IMAGE,
            imageUri = uri.toString()
        )
    }
    
    fun pickVideo(uri: Uri) {
        _selectedConfig.value = _selectedConfig.value.copy(
            type = BackgroundType.VIDEO,
            videoUri = uri.toString()
        )
    }
    
    fun setGradient(colors: List<String>, angle: Int) {
        _selectedConfig.value = _selectedConfig.value.copy(
            type = BackgroundType.GRADIENT,
            gradientColors = colors,
            gradientAngle = angle
        )
    }
    
    fun setBlurRadius(radius: Float) {
        _selectedConfig.value = _selectedConfig.value.copy(
            type = BackgroundType.BLUR,
            blurRadius = radius
        )
    }
    
    fun setOverlayAlpha(alpha: Float) {
        _selectedConfig.value = _selectedConfig.value.copy(overlayAlpha = alpha)
    }
    
    fun applyBackground(onComplete: () -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Sube a Storage la imagen/video local antes de persistir la config.
                val config = uploadLocalMediaIfNeeded(_selectedConfig.value)

                when (scope) {
                    BackgroundScope.APP -> {
                        appBackgroundManager.saveBackground(config)
                    }
                    BackgroundScope.CHAT -> {
                        chatId?.let { id ->
                            chatBackgroundRepository.saveBackground(id, config)
                        }
                    }
                }
                _selectedConfig.value = config
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
                _error.value = "No se pudo aplicar el fondo: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Sube a Firebase Storage la imagen/video elegido de galería (content://) y
     * reemplaza el URI local por la URL de descarga. Sin esto el URI local caduca
     * al reiniciar la app o al revocarse el permiso → el fondo desaparecía y, en
     * chats, no era visible para el otro participante.
     *
     * En scope CHAT reutiliza las rutas de media del chat; en scope APP usa una
     * ruta propia del usuario (user_backgrounds/{uid}).
     */
    private suspend fun uploadLocalMediaIfNeeded(config: BackgroundConfig): BackgroundConfig {
        val uid = auth.currentUser?.uid
        return when (config.type) {
            BackgroundType.IMAGE -> {
                val local = config.imageUri
                if (!local.isNullOrBlank() && local.startsWith("content://")) {
                    checkFileSize(Uri.parse(local), MAX_IMAGE_BYTES, "La imagen supera el límite de 15 MB")
                    val url = when {
                        scope == BackgroundScope.CHAT && chatId != null ->
                            storageRepository.uploadChatImage(Uri.parse(local), chatId)
                        uid != null ->
                            storageRepository.uploadUserBackground(Uri.parse(local), uid, isVideo = false)
                        else -> local
                    }
                    config.copy(imageUri = url)
                } else config
            }
            BackgroundType.VIDEO -> {
                val local = config.videoUri
                if (!local.isNullOrBlank() && local.startsWith("content://")) {
                    checkFileSize(Uri.parse(local), MAX_VIDEO_BYTES, "El video supera el límite de 60 MB")
                    val url = when {
                        scope == BackgroundScope.CHAT && chatId != null ->
                            storageRepository.uploadChatVideo(Uri.parse(local), chatId)
                        uid != null ->
                            storageRepository.uploadUserBackground(Uri.parse(local), uid, isVideo = true)
                        else -> local
                    }
                    config.copy(videoUri = url)
                } else config
            }
            else -> config
        }
    }
    
    /**
     * Lanza IllegalArgumentException con mensaje legible si el archivo excede
     * el límite. El catch de applyBackground lo muestra en _error. Si el tamaño
     * no se puede determinar (statSize = -1) se permite subir: el límite es
     * proteccion de UX, no de seguridad (esa la ponen las reglas de Storage).
     */
    private fun checkFileSize(uri: Uri, maxBytes: Long, message: String) {
        val size = storageRepository.getFileSizeBytes(uri)
        if (size > maxBytes) throw IllegalArgumentException(message)
    }

    fun clearBackground() {
        _selectedConfig.value = BackgroundConfig(
            type = if (scope == BackgroundScope.CHAT) BackgroundType.DEFAULT else BackgroundType.NONE
        )
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 15L * 1024 * 1024
        const val MAX_VIDEO_BYTES = 60L * 1024 * 1024
    }
}
