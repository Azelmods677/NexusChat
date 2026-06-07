package com.Azelmods.App.ui.screens.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.components.safeClickable
import com.Azelmods.App.ui.theme.rememberThemeColor
import com.Azelmods.App.ui.theme.rememberThemeSecondaryColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

/**
 * ModHomeScreen - Modern home screen with stats and navigation
 */
@Composable
fun ModHomeScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val themeColor = rememberThemeColor()
    val themeSecondaryColor = rememberThemeSecondaryColor()
    val currentUser = FirebaseAuth.getInstance().currentUser
    
    var batteryLevel by remember { mutableStateOf(0) }
    var currentTime by remember { mutableStateOf("") }
    var currentDate by remember { mutableStateOf("") }
    var actualDisplayName by remember { mutableStateOf("") }
    var actualPhotoUrl by remember { mutableStateOf<String?>(null) }
    
    // Load actual user data from Realtime Database (FirebaseAuth.displayName is null for email users)
    LaunchedEffect(currentUser?.uid) {
        val uid = currentUser?.uid ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val snapshot = FirebaseDatabase.getInstance().reference
                    .child("users").child(uid)
                    .get()
                    .await()
                val name = snapshot.child("displayName").getValue(String::class.java)
                val photo = snapshot.child("photoUrl").getValue(String::class.java)
                if (!name.isNullOrBlank()) actualDisplayName = name
                if (!photo.isNullOrBlank()) actualPhotoUrl = photo
            } catch (e: Exception) {
                android.util.Log.e("ModHomeScreen", "Error loading user data", e)
            }
        }
    }
    
    // Battery receiver
    DisposableEffect(Unit) {
        val batteryReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                batteryLevel = (level * 100 / scale)
            }
        }
        
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(batteryReceiver, filter)
        
        onDispose {
            try {
                context.unregisterReceiver(batteryReceiver)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Live clock
    LaunchedEffect(Unit) {
        while (true) {
            val now = Date()
            currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(now)
            currentDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(now)
            delay(1000)
        }
    }
    
    // Animated red glow
    val infiniteTransition = rememberInfiniteTransition(label = "glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Header with avatar and welcome
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                listOf(
                                    themeColor.copy(alpha = glowAlpha),
                                    themeSecondaryColor.copy(alpha = glowAlpha)
                                )
                            ),
                            shape = CircleShape
                        )
                        .padding(4.dp)
                ) {
                    UserAvatar(
                        name = actualDisplayName.ifBlank { currentUser?.displayName ?: currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() } ?: "Anónimo" },
                        photoUrl = actualPhotoUrl ?: currentUser?.photoUrl?.toString(),
                        size = 92.dp
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                Text(
                    text = "Bienvenido/a 👋",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                
                Text(
                    text = actualDisplayName.ifBlank { currentUser?.displayName ?: currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() } ?: "Anónimo" },
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        }
        
        item {
            // Stats grid 2x2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Default.BatteryFull,
                    label = "Batería",
                    value = "$batteryLevel%",
                    modifier = Modifier.weight(1f),
                    themeColor = themeColor
                )
                
                StatCard(
                    icon = Icons.Default.PhoneAndroid,
                    label = "Dispositivo",
                    value = Build.MODEL.take(10),
                    modifier = Modifier.weight(1f),
                    themeColor = themeColor
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    icon = Icons.Default.AccessTime,
                    label = "Hora",
                    value = currentTime,
                    modifier = Modifier.weight(1f),
                    themeColor = themeColor
                )
                
                StatCard(
                    icon = Icons.Default.CalendarToday,
                    label = "Fecha",
                    value = currentDate,
                    modifier = Modifier.weight(1f),
                    themeColor = themeColor
                )
            }
        }
        
        item {
            // Anime image placeholder
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF111111)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = null,
                        tint = themeColor.copy(alpha = 0.3f),
                        modifier = Modifier.size(64.dp)
                    )
                }
            }
        }
        
        item {
            Text(
                text = "Navegación",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
        
        item {
            // Navigation grid 2x2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavigationCard(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = "Chats",
                    onClick = { navController.navigate("home") },
                    modifier = Modifier.weight(1f),
                    themeColor = themeColor,
                    themeSecondaryColor = themeSecondaryColor
                )
                
                NavigationCard(
                    icon = Icons.Default.AutoStories,
                    label = "Stories",
                    onClick = { navController.navigate("home") },
                    modifier = Modifier.weight(1f),
                    themeColor = themeColor,
                    themeSecondaryColor = themeSecondaryColor
                )
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                NavigationCard(
                    icon = Icons.Default.Call,
                    label = "Llamadas",
                    onClick = { navController.navigate("calls") },
                    modifier = Modifier.weight(1f),
                    themeColor = themeColor,
                    themeSecondaryColor = themeSecondaryColor
                )
                
                NavigationCard(
                    icon = Icons.Default.Person,
                    label = "Perfil",
                    onClick = { navController.navigate("edit_profile") },
                    modifier = Modifier.weight(1f),
                    themeColor = themeColor,
                    themeSecondaryColor = themeSecondaryColor
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    themeColor: Color
) {
    Surface(
        modifier = modifier.height(100.dp),
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
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = themeColor,
                modifier = Modifier.size(24.dp)
            )
            
            Column {
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
private fun NavigationCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    themeColor: Color,
    themeSecondaryColor: Color
) {
    Surface(
        modifier = modifier
            .height(100.dp)
            .safeClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            themeColor.copy(alpha = 0.2f),
                            themeSecondaryColor.copy(alpha = 0.2f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = themeColor.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
            }
        }
    }
}
