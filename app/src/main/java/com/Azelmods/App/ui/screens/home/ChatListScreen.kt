package com.Azelmods.App.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.components.safeClickable
import com.Azelmods.App.ui.theme.rememberThemeColor
import com.Azelmods.App.ui.theme.rememberThemeSecondaryColor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * ChatListScreen - Mod redesigned chat list
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    navController: NavController,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val themeColor = rememberThemeColor()
    val themeSecondaryColor = rememberThemeSecondaryColor()
    
    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    var showBottomSheet by remember { mutableStateOf(false) }
    
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            state.isLoading -> {
                // Loading state with shimmer
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(8) {
                        ShimmerChatItem()
                    }
                }
            }
            
            state.error != null -> {
                // Error state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = state.error ?: "Error desconocido",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { viewModel.loadChats() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = themeColor
                        )
                    ) {
                        Text("Reintentar")
                    }
                }
            }
            
            state.chats.isEmpty() -> {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.ChatBubbleOutline,
                        contentDescription = null,
                        tint = themeColor.copy(alpha = 0.5f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay conversaciones",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Inicia una nueva conversación",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            else -> {
                // Chat list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.chats,
                        key = { it.chatId }
                    ) { chat ->
                        ChatListItem(
                            chat = chat,
                            onClick = {
                                navController.navigate("chat/${chat.chatId}")
                            },
                            onLongPress = {
                                selectedChat = chat
                                showBottomSheet = true
                            },
                            themeColor = themeColor
                        )
                    }
                }
            }
        }
        
        // FAB with red gradient and glow
        val infiniteTransition = rememberInfiniteTransition(label = "fab_glow")
        val glowAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.7f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow"
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            // Glow effect
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(
                        elevation = 12.dp,
                        shape = CircleShape,
                        ambientColor = themeColor.copy(alpha = glowAlpha),
                        spotColor = themeColor.copy(alpha = glowAlpha)
                    )
            )
            
            FloatingActionButton(
                onClick = { navController.navigate("new_conversation") },
                modifier = Modifier.size(56.dp),
                containerColor = Color.Transparent,
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                listOf(themeColor, themeSecondaryColor)
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "New chat",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
    
    // Bottom sheet for long press actions
    if (showBottomSheet && selectedChat != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            containerColor = Color(0xFF0F0F0F),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .background(themeColor, RoundedCornerShape(2.dp))
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = selectedChat?.contactName ?: "",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Grid 2x4
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            icon = Icons.Default.PushPin,
                            label = "Fijar",
                            onClick = {
                                viewModel.pinChat(selectedChat!!.chatId)
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                        ActionButton(
                            icon = Icons.Default.VolumeOff,
                            label = "Silenciar",
                            onClick = {
                                viewModel.muteChat(selectedChat!!.chatId)
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            icon = Icons.Default.Archive,
                            label = "Archivar",
                            onClick = {
                                viewModel.archiveChat(selectedChat!!.chatId)
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                        ActionButton(
                            icon = Icons.Default.Delete,
                            label = "Eliminar",
                            onClick = {
                                viewModel.deleteChat(selectedChat!!.chatId)
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            themeColor = Color(0xFFEF4444)
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            icon = Icons.Default.DoneAll,
                            label = "Marcar leído",
                            onClick = {
                                viewModel.markAsRead(selectedChat!!.chatId)
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                        ActionButton(
                            icon = Icons.Default.ContentCopy,
                            label = "Copiar",
                            onClick = {
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ActionButton(
                            icon = Icons.Default.Block,
                            label = "Bloquear",
                            onClick = {
                                viewModel.blockChat(selectedChat!!.chatId)
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                        ActionButton(
                            icon = Icons.Default.CleaningServices,
                            label = "Limpiar",
                            onClick = {
                                viewModel.clearChat(selectedChat!!.chatId)
                                showBottomSheet = false
                            },
                            modifier = Modifier.weight(1f),
                            themeColor = themeColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatListItem(
    chat: Chat,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    themeColor: Color
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            },
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF111111)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar with online dot
            Box {
                UserAvatar(
                    name = chat.contactName,
                    photoUrl = chat.contactPhotoUrl,
                    size = 56.dp
                )
                
                if (chat.isOnline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .size(14.dp)
                            .border(2.dp, Color(0xFF111111), CircleShape)
                            .background(themeColor, CircleShape)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.contactName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (chat.isPinned) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = themeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    
                    if (chat.isMuted) {
                        Icon(
                            Icons.Default.VolumeOff,
                            contentDescription = "Muted",
                            tint = Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = chat.lastMessage,
                        fontSize = 14.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Text(
                        text = formatTimestamp(chat.lastMessageTimestamp),
                        fontSize = 12.sp,
                        color = themeColor
                    )
                }
            }
            
            // Unread badge
            if (chat.unreadCount > 0) {
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(themeColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun ShimmerChatItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF111111)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.Gray.copy(alpha = alpha))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .background(Color.Gray.copy(alpha = alpha), RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(14.dp)
                        .background(Color.Gray.copy(alpha = alpha * 0.7f), RoundedCornerShape(4.dp))
                )
            }
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    themeColor: Color
) {
    Surface(
        modifier = modifier
            .height(80.dp)
            .safeClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1A1A1A)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = themeColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.White
            )
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Ahora"
        diff < 3600_000 -> "${diff / 60_000}m"
        diff < 86400_000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        else -> SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

data class ChatListState(
    val chats: List<Chat> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val database: FirebaseDatabase
) : ViewModel() {
    
    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()
    
    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    init {
        loadChats()
    }
    
    fun loadChats() {
        if (currentUserId == null) return
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                withContext(Dispatchers.IO) {
                    // Load chats from Firebase
                    // This is a placeholder - implement actual Firebase logic
                    _state.value = _state.value.copy(
                        chats = emptyList(),
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar conversaciones"
                )
            }
        }
    }
    
    fun pinChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.reference
                    .child("chats")
                    .child(chatId)
                    .child("isPinned")
                    .setValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun muteChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.reference
                    .child("chats")
                    .child(chatId)
                    .child("isMuted")
                    .setValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun archiveChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.reference
                    .child("chats")
                    .child(chatId)
                    .child("isArchived")
                    .setValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun deleteChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.reference
                    .child("chats")
                    .child(chatId)
                    .removeValue()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun markAsRead(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.reference
                    .child("chats")
                    .child(chatId)
                    .child("unreadCount")
                    .setValue(0)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun blockChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.reference
                    .child("users")
                    .child(currentUserId!!)
                    .child("blocked")
                    .child(chatId)
                    .setValue(true)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    fun clearChat(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                database.reference
                    .child("messages")
                    .child(chatId)
                    .removeValue()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Remove Firebase listeners if any
    }
}
