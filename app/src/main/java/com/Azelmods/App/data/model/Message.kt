package com.Azelmods.App.data.model

data class Message(
    val messageId: String = "",
    val chatId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val receiverId: String = "", // Added for compatibility
    val content: String = "",
    val isEncrypted: Boolean = false,
    val encryptedPayload: String? = null,
    val timestamp: Long = 0,
    val status: MessageStatus = MessageStatus.SENT,
    val isEdited: Boolean = false,
    val isGroup: Boolean = false, // Added for compatibility
    val replyTo: String? = null,
    /** Grabada con el micrófono: se muestra distinta de un archivo de audio. */
    val isVoice: Boolean = false,
    val reactions: Map<String, String> = emptyMap(), // userId to emoji
    val mediaUrl: String? = null, // URL for image/video/audio
    val mediaType: String? = null, // IMAGE, VIDEO, AUDIO, GIF, GIF_VIDEO, DOCUMENT, LOCATION, STICKER, SYSTEM
    val deletedFor: Map<String, Boolean> = emptyMap(), // userId to deleted status
    val deletedForEveryone: Boolean = false, // NEW: true if deleted for everyone
    val edited: Boolean = false,
    val editedAt: Long = 0,
    val forwardedFrom: String? = null, // Name of original sender if forwarded
    val latitude: Double? = null,
    val longitude: Double? = null,
    // ── Mensajes temporales ──
    val isEphemeral: Boolean = false,      // se borra solo
    val isViewOnce: Boolean = false,       // se borra al cerrarlo tras verlo una vez
    val selfDestructDuration: Long = 0,    // segundos desde que el destinatario lo abre
    val viewedAt: Long = 0,                // cuándo se abrió (marca de tiempo del servidor)
    val expireAt: Long = 0,                // tope si nadie llega a abrirlo
    val selfDestructAt: Long = 0,          // compatibilidad con mensajes de versiones anteriores
    val viewedBy: List<String> = emptyList()
) {
    /**
     * Momento en que este mensaje debe desaparecer (hora del servidor), o 0 si no
     * caduca. La cuenta atrás empieza al abrirlo: si empezara al enviarlo, alguien sin
     * conexión perdería el mensaje sin haberlo visto.
     */
    val expiresAt: Long
        get() = when {
            !isEphemeral -> 0L
            viewedAt > 0 && selfDestructDuration > 0 -> viewedAt + selfDestructDuration * 1000
            selfDestructAt > 0 -> selfDestructAt
            else -> expireAt
        }

    /** La cuenta atrás ya está en marcha (no basta con que tenga un tope de 7 días). */
    val isCountingDown: Boolean
        get() = isEphemeral && !isViewOnce && (viewedAt > 0 || selfDestructAt > 0)
}

enum class MessageStatus {
    SENDING,
    SENT,
    DELIVERED,
    READ,
    FAILED
}
