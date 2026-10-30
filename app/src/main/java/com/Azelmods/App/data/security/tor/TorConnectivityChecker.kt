package com.Azelmods.App.data.security.tor

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Comprueba de verdad si el tráfico sale por Tor.
 *
 * Antes "Conectado" significaba solo que el puerto de Orbot aceptaba conexiones,
 * y la tarjeta del circuito mostraba datos inventados ("Orbot → Red Tor → Nodo de
 * salida"). Ahora se pregunta al servicio oficial del Proyecto Tor
 * (check.torproject.org), que contesta si la petición le llegó desde un nodo de
 * salida de Tor y con qué IP: eso es lo que ve cualquier web.
 */
object TorConnectivityChecker {

    private const val TAG = "TorCheck"
    private const val CHECK_URL = "https://check.torproject.org/api/ip"

    data class Result(val isTor: Boolean, val ip: String?, val error: String? = null)

    /** Petición forzada por el SOCKS de Orbot (modo proxy). */
    fun checkViaSocks(port: Int): Result =
        check(Proxy(Proxy.Type.SOCKS, InetSocketAddress("127.0.0.1", port)))

    /**
     * Petición SIN proxy. Solo tiene sentido con una VPN activa: si aun así sale
     * por Tor, Orbot está en modo VPN y TODA la app (Firebase incluido) va por Tor.
     */
    fun checkDirect(): Result = check(Proxy.NO_PROXY)

    fun isVpnActive(context: Context): Boolean = runCatching {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        cm.getNetworkCapabilities(cm.activeNetwork)?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
    }.getOrDefault(false)

    private fun check(proxy: Proxy): Result {
        val client = OkHttpClient.Builder()
            .proxy(proxy)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(45, TimeUnit.SECONDS)
            .build()
        return try {
            val request = Request.Builder().url(CHECK_URL).header("Accept", "application/json").build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return Result(false, null, "HTTP ${response.code}")
                val json = JSONObject(response.body?.string().orEmpty())
                Result(
                    isTor = json.optBoolean("IsTor", false),
                    ip = json.optString("IP").takeIf { it.isNotBlank() }
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "No se pudo comprobar Tor (${proxy.type()}): ${e.message}")
            Result(false, null, e.message ?: e.javaClass.simpleName)
        }
    }
}
