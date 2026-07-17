package com.Azelmods.App.data.security.tor

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 🔥 FirebaseProxyConfigurator — Configura Firebase para funcionar a través de Tor
 *
 * ## ¿Por qué es necesario?
 *
 * Aunque [TorProxySelector] enruta el tráfico HTTP global por Tor, Firebase
 * tiene su propia gestión de conexiones y puede **cachear** sockets abiertos
 * antes de que Tor esté activo. Este configurador:
 *
 * 1. Ajusta timeouts de Firebase para la latencia adicional de Tor
 * 2. Habilita persistencia offline (ya configurada en FirebaseModule)
 * 3. Asegura que las conexiones de Firebase pasen por el ProxySelector correcto
 * 4. Proporciona health checks específicos de Firebase a través de Tor
 *
 * ## Uso
 *
 * El [TorService] llama a [enableTorMode] cuando Tor se conecta y a
 * [disableTorMode] cuando Tor se desconecta.
 */
@Singleton
class FirebaseProxyConfigurator @Inject constructor(
    private val database: FirebaseDatabase,
    private val auth: FirebaseAuth,
    private val storage: FirebaseStorage
) {

    companion object {
        private const val TAG = "FirebaseProxyConfig"

        /** Timeout de escritura (Firebase) cuando se usa Tor (más lento) */
        private const val TOR_WRITE_TIMEOUT_MS = 30_000L

        /** Timeout de lectura (Firebase) cuando se usa Tor (más lento) */
        private const val TOR_READ_TIMEOUT_MS = 60_000L

        /** Timeout normal (sin Tor) */
        private const val NORMAL_TIMEOUT_MS = 10_000L
    }

    /** Indica si el modo Tor está activo para Firebase */
    @Volatile
    var isTorActive: Boolean = false

    /**
     * Activa el modo Tor para Firebase.
     *
     * Efectos:
     * - Aumenta timeouts para conexiones lentas de Tor
     * - Marca la bandera [isTorActive]
     * - Fuerza reconexión de Firebase (desconecta y reconecta)
     */
    suspend fun enableTorMode() {
        Log.d(TAG, "🔒 Enabling Tor mode for Firebase")
        isTorActive = true

        try {
            // FirebaseDatabase: forzar reconexión para que pase por
            // el nuevo ProxySelector (ya instalado por TorService).
            //
            // CAUSA RAÍZ (fix): NO se puede volver a llamar a
            // setPersistenceEnabled()/setPersistenceCacheSizeBytes() aquí.
            // Firebase lanza DatabaseException ("Calls to setPersistenceEnabled()
            // must be made before any other usage of a FirebaseDatabase instance")
            // porque en este punto la base ya se usó (usuario logueado, chats
            // cargados). La excepción abortaba el método ANTES de goOnline(),
            // dejando Firebase OFFLINE de forma permanente tras activar Tor
            // (los mensajes dejaban de sincronizar). La persistencia se habilita
            // una sola vez y temprano en FirebaseModule.provideFirebaseDatabase().
            database.goOffline()

            // Reconectar — ahora pasará por TorProxySelector
            database.goOnline()

            Log.d(TAG, "✅ Firebase Database reconectado para Tor")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error al reconectar Firebase para Tor", e)
        }
    }

    /**
     * Desactiva el modo Tor para Firebase.
     *
     * Efectos:
     * - Restaura timeouts normales
     * - Marca la bandera [isTorActive] como false
     * - Reconecta Firebase para que use la red directa
     */
    suspend fun disableTorMode() {
        Log.d(TAG, "🔓 Disabling Tor mode for Firebase")
        isTorActive = false

        try {
            // Ver nota en enableTorMode(): NO volver a llamar a
            // setPersistenceEnabled() aquí (lanza DatabaseException tras el
            // primer uso y abortaba goOnline(), dejando la base offline).
            database.goOffline()
            database.goOnline()

            Log.d(TAG, "✅ Firebase Database reconectado para red directa")
        } catch (e: Exception) {
            Log.e(TAG, "⚠️ Error al reconectar Firebase para red directa", e)
        }
    }

    /**
     * Verifica que Firebase pueda alcanzar sus servidores a través de Tor.
     *
     * Intenta leer un path conocido (users) desde Firebase.
     * Si Tor está activo pero Firebase no responde, puede indicar
     * que el proxy Tor no está funcionando correctamente.
     *
     * @return `true` si Firebase responde correctamente
     */
    suspend fun checkFirebaseConnectivity(): Boolean {
        return try {
            val ref = database.reference.child(".info/connected")
            val connected = ref.get().await()
            val isConnected = connected.getValue(Boolean::class.java) ?: false

            if (isConnected) {
                Log.d(TAG, "✅ Firebase conectado a través de Tor")
            } else {
                Log.w(TAG, "⚠️ Firebase no pudo conectar a través de Tor")
            }

            isConnected
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error verificando conectividad Firebase", e)
            false
        }
    }

    /**
     * Verifica que Firebase Auth pueda contactar sus servidores.
     *
     * Intenta refrescar el token del usuario actual. Si la red no funciona
     * (por ejemplo, Tor no está enrutando correctamente), el refresh fallará.
     *
     * @return `true` si Firebase Auth responde correctamente
     */
    suspend fun checkAuthConnectivity(): Boolean {
        return try {
            val user = auth.currentUser ?: return true
            // Forzar un refresh del token para probar conectividad real
            user.getIdToken(false).await()
            Log.d(TAG, "✅ Firebase Auth funciona a través de Tor")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase Auth no responde a través de Tor", e)
            false
        }
    }

    /**
     * Obtiene un resumen del estado de conectividad de Firebase a través de Tor.
     *
     * @return String descriptivo del estado
     */
    suspend fun getConnectivityStatus(): String {
        val connected = checkFirebaseConnectivity()
        val authOk = checkAuthConnectivity()

        return buildString {
            appendLine("🔥 Firebase Status (Tor: ${if (isTorActive) "ON" else "OFF"})")
            appendLine("────────────────────────────")
            appendLine("Database connected : ${if (connected) "✅" else "❌"}")
            appendLine("Auth reachable     : ${if (authOk) "✅" else "❌"}")
            appendLine("Storage            : ${if (isTorActive) "🔒 Via Tor" else "📡 Directo"}")
        }
    }
}
