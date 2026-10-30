package com.Azelmods.App.webrtc

import android.content.Context
import android.util.Log
import com.Azelmods.App.data.model.IceCandidate
import com.Azelmods.App.util.CrashlyticsLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebRTCManager @Inject constructor(
    private val context: Context
) {
    
    private val TAG = "WebRTCManager"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // WebRTC components
    private var peerConnectionFactory: PeerConnectionFactory? = null
    private var peerConnection: PeerConnection? = null
    private var localVideoTrack: VideoTrack? = null
    private var localAudioTrack: AudioTrack? = null
    private var videoCapturer: CameraVideoCapturer? = null
    
    // EGL contexts — MUST keep references to prevent native crash from GC
    private var encoderEglBase: EglBase? = null
    private var decoderEglBase: EglBase? = null
    private var capturerEglBase: EglBase? = null
    
    // State flows
    private val _localVideoTrackFlow = MutableStateFlow<VideoTrack?>(null)
    val localVideoTrackFlow: StateFlow<VideoTrack?> = _localVideoTrackFlow.asStateFlow()
    
    private val _remoteVideoTrackFlow = MutableStateFlow<VideoTrack?>(null)
    val remoteVideoTrackFlow: StateFlow<VideoTrack?> = _remoteVideoTrackFlow.asStateFlow()
    
    private val _connectionState = MutableStateFlow<PeerConnection.PeerConnectionState?>(null)
    val connectionState: StateFlow<PeerConnection.PeerConnectionState?> = _connectionState.asStateFlow()

    // Estado del micrófono y la cámara. Vive aquí, en el singleton, y no en el
    // ViewModel de la pantalla: así el botón "Silenciar" de la notificación de la
    // llamada (CallService) y el de la pantalla actúan sobre la MISMA pista y se
    // ven el uno al otro. Antes la notificación solo cambiaba su propio icono y el
    // micrófono seguía abierto.
    private val _micEnabled = MutableStateFlow(true)
    val micEnabled: StateFlow<Boolean> = _micEnabled.asStateFlow()

    private val _cameraEnabled = MutableStateFlow(true)
    val cameraEnabled: StateFlow<Boolean> = _cameraEnabled.asStateFlow()

    /** Id de la llamada que tiene ahora mismo una PeerConnection viva (o null). */
    private val _activeCallId = MutableStateFlow<String?>(null)
    val activeCallId: StateFlow<String?> = _activeCallId.asStateFlow()

    /** Cámara frontal en uso (para decidir si se espeja la vista previa). */
    private val _isFrontCamera = MutableStateFlow(true)
    val isFrontCamera: StateFlow<Boolean> = _isFrontCamera.asStateFlow()
    
    // Callbacks
    var onIceCandidateListener: ((IceCandidate) -> Unit)? = null
    var onOfferCreatedListener: ((String) -> Unit)? = null
    var onAnswerCreatedListener: ((String) -> Unit)? = null

    // ── ICE candidate buffering ──
    // Remote ICE candidates that arrive BEFORE the remote description is set must be
    // queued, otherwise WebRTC drops them and the connection silently fails to establish.
    private val pendingIceCandidates = mutableListOf<org.webrtc.IceCandidate>()
    private var remoteDescriptionSet = false
    private val iceLock = Any()
    
    init {
        initializePeerConnectionFactory()
    }
    
    private fun initializePeerConnectionFactory() {
        val options = PeerConnectionFactory.InitializationOptions.builder(context)
            .setEnableInternalTracer(true)
            .setFieldTrials("WebRTC-H264HighProfile/Enabled/")
            .createInitializationOptions()
        
        PeerConnectionFactory.initialize(options)
        
        val encEgl = EglBase.create()
        encoderEglBase = encEgl
        val decEgl = EglBase.create()
        decoderEglBase = decEgl
        
        val encoderFactory = DefaultVideoEncoderFactory(
            encEgl.eglBaseContext,
            true,
            true
        )
        
        val decoderFactory = DefaultVideoDecoderFactory(decEgl.eglBaseContext)
        
        peerConnectionFactory = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
            .setOptions(PeerConnectionFactory.Options().apply {
                disableEncryption = false
                disableNetworkMonitor = false
            })
            .createPeerConnectionFactory()
        
        Log.d(TAG, "PeerConnectionFactory initialized")
    }
    
    /**
     * Crea la PeerConnection de una llamada.
     *
     * @param iceServers servidores STUN/TURN. Llegan de [IceServerProvider], que
     *   los lee de la configuración remota (`config/webrtc/iceServers`) para poder
     *   poner un TURN propio en producción sin publicar otra versión de la app.
     * @param relayOnly fuerza que TODO el medio pase por el TURN. Se usa con el
     *   modo anónimo activo: sin esto, WebRTC anuncia al otro participante las IP
     *   reales del dispositivo (candidatos host/srflx), que es justo lo que el modo
     *   anónimo pretende ocultar.
     */
    fun initializePeerConnection(
        isVideoCall: Boolean,
        callId: String? = null,
        iceServers: List<PeerConnection.IceServer> = IceServerProvider.DEFAULT_ICE_SERVERS,
        relayOnly: Boolean = false
    ) {
        // Este manager es un `@Singleton`: si la llamada anterior no se limpió
        // del todo (la pantalla murió sin pasar por endCall, por ejemplo), aquí
        // habría todavía una PeerConnection viva y un capturador con la cámara
        // abierta. Reasignar encima los dejaba huérfanos, y la SEGUNDA
        // videollamada de la sesión no conseguía abrir la cámara —ya ocupada por
        // el capturador de la primera— y se quedaba sin imagen.
        if (peerConnection != null || videoCapturer != null) {
            Log.w(TAG, "Quedaba una sesión WebRTC previa: se limpia antes de crear la nueva")
            cleanup()
        }

        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
            bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE
            rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE
            if (relayOnly) iceTransportsType = PeerConnection.IceTransportsType.RELAY
        }
        
        val observer = object : PeerConnection.Observer {
            override fun onIceCandidate(candidate: org.webrtc.IceCandidate?) {
                candidate?.let {
                    Log.d(TAG, "New ICE candidate: ${it.sdp}")
                    onIceCandidateListener?.invoke(
                        IceCandidate(
                            sdp = it.sdp,
                            sdpMid = it.sdpMid ?: "",
                            sdpMLineIndex = it.sdpMLineIndex,
                            userId = ""
                        )
                    )
                }
            }
            
            override fun onDataChannel(dataChannel: DataChannel?) {
                Log.d(TAG, "onDataChannel: ${dataChannel?.label()}")
            }
            
            override fun onIceConnectionChange(state: PeerConnection.IceConnectionState?) {
                Log.d(TAG, "ICE connection state: $state")
            }
            
            override fun onIceConnectionReceivingChange(receiving: Boolean) {
                Log.d(TAG, "ICE connection receiving: $receiving")
            }
            
            override fun onIceGatheringChange(state: PeerConnection.IceGatheringState?) {
                Log.d(TAG, "ICE gathering state: $state")
            }
            
            override fun onAddStream(stream: MediaStream?) {
                Log.d(TAG, "onAddStream: ${stream?.id}")
                stream?.videoTracks?.firstOrNull()?.let { remoteVideoTrack ->
                    _remoteVideoTrackFlow.value = remoteVideoTrack
                }
            }
            
            override fun onSignalingChange(state: PeerConnection.SignalingState?) {
                Log.d(TAG, "Signaling state: $state")
            }
            
            override fun onIceCandidatesRemoved(candidates: Array<out org.webrtc.IceCandidate>?) {
                Log.d(TAG, "ICE candidates removed: ${candidates?.size}")
            }
            
            override fun onRemoveStream(stream: MediaStream?) {
                Log.d(TAG, "onRemoveStream: ${stream?.id}")
            }
            
            override fun onRenegotiationNeeded() {
                Log.d(TAG, "onRenegotiationNeeded")
            }
            
            override fun onAddTrack(receiver: RtpReceiver?, streams: Array<out MediaStream>?) {
                Log.d(TAG, "onAddTrack: ${receiver?.track()?.kind()}")
                receiver?.track()?.let { track ->
                    if (track is VideoTrack) {
                        _remoteVideoTrackFlow.value = track
                    }
                }
            }
            
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState?) {
                Log.d(TAG, "Connection state: $newState")
                _connectionState.value = newState
            }
        }
        
        peerConnection = peerConnectionFactory?.createPeerConnection(rtcConfig, observer)
        
        // Reset per-call signaling state for the new connection.
        synchronized(iceLock) {
            remoteDescriptionSet = false
            pendingIceCandidates.clear()
        }
        _micEnabled.value = true
        _cameraEnabled.value = isVideoCall
        _isFrontCamera.value = true
        _activeCallId.value = callId
        
        // Create local media tracks
        createLocalMediaTracks(isVideoCall)
        
        Log.d(TAG, "PeerConnection initialized")
    }
    
    private fun createLocalMediaTracks(isVideoCall: Boolean) {
        // Audio track
        val audioConstraints = MediaConstraints().apply {
            mandatory.add(MediaConstraints.KeyValuePair("googEchoCancellation", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googAutoGainControl", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googHighpassFilter", "true"))
            mandatory.add(MediaConstraints.KeyValuePair("googNoiseSuppression", "true"))
        }
        
        val audioSource = peerConnectionFactory?.createAudioSource(audioConstraints)
        localAudioTrack = peerConnectionFactory?.createAudioTrack("audio_track", audioSource)
        
        // Video track (only for video calls)
        if (isVideoCall) {
            val videoSource = peerConnectionFactory?.createVideoSource(false)
            localVideoTrack = peerConnectionFactory?.createVideoTrack("video_track", videoSource)
            
            // Initialize camera
            val enumerator = Camera2Enumerator(context)
            val cameraName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) }
                ?: enumerator.deviceNames.firstOrNull()
            
            cameraName?.let {
                _isFrontCamera.value = enumerator.isFrontFacing(it)
                videoCapturer = enumerator.createCapturer(it, null) as? CameraVideoCapturer
                val capEgl = EglBase.create()
                capturerEglBase = capEgl
                videoCapturer?.initialize(
                    SurfaceTextureHelper.create("CaptureThread", capEgl.eglBaseContext),
                    context,
                    videoSource?.capturerObserver
                )
                videoCapturer?.startCapture(1280, 720, 30)
                
                _localVideoTrackFlow.value = localVideoTrack
            }
        }
        
        // Add tracks to peer connection
        val streamId = "local_stream"
        peerConnection?.addTrack(localAudioTrack, listOf(streamId))
        if (isVideoCall) {
            peerConnection?.addTrack(localVideoTrack, listOf(streamId))
        }
        
        Log.d(TAG, "Local media tracks created")
    }
    
    fun createOffer() {
        scope.launch {
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
            
            peerConnection?.createOffer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    sdp?.let {
                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                Log.d(TAG, "Local description set successfully")
                                onOfferCreatedListener?.invoke(it.description)
                            }
                            override fun onCreateFailure(error: String?) {
                                CrashlyticsLogger.warn("webrtc", "createOffer", "Failed to create local description: $error")
                                Log.e(TAG, "Failed to create local description: $error")
                            }
                            override fun onSetFailure(error: String?) {
                                CrashlyticsLogger.warn("webrtc", "setLocalDesc", "Failed: $error")
                                Log.e(TAG, "Failed to set local description: $error")
                            }
                        }, it)
                    }
                }
                
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) {
                    CrashlyticsLogger.warn("webrtc", "createOffer", "Failed: $error")
                    Log.e(TAG, "Failed to create offer: $error")
                }
                override fun onSetFailure(error: String?) {
                    CrashlyticsLogger.warn("webrtc", "setOffer", "Failed: $error")
                }
            }, constraints)
        }
    }
    
    fun createAnswer() {
        scope.launch {
            val constraints = MediaConstraints().apply {
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"))
                mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"))
            }
            
            peerConnection?.createAnswer(object : SdpObserver {
                override fun onCreateSuccess(sdp: SessionDescription?) {
                    sdp?.let {
                        peerConnection?.setLocalDescription(object : SdpObserver {
                            override fun onCreateSuccess(p0: SessionDescription?) {}
                            override fun onSetSuccess() {
                                Log.d(TAG, "Local description set successfully")
                                onAnswerCreatedListener?.invoke(it.description)
                            }
                            override fun onCreateFailure(error: String?) {
                                CrashlyticsLogger.warn("webrtc", "createAnswer", "Failed local desc: $error")
                                Log.e(TAG, "Failed to create local description: $error")
                            }
                            override fun onSetFailure(error: String?) {
                                CrashlyticsLogger.warn("webrtc", "setAnswer", "Failed: $error")
                                Log.e(TAG, "Failed to set local description: $error")
                            }
                        }, it)
                    }
                }
                
                override fun onSetSuccess() {}
                override fun onCreateFailure(error: String?) {
                    CrashlyticsLogger.warn("webrtc", "createAnswer", "Failed: $error")
                    Log.e(TAG, "Failed to create answer: $error")
                }
                override fun onSetFailure(error: String?) {
                    CrashlyticsLogger.warn("webrtc", "setAnswer", "Failed: $error")
                }
            }, constraints)
        }
    }
    
    /**
     * Aplica la descripción remota.
     *
     * Con una OFERTA, la respuesta se crea DENTRO de `onSetSuccess`. Antes el
     * ViewModel llamaba a `createAnswer()` justo después de `setRemoteDescription()`,
     * pero las dos operaciones son asíncronas: si `createAnswer` llegaba primero
     * WebRTC la rechazaba ("no remote description") y el receptor nunca respondía.
     * La llamada se quedaba en "Conectando…" para siempre en dispositivos rápidos.
     */
    fun setRemoteDescription(sdp: String, type: String) {
        val isOffer = type == "offer"
        val sessionDescription = SessionDescription(
            if (isOffer) SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER,
            sdp
        )

        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set successfully")
                // Flush any ICE candidates that arrived before the remote description was ready.
                flushPendingIceCandidates()
                if (isOffer) createAnswer()
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {
                CrashlyticsLogger.warn("webrtc", "setRemoteDesc", "Failed: $error")
                Log.e(TAG, "Failed to set remote description: $error")
            }
        }, sessionDescription)
    }

    private fun flushPendingIceCandidates() {
        synchronized(iceLock) {
            remoteDescriptionSet = true
            if (pendingIceCandidates.isNotEmpty()) {
                Log.d(TAG, "Flushing ${pendingIceCandidates.size} buffered ICE candidates")
                pendingIceCandidates.forEach { candidate ->
                    try {
                        peerConnection?.addIceCandidate(candidate)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to add buffered ICE candidate: ${e.message}")
                    }
                }
                pendingIceCandidates.clear()
            }
        }
    }
    
    fun addIceCandidate(candidate: IceCandidate) {
        val iceCandidate = org.webrtc.IceCandidate(
            candidate.sdpMid,
            candidate.sdpMLineIndex,
            candidate.sdp
        )
        synchronized(iceLock) {
            if (remoteDescriptionSet) {
                try {
                    peerConnection?.addIceCandidate(iceCandidate)
                    Log.d(TAG, "ICE candidate added")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to add ICE candidate: ${e.message}")
                }
            } else {
                // Buffer until setRemoteDescription completes.
                pendingIceCandidates.add(iceCandidate)
                Log.d(TAG, "ICE candidate buffered (remote description not set yet)")
            }
        }
    }
    
    fun toggleAudio(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
        _micEnabled.value = enabled
    }

    /** Invierte el micrófono (lo usa el botón de la notificación de la llamada). */
    fun toggleMic() = toggleAudio(!_micEnabled.value)

    fun toggleVideo(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
        _cameraEnabled.value = enabled
    }

    fun switchCamera() {
        videoCapturer?.switchCamera(object : CameraVideoCapturer.CameraSwitchHandler {
            override fun onCameraSwitchDone(isFrontCamera: Boolean) {
                _isFrontCamera.value = isFrontCamera
            }
            override fun onCameraSwitchError(error: String?) {
                Log.w(TAG, "No se pudo cambiar de cámara: $error")
            }
        })
    }
    
    /**
     * Libera todo lo de la llamada actual.
     *
     * El ORDEN importa y no es el que había. Antes se destruían las pistas y el
     * capturador primero y sólo al final se ponían los `StateFlow` a null: entre
     * una cosa y otra la interfaz seguía suscrita a una `VideoTrack` ya
     * destruida, y pintar sobre un objeto nativo liberado es un cierre inmediato
     * de la app, no una excepción que estos `try/catch` puedan recoger.
     *
     * Ahora se avisa primero a la interfaz (flows a null → los renderers sueltan
     * sus sinks), después se cierra la PeerConnection y sólo entonces se destruye
     * lo que colgaba de ella.
     */
    fun cleanup() {
        // 1) La interfaz suelta las pistas ANTES de que dejen de existir.
        _localVideoTrackFlow.value = null
        _remoteVideoTrackFlow.value = null
        _connectionState.value = null
        _activeCallId.value = null

        // 2) Se corta la captura de cámara.
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping video capture: ${e.message}")
        }

        // 3) Se cierra la conexión, que es quien referencia las pistas.
        try {
            peerConnection?.close()
            peerConnection?.dispose()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing peer connection: ${e.message}")
        }
        peerConnection = null

        // 4) Ya sin dueños, se destruyen capturador y pistas.
        try {
            videoCapturer?.dispose()
        } catch (e: Exception) {
            Log.w(TAG, "Error disposing video capturer: ${e.message}")
        }
        videoCapturer = null

        try {
            localVideoTrack?.dispose()
        } catch (e: Exception) {
            Log.w(TAG, "Error disposing local video track: ${e.message}")
        }
        try {
            localAudioTrack?.dispose()
        } catch (e: Exception) {
            Log.w(TAG, "Error disposing local audio track: ${e.message}")
        }
        localVideoTrack = null
        localAudioTrack = null

        // 5) Contexto EGL del capturador.
        try {
            capturerEglBase?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing capturer EglBase: ${e.message}")
        }
        capturerEglBase = null

        synchronized(iceLock) {
            remoteDescriptionSet = false
            pendingIceCandidates.clear()
        }
        _micEnabled.value = true
        _cameraEnabled.value = true

        Log.d(TAG, "WebRTC cleaned up")
    }
}
