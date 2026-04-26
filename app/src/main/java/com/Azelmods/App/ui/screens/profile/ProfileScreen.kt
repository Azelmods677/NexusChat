package com.Azelmods.App.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.DarkSurfaceVariant
import com.Azelmods.App.ui.theme.Purple
import java.text.SimpleDateFormat
import java.util.*

/**
 * ProfileScreen - Displays user profile with real-time online status.
 *
 * Features:
 * - Cover photo with gradient fallback
 * - Profile photo with online indicator
 * - Real-time online status and last seen
 * - Profile stats (messages, files, member since)
 * - Action buttons (message, call) for other users
 * - Edit profile bottom sheet for own profile
 * - Photo upload with crop support
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    userId: String,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val storedUserId by viewModel.userId.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // Image pickers
    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navController.navigate("image_crop?uri=${Uri.encode(it.toString())}&type=profile")
        }
    }

    val coverPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            navController.navigate("image_crop?uri=${Uri.encode(it.toString())}&type=cover")
        }
    }

    // Listen for crop results
    LaunchedEffect(Unit) {
        navController.currentBackStackEntry
            ?.savedStateHandle
            ?.getStateFlow<Map<String, Any>?>("cropResult", null)
            ?.collect { result ->
                result?.let {
                    val uri = Uri.parse(it["uri"] as String)
                    val type = it["type"] as String
                    val scale = (it["scale"] as? Number)?.toFloat() ?: 1f
                    val offsetX = (it["offsetX"] as? Number)?.toFloat() ?: 0f
                    val offsetY = (it["offsetY"] as? Number)?.toFloat() ?: 0f

                    when (type) {
                        "profile" -> viewModel.uploadProfilePhoto(uri, scale, offsetX, offsetY)
                        "cover" -> viewModel.uploadCoverPhoto(uri, scale, offsetX, offsetY)
                    }

                    // Clear the result
                    navController.currentBackStackEntry?.savedStateHandle?.remove<Map<String, Any>>("cropResult")
                }
            }
    }

    // Load profile on first composition
    LaunchedEffect(userId) {
        viewModel.loadUserProfile(userId)
    }

    // Show snackbar on upload success
    LaunchedEffect(state.uploadSuccess) {
        if (state.uploadSuccess) {
            snackbarHostState.showSnackbar("Profile updated successfully")
            viewModel.clearUploadSuccess()
        }
    }

    // Show snackbar on error
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading && state.user == null) {
                // Loading indicator
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Purple
                )
            } else if (state.user != null) {
                // Profile content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Cover photo
                    CoverPhotoSection(
                        coverUrl = state.user?.coverUrl,
                        isOwnProfile = state.isOwnProfile,
                        isUploading = state.isUploadingCover,
                        onCoverClick = { coverPhotoPicker.launch("image/*") }
                    )

                    // Avatar (overlapping cover)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (-48).dp),
                        contentAlignment = Alignment.Center
                    ) {
                        AvatarSection(
                            photoUrl = state.user?.photoUrl,
                            displayName = state.user?.displayName ?: state.user?.name ?: "",
                            isOnline = state.isOnline,
                            isOwnProfile = state.isOwnProfile,
                            isUploading = state.isUploadingProfile,
                            onAvatarClick = { profilePhotoPicker.launch("image/*") }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Name + username + edit button
                    NameSection(
                        displayName = state.user?.displayName ?: state.user?.name ?: "",
                        username = state.user?.username ?: "",
                        isOwnProfile = state.isOwnProfile,
                        onEditClick = { viewModel.toggleEditSheet() }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Online status
                    OnlineStatusSection(
                        isOnline = state.isOnline,
                        lastSeen = state.lastSeen
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Status/bio
                    StatusBioSection(
                        status = state.user?.status ?: "",
                        bio = state.user?.bio ?: ""
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Stats
                    StatsSection(
                        messageCount = state.user?.messageCount ?: 0,
                        filesShared = state.user?.filesShared ?: 0,
                        createdAt = state.user?.createdAt ?: 0L
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action buttons (if not own profile)
                    if (!state.isOwnProfile) {
                        ActionButtonsSection(
                            userId = userId,
                            onMessageClick = {
                                val chatId = viewModel.buildChatId(userId)
                                navController.navigate("chat/$chatId")
                            },
                            onCallClick = {
                                navController.navigate("incoming_call/$userId/audio")
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                }
            } else {
                // Error state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error ?: "Failed to load profile",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Edit profile bottom sheet
        if (state.showEditSheet && state.isOwnProfile) {
            EditProfileBottomSheet(
                user = state.user,
                onDismiss = { viewModel.toggleEditSheet() },
                onSave = { name, bio, status ->
                    viewModel.saveProfile(name, bio, status)
                }
            )
        }
    }
}

// ── Cover Photo Section ───────────────────────────────────────────────────────

@Composable
private fun CoverPhotoSection(
    coverUrl: String?,
    isOwnProfile: Boolean,
    isUploading: Boolean,
    onCoverClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        if (coverUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Purple, Color(0xFF1A1A2E))
                                )
                            )
                    )
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Purple, Color(0xFF1A1A2E))
                                )
                            )
                    )
                }
            )
        } else {
            // Gradient fallback
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Purple, Color(0xFF1A1A2E))
                        )
                    )
            )
        }

        // Camera button (own profile only)
        if (isOwnProfile) {
            IconButton(
                onClick = onCoverClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change cover photo",
                        tint = Color.White
                    )
                }
            }
        }
    }
}

// ── Avatar Section ────────────────────────────────────────────────────────────

@Composable
private fun AvatarSection(
    photoUrl: String?,
    displayName: String,
    isOnline: Boolean,
    isOwnProfile: Boolean,
    isUploading: Boolean,
    onAvatarClick: () -> Unit
) {
    val context = LocalContext.current

    Box(
        contentAlignment = Alignment.BottomEnd
    ) {
        // Avatar
        if (photoUrl != null) {
            SubcomposeAsyncImage(
                model = ImageRequest.Builder(context)
                    .data(photoUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile photo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .border(3.dp, DarkBackground, CircleShape),
                loading = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Purple, Color(0xFF1A1A2E))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                error = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(Purple, Color(0xFF1A1A2E))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            )
        } else {
            // Initials fallback
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(Purple, Color(0xFF1A1A2E))
                        )
                    )
                    .border(3.dp, DarkBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = displayName.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Online indicator
        if (isOnline) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4CAF50))
                    .border(2.dp, DarkBackground, CircleShape)
            )
        }

        // Camera button (own profile only)
        if (isOwnProfile) {
            FloatingActionButton(
                onClick = onAvatarClick,
                modifier = Modifier
                    .size(32.dp)
                    .offset(x = 8.dp, y = 8.dp),
                containerColor = Purple,
                contentColor = Color.White
            ) {
                if (isUploading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Change profile photo",
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ── Name Section ──────────────────────────────────────────────────────────────

@Composable
private fun NameSection(
    displayName: String,
    username: String,
    isOwnProfile: Boolean,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (username.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "@$username",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (isOwnProfile) {
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit profile",
                    tint = Purple
                )
            }
        }
    }
}

// ── Online Status Section ─────────────────────────────────────────────────────

@Composable
private fun OnlineStatusSection(
    isOnline: Boolean,
    lastSeen: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        if (isOnline) {
            Text(
                text = "● Online",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFF4CAF50)
            )
        } else {
            Text(
                text = "Last seen ${formatLastSeen(lastSeen)}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Status/Bio Section ────────────────────────────────────────────────────────

@Composable
private fun StatusBioSection(
    status: String,
    bio: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = status,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        if (bio.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = bio,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Stats Section ─────────────────────────────────────────────────────────────

@Composable
private fun StatsSection(
    messageCount: Int,
    filesShared: Int,
    createdAt: Long
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatItem(
            value = messageCount.toString(),
            label = "Messages"
        )

        StatItem(
            value = filesShared.toString(),
            label = "Files"
        )

        StatItem(
            value = formatMemberSince(createdAt),
            label = "Member since"
        )
    }
}

@Composable
private fun StatItem(
    value: String,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Action Buttons Section ────────────────────────────────────────────────────

@Composable
private fun ActionButtonsSection(
    userId: String,
    onMessageClick: () -> Unit,
    onCallClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FilledTonalButton(
            onClick = onMessageClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Purple,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Message,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Message")
        }

        FilledTonalButton(
            onClick = onCallClick,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = DarkSurfaceVariant,
                contentColor = Color.White
            )
        ) {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Call")
        }
    }
}

// ── Edit Profile Bottom Sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditProfileBottomSheet(
    user: com.Azelmods.App.data.model.User?,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    var name by remember { mutableStateOf(user?.displayName ?: user?.name ?: "") }
    var bio by remember { mutableStateOf(user?.bio ?: "") }
    var status by remember { mutableStateOf(user?.status ?: "") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Edit Profile",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Display Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = Purple,
                    cursorColor = Purple
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = bio,
                onValueChange = { bio = it },
                label = { Text("Bio") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = Purple,
                    cursorColor = Purple
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = status,
                onValueChange = { status = it },
                label = { Text("Status") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Purple,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    focusedLabelColor = Purple,
                    cursorColor = Purple
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        onSave(name, bio, status)
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple,
                        contentColor = Color.White
                    )
                ) {
                    Text("Save")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ── Helper Functions ──────────────────────────────────────────────────────────

private fun formatLastSeen(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "just now"
        diff < 3_600_000 -> "${diff / 60_000}m ago"
        diff < 86_400_000 -> "${diff / 3_600_000}h ago"
        diff < 172_800_000 -> "yesterday"
        else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatMemberSince(timestamp: Long): String {
    return SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}
