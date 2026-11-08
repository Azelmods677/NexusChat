package com.Azelmods.App.ui.screens.chat
import kotlinx.coroutines.launch

import com.Azelmods.App.i18n.tr

import com.Azelmods.App.ui.components.messageEnter
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.Azelmods.App.ui.theme.Teal
import com.Azelmods.App.ui.theme.IndigoDeep
import com.Azelmods.App.ui.theme.PurpleLight
import com.Azelmods.App.ui.theme.EmeraldGreen
import com.Azelmods.App.ui.theme.ErrorRed
import com.Azelmods.App.ui.theme.DarkElevated
import com.Azelmods.App.ui.theme.DarkSurfaceVariant
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    contactId: String,
    navController: NavController,
    viewModel: ChatViewModel = hiltViewModel(),
    settingsViewModel: com.Azelmods.App.ui.screens.settings.SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    
    var selectedIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var forwardMessages by remember { mutableStateOf<List<com.Azelmods.App.data.model.Message>>(emptyList()) }
    var showScheduled by remember { mutableStateOf(false) }
    var showQuickFunctions by remember { mutableStateOf(false) }
    // Atrás primero suelta la selección, no se sale del chat con mensajes marcados.
    androidx.activity.compose.BackHandler(enabled = selectedIds.isNotEmpty()) { selectedIds = emptySet() }
    if (forwardMessages.isNotEmpty()) {
        ForwardDialog(forwardMessages, onDismiss = { forwardMessages = emptyList() })
    }
    if (showScheduled) {
        ScheduledMessagesDialog(viewModel.chatIdFor(contactId), onDismiss = { showScheduled = false })
    }

    // Mientras se ve este chat, sus mensajes no generan avisos.
    val chatLifecycle = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(chatLifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> com.Azelmods.App.service.ActiveChat.isVisible = true
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> com.Azelmods.App.service.ActiveChat.isVisible = false
                else -> Unit
            }
        }
        chatLifecycle.lifecycle.addObserver(observer)
        onDispose {
            chatLifecycle.lifecycle.removeObserver(observer)
            com.Azelmods.App.service.ActiveChat.isVisible = false
        }
    }

    val savedMessages: SavedMessagesViewModel = hiltViewModel()
    var showSafetyNumber by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val state by viewModel.state.collectAsState()
    if (showSafetyNumber) {
        state.contact?.uid?.let { peer -> SafetyNumberDialog(peer, onDismiss = { showSafetyNumber = false }) }
    }
    var messageText by remember { mutableStateOf("") }
    var showMenu by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    var flashMessageId by remember { mutableStateOf<String?>(null) }
    val myUid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    fun senderNameOf(uid: String): String = when {
        uid == myUid -> tr("Tú")
        state.groupMembers[uid] != null -> state.groupMembers.getValue(uid).name
        else -> state.contact?.let { peer -> peer.displayName.ifBlank { peer.name } }.orEmpty().ifBlank { tr("Contacto") }
    }
    if (showQuickFunctions) {
        val quickScope = rememberCoroutineScope()
        QuickFunctionsSheet(
            chatId = viewModel.chatIdFor(contactId),
            contact = state.contact,
            messages = state.messages,
            navController = navController,
            onSelectAll = { selectedIds = state.messages.map { it.messageId }.toSet() },
            onFirstMessage = {
                quickScope.launch {
                    if (state.messages.isNotEmpty()) listState.animateScrollToItem(0)
                }
            },
            onDismiss = { showQuickFunctions = false }
        )
    }
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
    var selectedImageMessage by remember { mutableStateOf<com.Azelmods.App.data.model.Message?>(null) }
    var showClearChatDialog by remember { mutableStateOf(false) }
    var showMuteDialog by remember { mutableStateOf(false) }

    // Dictado por voz: se delega en el reconocedor del sistema y el resultado se
    // AÑADE al borrador en vez de reemplazarlo, para poder dictar por partes.
    val dictationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(android.speech.RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.trim()
            .orEmpty()
        if (spoken.isNotBlank()) {
            messageText = if (messageText.isBlank()) spoken else "$messageText $spoken"
        }
    }
    val launchDictation: () -> Unit = {
        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Habla ahora")
        }
        try {
            dictationLauncher.launch(intent)
        } catch (e: Exception) {
            // Hay dispositivos sin ningún reconocedor instalado; avisar es mejor
            // que dejar el botón sin reacción aparente.
            android.widget.Toast
                .makeText(context, "Este dispositivo no tiene reconocimiento de voz", android.widget.Toast.LENGTH_SHORT)
                .show()
        }
    }
    
    LaunchedEffect(contactId) {
        viewModel.loadChat(contactId)
    }
    
    // Auto-scroll to bottom when new message arrives.
    // No mientras se busca: saltar al final cada vez que llega un mensaje
    // arruinaría la navegación entre coincidencias.
    // La primera vez se coloca al final al instante: animar el recorrido por toda la
    // conversación era lo que hacía que entrar a un chat pareciera lento.
    var positioned by remember(contactId) { mutableStateOf(false) }
    LaunchedEffect(state.messages.size, state.isSearchOpen) {
        if (state.messages.isNotEmpty() && !state.isSearchOpen) {
            if (positioned) {
                listState.animateScrollToItem(state.messages.size - 1)
            } else {
                listState.scrollToItem(state.messages.size - 1)
                positioned = true
            }
        }
    }

    // Desplazamiento al resultado de búsqueda activo.
    LaunchedEffect(state.searchIndex, state.searchResults) {
        val objetivo = state.searchResults.getOrNull(state.searchIndex) ?: return@LaunchedEffect
        val posicion = state.messages.indexOfFirst { it.messageId == objetivo }
        if (posicion >= 0) {
            listState.animateScrollToItem(posicion)
        }
    }
    
    // Si el chat tiene temporizador o hay mensajes temporales en pantalla, se bloquean las
    // capturas y la miniatura de "apps recientes": si no, el borrado no valdría de mucho.
    val secureWindow = state.chatTimerSeconds > 0 || state.messages.any { it.isEphemeral }
    val activityWindow = (context as? android.app.Activity)?.window
    DisposableEffect(secureWindow, activityWindow) {
        if (secureWindow) activityWindow?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { activityWindow?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE) }
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
            if (selectedIds.isNotEmpty()) {
                SelectionTopBar(
                    count = selectedIds.size,
                    onClose = { selectedIds = emptySet() },
                    onCopy = {
                        val text = state.messages.filter { it.messageId in selectedIds && !it.isViewOnce }
                            .joinToString("\n") { withoutLinkPreview(it.content) }
                        (context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                            .setPrimaryClip(android.content.ClipData.newPlainText("mensajes", text))
                        selectedIds = emptySet()
                    },
                    onForward = {
                        forwardMessages = state.messages.filter { it.messageId in selectedIds && !it.isViewOnce }
                        selectedIds = emptySet()
                    },
                    onDelete = {
                        state.messages.filter { it.messageId in selectedIds }.forEach { viewModel.deleteMessage(it, false) }
                        selectedIds = emptySet()
                    }
                )
            } else ChatTopBar(
                // Un grupo no tiene "contacto": se muestra con su nombre y sus miembros.
                contact = state.contact ?: state.groupName?.let {
                    com.Azelmods.App.data.model.User(name = it, displayName = it, photoUrl = state.groupPhoto)
                },
                groupSubtitle = state.groupName?.let { tr("%s miembros", state.groupMembers.size.toString()) },
                isTyping = state.isTyping,
                onBackClick = { navController.navigateUp() },
                onProfileClick = {
                    try {
                        if (contactId.startsWith("group_")) {
                            navController.navigate("group_info/$contactId")
                            return@ChatTopBar
                        }
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

            // ── Barra de búsqueda dentro de la conversación ──
            // Aparece bajo la cabecera al elegir "Buscar en el chat". Muestra
            // la posición actual (p. ej. 2/7) y permite saltar entre
            // coincidencias, que es lo que hace útil una búsqueda en un chat
            // largo frente a un simple filtro.
            AnimatedVisibility(visible = state.isSearchOpen) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = DarkSurface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                        TextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChange,
                            placeholder = { Text(tr("Buscar en esta conversación"), color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                        if (state.searchQuery.isNotBlank()) {
                            Text(
                                text = if (state.searchResults.isEmpty()) {
                                    "0"
                                } else {
                                    "${state.searchIndex + 1}/${state.searchResults.size}"
                                },
                                color = if (state.searchResults.isEmpty()) ErrorRed else Color.Gray,
                                fontSize = 12.sp
                            )
                            com.Azelmods.App.ui.components.AnimatedIconButton(
                                onClick = { viewModel.previousSearchResult() },
                                enabled = state.searchResults.isNotEmpty()
                            ) {
                                Icon(Icons.Default.KeyboardArrowUp, "Anterior", tint = Color.White)
                            }
                            com.Azelmods.App.ui.components.AnimatedIconButton(
                                onClick = { viewModel.nextSearchResult() },
                                enabled = state.searchResults.isNotEmpty()
                            ) {
                                Icon(Icons.Default.KeyboardArrowDown, "Siguiente", tint = Color.White)
                            }
                        }
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { viewModel.closeSearch() }) {
                            Icon(Icons.Default.Close, "Cerrar búsqueda", tint = Color.White)
                        }
                    }
                }
            }
        },
        containerColor = com.Azelmods.App.ui.theme.NexusTokens.Color.BgBase
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
                    // El Scaffold (insets por defecto) ya reserva la barra de navegación
                    // gestual en paddingValues.bottom. Un `.imePadding()` normal sumaría
                    // OTRA vez la altura de esa barra cuando abre el teclado (el IME la
                    // solapa) → hueco visible entre el input y el teclado en navegación
                    // gestual (Redmi/HyperOS). Excluyendo navigationBars del inset del IME
                    // el empuje total = max(navbar, teclado), sin doble conteo.
                    .windowInsetsPadding(
                        WindowInsets.ime.exclude(WindowInsets.navigationBars)
                    )
            ) {
                val pinnedId = rememberPinnedMessageId(viewModel.chatIdFor(contactId))
                val pinnedMessage = pinnedId?.let { id -> state.messages.firstOrNull { it.messageId == id } }
                if (pinnedMessage != null) {
                    PinnedMessageBanner(
                        preview = pollPreview(pinnedMessage.content) ?: pinnedMessage.content.ifBlank { tr("Foto o archivo") },
                        onOpen = {
                            coroutineScope.launch {
                                val position = state.messages.indexOfFirst { it.messageId == pinnedMessage.messageId }
                                if (position >= 0) listState.animateScrollToItem(position)
                            }
                        },
                        onUnpin = { setPinnedMessage(viewModel.chatIdFor(contactId), null) }
                    )
                }

                // Messages List
                Box(Modifier.weight(1f)) {
                PullToRefreshBox(
                    isRefreshing = state.isLoading,
                    onRefresh = { viewModel.refreshChat() },
                    modifier = Modifier.fillMaxSize()
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
                                            tr("Cargando mensajes anteriores..."),
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
                                        tr("— No hay más mensajes —"),
                                        color = Color.Gray.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        items(
                            items = state.messages,
                            key = { message -> message.listKey() }
                        ) { message ->
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            // Resalte del resultado de búsqueda activo: sin esto,
                            // saltar a una coincidencia deja al usuario mirando la
                            // conversación sin saber cuál de los mensajes casó.
                            val esResultadoActivo = state.isSearchOpen &&
                                state.searchResults.getOrNull(state.searchIndex) == message.messageId
                            val resalte by animateColorAsState(
                                targetValue = if (esResultadoActivo || message.messageId == flashMessageId) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                                } else {
                                    Color.Transparent
                                },
                                label = "resalteBusqueda"
                            )
                            // Solo se anima lo que acaba de llegar, no al abrir ni al volver atrás.
                            val isFresh = remember(message.messageId) {
                                System.currentTimeMillis() - message.timestamp < FRESH_MESSAGE_MS
                            }
                            val isSelected = message.messageId in selectedIds
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .messageEnter(animate = isFresh, fromEnd = message.senderId == currentUserId)
                                    .fillMaxWidth()
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.22f) else resalte,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                            androidx.compose.runtime.CompositionLocalProvider(
                                com.Azelmods.App.ui.components.LocalBubblePeer provides (
                                    state.groupMembers[message.senderId]?.let { it.name to it.photoUrl }
                                        ?: state.contact?.let { peer -> peer.displayName.ifBlank { peer.name } to peer.photoUrl }
                                ),
                                com.Azelmods.App.ui.components.LocalBubbleSender provides state.groupMembers[message.senderId]
                                    ?.let { message.senderId to it.name }
                            ) {
                            MessageBubble(
                                message = message,
                                isOwnMessage = message.senderId == currentUserId,
                                repliedMessage = message.replyTo
                                    ?.takeUnless { it.startsWith("story:") }
                                    ?.let { id -> state.messages.firstOrNull { it.messageId == id } },
                                repliedSenderName = message.replyTo
                                    ?.takeUnless { it.startsWith("story:") }
                                    ?.let { id -> state.messages.firstOrNull { it.messageId == id } }
                                    ?.let { senderNameOf(it.senderId) },
                                onQuoteClick = {
                                    coroutineScope.launch {
                                        val target = message.replyTo ?: return@launch
                                        val position = state.messages.indexOfFirst { it.messageId == target }
                                        if (position >= 0) {
                                            listState.animateScrollToItem(position)
                                            flashMessageId = target
                                            kotlinx.coroutines.delay(1_600)
                                            flashMessageId = null
                                        }
                                    }
                                },
                                onMentionClick = { name ->
                                    val uid = state.groupMembers.values.firstOrNull { it.name.equals(name, ignoreCase = true) }?.uid
                                        ?: state.contact?.takeIf { peer ->
                                            peer.displayName.equals(name, ignoreCase = true) || peer.name.equals(name, ignoreCase = true)
                                        }?.uid
                                    if (!uid.isNullOrBlank()) navController.navigate(com.Azelmods.App.ui.navigation.Screen.ProfileViewer.createRoute(uid))
                                },
                                autoplayVideos = state.autoplayVideos,
                                serverNow = viewModel::serverNow,
                                onPin = { setPinnedMessage(viewModel.chatIdFor(contactId), message.messageId) },
                                onSave = { savedMessages.save(com.Azelmods.App.ui.screens.chat.withoutLinkPreview(message.content)) },
                                onForward = { forwardMessages = listOf(message) },
                                onSelect = { selectedIds = selectedIds + message.messageId },
                                onReply = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.setReplyingTo(message)
                                },
                                onReactionClick = { emoji -> viewModel.addReaction(message.messageId, emoji) },
                                onImageClick = { url, sender, timestamp ->
                                    selectedImageUrl = url
                                    selectedImageSender = sender
                                    selectedImageTimestamp = timestamp
                                    // Se guarda el mensaje entero, no sólo la URL: el
                                    // visor necesita el id para poder borrar la foto.
                                    selectedImageMessage = message
                                    showImageViewer = true
                                },
                                onDeleteClick = { forEveryone ->
                                    viewModel.deleteMessage(message, forEveryone)
                                },
                                onEditClick = { viewModel.setEditingMessage(message) },
                                onMessageViewed = { viewModel.markMessageViewed(message) },
                                onViewOnceClosed = { viewModel.consumeViewOnce(message) },
                                translatedText = state.translatedMessages[message.messageId],
                                isTranslating = message.messageId in state.translatingMessageIds,
                                onTranslate = {
                                    viewModel.translateMessage(message.messageId, message.content)
                                }
                            )
                            }
                            // Mientras se selecciona, un toque en un mensaje solo lo marca o desmarca.
                            if (selectedIds.isNotEmpty()) {
                                Box(
                                    Modifier
                                        .matchParentSize()
                                        .clickable(
                                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            selectedIds = if (isSelected) selectedIds - message.messageId else selectedIds + message.messageId
                                        }
                                )
                            }
                            }
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
                val quickFunctionsOn by com.Azelmods.App.ui.components.ChatLook.quickFunctions.collectAsState()
                if (quickFunctionsOn && selectedIds.isEmpty()) {
                    QuickFunctionsButton(
                        onClick = { showQuickFunctions = true },
                        modifier = Modifier.align(Alignment.TopEnd)
                    )
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
                                label = if (message.senderId == myUid) tr("Respondiendo a tu mensaje")
                                else tr("Respondiendo a %s", senderNameOf(message.senderId)),
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
                            ReplyPreviewBar(
                                message = message,
                                label = tr("Editar mensaje"),
                                onCancel = {
                                    viewModel.setEditingMessage(null)
                                    messageText = ""
                                }
                            )
                        }
                    }

                    // ── Dictado por voz ──
                    // Usa el reconocedor de Android (el mismo del teclado de
                    // Google), no un servicio externo: el audio no sale del
                    // dispositivo salvo que el propio Android decida procesarlo
                    // en la nube, que es lo que el usuario ya tiene configurado
                    // a nivel de sistema.
                    if (state.voiceDictationEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            SuggestionChip(
                                onClick = { launchDictation() },
                                label = { Text(tr("Dictar"), fontSize = 13.sp) },
                                icon = {
                                    Icon(
                                        Icons.Default.KeyboardVoice,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }

                    // ── Respuestas sugeridas por IA ──
                    // Sólo aparecen si el usuario activó la función y el modelo
                    // devolvió algo. Al tocar una, se escribe en el campo de
                    // texto en vez de enviarse: la decisión de mandar el mensaje
                    // sigue siendo del usuario, no de la IA.
                    if (state.smartRepliesEnabled && state.smartReplies.isNotEmpty() && messageText.isBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.smartReplies.forEach { suggestion ->
                                SuggestionChip(
                                    onClick = {
                                        messageText = suggestion
                                        viewModel.dismissSmartReplies()
                                    },
                                    label = { Text(suggestion, fontSize = 13.sp) },
                                    icon = {
                                        Icon(
                                            Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // ── Sugerencias de tono ──
                    if (state.toneSuggestionsEnabled && messageText.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (state.isRewritingTone) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            }
                            com.Azelmods.App.data.ai.MessageTone.entries.forEach { tone ->
                                SuggestionChip(
                                    enabled = !state.isRewritingTone,
                                    onClick = {
                                        viewModel.rewriteDraftTone(messageText, tone) { rewritten ->
                                            messageText = rewritten
                                        }
                                    },
                                    label = { Text(tone.label, fontSize = 13.sp) }
                                )
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
                                val downloadUrl = messageText.trim().takeIf { it.startsWith("/descargar ", ignoreCase = true) }
                                    ?.substringAfter(' ')?.trim()
                                if (downloadUrl != null) {
                                    navController.navigate("video_download?url=${android.net.Uri.encode(downloadUrl)}")
                                } else {
                                    viewModel.sendMessage(messageText, contactId)
                                }
                                messageText = ""
                            }
                        },
                        contactId = contactId,
                        viewModel = viewModel,
                        chatTimerSeconds = state.chatTimerSeconds,
                        viewOnceNext = state.viewOnceNext,
                        showEphemeralPicker = state.showEphemeralPicker,
                        isUploading = state.isUploading
                    )
                }
            }
            
            // Dropdown Menu (overlay)
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
                modifier = Modifier.background(DarkSurface)
            ) {
                DropdownMenuItem(
                    text = {
                        Text(if (contactId.startsWith("group_")) tr("Información del grupo") else tr("Ver perfil"), color = Color.White)
                    },
                    onClick = {
                        showMenu = false
                        if (contactId.startsWith("group_")) {
                            navController.navigate("group_info/$contactId")
                            return@DropdownMenuItem
                        }
                        try {
                            val profileId = state.contact?.uid?.takeIf { it.isNotBlank() } ?: contactId
                            navController.navigate("profile/$profileId")
                        } catch (e: Exception) { }
                    },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text(tr("Mensajes programados"), color = Color.White) },
                    onClick = {
                        showMenu = false
                        showScheduled = true
                    },
                    leadingIcon = { Icon(Icons.Default.Schedule, null, tint = Color.White) }
                )
                val safetyPeer = state.contact?.uid?.takeIf { it.isNotBlank() }
                if (safetyPeer != null) {
                    DropdownMenuItem(
                        text = { Text(tr("Número de seguridad"), color = Color.White) },
                        onClick = {
                            showMenu = false
                            showSafetyNumber = true
                        },
                        leadingIcon = { Icon(Icons.Default.VerifiedUser, null, tint = Color.White) }
                    )
                }
                DropdownMenuItem(
                    text = { Text(tr("Fondo del chat"), color = Color.White) },
                    onClick = {
                        showMenu = false
                        try {
                            navController.navigate("background_picker?chatId=$contactId")
                        } catch (e: Exception) { }
                    },
                    leadingIcon = { Icon(Icons.Default.Wallpaper, null, tint = Color.White) }
                )
                // Resumir la conversación con el modelo del usuario. Sólo aparece
                // si se activó en Ajustes → IA, para no ofrecer una acción que
                // fallaría por no haber proveedor configurado.
                if (state.summaryEnabled) {
                    DropdownMenuItem(
                        text = { Text(tr("Resumir chat"), color = Color.White) },
                        onClick = {
                            showMenu = false
                            viewModel.summarizeConversation()
                        },
                        leadingIcon = { Icon(Icons.Default.Summarize, null, tint = Color.White) }
                    )
                }
                // "Buscar" y "Silenciar" llevaban versiones con
                // `onClick = { showMenu = false }` y nada más: no hacían
                // absolutamente nada. Ahora tienen implementación real —búsqueda
                // sobre los mensajes de la conversación y silencio por usuario
                // con caducidad—.
                DropdownMenuItem(
                    text = { Text(tr("Mensajes temporales"), color = Color.White) },
                    onClick = {
                        showMenu = false
                        viewModel.toggleEphemeralPicker()
                    },
                    leadingIcon = { Icon(Icons.Default.Timer, null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = { Text(tr("Buscar en el chat"), color = Color.White) },
                    onClick = {
                        showMenu = false
                        viewModel.openSearch()
                    },
                    leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.White) }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            if (state.isMuted) tr("Reactivar notificaciones") else tr("Silenciar chat"),
                            color = Color.White
                        )
                    },
                    onClick = {
                        showMenu = false
                        if (state.isMuted) {
                            viewModel.toggleMute { message ->
                                android.widget.Toast
                                    .makeText(context, message, android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                        } else {
                            showMuteDialog = true
                        }
                    },
                    leadingIcon = {
                        Icon(
                            if (state.isMuted) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                            null,
                            tint = Color.White
                        )
                    }
                )
                DropdownMenuItem(
                    text = { Text(tr("Vaciar chat"), color = ErrorRed) },
                    onClick = {
                        showMenu = false
                        showClearChatDialog = true
                    },
                    leadingIcon = { Icon(Icons.Default.Delete, null, tint = ErrorRed) }
                )
            }
        }
        
        if (showMuteDialog) {
            val opciones = listOf(
                "1 hora" to com.Azelmods.App.data.model.ChatSettings.MUTE_1_HOUR,
                "8 horas" to com.Azelmods.App.data.model.ChatSettings.MUTE_8_HOURS,
                "1 semana" to com.Azelmods.App.data.model.ChatSettings.MUTE_1_WEEK,
                "Hasta que lo reactive" to com.Azelmods.App.data.model.ChatSettings.MUTE_ALWAYS
            )
            AlertDialog(
                onDismissRequest = { showMuteDialog = false },
                icon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
                title = { Text(tr("Silenciar conversación")) },
                text = {
                    Column {
                        Text(
                            tr("Dejarás de recibir notificaciones de este chat. Las llamadas seguirán entrando."),
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        opciones.forEach { (etiqueta, duracion) ->
                            TextButton(
                                onClick = {
                                    showMuteDialog = false
                                    viewModel.toggleMute(duracion) { message ->
                                        android.widget.Toast
                                            .makeText(context, message, android.widget.Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(etiqueta, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showMuteDialog = false }) { Text(tr("Cancelar")) }
                }
            )
        }

        // Resumen de la conversación (o el error si el proveedor falló).
        if (state.isSummarizing) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text(tr("Resumiendo…")) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(tr("Leyendo la conversación"))
                    }
                },
                confirmButton = {}
            )
        }
        state.conversationSummary?.let { summary ->
            AlertDialog(
                onDismissRequest = { viewModel.dismissSummary() },
                icon = { Icon(Icons.Default.Summarize, contentDescription = null) },
                title = { Text(tr("Resumen del chat")) },
                text = {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        com.Azelmods.App.ui.components.MarkdownText(markdown = summary)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.dismissSummary() }) { Text(tr("Cerrar")) }
                }
            )
        }

        if (showClearChatDialog) {
            AlertDialog(
                onDismissRequest = { showClearChatDialog = false },
                icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                title = { Text(tr("¿Vaciar la conversación?")) },
                text = { Text(tr("Se borrarán todos los mensajes de este chat para los dos participantes. La conversación se mantiene.")) },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearChatDialog = false
                            viewModel.clearChat { message ->
                                android.widget.Toast
                                    .makeText(context, message, android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    ) {
                        Text(tr("Vaciar"), color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearChatDialog = false }) {
                        Text(tr("Cancelar"))
                    }
                }
            )
        }

        // Full Screen Image Viewer
        if (showImageViewer) {
            val imageMessage = selectedImageMessage
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            FullScreenImageViewer(
                imageUrl = selectedImageUrl,
                senderName = selectedImageSender,
                timestamp = selectedImageTimestamp,
                onDismiss = { showImageViewer = false },
                // El botón de eliminar del visor sólo mostraba un aviso pidiendo
                // que se borrara desde el chat. Ahora borra de verdad; si la foto
                // es de otra persona sólo se puede quitar de la propia vista, así
                // que el visor recibe también si el mensaje es nuestro.
                onDelete = imageMessage?.let { msg ->
                    { forEveryone: Boolean ->
                        viewModel.deleteMessage(msg, forEveryone)
                        showImageViewer = false
                    }
                },
                canDeleteForEveryone = imageMessage?.senderId == currentUserId
            )
        }
    }
}

/**
 * Frase completa de última conexión para la barra del chat. Un reloj adelantado en el
 * teléfono daría diferencias negativas ("hace -3 min"), por eso ese caso va aparte.
 */
private fun formatLastSeen(timestamp: Long): String {
    if (timestamp <= 0L) return "sin conexión reciente"
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 0 -> tr("en línea hace poco")
        diff < 60_000 -> tr("última vez hace menos de un minuto")
        diff < 3_600_000 -> "última vez hace ${diff / 60_000} min"
        diff < 86_400_000 -> "última vez hace ${diff / 3_600_000} h"
        diff < 7L * 86_400_000L -> "última vez hace ${diff / 86_400_000} d"
        else -> "última vez el ${SimpleDateFormat("dd/MM/yy", Locale.getDefault()).format(Date(timestamp))}"
    }
}

@Composable
fun ChatTopBar(
    contact: com.Azelmods.App.data.model.User?,
    groupSubtitle: String? = null,
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
        groupSubtitle != null -> groupSubtitle
        contact?.isOnline == true -> tr("en línea")
        // formatLastSeen ya devuelve la frase completa; anteponer "última vez"
        // aquí producía "última vez visto hace 5min".
        else -> formatLastSeen(contact?.lastSeen ?: 0)
    }
    
    val statusColor = when {
        isTyping -> MaterialTheme.colorScheme.primary
        contact?.isOnline == true -> com.Azelmods.App.ui.theme.NexusTokens.Color.Online
        else -> com.Azelmods.App.ui.theme.NexusTokens.Color.TextMuted
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
                contentDescription = tr("Galería del chat"),
                onClick = onGalleryClick
            )
            // Las llamadas son de dos personas: en un grupo no tienen a quién llamar.
            if (groupSubtitle == null) {
                Box {
                    TopBarActionIcon(
                        icon = Icons.Default.Phone,
                        contentDescription = tr("Llamada de voz"),
                        onClick = onPhoneClick
                    )
                    com.Azelmods.App.ui.components.CallIconMotion(video = false, modifier = Modifier.matchParentSize())
                }
                Box {
                    TopBarActionIcon(
                        icon = Icons.Default.Videocam,
                        contentDescription = tr("Videollamada"),
                        onClick = onVideoClick
                    )
                    com.Azelmods.App.ui.components.CallIconMotion(video = true, modifier = Modifier.matchParentSize())
                }
            }
            TopBarActionIcon(
                icon = Icons.Default.MoreVert,
                contentDescription = tr("Más opciones"),
                onClick = onMoreClick
            )
        },
        backgroundColor = DarkSurface,
        contentColor = Color.White
    )
}

private const val FRESH_MESSAGE_MS = 4_000L
