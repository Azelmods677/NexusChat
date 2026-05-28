package com.Azelmods.App.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    
    // Get real battery info
    val batteryStatus = remember {
        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    }
    val batteryLevel = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
    val batteryScale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
    val batteryPct = (batteryLevel * 100 / batteryScale.toFloat()).toInt()
    val isCharging = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING
    
    // Get storage info
    val statFs = android.os.StatFs(Environment.getDataDirectory().path)
    val totalGB = (statFs.totalBytes / (1024 * 1024 * 1024))
    val availableGB = (statFs.availableBytes / (1024 * 1024 * 1024))
    val usedGB = totalGB - availableGB
    
    // Get RAM info
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? android.app.ActivityManager
    val memInfo = android.app.ActivityManager.MemoryInfo()
    activityManager?.getMemoryInfo(memInfo)
    val totalRAM = (memInfo.totalMem / (1024 * 1024))
    val availableRAM = (memInfo.availMem / (1024 * 1024))
    val usedRAM = totalRAM - availableRAM
    
    // Animations
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(100)
        visible = true
    }
    
    val infiniteTransition = rememberInfiniteTransition(label = "main")
    
    // Floating particles
    val particleOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particles"
    )
    
    // Logo pulse
    val logoPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )
    
    // Glow effect
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Acerca de", 
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Atrás",
                            tint = Color.White
                        )
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
            // Animated background particles
            Canvas(modifier = Modifier.fillMaxSize()) {
                for (i in 0..40) {
                    val progress = (particleOffset + i * 0.025f) % 1f
                    val x = (i * 137.5f) % size.width
                    val y = size.height * progress
                    val alpha = (1f - progress) * 0.4f
                    
                    drawCircle(
                        color = Color(0xFF7B5CFA).copy(alpha = alpha),
                        radius = (2 + (i % 4)).dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))
                
                // ═══════════════════════════════════════════════════
                // LOGO SECTION WITH GLOW
                // ═══════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800)) + scaleIn(tween(800))
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Outer glow
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .scale(logoPulse)
                                    .background(
                                        Brush.radialGradient(
                                            listOf(
                                                Color(0xFF7B5CFA).copy(alpha = glowAlpha),
                                                Color.Transparent
                                            )
                                        ),
                                        CircleShape
                                    )
                            )
                            
                            // Logo circle
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .scale(logoPulse)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF7B5CFA),
                                                Color(0xFF5A3FC8)
                                            )
                                        ),
                                        CircleShape
                                    )
                                    .border(3.dp, Color(0xFF9D7FFF).copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "NC",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                        }
                        
                        Spacer(Modifier.height(20.dp))
                        
                        Text(
                            "Nexus Chat",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        
                        Text(
                            "Mensajero Seguro y Privado",
                            fontSize = 14.sp,
                            color = Color(0xFF7B5CFA),
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color(0xFF1A1A2E),
                            border = BorderStroke(1.dp, Color(0xFF7B5CFA).copy(alpha = 0.5f))
                        ) {
                            Text(
                                "Versión 1.0.0 • Build 100",
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                                fontSize = 13.sp,
                                color = Color(0xFF7B5CFA),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                // ═══════════════════════════════════════════════════
                // DEVICE STATUS WITH REAL ICONS
                // ═══════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800, delayMillis = 200)) + slideInVertically(tween(800, delayMillis = 200)) { 50 }
                ) {
                    Column {
                        Text(
                            "ESTADO DEL DISPOSITIVO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7B5CFA),
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        // Battery Card
                        DeviceStatusCard(
                            icon = if (isCharging) Icons.Default.BatteryChargingFull else when {
                                batteryPct >= 90 -> Icons.Default.BatteryFull
                                batteryPct >= 60 -> Icons.Default.Battery6Bar
                                batteryPct >= 30 -> Icons.Default.Battery3Bar
                                else -> Icons.Default.Battery1Bar
                            },
                            iconColor = when {
                                batteryPct >= 60 -> Color(0xFF00E676)
                                batteryPct >= 30 -> Color(0xFFFFC107)
                                else -> Color(0xFFFF5252)
                            },
                            title = "Batería",
                            value = "$batteryPct%",
                            subtitle = if (isCharging) "Cargando" else "Disponible",
                            progress = batteryPct / 100f
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // Storage Card
                        DeviceStatusCard(
                            icon = Icons.Default.Storage,
                            iconColor = Color(0xFF00D4FF),
                            title = "Almacenamiento",
                            value = "${availableGB}GB",
                            subtitle = "Libre de ${totalGB}GB",
                            progress = usedGB.toFloat() / totalGB.toFloat()
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        // RAM Card
                        DeviceStatusCard(
                            icon = Icons.Default.Memory,
                            iconColor = Color(0xFFFC5C7D),
                            title = "Memoria RAM",
                            value = "${availableRAM}MB",
                            subtitle = "Libre de ${totalRAM}MB",
                            progress = usedRAM.toFloat() / totalRAM.toFloat()
                        )
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                // ═══════════════════════════════════════════════════
                // APP INFO GRID
                // ═══════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800, delayMillis = 400)) + slideInVertically(tween(800, delayMillis = 400)) { 50 }
                ) {
                    Column {
                        Text(
                            "INFORMACIÓN DE LA APP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7B5CFA),
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Code,
                                value = "MIT",
                                label = "Licencia"
                            )
                            InfoCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Tag,
                                value = "1.0.0",
                                label = "Versión"
                            )
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InfoCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.Person,
                                value = "Azelmods",
                                label = "Desarrollador"
                            )
                            InfoCard(
                                modifier = Modifier.weight(1f),
                                icon = Icons.Default.CalendarToday,
                                value = "2026",
                                label = "Año"
                            )
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                // ═══════════════════════════════════════════════════
                // DEVICE INFO
                // ═══════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800, delayMillis = 600)) + slideInVertically(tween(800, delayMillis = 600)) { 50 }
                ) {
                    Column {
                        Text(
                            "INFORMACIÓN DEL DISPOSITIVO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7B5CFA),
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFF1A1A2E),
                            border = BorderStroke(1.dp, Color(0xFF7B5CFA).copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                DeviceInfoRow(Icons.Default.PhoneAndroid, "Modelo", Build.MODEL)
                                DeviceInfoRow(Icons.Default.Business, "Fabricante", Build.MANUFACTURER.replaceFirstChar { it.uppercase() })
                                DeviceInfoRow(Icons.Default.Android, "Android", "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                                DeviceInfoRow(Icons.Default.Devices, "Dispositivo", Build.DEVICE)
                            }
                        }
                    }
                }
                
                Spacer(Modifier.height(32.dp))
                
                // ═══════════════════════════════════════════════════
                // SOCIAL LINKS
                // ═══════════════════════════════════════════════════
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(800, delayMillis = 800)) + slideInVertically(tween(800, delayMillis = 800)) { 50 }
                ) {
                    Column {
                        Text(
                            "SÍGUEME EN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7B5CFA),
                            letterSpacing = 1.5.sp,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        SocialLinkCard(
                            icon = "▶",
                            iconColor = Color(0xFFFF0000),
                            name = "YouTube",
                            handle = "@AzelModsx677",
                            onClick = { uriHandler.openUri("https://youtube.com/@AzelModsx677") }
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        SocialLinkCard(
                            icon = "♪",
                            iconColor = Color(0xFF00F2EA),
                            name = "TikTok",
                            handle = "@azelmods677",
                            onClick = { uriHandler.openUri("https://tiktok.com/@azelmods677") }
                        )
                        
                        Spacer(Modifier.height(12.dp))
                        
                        SocialLinkCard(
                            icon = "✈",
                            iconColor = Color(0xFF0088CC),
                            name = "Telegram",
                            handle = "t.me/AzelModsx7779",
                            onClick = { uriHandler.openUri("https://t.me/AzelModsx7779") }
                        )
                    }
                }
                
                Spacer(Modifier.height(40.dp))
                
                Text(
                    "© 2026 Nexus Chat • Hecho con ❤️ por Azelmods",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.4f),
                    textAlign = TextAlign.Center
                )
                
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DeviceStatusCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    value: String,
    subtitle: String,
    progress: Float
) {
    val animatedProgress = remember { Animatable(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "card_glow")
    
    // Animated border glow
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_glow"
    )
    
    // Rotating gradient angle
    val gradientAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_angle"
    )
    
    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            progress,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }
    
    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Outer glow effect
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            val angleRad = Math.toRadians(gradientAngle.toDouble())
            val centerX = size.width / 2
            val centerY = size.height / 2
            
            drawRoundRect(
                brush = Brush.sweepGradient(
                    listOf(
                        iconColor.copy(alpha = borderGlow * 0.5f),
                        iconColor.copy(alpha = 0.1f),
                        iconColor.copy(alpha = borderGlow * 0.5f)
                    ),
                    center = androidx.compose.ui.geometry.Offset(centerX, centerY)
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A2E),
            shadowElevation = 8.dp
        ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            // Info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    fontSize = 13.sp,
                    color = Color.White.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
                Text(
                    value,
                    fontSize = 24.sp,
                    color = iconColor,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    subtitle,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                
                Spacer(Modifier.height(8.dp))
                
                // Progress bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress.value)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(iconColor, iconColor.copy(alpha = 0.7f))
                                )
                            )
                    )
                }
            }
        }
    }
}
}

@Composable
private fun InfoCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    val scale = remember { Animatable(0f) }
    val infiniteTransition = rememberInfiniteTransition(label = "info_card")
    
    // Animated 3D border effect
    val borderGlow by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
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
    
    Box(modifier = modifier.scale(scale.value)) {
        // Animated glow border
        Canvas(modifier = Modifier.matchParentSize()) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(
                        Color(0xFF7B5CFA).copy(alpha = borderGlow),
                        Color(0xFF00D4FF).copy(alpha = borderGlow * 0.5f),
                        Color(0xFF7B5CFA).copy(alpha = borderGlow)
                    )
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = Stroke(width = 2.dp.toPx())
            )
        }
        
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1A1A2E),
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF7B5CFA),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    label,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun DeviceInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF7B5CFA),
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            label,
            fontSize = 14.sp,
            color = Color.White.copy(alpha = 0.7f),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            fontSize = 14.sp,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun SocialLinkCard(
    icon: String,
    iconColor: Color,
    name: String,
    handle: String,
    onClick: () -> Unit
) {
    val scale = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale.value)
            .clickable {
                scope.launch {
                    scale.animateTo(0.95f, tween(100))
                    scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                }
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A2E),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = iconColor.copy(alpha = 0.15f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        icon,
                        fontSize = 22.sp,
                        color = iconColor
                    )
                }
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    fontSize = 16.sp
                )
                Text(
                    handle,
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
            
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF7B5CFA),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
