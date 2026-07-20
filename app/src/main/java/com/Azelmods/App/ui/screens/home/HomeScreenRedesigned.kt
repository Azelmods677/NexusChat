package com.Azelmods.App.ui.screens.home

import com.Azelmods.App.data.manager.AppBackgroundManager
import com.Azelmods.App.data.model.BackgroundType
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.data.chat.ChatId
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.components.safeClickable
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.theme.rememberThemeColor
import com.Azelmods.App.ui.theme.rememberThemeSecondaryColor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.Azelmods.App.ui.components.VideoBackgroundPlayer
import com.Azelmods.App.ui.theme.parseHexColor
import com.Azelmods.App.ui.theme.linearGradientBrush
import com.Azelmods.App.ui.theme.Pink
import com.Azelmods.App.ui.theme.Teal
import com.Azelmods.App.ui.theme.PurpleBright
import com.Azelmods.App.ui.theme.CyanAccent
import com.Azelmods.App.ui.theme.EmeraldGreen
import com.Azelmods.App.ui.theme.NeonGreen
import com.Azelmods.App.ui.theme.ErrorRed
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.RosePink

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenRedesigned(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val backgroundConfig by viewModel.backgroundConfig.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    val themeColor = rememberThemeColor()
    val themeSecondaryColor = rememberThemeSecondaryColor()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Nexus Chat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = {
                        try {
                            navController.navigate("search")
                        } catch (e: Exception) { }
                    }) {
                        Icon(Icons.Default.Search, "Search", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent // Transparent to see background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    try {
                        navController.navigate("new_conversation")
                    } catch (e: Exception) { }
                },
                containerColor = themeColor,
                contentColor = Color.White,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(Icons.Default.Edit, "New Chat")
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Chat")
            }
        },
        containerColor = Color.Transparent // Transparent to see background
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Filter chips - IMPROVED VERSION
            // Filter chips - IMPROVED VERSION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ChatFilter.values().forEach { filter ->
                    val isSelected = state.selectedFilter == filter
                    
                    Surface(
                        modifier = Modifier
                            .height(42.dp)
                            .safeClickable { viewModel.onFilterChange(filter) },
                        shape = RoundedCornerShape(21.dp),
                        color = if (isSelected) Color.Transparent else Color.Black.copy(alpha = 0.3f)
                    ) {
                        Box(
                            modifier = if (isSelected) {
                                Modifier.background(
                                    Brush.linearGradient(
                                        listOf(
                                            themeColor,
                                            themeSecondaryColor
                                        )
                                    )
                                )
                            } else {
                                Modifier
                            },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Icon for each filter
                                val icon = when (filter) {
                                    ChatFilter.ALL -> Icons.AutoMirrored.Filled.Chat
                                    ChatFilter.UNREAD -> Icons.Default.MarkChatUnread
                                    ChatFilter.GROUPS -> Icons.Default.Group
                                    ChatFilter.ARCHIVED -> Icons.Default.Archive
                                }
                                
                                AnimatedVisibility(
                                    visible = isSelected,
                                    enter = scaleIn() + fadeIn(),
                                    exit = scaleOut() + fadeOut()
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                
                                Text(
                                    text = filter.name,
                                    color = if (isSelected) Color.White else Color.Gray,
                                    fontSize = 15.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                                
                                // Badge count for unread
                                if (filter == ChatFilter.UNREAD && !isSelected) {
                                    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                    val unreadCount = state.filteredChats.count { chat ->
                                        (chat.unreadCount[currentUserId] ?: 0) > 0
                                    }
                                    
                                    if (unreadCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = themeColor,
                                            modifier = Modifier.size(20.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
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
            
            // Chat list
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = themeColor)
                    }
                }
                
                state.filteredChats.isEmpty() -> {
                    EmptyChatsState(
                        onStartConversation = {
                            try {
                                navController.navigate("new_conversation")
                            } catch (e: Exception) { }
                        },
                        themeColor = themeColor
                    )
                }
                
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // ═══ DEMO CHAT - ALWAYS FIRST ═══
                        item {
                            DemoChatCard(
                                onClick = {
                                    try {
                                        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                                        if (currentUid == null) {
                                            android.util.Log.e("HomeScreen", "Cannot open demo chat: user not authenticated")
                                            android.widget.Toast.makeText(
                                                context,
                                                "Inicia sesión para abrir el Demo Chat",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        } else {
                                            // ChatId canónico y consistente con NewConversationViewModel.createDemoChat
                                            val chatId = ChatId.create(currentUid, "demo_azel_assistant")
                                            navController.navigate(Screen.Chat.createRoute(chatId))
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("HomeScreen", "Error opening demo chat: ${e.message}", e)
                                        android.widget.Toast.makeText(
                                            context,
                                            "No se pudo abrir el Demo Chat. Intenta de nuevo.",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                themeColor = themeColor
                            )
                        }
                        
                        items(
                            items = state.filteredChats,
                            key = { it.chatId }
                        ) { chat ->
                            ChatRow(
                                chat = chat,
                        onClick = {
                            try {
                                navController.navigate("chat/${chat.chatId}")
                            } catch (e: Exception) { }
                        },
                                onLongPress = {
                                    selectedChat = chat
                                    showBottomSheet = true
                                },
                                onAvatarClick = {
                                    try {
                                        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                        val contactId = chat.participantIds.find { 
                                            it != currentUserId 
                                        } ?: return@ChatRow
                                        navController.navigate("profile_viewer/$contactId")
                                    } catch (e: Exception) { }
                                },
                                themeColor = themeColor,
                                themeSecondaryColor = themeSecondaryColor
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Bottom sheet for long press actions
    selectedChat?.let { chat ->
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                containerColor = DarkSurface.copy(alpha = 0.95f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    ChatActionItem(
                        icon = Icons.Default.PushPin,
                        text = if (chat.isPinned) "Unpin Chat" else "Pin Chat",
                        onClick = {
                            viewModel.togglePin(chat.chatId)
                            showBottomSheet = false
                        }
                    )
                    ChatActionItem(
                        icon = Icons.Default.NotificationsOff,
                        text = if (chat.isMuted) "Unmute" else "Mute",
                        onClick = {
                            viewModel.toggleMute(chat.chatId)
                            showBottomSheet = false
                        }
                    )
                    ChatActionItem(
                        icon = Icons.Default.Archive,
                        text = "Archive",
                        onClick = {
                            viewModel.archiveChat(chat.chatId)
                            showBottomSheet = false
                        }
                    )
                    ChatActionItem(
                        icon = Icons.Default.Delete,
                        text = "Delete",
                        textColor = ErrorRed,
                        onClick = {
                            viewModel.deleteChat(chat.chatId)
                            showBottomSheet = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ChatRow(
    chat: Chat,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    themeColor: Color = MaterialTheme.colorScheme.primary,
    themeSecondaryColor: Color = MaterialTheme.colorScheme.secondary
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val otherUserId = chat.participantIds.ifEmpty { chat.participants }
        .firstOrNull { it != currentUserId }
    val contactName = otherUserId?.let { chat.participantNames[it] }
        ?: chat.contactName.ifBlank { null }
        ?: chat.participantNames.values.firstOrNull()
        ?: "Anónimo"
    val contactPhotoUrl = otherUserId?.let { chat.participantPhotos[it] }
        ?: chat.contactPhotoUrl
    val isOnline = chat.isTyping.any { (userId, typing) -> typing && userId != currentUserId }
    val unreadCount = chat.unreadCount[currentUserId] ?: 0
    
    // Animated entry
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
    }
    
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { -it } + fadeIn()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .safeClickable(onClick = onClick),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar with static gradient ring (no per-item animation)
                Box {
                    if (unreadCount > 0) {
                        Box(
                            modifier = Modifier
                                .size(62.dp)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            themeColor,
                                            Teal,
                                            Pink,
                                            themeColor
                                        )
                                    ),
                                    CircleShape
                                )
                        )
                    }
                    
                    // Inner background
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.2f), CircleShape)
                    )
                    
                    // Avatar with photo support
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .align(Alignment.Center)
                            .safeClickable {
                                onAvatarClick?.invoke()
                            }
                    ) {
                        UserAvatar(
                            name = contactName,
                            photoUrl = contactPhotoUrl,
                            size = 54.dp
                        )
                    }
                    
                    // Online indicator with pulse animation
                    if (isOnline) {
                        val onlineTransition = rememberInfiniteTransition(label = "online_${chat.chatId}")
                        val scale by onlineTransition.animateFloat(
                            initialValue = 1.0f,
                            targetValue = 1.2f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "pulse"
                        )
                        
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .offset(x = (-2).dp, y = (-2).dp)
                        ) {
                            // Glow effect
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .scale(scale)
                                    .background(
                                        EmeraldGreen.copy(alpha = 0.3f),
                                        CircleShape
                                    )
                            )
                            // Solid indicator
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .align(Alignment.Center)
                                    .background(EmeraldGreen, CircleShape)
                                    .padding(2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black, CircleShape)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                // Chat info with glassmorphism effect
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (chat.isE2EE) {
                                Text("🔒", fontSize = 14.sp)
                                Spacer(Modifier.width(4.dp))
                            }
                            if (chat.isMuted) {
                                Icon(
                                    Icons.Default.NotificationsOff,
                                    contentDescription = "Silenciado",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            Text(
                                text = contactName,
                                color = Color.White,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Time with better styling
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (unreadCount > 0) 
                                themeColor.copy(alpha = 0.2f) 
                            else 
                                Color.Transparent
                        ) {
                            Text(
                                text = formatTimestamp(chat.lastMessageTime),
                                color = if (unreadCount > 0) themeColor else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = if (unreadCount > 0) FontWeight.Bold else FontWeight.Normal,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (chat.isTyping.values.any { it }) {
                            // Typing indicator with animation
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Teal.copy(alpha = 0.15f)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "typing",
                                        color = Teal,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    TypingDotsSmall()
                                }
                            }
                        } else {
                            // Message preview with status icon
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (chat.lastMessageSenderId == currentUserId) {
                                    Text(
                                        text = "✓✓",
                                        color = Teal,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                
                                Text(
                                    text = chat.lastMessage,
                                    color = if (unreadCount > 0) Color.White else Color.Gray,
                                    fontSize = 14.sp,
                                    fontWeight = if (unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Unread badge with animation
                        AnimatedVisibility(
                            visible = unreadCount > 0,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Transparent,
                                modifier = Modifier.size(26.dp)
                            ) {
                                Box(
                                    modifier = Modifier.background(
                                        Brush.linearGradient(
                                            listOf(
                                                themeColor,
                                                themeSecondaryColor
                                            )
                                        )
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
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
fun TypingDotsSmall() {
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
                color = Teal.copy(alpha = alpha),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun EmptyChatsState(
    onStartConversation: () -> Unit,
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Chat,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.Gray
            )
            
            Text(
                text = "No conversations yet",
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Start chatting with your contacts",
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            Button(
                onClick = onStartConversation,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor
                )
            ) {
                Text("Start a conversation")
            }
        }
    }
}

@Composable
fun ChatActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: Color = Color.White,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
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
                fontSize = 16.sp
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "now"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> "${diff / 3600_000}h"
        diff < 172800_000 -> "Yesterday"
        else -> {
            val date = Date(timestamp)
            SimpleDateFormat("MM/dd", Locale.getDefault()).format(date)
        }
    }
}

@Composable
fun DemoChatCard(
    onClick: () -> Unit,
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    val animatedTransition = rememberInfiniteTransition(label = "demo_card")
    val borderGlow by animatedTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "border_glow"
    )
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Static gradient border (no rotation animation to avoid recomposition)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
        ) {
            drawRoundRect(
                brush = Brush.sweepGradient(
                    listOf(
                        PurpleBright.copy(alpha = borderGlow * 0.7f),
                        CyanAccent.copy(alpha = borderGlow * 0.4f),
                        RosePink.copy(alpha = borderGlow * 0.7f),
                        PurpleBright.copy(alpha = borderGlow * 0.7f)
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                ),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.dp.toPx())
            )
        }
        
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .safeClickable(onClick = onClick),
            shape = RoundedCornerShape(16.dp),
            color = Color.Black.copy(alpha = 0.3f),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Robot avatar with gradient
                Box(
                    modifier = Modifier.size(54.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        PurpleBright,
                                        CyanAccent
                                    )
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "🤖",
                            fontSize = 28.sp
                        )
                    }
                }
                
                Spacer(Modifier.width(14.dp))
                
                // Chat info
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Demo Chat",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(Modifier.height(4.dp))
                    
                    Text(
                        "Prueba el chat sin contactos",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                
                // DEMO badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonGreen.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, NeonGreen)
                ) {
                    Text(
                        "DEMO",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = NeonGreen,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
