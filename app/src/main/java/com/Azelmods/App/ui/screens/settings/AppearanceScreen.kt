package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
            
            // 15 accent colors in 5-column grid
            val accentColors = listOf(
                "Purple" to Color(0xFF7C3AED),
                "Blue" to Color(0xFF3B82F6),
                "Green" to Color(0xFF10B981),
                "Red" to Color(0xFFEF4444),
                "Pink" to Color(0xFFEC4899),
                "Orange" to Color(0xFFF97316),
                "Cyan" to Color(0xFF06B6D4),
                "Toxic" to Color(0xFF00FF00),
                "Dark" to Color(0xFF1F2937),
                "Gold" to Color(0xFFFBBF24),
                "Toxico_Red" to Color(0xFFFF0000),
                "Perverso" to Color(0xFFCC0000),
                "Crimson_Dark" to Color(0xFF8B0000),
                "Neon_Red" to Color(0xFFFF1744),
                "Blood_Moon" to Color(0xFFB71C1C)
            )
            
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(accentColors) { (name, color) ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (accentColor.equals(name, ignoreCase = true)) 3.dp else 1.dp,
                                color = if (accentColor.equals(name, ignoreCase = true)) 
                                    Color.White else Color.White.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                            .clickable { viewModel.setAccentColor(name) }
                    )
                }
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
