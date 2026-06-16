package com.Azelmods.App.ui.screens.settings

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.data.preferences.TutorialPreferences
import com.Azelmods.App.data.tutorials.AppFeature
import com.Azelmods.App.ui.components.AutoTutorial
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val tutorialPreferences = remember { TutorialPreferences(context) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Load actual user data from Realtime Database
    var actualUserName by remember { mutableStateOf("") }
    var actualUserPhoto by remember { mutableStateOf<String?>(null) }
    
    LaunchedEffect(Unit) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val snapshot = com.google.firebase.database.FirebaseDatabase.getInstance().reference
                    .child("users").child(uid)
                    .get()
                    .await()
                val name = snapshot.child("displayName").getValue(String::class.java)
                val photo = snapshot.child("photoUrl").getValue(String::class.java)
                if (!name.isNullOrBlank()) actualUserName = name
                if (!photo.isNullOrBlank()) actualUserPhoto = photo
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "Error loading user data", e)
            }
        }
    }
    
    // Show tutorial on first visit
    AutoTutorial(
        feature = AppFeature.SETTINGS,
        tutorialPreferences = tutorialPreferences
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // PREMIUM PROFILE HEADER
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF1A1A2E),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .clickable {
                            try {
                                navController.navigate(Screen.Profile.createRoute(""))
                            } catch (e: Exception) {
                                android.util.Log.e("SettingsScreen", "Navigation error: ${e.message}")
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar with animated gradient ring
                    Box(contentAlignment = Alignment.Center) {
                        // Animated gradient ring
                        val rotation by rememberInfiniteTransition(label = "ring").animateFloat(
                            initialValue = 0f,
                            targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(3000, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "ring_rotation"
                        )
                        
                        Canvas(
                            modifier = Modifier
                                .size(64.dp)
                                .rotate(rotation)
                        ) {
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(
                                        Color(0xFF7B5CFA), 
                                        Color(0xFF00D4FF),
                                        Color(0xFF7B5CFA)
                                    )
                                ),
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                            )
                        }
                        
                        // Avatar image/initials
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                // Load user photo if available
                                if (actualUserPhoto != null) {
                                    coil3.compose.AsyncImage(
                                        model = actualUserPhoto,
                                        contentDescription = "Profile",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else {
                                    Text(
                                        text = actualUserName.take(1).uppercase().ifEmpty { "?" },
                                        color = Color.White,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = actualUserName.ifBlank { "Usuario" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = if (actualUserName.isNotBlank()) actualUserName else "Usando Nexus Chat",
                            fontSize = 13.sp,
                            color = Color.White.copy(0.5f),
                            maxLines = 1
                        )
                        
                        // Online status
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(Color(0xFF00E676), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "En línea",
                                fontSize = 11.sp,
                                color = Color(0xFF00E676)
                            )
                        }
                    }
                    
                    // Settings gear (animated)
                    IconButton(onClick = { /* already on settings */ }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Premium banner
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable {
                        try {
                            navController.navigate(Screen.Premium.route)
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsScreen", "Navigation error: ${e.message}")
                        }
                    },
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(32.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Upgrade to Premium",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Unlock all features",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                    
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Settings sections
            SettingsSection(title = "Account") {
                SettingsItem(
                    icon = Icons.Default.Person,
                    title = "Account",
                    onClick = { navController.navigate(Screen.SettingsAccount.route) }
                )
            }
            
            SettingsSection(title = "Privacy & Security") {
                SettingsItem(
                    icon = Icons.Default.Lock,
                    title = "Privacy & Security",
                    onClick = { navController.navigate(Screen.SettingsPrivacy.route) }
                )
                SettingsItem(
                    icon = Icons.Default.Security,
                    title = "Security Tools",
                    badge = "ADVANCED",
                    onClick = { navController.navigate(Screen.Security.route) }
                )
            }
            
            SettingsSection(title = "Features") {
                SettingsItem(
                    icon = Icons.Default.AutoAwesome,
                    title = "AI Features",
                    badge = "NEW",
                    onClick = { navController.navigate(Screen.AiFeatures.route) }
                )
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    onClick = { navController.navigate(Screen.SettingsNotifications.route) }
                )
            }
            
            SettingsSection(title = "Appearance") {
                SettingsItem(
                    icon = Icons.Default.Palette,
                    title = "Appearance",
                    onClick = { navController.navigate(Screen.SettingsAppearance.route) }
                )
            }
            
            SettingsSection(title = "Storage") {
                SettingsItem(
                    icon = Icons.Default.Storage,
                    title = "Storage & Data",
                    onClick = { navController.navigate(Screen.SettingsStorage.route) }
                )
            }
            
            SettingsSection(title = "Support") {
                SettingsItem(
                    icon = Icons.AutoMirrored.Filled.Help,
                    title = "Help & Support",
                    onClick = { navController.navigate(Screen.SettingsHelp.route) }
                )
                SettingsItem(
                    icon = Icons.Default.Info,
                    title = "About Nexus Chat",
                    onClick = { navController.navigate(Screen.SettingsAbout.route) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Logout button
            TextButton(
                onClick = { showLogoutDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Logout",
                    color = Color.Red,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Logout confirmation dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        try {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("SettingsScreen", "Logout navigation error: ${e.message}")
                        }
                    }
                ) {
                    Text("Logout", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        Surface(
            color = DarkSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                content()
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    badge: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                try {
                    onClick()
                } catch (e: Exception) {
                    android.util.Log.e("SettingsScreen", "Click error: ${e.message}")
                }
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Text(
            text = title,
            color = Color.White,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        
        if (badge != null) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = badge,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
        }
        
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color.Gray
        )
    }
}
