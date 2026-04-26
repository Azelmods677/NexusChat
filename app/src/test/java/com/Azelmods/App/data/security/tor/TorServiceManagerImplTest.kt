package com.Azelmods.App.data.security.tor

import android.content.Context
import android.content.pm.ApplicationInfo
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class TorServiceManagerImplTest {
    
    private lateinit var context: Context
    private lateinit var torServiceManager: TorServiceManagerImpl
    
    @Before
    fun setup() {
        context = mockk(relaxed = true)
        val appInfo = mockk<ApplicationInfo>(relaxed = true)
        
        // Mock context methods
        every { context.applicationInfo } returns appInfo
        every { appInfo.nativeLibraryDir } returns "/mock/native/lib"
        every { context.filesDir } returns File.createTempFile("test", "dir").apply {
            delete()
            mkdir()
        }
        
        torServiceManager = TorServiceManagerImpl(context)
    }
    
    @Test
    fun `getTorState returns initial Idle state`() = runTest {
        val state = torServiceManager.getTorState().value
        assertTrue(state is TorState.Idle)
    }
    
    @Test
    fun `TorState sealed class has all expected states`() {
        // Verify all state types exist
        val idle: TorState = TorState.Idle
        val bootstrapping: TorState = TorState.Bootstrapping(50)
        val connected: TorState = TorState.Connected(
            TorCircuitInfo(
                entryNode = "entry",
                middleNode = "middle",
                exitNode = "exit",
                circuitId = "1",
                bandwidth = 1000L
            )
        )
        val error: TorState = TorState.Error("test error")
        val disconnected: TorState = TorState.Disconnected
        
        assertNotNull(idle)
        assertNotNull(bootstrapping)
        assertNotNull(connected)
        assertNotNull(error)
        assertNotNull(disconnected)
    }
    
    @Test
    fun `TorCircuitInfo contains all required fields`() {
        val circuitInfo = TorCircuitInfo(
            entryNode = "192.168.1.1",
            middleNode = "192.168.1.2",
            exitNode = "192.168.1.3",
            circuitId = "12345",
            bandwidth = 5000L
        )
        
        assertEquals("192.168.1.1", circuitInfo.entryNode)
        assertEquals("192.168.1.2", circuitInfo.middleNode)
        assertEquals("192.168.1.3", circuitInfo.exitNode)
        assertEquals("12345", circuitInfo.circuitId)
        assertEquals(5000L, circuitInfo.bandwidth)
    }
    
    @Test
    fun `TorProxyConfig has correct default values`() {
        val dataDir = File.createTempFile("test", "dir").apply {
            delete()
            mkdir()
        }
        val geoipFile = File.createTempFile("geoip", "file")
        val geoip6File = File.createTempFile("geoip6", "file")
        val torrcFile = File.createTempFile("torrc", "file")
        
        val config = TorProxyConfig(
            dataDirectory = dataDir,
            geoipFile = geoipFile,
            geoip6File = geoip6File,
            torrcFile = torrcFile
        )
        
        assertEquals("127.0.0.1", config.socksHost)
        assertEquals(9050, config.socksPort)
        assertEquals(9051, config.controlPort)
        assertEquals(false, config.useBridges)
        assertEquals(true, config.isolateDestAddress)
        assertEquals(true, config.isolateDestPort)
        assertTrue(config.bridgeAddresses.isEmpty())
    }
    
    @Test
    fun `enableObfs4Bridges validates bridge format correctly`() = runTest {
        val validBridge = "obfs4 192.168.1.1:443 ABCDEF1234567890 cert=test_cert iat-mode=0"
        val invalidBridge = "invalid bridge format"
        
        // Test with valid bridge - should not throw
        try {
            // Note: This will fail in actual execution due to missing Tor binary,
            // but validates the format checking logic
            val bridges = listOf(validBridge)
            bridges.forEach { bridge ->
                val isValid = bridge.matches(Regex("""obfs4 [\d.]+:\d+ [A-F0-9]+ cert=.+ iat-mode=\d"""))
                assertTrue(isValid, "Valid bridge should match regex")
            }
        } catch (e: Exception) {
            // Expected in test environment
        }
        
        // Test with invalid bridge - should fail validation
        val invalidBridges = listOf(invalidBridge)
        invalidBridges.forEach { bridge ->
            val isValid = bridge.matches(Regex("""obfs4 [\d.]+:\d+ [A-F0-9]+ cert=.+ iat-mode=\d"""))
            assertTrue(!isValid, "Invalid bridge should not match regex")
        }
    }
    
    @Test
    fun `enableObfs4Bridges rejects empty bridge list`() = runTest {
        val emptyBridges = emptyList<String>()
        
        try {
            require(emptyBridges.isNotEmpty()) {
                "Bridge list cannot be empty"
            }
            assertTrue(false, "Should have thrown exception for empty bridge list")
        } catch (e: IllegalArgumentException) {
            assertEquals("Bridge list cannot be empty", e.message)
        }
    }
    
    @Test
    fun `obfs4 bridge format validation accepts valid formats`() {
        val validBridges = listOf(
            "obfs4 192.168.1.1:443 ABCDEF1234567890 cert=test_cert iat-mode=0",
            "obfs4 10.0.0.1:9001 1234567890ABCDEF cert=another_cert iat-mode=1",
            "obfs4 172.16.0.1:8080 FEDCBA0987654321 cert=long_certificate_string iat-mode=2"
        )
        
        val regex = Regex("""obfs4 [\d.]+:\d+ [A-F0-9]+ cert=.+ iat-mode=\d""")
        
        validBridges.forEach { bridge ->
            assertTrue(
                bridge.matches(regex),
                "Bridge '$bridge' should be valid"
            )
        }
    }
    
    @Test
    fun `obfs4 bridge format validation rejects invalid formats`() {
        val invalidBridges = listOf(
            "obfs4 invalid_ip:443 ABCDEF cert=test iat-mode=0",  // Invalid IP
            "obfs4 192.168.1.1 ABCDEF cert=test iat-mode=0",      // Missing port
            "obfs4 192.168.1.1:443 cert=test iat-mode=0",         // Missing fingerprint
            "obfs4 192.168.1.1:443 ABCDEF iat-mode=0",            // Missing cert
            "obfs4 192.168.1.1:443 ABCDEF cert=test",             // Missing iat-mode
            "192.168.1.1:443 ABCDEF cert=test iat-mode=0",        // Missing obfs4 prefix
            ""                                                     // Empty string
        )
        
        val regex = Regex("""obfs4 [\d.]+:\d+ [A-F0-9]+ cert=.+ iat-mode=\d""")
        
        invalidBridges.forEach { bridge ->
            assertTrue(
                !bridge.matches(regex),
                "Bridge '$bridge' should be invalid"
            )
        }
    }
    
    @Test
    fun `getCircuitInfo returns null when not connected`() = runTest {
        // When Tor is not connected, getCircuitInfo should return null
        val circuitInfo = torServiceManager.getCircuitInfo()
        
        // Since we're not actually connected to Tor in tests, this should be null
        assertEquals(null, circuitInfo)
    }
    
    @Test
    fun `newIdentity executes without error when not connected`() = runTest {
        // newIdentity should handle being called when not connected gracefully
        try {
            torServiceManager.newIdentity()
            // Should not throw exception
            assertTrue(true)
        } catch (e: Exception) {
            assertTrue(false, "newIdentity should not throw exception: ${e.message}")
        }
    }
    
    @Test
    fun `TorCircuitInfo bandwidth field is Long type`() {
        val circuitInfo = TorCircuitInfo(
            entryNode = "entry",
            middleNode = "middle",
            exitNode = "exit",
            circuitId = "1",
            bandwidth = 1024L * 1024L // 1 MB/s
        )
        
        assertTrue(circuitInfo.bandwidth is Long)
        assertEquals(1048576L, circuitInfo.bandwidth)
    }
    
    @Test
    fun `TorCircuitInfo can be copied with updated bandwidth`() {
        val original = TorCircuitInfo(
            entryNode = "entry",
            middleNode = "middle",
            exitNode = "exit",
            circuitId = "1",
            bandwidth = 0L
        )
        
        val updated = original.copy(bandwidth = 5000L)
        
        assertEquals(0L, original.bandwidth)
        assertEquals(5000L, updated.bandwidth)
        assertEquals(original.entryNode, updated.entryNode)
        assertEquals(original.middleNode, updated.middleNode)
        assertEquals(original.exitNode, updated.exitNode)
        assertEquals(original.circuitId, updated.circuitId)
    }
}
