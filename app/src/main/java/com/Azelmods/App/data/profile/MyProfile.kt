package com.Azelmods.App.data.profile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MyProfileData(
    val uid: String = "",
    val name: String = "",
    val handle: String = "",
    val photoUrl: String? = null,
    val coverUrl: String? = null,
    val color: Int = -1
)

/**
 * El perfil del propio usuario, en vivo.
 *
 * El perfil real está en `users/{uid}`. La cuenta de FirebaseAuth casi nunca tiene nombre ni foto,
 * así que quien la lea para pintar un avatar acaba mostrando un "?" en vez de la foto.
 */
object MyProfile {
    private val _data = MutableStateFlow(MyProfileData())
    val data: StateFlow<MyProfileData> = _data.asStateFlow()

    private var reference: DatabaseReference? = null
    private var listener: ValueEventListener? = null
    private var watchedUid: String? = null

    /** Se puede llamar varias veces: solo cambia algo si la sesión es de otro usuario. */
    fun start() {
        val account = FirebaseAuth.getInstance().currentUser
        val uid = account?.uid
        if (uid == watchedUid) return
        stop()
        if (uid == null) return

        watchedUid = uid
        val fallbackName = account.displayName.orEmpty().ifBlank { account.email?.substringBefore('@').orEmpty() }
        _data.value = MyProfileData(uid = uid, name = fallbackName, photoUrl = account.photoUrl?.toString())

        val ref = FirebaseDatabase.getInstance().reference.child("users").child(uid)
        val watcher = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fun text(key: String) = snapshot.child(key).getValue(String::class.java)?.takeIf { it.isNotBlank() }
                val current = _data.value
                _data.value = current.copy(
                    name = text("displayName") ?: current.name,
                    handle = text("username").orEmpty(),
                    photoUrl = text("photoUrl") ?: current.photoUrl,
                    coverUrl = text("coverUrl"),
                    color = runCatching { snapshot.child("profileColor").getValue(Int::class.java) }.getOrNull() ?: -1
                )
            }

            override fun onCancelled(error: DatabaseError) {
                android.util.Log.w("MyProfile", "No se pudo leer el perfil: ${error.message}")
            }
        }
        ref.addValueEventListener(watcher)
        reference = ref
        listener = watcher
    }

    private fun stop() {
        val ref = reference
        val watcher = listener
        if (ref != null && watcher != null) ref.removeEventListener(watcher)
        reference = null
        listener = null
        watchedUid = null
        _data.value = MyProfileData()
    }
}

@Composable
fun rememberMyProfile(): MyProfileData {
    LaunchedEffect(Unit) { MyProfile.start() }
    val data by MyProfile.data.collectAsState()
    return data
}
