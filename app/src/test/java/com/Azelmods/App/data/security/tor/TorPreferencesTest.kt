package com.Azelmods.App.data.security.tor

import android.content.Context
import android.content.SharedPreferences
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for TorPreferences
 * 
 * Tests persistence of Tor settings:
 * - Anonymous Mode enabled/disabled state
 * - Bridge configuration
 * 
 * Requirements: 23.1, 23.2, 23.3
 */
class TorPreferencesTest {
    
    @Mock
    private lateinit var mockContext: Context
    
    @Mock
    private lateinit var mockSharedPreferences: SharedPreferences
    
    @Mock
    private lateinit var mockEditor: SharedPreferences.Editor
    
    private lateinit var torPreferences: TorPreferences
    
    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        
        // Setup mock behavior
        `when`(mockContext.getSharedPreferences(anyString(), anyInt()))
            .thenReturn(mockSharedPreferences)
        `when`(mockSharedPreferences.edit()).thenReturn(mockEditor)
        `when`(mockEditor.putBoolean(anyString(), anyBoolean())).thenReturn(mockEditor)
        `when`(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor)
        
        torPreferences = TorPreferences(mockContext)
    }
    
    @Test
    fun `test setAnonymousModeEnabled saves preference`() {
        // Requirement 23.1: Save Anonymous Mode preference
        torPreferences.setAnonymousModeEnabled(true)
        
        verify(mockEditor).putBoolean("anonymous_mode_enabled", true)
        verify(mockEditor).apply()
    }
    
    @Test
    fun `test isAnonymousModeEnabled returns saved value`() {
        // Requirement 23.3: Restore Anonymous Mode state
        `when`(mockSharedPreferences.getBoolean("anonymous_mode_enabled", false))
            .thenReturn(true)
        
        val result = torPreferences.isAnonymousModeEnabled()
        
        assertTrue(result)
        verify(mockSharedPreferences).getBoolean("anonymous_mode_enabled", false)
    }
    
    @Test
    fun `test isAnonymousModeEnabled returns false by default`() {
        `when`(mockSharedPreferences.getBoolean("anonymous_mode_enabled", false))
            .thenReturn(false)
        
        val result = torPreferences.isAnonymousModeEnabled()
        
        assertFalse(result)
    }
    
    @Test
    fun `test saveBridgeConfiguration saves bridges`() {
        // Requirement 23.2: Save bridge configuration
        val bridges = listOf(
            "obfs4 192.0.2.1:1234 ABCD1234 cert=xyz iat-mode=0",
            "obfs4 192.0.2.2:5678 EFGH5678 cert=abc iat-mode=1"
        )
        
        torPreferences.saveBridgeConfiguration(bridges)
        
        val expectedString = bridges.joinToString("|||")
        verify(mockEditor).putString("bridge_addresses", expectedString)
        verify(mockEditor).putBoolean("use_bridges", true)
        verify(mockEditor).apply()
    }
    
    @Test
    fun `test saveBridgeConfiguration with empty list`() {
        val bridges = emptyList<String>()
        
        torPreferences.saveBridgeConfiguration(bridges)
        
        verify(mockEditor).putString("bridge_addresses", "")
        verify(mockEditor).putBoolean("use_bridges", false)
        verify(mockEditor).apply()
    }
    
    @Test
    fun `test getBridgeAddresses returns saved bridges`() {
        // Requirement 23.3: Restore bridge configuration
        val bridgesString = "obfs4 192.0.2.1:1234 ABCD1234 cert=xyz iat-mode=0|||obfs4 192.0.2.2:5678 EFGH5678 cert=abc iat-mode=1"
        `when`(mockSharedPreferences.getString("bridge_addresses", ""))
            .thenReturn(bridgesString)
        
        val result = torPreferences.getBridgeAddresses()
        
        assertEquals(2, result.size)
        assertEquals("obfs4 192.0.2.1:1234 ABCD1234 cert=xyz iat-mode=0", result[0])
        assertEquals("obfs4 192.0.2.2:5678 EFGH5678 cert=abc iat-mode=1", result[1])
    }
    
    @Test
    fun `test getBridgeAddresses returns empty list when no bridges saved`() {
        `when`(mockSharedPreferences.getString("bridge_addresses", ""))
            .thenReturn("")
        
        val result = torPreferences.getBridgeAddresses()
        
        assertTrue(result.isEmpty())
    }
    
    @Test
    fun `test areBridgesConfigured returns true when bridges are configured`() {
        `when`(mockSharedPreferences.getBoolean("use_bridges", false))
            .thenReturn(true)
        
        val result = torPreferences.areBridgesConfigured()
        
        assertTrue(result)
    }
    
    @Test
    fun `test areBridgesConfigured returns false when no bridges configured`() {
        `when`(mockSharedPreferences.getBoolean("use_bridges", false))
            .thenReturn(false)
        
        val result = torPreferences.areBridgesConfigured()
        
        assertFalse(result)
    }
    
    @Test
    fun `test clearAll clears all preferences`() {
        torPreferences.clearAll()
        
        verify(mockEditor).clear()
        verify(mockEditor).apply()
    }
}
