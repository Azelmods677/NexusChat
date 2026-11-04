package com.Azelmods.App.webrtc

import android.media.AudioManager
import android.media.ToneGenerator
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * El "tuuu… tuuu…" que oye quien llama mientras suena en el otro teléfono.
 *
 * Sin esto, llamar era un silencio total y parecía que la llamada no funcionaba. Se usa el
 * tono estándar del sistema por el canal de voz, así que sale por el mismo sitio que la
 * llamada: auricular en voz, altavoz en vídeo.
 */
@Singleton
class CallRingback @Inject constructor() {

    private var generator: ToneGenerator? = null

    fun start() {
        if (generator != null) return
        generator = runCatching {
            ToneGenerator(AudioManager.STREAM_VOICE_CALL, VOLUME).apply {
                startTone(ToneGenerator.TONE_SUP_RINGTONE)
            }
        }.onFailure { Log.w(TAG, "No se pudo reproducir el tono de llamada: ${it.message}") }.getOrNull()
    }

    fun stop() {
        generator?.let { tone ->
            runCatching {
                tone.stopTone()
                tone.release()
            }
        }
        generator = null
    }

    private companion object {
        const val TAG = "CallRingback"
        const val VOLUME = 85
    }
}
