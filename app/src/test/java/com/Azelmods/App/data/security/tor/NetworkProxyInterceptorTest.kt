package com.Azelmods.App.data.security.tor

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for NetworkProxyInterceptor
 * 
 * Tests:
 * - User agent is added when Tor is connected
 * - User agent is randomized across multiple requests
 * - Original request is used when Tor is not connected
 * - Interceptor handles different Tor states correctly
 */
class NetworkProxyInterceptorTest {
    
    private lateinit var torManager: TorServiceManager
    private lateinit var interceptor: NetworkProxyInterceptor
    private lateinit var torStateFlow: MutableStateFlow<TorState>
    
    @Before
    fun setup() {
        torManager = mockk()
        torStateFlow = MutableStateFlow(TorState.Idle)
        every { torManager.getTorState() } returns torStateFlow
        
        interceptor = NetworkProxyInterceptor(torManager)
    }
    
    @Test
    fun `intercept adds user agent when Tor is connected`() {
        // Given: Tor is connected
        val circuitInfo = TorCircuitInfo(
            entryNode = "entry.node",
            middleNode = "middle.node",
            exitNode = "exit.node",
            circuitId = "circuit123",
            bandwidth = 1024L
        )
        torStateFlow.value = TorState.Connected(circuitInfo)
        
        // Mock chain and request
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder()
            .url("https://example.com")
            .build()
        val response = mockk<Response>()
        
        var capturedRequest: Request? = null
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            capturedRequest = firstArg()
            response
        }
        
        // When: Interceptor processes the request
        val result = interceptor.intercept(chain)
        
        // Then: User-Agent header is added
        assertEquals(response, result)
        verify { chain.proceed(any()) }
        
        val userAgent = capturedRequest?.header("User-Agent")
        assertNotEquals(null, userAgent)
        assertNotEquals("", userAgent)
    }
    
    @Test
    fun `intercept does not modify request when Tor is idle`() {
        // Given: Tor is idle
        torStateFlow.value = TorState.Idle
        
        // Mock chain and request
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder()
            .url("https://example.com")
            .build()
        val response = mockk<Response>()
        
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response
        
        // When: Interceptor processes the request
        val result = interceptor.intercept(chain)
        
        // Then: Original request is used
        assertEquals(response, result)
        verify { chain.proceed(request) }
    }
    
    @Test
    fun `intercept does not modify request when Tor is bootstrapping`() {
        // Given: Tor is bootstrapping
        torStateFlow.value = TorState.Bootstrapping(50)
        
        // Mock chain and request
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder()
            .url("https://example.com")
            .build()
        val response = mockk<Response>()
        
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response
        
        // When: Interceptor processes the request
        val result = interceptor.intercept(chain)
        
        // Then: Original request is used
        assertEquals(response, result)
        verify { chain.proceed(request) }
    }
    
    @Test
    fun `intercept does not modify request when Tor has error`() {
        // Given: Tor has an error
        torStateFlow.value = TorState.Error("Connection failed", null)
        
        // Mock chain and request
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder()
            .url("https://example.com")
            .build()
        val response = mockk<Response>()
        
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response
        
        // When: Interceptor processes the request
        val result = interceptor.intercept(chain)
        
        // Then: Original request is used
        assertEquals(response, result)
        verify { chain.proceed(request) }
    }
    
    @Test
    fun `intercept does not modify request when Tor is disconnected`() {
        // Given: Tor is disconnected
        torStateFlow.value = TorState.Disconnected
        
        // Mock chain and request
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder()
            .url("https://example.com")
            .build()
        val response = mockk<Response>()
        
        every { chain.request() } returns request
        every { chain.proceed(request) } returns response
        
        // When: Interceptor processes the request
        val result = interceptor.intercept(chain)
        
        // Then: Original request is used
        assertEquals(response, result)
        verify { chain.proceed(request) }
    }
    
    @Test
    fun `user agent is randomized across multiple requests`() {
        // Given: Tor is connected
        val circuitInfo = TorCircuitInfo(
            entryNode = "entry.node",
            middleNode = "middle.node",
            exitNode = "exit.node",
            circuitId = "circuit123",
            bandwidth = 1024L
        )
        torStateFlow.value = TorState.Connected(circuitInfo)
        
        val userAgents = mutableSetOf<String>()
        
        // When: Multiple requests are made
        repeat(20) {
            val chain = mockk<Interceptor.Chain>()
            val request = Request.Builder()
                .url("https://example.com")
                .build()
            val response = mockk<Response>()
            
            var capturedRequest: Request? = null
            every { chain.request() } returns request
            every { chain.proceed(any()) } answers {
                capturedRequest = firstArg()
                response
            }
            
            interceptor.intercept(chain)
            
            capturedRequest?.header("User-Agent")?.let { userAgents.add(it) }
        }
        
        // Then: Multiple different user agents are used (randomization works)
        // With 20 requests and 6 user agents, we should see at least 2 different ones
        assert(userAgents.size >= 2) {
            "Expected at least 2 different user agents, but got ${userAgents.size}"
        }
    }
    
    @Test
    fun `user agent contains valid browser information`() {
        // Given: Tor is connected
        val circuitInfo = TorCircuitInfo(
            entryNode = "entry.node",
            middleNode = "middle.node",
            exitNode = "exit.node",
            circuitId = "circuit123",
            bandwidth = 1024L
        )
        torStateFlow.value = TorState.Connected(circuitInfo)
        
        // Mock chain and request
        val chain = mockk<Interceptor.Chain>()
        val request = Request.Builder()
            .url("https://example.com")
            .build()
        val response = mockk<Response>()
        
        var capturedRequest: Request? = null
        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            capturedRequest = firstArg()
            response
        }
        
        // When: Interceptor processes the request
        interceptor.intercept(chain)
        
        // Then: User-Agent contains Mozilla (standard browser identifier)
        val userAgent = capturedRequest?.header("User-Agent")
        assertNotEquals(null, userAgent)
        assert(userAgent!!.contains("Mozilla")) {
            "User agent should contain 'Mozilla', but was: $userAgent"
        }
    }
}
