package com.Azelmods.App.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.Azelmods.App.data.model.BackgroundType
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.ui.components.safeClickable
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.components.UnifiedTopBar
import com.Azelmods.App.ui.components.TopBarActionIcon
import com.Azelmods.App.ui.components.CompleteEmojiPicker
import com.Azelmods.App.ui.components.StickerPicker
import com.Azelmods.App.ui.components.AttachmentBottomSheet
import com.Azelmods.App.ui.components.AttachmentType
import com.Azelmods.App.ui.components.FullScreenImageViewer
import com.Azelmods.App.ui.components.VideoWallpaper
import com.Azelmods.App.ui.components.chat.MessageBubble
import com.Azelmods.App.ui.components.AudioMessagePlayer
import com.Azelmods.App.ui.components.ReadReceiptIndicator
import com.Azelmods.App.ui.theme.linearGradientBrush
import com.Azelmods.App.ui.theme.parseHexColor
import com.Azelmods.App.ui.theme.rememberThemeColor
import com.Azelmods.App.ui.theme.rememberThemeSecondaryColor
import com.Azelmods.App.utils.AudioRecorder
import com.Azelmods.App.utils.PermissionHelper
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactId: String,
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel(),
    settingsViewModel: com.Azelmods.App.ui.screens.settings.SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    val state by viewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val themeColor = rememberThemeColor()
    val themeSecondaryColor = rememberThemeSecondaryColor()
    val haptic = LocalHapticFeedback.current
    
    // Load chat background from repository
    val backgroundConfig by viewModel.chatBackground.collectAsState()
    
    LaunchedEffect(contactId) {
        viewModel.loadChatBackground(contactId)
    }
    
    // Populate messageText when entering edit mode
    LaunchedEffect(state.editingMessage) {
        state.editingMessage?.let { editingMsg ->
            messageText = editingMsg.content
        }
    }
    
    // Image viewer state - MOVED TO CHATSCREEN LEVEL
    var showImageViewer by remember { mutableStateOf(false) }
    var selectedImageUrl by remember { mutableStateOf("") }
    var selectedImageSender by remember { mutableStateOf("") }
    var selectedImageTimestamp by remember { mutableStateOf("") }
    
    LaunchedEffect(contactId) {
        viewModel.loadChat(contactId)
    }
    
    // Auto-scroll to bottom when new message arrives
    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.animateScrollToItem(state.messages.size - 1)
        }
    }
    
    val sendErrorSnackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.error) {
        state.error?.let { err ->
            sendErrorSnackbar.showSnackbar(err)
            viewModel.clearError()
        }
    }

    LaunchedEffect(state.translationError) {
        state.translationError?.let { err ->
            sendErrorSnackbar.showSnackbar(err)
            viewModel.clearTranslationError()
        }
    }

    // Avisos informativos de traducción (truncado a 500 chars / cuota diaria baja):
    // la traducción sí ocurrió, pero el usuario debe saber sus límites.
    LaunchedEffect(state.translationNotice) {
        state.translationNotice?.let { notice ->
            sendErrorSnackbar.showSnackbar(notice)
            viewModel.clearTranslationNotice()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(sendErrorSnackbar) },
        topBar = {
            ChatTopBar(
                contact = state.contact,
                isTyping = state.isTyping,
                onBackClick = { navController.navigateUp() },
                onProfileClick = {
                    try {
                        // Navigate using the REAL contact uid, not the chatId.
                        val profileId = state.contact?.uid?.takeIf { it.isNotBlank() } ?: contactId
                        navController.navigate("profile_viewer/$profileId")
                    } catch (e: Exception) { }
                },
                onGalleryClick = {
                    try {
                        navController.navigate("media_gallery/$contactId")
                    } catch (e: Exception) { }
                },
                onPhoneClick = { 
                    try {
                        val calleeId = state.contact?.uid?.takeIf { it.isNotBlank() } ?: contactId
                        navController.navigate("active_call/$calleeId/audio")
                    } catch (e: Exception) { }
                },
                onVideoClick = { 
                    try {
                        val calleeId = state.contact?.uid?.takeIf { it.isNotBlank() } ?: contactId
                        navController.navigate("active_call/$calleeId/video")
                    } catch (e: Exception) { }
                },
                onMoreClick = { showMenu = true }
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Wallpaper background from ChatBackgroundRepository (per-chat)
            when (backgroundConfig.type) {
                BackgroundType.IMAGE -> {
                    backgroundConfig.imageUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            alpha = 1f - backgroundConfig.overlayAlpha
                        )
                    }
                }
                BackgroundType.VIDEO -> {
                    backgroundConfig.videoUri?.let { uri ->
                        VideoWallpaper(
                            videoUri = Uri.parse(uri),
                            modifier = Modifier.fillMaxSize(),
                            alpha = 1f - backgroundConfig.overlayAlpha
                        )
                    }
                }
                BackgroundType.SOLID_COLOR -> {
                    backgroundConfig.colorHex?.let { hex ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(parseHexColor(hex))
                        )
                    }
                }
                BackgroundType.GRADIENT -> {
                    val brush = linearGradientBrush(
                        gradientColors = backgroundConfig.gradientColors,
                        gradientAngle = backgroundConfig.gradientAngle
                    )
                    if (brush != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush)
                        )
                    }
                }
                else -> {
                    // No per-chat background — global wallpaper from MainActivity shows through
                    // Just use a transparent spacer so content layout is consistent
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding() // CRITICAL: This makes content adjust when keyboard appears
            ) {
                // Messages List
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.refreshChat() },
                    modifier = Modifier.weight(1f)
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // ⬆️ Loading MORE indicator at the top (pagination)
                        if (state.isLoadingMore) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(20.dp),
                                            color = MaterialTheme.colorScheme.primary,
                                            strokeWidth = 2.dp
                                        )
                                        Text(
                                            "Cargando mensajes anteriores...",
                                            color = Color.Gray,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }

                        // No more messages indicator
                        if (!state.hasMoreMessages && state.messages.isNotEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "— No hay más mensajes —",
                                        color = Color.Gray.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        items(
                            items = state.messages,
                            key = { message ->
                                if (message.messageId.isNotBlank()) {
                                    message.messageId
                                } else {
                                    // Generate unique key from content hash
                                    val contentHash = "${message.content}_${message.senderId}_${message.timestamp}_${message.mediaUrl}".hashCode()
                                    "fallback_${contentHash}"
                                }
                            }
                        ) { message ->
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            MessageBubble(
                                message = message,
                                isOwnMessage = message.senderId == currentUserId,
                                onLongPress = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setReplyingTo(message)
                                },
                                onReactionClick = { emoji -> viewModel.addReaction(message.messageId, emoji) },
                                onImageClick = { url, sender, timestamp ->
                                    selectedImageUrl = url
                                    selectedImageSender = sender
                                    selectedImageTimestamp = timestamp
                                    showImageViewer = true
                                },
                                themeColor = themeColor,
                                themeSecondaryColor = themeSecondaryColor,
                                onDeleteClick = { forEveryone ->
                                    viewModel.deleteMessage(message, forEveryone)
                                },
                                onEditClick = {
                                    viewModel.setEditingMessage(message)
                                },
                                onMessageViewed = {
                                    viewModel.markMessageViewed(message)
                                },
                                translatedText = state.translatedMessages[message.messageId],
                                isTranslating = message.messageId in state.translatingMessageIds,
                                onTranslate = {
                                    viewModel.translateMessage(message.messageId, message.content)
                                }
                            )
                        }
                        
                        // Typing indicator
                        if (state.isTyping) {
                            item {
                                TypingIndicator()
                            }
                        }
                    }
                    
                    // 🆕 Scroll-to-top detection for pagination
                    val firstVisibleItem by remember {
                        derivedStateOf {
                            listState.firstVisibleItemIndex
                        }
                    }
                    
                    LaunchedEffect(firstVisibleItem) {
                        // When scrolled to the very top (index 0), load more messages
                        if (firstVisibleItem == 0 && !state.isLoadingMore && state.hasMoreMessages) {
                            viewModel.loadMoreMessages()
                        }
                    }
                }
                
                // Reply Preview Bar + Input Area at bottom
                Column {
                    // Reply Preview Bar
                    AnimatedVisibility(
                        visible = state.replyingTo != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        state.replyingTo?.let { message ->
                            ReplyPreviewBar(
                                message = message,
                                onCancel = { viewModel.setReplyingTo(null) }
                            )
                        }
                    }
                    
                    // Edit Mode Preview Bar
                    AnimatedVisibility(
                        visible = state.editingMessage != null,
                        enter = slideInVertically { it } + fadeIn(),
                        exit = slideOutVertically { it } + fadeOut()
                    ) {
                        state.editingMessage?.let { message ->
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0xFF1A1A2E)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(40.dp)
                                            .background(Color(0xFF00BFA6))
                                    )
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Editar mensaje",
                                            color = Color(0xFF00BFA6),
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = message.content,
                                            color = Color.Gray,
                                            fontSize = 13.sp,
                                            maxLines = 5,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    IconButton(onClick = { 
                                        viewModel.setEditingMessage(null)
                                        messageText = ""
                                    }) {
                                        Icon(Icons.Default.Close, null, tint = Color.Gray)
                                    }
                                }
                            }
                        }
                    }
                    
                    // Input Area - ALWAYS AT BOTTOM
                    ChatInputArea(
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onSendClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            state.editingMessage?.let { editingMsg ->
                                // Edit mode - save edited message
                                viewModel.editMessage(editingMsg.messageId, messageText)
                                messageText = ""
                            } ?: run {
                                // Normal mode - send new message
                                viewModel.sendMessage(messageText, contactId)
                                messageText = ""
                            }
                        },
                        contactId = contactId,
                        viewModel = viewModel,
                        isEphemeralMode = state.isEphemeralMode,
                        ephemeralDuration = state.ephemeralDuration,
                        showEphemeralPicker = state.showEphemeralPicker,
                        onToggleEphemeral = { viewModel.toggleEphemeralMode() },
                        onSetEphemeralDuration = { viewModel.setEphemeralDuration(it) },
                        onToggleEphemeralPicker = { viewModel.toggleEphemeralPicker() },
                        onDismissEphemeralPicker = { viewModel.dismissEphemeralPicker() }
                    )
                }
            }
            
            // Dropdown Menu (overlay)
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(Color(0xFF1A1A2E))
            ) {
                DropdownMenuItem(
                    text = { Text("View Profile", color = Color.White) },
                    onClick = {
                        showMenu = false
                        try {
                            val profileId = state.contact?.uid?.takeIf { it.isNotBlank() } ?: contactId
                            navController.navigate("profile/$profileId")
                        } catch (e: Exception) { }
                    },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text("Fondo del Chat", color = Color.White) },
                    onClick = {
                        showMenu = false
                        try {
                            navController.navigate("background_picker?chatId=$contactId")
                        } catch (e: Exception) { }
                    },
                    leadingIcon = { Icon(Icons.Default.Wallpaper, null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text("Search", color = Color.White) },
                    onClick = { showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text("Mute", color = Color.White) },
                    onClick = { showMenu = false },
                    leadingIcon = { Icon(Icons.Default.NotificationsOff, null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text("Clear Chat", color = Color(0xFFEF4444)) },
                    onClick = { showMenu = false },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color(0xFFEF4444)) }
                )
            }
        }
        
        // Full Screen Image Viewer
        if (showImageViewer) {
            FullScreenImageViewer(
                imageUrl = selectedImageUrl,
                senderName = selectedImageSender,
                timestamp = selectedImageTimestamp,
                onDismiss = { showImageViewer = false }
            )
        }
    }
}

@Composable
fun ChatTopBar(
    contact: com.Azelmods.App.data.model.User?,
    isTyping: Boolean,
    onBackClick: () -> Unit,
    onProfileClick: () -> Unit,
    onPhoneClick: () -> Unit,
    onVideoClick: () -> Unit,
    onMoreClick: () -> Unit,
    onGalleryClick: () -> Unit = {}
) {
    // Resolve a real display name (never the generic "Usuario" placeholder).
    val displayName = com.Azelmods.App.ui.utils.UserProfileHelper.resolveDisplayName(
        displayName = contact?.displayName,
        name = contact?.name,
        email = contact?.email
    )
    
    // Status text dinámico
    val statusText = when {
        isTyping -> "escribiendo..."
        contact?.isOnline == true -> "en línea"
        else -> "última vez ${formatLastSeen(contact?.lastSeen ?: 0)}"
    }
    
    val statusColor = when {
        isTyping -> Color(0xFF00BFA6)
        contact?.isOnline == true -> Color(0xFF10B981)
        else -> Color.Gray
    }
    
    UnifiedTopBar(
        title = displayName,
        userName = displayName,
        userPhotoUrl = contact?.photoUrl,
        userSubtitle = statusText,
        onUserClick = onProfileClick,
        showBackButton = true,
        onBackClick = onBackClick,
        actions = {
            TopBarActionIcon(
                icon = Icons.Default.PhotoLibrary,
                contentDescription = "Gallery",
                onClick = onGalleryClick
            )
            TopBarActionIcon(
                icon = Icons.Default.Phone,
                contentDescription = "Voice Call",
                onClick = onPhoneClick
            )
            TopBarActionIcon(
                icon = Icons.Default.Videocam,
                contentDescription = "Video Call",
                onClick = onVideoClick
            )
            TopBarActionIcon(
                icon = Icons.Default.MoreVert,
                contentDescription = "More options",
                onClick = onMoreClick
            )
        },
        backgroundColor = Color(0xFF1A1A2E),
        contentColor = Color.White
    )
}

@Composable
fun TypingDots() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(start = 4.dp)
    ) {
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "dot$index")
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 200),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )
            
            Text(
                text = ".",
                color = Color(0xFF00BFA6).copy(alpha = alpha),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color(0xFF2D2D44),
            modifier = Modifier.padding(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "typing$index")
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = -8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(600, delayMillis = index * 200),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "offset"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .offset(y = offsetY.dp)
                            .background(Color.Gray, CircleShape)
                    )
                }
            }
        }
    }
}

@Composable
fun ReplyPreviewBar(
    message: Message,
    onCancel: () -> Unit
) {
    val themeColor = rememberThemeColor()
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1A2E)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(40.dp)
                    .background(themeColor)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.senderName,
                    color = themeColor,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message.content,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, null, tint = Color.Gray)
            }
        }
    }
}

@Composable
fun EphemeralDurationPicker(
    currentDuration: Long,
    onSelect: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        0L to "👁️ Ver una vez",
        5L to "🕐 5 segundos",
        30L to "🕐 30 segundos",
        60L to "🕐 1 minuto",
        300L to "🕐 5 minutos",
        3600L to "🕐 1 hora",
        86400L to "🕐 24 horas",
        604800L to "🕐 7 días"
    )
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF1A1A2E),
        shadowElevation = 8.dp
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            options.forEach { (duration, label) ->
                val isSelected = duration == currentDuration
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .safeClickable {
                            if (isSelected) onDismiss() else onSelect(duration)
                        },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF9B75FF) else Color.White,
                            fontSize = 14.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = null,
                                tint = Color(0xFF9B75FF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatInputArea(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    contactId: String,
    viewModel: ChatViewModel,
    isEphemeralMode: Boolean = false,
    ephemeralDuration: Long = 0L,
    showEphemeralPicker: Boolean = false,
    onToggleEphemeral: () -> Unit = {},
    onSetEphemeralDuration: (Long) -> Unit = {},
    onToggleEphemeralPicker: () -> Unit = {},
    onDismissEphemeralPicker: () -> Unit = {}
) {
    val context = LocalContext.current
    val themeColor = rememberThemeColor()
    val themeSecondaryColor = rememberThemeSecondaryColor()
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var audioRecorder by remember { mutableStateOf<AudioRecorder?>(null) }
    
    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Start recording
            audioRecorder = AudioRecorder(context)
            val file = audioRecorder?.startRecording()
            if (file != null) {
                isRecording = true
            }
        }
    }
    
    // Camera launcher - DECLARE BEFORE PERMISSION LAUNCHER
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            cameraUri?.let { uri ->
                viewModel.sendImageMessage(uri, contactId)
            }
        }
    }
    
    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraUri?.let { cameraLauncher.launch(it) }
        }
    }
    
    // Audio file picker launcher
    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            uri?.let {
                viewModel.sendAudioMessage(it, contactId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Get current location and send
            try {
                val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                
                location?.let {
                    viewModel.sendLocationMessage(
                        latitude = it.latitude,
                        longitude = it.longitude,
                        address = "Lat: ${it.latitude}, Lon: ${it.longitude}",
                        chatId = contactId
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    // Contact picker launcher
    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri: Uri? ->
        try {
            uri?.let { contactUri ->
                val cursor = context.contentResolver.query(contactUri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex = it.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                        val contactName = if (nameIndex >= 0) it.getString(nameIndex) else "Unknown Contact"
                        // Send contact as a text message
                        viewModel.sendMessage("[Contacto: $contactName]", contactId)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Contacts permission launcher
    val contactsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            contactPickerLauncher.launch(null)
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
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            uri?.let {
                viewModel.sendImageMessage(it, contactId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            uri?.let {
                viewModel.sendVideoMessage(it, contactId)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Document picker launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        try {
            uri?.let {
                val fileName = "document_${System.currentTimeMillis()}"
                viewModel.sendDocumentMessage(it, contactId, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            audioRecorder?.cancelRecording()
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF1A1A2E))
    ) {
        // Sticker Picker - NEW
        AnimatedVisibility(
            visible = showStickerPicker,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            StickerPicker(
                onStickerSelected = { sticker ->
                    viewModel.sendStickerMessage(sticker, contactId)
                    showStickerPicker = false
                },
                onDismiss = { showStickerPicker = false }
            )
        }
        
        // Emoji Picker - COMPLETE VERSION WITH 1000+ EMOJIS
        AnimatedVisibility(
            visible = showEmojiPicker,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            CompleteEmojiPicker(
                onEmojiSelected = { emoji ->
                    onMessageChange(messageText + emoji)
                }
            )
        }
        
        // Attachment Menu - NEW ADVANCED VERSION
        AnimatedVisibility(
            visible = showAttachmentMenu,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            AttachmentBottomSheet(
                onAttachmentSelected = { type ->
                    when (type) {
                        AttachmentType.GALLERY -> {
                            if (PermissionHelper.hasMediaPermissions(context)) {
                                imagePickerLauncher.launch("image/*")
                            } else {
                                mediaPermissionLauncher.launch(PermissionHelper.mediaPermissions)
                            }
                        }
                        AttachmentType.CAMERA -> {
                            // Create a temporary file for the camera image
                            val photoFile = java.io.File(
                                context.cacheDir,
                                "camera_${System.currentTimeMillis()}.jpg"
                            )
                            cameraUri = androidx.core.content.FileProvider.getUriForFile(
                                context,
                                "${context.packageName}.fileprovider",
                                photoFile
                            )
                            
                            // Check camera permission
                            val uri = cameraUri
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                if (context.checkSelfPermission(android.Manifest.permission.CAMERA) == 
                                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    uri?.let { cameraLauncher.launch(it) }
                                } else {
                                    cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
                                }
                            } else {
                                uri?.let { cameraLauncher.launch(it) }
                            }
                        }
                        AttachmentType.DOCUMENT -> {
                            documentPickerLauncher.launch("*/*")
                        }
                        AttachmentType.VIDEO -> {
                            videoPickerLauncher.launch("video/*")
                        }
                        AttachmentType.AUDIO -> {
                            // Launch audio file picker
                            audioPickerLauncher.launch("audio/*")
                        }
                        AttachmentType.LOCATION -> {
                            // Request location permissions and send location
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                val hasLocationPermission = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == 
                                    android.content.pm.PackageManager.PERMISSION_GRANTED ||
                                    context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == 
                                    android.content.pm.PackageManager.PERMISSION_GRANTED
                                
                                if (hasLocationPermission) {
                                    // Get current location and send
                                    try {
                                        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
                                        val location = locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                                            ?: locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                                        
                                        location?.let {
                                            viewModel.sendLocationMessage(
                                                latitude = it.latitude,
                                                longitude = it.longitude,
                                                address = "Lat: ${it.latitude}, Lon: ${it.longitude}",
                                                chatId = contactId
                                            )
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                } else {
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                }
                            }
                        }
                        AttachmentType.CONTACT -> {
                            // Request contacts permission and launch contact picker
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                if (context.checkSelfPermission(android.Manifest.permission.READ_CONTACTS) == 
                                    android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                    contactPickerLauncher.launch(null)
                                } else {
                                    contactsPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                }
                            } else {
                                contactPickerLauncher.launch(null)
                            }
                        }
                    }
                },
                onDismiss = { showAttachmentMenu = false }
            )
        }
        
        // Recording UI
        AnimatedVisibility(
            visible = isRecording,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            RecordingUI(
                onCancel = { 
                    audioRecorder?.cancelRecording()
                    audioRecorder = null
                    isRecording = false
                },
                onSend = { 
                    val audioFile = audioRecorder?.stopRecording()
                    audioRecorder = null
                    isRecording = false
                    
                    // Send audio message
                    audioFile?.let { file ->
                        val audioUri = Uri.fromFile(file)
                        viewModel.sendAudioMessage(audioUri, contactId)
                    }
                }
            )
        }
        // Ephemeral Duration Picker
        AnimatedVisibility(
            visible = showEphemeralPicker,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            EphemeralDurationPicker(
                currentDuration = ephemeralDuration,
                onSelect = onSetEphemeralDuration,
                onDismiss = onDismissEphemeralPicker
            )
        }
        
        // Ephemeral mode indicator bar
        AnimatedVisibility(
            visible = isEphemeralMode,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut()
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF2D1B69).copy(alpha = 0.6f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = Color(0xFF9B75FF),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (ephemeralDuration == 0L) "📷 Ver una vez"
                                   else "🕐 Se autodestruye en ${
                                       when (ephemeralDuration) {
                                           5L -> "5s"
                                           30L -> "30s"
                                           60L -> "1m"
                                           300L -> "5m"
                                           3600L -> "1h"
                                           86400L -> "24h"
                                           604800L -> "7d"
                                           else -> "${ephemeralDuration}s"
                                       }
                                   }",
                            color = Color(0xFF9B75FF),
                            fontSize = 13.sp
                        )
                    }
                    
                    Row {
                        TextButton(onClick = onToggleEphemeralPicker) {
                            Text(
                                if (ephemeralDuration == 0L) "Cambiar tiempo" else "Cambiar",
                                color = Color(0xFF9B75FF).copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        IconButton(
                            onClick = onToggleEphemeral,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Desactivar",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // ✨ BONITA ENTRY - Main Input Row con diseño mejorado
        AnimatedVisibility(visible = !isRecording) {
            // Contenedor principal con gradiente de fondo sutil
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF1A1A2E).copy(alpha = 0.95f),
                                Color(0xFF0F0F1A).copy(alpha = 0.98f)
                            )
                        )
                    )
                    .padding(horizontal = 10.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Ephemeral toggle button (timer icon)
                    val ephemeralScale by animateFloatAsState(
                        targetValue = if (isEphemeralMode) 1.2f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "ephemeral_scale"
                    )
                    
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(ephemeralScale)
                            .safeClickable {
                                if (isEphemeralMode) {
                                    onToggleEphemeralPicker()
                                } else {
                                    onToggleEphemeral()
                                }
                            },
                        shape = CircleShape,
                        color = if (isEphemeralMode) {
                            Color(0xFF9B75FF).copy(alpha = 0.2f)
                        } else {
                            Color(0xFF2D2D44).copy(alpha = 0.6f)
                        },
                        shadowElevation = if (isEphemeralMode) 6.dp else 2.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isEphemeralMode) {
                                        Modifier.background(
                                            Brush.radialGradient(
                                                listOf(
                                                    Color(0xFF9B75FF).copy(alpha = 0.2f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                    } else Modifier
                                )
                        ) {
                            Icon(
                                Icons.Default.Timer,
                                contentDescription = "Temporal",
                                tint = if (isEphemeralMode) Color(0xFF9B75FF) else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    // Emoji button con efecto glow
                    val emojiScale by animateFloatAsState(
                        targetValue = if (showEmojiPicker) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "emoji_scale"
                    )
                    
                    val emojiRotation by animateFloatAsState(
                        targetValue = if (showEmojiPicker) 15f else 0f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "emoji_rotation"
                    )
                    
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(emojiScale)
                            .graphicsLayer { rotationZ = emojiRotation }
                            .safeClickable {
                                showEmojiPicker = !showEmojiPicker
                                showStickerPicker = false
                                showAttachmentMenu = false
                            },
                        shape = CircleShape,
                        color = if (showEmojiPicker) {
                            themeColor.copy(alpha = 0.15f)
                        } else {
                            Color(0xFF2D2D44).copy(alpha = 0.6f)
                        },
                        shadowElevation = if (showEmojiPicker) 6.dp else 2.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (showEmojiPicker) {
                                        Modifier.background(
                                            Brush.radialGradient(
                                                listOf(
                                                    themeColor.copy(alpha = 0.2f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                    } else Modifier
                                )
                        ) {
                            Icon(
                                Icons.Default.EmojiEmotions,
                                contentDescription = "Emoji",
                                tint = if (showEmojiPicker) themeColor else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    // Sticker button con animación de rebote
                    val stickerScale by animateFloatAsState(
                        targetValue = if (showStickerPicker) 1.15f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "sticker_scale"
                    )
                    
                    Surface(
                        modifier = Modifier
                            .size(44.dp)
                            .scale(stickerScale)
                            .safeClickable {
                                showStickerPicker = !showStickerPicker
                                showEmojiPicker = false
                                showAttachmentMenu = false
                            },
                        shape = CircleShape,
                        color = if (showStickerPicker) {
                            themeSecondaryColor.copy(alpha = 0.15f)
                        } else {
                            Color(0xFF2D2D44).copy(alpha = 0.6f)
                        },
                        shadowElevation = if (showStickerPicker) 6.dp else 2.dp
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (showStickerPicker) {
                                        Modifier.background(
                                            Brush.radialGradient(
                                                listOf(
                                                    themeSecondaryColor.copy(alpha = 0.2f),
                                                    Color.Transparent
                                                )
                                            )
                                        )
                                    } else Modifier
                                )
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.StickyNote2,
                                contentDescription = "Sticker",
                                tint = if (showStickerPicker) themeSecondaryColor else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    // ✨ Text input con borde gradiente ESTÁTICO (sin rotación)
                    var isFocused by remember { mutableStateOf(false) }
                    val inputFocused = messageText.isNotEmpty() || isFocused
                    
                    // Animación de brillo pulsante (sin rotación)
                    val infiniteTransition = rememberInfiniteTransition(label = "input_glow")
                    val glowAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.8f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2000, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "glow"
                    )
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp) // ALTURA FIJA PEQUEÑA
                    ) {
                        // Borde gradiente ESTÁTICO (no gira)
                        if (inputFocused) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            colors = listOf(
                                                themeColor.copy(alpha = glowAlpha),
                                                themeSecondaryColor.copy(alpha = glowAlpha),
                                                themeColor.copy(alpha = glowAlpha * 0.6f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(24.dp)
                                    )
                            )
                        }
                        
                        // Input surface
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(if (inputFocused) 2.dp else 0.dp),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1A1A2E),
                            shadowElevation = if (inputFocused) 4.dp else 2.dp
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp)
                            ) {
                                BasicTextField(
                                    value = messageText,
                                    onValueChange = onMessageChange,
                                    modifier = Modifier
                                        .weight(1f)
                                        .onFocusChanged { isFocused = it.isFocused },
                                    textStyle = LocalTextStyle.current.copy(
                                        color = Color.White,
                                        fontSize = 15.sp
                                    ),
                                    maxLines = 5, // SOLO 1 LÍNEA
                                    singleLine = false,
                                    decorationBox = { innerTextField ->
                                        if (messageText.isEmpty()) {
                                            Text(
                                                "Escribe un mensaje...",
                                                color = Color.Gray.copy(alpha = 0.5f),
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                
                                // Attachment button
                                AnimatedVisibility(
                                    visible = messageText.isEmpty(),
                                    enter = scaleIn(initialScale = 0.7f) + fadeIn(),
                                    exit = scaleOut(targetScale = 0.7f) + fadeOut()
                                ) {
                                    IconButton(
                                        onClick = {
                                            showAttachmentMenu = !showAttachmentMenu
                                            showEmojiPicker = false
                                            showStickerPicker = false
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.AttachFile,
                                            contentDescription = "Attach",
                                            tint = if (showAttachmentMenu) themeColor else Color.Gray.copy(alpha = 0.7f),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // ✨ Send/Mic button con animación premium y efecto glow
                    AnimatedContent(
                        targetState = messageText.isNotEmpty(),
                        label = "send_button",
                        transitionSpec = {
                            (scaleIn(initialScale = 0.6f) + fadeIn()) togetherWith
                            (scaleOut(targetScale = 0.6f) + fadeOut())
                        }
                    ) { hasText ->
                        if (hasText) {
                            // Send button con efecto de pulso y glow
                            val infiniteTransition = rememberInfiniteTransition(label = "send_pulse")
                            val pulseScale by infiniteTransition.animateFloat(
                                initialValue = 1f,
                                targetValue = 1.08f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse"
                            )
                            
                            val glowAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 0.7f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1200),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "glow"
                            )
                            
                            Box(contentAlignment = Alignment.Center) {
                                // Glow effect background
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .scale(pulseScale)
                                        .background(
                                            Brush.radialGradient(
                                                listOf(
                                                    themeColor.copy(alpha = glowAlpha),
                                                    Color.Transparent
                                                )
                                            ),
                                            CircleShape
                                        )
                                )
                                
                                // Main button
                                Surface(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .scale(pulseScale)
                                        .safeClickable(onClick = onSendClick),
                                    shape = CircleShape,
                                    color = Color.Transparent,
                                    shadowElevation = 12.dp
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        themeColor,
                                                        themeSecondaryColor
                                                    )
                                                )
                                            )
                                            .border(
                                                width = 1.5.dp,
                                                brush = Brush.verticalGradient(
                                                    listOf(
                                                        Color.White.copy(alpha = 0.3f),
                                                        Color.Transparent,
                                                        Color.Black.copy(alpha = 0.2f)
                                                    )
                                                ),
                                                shape = CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = Color.White,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            // Mic button con efecto hover
                            val micScale = remember { Animatable(1f) }
                            
                            LaunchedEffect(Unit) {
                                while (true) {
                                    micScale.animateTo(
                                        1.05f,
                                        animationSpec = tween(1500, easing = FastOutSlowInEasing)
                                    )
                                    micScale.animateTo(
                                        1f,
                                        animationSpec = tween(1500, easing = FastOutSlowInEasing)
                                    )
                                }
                            }
                            
                            Surface(
                                modifier = Modifier
                                    .size(50.dp)
                                    .scale(micScale.value)
                                    .safeClickable {
                                        if (PermissionHelper.hasAudioPermission(context)) {
                                            audioRecorder = AudioRecorder(context)
                                            val file = audioRecorder?.startRecording()
                                            if (file != null) {
                                                isRecording = true
                                            }
                                        } else {
                                            audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                        }
                                    },
                                shape = CircleShape,
                                color = Color.Transparent,
                                shadowElevation = 6.dp
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.linearGradient(
                                                listOf(
                                                    Color(0xFF2D2D44),
                                                    Color(0xFF252538)
                                                )
                                            )
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = themeColor.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Mic,
                                        contentDescription = "Voice",
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecordingUI(
    onCancel: () -> Unit,
    onSend: () -> Unit
) {
    val themeColor = rememberThemeColor()
    val themeSecondaryColor = rememberThemeSecondaryColor()
    var recordingTime by remember { mutableStateOf(0) }
    
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            recordingTime++
        }
    }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cancel button
            IconButton(onClick = onCancel) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Recording animation
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pulsing red dot
                val infiniteTransition = rememberInfiniteTransition(label = "recording")
                val alpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "alpha"
                )
                
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(Color(0xFFEF4444).copy(alpha = alpha), CircleShape)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                // Time
                Text(
                    text = String.format("%02d:%02d", recordingTime / 60, recordingTime % 60),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Animated waveform
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(20) { index ->
                        val height by infiniteTransition.animateFloat(
                            initialValue = 4f,
                            targetValue = 24f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(
                                    durationMillis = 400 + (index * 50),
                                    easing = LinearEasing
                                ),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "wave$index"
                        )
                        
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(height.dp)
                                .background(
                                    themeColor,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Send button
            Surface(
                modifier = Modifier
                    .size(48.dp)
                    .safeClickable(onClick = onSend),
                shape = CircleShape,
                color = Color.Transparent,
                shadowElevation = 8.dp
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.linearGradient(
                            listOf(themeColor, themeSecondaryColor)
                        )
                    ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatLastSeen(timestamp: Long): String {
    if (timestamp <= 0L) return "Hace un momento"
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000 -> "en línea"
        diff < 3_600_000 -> "visto hace ${diff/60_000}min"
        diff < 86_400_000 -> "visto hace ${diff/3_600_000}h"
        diff < 7 * 86_400_000 -> "visto hace ${diff/86_400_000}d"
        else -> "visto el ${SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))}"
    }
}
