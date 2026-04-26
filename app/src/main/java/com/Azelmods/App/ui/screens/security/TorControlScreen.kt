package com.Azelmods.App.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.Purple

/**
 * Main screen for Tor control and configuration
 * 
 * Integrates AnonymousModeToggle and TorCircuitInfo components, provides
 * error handling, and allows bridge configuration for censorship bypass.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorControlScreen(
    navController: NavController,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val torState by viewModel.torState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val circuitInfo by viewModel.circuitInfo.collectAsState()
    
    var showBridgeDialog by remember { mutableStateOf(false) }
    
    // Show snackbar for UI state messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SecurityUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearUiState()
            }
            is SecurityUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "${state.message}\n${state.suggestion}",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearUiState()
            }
            else -> {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tor Control") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            // Anonymous Mode Toggle
            AnonymousModeToggle(
                torState = torState,
                onToggle = { enabled ->
                    if (enabled) {
                        viewModel.enableAnonymousMode()
                    } else {
                        viewModel.disableAnonymousMode()
                    }
                }
            )
            
            // Information card
            InfoCard(
                title = "What is Anonymous Mode?",
                description = "Anonymous Mode routes all your traffic through the Tor network, " +
                        "hiding your IP address and location. This provides military-grade privacy " +
                        "and helps bypass censorship."
            )
            
            // Circuit information (only shown when connected)
            if (torState is TorState.Connected) {
                TorCircuitInfo(
                    circuitInfo = circuitInfo,
                    onNewIdentity = { viewModel.requestNewIdentity() }
                )
            }
            
            // Bridge configuration section
            if (torState !is TorState.Connected) {
                BridgeConfigurationCard(
                    onConfigureBridges = { showBridgeDialog = true }
                )
            }
            
            // Error display
            if (torState is TorState.Error) {
                ErrorCard(
                    error = torState as TorState.Error,
                    onRetry = { viewModel.enableAnonymousMode() }
                )
            }
        }
    }
    
    // Bridge configuration dialog
    if (showBridgeDialog) {
        BridgeConfigurationDialog(
            onDismiss = { showBridgeDialog = false },
            onConfirm = { bridges ->
                viewModel.enableBridges(bridges)
                showBridgeDialog = false
            }
        )
    }
}

/**
 * Information card explaining features
 */
@Composable
private fun InfoCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Purple,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Card for bridge configuration
 */
@Composable
private fun BridgeConfigurationCard(
    onConfigureBridges: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Having trouble connecting?",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "If Tor is blocked in your region, you can use bridges to bypass censorship.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onConfigureBridges,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                )
            ) {
                Text("Configure Bridges")
            }
        }
    }
}

/**
 * Error display card with retry button
 */
@Composable
private fun ErrorCard(
    error: TorState.Error,
    onRetry: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Connection Error",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = error.message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontSize = 13.sp
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Retry")
            }
        }
    }
}

/**
 * Dialog for configuring obfs4 bridges
 */
@Composable
private fun BridgeConfigurationDialog(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit
) {
    var bridgeText by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Configure Bridges")
        },
        text = {
            Column {
                Text(
                    text = "Enter obfs4 bridge addresses (one per line):",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = bridgeText,
                    onValueChange = { bridgeText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = {
                        Text(
                            text = "obfs4 192.0.2.1:1234 ABCD1234...",
                            fontSize = 12.sp
                        )
                    },
                    maxLines = 5
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Get bridges from: https://bridges.torproject.org",
                    style = MaterialTheme.typography.bodySmall,
                    color = Purple,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val bridges = bridgeText.lines()
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                    onConfirm(bridges)
                }
            ) {
                Text("Configure")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
