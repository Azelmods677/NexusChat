package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.components.safeClickable
import com.Azelmods.App.ui.theme.rememberThemeColor
import com.Azelmods.App.ui.theme.rememberThemeSecondaryColor
import com.google.firebase.auth.FirebaseAuth

/**
 * ModSettingsScreen - Modern settings screen with grid layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModSettingsScreen(
    navController: NavController
) {
    val themeColor = rememberThemeColor()
    val themeSecondaryColor = rememberThemeSecondaryColor()
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F0F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header with avatar
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF111111)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .border(
                                    width = 2.dp,
                                    color = themeColor,
                                    shape = CircleShape
                                )
                                .padding(2.dp)
                        ) {
                            UserAvatar(
                                name = currentUser?.displayName ?: "User",
                                photoUrl = currentUser?.photoUrl?.toString(),
                                size = 56.dp
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentUser?.displayName ?: "Usuario",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Modders Private",
                                fontSize = 14.sp,
                                color = themeColor
                            )
                        }
                        
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
            
            item {
                Text(
                    text = "Configuración Principal",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            item {
                // Grid 2x4
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsGridItem(
                            icon = Icons.Default.AccountCircle,
                            label = "Cuenta",
                            onClick = { navController.navigate("settings_account") },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                        SettingsGridItem(
                            icon = Icons.Default.Chat,
                            label = "Conversaciones",
                            onClick = { navController.navigate("mod_functions") },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsGridItem(
                            icon = Icons.Default.Lock,
                            label = "Privacidades",
                            onClick = { navController.navigate("settings_privacy") },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                        SettingsGridItem(
                            icon = Icons.Default.Person,
                            label = "Avatar",
                            onClick = { navController.navigate("edit_profile") },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsGridItem(
                            icon = Icons.Default.Notifications,
                            label = "Notificaciones",
                            onClick = { navController.navigate("settings_notifications") },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                        SettingsGridItem(
                            icon = Icons.Default.Storage,
                            label = "Almacenamiento",
                            onClick = { navController.navigate("settings_storage") },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SettingsGridItem(
                            icon = Icons.Default.Help,
                            label = "Ayuda",
                            onClick = { navController.navigate("settings_help") },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                        SettingsGridItem(
                            icon = Icons.Default.Share,
                            label = "Invitar",
                            onClick = { /* Share app */ },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                    }
                }
            }
            
            item {
                Text(
                    text = "Opciones Adicionales",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            item {
                SettingsListItem(
                    icon = Icons.Default.Info,
                    title = "About",
                    onClick = { navController.navigate("mod_about") },
                    themeColor = themeColor
                )
            }
            
            item {
                SettingsListItem(
                    icon = Icons.Default.Star,
                    title = "Mensajes Favoritos",
                    onClick = { /* Navigate to favorites */ },
                    themeColor = themeColor
                )
            }
            
            item {
                SettingsListItem(
                    icon = Icons.Default.Share,
                    title = "Compartir",
                    onClick = { /* Share */ },
                    themeColor = themeColor
                )
            }
            
            item {
                SettingsListItem(
                    icon = Icons.Default.Wallpaper,
                    title = "Fondo de Aplicación",
                    onClick = { navController.navigate("background_picker") },
                    themeColor = themeColor
                )
            }
            
            item {
                // Footer
                Text(
                    text = "© NexusChat 2026",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun SettingsGridItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    themeColor: Color
) {
    Surface(
        modifier = modifier
            .height(100.dp)
            .safeClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF111111),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = themeColor.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = themeColor,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun SettingsListItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    themeColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF111111)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = themeColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
