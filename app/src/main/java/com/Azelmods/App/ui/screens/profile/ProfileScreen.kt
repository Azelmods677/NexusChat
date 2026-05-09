package com.Azelmods.App.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userId: String,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var displayName by remember { mutableStateOf("") }
    var bio by remember { mutableStateOf("") }
    var showFullscreenImage by remember { mutableStateOf(false) }
    var fullscreenImageUrl by remember { mutableStateOf("") }
    var fullscreenImageType by remember { mutableStateOf("avatar") } // "avatar" or "cover"
    
    // Request permissions
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Permissions granted, do nothing
    }
    
    LaunchedEffect(Unit) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.CAMERA
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.READ_EXTERNAL_STORAGE,
                    android.Manifest.permission.CAMERA
                )
            )
        }
    }
    
    // Load user profile on launch
    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }
    
    // Photo picker launchers - navigate to crop screen
    val avatarPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val encoded = android.net.Uri.encode(it.toString())
            navController.navigate("image_crop?uri=$encoded&type=profile")
        }
    }
    
    val coverPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri: android.net.Uri? ->
        uri?.let {
            val encoded = android.net.Uri.encode(it.toString())
            navController.navigate("image_crop?uri=$encoded&type=cover")
        }
    }
    
    // Handle crop result from ImageCropScreen
    val cropUri = navController.currentBackStackEntry?.savedStateHandle?.get<String>("crop_uri")
    val cropType = navController.currentBackStackEntry?.savedStateHandle?.get<String>("crop_type")
    val cropScale = navController.currentBackStackEntry?.savedStateHandle?.get<Float>("crop_scale") ?: 1f
    val cropOffsetX = navController.currentBackStackEntry?.savedStateHandle?.get<Float>("crop_offset_x") ?: 0f
    val cropOffsetY = navController.currentBackStackEntry?.savedStateHandle?.get<Float>("crop_offset_y") ?: 0f
    
    LaunchedEffect(cropUri) {
        cropUri?.let { uriString ->
            val uri = android.net.Uri.parse(uriString)
            when (cropType) {
                "profile" -> viewModel.uploadProfilePhoto(uri, cropScale, cropOffsetX, cropOffsetY)
                "cover" -> viewModel.uploadCoverPhoto(uri, cropScale, cropOffsetX, cropOffsetY)
            }
            // Clear saved state
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("crop_uri")
            navController.currentBackStackEntry?.savedStateHandle?.remove<String>("crop_type")
        }
    }
    
    LaunchedEffect(state.user) {
        state.user?.let {
            displayName = it.displayName
            bio = it.bio ?: ""
        }
    }
    
    // Animated gradient ring
    val infiniteTransition = rememberInfiniteTransition(label = "gradient_ring")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (state.isOwnProfile) {
                        IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Purple)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Cover gradient with click to change
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clickable {
                            val currentUser = state.user
                            if (state.isOwnProfile) {
                                // Own profile: open picker
                                coverPicker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            } else if (currentUser?.coverUrl != null) {
                                // Other user: view fullscreen
                                fullscreenImageUrl = currentUser.coverUrl
                                fullscreenImageType = "cover"
                                showFullscreenImage = true
                            }
                        }
                ) {
                    // Show cover photo if available, otherwise gradient
                    val currentUser = state.user
                    if (currentUser?.coverUrl != null) {
                        coil3.compose.AsyncImage(
                            model = currentUser.coverUrl,
                            contentDescription = "Cover Photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        // Beautiful gradient fallback
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            Color(0xFF00C9FF), 
                                            Color(0xFF92FE9D),
                                            Color(0xFFFC5C7D), 
                                            Color(0xFF6A3093)
                                        )
                                    )
                                )
                        )
                    }
                    
                    // Camera icon overlay for own profile
                    if (state.isOwnProfile) {
                        IconButton(
                            onClick = {
                                coverPicker.launch(
                                    androidx.activity.result.PickVisualMediaRequest(
                                        androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                    )
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(0.6f)
                            ) {
                                Icon(
                                    Icons.Default.CameraAlt,
                                    contentDescription = "Change Cover",
                                    tint = Color.White,
                                    modifier = Modifier.padding(6.dp).size(18.dp)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(-60.dp))
                
                // Avatar with animated rotating ring
                Box(
                    modifier = Modifier.size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated rotating rainbow ring
                    Canvas(
                        modifier = Modifier
                            .size(120.dp)
                            .rotate(angle)
                    ) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(
                                    Color(0xFF7B5CFA),
                                    Color(0xFF00D4FF),
                                    Color(0xFFFC5C7D),
                                    Color(0xFF7B5CFA)
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = 3.dp.toPx(),
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                    
                    // Black ring background
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .clip(CircleShape)
                            .background(DarkBackground)
                    )
                    
                    // Avatar container - CLICKABLE for fullscreen (ONLY if has photo)
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(Purple)
                            .clickable(
                                enabled = !state.user?.photoUrl.isNullOrBlank(),
                                onClick = {
                                    if (!state.user?.photoUrl.isNullOrBlank()) {
                                        fullscreenImageUrl = state.user?.photoUrl ?: ""
                                        fullscreenImageType = "avatar"
                                        showFullscreenImage = true
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        val photoUrl = state.user?.photoUrl
                        if (photoUrl != null && photoUrl.isNotBlank()) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = "Profile Photo",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = state.user?.displayName?.take(1)?.uppercase() ?: state.user?.name?.take(1)?.uppercase() ?: "?",
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Camera button for own profile - SEPARATE from avatar click
                    if (state.isOwnProfile) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Purple)
                                .clickable {
                                    avatarPicker.launch(
                                        androidx.activity.result.PickVisualMediaRequest(
                                            androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Upload Photo",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // User info
                Text(
                    text = state.user?.displayName ?: "",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "@${state.user?.username ?: ""}",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                
                if (!state.user?.bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = state.user?.bio ?: "",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Stats row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("Member Since", "2024")
                    StatItem("Messages", "1.2K")
                    StatItem("Files", "234")
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Action buttons
                if (state.isOwnProfile) {
                    Button(
                        onClick = { navController.navigate(Screen.EditProfile.route) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Purple)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar Perfil")
                    }
                } else {
                    // Other user actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = { /* Message */ },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Purple)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Message")
                        }
                        
                        OutlinedButton(
                            onClick = { /* Call */ },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Call")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
    
    // Fullscreen image viewer
    if (showFullscreenImage && fullscreenImageUrl.isNotBlank()) {
        com.Azelmods.App.ui.components.FullScreenImageViewer(
            imageUrl = fullscreenImageUrl,
            senderName = state.user?.displayName ?: state.user?.name ?: "Usuario",
            timestamp = "",
            onDismiss = { 
                showFullscreenImage = false
                fullscreenImageUrl = ""
            }
        )
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}
