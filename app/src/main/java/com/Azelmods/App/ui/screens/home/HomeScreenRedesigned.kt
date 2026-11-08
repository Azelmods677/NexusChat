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
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.GroupAdd
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Security
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
import androidx.compose.ui.draw.clip
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
    viewModel: HomeViewModel = hiltViewModel(),
    onOpenTab: (Int) -> Unit = {}
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

    val me = com.Azelmods.App.data.profile.rememberMyProfile()
    val folders by com.Azelmods.App.data.chat.ChatOrganizer.folders.collectAsState()
    val hiddenChats by com.Azelmods.App.data.chat.ChatOrganizer.hidden.collectAsState()
    var folderId by rememberSaveable { mutableStateOf<String?>(null) }
    var showHidden by remember { mutableStateOf(false) }
    var folderChat by remember { mutableStateOf<Chat?>(null) }
    var joinCode by remember { mutableStateOf<String?>(null) }
    val pendingInvite by com.Azelmods.App.ui.screens.group.GroupLinks.pending.collectAsState()
    LaunchedEffect(pendingInvite) {
        pendingInvite?.let {
            joinCode = it
            com.Azelmods.App.ui.screens.group.GroupLinks.pending.value = null
        }
    }
    // Al salir de la app los chats ocultos vuelven a esconderse.
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_STOP) showHidden = false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val activeFolder = folders.firstOrNull { it.id == folderId }
    val visibleChats = remember(state.filteredChats, hiddenChats, activeFolder, showHidden) {
        state.filteredChats.filter { chat ->
            when {
                showHidden -> chat.chatId in hiddenChats
                chat.chatId in hiddenChats -> false
                activeFolder != null -> chat.chatId in activeFolder.chatIds
                else -> true
            }
        }
    }
    val homeStyle by com.Azelmods.App.ui.components.ChatLook.homeStyle.collectAsState()

    fun toast(message: String) {
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val showAvatar by com.Azelmods.App.ui.components.ChatLook.homeAvatar.collectAsState()
                    HomeTitle(homeStyle, me, showAvatar) {
                        navController.navigate(Screen.Profile.createRoute(currentUserId))
                    }
                },
                actions = {
                    val ghostButton by com.Azelmods.App.ui.components.ChatLook.ghostButton.collectAsState()
                    if (ghostButton) {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { viewModel.toggleGhostMode() }) {
                            Icon(
                                if (ghostMode) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                tr("Modo fantasma"),
                                tint = if (ghostMode) MaterialTheme.colorScheme.primary else NexusTokens.Color.TextSecondary
                            )
                        }
                    }
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.navigate(Screen.Search.route) }) {
                        Icon(Icons.Default.Search, "Buscar", tint = NexusTokens.Color.TextSecondary)
                    }
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.navigate(Screen.AzelAI.route) }) {
                        Icon(Icons.Outlined.SmartToy, "Azel IA", tint = NexusTokens.Color.TextSecondary)
                    }
                    Box {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { showMenu = true }) {
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
                            HomeMenuItem(tr("Unirse a un grupo"), Icons.Outlined.GroupAdd) {
                                showMenu = false
                                joinCode = ""
                            }
                            HomeMenuItem(tr("Chats ocultos"), Icons.Outlined.VisibilityOff) {
                                showMenu = false
                                if (hiddenChats.isEmpty()) {
                                    toast(tr("No tienes chats ocultos. Mantén pulsado un chat para ocultarlo."))
                                } else {
                                    confirmDeviceOwner(context, tr("Ver chats ocultos"), onNoLock = {
                                        toast(tr("Pon un bloqueo de pantalla en el teléfono para usar los chats ocultos"))
                                    }) { showHidden = true }
                                }
                            }
                            HomeMenuItem(tr("Mensajes guardados"), Icons.Outlined.Bookmark) {
                                showMenu = false
                                navController.navigate("saved_messages")
                            }
                            HomeMenuItem(tr("Chats secretos"), Icons.Outlined.Lock) {
                                showMenu = false
                                navController.navigate("secret_chats")
                            }
                            HomeMenuItem(tr("Canales"), Icons.Outlined.Campaign) {
                                showMenu = false
                                navController.navigate("channels")
                            }
                            HomeMenuItem(tr("Descargar vídeos"), Icons.Outlined.Download) {
                                showMenu = false
                                navController.navigate("video_download")
                            }
                            HomeMenuItem("Seguridad y Tor", Icons.Outlined.Security) {
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
            if (showHidden) {
                Row(
                    Modifier
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(NexusTokens.Radius.xl))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .padding(start = 14.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        tr("Estás viendo tus chats ocultos"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusTokens.Color.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { showHidden = false }) { Text(tr("Cerrar")) }
                }
            }
            HomeHeader(
                style = homeStyle,
                me = me,
                ghost = ghostMode,
                anonymous = com.Azelmods.App.data.security.tor.AnonymityPolicy.active,
                onOpenProfile = { onOpenTab(3) },
                onOpenContacts = { navController.navigate(Screen.NewConversation.route) },
                onOpenStories = { onOpenTab(1) },
                onOpenCalls = { onOpenTab(2) }
            )
            val homeSearchBar by com.Azelmods.App.ui.components.ChatLook.homeSearchBar.collectAsState()
            val quickBar by com.Azelmods.App.ui.components.ChatLook.quickBar.collectAsState()
            val hideArchived by com.Azelmods.App.ui.components.ChatLook.hideArchived.collectAsState()
            if (homeSearchBar) {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(NexusTokens.Radius.pill))
                        .background(NexusTokens.Color.BgElevated)
                        .clickable { navController.navigate(Screen.Search.route) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, null, tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(tr("Buscar chats, mensajes y personas"), style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextMuted)
                }
            }
            if (quickBar) {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(NexusTokens.Radius.xl))
                        .background(NexusTokens.Color.BgSurface)
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.navigate("secret_chats") }) {
                        Icon(Icons.Outlined.Lock, tr("Chats secretos"), tint = MaterialTheme.colorScheme.primary)
                    }
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.navigate("channels") }) {
                        Icon(Icons.Outlined.Campaign, tr("Canales"), tint = MaterialTheme.colorScheme.primary)
                    }
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.navigate("saved_messages") }) {
                        Icon(Icons.Outlined.Bookmark, tr("Mensajes guardados"), tint = MaterialTheme.colorScheme.primary)
                    }
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.navigate("video_download") }) {
                        Icon(Icons.Outlined.Download, tr("Descargar vídeos"), tint = MaterialTheme.colorScheme.primary)
                    }
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.navigate("terminal") }) {
                        Icon(Icons.Outlined.Terminal, tr("Terminal"), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            val hideFilters by com.Azelmods.App.ui.components.ChatLook.hideFilters.collectAsState()
            val textFilters by com.Azelmods.App.ui.components.ChatLook.textFilters.collectAsState()
            if (!hideFilters) Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatFilter.values().filter { !(hideArchived && it == ChatFilter.ARCHIVED) }.forEach { filter ->
                    val count = when (filter) {
                        ChatFilter.UNREAD -> state.chats.count { (it.unreadCount[currentUserId] ?: 0) > 0 && !it.isArchived }
                        ChatFilter.ARCHIVED -> state.chats.count { it.isArchived }
                        else -> 0
                    }
                    HomeFilter(
                        asText = textFilters,
                        label = when (filter) {
                            ChatFilter.ALL -> tr("Todos")
                            ChatFilter.UNREAD -> tr("No leídos")
                            ChatFilter.GROUPS -> tr("Grupos")
                            ChatFilter.ARCHIVED -> tr("Archivados")
                        },
                        selected = folderId == null && state.selectedFilter == filter,
                        count = count,
                        onClick = {
                            folderId = null
                            viewModel.onFilterChange(filter)
                        }
                    )
                }
                folders.forEach { folder ->
                    HomeFilter(
                        asText = textFilters,
                        label = folder.name,
                        selected = folderId == folder.id,
                        onClick = {
                            folderId = folder.id
                            viewModel.onFilterChange(ChatFilter.ALL)
                        }
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

                activeFolder != null && visibleChats.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        tr("Esta carpeta está vacía. Mantén pulsado un chat para añadirlo."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusTokens.Color.TextMuted,
                        modifier = Modifier.padding(32.dp)
                    )
                }

                visibleChats.isEmpty() -> EmptyChatsState(
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
                        val archivedCount = state.chats.count { it.isArchived }
                        if (archivedCount > 0 && folderId == null && state.selectedFilter == ChatFilter.ALL && !hideArchived) {
                            item(key = "archived_row") {
                                ArchivedRow(archivedCount) { viewModel.onFilterChange(ChatFilter.ARCHIVED) }
                            }
                        }
                        itemsIndexed(items = visibleChats, key = { _, chat -> chat.chatId }) { index, chat ->
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
                                com.Azelmods.App.ui.components.RowDivider(startInset = 82.dp)
                            }
                        }
                        item { Spacer(Modifier.height(88.dp)) }
                    }
                }
            }
        }
    }

    folderChat?.let { chat ->
        ChatFoldersDialog(chatId = chat.chatId, folders = folders, onDismiss = { folderChat = null })
    }
    joinCode?.let { code ->
        com.Azelmods.App.ui.screens.group.JoinGroupDialog(
            initialCode = code,
            onDismiss = { joinCode = null },
            onJoined = { chatId ->
                joinCode = null
                navController.navigate("chat/$chatId")
            }
        )
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
                    if ((chat.unreadCount[currentUserId] ?: 0) == 0) {
                        ChatActionItem(
                            icon = Icons.Default.MarkChatUnread,
                            text = tr("Marcar como no leído"),
                            onClick = {
                                viewModel.markUnread(chat.chatId)
                                showBottomSheet = false
                            }
                        )
                    }
                    ChatActionItem(
                        icon = Icons.Default.Folder,
                        text = tr("Carpetas"),
                        onClick = {
                            folderChat = chat
                            showBottomSheet = false
                        }
                    )
                    ChatActionItem(
                        icon = if (chat.chatId in hiddenChats) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        text = if (chat.chatId in hiddenChats) tr("Dejar de ocultar") else tr("Ocultar chat"),
                        onClick = {
                            showBottomSheet = false
                            val hide = chat.chatId !in hiddenChats
                            confirmDeviceOwner(context, if (hide) tr("Ocultar chat") else tr("Dejar de ocultar"), onNoLock = {
                                toast(tr("Pon un bloqueo de pantalla en el teléfono para usar los chats ocultos"))
                            }) {
                                com.Azelmods.App.data.chat.ChatOrganizer.setHidden(chat.chatId, hide)
                                toast(if (hide) tr("Chat oculto. Lo encuentras en el menú, en Chats ocultos.") else tr("El chat vuelve a la lista"))
                            }
                        }
                    )
                    if (chat.chatType == ChatType.GROUP) {
                        ChatActionItem(
                            icon = Icons.Default.Groups,
                            text = tr("Información del grupo"),
                            onClick = {
                                showBottomSheet = false
                                navController.navigate("group_info/${chat.chatId}")
                            }
                        )
                    }
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
/** Una conversación de la lista, con el estilo de fila que haya elegido el usuario. */
@Composable
fun ChatRow(
    chat: Chat,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onAvatarClick: (() -> Unit)? = null,
    hasStory: Boolean = false,
    colorRing: Boolean = true
) {
    val rowStyle by com.Azelmods.App.ui.components.ChatLook.rowStyle.collectAsState()
    val selfId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val peerId = chat.participantIds.ifEmpty { chat.participants }.firstOrNull { it != selfId }
    val frameColors = remember(peerId, chat.contactName, chat.contactColor) {
        ProfileColors.forUser(peerId ?: chat.contactName, chat.contactColor)
    }
    com.Azelmods.App.ui.components.ChatRowFrame(
        style = rowStyle,
        contactColors = frameColors,
        photoUrl = peerId?.let { chat.participantPhotos[it] } ?: chat.contactPhotoUrl
    ) {
        ChatRowBody(chat, onClick, onLongPress, onAvatarClick, hasStory, colorRing)
    }
}

@Composable
private fun ChatRowBody(
    chat: Chat,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onAvatarClick: (() -> Unit)?,
    hasStory: Boolean,
    colorRing: Boolean
) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val otherUserId = chat.participantIds.ifEmpty { chat.participants }
        .firstOrNull { it != currentUserId }
    // El asistente siempre se llama igual, tenga lo que tenga su ficha en el servidor.
    val contactName = "Azel Assistant".takeIf { chat.chatId.contains(com.Azelmods.App.data.demo.DemoAssistant.USER_ID) }
        ?: otherUserId?.let { chat.participantNames[it] }
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

            if (com.Azelmods.App.ui.components.LocalChatRowDivider.current) {
                val lineTint by com.Azelmods.App.ui.components.ChatLook.rowTint.collectAsState()
                Box(
                    Modifier
                        .padding(vertical = 1.2.dp)
                        .fillMaxWidth()
                        .height(0.8.dp)
                        .background(
                            if (lineTint == com.Azelmods.App.ui.components.RowTint.BLUE) Color(0xFF0077FF)
                            else MaterialTheme.colorScheme.primary
                        )
                )
            }
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

/** Pide la huella o el bloqueo del teléfono. Sin bloqueo puesto no hay nada que proteja el chat. */
private fun confirmDeviceOwner(
    context: android.content.Context,
    title: String,
    onNoLock: () -> Unit,
    onConfirmed: () -> Unit
) {
    val keyguard = context.getSystemService(android.app.KeyguardManager::class.java)
    if (keyguard?.isDeviceSecure != true) {
        onNoLock()
        return
    }
    val authenticators = android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_WEAK or
        android.hardware.biometrics.BiometricManager.Authenticators.DEVICE_CREDENTIAL
    android.hardware.biometrics.BiometricPrompt.Builder(context)
        .setTitle(title)
        .setAllowedAuthenticators(authenticators)
        .build()
        .authenticate(
            android.os.CancellationSignal(),
            context.mainExecutor,
            object : android.hardware.biometrics.BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: android.hardware.biometrics.BiometricPrompt.AuthenticationResult) {
                    onConfirmed()
                }
            }
        )
}

@Composable
private fun ChatFoldersDialog(
    chatId: String,
    folders: List<com.Azelmods.App.data.chat.ChatFolder>,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Carpetas")) },
        text = {
            Column {
                folders.forEach { folder ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = chatId in folder.chatIds,
                            onCheckedChange = { com.Azelmods.App.data.chat.ChatOrganizer.toggleInFolder(folder.id, chatId) }
                        )
                        Text(
                            folder.name,
                            color = NexusTokens.Color.TextPrimary,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        com.Azelmods.App.ui.components.AnimatedIconButton(
                            onClick = { com.Azelmods.App.data.chat.ChatOrganizer.deleteFolder(folder.id) }
                        ) {
                            Icon(Icons.Default.DeleteOutline, tr("Borrar carpeta"), tint = NexusTokens.Color.TextMuted)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    singleLine = true,
                    label = { Text(tr("Nueva carpeta")) },
                    trailingIcon = {
                        com.Azelmods.App.ui.components.AnimatedIconButton(
                            enabled = name.isNotBlank(),
                            onClick = {
                                com.Azelmods.App.data.chat.ChatOrganizer.createFolder(name, chatId)
                                name = ""
                            }
                        ) {
                            Icon(Icons.Default.Add, tr("Crear"))
                        }
                    }
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

/** Un filtro de la lista: como cápsula o como pestaña de texto con un punto de color. */
@Composable
private fun HomeFilter(
    asText: Boolean,
    label: String,
    selected: Boolean,
    count: Int = 0,
    onClick: () -> Unit
) {
    if (!asText) {
        NexusFilterChip(label = label, selected = selected, count = count, onClick = onClick)
        return
    }
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
            color = if (selected) accent else NexusTokens.Color.TextMuted
        )
        if (count > 0) {
            Spacer(Modifier.width(4.dp))
            Text(count.toString(), style = MaterialTheme.typography.labelSmall, color = accent)
        }
        if (selected) {
            Spacer(Modifier.width(5.dp))
            Box(
                Modifier
                    .size(6.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .background(accent)
            )
        }
    }
}

/** Fila de Archivados: icono dentro de un aro de 1,5 dp y el estilo de fila elegido. */
@Composable
private fun ArchivedRow(count: Int, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    com.Azelmods.App.ui.components.ListRowFrame(seed = "archived", photoUrl = null) {
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .border(1.5.dp, accent, androidx.compose.foundation.shape.CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Archive, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Text(tr("Archivados"), style = MaterialTheme.typography.titleMedium, color = NexusTokens.Color.TextPrimary, modifier = Modifier.weight(1f))
            Text(count.toString(), style = MaterialTheme.typography.labelLarge, color = accent)
        }
    }
}
