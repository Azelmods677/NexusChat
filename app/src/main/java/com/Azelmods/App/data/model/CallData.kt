package com.Azelmods.App.data.model

data class CallData(
    val callId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerPhotoUrl: String? = null,
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverPhotoUrl: String? = null,
    val callType: CallType = CallType.AUDIO,
    val status: CallStatus = CallStatus.CALLING,
    val offer: String? = null,
    val answer: String? = null,
    val iceCandidates: List<IceCandidate> = emptyList(),
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null
)

data class IceCandidate(
    val sdp: String = "",
    val sdpMid: String = "",
    val sdpMLineIndex: Int = 0,
    val userId: String = ""
)

enum class CallType {
    AUDIO, VIDEO
}

enum class CallStatus {
    CALLING,      // Llamada iniciada, esperando respuesta
    RINGING,      // Sonando en el receptor
    ACCEPTED,     // Llamada aceptada, conectando
    CONNECTED,    // Llamada en progreso
    ENDED,        // Llamada terminada normalmente
    DECLINED,     // Llamada rechazada
    MISSED,       // Llamada perdida
    BUSY,         // Usuario ocupado
    FAILED        // Error en la conexión
}
