package com.Azelmods.App.ui.screens.profile
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.rememberAsyncImagePainter
import com.Azelmods.App.data.model.CallType
import com.Azelmods.App.data.model.User
import com.Azelmods.App.ui.components.FullScreenImageViewer
import com.Azelmods.App.ui.components.PhotoAdjuster
import com.Azelmods.App.ui.components.safeClickable
import com.Azelmods.App.ui.screens.call.CallViewModel
import com.Azelmods.App.utils.PermissionHelper
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreenRedesigned(
    userId: String?,
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel(),
    callViewModel: CallViewModel = hiltViewModel()
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val isOwnProfile = userId == null || userId == currentUserId
    val profileUserId = userId ?: currentUserId
    
    val userState by viewModel.userProfile.collectAsState()
    
    LaunchedEffect(profileUserId) {
        if (profileUserId.isNotEmpty()) {
            viewModel.loadUserProfile(profileUserId)
        }
    }
    
    val user = userState
    var showEditSheet by remember { mutableStateOf(false) }
    var showPhotoAdjuster by remember { mutableStateOf(false) }
    var showCoverAdjuster by remember { mutableStateOf(false) }
    var showFullscreenImage by remember { mutableStateOf(false) }
    var selectedPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var selectedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var photoVerticalPosition by remember { mutableStateOf(0f) }
    var coverVerticalPosition by remember { mutableStateOf(0f) }
    
    val context = LocalContext.current
    
    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            showPhotoAdjuster = true
        }
    }
    
    // Cover picker launcher
    val coverPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedCoverUri = it
            showCoverAdjuster = true
        }
    }
    
    // Media permission launcher
    val mediaPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permissions granted
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                actions = {
                    if (isOwnProfile) {
                        IconButton(onClick = { 
                            try {
                                navController.navigate("settings")
                            } catch (e: Exception) { }
                        }) {
                            Icon(
                                Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1A1A2E)
                )
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header with gradient
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                // Cover photo or gradient
                if (selectedCoverUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(selectedCoverUri),
                        contentDescription = "Cover photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                translationY = coverVerticalPosition
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF7C3AED), Color(0xFF00BFA6)),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, 200f)
                                )
                            )
                    )
                }
                
                // Edit cover button for own profile
                if (isOwnProfile) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .safeClickable {
                                if (PermissionHelper.hasMediaPermissions(context)) {
                                    coverPickerLauncher.launch("image/*")
                                } else {
                                    mediaPermissionLauncher.launch(PermissionHelper.mediaPermissions)
                                }
                            },
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.6f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.CameraAlt,
                                contentDescription = "Change cover",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Avatar overlapping header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-45).dp),
                contentAlignment = Alignment.Center
            ) {
                AnimatedAvatar(
                    name = user?.name ?: "?",
                    photoUri = selectedPhotoUri,
                    photoPosition = photoVerticalPosition,
                    isOwnProfile = isOwnProfile,
                    onCameraClick = {
                        if (PermissionHelper.hasMediaPermissions(context)) {
                            photoPickerLauncher.launch("image/*")
                        } else {
                            mediaPermissionLauncher.launch(PermissionHelper.mediaPermissions)
                        }
                    },
                    onPhotoClick = {
                        showFullscreenImage = true
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // User info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = user?.name ?: "Unknown",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                
                Text(
                    text = user?.username ?: "@unknown",
                    color = Color(0xFF00BFA6),
                    fontSize = 15.sp
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = user?.bio?.takeIf { it.isNotBlank() } ?: "Tap to add bio",
                    color = if (user?.bio?.isBlank() != false) Color.Gray else Color.White,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    fontStyle = if (user?.bio?.isBlank() != false) androidx.compose.ui.text.font.FontStyle.Italic else androidx.compose.ui.text.font.FontStyle.Normal,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Online status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (user?.isOnline == true) Color(0xFF10B981) else Color.Gray
                ) {
                    Text(
                        text = if (user?.isOnline == true) "Online" else "Last seen recently",
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Stats row
            if (isOwnProfile) {
                StatsRow(
                    memberSince = 2024,
                    messages = user?.messageCount ?: 1247,
                    files = user?.filesShared ?: 234
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Action buttons
            if (isOwnProfile) {
                OutlinedButton(
                    onClick = { showEditSheet = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFF7C3AED)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.linearGradient(listOf(Color(0xFF7C3AED), Color(0xFF7C3AED)))
                    )
                ) {
                    Icon(Icons.Default.Edit, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Profile")
                }
            } else {
                // Action buttons for other users
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // MESSAGE - primary action, wider
                    Button(
                        onClick = {
                            try {
                                navController.navigate("chat/$userId") {
                                    popUpTo("profile/$userId") { inclusive = true }
                                }
                            } catch (e: Exception) { }
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF7C3AED)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Chat,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Message", fontSize = 14.sp, maxLines = 1)
                    }
                    
                    // CALL
                    OutlinedButton(
                        onClick = {
                            try {
                                // Start audio call with WebRTC
                                userId?.let { contactId ->
                                    callViewModel.startCall(contactId, CallType.AUDIO)
                                    navController.navigate("active_call/$contactId/audio")
                                }
                            } catch (e: Exception) { }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Call", color = Color(0xFF7C3AED), fontSize = 14.sp)
                    }
                    
                    // VIDEO
                    OutlinedButton(
                        onClick = {
                            try {
                                // Start video call with WebRTC
                                userId?.let { contactId ->
                                    callViewModel.startCall(contactId, CallType.VIDEO)
                                    navController.navigate("active_call/$contactId/video")
                                }
                            } catch (e: Exception) { }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C3AED)),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = null,
                            tint = Color(0xFF7C3AED),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Video", color = Color(0xFF7C3AED), fontSize = 14.sp)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Other user actions
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1A1A2E)
                ) {
                    Column {
                        ProfileActionItem(
                            icon = Icons.Default.NotificationsOff,
                            text = "Mute Notifications",
                            hasSwitch = true,
                            onClick = { }
                        )
                        
                        HorizontalDivider(color = Color(0xFF2D2D44))
                        
                        ProfileActionItem(
                            icon = Icons.Default.Block,
                            text = "Block User",
                            textColor = Color(0xFFEF4444),
                            onClick = { }
                        )
                        
                        HorizontalDivider(color = Color(0xFF2D2D44))
                        
                        ProfileActionItem(
                            icon = Icons.Default.Flag,
                            text = "Report",
                            textColor = Color(0xFFEF4444),
                            onClick = { }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
    
    // Edit Profile Sheet
    if (showEditSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEditSheet = false },
            containerColor = Color(0xFF1A1A2E)
        ) {
            EditProfileSheet(
                user = user,
                onDismiss = { showEditSheet = false },
                onSave = { updatedUser ->
                    showEditSheet = false
                    // Show success snackbar
                }
            )
        }
    }
    
    // Photo adjuster overlay for profile photo
    if (showPhotoAdjuster && selectedPhotoUri != null) {
        PhotoAdjuster(
            imageUri = selectedPhotoUri,
            onPositionChange = { newPosition ->
                photoVerticalPosition = newPosition
            },
            onConfirm = {
                showPhotoAdjuster = false
                android.widget.Toast.makeText(
                    context,
                    "Foto de perfil ajustada",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                // TODO: Upload to Firebase Storage
            },
            onCancel = {
                showPhotoAdjuster = false
                selectedPhotoUri = null
                photoVerticalPosition = 0f
            },
            initialPosition = photoVerticalPosition,
            title = "Ajustar Foto de Perfil"
        )
    }
    
    // Photo adjuster overlay for cover photo
    if (showCoverAdjuster && selectedCoverUri != null) {
        PhotoAdjuster(
            imageUri = selectedCoverUri,
            onPositionChange = { newPosition ->
                coverVerticalPosition = newPosition
            },
            onConfirm = {
                showCoverAdjuster = false
                android.widget.Toast.makeText(
                    context,
                    "Foto de portada ajustada",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                // TODO: Upload to Firebase Storage
            },
            onCancel = {
                showCoverAdjuster = false
                selectedCoverUri = null
                coverVerticalPosition = 0f
            },
            initialPosition = coverVerticalPosition,
            title = "Ajustar Foto de Portada"
        )
    }
    
    // Fullscreen image viewer
    if (showFullscreenImage && selectedPhotoUri != null) {
        FullScreenImageViewer(
            imageUrl = selectedPhotoUri.toString(),
            senderName = user?.name ?: "Profile Photo",
            timestamp = "",
            onDismiss = { showFullscreenImage = false },
            onDownload = null
        )
    }
}

@Composable
fun AnimatedAvatar(
    name: String,
    photoUri: Uri?,
    photoPosition: Float,
    isOwnProfile: Boolean,
    onCameraClick: () -> Unit,
    onPhotoClick: () -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "avatar")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    
    Box(contentAlignment = Alignment.Center) {
        // Rainbow gradient ring
        Box(
            modifier = Modifier
                .size(98.dp)
                .rotate(rotation)
                .background(
                    Brush.sweepGradient(
                        listOf(
                            Color(0xFFFF0000),
                            Color(0xFFFF7F00),
                            Color(0xFFFFFF00),
                            Color(0xFF00FF00),
                            Color(0xFF0000FF),
                            Color(0xFF4B0082),
                            Color(0xFF9400D3),
                            Color(0xFFFF0000)
                        )
                    ),
                    CircleShape
                )
        )
        
        // Avatar
        Surface(
            modifier = Modifier
                .size(90.dp)
                .safeClickable(
                    enabled = photoUri != null,
                    onClick = onPhotoClick
                ),
            shape = CircleShape,
            color = Color(0xFF7C3AED)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (photoUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(photoUri),
                        contentDescription = "Profile photo",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .graphicsLayer {
                                translationY = photoPosition
                            },
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = name.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        // Camera icon for own profile
        if (isOwnProfile) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(32.dp)
                    .safeClickable(onClick = onCameraClick),
                shape = CircleShape,
                color = Color(0xFF7C3AED)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.CameraAlt,
                        contentDescription = "Change photo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun StatsRow(
    memberSince: Int,
    messages: Int,
    files: Int
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProfileStatItem(label = "Member Since", value = memberSince.toString())
            
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color(0xFF2D2D44))
            )
            
            ProfileStatItem(label = "Messages", value = messages.toString())
            
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(40.dp)
                    .background(Color(0xFF2D2D44))
            )
            
            ProfileStatItem(label = "Files", value = files.toString())
        }
    }
}

@Composable
private fun ProfileStatItem(label: String, value: String) {
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

@Composable
fun ProfileActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: Color = Color.White,
    hasSwitch: Boolean = false,
    onClick: () -> Unit
) {
    var switchState by remember { mutableStateOf(false) }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onClick = onClick),
        color = Color.Transparent
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
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = text,
                color = textColor,
                fontSize = 16.sp,
                modifier = Modifier.weight(1f)
            )
            
            if (hasSwitch) {
                Switch(
                    checked = switchState,
                    onCheckedChange = { switchState = it },
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = Color(0xFF7C3AED)
                    )
                )
            }
        }
    }
}

@Composable
fun EditProfileSheet(
    user: User?,
    onDismiss: () -> Unit,
    onSave: (User) -> Unit
) {
    var name by remember { mutableStateOf(user?.name ?: "") }
    var username by remember { mutableStateOf(user?.username ?: "") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }
    var phone by remember { mutableStateOf(user?.phone ?: "") }
    var email by remember { mutableStateOf(user?.email ?: "") }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Edit Profile",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Display Name", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedBorderColor = Color(0xFF3D3D5C)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.AlternateEmail, null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedBorderColor = Color(0xFF3D3D5C)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = bio,
            onValueChange = { if (it.length <= 150) bio = it },
            label = { Text("Bio", color = Color.Gray) },
            supportingText = { Text("${bio.length}/150", color = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedBorderColor = Color(0xFF3D3D5C)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedBorderColor = Color(0xFF3D3D5C)
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Email, null, tint = Color.Gray) },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF7C3AED),
                unfocusedBorderColor = Color(0xFF3D3D5C)
            )
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                // Simulate save
                onSave(user?.copy(
                    name = name,
                    username = username,
                    bio = bio,
                    phone = phone,
                    email = email
                ) ?: return@Button)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF7C3AED)
            )
        ) {
            Text("Save Changes")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
