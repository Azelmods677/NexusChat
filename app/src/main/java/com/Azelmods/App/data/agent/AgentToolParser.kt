package com.Azelmods.App.data.agent

import android.content.Context
import android.util.Log

/**
 * Agent Tool Parser - Detecta y ejecuta herramientas en respuestas de Ollama
 * 
 * Formato esperado en la respuesta de Ollama:
 * ```tool
 * shell: ls -la
 * ```
 * 
 * o
 * 
 * TOOL: shell
 * ARGS: ls -la
 */
class AgentToolParser(context: Context) {
    
    private val tools = LocalAgentTools(context)
    
    companion object {
        private const val TAG = "AgentToolParser"
        private val TOOL_PATTERN_1 = Regex("```tool\\s*\\n([^`]+)```", RegexOption.IGNORE_CASE)
        private val TOOL_PATTERN_2 = Regex("TOOL:\\s*([\\w_]+)\\s*\\nARGS:\\s*(.+)", RegexOption.IGNORE_CASE)
        private val INLINE_PATTERN = Regex("\\[EXECUTE:\\s*([\\w_]+)(?::\\s*(.+?))?\\]", RegexOption.IGNORE_CASE)
    }
    
    /**
     * Parsear y ejecutar herramientas en el texto
     */
    suspend fun parseAndExecute(text: String): ParsedResponse {
        val toolCalls = mutableListOf<ToolCall>()
        var modifiedText = text
        
        // Pattern 1: ```tool ... ```
        TOOL_PATTERN_1.findAll(text).forEach { match ->
            val toolCommand = match.groupValues[1].trim()
            val toolCall = parseToolCommand(toolCommand)
            if (toolCall != null) {
                toolCalls.add(toolCall)
                // Replace with execution result
                val result = executeToolCall(toolCall)
                modifiedText = modifiedText.replace(match.value, formatToolResult(result))
            }
        }
        
        // Pattern 2: TOOL: ... ARGS: ...
        TOOL_PATTERN_2.findAll(text).forEach { match ->
            val toolName = match.groupValues[1].trim()
            val args = match.groupValues[2].trim()
            val toolCall = ToolCall(toolName, args)
            toolCalls.add(toolCall)
            
            val result = executeToolCall(toolCall)
            modifiedText = modifiedText.replace(match.value, formatToolResult(result))
        }
        
        // Pattern 3: [EXECUTE: tool: args]
        INLINE_PATTERN.findAll(text).forEach { match ->
            val toolName = match.groupValues[1].trim()
            val args = match.groupValues.getOrNull(2)?.trim() ?: ""
            val toolCall = ToolCall(toolName, args)
            toolCalls.add(toolCall)
            
            val result = executeToolCall(toolCall)
            modifiedText = modifiedText.replace(match.value, formatToolResult(result))
        }
        
        return ParsedResponse(
            originalText = text,
            modifiedText = modifiedText,
            toolCalls = toolCalls,
            hasTools = toolCalls.isNotEmpty()
        )
    }
    
    /**
     * Parsear comando de herramienta
     */
    private fun parseToolCommand(command: String): ToolCall? {
        val parts = command.split(":", limit = 2)
        if (parts.size < 1) return null
        
        val toolName = parts[0].trim()
        val args = if (parts.size > 1) parts[1].trim() else ""
        
        return ToolCall(toolName, args)
    }
    
    /**
     * Ejecutar llamada a herramienta
     */
    private suspend fun executeToolCall(toolCall: ToolCall): ToolResult {
        Log.d(TAG, "Executing tool: ${toolCall.name} with args: ${toolCall.args}")
        
        return when (toolCall.name.lowercase()) {
            "shell" -> tools.executeShellCommand(toolCall.args)
            "system_info" -> tools.getSystemInfo()
            "network_scan" -> tools.scanNetwork()
            "list_files" -> tools.listFiles(toolCall.args)
            "read_file" -> tools.readFile(toolCall.args)
            "write_file" -> {
                val parts = toolCall.args.split("|", limit = 2)
                if (parts.size < 2) {
                    ToolResult.Error("write_file", "Invalid format. Use: filename | content")
                } else {
                    tools.writeFile(parts[0].trim(), parts[1].trim())
                }
            }
            "processes" -> tools.getProcesses()
            "ping" -> tools.ping(toolCall.args)
            else -> ToolResult.Error(toolCall.name, "Unknown tool: ${toolCall.name}")
        }
    }
    
    /**
     * Formatear resultado de herramienta para mostrar
     */
    private fun formatToolResult(result: ToolResult): String {
        return when (result) {
            is ToolResult.Success -> {
                buildString {
                    appendLine("\n🔧 **Tool: ${result.tool}**")
                    appendLine("```")
                    appendLine(result.output)
                    appendLine("```")
                    if (result.metadata.isNotEmpty()) {
                        appendLine("_Metadata: ${result.metadata}_")
                    }
                }
            }
            is ToolResult.Error -> {
                "\n❌ **Tool Error (${result.tool})**: ${result.message}\n"
            }
        }
    }
    
    /**
     * Obtener prompt del sistema para Ollama
     */
    fun getSystemPrompt(): String {
        val availableTools = tools.listAvailableTools()
        
        return buildString {
            appendLine("You are an advanced AI agent with access to system tools.")
            appendLine("You can execute commands and interact with the Android system.")
            appendLine()
            appendLine("AVAILABLE TOOLS:")
            appendLine("═══════════════")
            availableTools.forEach { tool ->
                appendLine("• ${tool.name}: ${tool.description}")
                appendLine("  Usage: ${tool.usage}")
                appendLine("  Example: ${tool.example}")
                appendLine()
            }
            appendLine()
            appendLine("HOW TO USE TOOLS:")
            appendLine("═════════════════")
            appendLine("To execute a tool, use one of these formats:")
            appendLine()
            appendLine("Format 1 (Code block):")
            appendLine("```tool")
            appendLine("tool_name: arguments")
            appendLine("```")
            appendLine()
            appendLine("Format 2 (Structured):")
            appendLine("TOOL: tool_name")
            appendLine("ARGS: arguments")
            appendLine()
            appendLine("Format 3 (Inline):")
            appendLine("[EXECUTE: tool_name: arguments]")
            appendLine()
            appendLine("The tool will be executed automatically and results will be shown.")
            appendLine("Always explain what you're doing before executing a tool.")
        }
    }
}

/**
 * Llamada a herramienta
 */
data class ToolCall(
    val name: String,
    val args: String
)

/**
 * Respuesta parseada
 */
data class ParsedResponse(
    val originalText: String,
    val modifiedText: String,
    val toolCalls: List<ToolCall>,
    val hasTools: Boolean
)
