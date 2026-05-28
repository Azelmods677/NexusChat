package com.Azelmods.App.ui.screens.settings

import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
fun NotificationsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val notificationsEnabled by viewModel.notificationsEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val messagePreview by viewModel.messagePreview.collectAsState()
    val groupNotifications by viewModel.groupNotifications.collectAsState()
    val notificationSoundName by viewModel.notificationSoundName.collectAsState()
    
    val context = androidx.compose.ui.platform.LocalContext.current
    
    val soundPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val uri = result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val name = ringtone.getTitle(context) ?: "Unknown"
                viewModel.setNotificationSound(uri.toString(), name)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", color = Color.White, fontWeight = FontWeight.Bold) },
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
            SettingsSwitchItem(
                title = "Notifications",
                subtitle = "Enable all notifications",
                icon = Icons.Default.Notifications,
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )
            
            HorizontalDivider(color = Color(0xFF1A1A2E))
            
            SettingsSwitchItem(
                title = "Sound",
                subtitle = "Play sound for notifications",
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                checked = soundEnabled,
                onCheckedChange = { viewModel.setSoundEnabled(it) }
            )
            
            SettingsItem(
                title = "Notification Sound",
                subtitle = notificationSoundName,
                icon = Icons.Default.MusicNote,
                onClick = {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Notification Sound")
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    }
                    soundPickerLauncher.launch(intent)
                }
            )
            
            SettingsSwitchItem(
                title = "Vibration",
                subtitle = "Vibrate on new messages",
                icon = Icons.Default.Vibration,
                checked = vibrationEnabled,
                onCheckedChange = { viewModel.setVibrationEnabled(it) }
            )
            
            HorizontalDivider(color = Color(0xFF1A1A2E), modifier = Modifier.padding(vertical = 8.dp))
            
            SettingsSwitchItem(
                title = "Message Preview",
                subtitle = "Show message content in notifications",
                icon = Icons.Default.Preview,
                checked = messagePreview,
                onCheckedChange = { viewModel.setMessagePreview(it) }
            )
            
            SettingsSwitchItem(
                title = "Group Notifications",
                subtitle = "Receive notifications from groups",
                icon = Icons.Default.Group,
                checked = groupNotifications,
                onCheckedChange = { viewModel.setGroupNotifications(it) }
            )
        }
    }
}
