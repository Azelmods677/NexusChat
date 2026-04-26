package com.Azelmods.App.data.security.tor

import okhttp3.Interceptor
import okhttp3.Response

/**
 * OkHttp interceptor that adds random user agent rotation for additional anonymity
 * when Tor is enabled.
 * 
 * This interceptor:
 * - Checks Tor connection state before intercepting
 * - Adds random user agent rotation to prevent browser fingerprinting
 * - Only modifies requests when Tor is in Connected state
 * 
 * **Validates: Requirements 2.5, 19.1, 20.1, 20.2, 20.3, 20.4**
 */
class NetworkProxyInterceptor(
    private val torManager: TorServiceManager
) : Interceptor {
    
    companion object {
        /**
         * List of common user agents for rotation
         * Includes Windows, Linux, and macOS variants for better anonymity
         */
        private val USER_AGENTS = listOf(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (X11; Linux x86_64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"
        )
    }
    
    /**
     * Intercepts HTTP requests and adds random user agent when Tor is connected
     * 
     * @param chain The interceptor chain
     * @return The response from the server
     */
    override fun intercept(chain: Interceptor.Chain): Response {
        val torState = torManager.getTorState().value
        
        return if (torState is TorState.Connected) {
            // Tor is connected, add random user agent for anonymity
            val request = chain.request()
            val proxiedRequest = request.newBuilder()
                .header("User-Agent", generateRandomUserAgent())
                .build()
            chain.proceed(proxiedRequest)
        } else {
            // Tor is not connected, proceed with original request
            chain.proceed(chain.request())
        }
    }
    
    /**
     * Generates a random user agent from the predefined list
     * 
     * @return A random user agent string
     */
    private fun generateRandomUserAgent(): String {
        return USER_AGENTS.random()
    }
}
