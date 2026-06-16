package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(
    navController: NavController
) {
    var lastSeenVisibility by remember { mutableStateOf("Everyone") }
    var profilePhotoVisibility by remember { mutableStateOf("Everyone") }
    var statusVisibility by remember { mutableStateOf("Everyone") }
    var readReceipts by remember { mutableStateOf(true) }
    var typingIndicator by remember { mutableStateOf(true) }
    var invisibleMode by remember { mutableStateOf(false) }
    var biometricLock by remember { mutableStateOf(false) }
    var twoStepVerification by remember { mutableStateOf(false) }
    var blockScreenshots by remember { mutableStateOf(false) }
    
    var showLastSeenDialog by remember { mutableStateOf(false) }
    var showProfilePhotoDialog by remember { mutableStateOf(false) }
    var showStatusDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Privacy & Security",
                        fontWeight = FontWeight.Bold
                    )
                },
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
        ) {
            // Visibility settings
            PrivacySection(title = "Who can see my...") {
                PrivacySettingItem(
                    title = "Last Seen",
                    value = lastSeenVisibility,
                    onClick = { showLastSeenDialog = true }
                )
                PrivacySettingItem(
                    title = "Profile Photo",
                    value = profilePhotoVisibility,
                    onClick = { showProfilePhotoDialog = true }
                )
                PrivacySettingItem(
                    title = "Status",
                    value = statusVisibility,
                    onClick = { showStatusDialog = true }
                )
            }
            
            // Toggle settings
            PrivacySection(title = "Privacy Controls") {
                PrivacyToggleItem(
                    title = "Read Receipts",
                    description = "Show when you've read messages",
                    checked = readReceipts,
                    onCheckedChange = { readReceipts = it }
                )
                PrivacyToggleItem(
                    title = "Typing Indicator",
                    description = "Show when you're typing",
                    checked = typingIndicator,
                    onCheckedChange = { typingIndicator = it }
                )
                PrivacyToggleItem(
                    title = "Invisible Mode",
                    description = "Hide online status and typing",
                    checked = invisibleMode,
                    onCheckedChange = { invisibleMode = it }
                )
            }
            
            // Security settings
            PrivacySection(title = "Security") {
                PrivacyToggleItem(
                    title = "Biometric Lock",
                    description = "Require fingerprint to open app",
                    checked = biometricLock,
                    onCheckedChange = { biometricLock = it }
                )
                PrivacyToggleItem(
                    title = "Two-Step Verification",
                    description = "Add extra security to your account",
                    checked = twoStepVerification,
                    onCheckedChange = { twoStepVerification = it }
                )
                PrivacyToggleItem(
                    title = "Block Screenshots",
                    description = "Prevent screenshots in chats",
                    checked = blockScreenshots,
                    onCheckedChange = { blockScreenshots = it }
                )
            }
            
            // Active sessions
            PrivacySection(title = "Active Sessions") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    color = DarkSurface,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Current Device",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Android • Active now",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Delete account
            TextButton(
                onClick = { /* Show delete confirmation */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Delete Account",
                    color = Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Visibility dialogs
    if (showLastSeenDialog) {
        VisibilityDialog(
            title = "Who can see my Last Seen",
            currentValue = lastSeenVisibility,
            onDismiss = { showLastSeenDialog = false },
            onSelect = {
                lastSeenVisibility = it
                showLastSeenDialog = false
            }
        )
    }
    
    if (showProfilePhotoDialog) {
        VisibilityDialog(
            title = "Who can see my Profile Photo",
            currentValue = profilePhotoVisibility,
            onDismiss = { showProfilePhotoDialog = false },
            onSelect = {
                profilePhotoVisibility = it
                showProfilePhotoDialog = false
            }
        )
    }
    
    if (showStatusDialog) {
        VisibilityDialog(
            title = "Who can see my Status",
            currentValue = statusVisibility,
            onDismiss = { showStatusDialog = false },
            onSelect = {
                statusVisibility = it
                showStatusDialog = false
            }
        )
    }
}

@Composable
fun PrivacySection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Surface(
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun PrivacySettingItem(
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp
            )
            
            Text(
                text = value,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun PrivacyToggleItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp
            )
            Text(
                text = description,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
fun VisibilityDialog(
    title: String,
    currentValue: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                listOf("Everyone", "Contacts", "Nobody").forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentValue == option,
                            onClick = { onSelect(option) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
