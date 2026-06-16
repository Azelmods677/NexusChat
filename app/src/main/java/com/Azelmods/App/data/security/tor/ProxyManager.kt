package com.Azelmods.App.data.security.tor

import android.util.Log
import androidx.webkit.ProxyConfig
import androidx.webkit.ProxyController
import androidx.webkit.WebViewFeature
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Resultado de una operación de proxy gestionada por [ProxyManager].
 *
 * - [Applied]: el proxy se aplicó correctamente (la solicitud se entregó a [ProxyController]).
 * - [Cleared]: el proxy se limpió correctamente.
 * - [Failed]: ocurrió un error o la funcionalidad no está soportada; la app debe continuar
 *   en modo directo (clearnet) sin crashear.
 */
sealed class ProxyResult {
    object Applied : ProxyResult()
    object Cleared : ProxyResult()
    data class Failed(val reason: String) : ProxyResult()
}

/**
 * Centraliza la configuración del proxy de Orbot a nivel de WebView usando la API oficial
 * de AndroidX WebKit ([ProxyController]).
 *
 * ## Causa raíz que corrige
 * `ProxyController.setProxyOverride` / `clearProxyOverride` son **asíncronos**: WebView invoca
 * el [Runnable] de callback en el [Executor] proporcionado MÁS TARDE
 * (`AwProxyController.proxyOverrideChanged`). El código anterior creaba un executor por operación
 * y lo apagaba (`shutdown()`) de inmediato, por lo que el callback se rechazaba con
 * `java.util.concurrent.RejectedExecutionException` y la app crasheaba.
 *
 * Aquí se usa un **executor de larga vida compartido** ([proxyExecutor]) que NUNCA se apaga
 * antes de que el callback se ejecute. Garantiza que el executor entregado a [ProxyController]
 * siempre esté vivo cuando WebView invoque el callback (Property 1).
 *
 * Toda interacción con [ProxyController] está envuelta en `try/catch`; ante cualquier excepción
 * se registra con el tag [TAG] y se devuelve [ProxyResult.Failed] para continuar en modo directo.
 */
object ProxyManager {

    private const val TAG = "TorBrowser"

    /** Host del proxy HTTP de Orbot (HTTP → SOCKS5 → Tor). */
    const val ORBOT_HTTP_HOST = "127.0.0.1"

    /** Puerto del proxy HTTP de Orbot. */
    const val ORBOT_HTTP_PORT = 8118

    /**
     * Executor de larga vida compartido para los callbacks de [ProxyController].
     *
     * NUNCA se apaga antes de que el callback se ejecute: vive mientras vive el proceso, de modo
     * que cualquier callback asíncrono de `setProxyOverride`/`clearProxyOverride` se ejecuta sobre
     * un executor no terminado, evitando `RejectedExecutionException`.
     */
    private val proxyExecutor: Executor = Executors.newSingleThreadExecutor()

    /**
     * Aplica el proxy de Orbot a nivel de WebView.
     *
     * @param httpHost host del proxy HTTP de Orbot (por defecto [ORBOT_HTTP_HOST]).
     * @param httpPort puerto del proxy HTTP de Orbot (por defecto [ORBOT_HTTP_PORT]).
     * @param onApplied callback invocado por WebView (en [proxyExecutor]) cuando el override
     *   de proxy se ha aplicado. Útil para actualizar la UI o cargar la primera URL.
     * @return [ProxyResult.Applied] si la solicitud se entregó correctamente,
     *   o [ProxyResult.Failed] si la funcionalidad no está soportada u ocurre una excepción.
     */
    fun applyOrbotProxy(
        httpHost: String = ORBOT_HTTP_HOST,
        httpPort: Int = ORBOT_HTTP_PORT,
        onApplied: () -> Unit
    ): ProxyResult = applyProxyRule("http://$httpHost:$httpPort", onApplied)

    /**
     * Aplica una regla de proxy arbitraria a nivel de WebView.
     *
     * @param proxyUrl URL completa del proxy, p. ej. `http://127.0.0.1:8118` o
     *   `socks5://127.0.0.1:9050`. Orbot expone HTTP en 8118 y SOCKS5 en 9050.
     * @param onApplied callback invocado por WebView (en [proxyExecutor]) cuando el
     *   override de proxy se ha aplicado.
     * @return [ProxyResult.Applied] si la solicitud se entregó correctamente,
     *   o [ProxyResult.Failed] si la funcionalidad no está soportada u ocurre una excepción.
     */
    fun applyProxyRule(
        proxyUrl: String,
        onApplied: () -> Unit
    ): ProxyResult {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Log.w(TAG, "PROXY_OVERRIDE no soportado en este dispositivo — modo directo")
            return ProxyResult.Failed("PROXY_OVERRIDE no soportado")
        }

        return try {
            val config = ProxyConfig.Builder()
                .addProxyRule(proxyUrl)
                .build()

            ProxyController.getInstance().setProxyOverride(
                config,
                proxyExecutor,
                Runnable { onApplied() }
            )

            Log.d(TAG, "✓ Proxy configurado: $proxyUrl")
            ProxyResult.Applied
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error configurando proxy ($proxyUrl) — continuando en modo directo", e)
            ProxyResult.Failed(e.message ?: "Error desconocido al aplicar el proxy")
        }
    }

    /**
     * Limpia la configuración del proxy para restaurar el tráfico directo.
     *
     * @param onCleared callback invocado por WebView (en [proxyExecutor]) cuando el override
     *   de proxy se ha limpiado.
     * @return [ProxyResult.Cleared] si la solicitud se entregó correctamente,
     *   o [ProxyResult.Failed] si la funcionalidad no está soportada u ocurre una excepción.
     */
    fun clearProxy(onCleared: () -> Unit): ProxyResult {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.PROXY_OVERRIDE)) {
            Log.w(TAG, "PROXY_OVERRIDE no soportado en este dispositivo — nada que limpiar")
            return ProxyResult.Failed("PROXY_OVERRIDE no soportado")
        }

        return try {
            ProxyController.getInstance().clearProxyOverride(
                proxyExecutor,
                Runnable { onCleared() }
            )

            Log.d(TAG, "✓ Proxy limpiado — tráfico directo restaurado")
            ProxyResult.Cleared
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error limpiando proxy — continuando en modo directo", e)
            ProxyResult.Failed(e.message ?: "Error desconocido al limpiar el proxy")
        }
    }
}
