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

/**
 * Superficie de vídeo WebRTC.
 *
 * @param eglContext el contexto gráfico de [com.Azelmods.App.webrtc.WebRTCManager]. Tiene que
 *   ser ese y no uno propio: los fotogramas llegan como texturas que solo existen en él, y con
 *   otro contexto la pantalla se queda en negro aunque la imagen esté llegando.
 * @param overlay para el recuadro de la propia cámara. Dos superficies de vídeo superpuestas
 *   necesitan decir cuál va encima, o el recuadro queda tapado por la imagen grande.
 */
@Composable
fun VideoRenderer(
    videoTrack: VideoTrack?,
    eglContext: EglBase.Context,
    modifier: Modifier = Modifier,
    mirror: Boolean = false,
    overlay: Boolean = false
) {
    val context = LocalContext.current

    val renderer = remember {
        SurfaceViewRenderer(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            if (overlay) setZOrderMediaOverlay(true)
            try {
                init(eglContext, null)
                setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
                setEnableHardwareScaler(true)
            } catch (e: Exception) {
                android.util.Log.e("VideoRenderer", "Error inicializando el renderer: ${e.message}", e)
            }
        }
    }

    Box(modifier = modifier.background(Color.Black)) {
        AndroidView(
            factory = { renderer },
            modifier = Modifier.fillMaxSize(),
            update = { view -> view.setMirror(mirror) }
        )
    }

    // Enganche del sink: se rehace cada vez que cambia la pista.
    DisposableEffect(videoTrack) {
        val track = videoTrack
        if (track != null) {
            try {
                track.addSink(renderer)
            } catch (e: Exception) {
                android.util.Log.w("VideoRenderer", "No se pudo enganchar la pista: ${e.message}")
            }
        }
        onDispose {
            try {
                track?.removeSink(renderer)
            } catch (e: Exception) {
                android.util.Log.w("VideoRenderer", "Error soltando el sink: ${e.message}")
            }
        }
    }

    // Recursos nativos: se liberan una sola vez, al salir de la composición.
    DisposableEffect(Unit) {
        onDispose {
            try {
                renderer.release()
            } catch (e: Exception) {
                android.util.Log.w("VideoRenderer", "Error liberando el renderer: ${e.message}")
            }
        }
    }
}
