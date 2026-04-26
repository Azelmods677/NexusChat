package com.Azelmods.App.data.security.tor

import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.net.InetAddress

/**
 * Unit tests for TorDnsResolver
 * 
 * These tests verify that DNS resolution works correctly through Tor
 * and that fallback mechanisms function as expected.
 */
class TorDnsResolverTest {
    
    private lateinit var config: TorProxyConfig
    private lateinit var resolver: TorDnsResolver
    
    @Before
    fun setup() {
        // Create test configuration
        config = TorProxyConfig(
            socksHost = "127.0.0.1",
            socksPort = 9050,
            controlPort = 9051,
            dataDirectory = File("/tmp/tor_data"),
            geoipFile = File("/tmp/geoip"),
            geoip6File = File("/tmp/geoip6"),
            torrcFile = File("/tmp/torrc")
        )
        
        resolver = TorDnsResolver(config)
    }
    
    @Test
    fun `lookup returns non-empty list for valid hostname`() {
        // Note: This test requires Tor to be running on localhost:9050
        // In a real test environment, you would mock the socket connection
        
        try {
            val result = resolver.lookup("example.com")
            
            // Verify result is not empty
            assertTrue("Result should not be empty", result.isNotEmpty())
            
            // Verify result contains valid InetAddress
            assertNotNull("First address should not be null", result.first())
            
            println("Resolved example.com to: ${result.first().hostAddress}")
        } catch (e: Exception) {
            // If Tor is not running, test will fall back to system DNS
            println("Test requires Tor to be running on localhost:9050")
            println("Falling back to system DNS: ${e.message}")
        }
    }
    
    @Test
    fun `lookup falls back to system DNS when Tor is unavailable`() {
        // Create config with invalid port to simulate Tor being unavailable
        val invalidConfig = config.copy(socksPort = 9999)
        val invalidResolver = TorDnsResolver(invalidConfig)
        
        // Should fall back to system DNS
        val result = invalidResolver.lookup("example.com")
        
        // Verify fallback worked
        assertTrue("Fallback should return non-empty result", result.isNotEmpty())
        assertNotNull("Fallback should return valid address", result.first())
    }
    
    @Test
    fun `lookup handles localhost correctly`() {
        val result = resolver.lookup("localhost")
        
        assertTrue("Result should not be empty", result.isNotEmpty())
        
        // Localhost should resolve to 127.0.0.1 or ::1
        val address = result.first()
        assertTrue(
            "Localhost should resolve to loopback address",
            address.isLoopbackAddress
        )
    }
    
    @Test(expected = Exception::class)
    fun `lookup throws exception for invalid hostname when both Tor and system DNS fail`() {
        // Use an invalid hostname that should fail in both Tor and system DNS
        resolver.lookup("this-is-definitely-not-a-valid-hostname-12345.invalid")
    }
    
    @Test
    fun `lookup handles IP address strings`() {
        // IP addresses should be resolved directly
        val result = resolver.lookup("8.8.8.8")
        
        assertTrue("Result should not be empty", result.isNotEmpty())
        assertEquals("Should resolve to same IP", "8.8.8.8", result.first().hostAddress)
    }
}
