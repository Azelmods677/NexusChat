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
    
    // Callbacks
    var onIceCandidateListener: ((IceCandidate) -> Unit)? = null
    var onOfferCreatedListener: ((String) -> Unit)? = null
    var onAnswerCreatedListener: ((String) -> Unit)? = null
    
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
    
    fun initializePeerConnection(isVideoCall: Boolean) {
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer()
        )
        
        val rtcConfig = PeerConnection.RTCConfiguration(iceServers).apply {
            sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN
            continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
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
    
    fun setRemoteDescription(sdp: String, type: String) {
        val sessionDescription = SessionDescription(
            if (type == "offer") SessionDescription.Type.OFFER else SessionDescription.Type.ANSWER,
            sdp
        )
        
        peerConnection?.setRemoteDescription(object : SdpObserver {
            override fun onCreateSuccess(p0: SessionDescription?) {}
            override fun onSetSuccess() {
                Log.d(TAG, "Remote description set successfully")
            }
            override fun onCreateFailure(error: String?) {}
            override fun onSetFailure(error: String?) {
                Log.e(TAG, "Failed to set remote description: $error")
            }
        }, sessionDescription)
    }
    
    fun addIceCandidate(candidate: IceCandidate) {
        val iceCandidate = org.webrtc.IceCandidate(
            candidate.sdpMid,
            candidate.sdpMLineIndex,
            candidate.sdp
        )
        peerConnection?.addIceCandidate(iceCandidate)
        Log.d(TAG, "ICE candidate added")
    }
    
    fun toggleAudio(enabled: Boolean) {
        localAudioTrack?.setEnabled(enabled)
    }
    
    fun toggleVideo(enabled: Boolean) {
        localVideoTrack?.setEnabled(enabled)
    }
    
    fun switchCamera() {
        videoCapturer?.switchCamera(null)
    }
    
    fun cleanup() {
        try {
            videoCapturer?.stopCapture()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping video capture: ${e.message}")
        }
        try {
            videoCapturer?.dispose()
        } catch (e: Exception) {
            Log.w(TAG, "Error disposing video capturer: ${e.message}")
        }
        
        localVideoTrack?.dispose()
        localAudioTrack?.dispose()
        
        peerConnection?.close()
        peerConnection?.dispose()
        
        encoderEglBase?.release()
        encoderEglBase = null
        decoderEglBase?.release()
        decoderEglBase = null
        capturerEglBase?.release()
        capturerEglBase = null
        
        _localVideoTrackFlow.value = null
        _remoteVideoTrackFlow.value = null
        _connectionState.value = null
        
        Log.d(TAG, "WebRTC cleaned up")
    }
}
