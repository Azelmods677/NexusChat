package com.Azelmods.App.ui.screens.security

import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.preferences.TutorialPreferences
import com.Azelmods.App.data.security.tor.TorCircuitInfo
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.data.tutorials.AppFeature
import com.Azelmods.App.ui.components.AutoTutorial
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.Purple

/**
 * Main security screen - entry point for all security features
 *
 * Displays:
 * - Tor Integration section with AnonymousModeToggle
 * - Circuit info card when connected
 * - New Circuit button when connected
 * - Connection logs (collapsed by default)
 * - Payload Generator section (optional, to be implemented)
 * - Navigation to detailed security screens
 *
 * Requirements: 17.1, 18.1, 36.1, 36.2, 36.3
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    navController: NavController,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val tutorialPreferences = remember { TutorialPreferences(context) }
    val torState by viewModel.torState.collectAsState()
    val torLogs by viewModel.torLogs.collectAsState()
    val circuitInfo by viewModel.circuitInfo.collectAsState()

    // Show tutorial on first visit
    AutoTutorial(
        feature = AppFeature.SECURITY,
        tutorialPreferences = tutorialPreferences
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security & Privacy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Advanced Security Features",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Protect your privacy with military-grade encryption and anonymous networking",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tor Integration ──────────────────────────────────────────────
            SecurityFeatureCard(
                title = "Anonymous Mode (Tor)",
                description = "Route all traffic through the Tor network for maximum privacy",
                icon = Icons.Default.Security,
                isActive = torState is TorState.Connected,
                onClick = { navController.navigate(Screen.TorControl.route) }
            ) {
                // Inline toggle for quick access
                AnonymousModeToggle(
                    torState = torState,
                    onToggle = { enabled ->
                        if (enabled) viewModel.enableAnonymousMode()
                        else viewModel.disableAnonymousMode()
                    }
                )

                // Circuit info card when fully connected
                if (torState is TorState.Connected) {
                    TorCircuitInfoCard(circuitInfo = circuitInfo)
                }

                // Detailed progress when actively connecting
                if (torState is TorState.Connecting) {
                    val connecting = torState as TorState.Connecting
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = connecting.progress / 100f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = Purple,
                        trackColor = Color.DarkGray
                    )
                    if (connecting.message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = connecting.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // New Circuit button (shown only when connected)
            if (torState is TorState.Connected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.requestNewCircuit() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Purple)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("New Circuit")
                    }
                }
            }

            // ── Tor Browser ──────────────────────────────────────────────────
            SecurityFeatureCard(
                title = "Tor Browser",
                description = "Browse .onion sites anonymously with DuckDuckGo integration",
                icon = Icons.Default.Language,
                isActive = false,
                onClick = { navController.navigate(Screen.TorBrowser.route) }
            )

            // ── Terminal ─────────────────────────────────────────────────────
            SecurityFeatureCard(
                title = "Terminal",
                description = "Comandos avanzados del sistema con interfaz hacker",
                icon = Icons.Default.Terminal,
                isActive = false,
                onClick = { navController.navigate("terminal") }
            )

            // ── Coming-soon features ─────────────────────────────────────────
            SecurityFeatureCard(
                title = "Encrypted Backups",
                description = "Secure cloud backups with end-to-end encryption (Coming Soon)",
                icon = Icons.Default.CloudUpload,
                isActive = false,
                isEnabled = false,
                onClick = { /* TODO: Implement encrypted backups */ }
            )

            SecurityFeatureCard(
                title = "Self-Destructing Messages",
                description = "Set messages to automatically delete after a specified time (Coming Soon)",
                icon = Icons.Default.Timer,
                isActive = false,
                isEnabled = false,
                onClick = { /* TODO: Implement self-destructing messages */ }
            )

            // ── Tor connection logs ──────────────────────────────────────────
            // Shown whenever Tor is not fully disconnected and there is at least one log entry
            if (torState !is TorState.Disconnected && torLogs.isNotEmpty()) {
                TorLogsCard(
                    logs = torLogs,
                    onClear = { viewModel.clearLogs() }
                )
            }

            // ── Info section ─────────────────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            InfoSection()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SecurityFeatureCard
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Card for each security feature
 */
@Composable
private fun SecurityFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    isActive: Boolean,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEnabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) Purple.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) Purple else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) Color.White else Color.Gray
                        )

                        if (isActive) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = Purple,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (isEnabled) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isEnabled) Color.Gray else Color.DarkGray,
                fontSize = 13.sp
            )

            // Optional inline content (e.g. toggle + circuit info)
            if (content != null) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TorCircuitInfoCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TorCircuitInfoCard(circuitInfo: TorCircuitInfo?) {
    if (circuitInfo == null) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        colors = CardDefaults.cardColors(containerColor = DarkBackground)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Current Circuit",
                style = MaterialTheme.typography.labelSmall,
                color = Purple,
                fontWeight = FontWeight.Bold
            )

            CircuitNodeRow(
                label = "Entry",
                node = circuitInfo.entryNode,
                icon = Icons.Default.LockOpen
            )
            CircuitNodeRow(
                label = "Middle",
                node = circuitInfo.middleNode,
                icon = Icons.Default.Router
            )
            CircuitNodeRow(
                label = "Exit",
                node = circuitInfo.exitNode,
                icon = Icons.Default.Public
            )

            if (circuitInfo.bandwidth > 0) {
                Text(
                    text = "Bandwidth: ${formatBandwidth(circuitInfo.bandwidth)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun CircuitNodeRow(label: String, node: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontSize = 11.sp,
            modifier = Modifier.width(40.dp)
        )
        Text(
            text = node,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private fun formatBandwidth(bytes: Long): String = when {
    bytes > 1_000_000 -> "${bytes / 1_000_000} MB/s"
    bytes > 1_000     -> "${bytes / 1_000} KB/s"
    else              -> "$bytes B/s"
}

// ─────────────────────────────────────────────────────────────────────────────
// TorLogsCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TorLogsCard(logs: List<String>, onClear: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connection Logs",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = Color.Gray, fontSize = 12.sp)
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess
                                          else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = Color.Gray
                        )
                    }
                }
            }

            // Scrollable log list
            if (expanded) {
                val listState = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// InfoSection
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Information section about security features
 */
@Composable
private fun InfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = Purple,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = "About Security Features",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Nexus Chat provides advanced security features designed for users who require maximum privacy and anonymity. These features are intended for legitimate security research, privacy protection, and educational purposes only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "⚠️ Use responsibly and in accordance with local laws and regulations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
