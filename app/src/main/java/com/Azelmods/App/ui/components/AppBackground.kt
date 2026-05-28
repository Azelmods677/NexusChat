package com.Azelmods.App.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.Azelmods.App.data.manager.AppBackgroundManager
import com.Azelmods.App.data.model.BackgroundConfig
import com.Azelmods.App.data.model.BackgroundType
import com.Azelmods.App.ui.theme.linearGradientBrush
import com.Azelmods.App.ui.theme.parseHexColor

/**
 * App-wide background component
 * 
 * Wraps entire app content and renders background behind all screens
 * 
 * Features:
 * - Solid color, image, video, gradient support
 * - Configurable overlay opacity (0-80%)
 * - Smooth crossfade transitions (500ms)
 * - Lifecycle-aware for video backgrounds
 */
@Composable
fun AppBackground(
    backgroundManager: AppBackgroundManager,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val backgroundConfig by backgroundManager.backgroundConfig.collectAsState(
        initial = BackgroundConfig()
    )
    
    Box(modifier = modifier.fillMaxSize()) {
        // Background layer with crossfade
        Crossfade(
            targetState = backgroundConfig,
            animationSpec = tween(500),
            label = "background_crossfade"
        ) { config ->
            when (config.type) {
                BackgroundType.NONE -> {
                    // Default dark background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0A0A))
                    )
                }
                
                BackgroundType.SOLID_COLOR -> {
                    config.colorHex?.let { hex ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(parseHexColor(hex))
                        )
                    }
                }
                
                BackgroundType.IMAGE -> {
                    config.imageUri?.let { uri ->
                        SubcomposeAsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(uri)
                                .crossfade(300)
                                .build(),
                            contentDescription = "App background",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            loading = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0A0A0A))
                                )
                            },
                            error = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0A0A0A))
                                )
                            }
                        )
                    }
                }
                
                BackgroundType.VIDEO -> {
                    config.videoUri?.let { uri ->
                        VideoBackgroundPlayer(
                            videoUri = uri,
                            modifier = Modifier.fillMaxSize(),
                            fallbackColor = Color(0xFF0A0A0A)
                        )
                    }
                }
                
                BackgroundType.GRADIENT -> {
                    val brush = linearGradientBrush(
                        gradientColors = config.gradientColors,
                        gradientAngle = config.gradientAngle
                    )
                    if (brush != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush)
                        )
                    }
                }
                
                BackgroundType.BLUR, BackgroundType.DEFAULT -> {
                    // Not applicable for app-wide background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF0A0A0A))
                    )
                }
            }
        }
        
        // Overlay layer for readability
        if (backgroundConfig.type != BackgroundType.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = backgroundConfig.overlayAlpha))
            )
        }
        
        // Content on top
        content()
    }
}


