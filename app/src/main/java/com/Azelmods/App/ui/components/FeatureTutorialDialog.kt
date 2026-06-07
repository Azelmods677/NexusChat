package com.Azelmods.App.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

/**
 * Generic Feature Tutorial Dialog
 *
 * Displays interactive tutorials for any app feature with markdown-style formatting.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeatureTutorialDialog(
    title: String,
    tutorialText: String,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                TopAppBar(
                    title = {
                        Text(
                            title,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Cerrar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )

                // Content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val sections = parseTutorialSections(tutorialText)
                    sections.forEachIndexed { index, section ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(tween(200 + index * 80)) +
                                    slideInVertically(
                                        initialOffsetY = { it / 3 },
                                        animationSpec = tween(200 + index * 80)
                                    )
                        ) {
                            TutorialSection(section)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TutorialSection(section: TutorialSectionData) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Section title
        if (section.title.isNotBlank()) {
            Text(
                section.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Section content by type
        when (section.type) {
            SectionType.TEXT -> {
                Text(
                    section.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            SectionType.CODE -> {
                CodeBlock(section.content)
            }
            SectionType.WARNING -> {
                CalloutBlock(
                    icon = "⚠️",
                    content = section.content,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            SectionType.INFO -> {
                CalloutBlock(
                    icon = "ℹ️",
                    content = section.content,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
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
        ),
        shape = MaterialTheme.shapes.small
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(4.dp),
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
                        contentDescription = "Copiar",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

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

@Composable
private fun CalloutBlock(
    icon: String,
    content: String,
    containerColor: Color,
    contentColor: Color
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = containerColor),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(icon, style = MaterialTheme.typography.titleLarge)
            Text(
                content,
                style = MaterialTheme.typography.bodyMedium,
                color = contentColor
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
    TEXT, CODE, WARNING, INFO
}

/**
 * Parses markdown-style tutorial text into structured sections.
 *
 * Supports:
 * - `# / ## / ###` headers → new TEXT section
 * - Triple backticks ``` → CODE block
 * - Lines starting with ⚠️ → WARNING callout
 * - Lines starting with ℹ️ → INFO callout
 */
private fun parseTutorialSections(tutorialText: String): List<TutorialSectionData> {
    val sections = mutableListOf<TutorialSectionData>()
    val lines = tutorialText.lines()

    var currentTitle = ""
    var currentContent = StringBuilder()
    var inCodeBlock = false
    var currentType = SectionType.TEXT

    fun flushCurrentSection() {
        if (currentContent.isNotBlank()) {
            sections.add(
                TutorialSectionData(
                    title = currentTitle,
                    content = currentContent.toString().trim(),
                    type = currentType
                )
            )
        }
    }

    for (line in lines) {
        when {
            // Toggle code block (triple backticks)
            line.trimStart().startsWith("```") -> {
                if (inCodeBlock) {
                    // End code block
                    flushCurrentSection()
                    currentContent = StringBuilder()
                    currentTitle = ""
                } else {
                    // Start code block
                    flushCurrentSection()
                    currentContent = StringBuilder()
                    currentTitle = ""
                    currentType = SectionType.CODE
                }
                inCodeBlock = !inCodeBlock
            }
            // Inside code block — append raw lines
            inCodeBlock -> {
                currentContent.append(line).append("\n")
            }
            // Headers
            line.startsWith("# ") || line.startsWith("## ") || line.startsWith("### ") -> {
                flushCurrentSection()
                currentTitle = line.removePrefix("### ").removePrefix("## ").removePrefix("# ").trim()
                currentContent = StringBuilder()
                currentType = SectionType.TEXT
            }
            // Warning line
            line.trimStart().startsWith("⚠️") -> {
                flushCurrentSection()
                currentType = SectionType.WARNING
                currentTitle = ""
                currentContent = StringBuilder()
                currentContent.append(line.trimStart().removePrefix("⚠️").trim())
            }
            // Info line
            line.trimStart().startsWith("ℹ️") -> {
                flushCurrentSection()
                currentType = SectionType.INFO
                currentTitle = ""
                currentContent = StringBuilder()
                currentContent.append(line.trimStart().removePrefix("ℹ️").trim())
            }
            // Continuation of WARNING or INFO (indented or next line)
            currentType == SectionType.WARNING || currentType == SectionType.INFO -> {
                if (line.isNotBlank()) {
                    currentContent.append("\n").append(line.trim())
                }
            }
            // Regular text
            else -> {
                if (line.isNotBlank() || currentContent.isNotBlank()) {
                    currentContent.append(line).append("\n")
                }
            }
        }
    }

    // Flush remaining
    flushCurrentSection()

    return sections
}
