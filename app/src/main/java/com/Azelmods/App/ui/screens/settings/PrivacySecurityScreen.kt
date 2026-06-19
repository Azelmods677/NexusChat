package com.Azelmods.App.ui.screens.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val lastSeenEnabled by viewModel.lastSeenEnabled.collectAsState()
    val profilePhotoVisible by viewModel.profilePhotoVisible.collectAsState()
    val readReceiptsEnabled by viewModel.readReceiptsEnabled.collectAsState()
    val twoFactorEnabled by viewModel.twoFactorEnabled.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showBlockedUsersDialog by remember { mutableStateOf(false) }
    var showActiveSessionsDialog by remember { mutableStateOf(false) }
    var showDownloadDataDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var deleteDataConfirm by remember { mutableStateOf("") }
    var blockedUsers by remember { mutableStateOf(listOf<String>()) }
    var isLoadingData by remember { mutableStateOf(false) }
    var dataExportResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacy & Security", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Privacy section
            Text(
                text = "Privacy",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            SettingsSwitchItem(
                title = "Last Seen",
                subtitle = "Show when you were last online",
                icon = Icons.Default.Visibility,
                checked = lastSeenEnabled,
                onCheckedChange = { viewModel.setLastSeenEnabled(it) }
            )

            SettingsSwitchItem(
                title = "Profile Photo",
                subtitle = "Who can see your profile photo",
                icon = Icons.Default.Photo,
                checked = profilePhotoVisible,
                onCheckedChange = { viewModel.setProfilePhotoVisible(it) }
            )

            SettingsSwitchItem(
                title = "Read Receipts",
                subtitle = "Show when you've read messages",
                icon = Icons.Default.DoneAll,
                checked = readReceiptsEnabled,
                onCheckedChange = { viewModel.setReadReceiptsEnabled(it) }
            )

            SettingsItem(
                title = "Blocked Users",
                subtitle = "Manage blocked contacts",
                icon = Icons.Default.Block,
                onClick = { showBlockedUsersDialog = true }
            )

            HorizontalDivider(color = Color(0xFF1A1A2E), modifier = Modifier.padding(vertical = 8.dp))

            // Security section
            Text(
                text = "Security",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            SettingsSwitchItem(
                title = "Two-Factor Authentication",
                subtitle = "Add an extra layer of security",
                icon = Icons.Default.Security,
                checked = twoFactorEnabled,
                onCheckedChange = { viewModel.setTwoFactorEnabled(it) }
            )

            SettingsItem(
                title = "Active Sessions",
                subtitle = "Manage your active sessions",
                icon = Icons.Default.Devices,
                onClick = { showActiveSessionsDialog = true }
            )

            SettingsItem(
                title = "Passcode Lock",
                subtitle = "Require passcode to open app",
                icon = Icons.Default.Lock,
                onClick = { navController.navigate(Screen.Security.route) }
            )

            HorizontalDivider(color = Color(0xFF1A1A2E), modifier = Modifier.padding(vertical = 8.dp))

            // Data section
            Text(
                text = "Data",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            SettingsItem(
                title = "Download My Data",
                subtitle = "Request a copy of your data",
                icon = Icons.Default.Download,
                onClick = { showDownloadDataDialog = true }
            )

            SettingsItem(
                title = "Delete My Data",
                subtitle = "Permanently delete all your data",
                icon = Icons.Default.DeleteForever,
                iconTint = Color.Red,
                onClick = { showDeleteDataDialog = true }
            )
        }
    }

    // Blocked Users Dialog
    if (showBlockedUsersDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedUsersDialog = false },
            title = { Text("Blocked Users", color = Color.White) },
            text = {
                Column {
                    if (blockedUsers.isEmpty()) {
                        Text("No blocked users", color = Color.Gray)
                    } else {
                        blockedUsers.forEach { user ->
                            Text(user, color = Color.White)
                            HorizontalDivider(color = Color(0xFF1A1A2E))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Use long-press on a chat to block a user.", color = Color.Gray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedUsersDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1A1A2E)
        )
    }

    // Active Sessions Dialog
    if (showActiveSessionsDialog) {
        AlertDialog(
            onDismissRequest = { showActiveSessionsDialog = false },
            title = { Text("Active Sessions", color = Color.White) },
            text = {
                Column {
                    Text("This device (current)", color = Color.White)
                    Text("Android - NexusChat", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Session management is managed by Firebase Auth.", color = Color.Gray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showActiveSessionsDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1A1A2E)
        )
    }

    // Download Data Dialog
    if (showDownloadDataDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDataDialog = false },
            title = { Text("Download My Data", color = Color.White) },
            text = {
                Column {
                    if (isLoadingData) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (dataExportResult != null) {
                        Text("Data exported!", color = Color(0xFF00FF41))
                        Text(dataExportResult ?: "", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        Text("Export your profile data, chats, and settings as JSON.", color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoadingData = true
                            try {
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                val snapshot = FirebaseDatabase.getInstance().reference
                                    .child("users").child(uid).get().await()
                                val json = JSONObject()
                                snapshot.children.forEach { child ->
                                    json.put(child.key ?: "", child.value?.toString() ?: "")
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "My NexusChat Data")
                                    putExtra(Intent.EXTRA_TEXT, json.toString(2))
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Data"))
                                dataExportResult = "JSON shared successfully"
                            } catch (e: Exception) {
                                dataExportResult = "Error: ${e.message}"
                            }
                            isLoadingData = false
                        }
                    },
                    enabled = !isLoadingData
                ) {
                    Text("Export", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDataDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1A1A2E)
        )
    }

    // Delete Data Dialog
    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = { Text("Delete My Data", color = Color(0xFFEF4444)) },
            text = {
                Column {
                    Text(
                        "This will delete all your messages, chats, and settings from this device. Your account will remain active.",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Type 'DELETE' to confirm:", color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteDataConfirm,
                        onValueChange = { deleteDataConfirm = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteDataConfirm == "DELETE") {
                            scope.launch {
                                try {
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                    // Clear local preferences
                                    viewModel.clearAllData()
                                    // Clear userChats index
                                    FirebaseDatabase.getInstance().reference
                                        .child("userChats").child(uid).removeValue().await()
                                    showDeleteDataDialog = false
                                    deleteDataConfirm = ""
                                } catch (e: Exception) {
                                    // handle error
                                }
                            }
                        }
                    },
                    enabled = deleteDataConfirm == "DELETE"
                ) {
                    Text("Delete", color = Color(0xFFEF4444))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1A1A2E)
        )
    }
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
