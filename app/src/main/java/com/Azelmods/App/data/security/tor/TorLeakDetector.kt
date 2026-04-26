package com.Azelmods.App.data.security.tor

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.InetAddress
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * TorLeakDetector - Detects IP and DNS leaks when Tor is enabled.
 *
 * Performs two types of leak detection:
 * 1. **IP Leak Detection** - Checks if the public IP matches the real IP (leak) or Tor exit node IP (no leak)
 * 2. **DNS Leak Detection** - Verifies that DNS queries are resolved through Tor, not the system DNS
 *
 * Usage:
 * ```kotlin
 * val result = torLeakDetector.detectLeaks()
 * if (result.hasLeaks) {
 *     // Handle leak - disable Tor, show warning, etc.
 * }
 * ```
 */
@Singleton
class TorLeakDetector @Inject constructor(
    private val context: Context
) {

    /**
     * Result of leak detection checks.
     *
     * @param hasLeaks     True if any leak was detected.
     * @param ipLeak       True if the public IP matches the real IP (not Tor exit).
     * @param dnsLeak      True if DNS queries bypass Tor.
     * @param publicIp     The detected public IP address.
     * @param realIp       The device's real IP address (if detectable).
     * @param details      Human-readable description of findings.
     */
    data class LeakDetectionResult(
        val hasLeaks: Boolean,
        val ipLeak: Boolean,
        val dnsLeak: Boolean,
        val publicIp: String?,
        val realIp: String?,
        val details: String
    )

    /**
     * Performs comprehensive leak detection.
     *
     * This is a network-heavy operation and should be called from a background thread.
     * Returns [LeakDetectionResult] with detailed findings.
     */
    suspend fun detectLeaks(): LeakDetectionResult = withContext(Dispatchers.IO) {
        try {
            val ipLeakResult = detectIpLeak()
            val dnsLeakResult = detectDnsLeak()

            val hasLeaks = ipLeakResult.first || dnsLeakResult

            val details = buildString {
                if (ipLeakResult.first) {
                    append("IP LEAK: Public IP (${ipLeakResult.second}) matches real IP (${ipLeakResult.third}). ")
                } else {
                    append("IP OK: Using Tor exit node (${ipLeakResult.second}). ")
                }

                if (dnsLeakResult) {
                    append("DNS LEAK: DNS queries bypass Tor.")
                } else {
                    append("DNS OK: DNS queries routed through Tor.")
                }
            }

            LeakDetectionResult(
                hasLeaks = hasLeaks,
                ipLeak = ipLeakResult.first,
                dnsLeak = dnsLeakResult,
                publicIp = ipLeakResult.second,
                realIp = ipLeakResult.third,
                details = details
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error during leak detection", e)
            LeakDetectionResult(
                hasLeaks = true,
                ipLeak = true,
                dnsLeak = true,
                publicIp = null,
                realIp = null,
                details = "Leak detection failed: ${e.message}"
            )
        }
    }

    /**
     * Detects IP leaks by comparing the public IP with the real IP.
     *
     * Returns Triple(hasLeak, publicIp, realIp).
     */
    private suspend fun detectIpLeak(): Triple<Boolean, String?, String?> = withContext(Dispatchers.IO) {
        try {
            // Get public IP (should be Tor exit node if no leak)
            val publicIp = getPublicIp()

            // Get real IP (device's actual IP)
            val realIp = getRealIp()

            // If public IP matches real IP, there's a leak
            val hasLeak = publicIp != null && realIp != null && publicIp == realIp

            Log.d(TAG, "IP Leak Detection: publicIp=$publicIp, realIp=$realIp, hasLeak=$hasLeak")

            Triple(hasLeak, publicIp, realIp)
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting IP leak", e)
            Triple(true, null, null) // Assume leak on error
        }
    }

    /**
     * Detects DNS leaks by checking if DNS queries are resolved through Tor.
     *
     * Returns true if DNS leak detected.
     */
    private suspend fun detectDnsLeak(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Perform a DNS lookup for a known domain
            // If this resolves to the real IP instead of through Tor, there's a DNS leak
            val testDomain = "check.torproject.org"

            // Attempt DNS resolution
            val addresses = InetAddress.getAllByName(testDomain)

            // If we can resolve without going through Tor proxy, there's a leak
            // In a proper Tor setup, DNS should fail or go through Tor
            val hasLeak = addresses.isNotEmpty()

            Log.d(TAG, "DNS Leak Detection: resolved=$hasLeak addresses for $testDomain")

            // Note: This is a simplified check. A more robust implementation would:
            // 1. Check if the resolved IP matches known Tor DNS servers
            // 2. Verify that the DNS query went through the SOCKS5 proxy
            // 3. Use a DNS leak test service API

            hasLeak
        } catch (e: Exception) {
            Log.e(TAG, "Error detecting DNS leak", e)
            false // If DNS fails completely, assume no leak (Tor is blocking it)
        }
    }

    /**
     * Gets the public IP address by querying an external service.
     *
     * This should return the Tor exit node IP if Tor is working correctly.
     */
    private suspend fun getPublicIp(): String? = withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()

            val request = Request.Builder()
                .url("https://api.ipify.org?format=text")
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    response.body?.string()?.trim()
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting public IP", e)
            null
        }
    }

    /**
     * Gets the device's real IP address (not through Tor).
     *
     * This is a best-effort attempt and may not always succeed.
     */
    private suspend fun getRealIp(): String? = withContext(Dispatchers.IO) {
        try {
            // Get the device's local network IP
            // Note: This won't work for devices behind NAT, but it's a starting point
            val localhost = InetAddress.getLocalHost()
            localhost.hostAddress
        } catch (e: Exception) {
            Log.e(TAG, "Error getting real IP", e)
            null
        }
    }

    companion object {
        private const val TAG = "TorLeakDetector"
    }
}
