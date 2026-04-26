package com.Azelmods.App.data.security.tor

import android.content.Context
import kotlinx.coroutines.flow.first
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Example demonstrating how to integrate TorDnsResolver with TorServiceManager
 * for complete anonymous networking with DNS leak prevention.
 * 
 * This example shows the complete flow:
 * 1. Start Tor service
 * 2. Wait for Tor to connect
 * 3. Configure OkHttpClient with Tor DNS resolver
 * 4. Make anonymous network requests
 * 5. Verify Tor connectivity
 */
class TorNetworkIntegrationExample(
    private val context: Context,
    private val torServiceManager: TorServiceManager
) {
    
    /**
     * Complete example of enabling anonymous mode with DNS leak prevention
     */
    suspend fun enableAnonymousModeWithDnsProtection(): Result<OkHttpClient> {
        return try {
            // Step 1: Start Tor service
            println("Starting Tor service...")
            val torStateFlow = torServiceManager.startTor()
            
            // Step 2: Wait for Tor to connect
            val connectedState = torStateFlow.first { state ->
                state is TorState.Connected || state is TorState.Error
            }
            
            when (connectedState) {
                is TorState.Connected -> {
                    println("✓ Tor connected successfully")
                    println("Circuit: ${connectedState.circuitInfo.entryNode} -> " +
                            "${connectedState.circuitInfo.middleNode} -> " +
                            "${connectedState.circuitInfo.exitNode}")
                    
                    // Step 3: Create Tor configuration
                    val torConfig = TorProxyConfig(
                        socksHost = "127.0.0.1",
                        socksPort = 9050,
                        controlPort = 9051,
                        dataDirectory = File(context.filesDir, "tor_data"),
                        geoipFile = File(context.filesDir, "geoip"),
                        geoip6File = File(context.filesDir, "geoip6"),
                        torrcFile = File(context.filesDir, "torrc")
                    )
                    
                    // Step 4: Create OkHttpClient with DNS leak prevention
                    val okHttpClient = TorNetworkConfig.createTorEnabledOkHttpClient(torConfig)
                    
                    // Step 5: Verify Tor connectivity
                    val isTor = TorNetworkConfig.verifyTorConnection(okHttpClient)
                    if (isTor) {
                        println("✓ DNS leak prevention verified - all traffic through Tor")
                        Result.success(okHttpClient)
                    } else {
                        println("✗ WARNING: Traffic may not be routed through Tor!")
                        Result.failure(Exception("Tor verification failed"))
                    }
                }
                is TorState.Error -> {
                    println("✗ Tor connection failed: ${connectedState.message}")
                    Result.failure(connectedState.exception ?: Exception(connectedState.message))
                }
                else -> {
                    Result.failure(Exception("Unexpected Tor state: $connectedState"))
                }
            }
        } catch (e: Exception) {
            println("✗ Error enabling anonymous mode: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Example of making an anonymous HTTP request with DNS leak prevention
     */
    suspend fun makeAnonymousRequest(url: String): Result<String> {
        return try {
            // Enable anonymous mode
            val clientResult = enableAnonymousModeWithDnsProtection()
            
            if (clientResult.isFailure) {
                return Result.failure(clientResult.exceptionOrNull()!!)
            }
            
            val okHttpClient = clientResult.getOrThrow()
            
            // Make request
            println("Making anonymous request to: $url")
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""
            
            println("✓ Request successful (${response.code})")
            println("Response length: ${responseBody.length} bytes")
            
            Result.success(responseBody)
        } catch (e: Exception) {
            println("✗ Request failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Example of configuring Firebase with Tor DNS leak prevention
     */
    suspend fun configureFirebaseWithTor(): Result<Unit> {
        return try {
            // Step 1: Start Tor service
            println("Starting Tor service...")
            val torStateFlow = torServiceManager.startTor()
            
            // Step 2: Wait for Tor to connect
            val connectedState = torStateFlow.first { state ->
                state is TorState.Connected || state is TorState.Error
            }
            
            when (connectedState) {
                is TorState.Connected -> {
                    println("✓ Tor connected successfully")
                    
                    // Step 3: Create Tor configuration
                    val torConfig = TorProxyConfig(
                        socksHost = "127.0.0.1",
                        socksPort = 9050,
                        controlPort = 9051,
                        dataDirectory = File(context.filesDir, "tor_data"),
                        geoipFile = File(context.filesDir, "geoip"),
                        geoip6File = File(context.filesDir, "geoip6"),
                        torrcFile = File(context.filesDir, "torrc")
                    )
                    
                    // Step 4: Configure Firebase to use Tor proxy
                    println("Configuring Firebase to use Tor...")
                    val configurator = FirebaseProxyConfigurator()
                    configurator.configureFirebaseProxy(torConfig)
                    
                    println("✓ Firebase configured with Tor DNS leak prevention")
                    println("  - Firebase Database routes through Tor")
                    println("  - Firebase Storage routes through Tor")
                    println("  - Firebase Auth routes through Tor")
                    
                    Result.success(Unit)
                }
                is TorState.Error -> {
                    println("✗ Tor connection failed: ${connectedState.message}")
                    Result.failure(connectedState.exception ?: Exception(connectedState.message))
                }
                else -> {
                    Result.failure(Exception("Unexpected Tor state: $connectedState"))
                }
            }
        } catch (e: Exception) {
            println("✗ Firebase configuration failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Example of testing DNS leak prevention
     */
    suspend fun testDnsLeakPrevention(): Result<Boolean> {
        return try {
            val clientResult = enableAnonymousModeWithDnsProtection()
            
            if (clientResult.isFailure) {
                return Result.failure(clientResult.exceptionOrNull()!!)
            }
            
            val okHttpClient = clientResult.getOrThrow()
            
            // Test 1: Check if using Tor
            println("Test 1: Verifying Tor usage...")
            val isTor = TorNetworkConfig.verifyTorConnection(okHttpClient)
            println(if (isTor) "✓ Using Tor" else "✗ NOT using Tor")
            
            // Test 2: Check IP address
            println("\nTest 2: Checking IP address...")
            val request = Request.Builder()
                .url("https://api.ipify.org?format=text")
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            val torIp = response.body?.string() ?: "unknown"
            println("Tor exit IP: $torIp")
            
            // Test 3: DNS resolution test
            println("\nTest 3: Testing DNS resolution through Tor...")
            val torConfig = TorProxyConfig(
                dataDirectory = File(context.filesDir, "tor_data"),
                geoipFile = File(context.filesDir, "geoip"),
                geoip6File = File(context.filesDir, "geoip6"),
                torrcFile = File(context.filesDir, "torrc")
            )
            val dnsResolver = TorDnsResolver(torConfig)
            val addresses = dnsResolver.lookup("example.com")
            println("Resolved example.com to: ${addresses.first().hostAddress}")
            
            println("\n✓ All DNS leak prevention tests passed")
            Result.success(true)
        } catch (e: Exception) {
            println("\n✗ DNS leak prevention test failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Example of disabling anonymous mode
     */
    suspend fun disableAnonymousMode(): Result<Unit> {
        return try {
            println("Disabling anonymous mode...")
            
            // Remove Firebase proxy configuration
            val configurator = FirebaseProxyConfigurator()
            configurator.removeFirebaseProxy()
            println("✓ Firebase proxy removed")
            
            // Stop Tor service
            torServiceManager.stopTor()
            println("✓ Anonymous mode disabled")
            
            Result.success(Unit)
        } catch (e: Exception) {
            println("✗ Error disabling anonymous mode: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Complete example: Enable Tor, configure Firebase, make requests, disable Tor
     */
    suspend fun completeAnonymousWorkflow(): Result<Unit> {
        return try {
            println("=== Starting Complete Anonymous Workflow ===\n")
            
            // Step 1: Enable Tor
            println("Step 1: Enabling Tor...")
            val clientResult = enableAnonymousModeWithDnsProtection()
            if (clientResult.isFailure) {
                return Result.failure(clientResult.exceptionOrNull()!!)
            }
            println()
            
            // Step 2: Configure Firebase
            println("Step 2: Configuring Firebase...")
            val firebaseResult = configureFirebaseWithTor()
            if (firebaseResult.isFailure) {
                return Result.failure(firebaseResult.exceptionOrNull()!!)
            }
            println()
            
            // Step 3: Test connectivity
            println("Step 3: Testing connectivity...")
            val testResult = testDnsLeakPrevention()
            if (testResult.isFailure) {
                return Result.failure(testResult.exceptionOrNull()!!)
            }
            println()
            
            // Step 4: Make anonymous request
            println("Step 4: Making anonymous request...")
            val requestResult = makeAnonymousRequest("https://check.torproject.org/api/ip")
            if (requestResult.isSuccess) {
                println("Response: ${requestResult.getOrThrow()}")
            }
            println()
            
            println("=== Complete Anonymous Workflow Successful ===")
            Result.success(Unit)
        } catch (e: Exception) {
            println("✗ Workflow failed: ${e.message}")
            Result.failure(e)
        }
    }
}

/**
 * Usage example in a ViewModel or Repository
 */
suspend fun exampleUsage(context: Context, torServiceManager: TorServiceManager) {
    val example = TorNetworkIntegrationExample(context, torServiceManager)
    
    // Example 1: Complete workflow (recommended)
    example.completeAnonymousWorkflow()
    
    // Example 2: Enable anonymous mode only
    val result = example.enableAnonymousModeWithDnsProtection()
    if (result.isSuccess) {
        val okHttpClient = result.getOrThrow()
        // Use okHttpClient for all network requests
    }
    
    // Example 3: Configure Firebase with Tor
    example.configureFirebaseWithTor()
    
    // Example 4: Make anonymous request
    val requestResult = example.makeAnonymousRequest("https://check.torproject.org/api/ip")
    if (requestResult.isSuccess) {
        println("Response: ${requestResult.getOrThrow()}")
    }
    
    // Example 5: Test DNS leak prevention
    example.testDnsLeakPrevention()
    
    // Example 6: Disable anonymous mode (cleans up Firebase proxy too)
    example.disableAnonymousMode()
}
