package com.Azelmods.App.data.chat

/**
 * Utilidad para derivar el identificador determinista de un chat privado.
 *
 * El ChatId se forma ordenando alfabéticamente los UIDs de los participantes y
 * uniéndolos con `_`. Esto garantiza que ambos participantes y todas las pantallas
 * (inicio, NewConversationScreen, Demo Chat) resuelvan el mismo nodo de chat.
 */
object ChatId {

    /**
     * Crea un ChatId determinista e independiente del orden de los UIDs.
     *
     * Propiedad: `create(a, b) == create(b, a)` (conmutativo) y siempre produce
     * el mismo resultado para las mismas entradas (determinista).
     *
     * @param uidA UID de uno de los participantes.
     * @param uidB UID del otro participante.
     * @return ChatId con los UIDs ordenados alfabéticamente y unidos por `_`.
     */
    fun create(uidA: String, uidB: String): String =
        listOf(uidA, uidB).sorted().joinToString("_")
}
