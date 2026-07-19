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
    
    // remember(videoUri): si el usuario cambia el video de fondo con el chat
    // abierto, DisposableEffect libera el player viejo — sin la key, remember
    // seguiría devolviendo el player liberado y el fondo quedaba en negro.
    val exoPlayer = remember(videoUri) {
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
            // update se ejecuta en cada recomposición: sin esto, al cambiar el
            // video de fondo el PlayerView seguía atado al player anterior (ya
            // liberado por DisposableEffect) y el fondo quedaba en negro.
            update = { view ->
                if (view.player !== exoPlayer) view.player = exoPlayer
                view.alpha = alpha
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
