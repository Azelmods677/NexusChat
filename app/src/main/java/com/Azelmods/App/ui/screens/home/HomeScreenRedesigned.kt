package com.Azelmods.App.ui.screens.home

import com.Azelmods.App.data.manager.AppBackgroundManager
import com.Azelmods.App.data.model.BackgroundType
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import com.Azelmods.App.ui.components.ReadReceiptIndicator
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
                    // Bloquear solo tiene sentido en chats de dos personas: en un
                    // grupo no hay un "otro" unico a quien bloquear.
                    if (chat.chatType != com.Azelmods.App.data.model.ChatType.GROUP) {
                        ChatActionItem(
                            icon = Icons.Default.Block,
                            text = "Bloquear / Desbloquear",
                            onClick = {
                                viewModel.toggleBlock(chat.chatId) { message ->
                                    android.widget.Toast
                                        .makeText(context, message, android.widget.Toast.LENGTH_LONG)
                                        .show()
                                }
                                showBottomSheet = false
                            }
                        )
                    }
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

@OptIn(ExperimentalFoundationApi::class)
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
    // Presencia REAL del contacto. Antes se calculaba como "alguien está escribiendo",
    // así que el punto verde de "en línea" solo aparecía mientras el otro tecleaba y
    // desaparecía al parar: no indicaba presencia en absoluto.
    val isOnline = chat.isOnline
    val isPeerTyping = chat.isTyping.any { (userId, typing) -> typing && userId != currentUserId }
    val unreadCount = chat.unreadCount[currentUserId] ?: 0
    val hasUnread = unreadCount > 0

    // Feedback táctil: la tarjeta se hunde ligeramente al pulsarla.
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "press_scale"
    )

    // NOTA: se retiró el AnimatedVisibility de entrada por fila. Al reciclarse los
    // items del LazyColumn la animación se re-disparaba en cada scroll y la lista
    // "parpadeaba". La entrada se anima ahora a nivel de lista, no por tarjeta.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .scale(pressScale)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 84.dp)
                // combinedClickable y no safeClickable: [onLongPress] se recibía como
                // parámetro pero NUNCA se conectaba, así que el menú de mantener pulsado
                // (fijar / silenciar / archivar / eliminar) era inalcanzable. De paso, el
                // interactionSource compartido alimenta la animación de pulsación.
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = ripple(),
                    onClick = onClick,
                    onLongClick = onLongPress
                ),
            shape = RoundedCornerShape(20.dp),
            // Translúcido a propósito: la Home deja ver el fondo/wallpaper del usuario.
            // Un color opaco aquí rompería esa transparencia.
            color = if (hasUnread) Color.Black.copy(alpha = 0.42f) else Color.Black.copy(alpha = 0.28f),
            border = BorderStroke(
                width = 1.dp,
                brush = if (hasUnread) {
                    Brush.linearGradient(listOf(themeColor.copy(alpha = 0.85f), themeSecondaryColor.copy(alpha = 0.55f)))
                } else {
                    Brush.linearGradient(listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.04f)))
                }
            ),
            shadowElevation = if (hasUnread) 6.dp else 2.dp
        ) {
            // fillMaxWidth y NO fillMaxSize: la tarjeta ya no tiene altura fija sino
            // mínima, así que pedir el alto máximo dejaría la fila a merced de las
            // constraints del contenedor en lugar de ajustarse al contenido.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // ── Avatar ────────────────────────────────────────────────────
                // El anillo de gradiente solo aparece cuando hay mensajes sin leer:
                // así el adorno significa algo en vez de ser decoración constante.
                Box(modifier = Modifier.size(56.dp)) {
                    if (hasUnread) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.sweepGradient(
                                        listOf(themeColor, Teal, Pink, themeColor)
                                    ),
                                    CircleShape
                                )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(if (hasUnread) 50.dp else 56.dp)
                            .align(Alignment.Center)
                            .safeClickable { onAvatarClick?.invoke() }
                    ) {
                        UserAvatar(
                            name = contactName,
                            photoUrl = contactPhotoUrl,
                            size = if (hasUnread) 50.dp else 56.dp
                        )
                    }

                    // Punto de presencia (verde = en línea de verdad).
                    if (isOnline) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(15.dp)
                                .background(Color.Black.copy(alpha = 0.75f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(EmeraldGreen, CircleShape)
                            )
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
                            if (chat.isPinned) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "Fijado",
                                    tint = themeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                            }
                            if (chat.isE2EE) {
                                Icon(
                                    Icons.Default.Lock,
                                    contentDescription = "Cifrado de extremo a extremo",
                                    tint = EmeraldGreen.copy(alpha = 0.9f),
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(Modifier.width(5.dp))
                            }
                            Text(
                                text = contactName,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (chat.isMuted) {
                                Spacer(Modifier.width(5.dp))
                                Icon(
                                    Icons.Default.NotificationsOff,
                                    contentDescription = "Silenciado",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = formatTimestamp(chat.lastMessageTime),
                            color = if (hasUnread) themeColor else Color.White.copy(alpha = 0.45f),
                            fontSize = 11.sp,
                            fontWeight = if (hasUnread) FontWeight.Bold else FontWeight.Normal
                        )
                    }

                    Spacer(modifier = Modifier.height(5.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isPeerTyping) {
                            // "Escribiendo…" en el idioma de la app (antes: "typing").
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "escribiendo",
                                    color = Teal,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                TypingDotsSmall()
                            }
                        } else {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Doble check real del design system en vez del texto
                                // "✓✓" en verde, que se pintaba SIEMPRE igual aunque el
                                // mensaje no estuviera entregado ni leído.
                                //
                                // "Leído" depende de los NO LEÍDOS DEL OTRO, no de los
                                // míos: si al destinatario no le queda nada pendiente,
                                // ha visto mi último mensaje.
                                if (chat.lastMessageSenderId == currentUserId) {
                                    val peerUnread = otherUserId?.let { chat.unreadCount[it] } ?: 0
                                    ReadReceiptIndicator(
                                        status = if (peerUnread > 0) MessageStatus.DELIVERED else MessageStatus.READ,
                                        modifier = Modifier.padding(end = 5.dp)
                                    )
                                }

                                Text(
                                    text = chat.lastMessage,
                                    color = if (hasUnread) Color.White.copy(alpha = 0.92f) else Color.White.copy(alpha = 0.5f),
                                    fontSize = 13.sp,
                                    fontWeight = if (hasUnread) FontWeight.Medium else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        // Contador de no leídos. La píldora se ensancha con el número
                        // (antes era un círculo fijo de 26.dp y "99+" se salía).
                        AnimatedVisibility(
                            visible = hasUnread,
                            enter = scaleIn() + fadeIn(),
                            exit = scaleOut() + fadeOut()
                        ) {
                            Box(
                                modifier = Modifier
                                    .heightIn(min = 22.dp)
                                    .widthIn(min = 22.dp)
                                    .background(
                                        Brush.linearGradient(listOf(themeColor, themeSecondaryColor)),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
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
    
    // Mismas métricas que ChatRow (radio 20, margen 12) para que la lista se lea
    // como un único sistema y no como dos componentes de apps distintas.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
    ) {
        // Static gradient border (no rotation animation to avoid recomposition)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
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
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(84.dp)
                .safeClickable(onClick = onClick),
            shape = RoundedCornerShape(20.dp),
            color = Color.Black.copy(alpha = 0.32f),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Robot avatar with gradient
                Box(
                    modifier = Modifier.size(56.dp),
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
                        "Azel Assistant",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(4.dp))

                    Text(
                        "Escríbeme y te enseño la app",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                // DEMO badge
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = NeonGreen.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, NeonGreen.copy(alpha = 0.7f))
                ) {
                    Text(
                        "DEMO",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        color = NeonGreen,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
