package com.Azelmods.App.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.Azelmods.App.data.security.payload.GeneratedPayload

/**
 * Payload Success Card
 * 
 * Displays payload details with actions (Share, Share in Chat, Tutorial, Delete)
 * 
 * Requirements: 18.8, 13.1, 13.4, 13.5
 */
@Composable
fun PayloadSuccessCard(
    payload: GeneratedPayload,
    onShare: () -> Unit,
    onShareInChat: () -> Unit,
    onShowTutorial: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Text(
                "✓ Payload Generated Successfully",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            HorizontalDivider()
            
            // Payload Details
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Type", payload.type.displayName)
                DetailRow("LHOST", payload.lhost)
                DetailRow("LPORT", payload.lport.toString())
                DetailRow("File Name", payload.fileName)
                DetailRow("File Size", "${payload.fileSize / 1024} KB")
                DetailRow("Obfuscation", payload.obfuscationLevel.name)
                
                // SHA256 Hash (monospace font)
                Column {
                    Text(
                        "SHA256 Hash:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        payload.sha256Hash,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                // Features
                if (payload.hasAntiEmulator || payload.hasAntiDebug || payload.hasPersistence) {
                    Column {
                        Text(
                            "Features:",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (payload.hasAntiEmulator) {
                            Text("• Anti-Emulator Detection", style = MaterialTheme.typography.bodySmall)
                        }
                        if (payload.hasAntiDebug) {
                            Text("• Anti-Debug Protection", style = MaterialTheme.typography.bodySmall)
                        }
                        if (payload.hasPersistence) {
                            Text("• Persistence Mechanisms", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
            
            HorizontalDivider()
            
            // Action Buttons
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // First Row: Share and Share in Chat
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Share Button
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share")
                    }
                    
                    // Share in Chat Button
                    Button(
                        onClick = onShareInChat,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Chat")
                    }
                }
                
                // Second Row: Tutorial and Delete
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Tutorial Button
                    Button(
                        onClick = onShowTutorial,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Tutorial")
                    }
                    
                    // Delete Button
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "$label:",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
