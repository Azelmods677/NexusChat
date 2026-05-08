package com.Azelmods.App.ui.screens.settings

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.*

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
                onClick = { /* TODO: Blocked users screen */ }
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
                onClick = { /* TODO: Active sessions screen */ }
            )
            
            SettingsItem(
                title = "Passcode Lock",
                subtitle = "Require passcode to open app",
                icon = Icons.Default.Lock,
                onClick = { /* TODO: Passcode settings */ }
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
                onClick = { /* TODO: Download data */ }
            )
            
            SettingsItem(
                title = "Delete My Data",
                subtitle = "Permanently delete all your data",
                icon = Icons.Default.DeleteForever,
                iconTint = Color.Red,
                onClick = { /* TODO: Delete data */ }
            )
        }
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