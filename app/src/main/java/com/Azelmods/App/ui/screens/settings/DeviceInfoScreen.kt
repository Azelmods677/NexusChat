package com.Azelmods.App.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.rememberThemeColor
import com.Azelmods.App.ui.theme.rememberThemeSecondaryColor
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val themeColor = rememberThemeColor()
    val themeSecondary = rememberThemeSecondaryColor()
    
    // Get device info
    val deviceInfo = remember { getDeviceInfo(context) }
    val batteryInfo = remember { getBatteryInfo(context) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Device Info", color = Color.White, fontWeight = FontWeight.Bold) },
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
            Spacer(modifier = Modifier.height(16.dp))
            
            // Battery Card with Animation
            BatteryCard(
                batteryLevel = batteryInfo.level,
                isCharging = batteryInfo.isCharging,
                temperature = batteryInfo.temperature,
                themeColor = themeColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Device Information
            Text(
                text = "DEVICE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DeviceInfoCard(
                icon = Icons.Default.PhoneAndroid,
                title = "Brand",
                value = deviceInfo.brand,
                themeColor = themeColor
            )
            
            DeviceInfoCard(
                icon = Icons.Default.Devices,
                title = "Model",
                value = deviceInfo.model,
                themeColor = themeColor
            )
            
            DeviceInfoCard(
                icon = Icons.Default.Android,
                title = "Android Version",
                value = deviceInfo.androidVersion,
                themeColor = themeColor
            )
            
            DeviceInfoCard(
                icon = Icons.Default.Info,
                title = "SDK Level",
                value = deviceInfo.sdkLevel,
                themeColor = themeColor
            )
            
            DeviceInfoCard(
                icon = Icons.Default.Build,
                title = "Build ID",
                value = deviceInfo.buildId,
                themeColor = themeColor
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Hardware Information
            Text(
                text = "HARDWARE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Gray,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            DeviceInfoCard(
                icon = Icons.Default.Memory,
                title = "Processor",
                value = deviceInfo.processor,
                themeColor = themeColor
            )
            
            DeviceInfoCard(
                icon = Icons.Default.Storage,
                title = "Board",
                value = deviceInfo.board,
                themeColor = themeColor
            )
            
            DeviceInfoCard(
                icon = Icons.Default.Fingerprint,
                title = "Fingerprint",
                value = deviceInfo.fingerprint,
                themeColor = themeColor
            )
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun BatteryCard(
    batteryLevel: Int,
    isCharging: Boolean,
    temperature: Float,
    themeColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1A1A2E),
        shadowElevation = 8.dp
    ) {
        Box {
            // Background gradient based on battery level
            Box(
                modifier = Modifier
                    .fillMaxWidth(batteryLevel / 100f)
                    .height(160.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                themeColor.copy(alpha = 0.3f),
                                themeColor.copy(alpha = 0.1f)
                            )
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Animated battery icon
                val infiniteTransition = rememberInfiniteTransition(label = "battery")
                val scale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = if (isCharging) 1.1f else 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1000),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                
                Icon(
                    if (isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer(scaleX = scale, scaleY = scale)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "$batteryLevel%",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = if (isCharging) "Charging" else "Not Charging",
                    fontSize = 14.sp,
                    color = if (isCharging) themeColor else Color.Gray
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "🌡️ ${temperature}°C",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun DeviceInfoCard(
    icon: ImageVector,
    title: String,
    value: String,
    themeColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = themeColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 15.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

// Data classes
data class DeviceInfo(
    val brand: String,
    val model: String,
    val androidVersion: String,
    val sdkLevel: String,
    val buildId: String,
    val processor: String,
    val board: String,
    val fingerprint: String
)

data class BatteryInfo(
    val level: Int,
    val isCharging: Boolean,
    val temperature: Float
)

// Helper functions
private fun getDeviceInfo(context: Context): DeviceInfo {
    return DeviceInfo(
        brand = Build.BRAND.replaceFirstChar { it.uppercase() },
        model = Build.MODEL,
        androidVersion = "Android ${Build.VERSION.RELEASE}",
        sdkLevel = "API ${Build.VERSION.SDK_INT}",
        buildId = Build.ID,
        processor = Build.HARDWARE,
        board = Build.BOARD,
        fingerprint = Build.FINGERPRINT.take(50) + "..."
    )
}

private fun getBatteryInfo(context: Context): BatteryInfo {
    val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { ifilter ->
        context.registerReceiver(null, ifilter)
    }
    
    val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: 0
    val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: 100
    val batteryPct = (level / scale.toFloat() * 100).toInt()
    
    val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
    val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                     status == BatteryManager.BATTERY_STATUS_FULL
    
    val temperature = batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
    val tempCelsius = temperature / 10f
    
    return BatteryInfo(
        level = batteryPct,
        isCharging = isCharging,
        temperature = tempCelsius
    )
}
