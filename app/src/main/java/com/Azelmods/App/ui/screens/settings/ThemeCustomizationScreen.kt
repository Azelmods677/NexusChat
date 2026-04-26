package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.navigation.NavController
import com.Azelmods.App.data.preferences.ChatBackground
import com.Azelmods.App.data.preferences.MessageStyle
import com.Azelmods.App.data.preferences.ThemePreferences
import com.Azelmods.App.data.preferences.ThemePreset

/**
 * Screen for customizing app theme and colors.
 *
 * Features:
 * - Predefined theme presets
 * - Custom color picker
 * - Chat background selection
 * - Message bubble style
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeCustomizationScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences(context) }
    
    var selectedPreset by remember { mutableStateOf(themePrefs.getThemePreset()) }
    var selectedBackground by remember { mutableStateOf(themePrefs.getChatBackground()) }
    var selectedStyle by remember { mutableStateOf(themePrefs.getMessageStyle()) }
    var videoWallpaperUri by remember { mutableStateOf(themePrefs.getVideoWallpaperUri()) }
    
    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            videoWallpaperUri = it.toString()
            themePrefs.setVideoWallpaperUri(it.toString())
            selectedBackground = ChatBackground.VIDEO
            themePrefs.setChatBackground(ChatBackground.VIDEO)
        }
    }
    
    // Tab order state
    val tabNames = listOf("Chats", "Stories", "Calls", "Profile")
    val tabIcons = listOf(
        Icons.Default.Chat,
        Icons.Default.AutoStories,
        Icons.Default.Call,
        Icons.Default.Person
    )
    var tabOrder by remember { mutableStateOf(themePrefs.getTabOrder().toMutableList()) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Personalización") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Tab Order Section - NEW
            item {
                Text(
                    text = "Orden de Pestañas",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mantén presionado y arrastra para reordenar",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            itemsIndexed(tabOrder) { index, tabIndex ->
                TabOrderItem(
                    tabName = tabNames.getOrNull(tabIndex) ?: "",
                    tabIcon = tabIcons.getOrNull(tabIndex) ?: Icons.Default.Chat,
                    position = index + 1,
                    onMoveUp = if (index > 0) {
                        {
                            val newOrder = tabOrder.toMutableList()
                            val temp = newOrder[index]
                            newOrder[index] = newOrder[index - 1]
                            newOrder[index - 1] = temp
                            tabOrder = newOrder
                            themePrefs.setTabOrder(newOrder)
                        }
                    } else null,
                    onMoveDown = if (index < tabOrder.size - 1) {
                        {
                            val newOrder = tabOrder.toMutableList()
                            val temp = newOrder[index]
                            newOrder[index] = newOrder[index + 1]
                            newOrder[index + 1] = temp
                            tabOrder = newOrder
                            themePrefs.setTabOrder(newOrder)
                        }
                    } else null
                )
            }
            
            // Theme Presets Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Temas Predefinidos",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            items(ThemePreset.values().toList()) { preset ->
                ThemePresetCard(
                    preset = preset,
                    isSelected = preset == selectedPreset,
                    onClick = {
                        selectedPreset = preset
                        themePrefs.setThemePreset(preset)
                    }
                )
            }
            
            // Chat Background Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Fondo del Chat",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            items(ChatBackground.values().toList()) { background ->
                BackgroundOption(
                    background = background,
                    isSelected = background == selectedBackground,
                    onClick = {
                        if (background == ChatBackground.VIDEO) {
                            videoPickerLauncher.launch("video/*")
                        } else {
                            selectedBackground = background
                            themePrefs.setChatBackground(background)
                        }
                    },
                    videoUri = if (background == ChatBackground.VIDEO) videoWallpaperUri else null
                )
            }
            
            // Message Style Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Estilo de Mensajes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            items(MessageStyle.values().toList()) { style ->
                MessageStyleOption(
                    style = style,
                    isSelected = style == selectedStyle,
                    onClick = {
                        selectedStyle = style
                        themePrefs.setMessageStyle(style)
                    }
                )
            }
            
            // Info
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF1A1A2E)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = themePrefs.getPrimaryColor()
                        )
                        Text(
                            text = "Los cambios se aplicarán inmediatamente en toda la app",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemePresetCard(
    preset: ThemePreset,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2D2D44) else Color(0xFF1A1A2E)
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, preset.primaryColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Color preview
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(preset.primaryColor, preset.secondaryColor)
                            )
                        )
                )
                
                Text(
                    text = preset.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Seleccionado",
                    tint = preset.primaryColor
                )
            }
        }
    }
}

@Composable
private fun BackgroundOption(
    background: ChatBackground,
    isSelected: Boolean,
    onClick: () -> Unit,
    videoUri: String? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2D2D44) else Color(0xFF1A1A2E)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        when (background) {
                            ChatBackground.DEFAULT -> Icons.Default.Wallpaper
                            ChatBackground.SOLID_DARK -> Icons.Default.FormatColorFill
                            ChatBackground.GRADIENT -> Icons.Default.Gradient
                            ChatBackground.VIDEO -> Icons.Default.VideoLibrary
                            ChatBackground.IMAGE -> Icons.Default.Image
                        },
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF7C3AED) else Color.Gray
                    )
                    
                    Text(
                        text = background.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White
                    )
                }
                
                if (isSelected) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Seleccionado",
                        tint = Color(0xFF7C3AED)
                    )
                }
            }
            
            // Show video URI if VIDEO background is selected
            if (background == ChatBackground.VIDEO && videoUri != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Video seleccionado",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}
                    tint = Color(0xFF7C3AED)
                )
            }
        }
    }
}

@Composable
private fun MessageStyleOption(
    style: MessageStyle,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2D2D44) else Color(0xFF1A1A2E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Style preview
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .then(
                            when (style) {
                                MessageStyle.CARD_3D -> Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF7C3AED))
                                    .border(1.dp, Color.White.copy(0.3f), RoundedCornerShape(12.dp))
                                MessageStyle.FLAT -> Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFF7C3AED))
                                MessageStyle.ROUNDED -> Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF7C3AED))
                                MessageStyle.BUBBLE -> Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF7C3AED))
                            }
                        )
                )
                
                Text(
                    text = style.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White
                )
            }
            
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Seleccionado",
                    tint = Color(0xFF7C3AED)
                )
            }
        }
    }
}


@Composable
private fun TabOrderItem(
    tabName: String,
    tabIcon: androidx.compose.ui.graphics.vector.ImageVector,
    position: Int,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A2E)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Position number
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF7C3AED), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = position.toString(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
                
                // Tab icon
                Icon(
                    tabIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                
                // Tab name
                Text(
                    text = tabName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            // Move buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Move up button
                IconButton(
                    onClick = { onMoveUp?.invoke() },
                    enabled = onMoveUp != null
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowUp,
                        contentDescription = "Mover arriba",
                        tint = if (onMoveUp != null) Color(0xFF7C3AED) else Color.Gray
                    )
                }
                
                // Move down button
                IconButton(
                    onClick = { onMoveDown?.invoke() },
                    enabled = onMoveDown != null
                ) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Mover abajo",
                        tint = if (onMoveDown != null) Color(0xFF7C3AED) else Color.Gray
                    )
                }
            }
        }
    }
}
