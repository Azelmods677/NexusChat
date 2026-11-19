package com.Azelmods.App.data.repository

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Si Premium se puede comprar. Lo decide `config/premium/enabled` en la base de datos.
 *
 * Mientras el cobro no esté configurado en el servidor, Premium no se enseña: antes aparecía y
 * al entrar solo decía que los pagos no estaban activados. Cuando el cobro funcione, basta con
 * poner ese valor a `true` en la consola de Firebase; no hace falta publicar otra versión.
 */
object PremiumAvailability {
    private val _enabled = MutableStateFlow(false)
    val enabled: StateFlow<Boolean> = _enabled.asStateFlow()
    @Volatile private var listening = false

    fun start() {
        if (listening) return
        listening = true
        FirebaseDatabase.getInstance().getReference("config/premium/enabled")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _enabled.value = snapshot.getValue(Boolean::class.java) == true
                }

                override fun onCancelled(error: DatabaseError) {
                    // Sin sesión todavía: se vuelve a intentar la próxima vez que se pida.
                    listening = false
                }
            })
    }

    @Composable
    fun isEnabled(): Boolean {
        start()
        return enabled.collectAsState().value
    }
}
