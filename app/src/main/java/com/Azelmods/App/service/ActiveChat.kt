package com.Azelmods.App.service

/**
 * El chat que se está viendo ahora mismo: sus mensajes no necesitan aviso.
 * El identificador lo fija el ViewModel al resolverlo, y la pantalla dice si está a la vista.
 */
object ActiveChat {
    @Volatile
    var chatId: String? = null

    @Volatile
    var isVisible: Boolean = false

    fun isShowing(id: String): Boolean = isVisible && chatId == id
}
