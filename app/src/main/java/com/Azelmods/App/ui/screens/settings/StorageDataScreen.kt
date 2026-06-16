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
    val context = androidx.compose.ui.platform.LocalContext.current
    val autoDownloadPhotos by viewModel.autoDownloadPhotos.collectAsState()
    val autoDownloadVideos by viewModel.autoDownloadVideos.collectAsState()
    val autoDownloadFiles by viewModel.autoDownloadFiles.collectAsState()
    
    // Real-time storage info
    var totalStorage by remember { mutableStateOf(0L) }
    var usedStorage by remember { mutableStateOf(0L) }
    var availableStorage by remember { mutableStateOf(0L) }
    var cacheSize by remember { mutableStateOf(0L) }
    
    // Update storage info every 2 seconds
    LaunchedEffect(Unit) {
        while (true) {
            val statFs = android.os.StatFs(android.os.Environment.getDataDirectory().path)
            totalStorage = statFs.totalBytes
            availableStorage = statFs.availableBytes
            usedStorage = totalStorage - availableStorage
            
            // Get cache size
            cacheSize = context.cacheDir.walkTopDown()
                .filter { it.isFile }
                .map { it.length() }
                .sum()
            
            kotlinx.coroutines.delay(2000)
        }
    }
    
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> String.format("%.2f MB", bytes / 1_000_000.0)
            bytes >= 1_000 -> String.format("%.2f KB", bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
    
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
            // Storage usage with real-time data
            Text(
                text = "Storage Usage",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            // Storage card with progress
            androidx.compose.material3.Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = androidx.compose.material3.CardDefaults.cardColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Usado: ${formatBytes(usedStorage)}",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Total: ${formatBytes(totalStorage)}",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { if (totalStorage > 0) usedStorage.toFloat() / totalStorage.toFloat() else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color(0xFF2D2D44)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Disponible: ${formatBytes(availableStorage)}",
                        color = Color(0xFF00E676),
                        fontSize = 12.sp
                    )
                }
            }
            
            SettingsItem(
                title = "Clear Cache",
                subtitle = "Liberar ${formatBytes(cacheSize)}",
                icon = Icons.Default.CleaningServices,
                onClick = { 
                    context.cacheDir.deleteRecursively()
                    cacheSize = 0L
                }
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
