package com.Azelmods.App.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.data.model.ChatType
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.components.UnifiedTopBar
import com.Azelmods.App.ui.components.TopBarActionIcon
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.DarkSurfaceVariant
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var searchVisible by remember { mutableStateOf(false) }

    // Obtener datos del usuario actual
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val currentUserName = remember { 
        currentUser?.displayName?.takeIf { it.isNotBlank() } 
            ?: currentUser?.email?.substringBefore("@")?.replaceFirstChar { it.uppercaseChar() }
            ?: "Anónimo"
    }
    val currentUserPhoto = remember { currentUser?.photoUrl?.toString() }
    val currentUserEmail = remember { currentUser?.email }

    Scaffold(
        topBar = {
            UnifiedTopBar(
                title = "NexusChat",
                userName = currentUserName,
                userPhotoUrl = currentUserPhoto,
                userSubtitle = currentUserEmail,
                onUserClick = {
                    // Navegar al perfil del usuario actual
                    navController.navigate(Screen.Profile.createRoute(currentUserId))
                },
                actions = {
                    // Toggle animated inline search bar
                    TopBarActionIcon(
                        icon = if (searchVisible) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = if (searchVisible) "Close search" else "Open search",
                        onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) viewModel.onSearchQueryChange("")
                        }
                    )
                    // Quick compose / new conversation
                    TopBarActionIcon(
                        icon = Icons.Default.Edit,
                        contentDescription = "New Conversation",
                        onClick = { navController.navigate(Screen.NewConversation.route) }
                    )
                },
                backgroundColor = DarkSurface,
                contentColor = Color.White
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.NewConversation.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        },
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0) // Edge-to-Edge: control manual de insets
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues) // Edge-to-Edge: consume los insets del Scaffold
                .navigationBarsPadding() // Edge-to-Edge: respeta barra de navegación
                .padding(horizontal = 16.dp) // Padding general de 16dp
        ) {

            // ── Animated inline search bar ─────────────────────────────────
            AnimatedVisibility(
                visible = searchVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1A1A2E))
                        .border(
                            1.dp, 
                            Color(0x22FFFFFF), 
                            RoundedCornerShape(16.dp)
                        )
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                text = "Buscar conversaciones...",
                                color = Color(0xFF55556A)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = Color(0xFF55556A),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (state.searchQuery.isNotBlank()) {
                                IconButton(
                                    onClick = { viewModel.onSearchQueryChange("") }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = Color(0xFF55556A)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
                            unfocusedLeadingIconColor = Color(0xFF55556A),
                            focusedPlaceholderColor = Color(0xFF55556A),
                            unfocusedPlaceholderColor = Color(0xFF55556A)
                        )
                    )
                }
            }

            // ── Filter chip row ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.selectedFilter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = {
                            Text(
                                text = filter.name
                                    .lowercase()
                                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // ── Main content ───────────────────────────────────────────────
            when {

                // Loading spinner
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                // Error state with retry
                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.error ?: "Something went wrong",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = { viewModel.refreshChats() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }

                // Illustrated empty state
                state.filteredChats.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No conversations yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap + to start chatting",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Populated chat list
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF070714), Color(0xFF0D0D1E))
                                )
                            ),
                        contentPadding = PaddingValues(top = 6.dp, bottom = 90.dp)
                    ) {
                        items(
                            items = state.filteredChats,
                            key = { chat -> chat.chatId }
                        ) { chat ->
                            ChatItem(
                                chat = chat,
                                currentUserId = currentUserId,
                                onChatClick = {
                                    navController.navigate(
                                        Screen.Chat.createRoute(chat.chatId)
                                    )
                                },
                                onTogglePin = { viewModel.togglePin(chat.chatId) },
                                onToggleMute = { viewModel.toggleMute(chat.chatId) },
                                onArchiveChat = { viewModel.archiveChat(chat.chatId) },
                                onDeleteChat = { viewModel.deleteChat(chat.chatId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── ChatItem — Muzli 2026 Design ─────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItem(
    chat: Chat,
    currentUserId: String,
    onChatClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onArchiveChat: () -> Unit,
    onDeleteChat: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    // Extraer datos del chat
    val otherUserName = chat.participantNames.values.firstOrNull() ?: "Anónimo"
    val otherPhotoUrl = chat.participantPhotos.values.firstOrNull()
    val unreadCount = chat.unreadCount[currentUserId] ?: 0
    val isPinned = chat.isPinned
    val isMuted = chat.isMuted
    val isOnline = chat.isOnline // presencia real del otro participante
    val isTyping = chat.isTyping.any { (uid, typing) -> uid != currentUserId && typing }
    val isLastMessageMine = chat.lastMessageSenderId == currentUserId
    val messageStatus = "read"

    // Spring physics interaction
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = spring(
            stiffness = 650f, 
            dampingRatio = 0.48f
        ),
        label = "press_scale"
    )

    // Color del acento izquierdo según estado
    val accentColor = when {
        isPinned -> Color(0xFFFFD700)          // Dorado = pinneado
        isOnline -> Color(0xFF00E676)          // Verde = online
        unreadCount > 0 -> MaterialTheme.colorScheme.primary   // Acento = no leído
        else -> Color.Transparent
    }

    // Fondo de la card
    val cardGradient = if (unreadCount > 0)
        Brush.linearGradient(
            listOf(Color(0xFF1A1535), Color(0xFF161628))
        )
    else
        Brush.linearGradient(
            listOf(Color(0xFF141424), Color(0xFF101020))
        )

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { 
                    scaleX = scale
                    scaleY = scale 
                }
                .padding(horizontal = 10.dp, vertical = 3.dp)
        ) {
            // ── CARD PRINCIPAL ───────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(cardGradient)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color(0x1FFFFFFF), Color(0x08FFFFFF))
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onChatClick,
                        onLongClick = { showMenu = true }
                    )
                    .padding(start = 0.dp, end = 14.dp, top = 11.dp, bottom = 11.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                // ── BARRA ACENTO IZQUIERDA ────────────────
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(accentColor, accentColor.copy(0.2f))
                            )
                        )
                )

                Spacer(Modifier.width(10.dp))

                // ── AVATAR CON ESTADO ─────────────────────
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(54.dp)
                ) {
                    // Anillo de gradiente animado (solo si online)
                    if (isOnline) {
                        val infinite = rememberInfiniteTransition(label = "ring")
                        val rotation by infinite.animateFloat(
                            initialValue = 0f, targetValue = 360f,
                            animationSpec = infiniteRepeatable(
                                tween(3000, easing = LinearEasing)
                            ),
                            label = "ring_rot"
                        )
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .rotate(rotation)
                                .clip(CircleShape)
                                .background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFF00E676),
                                            Color(0xFF00D4FF),
                                            Color(0xFF7C6FE0),
                                            Color(0xFF00E676)
                                        )
                                    )
                                )
                        )
                        // Separador interior
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF101020))
                        )
                    }

                    // Avatar del usuario
                    if (chat.chatType == ChatType.GROUP) {
                        Box(
                            modifier = Modifier
                                .size(if (isOnline) 47.dp else 52.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Group,
                                contentDescription = "Group Chat",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    } else {
                        UserAvatar(
                            name = otherUserName,
                            photoUrl = otherPhotoUrl,
                            size = if (isOnline) 47.dp else 52.dp
                        )
                    }

                    // Punto de estado (online)
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00E676))
                                .border(
                                    2.5.dp, 
                                    Color(0xFF101020), 
                                    CircleShape
                                )
                                .align(Alignment.BottomEnd)
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // ── CONTENIDO TEXTO ───────────────────────
                Column(modifier = Modifier.weight(1f)) {

                    // Fila superior: nombre + hora
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            // Icono pin si está fijado
                            if (isPinned) {
                                Text("📌", fontSize = 11.sp)
                            }
                            Text(
                                text = otherUserName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            // Icono silenciado junto al nombre
                            if (isMuted) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeOff,
                                    contentDescription = null,
                                    tint = Color(0xFF555575),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }

                        // Timestamp relativo
                        Text(
                            text = formatTimestamp(chat.lastMessageTime),
                            color = if (unreadCount > 0) Color(0xFF9C8FFF)
                            else Color(0xFF55556A),
                            fontSize = 11.sp,
                            fontWeight = if (unreadCount > 0) FontWeight.SemiBold
                            else FontWeight.Normal
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    // Fila inferior: preview mensaje + badge
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // ✓✓ estado si es mensaje mío
                            if (isLastMessageMine) {
                                val tickColor = when (messageStatus) {
                                    "read" -> Color(0xFF00D4FF)  // Cyan leído
                                    "delivered" -> Color(0xFF6A6A8A)  // Gris entregado
                                    else -> Color(0xFF444460)  // Más gris = enviado
                                }
                                Text(
                                    text = if (messageStatus == "sent") "✓" else "✓✓",
                                    color = tickColor,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Spacer(Modifier.width(3.dp))
                            }

                            // Typing animado O preview del mensaje
                            if (isTyping) {
                                TypingDots()
                            } else {
                                Text(
                                    text = chat.lastMessage.ifBlank { "No messages yet" },
                                    color = if (unreadCount > 0) Color(0xFFAAAAAC)
                                    else Color(0xFF55556A),
                                    fontSize = 13.sp,
                                    fontWeight = if (unreadCount > 0) FontWeight.Medium
                                    else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        // Badge no leídos con gradiente
                        if (unreadCount > 0) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 22.dp, minHeight = 22.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(MaterialTheme.colorScheme.primary, Color(0xFF00D4FF))
                                        )
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else "$unreadCount",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    lineHeight = 10.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // ── Long-press context menu ────────────────────────────────────────
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            DropdownMenuItem(
                text = { Text("Pin Chat", color = Color.White) },
                onClick = { showMenu = false; onTogglePin() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Mute", color = Color.White) },
                onClick = { showMenu = false; onToggleMute() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Archive", color = Color.White) },
                onClick = { showMenu = false; onArchiveChat() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            )
            HorizontalDivider(color = DarkSurfaceVariant)
            DropdownMenuItem(
                text = {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                },
                onClick = { showMenu = false; onDeleteChat() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

// ── TYPING DOTS ANIMADOS ─────────────────────────────────────────────────────

@Composable
fun TypingDots() {
    val infinite = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(0, 150, 300).forEachIndexed { i, delay ->
            val alpha by infinite.animateFloat(
                initialValue = 0.2f, targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    tween(600, delayMillis = delay),
                    RepeatMode.Reverse
                ),
                label = "dot_$i"
            )
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = alpha))
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "escribiendo...",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontStyle = FontStyle.Italic
        )
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Converts a Unix-epoch millisecond [timestamp] into a human-readable string:
 *   - < 1 min   → "Just now"
 *   - < 1 hour  → "Xm"
 *   - < 1 day   → "Xh"
 *   - yesterday → "Yesterday"
 *   - < 1 week  → "Mon" / "Tue" / …
 *   - older     → "Jan 5" / "Dec 12" / …
 */
private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L}m"
        diff < 86_400_000L -> "${diff / 3_600_000L}h"
        diff < 172_800_000L -> "Yesterday"
        diff < 604_800_000L ->
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))

        else ->
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
