package com.Azelmods.App.ui.screens.home

import com.Azelmods.App.i18n.tr

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ripple
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import com.Azelmods.App.ui.components.AvatarRing
import com.Azelmods.App.ui.components.ProfileColors
import com.Azelmods.App.ui.components.pressScale
import com.Azelmods.App.ui.components.staggeredEnter
import kotlinx.coroutines.delay
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.chat.ChatId
import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.data.model.ChatSettings
import com.Azelmods.App.data.model.ChatType
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.ui.components.NexusCountBadge
import com.Azelmods.App.ui.components.NexusDivider
import com.Azelmods.App.ui.components.NexusEmptyState
import com.Azelmods.App.ui.components.NexusFilterChip
import com.Azelmods.App.ui.components.ReadReceiptIndicator
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenRedesigned(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var selectedChat by remember { mutableStateOf<Chat?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMuteOptions by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val storyOwners by viewModel.storyOwners.collectAsState()
    val ghostMode by viewModel.ghostModeEnabled.collectAsState()
    val avatarRings = remember { com.Azelmods.App.data.preferences.ThemePreferences(context).areAvatarRingsEnabled() }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    fun toast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        tr("Chats"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = NexusTokens.Color.TextPrimary
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(Icons.Default.Search, "Buscar", tint = NexusTokens.Color.TextSecondary)
                    }
                    IconButton(onClick = { navController.navigate(Screen.AzelAI.route) }) {
                        Icon(Icons.Outlined.SmartToy, "Azel IA", tint = NexusTokens.Color.TextSecondary)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Más opciones", tint = NexusTokens.Color.TextSecondary)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            containerColor = NexusTokens.Color.BgElevated
                        ) {
                            HomeMenuItem(
                                if (ghostMode) tr("Desactivar modo fantasma") else tr("Activar modo fantasma"),
                                if (ghostMode) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility
                            ) {
                                showMenu = false
                                viewModel.toggleGhostMode()
                                toast(if (ghostMode) tr("Modo fantasma desactivado") else tr("Modo fantasma activado"))
                            }
                            HomeMenuItem("Seguridad y Tor", Icons.Outlined.Lock) {
                                showMenu = false
                                navController.navigate(Screen.Security.route)
                            }
                            HomeMenuItem("Editor de código", Icons.Outlined.Code) {
                                showMenu = false
                                navController.navigate("code_editor")
                            }
                            HomeMenuItem("Terminal", Icons.Outlined.Terminal) {
                                showMenu = false
                                navController.navigate("terminal")
                            }
                            HomeMenuItem("Ajustes", Icons.Outlined.Settings) {
                                showMenu = false
                                navController.navigate(Screen.Settings.route)
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.NewConversation.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(2.dp, 2.dp, 2.dp, 2.dp)
            ) {
                Icon(Icons.Default.Edit, "Nueva conversación")
            }
        },
        containerColor = Color.Transparent
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Filtros: la fila desplaza en horizontal para que "Archivados" siempre
            // sea alcanzable; es el único camino de vuelta de un chat archivado.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatFilter.values().forEach { filter ->
                    val count = when (filter) {
                        ChatFilter.UNREAD -> state.chats.count { (it.unreadCount[currentUserId] ?: 0) > 0 && !it.isArchived }
                        ChatFilter.ARCHIVED -> state.chats.count { it.isArchived }
                        else -> 0
                    }
                    NexusFilterChip(
                        label = when (filter) {
                            ChatFilter.ALL -> tr("Todos")
                            ChatFilter.UNREAD -> tr("No leídos")
                            ChatFilter.GROUPS -> tr("Grupos")
                            ChatFilter.ARCHIVED -> tr("Archivados")
                        },
                        selected = state.selectedFilter == filter,
                        count = count,
                        onClick = { viewModel.onFilterChange(filter) }
                    )
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(28.dp)
                    )
                }

                state.filteredChats.isEmpty() -> EmptyChatsState(
                    filter = state.selectedFilter,
                    onStartConversation = { navController.navigate(Screen.NewConversation.route) },
                    onOpenAssistant = {
                        if (currentUserId.isNotEmpty()) {
                            val chatId = ChatId.create(currentUserId, "demo_azel_assistant")
                            navController.navigate(Screen.Chat.createRoute(chatId))
                        }
                    }
                )

                else -> {
                    // La entrada escalonada solo se ve al abrir: al desplazarse las filas
                    // deben estar ya en su sitio.
                    var listEntered by rememberSaveable { mutableStateOf(false) }
                    LaunchedEffect(Unit) {
                        delay(700)
                        listEntered = true
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        itemsIndexed(items = state.filteredChats, key = { _, chat -> chat.chatId }) { index, chat ->
                            val contactId = chat.participantIds.find { it != currentUserId }
                            val hasStory = contactId != null && contactId in storyOwners
                            Column(
                                Modifier
                                    .animateItem()
                                    .staggeredEnter(index, enabled = !listEntered)
                            ) {
                                ChatRow(
                                    chat = chat,
                                    hasStory = hasStory,
                                    colorRing = avatarRings,
                                    onClick = { navController.navigate("chat/${chat.chatId}") },
                                    onLongPress = {
                                        selectedChat = chat
                                        showBottomSheet = true
                                    },
                                    onAvatarClick = {
                                        if (contactId != null) {
                                            navController.navigate(
                                                if (hasStory) "story_viewer/$contactId" else "profile_viewer/$contactId"
                                            )
                                        }
                                    }
                                )
                                NexusDivider(startInset = 82.dp)
                            }
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            }
        }
    }

    // Acciones al mantener pulsado un chat
    selectedChat?.let { chat ->
        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                containerColor = NexusTokens.Color.BgSurface,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    ChatActionItem(
                        icon = Icons.Default.PushPin,
                        text = if (chat.isPinned) tr("Dejar de fijar") else tr("Fijar arriba"),
                        onClick = {
                            viewModel.togglePin(chat.chatId)
                            showBottomSheet = false
                        }
                    )
                    ChatActionItem(
                        icon = if (chat.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        text = if (chat.isMuted) tr("Reactivar notificaciones") else tr("Silenciar"),
                        onClick = {
                            showBottomSheet = false
                            if (chat.isMuted) viewModel.toggleMute(chat.chatId) { toast(it) }
                            else showMuteOptions = true
                        }
                    )
                    ChatActionItem(
                        icon = if (chat.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        text = if (chat.isArchived) tr("Sacar de archivados") else tr("Archivar"),
                        onClick = {
                            viewModel.toggleArchive(chat.chatId) { toast(it) }
                            showBottomSheet = false
                        }
                    )
                    if (chat.chatType != ChatType.GROUP) {
                        ChatActionItem(
                            icon = Icons.Default.Block,
                            text = tr("Bloquear o desbloquear"),
                            onClick = {
                                viewModel.toggleBlock(chat.chatId) { toast(it) }
                                showBottomSheet = false
                            }
                        )
                    }
                    ChatActionItem(
                        icon = Icons.Default.CleaningServices,
                        text = tr("Vaciar chat"),
                        onClick = {
                            viewModel.clearChat(chat.chatId) { toast(it) }
                            showBottomSheet = false
                        }
                    )
                    ChatActionItem(
                        icon = Icons.Default.Delete,
                        text = tr("Eliminar conversación"),
                        textColor = NexusTokens.Color.Error,
                        onClick = {
                            showBottomSheet = false
                            showDeleteConfirm = true
                        }
                    )
                }
            }
        }
    }

    selectedChat?.let { chat ->
        if (showMuteOptions) {
            val opciones = listOf(
                "1 hora" to ChatSettings.MUTE_1_HOUR,
                "8 horas" to ChatSettings.MUTE_8_HOURS,
                "1 semana" to ChatSettings.MUTE_1_WEEK,
                "Hasta que lo reactive" to ChatSettings.MUTE_ALWAYS
            )
            AlertDialog(
                onDismissRequest = { showMuteOptions = false },
                title = { Text(tr("Silenciar conversación")) },
                text = {
                    Column {
                        Text(
                            tr("Dejarás de recibir notificaciones de este chat. Las llamadas seguirán entrando."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexusTokens.Color.TextSecondary
                        )
                        Spacer(Modifier.height(8.dp))
                        opciones.forEach { (etiqueta, duracion) ->
                            Text(
                                text = etiqueta,
                                style = MaterialTheme.typography.bodyLarge,
                                color = NexusTokens.Color.TextPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showMuteOptions = false
                                        viewModel.toggleMute(chat.chatId, duracion) { toast(it) }
                                    }
                                    .padding(vertical = 12.dp)
                            )
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showMuteOptions = false }) { Text(tr("Cancelar")) }
                },
                containerColor = NexusTokens.Color.BgSurface
            )
        }
    }

    selectedChat?.let { chat ->
        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(tr("¿Eliminar la conversación?")) },
                text = {
                    Text(
                        tr("Se borrarán todos los mensajes para los dos participantes. " +
                            "No se puede deshacer.\n\n" +
                            "Si solo quieres limpiar el historial, usa «Vaciar chat»."),
                        color = NexusTokens.Color.TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        viewModel.deleteChat(chat.chatId) { toast(it) }
                    }) {
                        Text(tr("Eliminar"), color = NexusTokens.Color.Error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) { Text(tr("Cancelar")) }
                },
                containerColor = NexusTokens.Color.BgSurface
            )
        }
    }
}

@Composable
private fun HomeMenuItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = { Text(label, color = NexusTokens.Color.TextPrimary) },
        leadingIcon = { Icon(icon, null, tint = NexusTokens.Color.TextSecondary) },
        onClick = onClick
    )
}

/** Fila de conversación: plana, a todo el ancho, con el acento solo en lo que está sin leer. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatRow(
    chat: Chat,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    hasStory: Boolean = false,
    colorRing: Boolean = true
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val otherUserId = chat.participantIds.ifEmpty { chat.participants }
        .firstOrNull { it != currentUserId }
    val contactName = otherUserId?.let { chat.participantNames[it] }
        ?: chat.contactName.ifBlank { null }
        ?: chat.participantNames.values.firstOrNull()
        ?: tr("Sin nombre")
    val contactPhotoUrl = otherUserId?.let { chat.participantPhotos[it] } ?: chat.contactPhotoUrl
    val isPeerTyping = chat.isTyping.any { (userId, typing) -> typing && userId != currentUserId }
    val unreadCount = chat.unreadCount[currentUserId] ?: 0
    val hasUnread = unreadCount > 0
    val interaction = remember { MutableInteractionSource() }
    val ringColors = remember(otherUserId, contactName, chat.contactColor) {
        ProfileColors.forUser(otherUserId ?: contactName, chat.contactColor)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interaction, pressedScale = 0.985f)
            .combinedClickable(
                interactionSource = interaction,
                indication = ripple(),
                onClick = onClick,
                onLongClick = onLongPress
            )
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(54.dp)) {
            UserAvatar(
                name = contactName,
                photoUrl = contactPhotoUrl,
                size = 54.dp,
                ring = when {
                    // Historia activa: el aro gira, como aviso de que hay algo nuevo que ver.
                    hasStory -> AvatarRing(ringColors, width = 2.5.dp, rotating = true)
                    colorRing -> AvatarRing(ringColors, width = 1.5.dp)
                    else -> null
                },
                modifier = Modifier.clickable(enabled = onAvatarClick != null) { onAvatarClick?.invoke() }
            )
            // Presencia real: solo aparece si el contacto está conectado.
            if (chat.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .background(NexusTokens.Color.BgBase, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(9.dp).background(NexusTokens.Color.Online, CircleShape))
                }
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // El nombre y el icono de silencio ocupan el hueco: así la hora queda
                // siempre alineada a la derecha, sea cual sea la longitud del nombre.
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = contactName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (hasUnread) FontWeight.SemiBold else FontWeight.Medium,
                        color = NexusTokens.Color.TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (chat.isMuted) {
                        Spacer(Modifier.width(5.dp))
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = tr("Silenciado"),
                            tint = NexusTokens.Color.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = formatTimestamp(chat.lastMessageTime),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (hasUnread) MaterialTheme.colorScheme.primary else NexusTokens.Color.TextMuted
                )
            }

            // Sin último mensaje (chat vacío o vaciado) no se reserva una línea en blanco:
            // el nombre queda centrado junto al avatar.
            val showSecondLine = isPeerTyping || chat.lastMessage.isNotBlank() || chat.isPinned || hasUnread
            if (showSecondLine) Spacer(Modifier.height(3.dp))

            if (showSecondLine) Row(verticalAlignment = Alignment.CenterVertically) {
                if (isPeerTyping) {
                    Text(
                        text = tr("escribiendo"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    TypingDotsSmall()
                    Spacer(Modifier.weight(1f))
                } else {
                    if (chat.lastMessageSenderId == currentUserId) {
                        val peerUnread = otherUserId?.let { chat.unreadCount[it] } ?: 0
                        ReadReceiptIndicator(
                            status = if (peerUnread > 0) MessageStatus.DELIVERED else MessageStatus.READ,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                    }
                    Text(
                        text = chat.lastMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (hasUnread) NexusTokens.Color.TextSecondary else NexusTokens.Color.TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (chat.isPinned) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Default.PushPin,
                        contentDescription = tr("Fijado"),
                        tint = NexusTokens.Color.TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
                if (hasUnread) {
                    Spacer(Modifier.width(8.dp))
                    NexusCountBadge(count = unreadCount, muted = chat.isMuted)
                }
            }
        }
    }
}

/** Indicador de escritura: animación con sentido, comunica un estado en curso. */
@Composable
fun TypingDotsSmall() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.padding(start = 5.dp)
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, delayMillis = index * 180),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dot$index"
            )
            Box(
                Modifier
                    .size(4.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}

@Composable
fun EmptyChatsState(
    filter: ChatFilter,
    onStartConversation: () -> Unit,
    onOpenAssistant: () -> Unit
) {
    when (filter) {
        ChatFilter.ALL -> Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NexusEmptyState(
                icon = Icons.AutoMirrored.Filled.Chat,
                title = tr("Aún no tienes conversaciones"),
                body = "Busca a alguien por su usuario o escanea su código QR para empezar.",
                actionLabel = tr("Nueva conversación"),
                onAction = onStartConversation,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = onOpenAssistant, modifier = Modifier.padding(bottom = 96.dp)) {
                Text(tr("Hablar con el asistente de bienvenida"))
            }
        }
        ChatFilter.UNREAD -> NexusEmptyState(Icons.Default.MarkChatRead, "Estás al día", body = "No tienes mensajes sin leer.")
        ChatFilter.GROUPS -> NexusEmptyState(Icons.Default.Group, "Sin grupos", body = "Crea uno desde «Nueva conversación».")
        ChatFilter.ARCHIVED -> NexusEmptyState(Icons.Default.Archive, "Sin archivados", body = "Mantén pulsado un chat para archivarlo.")
    }
}

@Composable
fun ChatActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    textColor: Color = NexusTokens.Color.TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (textColor == NexusTokens.Color.TextPrimary) NexusTokens.Color.TextSecondary else textColor,
            modifier = Modifier.size(NexusTokens.IconSize.md)
        )
        Spacer(Modifier.width(20.dp))
        Text(text = text, color = textColor, style = MaterialTheme.typography.bodyLarge)
    }
}

/**
 * Hora en la lista de chats: hoy la hora, ayer "Ayer", esta semana el día, y si no la
 * fecha. Es lo que el usuario espera de una app de mensajería, en vez de "3h" o "12d".
 */
private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""
    val locale = Locale.forLanguageTag("es")
    val date = Date(timestamp)
    val then = Calendar.getInstance().apply { time = date }
    val now = Calendar.getInstance()
    val sameYear = then.get(Calendar.YEAR) == now.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    return when {
        sameYear && dayDiff == 0 -> SimpleDateFormat("HH:mm", locale).format(date)
        sameYear && dayDiff == 1 -> tr("Ayer")
        sameYear && dayDiff in 2..6 ->
            SimpleDateFormat("EEE", locale).format(date).replaceFirstChar { it.uppercase() }
        sameYear -> SimpleDateFormat("d MMM", locale).format(date)
        else -> SimpleDateFormat("dd/MM/yy", locale).format(date)
    }
}
