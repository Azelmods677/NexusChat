package com.Azelmods.App.ui.screens.auth

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.Azelmods.App.BuildConfig
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import javax.inject.Inject

sealed interface GoogleTokenResult {
    data class Token(val idToken: String) : GoogleTokenResult
    data object Cancelled : GoogleTokenResult
    data class Failed(val message: String) : GoogleTokenResult
}

/**
 * Pide a Google una sesión con el selector de cuentas del sistema.
 *
 * Necesita el contexto de la pantalla: con el de la aplicación, Android no puede mostrar el
 * selector encima de la app y la petición falla sin que se vea nada.
 */
class GoogleIdTokenProvider @Inject constructor(
    private val credentialManager: CredentialManager,
    private val googleClientId: String
) {
    suspend fun request(activity: Context): GoogleTokenResult = try {
        val response = try {
            // Primero la cuenta con la que ya se entró: un toque y dentro.
            getCredential(activity, authorizedOnly = true)
        } catch (_: NoCredentialException) {
            getCredential(activity, authorizedOnly = false)
        }
        val credential = runCatching { GoogleIdTokenCredential.createFrom(response.credential.data) }.getOrNull()
        if (credential != null) {
            GoogleTokenResult.Token(credential.idToken)
        } else {
            GoogleTokenResult.Failed("No se pudo leer la respuesta de Google. Inténtalo de nuevo.")
        }
    } catch (_: GetCredentialCancellationException) {
        GoogleTokenResult.Cancelled
    } catch (e: NoCredentialException) {
        Log.e(TAG, "Sin credenciales", e)
        GoogleTokenResult.Failed(
            "Google no ofreció ninguna cuenta. Comprueba que hay una en los ajustes del teléfono." + signatureHint(activity)
        )
    } catch (e: GetCredentialProviderConfigurationException) {
        Log.e(TAG, "Google Play Services no disponible", e)
        GoogleTokenResult.Failed("Actualiza Google Play Services para entrar con Google.")
    } catch (e: GetCredentialException) {
        Log.e(TAG, "Fallo de Google: ${e.type}: ${e.message}", e)
        val raw = e.message.orEmpty()
        val notRegistered = "28444" in raw || "Developer console" in raw
        GoogleTokenResult.Failed(
            if (notRegistered) {
                "Google no reconoce esta versión de la app." + signatureHint(activity)
            } else {
                "No se pudo entrar con Google. Revisa tu conexión e inténtalo de nuevo."
            }
        )
    }

    private suspend fun getCredential(activity: Context, authorizedOnly: Boolean): GetCredentialResponse {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(authorizedOnly)
            .setServerClientId(googleClientId)
            .setAutoSelectEnabled(authorizedOnly)
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        return credentialManager.getCredential(activity, request)
    }

    /** En compilaciones de desarrollo dice qué huella hay que registrar en Firebase. */
    private fun signatureHint(context: Context): String {
        if (!BuildConfig.DEBUG) return ""
        val sha1 = appSignatureSha1(context) ?: return ""
        return "\n\nDesarrollo: registra en Firebase la huella SHA-1 $sha1"
    }

    private companion object {
        const val TAG = "GoogleSignIn"
    }
}

/** Huella SHA-1 del certificado con el que está firmada la app instalada. */
fun appSignatureSha1(context: Context): String? = runCatching {
    val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
    val certificate = info.signingInfo?.apkContentsSigners?.firstOrNull() ?: return null
    MessageDigest.getInstance("SHA-1").digest(certificate.toByteArray())
        .joinToString(":") { "%02X".format(it) }
}.getOrNull()
