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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WallpaperScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val wallpaperType by viewModel.wallpaperType.collectAsState()
    val wallpaperValue by viewModel.wallpaperValue.collectAsState()
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setWallpaper("image", it.toString())
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat Wallpaper", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
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
                    text = "Preview",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    Box {
                        // Wallpaper background
                        when (wallpaperType) {
                            "image" -> {
                                wallpaperValue.takeIf { it.isNotEmpty() }?.let { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop,
                                        alpha = 0.35f
                                    )
                                }
                            }
                            "color" -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(wallpaperValue.toLongOrNull() ?: 0xFF1A1A2E))
                                )
                            }
                            else -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFF0F0F1A))
                                )
                            }
                        }
                        
                        // Mock chat bubbles
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Received message
                            Surface(
                                shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                                color = Color(0xFF1A1A2E),
                                modifier = Modifier.widthIn(max = 250.dp)
                            ) {
                                Text(
                                    text = "Hey! How are you?",
                                    modifier = Modifier.padding(12.dp),
                                    color = Color.White
                                )
                            }
                            
                            // Sent message
                            Surface(
                                shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .widthIn(max = 250.dp)
                                    .align(Alignment.End)
                            ) {
                                Text(
                                    text = "I'm great! Thanks for asking",
                                    modifier = Modifier.padding(12.dp),
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
            
            // Options
            item {
                Text(
                    text = "Wallpaper Options",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
            }
            
            // Default
            item {
                WallpaperOption(
                    title = "Default",
                    subtitle = "Dark background",
                    icon = Icons.Default.Block,
                    selected = wallpaperType == "default" || wallpaperType.isEmpty(),
                    onClick = { 
                        viewModel.setWallpaper("default", "")
                    }
                )
            }
            
            // Gallery
            item {
                WallpaperOption(
                    title = "Choose from Gallery",
                    subtitle = "Select a custom image",
                    icon = Icons.Default.Image,
                    selected = wallpaperType == "image",
                    onClick = { galleryLauncher.launch("image/*") }
                )
            }
            
            // Solid colors
            item {
                Text(
                    text = "Solid Colors",
                    color = Color.Gray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val colors = listOf(
                        0xFF1A1A2E, 0xFF2D2D44, 0xFF1E3A5F, 0xFF2C5F2D,
                        0xFF5F2C2C, 0xFF5F4C2C, 0xFF4C2C5F, 0xFF2C4C5F
                    )
                    
                    colors.forEach { colorValue ->
                        val colorHex = colorValue.toString()
                        val isSelected = wallpaperType == "color" && wallpaperValue == colorHex
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color(colorValue))
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape
                                )
                                .clickable {
                                    viewModel.setWallpaper("color", colorHex)
                                }
                        )
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
        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFF1A1A2E),
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
