package com.Azelmods.App.data.model

data class AIMessage(
    val id: String = "",
    val content: String = "",
    val role: String = "user", // "user" | "assistant" | "system"
    val timestamp: Long = System.currentTimeMillis(),
    val isLoading: Boolean = false,
    val error: Boolean = false,
    val model: String = "llama3.3:70b",
    val tokens: Int = 0,
    val attachments: List<String> = emptyList()
)
