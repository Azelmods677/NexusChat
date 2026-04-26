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
fun AppearanceScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val darkModeEnabled by viewModel.darkModeEnabled.collectAsState()
    val accentColor by viewModel.accentColor.collectAsState()
    val fontSize by viewModel.fontSize.collectAsState()
    val wallpaperType by viewModel.wallpaperType.collectAsState()
    
    val wallpaperSubtitle = when (wallpaperType) {
        "image" -> "Custom image"
        "color" -> "Solid color"
        else -> "Default"
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Appearance", color = Color.White, fontWeight = FontWeight.Bold) },
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
            Text(
                text = "Theme",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsSwitchItem(
                title = "Dark Mode",
                subtitle = "Use dark theme",
                icon = Icons.Default.DarkMode,
                checked = darkModeEnabled,
                onCheckedChange = { viewModel.setDarkModeEnabled(it) }
            )
            
            HorizontalDivider(color = Color(0xFF1A1A2E), modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Accent Color",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            listOf("Purple", "Blue", "Green", "Red", "Orange").forEach { theme ->
                SettingsRadioItem(
                    title = theme,
                    selected = accentColor == theme,
                    onClick = { viewModel.setAccentColor(theme) }
                )
            }
            
            HorizontalDivider(color = Color(0xFF1A1A2E), modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Display",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "Font Size",
                subtitle = fontSize,
                icon = Icons.Default.FormatSize,
                onClick = { navController.navigate("font_size") }
            )
            
            SettingsItem(
                title = "Chat Wallpaper",
                subtitle = wallpaperSubtitle,
                icon = Icons.Default.Wallpaper,
                onClick = { navController.navigate("wallpaper") }
            )
        }
    }
}

@Composable
fun SettingsRadioItem(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary
            )
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}
