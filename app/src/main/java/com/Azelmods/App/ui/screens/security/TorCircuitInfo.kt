package com.Azelmods.App.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Azelmods.App.data.security.tor.TorCircuitInfo
import com.Azelmods.App.ui.theme.Purple

/**
 * Displays current Tor circuit information
 * 
 * Shows:
 * - Entry, middle, and exit nodes
 * - Circuit ID
 * - Bandwidth information
 * - "New Identity" button to create a new circuit
 * 
 * Requirements: 17.6, 17.7, 17.8, 4.1, 4.2, 4.3, 4.4, 4.5, 4.6, 4.7
 */
@Composable
fun TorCircuitInfo(
    circuitInfo: TorCircuitInfo?,
    onNewIdentity: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (circuitInfo == null) {
        return
    }
    
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Circuit Information",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                // New Identity button
                IconButton(
                    onClick = onNewIdentity,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "New Identity",
                        tint = Purple
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Circuit ID
            CircuitInfoRow(
                label = "Circuit ID",
                value = circuitInfo.circuitId
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Entry Node
            CircuitInfoRow(
                label = "Entry Node",
                value = circuitInfo.entryNode,
                description = "Your connection enters Tor here"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Middle Node
            CircuitInfoRow(
                label = "Middle Node",
                value = circuitInfo.middleNode,
                description = "Traffic is relayed through this node"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Exit Node
            CircuitInfoRow(
                label = "Exit Node",
                value = circuitInfo.exitNode,
                description = "Your traffic exits Tor from here"
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Bandwidth
            CircuitInfoRow(
                label = "Bandwidth",
                value = formatBandwidth(circuitInfo.bandwidth)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // New Identity button (full width)
            Button(
                onClick = onNewIdentity,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text("Request New Identity")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Info text
            Text(
                text = "Creating a new identity will establish a new circuit with different nodes, " +
                        "making it harder to track your activity.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Row displaying circuit information
 */
@Composable
private fun CircuitInfoRow(
    label: String,
    value: String,
    description: String? = null
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontSize = 13.sp
            )
            
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
        
        if (description != null) {
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.DarkGray,
                fontSize = 11.sp
            )
        }
    }
}

/**
 * Formats bandwidth in human-readable format
 */
private fun formatBandwidth(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B/s"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB/s"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB/s"
        else -> "${bytes / (1024 * 1024 * 1024)} GB/s"
    }
}
