package com.Azelmods.App.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Azelmods.App.data.security.payload.*

/**
 * Payload Generator Screen
 * 
 * Allows users to configure and generate Android payloads with advanced obfuscation
 * Requires security disclaimer acceptance before access
 * 
 * Requirements: 18.1, 18.2, 18.3, 18.4, 18.5, 18.6, 18.7, 25.1, 25.4, 25.5
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayloadGeneratorScreen(
    onNavigateBack: () -> Unit,
    viewModel: SecurityViewModel = hiltViewModel(),
    disclaimerPreferences: com.Azelmods.App.data.security.SecurityDisclaimerPreferences = 
        androidx.hilt.navigation.compose.hiltViewModel<SecurityViewModel>().let {
            // Get from DI
            com.Azelmods.App.data.security.SecurityDisclaimerPreferences(
                androidx.compose.ui.platform.LocalContext.current
            )
        }
) {
    var showDisclaimer by remember { 
        mutableStateOf(!disclaimerPreferences.hasAcceptedDisclaimer()) 
    }
    var disclaimerDeclined by remember { mutableStateOf(false) }
    
    // Show disclaimer dialog if not accepted
    if (showDisclaimer && !disclaimerDeclined) {
        SecurityDisclaimerDialog(
            onAccept = {
                disclaimerPreferences.setDisclaimerAccepted(true)
                showDisclaimer = false
            },
            onDecline = {
                disclaimerDeclined = true
                showDisclaimer = false
            }
        )
    }
    
    // Show blocked screen if disclaimer was declined
    if (disclaimerDeclined) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Payload Generator") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(32.dp)
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        "Acceso Denegado",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Debes aceptar la advertencia legal para usar el Generador de Payloads",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Button(
                        onClick = {
                            disclaimerDeclined = false
                            showDisclaimer = true
                        }
                    ) {
                        Text("Ver Advertencia Legal")
                    }
                }
            }
        }
        return
    }
    var lhost by remember { mutableStateOf("") }
    var lport by remember { mutableStateOf("4444") }
    var selectedPayloadType by remember { mutableStateOf(PayloadType.METERPRETER_REVERSE_TCP) }
    var obfuscationLevel by remember { mutableStateOf(ObfuscationLevel.MEDIUM) }
    var enableAntiEmulator by remember { mutableStateOf(false) }
    var enableAntiDebug by remember { mutableStateOf(false) }
    var enablePersistence by remember { mutableStateOf(false) }
    var customPackageName by remember { mutableStateOf("") }
    
    val payloadGenerationState by viewModel.payloadGenerationState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payload Generator") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Configure Payload",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            // Basic Configuration
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Basic Configuration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    // LHOST
                    OutlinedTextField(
                        value = lhost,
                        onValueChange = { lhost = it },
                        label = { Text("LHOST (IP Address)") },
                        placeholder = { Text("192.168.1.100") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // LPORT
                    OutlinedTextField(
                        value = lport,
                        onValueChange = { lport = it },
                        label = { Text("LPORT (Port)") },
                        placeholder = { Text("4444") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    // Payload Type
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedPayloadType.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Payload Type") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            PayloadType.values().forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        selectedPayloadType = type
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    
                    // Custom Package Name
                    OutlinedTextField(
                        value = customPackageName,
                        onValueChange = { customPackageName = it },
                        label = { Text("Custom Package Name (Optional)") },
                        placeholder = { Text("com.example.app") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            }
            
            // Obfuscation Settings
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Obfuscation Level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        "Current: ${obfuscationLevel.name}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Slider(
                        value = obfuscationLevel.ordinal.toFloat(),
                        onValueChange = { 
                            obfuscationLevel = ObfuscationLevel.values()[it.toInt()]
                        },
                        valueRange = 0f..(ObfuscationLevel.values().size - 1).toFloat(),
                        steps = ObfuscationLevel.values().size - 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text(
                        "Higher levels provide better evasion but take longer to generate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Advanced Options
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Advanced Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anti-Emulator Detection")
                            Text(
                                "Detects and exits on emulators",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableAntiEmulator,
                            onCheckedChange = { enableAntiEmulator = it }
                        )
                    }
                    
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Anti-Debug Protection")
                            Text(
                                "Exits if debugger is detected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enableAntiDebug,
                            onCheckedChange = { enableAntiDebug = it }
                        )
                    }
                    
                    HorizontalDivider()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Persistence Mechanisms")
                            Text(
                                "Auto-start on boot and restart",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = enablePersistence,
                            onCheckedChange = { enablePersistence = it }
                        )
                    }
                }
            }
            
            // Generation State
            when (val state = payloadGenerationState) {
                is PayloadGenerationState.Idle -> {
                    // Show generate button
                }
                is PayloadGenerationState.Validating -> {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(state.message)
                }
                is PayloadGenerationState.Generating -> {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${state.progress}% - ${state.message}")
                }
                is PayloadGenerationState.Obfuscating -> {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${state.progress}% - ${state.message}")
                }
                is PayloadGenerationState.Signing -> {
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("${state.progress}% - ${state.message}")
                }
                is PayloadGenerationState.Success -> {
                    // Show success card with actions
                    var showTutorial by remember { mutableStateOf(false) }
                    
                    PayloadSuccessCard(
                        payload = state.payload,
                        onShare = {
                            viewModel.sharePayload(state.payload.id)
                        },
                        onShareInChat = {
                            // TODO: Navigate to chat selection screen
                            // For now, just upload to Firebase
                            viewModel.uploadPayloadToFirebase(state.payload.id)
                        },
                        onShowTutorial = {
                            viewModel.generateTutorial(state.payload)
                            showTutorial = true
                        },
                        onDelete = {
                            viewModel.deletePayload(state.payload.id)
                            viewModel.resetPayloadGenerationState()
                        }
                    )
                    
                    // Tutorial Dialog
                    val tutorialState by viewModel.tutorialState.collectAsState()
                    if (showTutorial && tutorialState is TutorialState.Success) {
                        TutorialDialog(
                            tutorialText = (tutorialState as TutorialState.Success).tutorial,
                            onDismiss = {
                                showTutorial = false
                                viewModel.clearTutorialState()
                            }
                        )
                    }
                }
                is PayloadGenerationState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "✗ Error",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Text(state.message)
                            Text(
                                "Suggestion: ${state.suggestion}",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            
            // Generate Button
            Button(
                onClick = {
                    val config = PayloadConfig(
                        type = selectedPayloadType,
                        lhost = lhost,
                        lport = lport.toIntOrNull() ?: 4444,
                        obfuscationLevel = obfuscationLevel,
                        enableAntiEmulator = enableAntiEmulator,
                        enableAntiDebug = enableAntiDebug,
                        enablePersistence = enablePersistence,
                        customPackageName = customPackageName.ifBlank { null }
                    )
                    viewModel.generatePayload(config)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = payloadGenerationState is PayloadGenerationState.Idle || 
                         payloadGenerationState is PayloadGenerationState.Success ||
                         payloadGenerationState is PayloadGenerationState.Error
            ) {
                Text("Generate Payload")
            }
        }
    }
}
