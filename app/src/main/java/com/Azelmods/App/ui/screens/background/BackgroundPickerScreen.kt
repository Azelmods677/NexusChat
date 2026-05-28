package com.Azelmods.App.ui.screens.background

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.Azelmods.App.data.model.BackgroundPresets
import com.Azelmods.App.data.model.BackgroundConfig
import com.Azelmods.App.data.model.BackgroundType
import com.Azelmods.App.ui.components.AppBackground
import com.Azelmods.App.ui.components.ColorPickerDialog
import com.Azelmods.App.ui.components.VideoBackgroundPlayer
import com.Azelmods.App.ui.theme.linearGradientBrush
import com.Azelmods.App.ui.theme.parseHexColor
import com.Azelmods.App.ui.theme.rememberThemeColor
import com.Azelmods.App.ui.theme.rememberThemeSecondaryColor

/**
 * Background picker screen - Enhanced Chat Wallpaper
 * 
 * Features:
 * - Top tabs for App/Chat scope
 * - Live preview with chat bubble mockup (40% of screen)
 * - Type selector (None, Color, Image, Video, Gradient)
 * - Beautiful gradient presets organized by category
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
                title = { Text("Chat Wallpaper") },
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
            
            // Preview section (35%) - with chat bubble mockup
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .background(Color.Black)
                    .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
            ) {
                BackgroundPreview(config)
                
                // Chat bubble mockup overlay
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Preview label
                    Text(
                        text = "Vista Previa",
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    // Received message bubble
                    Surface(
                        shape = RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp),
                        color = Color(0xFF1A1A2E).copy(alpha = 0.85f),
                        modifier = Modifier.widthIn(max = 220.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp, 8.dp)) {
                            Text(
                                text = "¡Hola! ¿Cómo estás? 😊",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "10:30 AM",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 10.sp,
                                modifier = Modifier.align(Alignment.End)
                            )
                        }
                    }
                    
                    // Sent message bubble
                    Surface(
                        shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp),
                        color = themeColor.copy(alpha = 0.85f),
                        modifier = Modifier
                            .widthIn(max = 220.dp)
                            .align(Alignment.End)
                    ) {
                        Column(modifier = Modifier.padding(10.dp, 8.dp)) {
                            Text(
                                text = "¡Genial! Me encanta este fondo ✨",
                                color = Color.White,
                                fontSize = 14.sp
                            )
                            Row(
                                modifier = Modifier.align(Alignment.End),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "10:31 AM",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp
                                )
                                Icon(
                                    Icons.Default.DoneAll,
                                    contentDescription = null,
                                    tint = Color(0xFF4FC3F7),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Content section (65%) - scrollable
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f)
                    .background(Color(0xFF0F0F0F))
                    .verticalScroll(rememberScrollState())
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
                            onPickImage = { imagePickerLauncher.launch("image/*") },
                            themeColor = themeColor
                        )
                    }
                    
                    BackgroundType.VIDEO -> {
                        VideoContent(
                            onPickVideo = { videoPickerLauncher.launch("video/*") },
                            themeColor = themeColor
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
                            },
                            onPresetSelected = { colors ->
                                viewModel.setGradient(colors, config.gradientAngle)
                            },
                            themeColor = themeColor
                        )
                    }
                    
                    BackgroundType.BLUR -> {
                        BlurContent(
                            blurRadius = config.blurRadius,
                            onBlurChange = { viewModel.setBlurRadius(it) },
                            themeColor = themeColor
                        )
                    }
                }
                
                // Overlay opacity slider (always visible for non-default types)
                if (config.type != BackgroundType.NONE && config.type != BackgroundType.DEFAULT) {
                    OverlaySlider(
                        alpha = config.overlayAlpha,
                        onAlphaChange = { viewModel.setOverlayAlpha(it) },
                        themeColor = themeColor
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
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
                
                Spacer(modifier = Modifier.height(16.dp))
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
private fun BackgroundPreview(config: BackgroundConfig) {
    val previewBrush = remember(config) {
        linearGradientBrush(
            gradientColors = config.gradientColors,
            gradientAngle = config.gradientAngle
        )
    }
    Box(modifier = Modifier.fillMaxSize()) {
        when (config.type) {
            BackgroundType.SOLID_COLOR -> {
                config.colorHex?.let { hex ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(parseHexColor(hex))
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
                val brush = previewBrush
                if (brush != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(brush)
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
    val themeColor = rememberThemeColor()
    Surface(
        modifier = Modifier
            .weight(1f)
            .height(40.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) themeColor.copy(alpha = 0.2f) else Color(0xFF1A1A1A),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) themeColor else Color.Transparent
        )
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (selected) themeColor else Color.Gray,
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
                        .background(parseHexColor(colorHex))
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
private fun ImageContent(
    onPickImage: () -> Unit,
    themeColor: Color = Color(0xFFCC0000)
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = onPickImage,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1A1A1A)
            )
        ) {
            Icon(Icons.Default.Image, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Seleccionar Imagen de Galería")
        }
        
        // Info text
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = themeColor.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "La imagen se aplicará como fondo en toda la aplicación",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun VideoContent(
    onPickVideo: () -> Unit,
    themeColor: Color = Color(0xFFCC0000)
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
        
        // Info text
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = themeColor.copy(alpha = 0.1f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "El video se reproducirá en loop como fondo animado",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun GradientContent(
    color1: String,
    color2: String,
    angle: Int,
    onColor1Click: () -> Unit,
    onColor2Click: () -> Unit,
    onAngleChange: (Int) -> Unit,
    onPresetSelected: (List<String>) -> Unit,
    themeColor: Color = Color(0xFFCC0000)
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Preset gradients section
        Text(
            text = "Fondos Predefinidos",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
        // Gradient presets grid - scrollable horizontal rows by category
        val presets = BackgroundPresets.NAMED_GRADIENT_PRESETS
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier.height(200.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(presets) { preset ->
                val colors = preset.colors.map { parseHexColor(it) }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onPresetSelected(preset.colors) }
                ) {
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(colors))
                            .border(
                                width = if (preset.colors == listOf(color1, color2) || 
                                           preset.colors.containsAll(listOf(color1, color2))) 2.dp else 0.dp,
                                color = Color.White,
                                shape = RoundedCornerShape(12.dp)
                            )
                    )
                    Text(
                        text = preset.name,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
        
        // Divider
        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
        
        // Custom gradient colors
        Text(
            text = "Personalizar Degradado",
            fontSize = 14.sp,
            color = Color.Gray
        )
        
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
                    thumbColor = themeColor,
                    activeTrackColor = themeColor
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
                    .background(parseHexColor(colorHex))
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
    onBlurChange: (Float) -> Unit,
    themeColor: Color = Color(0xFFCC0000)
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
                thumbColor = themeColor,
                activeTrackColor = themeColor
            )
        )
    }
}

@Composable
private fun OverlaySlider(
    alpha: Float,
    onAlphaChange: (Float) -> Unit,
    themeColor: Color = Color(0xFFCC0000)
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
                thumbColor = themeColor,
                activeTrackColor = themeColor
            )
        )
    }
}


