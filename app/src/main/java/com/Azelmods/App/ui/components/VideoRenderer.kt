package com.Azelmods.App.ui.components

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val eglBase = remember { EglBase.create() }
    
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
                        
                        // Initialize renderer
                        init(eglBase.eglBaseContext, null)
                        
                        // Set scaling type
                        setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                        
                        // Set mirror for front camera
                        setMirror(mirror)
                        
                        // Enable hardware scaler
                        setEnableHardwareScaler(true)
                        
                        // Add video track
                        videoTrack.addSink(this)
                    }
                },
                modifier = Modifier.fillMaxSize(),
                update = { view ->
                    // Update mirror setting if changed
                    view.setMirror(mirror)
                }
            )
        }
    }
    
    DisposableEffect(videoTrack) {
        onDispose {
            videoTrack?.removeSink(null)
            eglBase.release()
        }
    }
}
