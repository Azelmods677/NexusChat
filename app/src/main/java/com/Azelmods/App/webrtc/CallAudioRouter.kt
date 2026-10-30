package com.Azelmods.App.webrtc

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enrutado del audio de las llamadas.
 *
 * Antes el modo de comunicación solo se activaba al pulsar "Altavoz" y nunca se
 * restauraba. Consecuencias: la llamada sonaba por el canal multimedia (sin
 * cancelación de eco del sistema, con el volumen de la música), el auricular
 * Bluetooth se ignoraba y, después de colgar, el teléfono se quedaba en modo
 * llamada y la música de otras apps salía por el auricular.
 *
 * Ahora:
 * - [start] pone `MODE_IN_COMMUNICATION` al empezar la llamada y elige la salida:
 *   auricular Bluetooth/cable si hay uno conectado; si no, altavoz en vídeo y
 *   auricular del teléfono en voz (como cualquier app de llamadas).
 * - [setSpeakerOn] alterna altavoz y salida "privada".
 * - [stop] lo deja todo como estaba al colgar.
 *
 * minSdk 31 → API de dispositivos de comunicación (`setCommunicationDevice`).
 */
@Singleton
class CallAudioRouter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioManager: AudioManager? =
        context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager

    private val _speakerOn = MutableStateFlow(false)
    val speakerOn: StateFlow<Boolean> = _speakerOn.asStateFlow()

    private val _headsetConnected = MutableStateFlow(false)
    val headsetConnected: StateFlow<Boolean> = _headsetConnected.asStateFlow()

    private var previousMode: Int = AudioManager.MODE_NORMAL
    private var active = false

    fun start(isVideoCall: Boolean) {
        val am = audioManager ?: return
        if (!active) {
            previousMode = am.mode
            active = true
        }
        runCatching { am.mode = AudioManager.MODE_IN_COMMUNICATION }

        val headset = findHeadset()
        _headsetConnected.value = headset != null
        when {
            headset != null -> setDevice(headset, speaker = false)
            isVideoCall -> setSpeakerOn(true)
            else -> setSpeakerOn(false)
        }
    }

    fun setSpeakerOn(on: Boolean) {
        val am = audioManager ?: return
        val target = if (on) {
            am.availableCommunicationDevices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER }
        } else {
            findHeadset() ?: am.availableCommunicationDevices.firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BUILTIN_EARPIECE
            }
        }
        if (target != null) {
            setDevice(target, speaker = on)
        } else {
            runCatching { am.clearCommunicationDevice() }
            _speakerOn.value = false
        }
    }

    fun toggleSpeaker() = setSpeakerOn(!_speakerOn.value)

    fun stop() {
        val am = audioManager ?: return
        if (!active) return
        active = false
        runCatching { am.clearCommunicationDevice() }
        runCatching { am.mode = previousMode }
        _speakerOn.value = false
        _headsetConnected.value = false
    }

    private fun setDevice(device: AudioDeviceInfo, speaker: Boolean) {
        val ok = runCatching { audioManager?.setCommunicationDevice(device) == true }
            .onFailure { Log.w(TAG, "setCommunicationDevice falló: ${it.message}") }
            .getOrDefault(false)
        _speakerOn.value = ok && speaker
    }

    private fun findHeadset(): AudioDeviceInfo? {
        val devices = audioManager?.availableCommunicationDevices ?: return null
        return devices.firstOrNull { it.type in HEADSET_TYPES }
    }

    private companion object {
        const val TAG = "CallAudioRouter"
        val HEADSET_TYPES = setOf(
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_USB_HEADSET
        )
    }
}
