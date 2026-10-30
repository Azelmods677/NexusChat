package com.Azelmods.App.ui.screens.call

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Azelmods.App.data.model.CallStatus
import com.Azelmods.App.data.model.CallType
import com.Azelmods.App.data.model.User
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.data.security.tor.TorService
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.service.CallNotifications
import com.Azelmods.App.service.NotificationHelper
import com.Azelmods.App.services.CallService
import com.Azelmods.App.webrtc.CallAudioRouter
import com.Azelmods.App.webrtc.IceServerProvider
import com.Azelmods.App.webrtc.WebRTCManager
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.webrtc.PeerConnection
import org.webrtc.VideoTrack
import javax.inject.Inject

/** En qué punto está la llamada, para que la interfaz diga algo honesto. */
enum class CallPhase {
    /** Preparando: creando la llamada, pidiendo servidores ICE… */
    STARTING,
    /** Emisor: sonando en el otro teléfono. */
    RINGING,
    /** Aceptada: negociando la conexión de medios. */
    CONNECTING,
    /** Audio/vídeo fluyendo. */
    CONNECTED,
    /** Se perdió la red un momento; WebRTC intenta recuperarla. */
    RECONNECTING,
    ENDED
}

/** Por qué terminó: decide el texto final y si hay que escribir en la base de datos. */
enum class CallEndReason {
    HANGUP, REMOTE_HANGUP, DECLINED, NO_ANSWER, CONNECTION_LOST, UNAVAILABLE, ERROR;

    val label: String
        get() = when (this) {
            HANGUP -> "Llamada finalizada"
            REMOTE_HANGUP -> "El contacto colgó"
            DECLINED -> "Llamada rechazada"
            NO_ANSWER -> "Sin respuesta"
            CONNECTION_LOST -> "Se perdió la conexión"
            UNAVAILABLE -> "La llamada ya no está disponible"
            ERROR -> "No se pudo completar la llamada"
        }
}

@HiltViewModel
class CallViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val databaseRepository: RealtimeDatabaseRepository,
    private val webRTCManager: WebRTCManager,
    private val audioRouter: CallAudioRouter,
    private val iceServerProvider: IceServerProvider,
    private val torService: TorService
) : ViewModel() {

    private companion object {
        const val TAG = "CallViewModel"

        /** Tiempo que suena una llamada antes de darse por perdida. */
        val RINGING_TIMEOUT_MS = CallNotifications.RING_TIMEOUT_MS - 5_000L

        /** Margen para que WebRTC recupere una conexión caída antes de colgar. */
        const val RECONNECT_GRACE_MS = 20_000L

        /**
         * Ámbito que sobrevive al ViewModel: la escritura del fin de la llamada
         * debe completarse aunque la pantalla ya se haya cerrado.
         */
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }

    // ── Estado para la interfaz ──────────────────────────────────────────
    private val _contactProfile = MutableStateFlow<User?>(null)
    val contactProfile: StateFlow<User?> = _contactProfile.asStateFlow()

    private val _phase = MutableStateFlow(CallPhase.STARTING)
    val phase: StateFlow<CallPhase> = _phase.asStateFlow()

    private val _endReason = MutableStateFlow<CallEndReason?>(null)
    val endReason: StateFlow<CallEndReason?> = _endReason.asStateFlow()

    /** `SystemClock.elapsedRealtime()` del momento en que el medio conectó. */
    private val _connectedAt = MutableStateFlow<Long?>(null)
    val connectedAt: StateFlow<Long?> = _connectedAt.asStateFlow()

    /** true si el medio va forzado por TURN para no revelar la IP (modo anónimo). */
    private val _isRelayOnly = MutableStateFlow(false)
    val isRelayOnly: StateFlow<Boolean> = _isRelayOnly.asStateFlow()

    val isAudioEnabled: StateFlow<Boolean> = webRTCManager.micEnabled
    val isVideoEnabled: StateFlow<Boolean> = webRTCManager.cameraEnabled
    val isSpeakerOn: StateFlow<Boolean> = audioRouter.speakerOn
    val isFrontCamera: StateFlow<Boolean> = webRTCManager.isFrontCamera

    val localVideoTrack: StateFlow<VideoTrack?> = webRTCManager.localVideoTrackFlow
    val remoteVideoTrack: StateFlow<VideoTrack?> = webRTCManager.remoteVideoTrackFlow
    val connectionState = webRTCManager.connectionState

    // ── Estado interno ────────────────────────────────────────────────────
    private var currentCallId: String? = null
    private var isVideoCall = false

    /** Evita arrancar dos veces (recomposición, vuelta a la pantalla…). */
    private var started = false

    /** Si la llamada llegó a aceptarse (para distinguir las perdidas). */
    private var wasAccepted = false

    private var callStartCallerName = ""
    private var callStartCallerPhotoUrl: String? = null
    private var callStartOtherUserId: String? = null

    /** Pantalla de llamada entrante: solo observa, no tiene conexión propia. */
    private var isReceiverSide = false

    /**
     * true  -> este ViewModel inició la llamada (caller): consume el `answer`.
     * false -> este ViewModel contesta la llamada (callee): consume el `offer`.
     */
    private var isCaller = false

    /**
     * true cuando este ViewModel ha creado realmente una PeerConnection WebRTC
     * (startCall o acceptCall). Sólo en ese caso debe limpiar el WebRTCManager
     * compartido al destruirse, para no romper la llamada de otra pantalla.
     */
    private var ownsWebRTCSession = false
    private var remoteOfferHandled = false
    private var remoteAnswerHandled = false

    private var ringingTimeoutJob: Job? = null
    private var reconnectJob: Job? = null
    private var listenersStarted = false

    private fun myUid(): String = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    /**
     * Registra los callbacks de señalización en el [WebRTCManager].
     *
     * NO se llama desde `init`. El WebRTCManager es un `@Singleton` con un único
     * juego de listeners, y cada pantalla de llamada crea su propio
     * `CallViewModel`: si todos los registraran al construirse, el ViewModel de
     * la pantalla de llamada entrante —que sólo observa— pisaría los del
     * ViewModel que sí conduce la conexión, y la oferta, la respuesta y los
     * candidatos ICE dejarían de escribirse en Firebase.
     */
    private fun setupWebRTCCallbacks() {
        webRTCManager.onIceCandidateListener = { candidate ->
            currentCallId?.let { callId ->
                appScope.launch {
                    runCatching {
                        databaseRepository.addIceCandidate(
                            callId,
                            mapOf(
                                "sdp" to candidate.sdp,
                                "sdpMid" to candidate.sdpMid,
                                "sdpMLineIndex" to candidate.sdpMLineIndex,
                                "userId" to myUid()
                            )
                        )
                    }.onFailure { Log.w(TAG, "No se pudo enviar un candidato ICE: ${it.message}") }
                }
            }
        }
        webRTCManager.onOfferCreatedListener = { offer ->
            currentCallId?.let { callId ->
                appScope.launch {
                    runCatching { databaseRepository.setCallOffer(callId, offer) }
                        .onFailure { Log.e(TAG, "No se pudo enviar la oferta", it) }
                }
            }
        }
        webRTCManager.onAnswerCreatedListener = { answer ->
            currentCallId?.let { callId ->
                appScope.launch {
                    runCatching { databaseRepository.setCallAnswer(callId, answer) }
                        .onFailure { Log.e(TAG, "No se pudo enviar la respuesta", it) }
                }
            }
        }
    }

    // ── Perfil del contacto ───────────────────────────────────────────────

    /**
     * Carga el perfil del otro participante a partir del id de la llamada y
     * guarda nombre/foto para la notificación de llamada perdida.
     */
    fun loadContactProfileFromCall(callId: String) {
        viewModelScope.launch { resolveContactFromCall(callId) }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun resolveContactFromCall(callId: String): Map<String, Any>? {
        return try {
            val currentUserId = myUid()
            val callData = databaseRepository.getCallData(callId)
            if (callData == null) {
                if (_contactProfile.value == null) {
                    _contactProfile.value = User(uid = "", name = "Desconocido", username = "", email = "")
                }
                return null
            }

            val callerId = callData["callerId"] as? String ?: ""
            val receiverId = callData["receiverId"] as? String ?: ""
            callStartCallerName = (callData["callerName"] as? String).orEmpty().ifBlank { "Desconocido" }
            callStartCallerPhotoUrl = (callData["callerPhotoUrl"] as? String)?.takeIf { it.isNotBlank() }

            val contactId = when (currentUserId) {
                callerId -> receiverId
                receiverId -> callerId
                else -> ""
            }.takeIf { it.isNotBlank() }
            callStartOtherUserId = contactId

            val userData = contactId?.let { databaseRepository.getUserById(it) }
            _contactProfile.value = if (userData != null) {
                userData.toUser(contactId.orEmpty())
            } else {
                User(
                    uid = contactId.orEmpty(),
                    name = callStartCallerName,
                    username = "",
                    email = "",
                    photoUrl = callStartCallerPhotoUrl
                )
            }
            callData
        } catch (e: Exception) {
            Log.e(TAG, "No se pudo cargar el contacto de la llamada", e)
            if (_contactProfile.value == null) {
                _contactProfile.value = User(uid = "", name = "Desconocido", username = "", email = "")
            }
            null
        }
    }

    private fun Map<String, Any>.toUser(fallbackId: String): User = User(
        uid = this["uid"] as? String ?: fallbackId,
        name = displayNameOf(this) ?: "Anónimo",
        username = this["username"] as? String ?: "",
        email = this["email"] as? String ?: "",
        photoUrl = (this["photoUrl"] as? String)?.takeIf { it.isNotBlank() },
        bio = this["bio"] as? String ?: "",
        isOnline = this["isOnline"] as? Boolean ?: false,
        lastSeen = (this["lastSeen"] as? Number)?.toLong() ?: 0L
    )

    private fun displayNameOf(data: Map<String, Any>?): String? =
        (data?.get("displayName") as? String)?.takeIf { it.isNotBlank() }
            ?: (data?.get("name") as? String)?.takeIf { it.isNotBlank() }
            ?: (data?.get("username") as? String)?.takeIf { it.isNotBlank() }

    // ── Emisor ────────────────────────────────────────────────────────────

    fun startCall(contactId: String, callType: CallType) {
        if (started) return
        started = true
        isCaller = true
        ownsWebRTCSession = true
        isVideoCall = callType == CallType.VIDEO
        _phase.value = CallPhase.STARTING
        setupWebRTCCallbacks()

        viewModelScope.launch {
            try {
                val currentUserId = myUid().ifBlank { throw IllegalStateException("Sin sesión") }

                val currentUser = databaseRepository.getUserById(currentUserId)
                val contactUser = databaseRepository.getUserById(contactId)
                if (contactUser != null) _contactProfile.value = contactUser.toUser(contactId)
                val contactName = displayNameOf(contactUser) ?: "Anónimo"

                val callData = mapOf(
                    "callerId" to currentUserId,
                    "callerName" to (displayNameOf(currentUser) ?: "Anónimo"),
                    "callerPhotoUrl" to (currentUser?.get("photoUrl") as? String ?: ""),
                    "receiverId" to contactId,
                    "receiverName" to contactName,
                    "receiverPhotoUrl" to (contactUser?.get("photoUrl") as? String ?: ""),
                    "callType" to callType.name,
                    "status" to CallStatus.CALLING.name,
                    "startTime" to System.currentTimeMillis()
                )

                val callId = databaseRepository.createCall(callData)
                currentCallId = callId

                // WebRTC ANTES de escuchar: la conexión debe existir cuando lleguen
                // la respuesta y los candidatos del otro lado.
                preparePeerConnection(callId)
                startCallService(callId, callType, contactName)
                listenToCallUpdates(callId)
                webRTCManager.createOffer()

                if (_endReason.value == null) _phase.value = CallPhase.RINGING
                startRingingTimeout()
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo iniciar la llamada", e)
                endCall(CallEndReason.ERROR)
            }
        }
    }

    /**
     * Cuelga si nadie contesta. Sin esto, una llamada a alguien con el teléfono
     * apagado dejaba al emisor sonando indefinidamente con el micrófono abierto.
     */
    private fun startRingingTimeout() {
        ringingTimeoutJob?.cancel()
        ringingTimeoutJob = viewModelScope.launch {
            delay(RINGING_TIMEOUT_MS)
            if (!wasAccepted && _endReason.value == null) {
                Log.d(TAG, "Sin respuesta tras ${RINGING_TIMEOUT_MS / 1000}s: se cuelga")
                endCall(CallEndReason.NO_ANSWER)
            }
        }
    }

    // ── Receptor ──────────────────────────────────────────────────────────

    fun acceptCall(callId: String, callType: CallType) {
        if (started) return
        started = true
        wasAccepted = true
        isCaller = false
        ownsWebRTCSession = true
        isVideoCall = callType == CallType.VIDEO
        // currentCallId ANTES de registrar los callbacks: si un candidato ICE se
        // generase entre ambas líneas, el listener lo descartaría en silencio.
        currentCallId = callId
        _phase.value = CallPhase.CONNECTING
        setupWebRTCCallbacks()
        CallNotifications.cancelIncoming(context, callId)

        viewModelScope.launch {
            try {
                val callData = resolveContactFromCall(callId)
                val status = (callData?.get("status") as? String)?.uppercase()
                if (callData == null || status in setOf("ENDED", "DECLINED", "MISSED", "FAILED")) {
                    // El emisor colgó mientras se pulsaba "Aceptar".
                    endCall(CallEndReason.UNAVAILABLE)
                    return@launch
                }

                databaseRepository.updateCallStatus(callId, CallStatus.ACCEPTED.name)
                preparePeerConnection(callId)
                startCallService(callId, callType, _contactProfile.value?.name ?: callStartCallerName)
                listenToCallUpdates(callId)
            } catch (e: Exception) {
                Log.e(TAG, "No se pudo contestar la llamada", e)
                endCall(CallEndReason.ERROR)
            }
        }
    }

    /**
     * La pantalla de llamada entrante escucha la llamada ANTES de contestar para
     * cerrarse (y avisar de la perdida) si quien llama cuelga.
     */
    fun observeIncomingCall(callId: String) {
        if (isReceiverSide) return
        isReceiverSide = true
        currentCallId = callId
        listenToCallUpdates(callId)
    }

    fun declineCall(callId: String) {
        if (_endReason.value != null) return
        _endReason.value = CallEndReason.DECLINED
        _phase.value = CallPhase.ENDED
        CallNotifications.cancelIncoming(context, callId)
        appScope.launch {
            runCatching { databaseRepository.updateCallStatus(callId, CallStatus.DECLINED.name) }
                .onFailure { Log.w(TAG, "No se pudo rechazar la llamada: ${it.message}") }
        }
    }

    // ── Conexión ──────────────────────────────────────────────────────────

    private suspend fun preparePeerConnection(callId: String) {
        val servers = iceServerProvider.getIceServers()
        // Modo anónimo (Tor) activo → todo el medio por TURN, para que el otro
        // participante no vea la IP real. Solo si hay TURN: sin él no conectaría.
        val anonymous = torService.torState.value is TorState.Connected
        val relayOnly = anonymous && iceServerProvider.hasTurn(servers)
        _isRelayOnly.value = relayOnly

        withContext(Dispatchers.Main) {
            webRTCManager.initializePeerConnection(isVideoCall, callId, servers, relayOnly)
            audioRouter.start(isVideoCall)
        }
        observeConnectionState()
    }

    private fun observeConnectionState() {
        viewModelScope.launch {
            webRTCManager.connectionState.collect { state ->
                if (_endReason.value != null || state == null) return@collect
                when (state) {
                    PeerConnection.PeerConnectionState.CONNECTED -> {
                        reconnectJob?.cancel()
                        _phase.value = CallPhase.CONNECTED
                        if (_connectedAt.value == null) {
                            _connectedAt.value = SystemClock.elapsedRealtime()
                            currentCallId?.let { id ->
                                appScope.launch { runCatching { databaseRepository.markCallConnected(id) } }
                            }
                        }
                    }
                    PeerConnection.PeerConnectionState.CONNECTING -> {
                        if (_phase.value != CallPhase.RECONNECTING && _phase.value != CallPhase.RINGING) {
                            _phase.value = CallPhase.CONNECTING
                        }
                    }
                    PeerConnection.PeerConnectionState.DISCONNECTED -> {
                        _phase.value = CallPhase.RECONNECTING
                        scheduleConnectionLoss()
                    }
                    PeerConnection.PeerConnectionState.FAILED -> {
                        // Sin reinicio de ICE no hay vuelta atrás: mejor decirlo que
                        // dejar al usuario mirando una llamada muda.
                        endCall(CallEndReason.CONNECTION_LOST)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun scheduleConnectionLoss() {
        if (reconnectJob?.isActive == true) return
        reconnectJob = viewModelScope.launch {
            delay(RECONNECT_GRACE_MS)
            if (_phase.value == CallPhase.RECONNECTING && _endReason.value == null) {
                endCall(CallEndReason.CONNECTION_LOST)
            }
        }
    }

    /**
     * Escucha el nodo de la llamada (señalización + estado) y sus candidatos ICE.
     */
    private fun listenToCallUpdates(callId: String) {
        if (listenersStarted) return
        listenersStarted = true

        viewModelScope.launch {
            try {
                databaseRepository.listenToCall(callId).collect { data ->
                    if (data == null) {
                        if (_endReason.value == null && ownsWebRTCSession) endCall(CallEndReason.UNAVAILABLE)
                        return@collect
                    }
                    handleSignaling(data)
                    handleStatus(callId, data)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Se perdió la escucha de la llamada", e)
            }
        }

        viewModelScope.launch {
            try {
                val me = myUid()
                databaseRepository.observeIceCandidates(callId).collect { candidateData ->
                    // Solo aplica candidatos quien tiene una PeerConnection propia.
                    if (!ownsWebRTCSession) return@collect
                    val userId = candidateData["userId"] as? String
                    if (userId == me) return@collect
                    webRTCManager.addIceCandidate(
                        com.Azelmods.App.data.model.IceCandidate(
                            sdp = candidateData["sdp"] as? String ?: "",
                            sdpMid = candidateData["sdpMid"] as? String ?: "",
                            sdpMLineIndex = (candidateData["sdpMLineIndex"] as? Number)?.toInt() ?: 0,
                            userId = userId ?: ""
                        )
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Se perdió la escucha de candidatos ICE", e)
            }
        }
    }

    private fun handleSignaling(data: Map<String, Any>) {
        if (!ownsWebRTCSession) return
        if (isCaller) {
            // El emisor consume SOLO la respuesta; nunca su propia oferta.
            val answer = data["answer"] as? String
            if (answer != null && !remoteAnswerHandled) {
                remoteAnswerHandled = true
                webRTCManager.setRemoteDescription(answer, "answer")
            }
        } else {
            // El receptor consume SOLO la oferta; la respuesta se crea cuando
            // WebRTC confirma la descripción remota (ver WebRTCManager).
            val offer = data["offer"] as? String
            if (offer != null && !remoteOfferHandled) {
                remoteOfferHandled = true
                webRTCManager.setRemoteDescription(offer, "offer")
            }
        }
    }

    private fun handleStatus(callId: String, data: Map<String, Any>) {
        when ((data["status"] as? String)?.uppercase()) {
            CallStatus.ACCEPTED.name, CallStatus.CONNECTED.name -> {
                wasAccepted = true
                ringingTimeoutJob?.cancel()
                if (isCaller && _phase.value == CallPhase.RINGING) _phase.value = CallPhase.CONNECTING
            }
            CallStatus.ENDED.name -> {
                if (_endReason.value != null) return
                val endedByMe = (data["endedBy"] as? String) == myUid()
                // ── Llamada perdida: el emisor colgó antes de que contestáramos ──
                if (isReceiverSide && !wasAccepted && !ownsWebRTCSession) {
                    CallNotifications.cancelIncoming(context, callId)
                    val name = callStartCallerName.ifBlank { _contactProfile.value?.name ?: "Desconocido" }
                    val photo = callStartCallerPhotoUrl
                    val otherId = callStartOtherUserId
                    // La notificación descarga la foto: nunca en el hilo principal.
                    appScope.launch {
                        NotificationHelper.showMissedCallNotification(
                            context = context,
                            callId = callId,
                            callerName = name,
                            callerPhotoUrl = photo,
                            callerId = otherId
                        )
                    }
                }
                endCall(if (endedByMe) CallEndReason.HANGUP else CallEndReason.REMOTE_HANGUP)
            }
            CallStatus.DECLINED.name -> {
                if (_endReason.value == null) endCall(CallEndReason.DECLINED)
            }
        }
    }

    // ── Fin ───────────────────────────────────────────────────────────────

    /**
     * Termina la llamada. Es idempotente: la pueden pedir el botón, el otro
     * extremo, el temporizador o la pérdida de conexión, y solo cuenta la primera.
     */
    fun endCall(reason: CallEndReason = CallEndReason.HANGUP) {
        if (_endReason.value != null) return
        _endReason.value = reason
        _phase.value = CallPhase.ENDED
        ringingTimeoutJob?.cancel()
        reconnectJob?.cancel()

        val callId = currentCallId
        val durationSec = _connectedAt.value?.let { (SystemClock.elapsedRealtime() - it) / 1000 }
        // Lo remoto ya está cerrado si fue el otro quien colgó o rechazó; aun así
        // endCall() del repositorio no pisa un estado final, así que es seguro.
        if (callId != null && reason != CallEndReason.DECLINED && reason != CallEndReason.UNAVAILABLE) {
            appScope.launch {
                withTimeoutOrNull(10_000L) {
                    runCatching { databaseRepository.endCall(callId, myUid(), durationSec) }
                        .onFailure { Log.w(TAG, "No se pudo cerrar la llamada en la base de datos: ${it.message}") }
                }
            }
        }
        releaseMedia()
    }

    private fun releaseMedia() {
        CallNotifications.cancelIncoming(context, currentCallId)
        if (ownsWebRTCSession) {
            stopCallService()
            webRTCManager.cleanup()
            audioRouter.stop()
            ownsWebRTCSession = false
        }
    }

    // ── Controles ─────────────────────────────────────────────────────────

    fun toggleAudio() = webRTCManager.toggleMic()

    fun toggleVideo() = webRTCManager.toggleVideo(!webRTCManager.cameraEnabled.value)

    fun switchCamera() = webRTCManager.switchCamera()

    fun toggleSpeaker() = audioRouter.toggleSpeaker()

    private fun startCallService(callId: String, callType: CallType, contactName: String) {
        val intent = Intent(context, CallService::class.java).apply {
            action = CallService.ACTION_START_CALL
            putExtra(CallService.EXTRA_CALL_ID, callId)
            putExtra(CallService.EXTRA_CALL_TYPE, callType.name.lowercase())
            putExtra(CallService.EXTRA_CONTACT_NAME, contactName)
        }
        runCatching { context.startForegroundService(intent) }
            .onFailure { Log.e(TAG, "No se pudo iniciar el servicio de llamada", it) }
    }

    private fun stopCallService() {
        val intent = Intent(context, CallService::class.java).apply {
            action = CallService.ACTION_END_CALL
        }
        runCatching { context.startService(intent) }
    }

    override fun onCleared() {
        super.onCleared()
        // Si la pantalla que conduce la llamada desaparece sin colgar (la tarea se
        // cerró, el sistema la destruyó…), la llamada no puede seguir: se cierra
        // también en la base de datos para que el otro no se quede hablando solo.
        if (ownsWebRTCSession && _endReason.value == null) {
            endCall(CallEndReason.HANGUP)
        }
    }
}
