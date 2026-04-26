package com.Azelmods.App.data.security.tor

import java.io.File

/**
 * Configuration for Tor SOCKS5 proxy
 */
data class TorProxyConfig(
    val socksHost: String = "127.0.0.1",
    val socksPort: Int = 9050,
    val controlPort: Int = 9051,
    val dataDirectory: File,
    val geoipFile: File,
    val geoip6File: File,
    val torrcFile: File,
    val useBridges: Boolean = false,
    val bridgeAddresses: List<String> = emptyList(),
    val isolateDestAddress: Boolean = true,
    val isolateDestPort: Boolean = true
)
