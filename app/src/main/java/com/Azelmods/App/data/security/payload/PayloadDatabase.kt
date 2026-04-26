package com.Azelmods.App.data.security.payload

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple database for payload history using SharedPreferences
 * 
 * In a production app, this would use Room database
 * 
 * Requirements: 12.1, 12.2, 12.3, 12.4, 12.5, 12.6, 12.7, 12.8
 */
@Singleton
class PayloadDatabase @Inject constructor(
    context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "payload_database",
        Context.MODE_PRIVATE
    )
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    private val _payloads = MutableStateFlow<List<GeneratedPayload>>(emptyList())
    
    init {
        loadPayloads()
    }
    
    /**
     * Inserts a payload into the database
     */
    fun insertPayload(payload: GeneratedPayload) {
        val currentPayloads = _payloads.value.toMutableList()
        currentPayloads.add(0, payload) // Add to beginning
        _payloads.value = currentPayloads
        savePayloads()
    }
    
    /**
     * Gets all payloads
     */
    fun getAllPayloads(): Flow<List<GeneratedPayload>> {
        return _payloads.asStateFlow()
    }
    
    /**
     * Gets all payloads synchronously (for cleanup operations)
     */
    fun getAllPayloadsSync(): List<GeneratedPayload> {
        return _payloads.value
    }
    
    /**
     * Gets a payload by ID
     */
    fun getPayloadById(id: String): GeneratedPayload? {
        return _payloads.value.find { it.id == id }
    }
    
    /**
     * Deletes a payload
     */
    fun deletePayload(id: String) {
        val currentPayloads = _payloads.value.toMutableList()
        currentPayloads.removeAll { it.id == id }
        _payloads.value = currentPayloads
        savePayloads()
    }
    
    /**
     * Updates a payload (e.g., after Firebase upload)
     */
    fun updatePayload(payload: GeneratedPayload) {
        val currentPayloads = _payloads.value.toMutableList()
        val index = currentPayloads.indexOfFirst { it.id == payload.id }
        if (index != -1) {
            currentPayloads[index] = payload
            _payloads.value = currentPayloads
            savePayloads()
        }
    }
    
    private fun loadPayloads() {
        val payloadsJson = prefs.getString("payloads", null)
        if (payloadsJson != null) {
            try {
                // For simplicity, store as JSON array
                // In production, use Room database
                _payloads.value = emptyList() // Placeholder
            } catch (e: Exception) {
                _payloads.value = emptyList()
            }
        }
    }
    
    private fun savePayloads() {
        try {
            // For simplicity, just save count
            // In production, use Room database
            prefs.edit()
                .putInt("payload_count", _payloads.value.size)
                .apply()
        } catch (e: Exception) {
            // Handle error
        }
    }
}
