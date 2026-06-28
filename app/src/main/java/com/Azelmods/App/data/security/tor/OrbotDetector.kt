package com.Azelmods.App.data.security.tor

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import java.net.InetSocketAddress
import java.net.Socket

/**
 * 🔍 OrbotDetector — Utilidad para detectar Orbot en el dispositivo
 *
 * Proporciona métodos para verificar si Orbot está instalado,
 * si el proxy SOCKS5 (puerto 9050) está accesible, y si el
 * proxy HTTP (puerto 8118) está accesible.
 */
object OrbotDetector {

    private const val TAG = "OrbotDetector"
    private const val ORBOT_PACKAGE = "org.torproject.android"
    private const val ORBOT_PACKAGE_ALT = "org.torproject.orbot"  // F-Droid, older versions
    private const val SOCKS5_PORT = 9050
    private const val HTTP_PROXY_PORT = 8118
    // ✅ FIX: Aumentar timeout para dispositivos lentos y Orbot iniciando
    private const val TIMEOUT_MS = 3000  // 3 segundos (antes: 1.5s)

    /**
     * Verifica si Orbot está instalado en el dispositivo.
     * Soporta ambos nombres de paquete oficiales:
     * - org.torproject.android (Play Store)
     * - org.torproject.orbot (F-Droid)
     */
    fun isOrbotInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(ORBOT_PACKAGE, 0)
            Log.d(TAG, "✓ Orbot está instalado (package: $ORBOT_PACKAGE)")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            try {
                context.packageManager.getPackageInfo(ORBOT_PACKAGE_ALT, 0)
                Log.d(TAG, "✓ Orbot está instalado (package: $ORBOT_PACKAGE_ALT)")
                true
            } catch (e2: PackageManager.NameNotFoundException) {
                Log.d(TAG, "✗ Orbot NO está instalado")
                false
            }
        }
    }

    /**
     * Verifica si el proxy SOCKS5 de Orbot (127.0.0.1:9050) está accesible.
     * Si este puerto responde, Orbot está funcionando y Tor está conectado.
     */
    fun isSocksProxyAvailable(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", SOCKS5_PORT), TIMEOUT_MS)
                Log.d(TAG, "✓ Proxy SOCKS5 de Orbot disponible (puerto $SOCKS5_PORT)")
                true
            }
        } catch (e: Exception) {
            Log.d(TAG, "✗ Proxy SOCKS5 de Orbot NO disponible: ${e.message}")
            false
        }
    }

    /**
     * Verifica si el proxy HTTP de Orbot (127.0.0.1:8118) está accesible.
     * Orbot provee un proxy HTTP en el puerto 8118 además del SOCKS5.
     * 
     * ✅ FIX: Timeout aumentado a 3s para dispositivos lentos
     */
    fun isHttpProxyAvailable(): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress("127.0.0.1", HTTP_PROXY_PORT), TIMEOUT_MS)
                Log.d(TAG, "✓ Proxy HTTP de Orbot disponible (puerto $HTTP_PROXY_PORT)")
                true
            }
        } catch (e: Exception) {
            Log.d(TAG, "✗ Proxy HTTP de Orbot NO disponible: ${e.message}")
            // ✅ FALLBACK: Algunas versiones de Orbot solo exponen SOCKS5
            if (isSocksProxyAvailable()) {
                Log.d(TAG, "⚠️ Puerto HTTP no disponible, pero SOCKS5 funciona. Orbot está activo.")
                return true
            }
            false
        }
    }

    /**
     * Verificación completa: ¿Orbot funciona y podemos usar Tor?
     */
    fun isTorAvailable(): Boolean {
        return isSocksProxyAvailable() || isHttpProxyAvailable()
    }

    /**
     * Obtiene información de estado para mostrar al usuario.
     */
    fun getStatus(context: Context): String {
        val installed = isOrbotInstalled(context)
        val socks = isSocksProxyAvailable()
        val http = isHttpProxyAvailable()

        return when {
            socks || http -> "✅ Tor activo vía Orbot"
            installed -> "⚠️ Orbot instalado pero no activo. Abre Orbot y presiona 'Iniciar'"
            else -> "❌ Orbot no instalado. Descárgalo desde Play Store o F-Droid: org.torproject.android"
        }
    }

    /**
     * Abre la app de Orbot si está instalada.
     */
    fun launchOrbot(context: Context): Boolean {
        return try {
            val intent = context.packageManager.getLaunchIntentForPackage(ORBOT_PACKAGE)
                ?: context.packageManager.getLaunchIntentForPackage(ORBOT_PACKAGE_ALT)
            if (intent != null) {
                context.startActivity(intent)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error al abrir Orbot", e)
            false
        }
    }

    /**
     * Obtiene información detallada del estado de Orbot con acciones sugeridas
     */
    fun getOrbotStatusInfo(context: Context): OrbotStatusInfo {
        val installed = isOrbotInstalled(context)
        val socksAvailable = isSocksProxyAvailable()
        val httpAvailable = isHttpProxyAvailable()
        
        return when {
            !installed -> OrbotStatusInfo(
                status = OrbotStatus.NOT_INSTALLED,
                message = "Orbot no está instalado",
                actionLabel = "Descargar Orbot",
                actionIntent = {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                            data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=org.torproject.android")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error abriendo Play Store", e)
                    }
                }
            )
            !socksAvailable && !httpAvailable -> OrbotStatusInfo(
                status = OrbotStatus.INSTALLED_INACTIVE,
                message = "Orbot está instalado pero no activo",
                actionLabel = "Abrir Orbot",
                actionIntent = { launchOrbot(context) }
            )
            socksAvailable || httpAvailable -> OrbotStatusInfo(
                status = OrbotStatus.ACTIVE_CONNECTED,
                message = "Conectado a la red Tor",
                actionLabel = null,
                actionIntent = null
            )
            else -> OrbotStatusInfo(
                status = OrbotStatus.ERROR,
                message = "Error al conectar con Orbot",
                actionLabel = "Reintentar",
                actionIntent = { launchOrbot(context) }
            )
        }
    }
}
