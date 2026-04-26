package com.Azelmods.App.ui.screens.background

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.manager.AppBackgroundManager
import com.Azelmods.App.data.model.BackgroundConfig
import com.Azelmods.App.data.model.BackgroundType
import com.Azelmods.App.data.repository.ChatBackgroundRepository
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
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    private val chatId: String? = savedStateHandle.get<String>("chatId")
    
    val scope: BackgroundScope = if (chatId != null) BackgroundScope.CHAT else BackgroundScope.APP
    
    private val _selectedConfig = MutableStateFlow(BackgroundConfig())
    val selectedConfig: StateFlow<BackgroundConfig> = _selectedConfig.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
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
                when (scope) {
                    BackgroundScope.APP -> {
                        appBackgroundManager.saveBackground(_selectedConfig.value)
                    }
                    BackgroundScope.CHAT -> {
                        chatId?.let { id ->
                            chatBackgroundRepository.saveBackground(id, _selectedConfig.value)
                        }
                    }
                }
                onComplete()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearBackground() {
        _selectedConfig.value = BackgroundConfig(
            type = if (scope == BackgroundScope.CHAT) BackgroundType.DEFAULT else BackgroundType.NONE
        )
    }
}
