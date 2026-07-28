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
 * ## Por qué el contexto EGL ya no cuelga de [videoTrack]
 *
 * La versión anterior liberaba el `EglBase` dentro de un
 * `DisposableEffect(videoTrack)`. Como la pista siempre empieza en `null` y pasa
 * a existir cuando la llamada se conecta, ese cambio de clave disparaba el
 * `onDispose` de la clave anterior **justo después** de que el renderer se
 * hubiera inicializado con ese mismo contexto: el `SurfaceViewRenderer` se
 * quedaba pintando contra un contexto EGL ya liberado y la videollamada se veía
 * en negro de principio a fin, aunque la conexión estuviera perfectamente
 * establecida.
 *
 * Ahora hay dos efectos con responsabilidades separadas:
 * - uno atado a [videoTrack], que sólo engancha y desengancha el sink;
 * - otro atado a `Unit`, que libera renderer y contexto EGL una única vez,
 *   cuando el composable abandona la composición.
 *
 * Como efecto secundario, cambiar de pista en caliente (por ejemplo al girar la
 * cámara o al reconectar) ya funciona: antes el `factory` de `AndroidView` sólo
 * corría una vez y la pista nueva nunca llegaba a recibir un sink.
 */
@Composable
fun VideoRenderer(
    videoTrack: VideoTrack?,
    modifier: Modifier = Modifier,
    mirror: Boolean = false
) {
    val context = LocalContext.current
    val eglBase = remember { EglBase.create() }

    val renderer = remember {
        SurfaceViewRenderer(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            try {
                init(eglBase.eglBaseContext, null)
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
            try {
                eglBase.release()
            } catch (e: Exception) {
                android.util.Log.w("VideoRenderer", "Error liberando eglBase: ${e.message}")
            }
        }
    }
}
