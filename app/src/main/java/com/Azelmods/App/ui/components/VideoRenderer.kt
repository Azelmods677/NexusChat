package com.Azelmods.App.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import org.webrtc.EglBase
import org.webrtc.RendererCommon
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun VideoRenderer(
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    mirror: Boolean = false
) {
    val eglBase = remember { EglBase.create() }
    // Keep a reference to the renderer and the track it is bound to, so we can
    // detach the sink and release native resources deterministically.
    val rendererRef = remember { mutableStateOf<SurfaceViewRenderer?>(null) }
    val boundTrackRef = remember { mutableStateOf<VideoTrack?>(null) }
    
    Box(
        modifier = modifier.background(Color.Black)
    ) {
        if (videoTrack != null) {
            AndroidView(
                factory = { ctx ->
                    SurfaceViewRenderer(ctx).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        try {
                            init(eglBase.eglBaseContext, null)
                            setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                            setMirror(mirror)
                            setEnableHardwareScaler(true)
                            videoTrack.addSink(this)
                            rendererRef.value = this
                            boundTrackRef.value = videoTrack
                        } catch (e: Exception) {
                            android.util.Log.e("VideoRenderer", "Error initializing renderer: ${e.message}", e)
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    view.setMirror(mirror)
                }
            )
        }
    }
    
    DisposableEffect(videoTrack) {
        onDispose {
            try {
                // Detach the sink from the exact track it was bound to.
                boundTrackRef.value?.removeSink(rendererRef.value)
            } catch (e: Exception) {
                android.util.Log.w("VideoRenderer", "Error removing sink: ${e.message}")
            }
            try {
                rendererRef.value?.release()
            } catch (e: Exception) {
                android.util.Log.w("VideoRenderer", "Error releasing renderer: ${e.message}")
            }
            rendererRef.value = null
            boundTrackRef.value = null
            try {
                eglBase.release()
            } catch (e: Exception) {
                android.util.Log.w("VideoRenderer", "Error releasing eglBase: ${e.message}")
            }
        }
    }
}
