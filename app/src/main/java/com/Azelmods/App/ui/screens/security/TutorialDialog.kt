package com.Azelmods.App.ui.screens.security

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/**
 * Tutorial Dialog
 * 
 * Displays Azel IA generated tutorial with syntax highlighting and copy buttons
 * 
 * Requirements: 14.1, 14.2, 14.3, 14.4, 14.5, 14.6, 14.7
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TutorialDialog(
    tutorialText: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Header
                TopAppBar(
                    title = { Text("Metasploit Handler Tutorial") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    }
                )
                
                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Parse and render tutorial sections
                    parseTutorialSections(tutorialText).forEach { section ->
                        TutorialSection(section)
                    }
                }
            }
        }
    }
}

@Composable
private fun TutorialSection(section: TutorialSectionData) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Section title
        if (section.title.isNotBlank()) {
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Section content
        when (section.type) {
            SectionType.TEXT -> {
                Text(
                    section.content,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            SectionType.CODE -> {
                CodeBlock(section.content)
            }
        }
    }
}

@Composable
private fun CodeBlock(code: String) {
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column {
            // Copy button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            // Code content
            Text(
                code,
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class TutorialSectionData(
    val title: String,
    val content: String,
    val type: SectionType
)

private enum class SectionType {
    TEXT, CODE
}

private fun parseTutorialSections(tutorialText: String): List<TutorialSectionData> {
    val sections = mutableListOf<TutorialSectionData>()
    val lines = tutorialText.lines()
    
    var currentTitle = ""
    var currentContent = StringBuilder()
    var inCodeBlock = false
    var currentType = SectionType.TEXT
    
    for (line in lines) {
        when {
            line.startsWith("# ") || line.startsWith("## ") -> {
                // Save previous section
                if (currentContent.isNotBlank()) {
                    sections.add(
                        TutorialSectionData(
                            title = currentTitle,
                            content = currentContent.toString().trim(),
                            type = currentType
                        )
                    )
                    currentContent = StringBuilder()
                }
                
                // Start new section
                currentTitle = line.removePrefix("# ").removePrefix("## ").trim()
                currentType = SectionType.TEXT
                inCodeBlock = false
            }
            line.startsWith("```") -> {
                // Toggle code block
                if (inCodeBlock) {
                    // End code block
                    sections.add(
                        TutorialSectionData(
                            title = "",
                            content = currentContent.toString().trim(),
                            type = SectionType.CODE
                        )
                    )
                    currentContent = StringBuilder()
                    currentType = SectionType.TEXT
                } else {
                    // Start code block
                    if (currentContent.isNotBlank()) {
                        sections.add(
                            TutorialSectionData(
                                title = currentTitle,
                                content = currentContent.toString().trim(),
                                type = SectionType.TEXT
                            )
                        )
                        currentTitle = ""
                        currentContent = StringBuilder()
                    }
                    currentType = SectionType.CODE
                }
                inCodeBlock = !inCodeBlock
            }
            else -> {
                if (line.isNotBlank() || inCodeBlock) {
                    currentContent.append(line).append("\n")
                }
            }
        }
    }
    
    // Add last section
    if (currentContent.isNotBlank()) {
        sections.add(
            TutorialSectionData(
                title = currentTitle,
                content = currentContent.toString().trim(),
                type = currentType
            )
        )
    }
    
    return sections
}
