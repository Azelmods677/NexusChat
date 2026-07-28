package com.Azelmods.App.domain.usecase.auth

import com.Azelmods.App.data.model.User
import com.Azelmods.App.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * Inicio de sesión con número de teléfono.
 *
 * Recibe la credencial ya construida (la obtiene [com.Azelmods.App.ui.screens.auth.PhoneLoginViewModel]
 * a partir del SMS), autentica contra Firebase y, si es la primera vez, crea el
 * perfil en `users/{uid}`.
 *
 * ## Por qué se escribe `email` aunque no haya correo
 *
 * La regla de `users/$uid` en `database.rules.json` valida
 * `newData.hasChildren(['displayName', 'email'])`. Una cuenta de teléfono no
 * tiene correo, así que se guarda una cadena vacía: el hijo existe —que es lo
 * que la regla comprueba— y el resto de la app ya trata `email` vacío como
 * "sin correo". Sin este campo la escritura del perfil se rechazaría y el
 * usuario quedaría autenticado pero invisible para los demás.
 */
class PhoneLoginUseCase @Inject constructor(
    private val auth: FirebaseAuth,
    private val database: FirebaseDatabase
) {
    suspend operator fun invoke(credential: PhoneAuthCredential): Resource<User> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: return Resource.Error("Autenticación fallida")

            val phone = firebaseUser.phoneNumber ?: ""
            val userRef = database.reference.child("users").child(firebaseUser.uid)
            val snapshot = userRef.get().await()

            if (!snapshot.exists()) {
                // Nombre y usuario derivados del teléfono. Se usan los últimos
                // dígitos y no el número completo: el nombre visible no debería
                // exponer el número entero a quien todavía no lo tiene.
                val tail = phone.filter { it.isDigit() }.takeLast(4)
                val derivedName = firebaseUser.displayName?.trim()?.takeIf { it.isNotBlank() }
                    ?: "Usuario $tail".takeIf { tail.isNotBlank() }
                    ?: "Usuario ${firebaseUser.uid.take(4)}"
                val derivedUsername = "@user${phone.filter { it.isDigit() }.takeLast(8)
                    .ifBlank { firebaseUser.uid.take(8) }}"

                val userData = mapOf(
                    "uid" to firebaseUser.uid,
                    "displayName" to derivedName,
                    "username" to derivedUsername,
                    "email" to "",
                    "photoUrl" to (firebaseUser.photoUrl?.toString() ?: ""),
                    "phoneNumber" to phone,
                    "bio" to "Hey there! I'm using Nexus Chat",
                    "createdAt" to ServerValue.TIMESTAMP,
                    "isOnline" to true
                )
                userRef.setValue(userData).await()

                Resource.Success(
                    User(
                        uid = firebaseUser.uid,
                        name = derivedName,
                        displayName = derivedName,
                        username = derivedUsername,
                        email = "",
                        photoUrl = firebaseUser.photoUrl?.toString(),
                        bio = "Hey there! I'm using Nexus Chat",
                        status = "Hey there! I'm using Nexus Chat",
                        isOnline = true,
                        lastSeen = System.currentTimeMillis(),
                        createdAt = System.currentTimeMillis()
                    )
                )
            } else {
                @Suppress("UNCHECKED_CAST")
                val userData = snapshot.value as? Map<String, Any>
                    ?: return Resource.Error("No se pudo leer el perfil")

                userRef.child("isOnline").setValue(true).await()
                // Un usuario que existía por correo y ahora enlaza su teléfono
                // debe quedar con el número guardado.
                if (phone.isNotBlank() && userData["phoneNumber"] != phone) {
                    runCatching { userRef.child("phoneNumber").setValue(phone).await() }
                }

                val name = userData["displayName"] as? String
                    ?: userData["name"] as? String
                    ?: "Usuario ${firebaseUser.uid.take(4)}"

                Resource.Success(
                    User(
                        uid = userData["uid"] as? String ?: firebaseUser.uid,
                        name = name,
                        displayName = name,
                        username = userData["username"] as? String ?: "@user",
                        email = userData["email"] as? String ?: "",
                        photoUrl = userData["photoUrl"] as? String,
                        bio = userData["bio"] as? String ?: "",
                        status = userData["status"] as? String ?: "Hey there! I'm using Nexus Chat",
                        isOnline = true,
                        lastSeen = System.currentTimeMillis(),
                        createdAt = (userData["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis()
                    )
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PhoneLoginUseCase", "Error: ${e.message}", e)
            Resource.Error(translateError(e))
        }
    }

    /**
     * Traduce los errores de Firebase a algo accionable.
     *
     * El mensaje crudo de Firebase para un código mal escrito es
     * "The sms verification code used to create the phone auth credential is
     * invalid", que en pantalla no ayuda a nadie.
     */
    private fun translateError(e: Exception): String = when {
        e.message?.contains("invalid", ignoreCase = true) == true &&
            e.message?.contains("code", ignoreCase = true) == true ->
            "El código no es correcto. Revísalo e inténtalo otra vez."
        e.message?.contains("expired", ignoreCase = true) == true ->
            "El código ha caducado. Pide uno nuevo."
        e.message?.contains("network", ignoreCase = true) == true ->
            "Sin conexión. Comprueba tu red e inténtalo de nuevo."
        else -> e.message ?: "No se pudo iniciar sesión con el teléfono"
    }
}
