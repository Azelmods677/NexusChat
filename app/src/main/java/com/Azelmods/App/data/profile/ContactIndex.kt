package com.Azelmods.App.data.profile

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

/**
 * Buscar a alguien por su teléfono o su correo sin publicarlos.
 *
 * Antes el correo y el teléfono iban en el perfil público (`users/{uid}`) y cualquier usuario
 * con sesión podía leer los de todos. Ahora el perfil no los lleva: solo se guarda un resumen
 * (SHA-256) en `contactIndex/{hash} = uid`. Quien ya conoce tu número o tu correo puede
 * encontrarte; nadie puede listar los de los demás, porque las reglas no dejan leer el nodo
 * entero, solo una entrada concreta.
 *
 * El mismo cálculo está en la migración del servidor: si se cambia aquí, hay que cambiarlo allí.
 */
object ContactIndex {
    private const val NODE = "contactIndex"

    /** Últimos 10 dígitos: así da igual escribir +57, espacios o guiones. */
    fun normalizePhone(raw: String): String? =
        raw.filter(Char::isDigit).takeLast(10).takeIf { it.length >= 7 }

    fun normalizeEmail(raw: String): String? =
        raw.trim().lowercase().takeIf { it.contains("@") && it.length >= 5 }

    fun hash(kind: String, normalized: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest("nexuschat:$kind:$normalized".toByteArray())
            .joinToString("") { "%02x".format(it) }

    /** Apunta tu teléfono y tu correo en el índice. No falla si no hay nada que apuntar. */
    suspend fun register(phone: String?, email: String?) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val updates = buildMap<String, Any> {
            phone?.let(::normalizePhone)?.let { put("$NODE/${hash("phone", it)}", uid) }
            email?.let(::normalizeEmail)?.let { put("$NODE/${hash("email", it)}", uid) }
        }
        if (updates.isEmpty()) return
        runCatching { FirebaseDatabase.getInstance().reference.updateChildren(updates).await() }
            .onFailure { android.util.Log.w("ContactIndex", "No se pudo actualizar el índice: ${it.message}") }
    }

    /** uid de quien tiene ese teléfono o ese correo, o null. */
    suspend fun lookup(query: String): String? {
        val key = normalizeEmail(query)?.let { hash("email", it) }
            ?: normalizePhone(query)?.let { hash("phone", it) }
            ?: return null
        return runCatching {
            FirebaseDatabase.getInstance().getReference("$NODE/$key").get().await().getValue(String::class.java)
        }.getOrNull()
    }
}
