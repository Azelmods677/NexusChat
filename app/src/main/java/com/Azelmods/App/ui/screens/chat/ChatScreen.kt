package com.Azelmods.App.ui.screens.chat

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.ui.components.safeClickable
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.components.CompleteEmojiPicker
import com.Azelmods.App.ui.components.StickerPicker
import com.Azelmods.App.ui.components.AttachmentBottomSheet
import com.Azelmods.App.ui.components.AttachmentType
import com.Azelmods.App.ui.components.FullScreenImageViewer
import com.Azelmods.App.ui.components.VideoWallpaper
import com.Azelmods.App.ui.theme.rememberThemeColor
import com.Azelmods.App.ui.theme.rememberThemeSecondaryColor
import com.Azelmods.App.data.preferences.ThemePreferences
import com.Azelmods.App.data.preferences.ChatBackground
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
    viewModel: ChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences(context) }
    val chatBackground = remember { themePrefs.getChatBackground() }
    val videoWallpaperUri = remember { themePrefs.getVideoWallpaperUri() }
    
    val state by viewModel.state.collectAsState()
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val themeColor = rememberThemeColor()
    val themeSecondaryColor = rememberThemeSecondaryColor()
    
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
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            ChatTopBar(
                contact = state.contact,
                isTyping = state.isTyping,
                onBackClick = { navController.navigateUp() },
                onProfileClick = {
                    try {
                        navController.navigate("profile_viewer/$contactId")
                    } catch (e: Exception) { }
                },
                onGalleryClick = {
                    try {
                        navController.navigate("media_gallery/$contactId")
                    } catch (e: Exception) { }
                },
                onPhoneClick = { 
                    try {
                        navController.navigate("incoming_call/$contactId/audio")
                    } catch (e: Exception) { }
                },
                onVideoClick = { 
                    try {
                        navController.navigate("incoming_call/$contactId/video")
                    } catch (e: Exception) { }
                },
                onMoreClick = { showMenu = true }
            )
        },
        containerColor = Color(0xFF0F0F1A)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Video wallpaper background
            if (chatBackground == ChatBackground.VIDEO && videoWallpaperUri != null) {
                VideoWallpaper(
                    videoUri = Uri.parse(videoWallpaperUri),
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .imePadding() // CRITICAL: This makes content adjust when keyboard appears
            ) {
                // Messages List
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(
                            items = state.messages,
                            key = { it.messageId }
                        ) { message ->
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            MessageBubble(
                                message = message,
                                isOwnMessage = message.senderId == currentUserId,
                                onLongPress = { viewModel.setReplyingTo(message) },
                                onReactionClick = { emoji -> viewModel.addReaction(message.messageId, emoji) },
                                onImageClick = { url, sender, timestamp ->
                                    selectedImageUrl = url
                                    selectedImageSender = sender
                                    selectedImageTimestamp = timestamp
                                    showImageViewer = true
                                },
                                themeColor = themeColor,
                                themeSecondaryColor = themeSecondaryColor
                            )
                        }
                        
                        // Typing indicator
                        if (state.isTyping) {
                            item {
                                TypingIndicator()
                            }
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
                    
                    // Input Area - ALWAYS AT BOTTOM
                    ChatInputArea(
                        messageText = messageText,
                        onMessageChange = { messageText = it },
                        onSendClick = {
                            viewModel.sendMessage(messageText, contactId)
                            messageText = ""
                        },
                        contactId = contactId,
                        viewModel = viewModel
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
                            navController.navigate("profile/$contactId")
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
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.VolumeOff, null, tint = Color.White) }
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
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .shadow(4.dp),
        color = Color(0xFF1A1A2E)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back button
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(4.dp))
            
            // Avatar with online indicator - CLICKABLE
            Box(modifier = Modifier.safeClickable(onClick = onProfileClick)) {
                UserAvatar(
                    name = contact?.name ?: "?",
                    photoUrl = contact?.photoUrl,
                    size = 40.dp
                )
                
                if (contact?.isOnline == true) {
                    val infiniteTransition = rememberInfiniteTransition(label = "online")
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1.0f,
                        targetValue = 1.15f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "scale"
                    )
                    
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(10.dp)
                            .scale(scale)
                            .background(Color(0xFF10B981), CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Contact info - ALSO CLICKABLE
            Column(
                modifier = Modifier
                    .weight(1f)
                    .safeClickable(onClick = onProfileClick)
            ) {
                Text(
                    text = contact?.name ?: "Loading...",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                AnimatedContent(
                    targetState = isTyping,
                    label = "status"
                ) { typing ->
                    if (typing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "typing",
                                color = Color(0xFF00BFA6),
                                fontSize = 13.sp
                            )
                            TypingDots()
                        }
                    } else {
                        Text(
                            text = if (contact?.isOnline == true) "online" 
                                   else "last seen ${formatLastSeen(contact?.lastSeen ?: 0)}",
                            color = if (contact?.isOnline == true) Color(0xFF10B981) else Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            }
            
            // Action buttons
            IconButton(onClick = onGalleryClick) {
                Icon(Icons.Default.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            IconButton(onClick = onPhoneClick) {
                Icon(Icons.Default.Phone, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            IconButton(onClick = onVideoClick) {
                Icon(Icons.Default.Videocam, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            
            IconButton(onClick = onMoreClick) {
                Icon(Icons.Default.MoreVert, null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
        }
    }
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
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    onLongPress: () -> Unit,
    onReactionClick: (String) -> Unit,
    onImageClick: ((String, String, String) -> Unit)? = null,
    themeColor: Color = Color(0xFF7C3AED),
    themeSecondaryColor: Color = Color(0xFF5B21B6)
) {
    var showReactionPicker by remember { mutableStateOf(false) }
    val quickReactions = listOf("❤️", "👍", "😂", "😮", "😢", "🙏")
    
    // Entry animation - use remember with key to prevent reset
    var visible by remember(message.messageId) { mutableStateOf(false) }
    LaunchedEffect(message.messageId) {
        visible = true
    }
    
    val offsetX by animateDpAsState(
        targetValue = if (visible) 0.dp else if (isOwnMessage) 50.dp else (-50).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slide_${message.messageId}"
    )
    
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "fade_${message.messageId}"
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX)
            .graphicsLayer { this.alpha = alpha },
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        Column {
            // ✨ DISEÑO 3D TIPO CARTA - Efecto flotante con sombras profundas
            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .graphicsLayer {
                        // Efecto 3D con profundidad
                        shadowElevation = 18.dp.toPx()
                        shape = if (isOwnMessage) {
                            RoundedCornerShape(24.dp, 24.dp, 6.dp, 24.dp)
                        } else {
                            RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp)
                        }
                        clip = true
                        // Sombras personalizadas más oscuras
                        ambientShadowColor = Color.Black.copy(alpha = 0.5f)
                        spotShadowColor = Color.Black.copy(alpha = 0.7f)
                        // Profundidad Z para efecto flotante
                        translationZ = 10.dp.toPx()
                    }
                    .border(
                        width = 1.5.dp,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.25f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f)
                            )
                        ),
                        shape = if (isOwnMessage) {
                            RoundedCornerShape(24.dp, 24.dp, 6.dp, 24.dp)
                        } else {
                            RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp)
                        }
                    )
                    .safeClickable { showReactionPicker = !showReactionPicker },
                shape = if (isOwnMessage) {
                    RoundedCornerShape(24.dp, 24.dp, 6.dp, 24.dp)
                } else {
                    RoundedCornerShape(24.dp, 24.dp, 24.dp, 6.dp)
                },
                color = Color.Transparent,
                shadowElevation = 18.dp, // Aumentado de 4dp a 18dp
                tonalElevation = 10.dp // Elevación tonal para más profundidad
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            if (isOwnMessage) {
                                Brush.linearGradient(
                                    listOf(
                                        themeColor.copy(alpha = 0.95f),
                                        themeSecondaryColor.copy(alpha = 0.95f)
                                    )
                                )
                            } else {
                                Brush.linearGradient(
                                    listOf(
                                        Color(0xFF2D2D44).copy(alpha = 0.95f),
                                        Color(0xFF252538).copy(alpha = 0.95f)
                                    )
                                )
                            }
                        )
                        // Borde interno sutil para efecto de profundidad
                        .padding(1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        // Media content
                        when (message.mediaType) {
                            "IMAGE" -> {
                                message.mediaUrl?.let { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Image message",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 300.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .safeClickable {
                                                onImageClick?.invoke(
                                                    url,
                                                    if (isOwnMessage) "Tú" else "Contacto",
                                                    SimpleDateFormat("HH:mm", Locale.getDefault())
                                                        .format(Date(message.timestamp))
                                                )
                                            },
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    if (message.content.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            "VIDEO" -> {
                                message.mediaUrl?.let { url ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.3f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.PlayCircle,
                                            contentDescription = "Play video",
                                            tint = Color.White,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                    if (message.content.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            "AUDIO" -> {
                                message.mediaUrl?.let { url ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.2f))
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.PlayArrow,
                                            contentDescription = "Play audio",
                                            tint = Color.White,
                                            modifier = Modifier.size(32.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Audio Message",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "0:00",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    if (message.content.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                        
                        // Text content (if any)
                        if (message.content.isNotEmpty()) {
                            // Detect emoji-only messages
                            val isEmojiOnly = message.content.all { char ->
                                char.code in 0x1F300..0x1F9FF || // Emoticons & Symbols
                                char.code in 0x2600..0x26FF ||   // Miscellaneous Symbols
                                char.code in 0x2700..0x27BF ||   // Dingbats
                                char.isWhitespace()
                            }
                            val emojiCount = message.content.count { !it.isWhitespace() }
                            
                            // Message content with conditional styling
                            if (isEmojiOnly && emojiCount <= 5 && message.mediaType == null) {
                                // Large emoji display without bubble background
                                Text(
                                    text = message.content,
                                    fontSize = 44.sp,
                                    lineHeight = 48.sp
                                )
                            } else {
                                // Normal text message
                                Text(
                                    text = message.content,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Time and status
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatTime(message.timestamp),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )
                            
                            if (isOwnMessage) {
                                Spacer(modifier = Modifier.width(4.dp))
                                MessageStatusIcon(message.status)
                            }
                        }
                    }
                }
            }
            
            // Quick reaction picker
            AnimatedVisibility(
                visible = showReactionPicker,
                enter = scaleIn(initialScale = 0.8f) + fadeIn(),
                exit = scaleOut(targetScale = 0.8f) + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF1A1A2E),
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        quickReactions.forEach { emoji ->
                            val scale = remember { Animatable(0f) }
                            
                            LaunchedEffect(Unit) {
                                scale.animateTo(
                                    1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    )
                                )
                            }
                            
                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .scale(scale.value)
                                    .safeClickable {
                                        onReactionClick(emoji)
                                        showReactionPicker = false
                                    },
                                shape = CircleShape,
                                color = Color(0xFF2D2D44)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = emoji,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Reactions display
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.reactions.forEach { (_, emoji) ->
                    val scale = remember { Animatable(0f) }
                    
                    LaunchedEffect(Unit) {
                        scale.animateTo(
                            1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy
                            )
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF2D2D44).copy(alpha = 0.8f),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .scale(scale.value)
                            .safeClickable { onReactionClick(emoji) }
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = emoji,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MessageStatusIcon(status: MessageStatus) {
    val icon = when (status) {
        MessageStatus.SENT -> "✓"
        MessageStatus.DELIVERED -> "✓✓"
        MessageStatus.READ -> "✓✓"
        else -> ""
    }
    
    val color = when (status) {
        MessageStatus.READ -> Color(0xFF00BFA6)
        else -> Color.White.copy(alpha = 0.6f)
    }
    
    Text(
        text = icon,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold
    )
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
                    color = Color(0xFF7C3AED),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = message.content,
                    color = Color.Gray,
                    fontSize = 13.sp,
                    maxLines = 1,
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
fun ChatInputArea(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    contactId: String,
    viewModel: ChatViewModel
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
                            // TODO: Launch camera
                        }
                        AttachmentType.DOCUMENT -> {
                            documentPickerLauncher.launch("*/*")
                        }
                        AttachmentType.AUDIO -> {
                            // TODO: Launch audio picker
                        }
                        AttachmentType.LOCATION -> {
                            // TODO: Send location
                        }
                        AttachmentType.CONTACT -> {
                            // TODO: Send contact
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
        
        // ✨ BONITA ENTRY - Main Input Row con diseño mejorado estilo WhatsApp Mods
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
                                Icons.Default.AddReaction,
                                contentDescription = "Sticker",
                                tint = if (showStickerPicker) themeSecondaryColor else Color.Gray,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    
                    // ✨ Text input con diseño premium - Bordes con gradiente y sombra profunda
                    val inputFocused = messageText.isNotEmpty()
                    val inputElevation by animateDpAsState(
                        targetValue = if (inputFocused) 8.dp else 3.dp,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "input_elevation"
                    )
                    
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 120.dp)
                            .graphicsLayer {
                                shadowElevation = inputElevation.toPx()
                                shape = RoundedCornerShape(28.dp)
                                clip = true
                                // Sombra con color del tema cuando está activo
                                if (inputFocused) {
                                    spotShadowColor = themeColor.copy(alpha = 0.4f)
                                    ambientShadowColor = themeColor.copy(alpha = 0.3f)
                                }
                            }
                            .border(
                                width = if (inputFocused) 1.5.dp else 1.dp,
                                brush = if (inputFocused) {
                                    Brush.horizontalGradient(
                                        listOf(
                                            themeColor.copy(alpha = 0.6f),
                                            themeSecondaryColor.copy(alpha = 0.6f),
                                            themeColor.copy(alpha = 0.6f)
                                        )
                                    )
                                } else {
                                    Brush.linearGradient(
                                        listOf(
                                            Color.White.copy(alpha = 0.1f),
                                            Color.Transparent
                                        )
                                    )
                                },
                                shape = RoundedCornerShape(28.dp)
                            ),
                        shape = RoundedCornerShape(28.dp),
                        color = Color.Transparent,
                        shadowElevation = inputElevation
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    if (inputFocused) {
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF2D2D44).copy(alpha = 0.95f),
                                                Color(0xFF252538).copy(alpha = 0.95f)
                                            )
                                        )
                                    } else {
                                        Brush.linearGradient(
                                            listOf(
                                                Color(0xFF2D2D44).copy(alpha = 0.8f),
                                                Color(0xFF252538).copy(alpha = 0.8f)
                                            )
                                        )
                                    }
                                )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 18.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = messageText,
                                    onValueChange = onMessageChange,
                                    modifier = Modifier.weight(1f),
                                    textStyle = LocalTextStyle.current.copy(
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        lineHeight = 20.sp
                                    ),
                                    maxLines = 5,
                                    decorationBox = { innerTextField ->
                                        if (messageText.isEmpty()) {
                                            Text(
                                                "Escribe un mensaje...",
                                                color = Color.Gray.copy(alpha = 0.6f),
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                )
                                
                                // Attachment button con animación de rotación
                                AnimatedVisibility(
                                    visible = messageText.isEmpty(),
                                    enter = scaleIn(initialScale = 0.7f) + fadeIn(),
                                    exit = scaleOut(targetScale = 0.7f) + fadeOut()
                                ) {
                                    val attachRotation by animateFloatAsState(
                                        targetValue = if (showAttachmentMenu) 45f else 0f,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                        label = "attach_rotation"
                                    )
                                    
                                    IconButton(
                                        onClick = {
                                            showAttachmentMenu = !showAttachmentMenu
                                            showEmojiPicker = false
                                            showStickerPicker = false
                                        },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .graphicsLayer { rotationZ = attachRotation }
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
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        else -> "${diff / 86400_000}d ago"
    }
}
