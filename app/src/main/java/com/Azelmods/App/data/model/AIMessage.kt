package com.Azelmods.App.data.model

data class AIMessage(
    val id: String = "",
    val content: String = "",
    val role: String = "user", // "user" | "assistant" | "system"
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val error: Boolean = false,
    val model: String = "gpt-4-turbo-2024-04-09", // Modelo por defecto actualizado
    val tokens: Int = 0,
    val attachments: List<String> = emptyList(),
    val apiProvider: String = "opencode", // "opencode" | "ollama" | "local"
    val temperature: Double = 0.9,
    val maxTokens: Int = 8192
)
