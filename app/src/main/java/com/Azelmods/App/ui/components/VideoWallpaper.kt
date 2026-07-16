package com.Azelmods.App.ui.components

import android.net.Uri
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.Azelmods.App.ui.theme.DarkBackground

/**
 * Video wallpaper component for chat background.
 * 
 * Features:
 * - Looping playback
 * - Muted audio
 * - Scales to fill screen
 * - Fallback to solid color if video fails
 * - Adjustable opacity
 */
@Composable
fun VideoWallpaper(
    videoUri: Uri?,
    modifier: Modifier = Modifier,
    alpha: Float = 1f
) {
    val context = LocalContext.current
    var isError by remember { mutableStateOf(false) }
    
    if (videoUri == null || isError) {
        // Fallback to solid background
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
        )
        return
    }
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            repeatMode = Player.REPEAT_MODE_ONE
            volume = 0f // Muted
            playWhenReady = true
            prepare()
        }
    }
    
    DisposableEffect(videoUri) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                isError = true
            }
        }
        exoPlayer.addListener(listener)
        
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = false
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    // Scale to fill
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    this.alpha = alpha
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
