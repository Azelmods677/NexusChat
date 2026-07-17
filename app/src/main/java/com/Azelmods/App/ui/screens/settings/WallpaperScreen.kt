package com.Azelmods.App.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val backgroundConfig by viewModel.appBackgroundManager.backgroundConfig.collectAsState(
        initial = com.Azelmods.App.data.model.BackgroundConfig()
    )
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setWallpaper("image", it.toString())
        }
    }
    
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setWallpaper("video", it.toString())
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fondo de Chat", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview
            item {
                Text(
                    text = "Previsualización",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    color = DarkSurface
                ) {
                    Box {
                        // Real AppBackground preview
                        com.Azelmods.App.ui.components.AppBackground(
                            backgroundManager = viewModel.appBackgroundManager,
                            modifier = Modifier.fillMaxSize()
                        ) {}
                        
                        // Mock chat bubbles for context
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Received message
                            Surface(
                                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                                color = DarkSurface.copy(alpha = 0.8f),
                                modifier = Modifier.widthIn(max = 250.dp)
                            ) {
                                Text(
                                    text = "¡Hola! ¿Cómo se ve el nuevo fondo?",
                                    modifier = Modifier.padding(12.dp),
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                            
                            // Sent message
                            Surface(
                                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                                modifier = Modifier
                                    .widthIn(max = 250.dp)
                                    .align(Alignment.End)
                            ) {
                                Text(
                                    text = "¡Se ve increíble y muy fluido!",
                                    modifier = Modifier.padding(12.dp),
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // Options
            item {
                Text(
                    text = "Opciones de Fondo",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }
            
            // Default
            item {
                WallpaperOption(
                    title = "Predeterminado",
                    subtitle = "Fondo oscuro estándar",
                    icon = Icons.Default.Block,
                    selected = backgroundConfig.type == com.Azelmods.App.data.model.BackgroundType.NONE,
                    onClick = { 
                        viewModel.setWallpaper("default", "")
                    }
                )
            }
            
            // Gallery — Image
            item {
                WallpaperOption(
                    title = "Elegir de Galería",
                    subtitle = "Selecciona una imagen personalizada",
                    icon = Icons.Default.Image,
                    selected = backgroundConfig.type == com.Azelmods.App.data.model.BackgroundType.IMAGE,
                    onClick = { galleryLauncher.launch("image/*") }
                )
            }
            
            // Gallery — Video
            item {
                WallpaperOption(
                    title = "Fondo de Video",
                    subtitle = "Selecciona un video animado",
                    icon = Icons.Default.Videocam,
                    selected = backgroundConfig.type == com.Azelmods.App.data.model.BackgroundType.VIDEO,
                    onClick = { videoLauncher.launch("video/*") }
                )
            }
            
            // Solid colors
            item {
                Text(
                    text = "Colores Sólidos",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(com.Azelmods.App.data.model.BackgroundPresets.PRESET_COLORS.size) { index ->
                        val colorHex = com.Azelmods.App.data.model.BackgroundPresets.PRESET_COLORS[index]
                        val isSelected = backgroundConfig.type == com.Azelmods.App.data.model.BackgroundType.SOLID_COLOR && 
                                       backgroundConfig.colorHex == colorHex
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(com.Azelmods.App.ui.theme.parseHexColor(colorHex))
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.setWallpaper("color", colorHex)
                                }
                        )
                    }
                }
            }

            // Gradients
            item {
                Text(
                    text = "Degradados Premium",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                
                androidx.compose.foundation.lazy.LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(com.Azelmods.App.data.model.BackgroundPresets.NAMED_GRADIENT_PRESETS.size) { index ->
                        val preset = com.Azelmods.App.data.model.BackgroundPresets.NAMED_GRADIENT_PRESETS[index]
                        val isSelected = backgroundConfig.type == com.Azelmods.App.data.model.BackgroundType.GRADIENT && 
                                       backgroundConfig.gradientColors == preset.colors
                        
                        val brush = com.Azelmods.App.ui.theme.linearGradientBrush(preset.colors)
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                viewModel.setGradientWallpaper(preset.colors)
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .then(if (brush != null) Modifier.background(brush) else Modifier.background(Color.Gray))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            )
                            Text(
                                text = preset.name,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperOption(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else DarkSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else Color.White,
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
                    fontSize = 13.sp
                )
            }
            
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
