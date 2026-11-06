package com.Azelmods.App.data.security.secret

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

data class SecretChat(val id: String, val peerId: String, val peerName: String, val peerPhoto: String?, val createdBy: String)

data class SecretMessage(val id: String, val mine: Boolean, val text: String, val at: Long)

enum class SecretChatStatus { ESTABLISHING, READY, FAILED }

/**
 * Chats secretos: viven solo en los dos teléfonos.
 *
 * El servidor es un mero cartero: guarda el texto cifrado hasta que se entrega y lo borra a
 * continuación. Los mensajes quedan en un archivo cifrado en cada teléfono, no hay copia en la
 * nube y no se pueden abrir en otro dispositivo: perder el teléfono es perder el chat.
 */
@Singleton
class SecretChatRepository @Inject constructor(
    @param:ApplicationContext context: Context
) {
    private val db = FirebaseDatabase.getInstance()
    private val me get() = FirebaseAuth.getInstance().currentUser?.uid

    private val vault by lazy {
        EncryptedSharedPreferences.create(
            context,
            "secret_chats",
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val conversations = mutableMapOf<String, MutableStateFlow<List<SecretMessage>>>()

    fun messages(chatId: String): StateFlow<List<SecretMessage>> =
        conversations.getOrPut(chatId) { MutableStateFlow(loadLocal(chatId)) }.asStateFlow()

    /** Los chats secretos de este usuario, con los datos de la otra persona. */
    fun observeChats(): Flow<List<SecretChat>> = callbackFlow {
        val uid = me
        if (uid == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = db.getReference("userSecretChats/$uid")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val ids = snapshot.children.mapNotNull { it.key }
                if (ids.isEmpty()) trySend(emptyList())
                val found = java.util.concurrent.ConcurrentHashMap<String, SecretChat>()
                var pending = ids.size
                ids.forEach { id ->
                    db.getReference("secretChats/$id").get().addOnCompleteListener { task ->
                        val chat = task.result
                        val peerId = chat?.child("members")?.children?.mapNotNull { it.key }?.firstOrNull { it != uid }
                        if (peerId == null) {
                            if (--pending == 0) trySend(found.values.sortedBy { it.id })
                            return@addOnCompleteListener
                        }
                        db.getReference("users/$peerId").get().addOnCompleteListener { userTask ->
                            val user = userTask.result
                            found[id] = SecretChat(
                                id = id,
                                peerId = peerId,
                                peerName = user?.child("displayName")?.getValue(String::class.java)
                                    ?: user?.child("name")?.getValue(String::class.java).orEmpty(),
                                peerPhoto = user?.child("photoUrl")?.getValue(String::class.java),
                                createdBy = chat?.child("createdBy")?.getValue(String::class.java).orEmpty()
                            )
                            if (--pending == 0) trySend(found.values.sortedBy { it.id })
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Lista de chats secretos: ${error.message}")
                trySend(emptyList())
            }
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Crea el chat con [peerId] y publica la clave pública de este teléfono. */
    suspend fun start(peerId: String): String {
        val uid = me ?: error("Sin sesión")
        val id = db.getReference("secretChats").push().key ?: error("Sin identificador")
        val keys = SecretRatchet.newKeyPair()
        vault.edit().putString(privateKeyName(id), encode(keys.private.encoded)).apply()
        db.getReference("secretChats/$id").setValue(
            mapOf(
                "members" to mapOf(uid to true, peerId to true),
                "createdBy" to uid,
                "createdAt" to ServerValue.TIMESTAMP,
                "keys" to mapOf(uid to encode(keys.public.encoded))
            )
        ).await()
        db.reference.updateChildren(
            mapOf("userSecretChats/$uid/$id" to true, "userSecretChats/$peerId/$id" to true)
        ).await()
        return id
    }

    /**
     * Acuerda las claves del chat. Quien lo recibe publica su clave al abrirlo por primera vez;
     * quien lo creó deriva la raíz cuando esa clave aparece. Se llama cada vez que cambia el nodo.
     */
    private suspend fun establish(chatId: String, keysNode: DataSnapshot, peerId: String): Boolean {
        val uid = me ?: return false
        if (vault.contains(rootName(chatId))) return true
        var privateKey = vault.getString(privateKeyName(chatId), null)
        if (privateKey == null) {
            if (keysNode.hasChild(uid)) return false // La clave de este lado se perdió (otro teléfono).
            val keys = SecretRatchet.newKeyPair()
            privateKey = encode(keys.private.encoded)
            vault.edit().putString(privateKeyName(chatId), privateKey).apply()
            db.getReference("secretChats/$chatId/keys/$uid").setValue(encode(keys.public.encoded)).await()
        }
        val peerKey = keysNode.child(peerId).getValue(String::class.java) ?: return false
        val root = SecretRatchet.sharedRoot(decode(privateKey), decode(peerKey), chatId)
        vault.edit()
            .putString(sendChainName(chatId), encode(SecretRatchet.chainFor(root, uid)))
            .putString(receiveChainName(chatId), encode(SecretRatchet.chainFor(root, peerId)))
            .putInt(sendCountName(chatId), 0)
            .putInt(receiveCountName(chatId), 0)
            .putString(rootName(chatId), "ok")
            // La clave efímera ya cumplió su función: sin ella nadie puede rehacer esta raíz.
            .remove(privateKeyName(chatId))
            .apply()
        return true
    }

    /** Mientras se observa, recibe y descifra mensajes y los borra del servidor. */
    fun connect(chatId: String, peerId: String): Flow<SecretChatStatus> = callbackFlow {
        val uid = me
        if (uid == null) {
            trySend(SecretChatStatus.FAILED)
            close()
            return@callbackFlow
        }
        val messagesRef = db.getReference("secretChats/$chatId/messages").orderByChild("n")
        val incoming = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previous: String?) {
                if (snapshot.child("s").getValue(String::class.java) != uid) receive(chatId, peerId, snapshot)
            }
            override fun onChildChanged(snapshot: DataSnapshot, previous: String?) = Unit
            override fun onChildRemoved(snapshot: DataSnapshot) = Unit
            override fun onChildMoved(snapshot: DataSnapshot, previous: String?) = Unit
            override fun onCancelled(error: DatabaseError) = Unit
        }
        // Los mensajes se escuchan cuando ya hay claves: antes no se podrían descifrar.
        var listening = false

        val keysRef = db.getReference("secretChats/$chatId/keys")
        val keysListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                launch(Dispatchers.IO) {
                    val ready = runCatching { establish(chatId, snapshot, peerId) }
                        .onFailure { Log.e(TAG, "Claves del chat secreto", it) }
                        .getOrDefault(false)
                    if (ready && !listening) {
                        listening = true
                        messagesRef.addChildEventListener(incoming)
                    }
                    trySend(if (ready) SecretChatStatus.READY else SecretChatStatus.ESTABLISHING)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(SecretChatStatus.FAILED)
            }
        }
        keysRef.addValueEventListener(keysListener)
        awaitClose {
            keysRef.removeEventListener(keysListener)
            messagesRef.removeEventListener(incoming)
        }
    }

    private fun receive(chatId: String, peerId: String, snapshot: DataSnapshot) {
        val id = snapshot.key ?: return
        val number = snapshot.child("n").getValue(Int::class.java) ?: return
        val body = snapshot.child("c").getValue(String::class.java) ?: return
        val at = snapshot.child("at").getValue(Long::class.java) ?: System.currentTimeMillis()
        var chain = vault.getString(receiveChainName(chatId), null)?.let(::decode) ?: return
        var expected = vault.getInt(receiveCountName(chatId), 0)
        if (number < expected) return // Ya se descifró: Firebase puede repetir la entrega.

        // Si llegó fuera de orden, se guardan las claves de los que faltan para cuando aparezcan.
        val skipped = vault.edit()
        var key: ByteArray? = vault.getString(skippedName(chatId, number), null)?.let(::decode)
        if (key == null) {
            while (expected <= number) {
                val step = SecretRatchet.advance(chain)
                if (expected == number) key = step.messageKey else skipped.putString(skippedName(chatId, expected), encode(step.messageKey))
                chain = step.nextChain
                expected++
            }
            skipped.putString(receiveChainName(chatId), encode(chain)).putInt(receiveCountName(chatId), expected)
        } else {
            skipped.remove(skippedName(chatId, number))
        }
        skipped.apply()

        runCatching {
            SecretRatchet.open(key!!, decode(body), "$chatId|$peerId|$number")
        }.onSuccess { text ->
            append(chatId, SecretMessage(id, mine = false, text = text, at = at))
            snapshot.ref.removeValue()
        }.onFailure { Log.w(TAG, "Mensaje secreto rechazado: ${it.message}") }
    }

    suspend fun send(chatId: String, text: String) {
        val uid = me ?: return
        val chain = vault.getString(sendChainName(chatId), null)?.let(::decode) ?: error("El chat aún no está listo")
        val number = vault.getInt(sendCountName(chatId), 0)
        val step = SecretRatchet.advance(chain)
        // La cadena se guarda antes de enviar: nunca se reutiliza una clave, ni si falla el envío.
        vault.edit()
            .putString(sendChainName(chatId), encode(step.nextChain))
            .putInt(sendCountName(chatId), number + 1)
            .commit()
        val ref = db.getReference("secretChats/$chatId/messages").push()
        val sealed = SecretRatchet.seal(step.messageKey, text, "$chatId|$uid|$number")
        ref.setValue(mapOf("s" to uid, "n" to number, "c" to encode(sealed), "at" to ServerValue.TIMESTAMP)).await()
        append(chatId, SecretMessage(ref.key.orEmpty(), mine = true, text = text, at = System.currentTimeMillis()))
    }

    // ── Temporizador ─────────────────────────────────────────────────────────

    fun timerSeconds(chatId: String): Long = vault.getLong(timerName(chatId), 0L)

    fun setTimer(chatId: String, seconds: Long) {
        vault.edit().putLong(timerName(chatId), seconds).apply()
        purgeExpired(chatId)
    }

    fun purgeExpired(chatId: String) {
        val seconds = timerSeconds(chatId)
        if (seconds <= 0L) return
        val flow = conversations[chatId] ?: return
        val limit = System.currentTimeMillis() - seconds * 1000
        val remaining = flow.value.filter { it.at >= limit }
        if (remaining.size != flow.value.size) {
            flow.value = remaining
            saveLocal(chatId, remaining)
        }
    }

    /** Borra el chat de este teléfono y del servidor. */
    suspend fun delete(chatId: String) {
        val uid = me ?: return
        runCatching { db.getReference("userSecretChats/$uid/$chatId").removeValue().await() }
        runCatching { db.getReference("secretChats/$chatId").removeValue().await() }
        vault.edit().apply {
            vault.all.keys.filter { it.contains(chatId) }.forEach(::remove)
        }.apply()
        conversations.remove(chatId)
    }

    // ── Almacén local cifrado ────────────────────────────────────────────────

    private fun append(chatId: String, message: SecretMessage) {
        val flow = conversations.getOrPut(chatId) { MutableStateFlow(loadLocal(chatId)) }
        if (flow.value.any { it.id == message.id }) return
        val next = flow.value + message
        flow.value = next
        saveLocal(chatId, next)
    }

    private fun loadLocal(chatId: String): List<SecretMessage> = runCatching {
        val array = JSONArray(vault.getString(messagesName(chatId), "[]"))
        List(array.length()) {
            val item = array.getJSONObject(it)
            SecretMessage(item.getString("id"), item.getBoolean("mine"), item.getString("text"), item.getLong("at"))
        }
    }.getOrDefault(emptyList())

    private fun saveLocal(chatId: String, messages: List<SecretMessage>) {
        val array = JSONArray()
        messages.forEach {
            array.put(JSONObject().put("id", it.id).put("mine", it.mine).put("text", it.text).put("at", it.at))
        }
        vault.edit().putString(messagesName(chatId), array.toString()).apply()
    }

    private fun encode(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun decode(text: String) = Base64.decode(text, Base64.NO_WRAP)

    private fun privateKeyName(id: String) = "priv_$id"
    private fun rootName(id: String) = "root_$id"
    private fun sendChainName(id: String) = "send_$id"
    private fun receiveChainName(id: String) = "recv_$id"
    private fun sendCountName(id: String) = "sendn_$id"
    private fun receiveCountName(id: String) = "recvn_$id"
    private fun skippedName(id: String, n: Int) = "skip_${id}_$n"
    private fun timerName(id: String) = "timer_$id"
    private fun messagesName(id: String) = "msgs_$id"

    private companion object {
        const val TAG = "SecretChat"
    }
}
