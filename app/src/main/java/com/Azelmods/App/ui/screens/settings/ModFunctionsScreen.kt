package com.Azelmods.App.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.components.safeClickable
import com.Azelmods.App.ui.theme.rememberThemeColor

/**
 * ModFunctionsScreen - Expandable sections with toggles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModFunctionsScreen(
    navController: NavController
) {
    val themeColor = rememberThemeColor()
    
    var expandedPrivacy by remember { mutableStateOf(false) }
    var expandedBackup by remember { mutableStateOf(false) }
    var expandedMedia by remember { mutableStateOf(false) }
    var expandedHome by remember { mutableStateOf(false) }
    var expandedChats by remember { mutableStateOf(false) }
    var expandedAdditional by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Funciones Mod") },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ExpandableSection(
                    title = "Privacidades Gerais",
                    icon = Icons.Default.Lock,
                    expanded = expandedPrivacy,
                    onToggle = { expandedPrivacy = !expandedPrivacy },
                    themeColor = themeColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleOption("Ocultar última vez", false, {}, themeColor)
                        ToggleOption("Ocultar en línea", false, {}, themeColor)
                        ToggleOption("Ocultar confirmaciones de lectura", false, {}, themeColor)
                    }
                }
            }
            
            item {
                ExpandableSection(
                    title = "Backup e Restauração",
                    icon = Icons.Default.Backup,
                    expanded = expandedBackup,
                    onToggle = { expandedBackup = !expandedBackup },
                    themeColor = themeColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { /* Backup */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor
                            )
                        ) {
                            Text("Crear Backup")
                        }
                        Button(
                            onClick = { /* Restore */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors()
                        ) {
                            Text("Restaurar Backup")
                        }
                    }
                }
            }
            
            item {
                ExpandableSection(
                    title = "Opções de Mídia",
                    icon = Icons.Default.Image,
                    expanded = expandedMedia,
                    onToggle = { expandedMedia = !expandedMedia },
                    themeColor = themeColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleOption("Auto-descargar imágenes", true, {}, themeColor)
                        ToggleOption("Auto-descargar videos", false, {}, themeColor)
                        ToggleOption("Auto-descargar audios", true, {}, themeColor)
                    }
                }
            }
            
            item {
                ExpandableSection(
                    title = "Tela Inicial",
                    icon = Icons.Default.Home,
                    expanded = expandedHome,
                    onToggle = { expandedHome = !expandedHome },
                    themeColor = themeColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleOption("Mostrar estadísticas", true, {}, themeColor)
                        ToggleOption("Mostrar batería", true, {}, themeColor)
                        ToggleOption("Mostrar reloj", true, {}, themeColor)
                    }
                }
            }
            
            item {
                ExpandableSection(
                    title = "Tela de Conversas",
                    icon = Icons.Default.Chat,
                    expanded = expandedChats,
                    onToggle = { expandedChats = !expandedChats },
                    themeColor = themeColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ToggleOption("Burbujas 3D", true, {}, themeColor)
                        ToggleOption("Animaciones entrada", true, {}, themeColor)
                        ToggleOption("Efectos de glow", true, {}, themeColor)
                    }
                }
            }
            
            item {
                ExpandableSection(
                    title = "Funções Adicionais",
                    icon = Icons.Default.Extension,
                    expanded = expandedAdditional,
                    onToggle = { expandedAdditional = !expandedAdditional },
                    themeColor = themeColor
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { navController.navigate("internal_bot") },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor
                            )
                        ) {
                            Text("Bot Interno")
                        }
                        Button(
                            onClick = { /* Sticker creator */ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = themeColor
                            )
                        ) {
                            Text("Crear Figurinhas")
                        }
                    }
                }
            }
            
            item {
                // Credits button
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .safeClickable { navController.navigate("mod_about") },
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
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Créditos",
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableSection(
    title: String,
    icon: ImageVector,
    expanded: Boolean,
    onToggle: () -> Unit,
    themeColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF111111)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .safeClickable(onClick = onToggle)
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
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .padding(top = 0.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun ToggleOption(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    themeColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = themeColor,
                uncheckedThumbColor = Color.Gray,
                uncheckedTrackColor = Color(0xFF333333)
            )
        )
    }
}
