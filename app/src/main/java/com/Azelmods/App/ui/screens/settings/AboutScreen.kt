package com.Azelmods.App.ui.screens.settings

import android.content.Context
import android.os.Build
import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    // Entrance animation
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    
    // Infinite pulse animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    
    // Glow animation
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    // Floating particles animation
    val particleAnim by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )
    
    // Rotating gradient for header
    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_rotation"
    )
    
    // Typewriter effect for version
    var displayedVersion by remember { mutableStateOf("") }
    val fullVersion = "Versión 1.0.0 • Build 100"
    LaunchedEffect(visible) {
        if (visible) {
            fullVersion.forEachIndexed { i, _ ->
                delay(50)
                displayedVersion = fullVersion.substring(0, i + 1)
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Acerca de", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0D0D1A)
                )
            )
        },
        containerColor = Color(0xFF0D0D1A)
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // ✨ FLOATING PARTICLES BACKGROUND
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                for (i in 0..30) {
                    val x = (i * 137.5f % size.width)
                    val y = (size.height * ((particleAnim + i * 0.03f) % 1f))
                    val alpha = ((particleAnim + i * 0.05f) % 1f)
                    
                    drawCircle(
                        color = Color(0xFF7B5CFA).copy(alpha = alpha * 0.3f),
                        radius = (3 + (i % 3)).dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(24.dp))
                
                // ── ANIMATED HEADER SIMPLE ──────────────────────────────
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(600)) + slideInVertically(tween(600)) { -40 }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF7B5CFA).copy(alpha = 0.3f),
                                        Color(0xFF0D0D1A).copy(alpha = 0.9f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                Color(0xFF7B5CFA).copy(glowAlpha * 0.5f),
                                RoundedCornerShape(20.dp)
                            )
                    )
                }
                
                Spacer(Modifier.height(24.dp))
                
                // ── APP LOGO SIMPLE Y PROFESIONAL ──────────────────────────────────
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(700, delayMillis = 200)) + scaleIn(tween(700, delayMillis = 200))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Logo simple con glow effect
                        Box(
                            modifier = Modifier.size(80.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Glow effect
                            Box(
                                modifier = Modifier
                                    .size(85.dp)
                                    .scale(pulseScale)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFF7B5CFA).copy(alpha = glowAlpha * 0.6f),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )
                            
                            // Logo circle
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .scale(pulseScale)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF7B5CFA), Color(0xFF5A3FC8))
                                        ),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "NC",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Text(
                            "Nexus Chat",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        
                        Text(
                            "Mensajero",
                            fontSize = 14.sp,
                            color = Color(0xFF7B5CFA),
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(Modifier.height(8.dp))
                        
                        // Version badge with typewriter effect
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1A1A2E),
                            border = BorderStroke(1.dp, Color(0xFF7B5CFA).copy(alpha = glowAlpha * 0.5f))
                        ) {
                            Text(
                                displayedVersion,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                fontSize = 12.sp,
                                color = Color(0xFF7B5CFA)
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Text(
                            "Mensajería segura con IA sin censura,\nnavegador Tor y herramientas avanzadas",
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            lineHeight = 20.sp
                        )
                    }
                }
                
                Spacer(Modifier.height(28.dp))
                
                // ── ANIMATED STATS ROW (REAL DEVICE INFO) ────────────────────────────────
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(700, delayMillis = 350)) + slideInVertically(tween(700, delayMillis = 350)) { 40 }
                ) {
                    Column {
                        Text(
                            "ESTADO DEL DISPOSITIVO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7B5CFA),
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Battery status
                            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
                            val batteryLevel = batteryManager?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
                            
                            AnimatedStatCard("$batteryLevel%", "Batería", Color(0xFF00E676))
                            
                            // Storage
                            val statFs = android.os.StatFs(Environment.getDataDirectory().path)
                            val availableGB = (statFs.availableBytes / (1024 * 1024 * 1024))
                            AnimatedStatCard("${availableGB}GB", "Libre", Color(0xFF00D4FF))
                            
                            // RAM
                            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
                            val memInfo = android.app.ActivityManager.MemoryInfo()
                            activityManager?.getMemoryInfo(memInfo)
                            val availableRAM = (memInfo.availMem / (1024 * 1024))
                            AnimatedStatCard("${availableRAM}MB", "RAM", Color(0xFFFC5C7D))
                        }
                    }
                }
                
                Spacer(Modifier.height(28.dp))
            
            // ── INFO GRID (MIT, Version, Developer, Date) ────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700, delayMillis = 400)) + slideInVertically(tween(700, delayMillis = 400)) { 40 }
            ) {
                Column {
                    Text(
                        "INFORMACIÓN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B5CFA),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoCard(Modifier.weight(1f), "MIT", "Licencia")
                        InfoCard(Modifier.weight(1f), "1.0.0", "Versión")
                    }
                    
                    Spacer(Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        InfoCard(Modifier.weight(1f), "Azelmods", "Desarrollador")
                        InfoCard(Modifier.weight(1f), "2026", "Año")
                    }
                }
            }
            
            Spacer(Modifier.height(28.dp))
            
            // ── DEVICE INFORMATION ────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700, delayMillis = 500)) + slideInVertically(tween(700, delayMillis = 500)) { 40 }
            ) {
                Column {
                    Text(
                        "INFORMACIÓN DEL DISPOSITIVO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B5CFA),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1A1A2E),
                        border = BorderStroke(1.dp, Color(0xFF7B5CFA).copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DeviceInfoRow("Modelo", Build.MODEL)
                            DeviceInfoRow("Fabricante", Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
                            DeviceInfoRow("Android", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                            DeviceInfoRow("Dispositivo", Build.DEVICE)
                            DeviceInfoRow("Producto", Build.PRODUCT)
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(28.dp))
            
            // ── SOCIAL LINKS ──────────────────────────────────────
            AnimatedVisibility(
                visible = visible,
                enter = fadeIn(tween(700, delayMillis = 600)) + slideInVertically(tween(700, delayMillis = 600)) { 40 }
            ) {
                Column {
                    Text(
                        "SÍGUEME EN",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF7B5CFA),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    SocialLinkItem(
                        icon = "▶",
                        iconColor = Color.Red,
                        name = "YouTube",
                        handle = "@AzelModsx677",
                        url = "https://youtube.com/@AzelModsx677",
                        uriHandler = uriHandler
                    )
                    
                    SocialLinkItem(
                        icon = "♪",
                        iconColor = Color(0xFF00F2EA),
                        name = "TikTok",
                        handle = "@azelmods677",
                        url = "https://tiktok.com/@azelmods677",
                        uriHandler = uriHandler
                    )
                    
                    SocialLinkItem(
                        icon = "✈",
                        iconColor = Color(0xFF0088CC),
                        name = "Telegram",
                        handle = "t.me/AzelModsx7779",
                        url = "https://t.me/AzelModsx7779",
                        uriHandler = uriHandler
                    )
                }
            }
            
            Spacer(Modifier.height(40.dp))
            
            Text(
                "© Nexus Chat 2026 • Hecho con ❤ por Azelmods",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.3f),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun AnimatedStatCard(value: String, label: String, color: Color) {
    val scale = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        scale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    Surface(
        modifier = Modifier
            .scale(scale.value)
            .width(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A2E),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = color
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun InfoCard(modifier: Modifier, value: String, label: String) {
    val scale = remember { Animatable(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border"
    )
    
    LaunchedEffect(Unit) {
        delay(100)
        scale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy
            )
        )
    }
    
    Surface(
        modifier = modifier.scale(scale.value),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A2E),
        border = BorderStroke(1.dp, Color(0xFF7B5CFA).copy(alpha = borderAlpha))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFF7B5CFA)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun DeviceInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f),
            fontWeight = FontWeight.Medium
        )
        Text(
            value,
            fontSize = 13.sp,
            color = Color(0xFF7B5CFA),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

@Composable
private fun SocialLinkItem(
    icon: String,
    iconColor: Color,
    name: String,
    handle: String,
    url: String,
    uriHandler: androidx.compose.ui.platform.UriHandler
) {
    val scale = remember { Animatable(1f) }
    val coroutineScope = rememberCoroutineScope()
    val infiniteTransition = rememberInfiniteTransition(label = "social_pulse")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "icon_scale"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .scale(scale.value)
            .clickable {
                coroutineScope.launch {
                    scale.animateTo(0.95f, tween(100))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                uriHandler.openUri(url)
            },
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF1A1A2E),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.2f)),
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(40.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.scale(iconScale)
                ) {
                    Text(icon, fontSize = 18.sp, color = iconColor)
                }
            }
            
            Spacer(Modifier.width(14.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 14.sp
                )
                Text(
                    handle,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )
            }
            
            // Animated arrow
            val arrowRotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 15f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "arrow"
            )
            
            Text(
                "↗",
                color = Color(0xFF7B5CFA),
                fontSize = 16.sp,
                modifier = Modifier.rotate(arrowRotation)
            )
        }
    }
}
