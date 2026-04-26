package com.Azelmods.App.data.security.tor

import okhttp3.OkHttpClient
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Example demonstrating how to integrate NetworkProxyInterceptor with OkHttp
 * for Tor-enabled network requests with user agent rotation.
 * 
 * This example shows:
 * 1. Creating an OkHttpClient with SOCKS5 proxy configuration
 * 2. Adding NetworkProxyInterceptor for user agent rotation
 * 3. Adding TorDnsResolver for DNS leak prevention
 * 4. Configuring timeouts for Tor network latency
 * 
 * **Validates: Requirements 2.5, 19.1, 20.1, 20.2, 20.3, 20.4**
 */
object NetworkProxyIntegrationExample {
    
    /**
     * Creates an OkHttpClient configured to route traffic through Tor
     * with user agent rotation and DNS leak prevention.
     * 
     * @param torManager The TorServiceManager instance
     * @param config The Tor proxy configuration
     * @return Configured OkHttpClient instance
     */
    fun createTorEnabledOkHttpClient(
        torManager: TorServiceManager,
        config: TorProxyConfig
    ): OkHttpClient {
        // Step 1: Create SOCKS5 proxy pointing to Tor
        val socksProxy = Proxy(
            Proxy.Type.SOCKS,
            InetSocketAddress(config.socksHost, config.socksPort)
        )
        
        // Step 2: Build OkHttpClient with Tor configuration
        return OkHttpClient.Builder()
            // Configure SOCKS5 proxy for Tor routing
            .proxy(socksProxy)
            
            // Add NetworkProxyInterceptor for user agent rotation
            .addInterceptor(NetworkProxyInterceptor(torManager))
            
            // Add TorDnsResolver to prevent DNS leaks
            .dns(TorDnsResolver(config))
            
            // Increase timeouts for Tor network latency
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            
            // Build the client
            .build()
    }
    
    /**
     * Example usage in a repository or network service
     */
    fun exampleUsage(torManager: TorServiceManager, config: TorProxyConfig) {
        // Create Tor-enabled OkHttpClient
        val okHttpClient = createTorEnabledOkHttpClient(torManager, config)
        
        // Use the client for network requests
        // All requests will automatically:
        // 1. Route through Tor SOCKS5 proxy
        // 2. Use random user agents (when Tor is connected)
        // 3. Resolve DNS through Tor (prevent DNS leaks)
        
        // Example: Make a request
        val request = okhttp3.Request.Builder()
            .url("https://example.com/api/data")
            .build()
        
        // Execute request (will use Tor if connected)
        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                val body = response.body?.string()
                // Process response
            }
        }
    }
    
    /**
     * Example: Configure Firebase to use Tor
     * 
     * Note: This requires reflection or custom Firebase configuration
     * as Firebase doesn't officially support custom OkHttpClient injection.
     */
    fun configureFirebaseWithTor(
        torManager: TorServiceManager,
        config: TorProxyConfig
    ) {
        val okHttpClient = createTorEnabledOkHttpClient(torManager, config)
        
        // Firebase configuration would go here
        // This is a placeholder showing the concept
        // Actual implementation may require reflection or custom Firebase setup
        
        /*
        // Example (pseudo-code):
        FirebaseDatabase.getInstance().apply {
            setOkHttpClient(okHttpClient)
        }
        
        FirebaseStorage.getInstance().apply {
            setOkHttpClient(okHttpClient)
        }
        
        FirebaseAuth.getInstance().apply {
            setOkHttpClient(okHttpClient)
        }
        */
    }
    
    /**
     * Example: Verify Tor connection is working
     * 
     * This makes a request to check.torproject.org to verify
     * that traffic is actually routing through Tor.
     */
    suspend fun verifyTorConnection(
        torManager: TorServiceManager,
        config: TorProxyConfig
    ): Boolean {
        val okHttpClient = createTorEnabledOkHttpClient(torManager, config)
        
        return try {
            val request = okhttp3.Request.Builder()
                .url("https://check.torproject.org/api/ip")
                .build()
            
            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: ""
                    // Check if response indicates we're using Tor
                    body.contains("\"IsTor\":true", ignoreCase = true)
                } else {
                    false
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}
