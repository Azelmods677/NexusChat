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
import com.Azelmods.App.ui.navigation.Screen
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
    
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteAccountDialog by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var deleteConfirmText by remember { mutableStateOf("") }
    
    val accountActionState by viewModel.accountActionState.collectAsState()
    
    LaunchedEffect(accountActionState) {
        when (accountActionState) {
            is SettingsViewModel.AccountActionState.Success -> {
                val message = (accountActionState as SettingsViewModel.AccountActionState.Success).message
                if (message.contains("Account deleted", ignoreCase = true)) {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                } else {
                    showChangePasswordDialog = false
                    showDeleteAccountDialog = false
                    newPassword = ""
                    confirmPassword = ""
                    deleteConfirmText = ""
                }
                viewModel.clearAccountActionState()
            }
            is SettingsViewModel.AccountActionState.Error -> {
                viewModel.clearAccountActionState()
            }
            else -> {}
        }
    }
    
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
            // Profile preview - link to full profile
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        try {
                            navController.navigate(Screen.Profile.createRoute(""))
                        } catch (e: Exception) {
                            android.util.Log.e("AccountSettingsScreen", "Navigation error: ${e.message}")
                        }
                    },
                color = Color(0xFF1A1A2E)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.takeIf { it.isNotBlank() }?.take(1)?.uppercase() ?: "U",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName.ifEmpty { "User Name" },
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (username.isNotEmpty()) "@$username" else "Set username",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Edit Profile",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Tap to edit your profile, photo, name, username, and bio",
                color = Color.Gray,
                fontSize = 12.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            HorizontalDivider(color = Color(0xFF1A1A2E))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Account Information",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
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
                onClick = { showChangePasswordDialog = true }
            )
            
            SettingsItem(
                title = "Delete Account",
                subtitle = "Permanently delete your account and all data",
                icon = Icons.Default.DeleteForever,
                iconTint = Color.Red,
                onClick = { showDeleteAccountDialog = true }
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

    // Change Password Dialog
    if (showChangePasswordDialog) {
        AlertDialog(
            onDismissRequest = { showChangePasswordDialog = false },
            title = { Text("Change Password", color = Color.White) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newPassword,
                        onValueChange = { newPassword = it },
                        label = { Text("New Password", color = Color.Gray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        label = { Text("Confirm Password", color = Color.Gray) },
                        singleLine = true,
                        isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    if (accountActionState is SettingsViewModel.AccountActionState.Error) {
                        val err = (accountActionState as SettingsViewModel.AccountActionState.Error).message
                        Text(err, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPassword.length >= 6 && newPassword == confirmPassword) {
                            viewModel.changePassword("", newPassword)
                        }
                    },
                    enabled = newPassword.length >= 6 && newPassword == confirmPassword &&
                        accountActionState != SettingsViewModel.AccountActionState.Loading
                ) {
                    if (accountActionState is SettingsViewModel.AccountActionState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Change", color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePasswordDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF1A1A2E)
        )
    }

    // Delete Account Dialog
    if (showDeleteAccountDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountDialog = false },
            title = { Text("Delete Account", color = Color(0xFFEF4444)) },
            text = {
                Column {
                    Text(
                        "This will permanently delete your account and all data. This action cannot be undone.",
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Type 'DELETE' to confirm:",
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFFEF4444),
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                    if (accountActionState is SettingsViewModel.AccountActionState.Error) {
                        val err = (accountActionState as SettingsViewModel.AccountActionState.Error).message
                        Text(err, color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteConfirmText == "DELETE") {
                            viewModel.deleteAccount()
                        }
                    },
                    enabled = deleteConfirmText == "DELETE" &&
                        accountActionState != SettingsViewModel.AccountActionState.Loading
                ) {
                    if (accountActionState is SettingsViewModel.AccountActionState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                    } else {
                        Text("Delete", color = Color(0xFFEF4444))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountDialog = false }) {
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