package com.Azelmods.App.data.api

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Ollama API Service - Uncensored AI Integration
 * Supports local and remote Ollama instances
 */
class OllamaApiService(
    private val baseUrl: String = "http://localhost:11434"
) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    /**
     * Generate response from Ollama model
     * @param model Model name (e.g., "llama2", "mistral", "codellama")
     * @param prompt User prompt
     * @param systemPrompt System instructions (optional)
     * @param temperature Creativity level (0.0 - 2.0)
     * @param stream Enable streaming responses
     */
    suspend fun generate(
        model: String = "llama2",
        prompt: String,
        systemPrompt: String? = null,
        temperature: Double = 0.8,
        stream: Boolean = false
    ): Flow<String> = flow {
        val json = JSONObject().apply {
            put("model", model)
            put("prompt", prompt)
            systemPrompt?.let { put("system", it) }
            put("temperature", temperature)
            put("stream", stream)
        }
        
        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$baseUrl/api/generate")
            .post(requestBody)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit("Error: ${response.code} - ${response.message}")
                return@flow
            }
            
            val responseBody = response.body?.string() ?: ""
            
            if (stream) {
                // Parse streaming response
                responseBody.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            val jsonLine = JSONObject(line)
                            val text = jsonLine.optString("response", "")
                            if (text.isNotEmpty()) {
                                emit(text)
                            }
                        } catch (e: Exception) {
                            // Skip malformed lines
                        }
                    }
                }
            } else {
                // Parse complete response
                try {
                    val jsonResponse = JSONObject(responseBody)
                    val text = jsonResponse.optString("response", "")
                    emit(text)
                } catch (e: Exception) {
                    emit("Error parsing response: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Chat with conversation history
     */
    suspend fun chat(
        model: String = "llama2",
        messages: List<ChatMessage>,
        temperature: Double = 0.8,
        stream: Boolean = false
    ): Flow<String> = flow {
        val json = JSONObject().apply {
            put("model", model)
            put("messages", messages.map { it.toJson() })
            put("temperature", temperature)
            put("stream", stream)
        }
        
        val requestBody = json.toString()
            .toRequestBody("application/json".toMediaType())
        
        val request = Request.Builder()
            .url("$baseUrl/api/chat")
            .post(requestBody)
            .build()
        
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                emit("Error: ${response.code} - ${response.message}")
                return@flow
            }
            
            val responseBody = response.body?.string() ?: ""
            
            if (stream) {
                responseBody.lines().forEach { line ->
                    if (line.isNotBlank()) {
                        try {
                            val jsonLine = JSONObject(line)
                            val message = jsonLine.optJSONObject("message")
                            val text = message?.optString("content", "") ?: ""
                            if (text.isNotEmpty()) {
                                emit(text)
                            }
                        } catch (e: Exception) {
                            // Skip malformed lines
                        }
                    }
                }
            } else {
                try {
                    val jsonResponse = JSONObject(responseBody)
                    val message = jsonResponse.optJSONObject("message")
                    val text = message?.optString("content", "") ?: ""
                    emit(text)
                } catch (e: Exception) {
                    emit("Error parsing response: ${e.message}")
                }
            }
        }
    }
    
    /**
     * List available models
     */
    suspend fun listModels(): List<String> {
        val request = Request.Builder()
            .url("$baseUrl/api/tags")
            .get()
            .build()
        
        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                
                val responseBody = response.body?.string() ?: return emptyList()
                val json = JSONObject(responseBody)
                val models = json.optJSONArray("models") ?: return emptyList()
                
                (0 until models.length()).mapNotNull {
                    models.optJSONObject(it)?.optString("name")
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    /**
     * Check if Ollama server is running
     */
    suspend fun isServerAvailable(): Boolean {
        return try {
            val request = Request.Builder()
                .url(baseUrl)
                .get()
                .build()
            
            client.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            false
        }
    }
}

data class ChatMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("role", role)
        put("content", content)
    }
}
