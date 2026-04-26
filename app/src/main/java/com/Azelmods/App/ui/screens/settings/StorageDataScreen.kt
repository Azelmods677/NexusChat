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
fun StorageDataScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val autoDownloadPhotos by viewModel.autoDownloadPhotos.collectAsState()
    val autoDownloadVideos by viewModel.autoDownloadVideos.collectAsState()
    val autoDownloadFiles by viewModel.autoDownloadFiles.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Storage & Data", color = Color.White, fontWeight = FontWeight.Bold) },
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
            // Storage usage
            Text(
                text = "Storage Usage",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "Manage Storage",
                subtitle = "2.3 GB used",
                icon = Icons.Default.Storage,
                onClick = { /* TODO: Storage management */ }
            )
            
            SettingsItem(
                title = "Clear Cache",
                subtitle = "Free up 450 MB",
                icon = Icons.Default.CleaningServices,
                onClick = { /* TODO: Clear cache */ }
            )
            
            HorizontalDivider(color = Color(0xFF1A1A2E), modifier = Modifier.padding(vertical = 8.dp))
            
            // Auto-download
            Text(
                text = "Auto-Download Media",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsSwitchItem(
                title = "Photos",
                subtitle = "Auto-download photos",
                icon = Icons.Default.Photo,
                checked = autoDownloadPhotos,
                onCheckedChange = { viewModel.setAutoDownloadPhotos(it) }
            )
            
            SettingsSwitchItem(
                title = "Videos",
                subtitle = "Auto-download videos",
                icon = Icons.Default.VideoLibrary,
                checked = autoDownloadVideos,
                onCheckedChange = { viewModel.setAutoDownloadVideos(it) }
            )
            
            SettingsSwitchItem(
                title = "Files",
                subtitle = "Auto-download documents",
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                checked = autoDownloadFiles,
                onCheckedChange = { viewModel.setAutoDownloadFiles(it) }
            )
            
            HorizontalDivider(color = Color(0xFF1A1A2E), modifier = Modifier.padding(vertical = 8.dp))
            
            // Network usage
            Text(
                text = "Network Usage",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "Data Usage",
                subtitle = "View data consumption",
                icon = Icons.Default.DataUsage,
                onClick = { /* TODO: Data usage stats */ }
            )
            
            SettingsItem(
                title = "Low Data Mode",
                subtitle = "Reduce data consumption",
                icon = Icons.Default.DataSaverOn,
                onClick = { /* TODO: Low data mode */ }
            )
        }
    }
}
