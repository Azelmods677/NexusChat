package com.Azelmods.App.data.model

data class User(
    val uid: String = "",
    val name: String = "",
    val displayName: String = name, // Alias for compatibility
    val username: String = "",
    val email: String = "",
    val phone: String = "",
    val photoUrl: String? = null,
    val coverUrl: String? = null,
    val coverVideoUrl: String? = null,
    /** Vídeo corto que se reproduce en bucle en lugar de la foto, dentro del perfil. */
    val profileVideoUrl: String? = null,
    /** Índice en ProfileColors del color elegido, o -1 si no eligió ninguno. */
    val profileColor: Int = -1,
    val bio: String = "",
    val status: String = "¡Hola! Estoy usando NexusChat",
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis(),
    val isPremium: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val fcmToken: String? = null,
    val messageCount: Int = 0,
    val filesShared: Int = 0
)
