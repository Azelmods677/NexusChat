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
    
    fun loadContactProfile(contactId: String) {
        viewModelScope.launch {
            try {
                val userData = databaseRepository.getUserById(contactId)
                
                if (userData != null) {
                    val user = User(
                        uid = userData["uid"] as? String ?: contactId,
                        name = userData["displayName"] as? String ?: userData["name"] as? String ?: "Unknown",
                        username = userData["username"] as? String ?: "",
                        email = userData["email"] as? String ?: "",
                        photoUrl = userData["photoUrl"] as? String,
                        bio = userData["bio"] as? String ?: "",
                        isOnline = userData["isOnline"] as? Boolean ?: false,
                        lastSeen = userData["lastSeen"] as? Long ?: 0L
                    )
                    
                    _contactProfile.value = user
                }
            } catch (e: Exception) {
                e.printStackTrace()
                // Set default user on error
                _contactProfile.value = User(
                    uid = contactId,
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
                    "callerName" to (currentUser?.get("name") as? String ?: "Unknown"),
                    "callerPhotoUrl" to (currentUser?.get("photoUrl") as? String ?: ""),
                    "receiverId" to contactId,
                    "receiverName" to (contactUser?.get("name") as? String ?: "Unknown"),
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
                startCallService(callId, callType, contactUser?.get("name") as? String ?: "Unknown")
                
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
        viewModelScope.launch {
            try {
                currentCallId = callId
                
                // Update call status
                databaseRepository.updateCallStatus(callId, CallStatus.ACCEPTED.name)
                
                // Initialize WebRTC
                webRTCManager.initializePeerConnection(callType == CallType.VIDEO)
                
                // Start foreground service
                val contactName = _contactProfile.value?.name ?: "Unknown"
                startCallService(callId, callType, contactName)
                
                // Listen to call updates
                listenToCallUpdates(callId)
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
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
                    if (status == CallStatus.ENDED.name || status == CallStatus.DECLINED.name) {
                        endCall()
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
