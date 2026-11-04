package com.Azelmods.App.ui.screens.settings

import com.Azelmods.App.i18n.tr

import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
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
            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            }
            if (uri != null) {
                val ringtone = RingtoneManager.getRingtone(context, uri)
                val name = ringtone.getTitle(context) ?: tr("Desconocido")
                viewModel.setNotificationSound(uri.toString(), name)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Notificaciones"), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Atrás"), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
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
            SettingsSwitchItem(
                title = tr("Notificaciones"),
                subtitle = tr("Activar todas las notificaciones"),
                icon = Icons.Default.Notifications,
                checked = notificationsEnabled,
                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
            )
            
            HorizontalDivider(color = DarkSurface)
            
            SettingsSwitchItem(
                title = tr("Sonido"),
                subtitle = tr("Reproducir un sonido al recibir avisos"),
                icon = Icons.AutoMirrored.Filled.VolumeUp,
                checked = soundEnabled,
                onCheckedChange = { viewModel.setSoundEnabled(it) }
            )
            
            SettingsItem(
                title = tr("Sonido de notificación"),
                subtitle = notificationSoundName,
                icon = Icons.Default.MusicNote,
                onClick = {
                    val intent = android.content.Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, tr("Sonido de notificación"))
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
                    }
                    soundPickerLauncher.launch(intent)
                }
            )
            
            SettingsSwitchItem(
                title = tr("Vibración"),
                subtitle = tr("Vibrar con mensajes nuevos"),
                icon = Icons.Default.Vibration,
                checked = vibrationEnabled,
                onCheckedChange = { viewModel.setVibrationEnabled(it) }
            )
            
            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))
            
            SettingsSwitchItem(
                title = tr("Vista previa del mensaje"),
                subtitle = tr("Mostrar el contenido en la notificación"),
                icon = Icons.Default.Preview,
                checked = messagePreview,
                onCheckedChange = { viewModel.setMessagePreview(it) }
            )
            
            SettingsSwitchItem(
                title = tr("Notificaciones de grupos"),
                subtitle = tr("Recibir avisos de los grupos"),
                icon = Icons.Default.Group,
                checked = groupNotifications,
                onCheckedChange = { viewModel.setGroupNotifications(it) }
            )
        }
    }
}
