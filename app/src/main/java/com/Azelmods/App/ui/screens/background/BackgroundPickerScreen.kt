package com.Azelmods.App.ui.screens.background

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.Azelmods.App.data.model.BackgroundPresets
import com.Azelmods.App.data.model.BackgroundType
import com.Azelmods.App.ui.components.AppBackground
import com.Azelmods.App.ui.components.ColorPickerDialog
import com.Azelmods.App.ui.components.VideoBackgroundPlayer
import com.Azelmods.App.ui.theme.rememberThemeColor
import com.Azelmods.App.ui.theme.rememberThemeSecondaryColor

/**
 * Background picker screen
 * 
 * Features:
 * - Top tabs for App/Chat scope
 * - Live preview (40% of screen)
 * - Type selector
 * - Content per type
 * - Overlay opacity slider
 * - Apply/Cancel buttons
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackgroundPickerScreen(
    navController: NavController,
    chatId: String? = null,
    viewModel: BackgroundPickerViewModel = hiltViewModel()
) {
    val config by viewModel.selectedConfig.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val themeColor = rememberThemeColor()
    val themeSecondaryColor = rememberThemeSecondaryColor()
    
    var showColorPicker by remember { mutableStateOf(false) }
    var showGradientPicker1 by remember { mutableStateOf(false) }
    var showGradientPicker2 by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }
    
    val gradientColor1 = remember(config.gradientColors) { 
        config.gradientColors.getOrNull(0) ?: "#CC0000" 
    }
    val gradientColor2 = remember(config.gradientColors) { 
        config.gradientColors.getOrNull(1) ?: "#000000" 
    }
    
    // Image picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.pickImage(it) }
    }
    
    // Video picker
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.pickVideo(it) }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fondo") },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tabs (if chat scope)
            if (chatId != null) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color(0xFF0F0F0F),
                    contentColor = themeColor
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("App") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Este Chat") }
                    )
                }
            }
            
            // Preview section (40%)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.4f)
                    .background(Color.Black)
            ) {
                BackgroundPreview(config)
                
                // Overlay indicator
                Text(
                    text = "Vista Previa",
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp),
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
            
            // Content section (60%)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.6f)
                    .background(Color(0xFF0F0F0F))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Type selector
                TypeSelector(
                    selectedType = config.type,
                    onTypeSelected = { viewModel.setType(it) },
                    showDefault = chatId != null
                )
                
                // Content per type
                when (config.type) {
                    BackgroundType.NONE, BackgroundType.DEFAULT -> {
                        Text(
                            text = if (config.type == BackgroundType.DEFAULT) 
                                "Usar fondo de la aplicación" 
                            else 
                                "Sin fondo personalizado",
                            color = Color.Gray,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    }
                    
                    BackgroundType.SOLID_COLOR -> {
                        ColorContent(
                            selectedColor = config.colorHex ?: "#CC0000",
                            onColorSelected = { viewModel.setSolidColor(it) },
                            onCustomClick = { showColorPicker = true }
                        )
                    }
                    
                    BackgroundType.IMAGE -> {
                        ImageContent(
                            onPickImage = { imagePickerLauncher.launch("image/*") }
                        )
                    }
                    
                    BackgroundType.VIDEO -> {
                        VideoContent(
                            onPickVideo = { videoPickerLauncher.launch("video/*") }
                        )
                    }
                    
                    BackgroundType.GRADIENT -> {
                        GradientContent(
                            color1 = gradientColor1,
                            color2 = gradientColor2,
                            angle = config.gradientAngle,
                            onColor1Click = { showGradientPicker1 = true },
                            onColor2Click = { showGradientPicker2 = true },
                            onAngleChange = { angle ->
                                viewModel.setGradient(listOf(gradientColor1, gradientColor2), angle)
                            }
                        )
                    }
                    
                    BackgroundType.BLUR -> {
                        BlurContent(
                            blurRadius = config.blurRadius,
                            onBlurChange = { viewModel.setBlurRadius(it) }
                        )
                    }
                }
                
                // Overlay opacity slider (always visible)
                if (config.type != BackgroundType.NONE && config.type != BackgroundType.DEFAULT) {
                    OverlaySlider(
                        alpha = config.overlayAlpha,
                        onAlphaChange = { viewModel.setOverlayAlpha(it) }
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { navController.navigateUp() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White
                        )
                    ) {
                        Text("Cancelar")
                    }
                    
                    Button(
                        onClick = {
                            viewModel.applyBackground {
                                navController.navigateUp()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        )
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(themeColor, themeSecondaryColor)
                                    ),
                                    RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White
                                )
                            } else {
                                Text("Aplicar", color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
    
    // Dialogs
    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = config.colorHex ?: "#CC0000",
            onDismiss = { showColorPicker = false },
            onColorSelected = {
                viewModel.setSolidColor(it)
                showColorPicker = false
            }
        )
    }
    
    if (showGradientPicker1) {
        ColorPickerDialog(
            initialColor = gradientColor1,
            onDismiss = { showGradientPicker1 = false },
            onColorSelected = {
                viewModel.setGradient(listOf(it, gradientColor2), config.gradientAngle)
                showGradientPicker1 = false
            }
        )
    }
    
    if (showGradientPicker2) {
        ColorPickerDialog(
            initialColor = gradientColor2,
            onDismiss = { showGradientPicker2 = false },
            onColorSelected = {
                viewModel.setGradient(listOf(gradientColor1, it), config.gradientAngle)
                showGradientPicker2 = false
            }
        )
    }
}

@Composable
private fun BackgroundPreview(config: com.Azelmods.App.data.model.BackgroundConfig) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (config.type) {
            BackgroundType.SOLID_COLOR -> {
                config.colorHex?.let { hex ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(parseColor(hex))
                    )
                }
            }
            BackgroundType.VIDEO -> {
                config.videoUri?.let { uri ->
                    VideoBackgroundPlayer(
                        videoUri = uri,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            BackgroundType.GRADIENT -> {
                if (config.gradientColors.size >= 2) {
                    val colors = config.gradientColors.map { parseColor(it) }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(colors)
                            )
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF0A0A0A))
                )
            }
        }
        
        // Overlay
        if (config.type != BackgroundType.NONE) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = config.overlayAlpha))
            )
        }
    }
}

@Composable
private fun TypeSelector(
    selectedType: BackgroundType,
    onTypeSelected: (BackgroundType) -> Unit,
    showDefault: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Tipo de Fondo",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (showDefault) {
                TypeChip("Por Defecto", selectedType == BackgroundType.DEFAULT) {
                    onTypeSelected(BackgroundType.DEFAULT)
                }
            }
            TypeChip("Ninguno", selectedType == BackgroundType.NONE) {
                onTypeSelected(BackgroundType.NONE)
            }
            TypeChip("Color", selectedType == BackgroundType.SOLID_COLOR) {
                onTypeSelected(BackgroundType.SOLID_COLOR)
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TypeChip("Imagen", selectedType == BackgroundType.IMAGE) {
                onTypeSelected(BackgroundType.IMAGE)
            }
            TypeChip("Video", selectedType == BackgroundType.VIDEO) {
                onTypeSelected(BackgroundType.VIDEO)
            }
            TypeChip("Degradado", selectedType == BackgroundType.GRADIENT) {
                onTypeSelected(BackgroundType.GRADIENT)
            }
        }
    }
}

@Composable
private fun RowScope.TypeChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) Color(0xFFCC0000).copy(alpha = 0.2f) else Color(0xFF1A1A1A),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (selected) Color(0xFFCC0000) else Color.Transparent
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (selected) Color(0xFFCC0000) else Color.Gray,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun ColorContent(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    onCustomClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Colores Predefinidos",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(120.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(BackgroundPresets.PRESET_COLORS.take(11)) { colorHex ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(parseColor(colorHex))
                        .border(
                            width = if (selectedColor.equals(colorHex, ignoreCase = true)) 3.dp else 1.dp,
                            color = if (selectedColor.equals(colorHex, ignoreCase = true)) 
                                Color.White else Color.White.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .clickable { onColorSelected(colorHex) }
                )
            }
            
            // Custom button
            item {
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(Color(0xFF1A1A1A))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                        .clickable(onClick = onCustomClick),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Custom",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ImageContent(onPickImage: () -> Unit) {
    Button(
        onClick = onPickImage,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Icon(Icons.Default.Image, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Seleccionar Imagen")
    }
}

@Composable
private fun VideoContent(onPickVideo: () -> Unit) {
    Button(
        onClick = onPickVideo,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Icon(Icons.Default.VideoLibrary, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Seleccionar Video")
    }
}

@Composable
private fun GradientContent(
    color1: String,
    color2: String,
    angle: Int,
    onColor1Click: () -> Unit,
    onColor2Click: () -> Unit,
    onAngleChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ColorButton("Color 1", color1, onColor1Click, Modifier.weight(1f))
            ColorButton("Color 2", color2, onColor2Click, Modifier.weight(1f))
        }
        
        Column {
            Text(
                text = "Ángulo: $angle°",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Slider(
                value = angle.toFloat(),
                onValueChange = { onAngleChange(it.toInt()) },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(
                    thumbColor = Color(0xFFCC0000),
                    activeTrackColor = Color(0xFFCC0000)
                )
            )
        }
    }
}

@Composable
private fun ColorButton(
    label: String,
    colorHex: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF1A1A1A)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(parseColor(colorHex))
                    .border(1.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
            Text(
                text = label,
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
private fun BlurContent(
    blurRadius: Float,
    onBlurChange: (Float) -> Unit
) {
    Column {
        Text(
            text = "Intensidad: ${blurRadius.toInt()}dp",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Slider(
            value = blurRadius,
            onValueChange = onBlurChange,
            valueRange = 0f..25f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFCC0000),
                activeTrackColor = Color(0xFFCC0000)
            )
        )
    }
}

@Composable
private fun OverlaySlider(
    alpha: Float,
    onAlphaChange: (Float) -> Unit
) {
    Column {
        Text(
            text = "Opacidad de Capa: ${(alpha * 100).toInt()}%",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Slider(
            value = alpha,
            onValueChange = onAlphaChange,
            valueRange = 0f..0.8f,
            colors = SliderDefaults.colors(
                thumbColor = Color(0xFFCC0000),
                activeTrackColor = Color(0xFFCC0000)
            )
        )
    }
}

private fun parseColor(hex: String): Color {
    return try {
        val cleanHex = hex.removePrefix("#")
        val colorInt = cleanHex.toLong(16)
        Color(colorInt or 0xFF000000)
    } catch (e: Exception) {
        Color(0xFF0A0A0A)
    }
}
