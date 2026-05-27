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
    private const val SOCKS5_PORT = 9050
    private const val HTTP_PROXY_PORT = 8118
    private const val TIMEOUT_MS = 1500

    /**
     * Verifica si Orbot está instalado en el dispositivo.
     */
    fun isOrbotInstalled(context: Context): Boolean {
        return try {
            context.packageManager.getPackageInfo(ORBOT_PACKAGE, 0)
            Log.d(TAG, "✓ Orbot está instalado")
            true
        } catch (e: PackageManager.NameNotFoundException) {
            Log.d(TAG, "✗ Orbot NO está instalado")
            false
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
}
