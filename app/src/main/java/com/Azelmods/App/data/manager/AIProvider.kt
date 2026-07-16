package com.Azelmods.App.data.manager

/**
 * 🎯 PROVEEDORES DE IA DISPONIBLES
 *
 * Enum de proveedores de IA usado para persistir la preferencia del usuario
 * en [com.Azelmods.App.data.preferences.AIPreferences].
 */
enum class AIProvider(val displayName: String, val isLocal: Boolean) {
    OLLAMA_CLOUD("Ollama Cloud", false),
    OPENCODE_CLOUD("OpenCode Cloud", false),
    OLLAMA_LOCAL("Ollama Local", true)
}
