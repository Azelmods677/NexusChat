package com.Azelmods.App.ui.screens.call

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.model.CallData
import com.Azelmods.App.data.model.CallStatus
import com.Azelmods.App.data.model.CallType
import com.Azelmods.App.data.model.User
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.service.NotificationHelper
import com.Azelmods.App.services.CallService
import com.Azelmods.App.webrtc.WebRTCManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack
import javax.inject.Inject

@HiltViewModel
class CallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseRepository: RealtimeDatabaseRepository,
    private val webRTCManager: WebRTCManager
) : ViewModel() {
    
    // Contact profile state for call screens
    private val _contactProfile = MutableStateFlow<User?>(null)
    val contactProfile: StateFlow<User?> = _contactProfile.asStateFlow()
    
    // Call state
    private val _callData = MutableStateFlow<CallData?>(null)
    val callData: StateFlow<CallData?> = _callData.asStateFlow()
    
    private val _isAudioEnabled = MutableStateFlow(true)
    val isAudioEnabled: StateFlow<Boolean> = _isAudioEnabled.asStateFlow()
    
    private val _isVideoEnabled = MutableStateFlow(true)
    val isVideoEnabled: StateFlow<Boolean> = _isVideoEnabled.asStateFlow()
    
    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn: StateFlow<Boolean> = _isSpeakerOn.asStateFlow()
    
    // WebRTC video tracks
    val localVideoTrack: StateFlow<VideoTrack?> = webRTCManager.localVideoTrackFlow
    val remoteVideoTrack: StateFlow<VideoTrack?> = webRTCManager.remoteVideoTrackFlow
    val connectionState = webRTCManager.connectionState
    
    private var currentCallId: String? = null
    /**
     * Indica si la llamada fue aceptada en algún momento.
     * Si el estado cambia a ENDED sin que esto sea true, es una llamada perdida.
     */
    private var wasAccepted = false
    /** Nombre de quien llama (guardado para notificación de llamada perdida) */
    private var callStartCallerName = ""
    /** Foto de quien llama (guardada para notificación de llamada perdida) */
    private var callStartCallerPhotoUrl: String? = null
    /** ID del contacto para devolver la llamada */
    private var callStartOtherUserId: String? = null
    /** Si este ViewModel está actuando como receptor (para detectar llamadas perdidas) */
    private var isReceiverSide = false
    
    init {
        setupWebRTCCallbacks()
    }
    
    private fun setupWebRTCCallbacks() {
        webRTCManager.onIceCandidateListener = { candidate ->
            currentCallId?.let { callId ->
                viewModelScope.launch {
                    try {
                        val candidateMap = mapOf(
                            "sdp" to candidate.sdp,
                            "sdpMid" to candidate.sdpMid,
                            "sdpMLineIndex" to candidate.sdpMLineIndex,
                            "userId" to (FirebaseAuth.getInstance().currentUser?.uid ?: "")
                        )
                        databaseRepository.addIceCandidate(callId, candidateMap)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        webRTCManager.onOfferCreatedListener = { offer ->
            currentCallId?.let { callId ->
                viewModelScope.launch {
                    try {
                        databaseRepository.setCallOffer(callId, offer)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
        
        webRTCManager.onAnswerCreatedListener = { answer ->
            currentCallId?.let { callId ->
                viewModelScope.launch {
                    try {
                        databaseRepository.setCallAnswer(callId, answer)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
    
    /**
     * Load contact profile from a call ID.
     * First fetches the call data from Firebase to extract the other participant's ID,
     * then loads their full profile.
     * Almacena [callStartCallerName] y [callStartCallerPhotoUrl] para usarlos
     * en la notificación de llamada perdida.
     */
    fun loadContactProfileFromCall(callId: String) {
        viewModelScope.launch {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                // Step 1: Get call data to find who the caller/receiver is
                val callData = databaseRepository.getCallData(callId)
                if (callData == null) {
                    _contactProfile.value = User(
                        uid = callId,
                        name = "Unknown",
                        username = "",
                        email = ""
                    )
                    return@launch
                }

                val callerId     = callData["callerId"] as? String ?: ""
                val receiverId   = callData["receiverId"] as? String ?: ""
                val callerName   = callData["callerName"] as? String ?: ""
                val callerPhoto  = callData["callerPhotoUrl"] as? String

                // Store the other user's ID for "Call back" action
                callStartOtherUserId = if (currentUserId == callerId && receiverId.isNotBlank()) {
                    receiverId
                } else if (currentUserId == receiverId && callerId.isNotBlank()) {
                    callerId
                } else null

                // Store caller info for missed call notification
                callStartCallerName = callerName.ifBlank { "Unknown" }
                callStartCallerPhotoUrl = callerPhoto

                // Step 2: Determine the "other" participant ID
                val contactId = when {
                    currentUserId == callerId && receiverId.isNotBlank() -> receiverId
                    currentUserId == receiverId && callerId.isNotBlank() -> callerId
                    else -> {
                        // Can't determine, use callerName as fallback
                        _contactProfile.value = User(
                            uid = callId,
                            name = callStartCallerName,
                            username = "",
                            email = ""
                        )
                        return@launch
                    }
                }

                // Step 3: Fetch full user profile
                val userData = databaseRepository.getUserById(contactId)
                if (userData != null) {
                    _contactProfile.value = User(
                        uid = userData["uid"] as? String ?: contactId,
                        name = userData["displayName"] as? String ?: userData["name"] as? String ?: callStartCallerName,
                        username = userData["username"] as? String ?: "",
                        email = userData["email"] as? String ?: "",
                        photoUrl = userData["photoUrl"] as? String,
                        bio = userData["bio"] as? String ?: "",
                        isOnline = userData["isOnline"] as? Boolean ?: false,
                        lastSeen = userData["lastSeen"] as? Long ?: 0L
                    )
                } else {
                    _contactProfile.value = User(
                        uid = contactId,
                        name = callStartCallerName,
                        username = "",
                        email = ""
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _contactProfile.value = User(
                    uid = callId,
                    name = "Unknown",
                    username = "",
                    email = ""
                )
            }
        }
    }
    
    /**
     * Start a new call (caller side)
     */
    fun startCall(contactId: String, callType: CallType) {
        viewModelScope.launch {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                    ?: throw Exception("User not authenticated")
                
                val currentUser = databaseRepository.getUserById(currentUserId)
                val contactUser = databaseRepository.getUserById(contactId)
                
                // Create call data
                val callData = mapOf(
                    "callerId" to currentUserId,
                    "callerName" to (currentUser?.get("name") as? String ?: "Anónimo"),
                    "callerPhotoUrl" to (currentUser?.get("photoUrl") as? String ?: ""),
                    "receiverId" to contactId,
                    "receiverName" to (contactUser?.get("name") as? String ?: "Anónimo"),
                    "receiverPhotoUrl" to (contactUser?.get("photoUrl") as? String ?: ""),
                    "callType" to callType.name,
                    "status" to CallStatus.CALLING.name,
                    "startTime" to System.currentTimeMillis()
                )
                
                // Create call in Firebase
                val callId = databaseRepository.createCall(callData)
                currentCallId = callId
                
                // Initialize WebRTC
                webRTCManager.initializePeerConnection(callType == CallType.VIDEO)
                
                // Start foreground service
                startCallService(callId, callType, contactUser?.get("name") as? String ?: "Anónimo")
                
                // Listen to call updates
                listenToCallUpdates(callId)
                
                // Create offer
                webRTCManager.createOffer()
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Accept incoming call (receiver side)
     */
    fun acceptCall(callId: String, callType: CallType) {
        wasAccepted = true
        viewModelScope.launch {
            try {
                currentCallId = callId

                // Update call status
                databaseRepository.updateCallStatus(callId, CallStatus.ACCEPTED.name)

                // Initialize WebRTC
                webRTCManager.initializePeerConnection(callType == CallType.VIDEO)

                // Start foreground service
                val contactName = _contactProfile.value?.name ?: "Anónimo"
                startCallService(callId, callType, contactName)

                // Only start listener if observeIncomingCall() didn't already start it
                if (!isReceiverSide) {
                    listenToCallUpdates(callId)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Start listening to an incoming call BEFORE accepting it.
     * Esto permite detectar si quien llama cuelga antes de que respondamos
     * y mostrar la notificación de llamada perdida.
     *
     * Debe llamarse desde IncomingCallScreen al cargar el perfil.
     */
    fun observeIncomingCall(callId: String) {
        isReceiverSide = true
        listenToCallUpdates(callId)
    }

    /**
     * Escucha los cambios en la llamada.
     *
     * @param isReceiverSide Si es true, detecta llamadas perdidas
     *                       (cuando status cambia a ENDED sin haber sido ACCEPTED).
     */
    private fun listenToCallUpdates(callId: String) {
        viewModelScope.launch {
            databaseRepository.listenToCall(callId).collect { callData ->
                callData?.let { data ->
                    // Handle offer
                    val offer = data["offer"] as? String
                    if (offer != null && webRTCManager.connectionState.value == null) {
                        webRTCManager.setRemoteDescription(offer, "offer")
                        webRTCManager.createAnswer()
                    }

                    // Handle answer
                    val answer = data["answer"] as? String
                    if (answer != null) {
                        webRTCManager.setRemoteDescription(answer, "answer")
                    }

                    // Handle status changes
                    val status = data["status"] as? String
                    when (status) {
                        CallStatus.ACCEPTED.name -> {
                            wasAccepted = true
                        }
                        CallStatus.ENDED.name -> {
                            // ── Detectar llamada perdida ──
                            if (isReceiverSide && !wasAccepted) {
                                NotificationHelper.showMissedCallNotification(
                                    context = context,
                                    callId = callId,
                                    callerName = callStartCallerName.ifBlank { "Unknown" },
                                    callerPhotoUrl = callStartCallerPhotoUrl,
                                    callerId = callStartOtherUserId
                                )
                            }
                            endCall()
                        }
                        CallStatus.DECLINED.name -> {
                            // Si el receptor rechazó activamente, no es "perdida"
                            // pero aún así limpiamos
                            endCall()
                        }
                    }
                }
            }
        }

        // Listen to ICE candidates
        viewModelScope.launch {
            databaseRepository.listenToIceCandidates(callId).collect { candidates ->
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                candidates.forEach { candidateData ->
                    val userId = candidateData["userId"] as? String
                    // Only add candidates from the other user
                    if (userId != currentUserId) {
                        val candidate = com.Azelmods.App.data.model.IceCandidate(
                            sdp = candidateData["sdp"] as? String ?: "",
                            sdpMid = candidateData["sdpMid"] as? String ?: "",
                            sdpMLineIndex = (candidateData["sdpMLineIndex"] as? Long)?.toInt() ?: 0,
                            userId = userId ?: ""
                        )
                        webRTCManager.addIceCandidate(candidate)
                    }
                }
            }
        }
    }
    
    /**
     * End call
     */
    /**
     * Decline incoming call (receiver side)
     */
    fun declineCall(callId: String) {
        viewModelScope.launch {
            try {
                databaseRepository.updateCallStatus(callId, CallStatus.DECLINED.name)
                val endData = mapOf(
                    "endTime" to com.google.firebase.database.ServerValue.TIMESTAMP
                )
                databaseRepository.updateCallStatus(callId, "DECLINED")
                currentCallId = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun endCall() {
        viewModelScope.launch {
            try {
                currentCallId?.let { callId ->
                    databaseRepository.endCall(callId)
                }
                
                // Stop foreground service
                stopCallService()
                
                // Cleanup WebRTC
                webRTCManager.cleanup()
                
                currentCallId = null
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Toggle audio on/off
     */
    fun toggleAudio() {
        val newState = !_isAudioEnabled.value
        _isAudioEnabled.value = newState
        webRTCManager.toggleAudio(newState)
    }
    
    /**
     * Toggle video on/off
     */
    fun toggleVideo() {
        val newState = !_isVideoEnabled.value
        _isVideoEnabled.value = newState
        webRTCManager.toggleVideo(newState)
    }
    
    /**
     * Switch camera (front/back)
     */
    fun switchCamera() {
        webRTCManager.switchCamera()
    }
    
    /**
     * Toggle speaker on/off
     */
    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
        // TODO: Implement audio routing
    }
    
    private fun startCallService(callId: String, callType: CallType, contactName: String) {
        val intent = Intent(context, CallService::class.java).apply {
            action = CallService.ACTION_START_CALL
            putExtra(CallService.EXTRA_CALL_ID, callId)
            putExtra(CallService.EXTRA_CALL_TYPE, callType.name.lowercase())
            putExtra(CallService.EXTRA_CONTACT_NAME, contactName)
        }
        context.startForegroundService(intent)
    }
    
    private fun stopCallService() {
        val intent = Intent(context, CallService::class.java).apply {
            action = CallService.ACTION_END_CALL
        }
        context.startService(intent)
    }
    
    override fun onCleared() {
        super.onCleared()
        webRTCManager.cleanup()
    }
}
