package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val displayName by viewModel.displayName.collectAsState()
    val username by viewModel.username.collectAsState()
    val bio by viewModel.bio.collectAsState()
    val phoneNumber by viewModel.phoneNumber.collectAsState()
    val email by viewModel.email.collectAsState()
    
    var showEditDialog by remember { mutableStateOf(false) }
    var editField by remember { mutableStateOf("") }
    var editValue by remember { mutableStateOf("") }
    
    // Safe navigation helper
    fun safeNavigateBack() {
        try {
            navController.popBackStack()
        } catch (e: Exception) {
            android.util.Log.e("AccountSettingsScreen", "Navigation error: ${e.message}")
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Account", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { safeNavigateBack() }) {
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
            // Profile photo section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.takeIf { it.isNotBlank() }?.take(1)?.uppercase() ?: "U",
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    IconButton(
                        onClick = { /* TODO: Change photo */ },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = "Change photo",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(onClick = { /* TODO: Change photo */ }) {
                    Text("Change Profile Photo", color = MaterialTheme.colorScheme.primary)
                }
            }
            
            HorizontalDivider(color = Color(0xFF1A1A2E))
            
            // Account info
            AccountInfoItem(
                label = "Display Name",
                value = displayName.ifEmpty { "Not set" },
                icon = Icons.Default.Person,
                onClick = {
                    editField = "displayName"
                    editValue = displayName
                    showEditDialog = true
                }
            )
            
            AccountInfoItem(
                label = "Username",
                value = if (username.isNotEmpty()) "@$username" else "Not set",
                icon = Icons.Default.AlternateEmail,
                onClick = {
                    editField = "username"
                    editValue = username
                    showEditDialog = true
                }
            )
            
            AccountInfoItem(
                label = "Bio",
                value = bio.ifEmpty { "Not set" },
                icon = Icons.Default.Info,
                onClick = {
                    editField = "bio"
                    editValue = bio
                    showEditDialog = true
                }
            )
            
            HorizontalDivider(color = Color(0xFF1A1A2E))
            
            AccountInfoItem(
                label = "Phone Number",
                value = phoneNumber.ifEmpty { "Not set" },
                icon = Icons.Default.Phone,
                onClick = {
                    editField = "phone"
                    editValue = phoneNumber
                    showEditDialog = true
                }
            )
            
            AccountInfoItem(
                label = "Email",
                value = email.ifEmpty { "Not set" },
                icon = Icons.Default.Email,
                onClick = {
                    editField = "email"
                    editValue = email
                    showEditDialog = true
                }
            )
            
            HorizontalDivider(color = Color(0xFF1A1A2E))
            
            // Danger zone
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Danger Zone",
                color = Color.Red,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            SettingsItem(
                title = "Change Password",
                icon = Icons.Default.Lock,
                iconTint = Color.Yellow,
                onClick = { /* TODO: Change password */ }
            )
            
            SettingsItem(
                title = "Delete Account",
                subtitle = "Permanently delete your account and all data",
                icon = Icons.Default.DeleteForever,
                iconTint = Color.Red,
                onClick = { /* TODO: Delete account */ }
            )
        }
    }
    
    // Edit Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Edit ${editField.replaceFirstChar { it.uppercase() }}",
                    color = Color.White
                )
            },
            text = {
                OutlinedTextField(
                    value = editValue,
                    onValueChange = { editValue = it },
                    label = { Text(editField.replaceFirstChar { it.uppercase() }) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                        unfocusedLabelColor = Color.Gray
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            when (editField) {
                                "displayName" -> viewModel.updateDisplayName(editValue)
                                "username" -> viewModel.updateUsername(editValue)
                                "bio" -> viewModel.updateBio(editValue)
                                "phone" -> viewModel.updatePhoneNumber(editValue)
                                "email" -> viewModel.updateEmail(editValue)
                            }
                            showEditDialog = false
                        } catch (e: Exception) {
                            android.util.Log.e("AccountSettingsScreen", "Update error: ${e.message}")
                            showEditDialog = false
                        }
                    }
                ) {
                    Text("Save", color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1A1A2E)
        )
    }
}

@Composable
fun AccountInfoItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
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
                text = label,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = Color.White,
                fontSize = 16.sp
            )
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}