package com.Azelmods.App.ui.screens.cybersec

import android.content.Context
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.net.Socket
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class CyberSecViewModel @Inject constructor() : ViewModel() {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .build()
    
    private val _result = MutableStateFlow("")
    val result = _result.asStateFlow()
    
    private val _loading = MutableStateFlow(false)
    val loading = _loading.asStateFlow()
    
    // PORT SCANNER
    fun scanPorts(host: String, fromPort: Int, toPort: Int) {
        viewModelScope.launch {
            _loading.value = true
            _result.value = "Escaneando $host:$fromPort-$toPort...\n"
            withContext(Dispatchers.IO) {
                val open = mutableListOf<Int>()
                (fromPort..toPort).forEach { port ->
                    try {
                        Socket().use { s ->
                            s.connect(
                                java.net.InetSocketAddress(host, port), 500
                            )
                            open.add(port)
                            _result.value += "  ✅ $port/tcp OPEN\n"
                        }
                    } catch (_: Exception) {}
                }
                if (open.isEmpty()) _result.value += "  No se encontraron puertos abiertos.\n"
                _result.value += "\nScan completado — ${open.size} puertos abiertos."
            }
            _loading.value = false
        }
    }
    
    // DNS LOOKUP
    fun dnsLookup(host: String) {
        viewModelScope.launch {
            _loading.value = true
            withContext(Dispatchers.IO) {
                try {
                    val addresses = InetAddress.getAllByName(host)
                    _result.value = "DNS Lookup: $host\n" +
                        addresses.joinToString("\n") { "  → ${it.hostAddress}" }
                } catch (e: Exception) {
                    _result.value = "Error: ${e.message}"
                }
            }
            _loading.value = false
        }
    }
    
    // HTTP HEADERS ANALYZER
    fun analyzeHeaders(url: String) {
        viewModelScope.launch {
            _loading.value = true
            withContext(Dispatchers.IO) {
                try {
                    val req = Request.Builder().url(url).head().build()
                    val resp = client.newCall(req).execute()
                    val sb = StringBuilder("HTTP ${resp.code} ${resp.message}\n\n")
                    resp.headers.forEach { (name, value) ->
                        sb.append("$name: $value\n")
                    }
                    _result.value = sb.toString()
                } catch (e: Exception) {
                    _result.value = "Error: ${e.message}"
                }
            }
            _loading.value = false
        }
    }
    
    // HASH GENERATOR
    fun generateHash(input: String, algorithm: String) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val digest = MessageDigest.getInstance(algorithm)
                    val hash = digest.digest(input.toByteArray())
                        .joinToString("") { "%02x".format(it) }
                    _result.value = "$algorithm:\n$hash"
                } catch (e: Exception) {
                    _result.value = "Error: ${e.message}"
                }
            }
        }
    }
    
    // BASE64 ENCODE/DECODE
    fun base64Encode(input: String) {
        _result.value = "Encoded:\n" +
            Base64.encodeToString(input.toByteArray(), Base64.DEFAULT)
    }
    
    fun base64Decode(input: String) {
        try {
            _result.value = "Decoded:\n" +
                String(Base64.decode(input, Base64.DEFAULT))
        } catch (e: Exception) {
            _result.value = "Error: input Base64 inválido"
        }
    }
    
    // HEX ENCODE/DECODE
    fun hexEncode(input: String) {
        _result.value = "Hex:\n" +
            input.toByteArray().joinToString("") { "%02x".format(it) }
    }
    
    fun hexDecode(input: String) {
        try {
            val bytes = input.chunked(2)
                .map { it.toInt(16).toByte() }.toByteArray()
            _result.value = "Decoded:\n${String(bytes)}"
        } catch (e: Exception) {
            _result.value = "Error: hex inválido"
        }
    }
    
    // URL ENCODE/DECODE
    fun urlEncode(input: String) {
        _result.value = "URL Encoded:\n" +
            java.net.URLEncoder.encode(input, "UTF-8")
    }
    
    fun urlDecode(input: String) {
        _result.value = "URL Decoded:\n" +
            java.net.URLDecoder.decode(input, "UTF-8")
    }
    
    // NETWORK INFO
    fun getNetworkInfo(context: Context) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val cm = context.getSystemService(
                        Context.CONNECTIVITY_SERVICE
                    ) as android.net.ConnectivityManager
                    val network = cm.activeNetwork
                    val capabilities = cm.getNetworkCapabilities(network)
                    
                    // Get WiFi info if available
                    val wm = context.getSystemService(
                        Context.WIFI_SERVICE
                    ) as android.net.wifi.WifiManager
                    val info = wm.connectionInfo
                    
                    _result.value = buildString {
                        append("=== NETWORK INFO ===\n")
                        append("SSID:      ${info.ssid}\n")
                        append("BSSID:     ${info.bssid}\n")
                        append("Signal:    ${info.rssi} dBm\n")
                        append("Speed:     ${info.linkSpeed} Mbps\n")
                        append("Network:   ${if (capabilities?.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) == true) "WiFi" else "Mobile"}\n")
                    }
                } catch (e: Exception) {
                    _result.value = "Error: ${e.message}"
                }
            }
        }
    }
    
    fun clearResult() {
        _result.value = ""
    }
}
