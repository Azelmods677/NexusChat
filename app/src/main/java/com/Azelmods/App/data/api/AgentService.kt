package com.Azelmods.App.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Agent Service - Advanced Backend Integration
 * 
 * Integrates with backend server via ACP (Agent Client Protocol)
 * Capabilities: Shell commands, file operations, web search, code generation
 */
class AgentService(
    private val serverUrl: String = "http://localhost:8080" // Backend server
) {
    
    companion object {
        private const val TAG = "AgentService"
    }
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    private var sessionId: String? = null
    
    /**
     * Initialize agent session
     */
    suspend fun initializeSession(workspace: String = "/tmp/workspace"): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val requestBody = JSONObject().apply {
                put("workspace", workspace)
                put("trustAllTools", true) // Permite ejecución sin aprobación
                put("capabilities", JSONObject().apply {
                    put("shell", true)
                    put("fs", true)
                    put("web", true)
                    put("code", true)
                })
            }
            
            val request = Request.Builder()
                .url("$serverUrl/api/agent/session/new")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to initialize session: ${response.code} - $responseBody")
            }
            
            val json = JSONObject(responseBody)
            sessionId = json.getString("sessionId")
            
            Log.d(TAG, "Agent session initialized: $sessionId")
            sessionId!!
        }
    }
    
    /**
     * Send message to agent with streaming response
     */
    suspend fun sendMessage(
        message: String,
        conversationHistory: List<AgentMessage> = emptyList()
    ): Flow<AgentResponse> = flow {
        if (sessionId == null) {
            initializeSession().getOrThrow()
        }
        
        val requestBody = JSONObject().apply {
            put("sessionId", sessionId)
            put("message", message)
            put("history", JSONArray().apply {
                conversationHistory.forEach { msg ->
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            })
            put("stream", true)
        }
        
        val request = Request.Builder()
            .url("$serverUrl/api/agent/chat")
            .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
            .build()
        
        withContext(Dispatchers.IO) {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    emit(AgentResponse.Error("Request failed: ${response.code}"))
                    return@withContext
                }
                
                response.body?.charStream()?.buffered()?.use { reader ->
                    reader.lineSequence().forEach { line ->
                        if (line.isNotBlank()) {
                            try {
                                val json = JSONObject(line)
                                val type = json.optString("type", "")
                                
                                when (type) {
                                    "text" -> {
                                        val content = json.getString("content")
                                        emit(AgentResponse.TextChunk(content))
                                    }
                                    "tool_use" -> {
                                        val toolName = json.getString("tool")
                                        val toolInput = json.optJSONObject("input")?.toString() ?: "{}"
                                        emit(AgentResponse.ToolExecution(toolName, toolInput))
                                    }
                                    "tool_result" -> {
                                        val result = json.getString("result")
                                        emit(AgentResponse.ToolResult(result))
                                    }
                                    "complete" -> {
                                        emit(AgentResponse.Complete)
                                    }
                                    "error" -> {
                                        val error = json.getString("message")
                                        emit(AgentResponse.Error(error))
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing response line: $line", e)
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Execute shell command directly
     */
    suspend fun executeCommand(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionId == null) {
                initializeSession().getOrThrow()
            }
            
            val requestBody = JSONObject().apply {
                put("sessionId", sessionId)
                put("command", command)
            }
            
            val request = Request.Builder()
                .url("$serverUrl/api/agent/execute")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Command execution failed: ${response.code} - $responseBody")
            }
            
            val json = JSONObject(responseBody)
            json.getString("output")
        }
    }
    
    /**
     * List available tools/capabilities
     */
    suspend fun listTools(): Result<List<AgentTool>> = withContext(Dispatchers.IO) {
        runCatching {
            val request = Request.Builder()
                .url("$serverUrl/api/agent/tools")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            
            if (!response.isSuccessful) {
                throw Exception("Failed to list tools: ${response.code}")
            }
            
            val json = JSONObject(responseBody)
            val toolsArray = json.getJSONArray("tools")
            
            (0 until toolsArray.length()).map { i ->
                val tool = toolsArray.getJSONObject(i)
                AgentTool(
                    name = tool.getString("name"),
                    description = tool.getString("description"),
                    category = tool.getString("category")
                )
            }
        }
    }
    
    /**
     * Check if server is available
     */
    suspend fun isServerAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("$serverUrl/api/health")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            response.isSuccessful
        } catch (e: Exception) {
            Log.e(TAG, "Server not available", e)
            false
        }
    }
    
    /**
     * Close current session
     */
    suspend fun closeSession(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (sessionId == null) return@runCatching
            
            val requestBody = JSONObject().apply {
                put("sessionId", sessionId)
            }
            
            val request = Request.Builder()
                .url("$serverUrl/api/agent/session/close")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()
            
            client.newCall(request).execute()
            sessionId = null
            
            Log.d(TAG, "Agent session closed")
        }
    }
}

/**
 * Agent message for conversation history
 */
data class AgentMessage(
    val role: String, // "user", "assistant", "system"
    val content: String
)

/**
 * Agent response types
 */
sealed class AgentResponse {
    data class TextChunk(val content: String) : AgentResponse()
    data class ToolExecution(val toolName: String, val input: String) : AgentResponse()
    data class ToolResult(val result: String) : AgentResponse()
    data class Error(val message: String) : AgentResponse()
    object Complete : AgentResponse()
}

/**
 * Agent tool definition
 */
data class AgentTool(
    val name: String,
    val description: String,
    val category: String // "shell", "fs", "web", "code"
)
