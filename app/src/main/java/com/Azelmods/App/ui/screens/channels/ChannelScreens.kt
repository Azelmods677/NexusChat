package com.Azelmods.App.ui.screens.channels

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.Azelmods.App.data.channels.Channel
import com.Azelmods.App.data.channels.ChannelPost
import com.Azelmods.App.data.channels.ChannelRepository
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChannelsViewModel @Inject constructor(
    private val channels: ChannelRepository
) : ViewModel() {
    val subscribed: StateFlow<List<Channel>> = channels.observeSubscribed()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private val _popular = MutableStateFlow<List<Channel>>(emptyList())
    val popular = _popular.asStateFlow()

    fun loadPopular() {
        viewModelScope.launch {
            runCatching { channels.popular() }
                .onSuccess { _popular.value = it }
                .onFailure { android.util.Log.w("Channels", "No se pudieron cargar los canales", it) }
        }
    }

    fun create(name: String, handle: String, description: String, onCreated: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = runCatching { channels.create(name, handle, description) }.getOrNull()
            if (id == null) {
                _message.value = tr("Ese nombre de canal ya está cogido")
            } else {
                kotlinx.coroutines.withContext(Dispatchers.Main) { onCreated(id) }
            }
        }
    }

    fun find(handle: String, onFound: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val channel = runCatching { channels.findByHandle(handle) }.getOrNull()
            if (channel == null) {
                _message.value = tr("No hay ningún canal con ese nombre")
            } else {
                kotlinx.coroutines.withContext(Dispatchers.Main) { onFound(channel.id) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelsScreen(navController: NavController, viewModel: ChannelsViewModel = hiltViewModel()) {
    val subscribed by viewModel.subscribed.collectAsState()
    val popular by viewModel.popular.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadPopular() }
    val message by viewModel.message.collectAsState()
    var creating by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Canales")) },
                navigationIcon = {
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusTokens.Color.BgBase)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) { Icon(Icons.Default.Add, tr("Crear canal")) }
        },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        placeholder = { Text(tr("Buscar por @nombre")) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = { viewModel.find(search) { id -> navController.navigate("channel/$id") } },
                        enabled = search.isNotBlank()
                    ) { Text(tr("Buscar")) }
                }
                message?.let {
                    Text(it, color = NexusTokens.Color.Warning, modifier = Modifier.padding(horizontal = 16.dp))
                }
                // Difusión: el mismo mensaje a varios contactos, cada uno en su privado.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("broadcast") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Campaign, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(tr("Nueva difusión"), color = MaterialTheme.colorScheme.primary)
                        Text(
                            tr("Envía un mensaje a varios contactos a la vez"),
                            style = MaterialTheme.typography.bodySmall,
                            color = NexusTokens.Color.TextMuted
                        )
                    }
                }
                if (subscribed.isEmpty()) {
                    Text(
                        tr("Aún no sigues ningún canal. Busca uno por su nombre o crea el tuyo para publicar a quien quiera seguirte."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusTokens.Color.TextSecondary,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(subscribed, key = { it.id }) { channel ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("channel/${channel.id}") }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ChannelAvatar(channel, 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(channel.name, style = MaterialTheme.typography.titleMedium, color = NexusTokens.Color.TextPrimary, maxLines = 1)
                        Text(
                            channel.lastPost.ifBlank { "@${channel.handle}" },
                            style = MaterialTheme.typography.bodySmall,
                            color = NexusTokens.Color.TextMuted,
                            maxLines = 1
                        )
                    }
                }
            }
            // Descubrir canales sin saber su @nombre exacto.
            val toDiscover = popular.filter { p -> subscribed.none { it.id == p.id } }
            if (toDiscover.isNotEmpty()) {
                item {
                    Text(
                        tr("Explorar canales"),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp)
                    )
                }
                items(toDiscover, key = { "explore_" + it.id }) { channel ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { navController.navigate("channel/${channel.id}") }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChannelAvatar(channel, 48.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(channel.name, style = MaterialTheme.typography.titleMedium, color = NexusTokens.Color.TextPrimary, maxLines = 1)
                            Text(
                                channel.description.ifBlank { "@${channel.handle}" },
                                style = MaterialTheme.typography.bodySmall,
                                color = NexusTokens.Color.TextMuted,
                                maxLines = 1
                            )
                        }
                        Text(
                            tr("%s seguidores", channel.subscribers.toString()),
                            style = MaterialTheme.typography.labelSmall,
                            color = NexusTokens.Color.TextMuted
                        )
                    }
                }
            }
        }
    }

    if (creating) {
        var name by remember { mutableStateOf("") }
        var handle by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { creating = false },
            title = { Text(tr("Crear canal")) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(tr("Nombre")) }, singleLine = true)
                    OutlinedTextField(handle, { handle = it }, label = { Text(tr("@nombre para encontrarlo")) }, singleLine = true)
                    OutlinedTextField(description, { description = it }, label = { Text(tr("De qué trata")) })
                }
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank() && handle.isNotBlank(),
                    onClick = {
                        creating = false
                        viewModel.create(name, handle, description) { id -> navController.navigate("channel/$id") }
                    }
                ) { Text(tr("Crear")) }
            },
            dismissButton = { TextButton(onClick = { creating = false }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }
}

@HiltViewModel
class ChannelViewModel @Inject constructor(
    private val channels: ChannelRepository,
    @param:dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {
    private val _channel = MutableStateFlow<Channel?>(null)
    val channel = _channel.asStateFlow()
    private val _posts = MutableStateFlow<List<ChannelPost>>(emptyList())
    val posts = _posts.asStateFlow()
    private val _subscribed = MutableStateFlow(false)
    val subscribed = _subscribed.asStateFlow()
    private val _publishing = MutableStateFlow(false)
    val publishing = _publishing.asStateFlow()
    private val _muted = MutableStateFlow(false)
    val muted = _muted.asStateFlow()
    private val _gone = MutableStateFlow(false)
    /** El canal se borró: la pantalla vuelve atrás. */
    val gone = _gone.asStateFlow()
    private var id = ""
    private val viewed = mutableSetOf<String>()

    fun open(channelId: String) {
        if (id == channelId) return
        id = channelId
        _muted.value = channels.isMuted(context, channelId)
        viewModelScope.launch { channels.observeChannel(channelId).collect { _channel.value = it } }
        viewModelScope.launch {
            channels.observePosts(channelId).collect { list ->
                _posts.value = list
                // Una vista por persona y publicación, sin repetir escrituras.
                val fresh = list.map { it.id }.filter { viewed.add(it) }
                runCatching { channels.markViewed(channelId, fresh) }
            }
        }
        viewModelScope.launch(Dispatchers.IO) { _subscribed.value = runCatching { channels.isSubscribed(channelId) }.getOrDefault(false) }
    }

    fun toggleSubscription() {
        val next = !_subscribed.value
        _subscribed.value = next
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { if (next) channels.subscribe(id) else channels.unsubscribe(id) }
                .onFailure { _subscribed.value = !next }
        }
    }

    fun toggleMute() {
        val next = !_muted.value
        _muted.value = next
        channels.setMuted(context, id, next)
    }

    fun publish(text: String, image: Uri?, audio: Uri? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            _publishing.value = true
            runCatching { channels.publish(id, text, image, audio) }
            _publishing.value = false
        }
    }

    fun react(postId: String, emoji: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { channels.react(id, postId, emoji) } }
    }

    fun edit(name: String, description: String, photo: Uri?) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { channels.edit(id, name, description, photo) } }
    }

    fun delete() {
        val current = _channel.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { channels.delete(current) }.onSuccess { _gone.value = true }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { channels.deletePost(id, postId) } }
    }
}

private val QUICK_REACTIONS = listOf("👍", "❤️", "🔥", "😂", "😮", "🙏")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(navController: NavController, channelId: String, viewModel: ChannelViewModel = hiltViewModel()) {
    LaunchedEffect(channelId) { viewModel.open(channelId) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val channel by viewModel.channel.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val subscribed by viewModel.subscribed.collectAsState()
    val publishing by viewModel.publishing.collectAsState()
    val muted by viewModel.muted.collectAsState()
    val gone by viewModel.gone.collectAsState()
    LaunchedEffect(gone) { if (gone) navController.popBackStack() }
    val isOwner = channel?.ownerId == FirebaseAuth.getInstance().currentUser?.uid
    var draft by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<Uri?>(null) }
    var menu by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { image = it }
    var audio by remember { mutableStateOf<Uri?>(null) }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { if (it != null) audio = it }
    var viewing by remember { mutableStateOf<String?>(null) }
    val list = rememberLazyListState()
    LaunchedEffect(posts.size) { if (posts.isNotEmpty()) list.animateScrollToItem(posts.lastIndex) }

    fun share(text: String) {
        context.startActivity(
            android.content.Intent.createChooser(
                android.content.Intent(android.content.Intent.ACTION_SEND).setType("text/plain").putExtra(android.content.Intent.EXTRA_TEXT, text),
                null
            )
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChannelAvatar(channel, 40.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(channel?.name.orEmpty(), maxLines = 1, style = MaterialTheme.typography.titleMedium)
                            channel?.let {
                                Text(
                                    tr("@%s, %d seguidores", it.handle, it.subscribers),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = NexusTokens.Color.TextMuted
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"))
                    }
                },
                actions = {
                    if (!isOwner) {
                        TextButton(onClick = viewModel::toggleSubscription) {
                            Text(if (subscribed) tr("Siguiendo") else tr("Seguir"))
                        }
                    }
                    Box {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { menu = true }) {
                            Icon(androidx.compose.material.icons.Icons.Default.MoreVert, tr("Más opciones"))
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }, containerColor = NexusTokens.Color.BgSurface) {
                            channel?.let { c ->
                                DropdownMenuItem(text = { Text(tr("Compartir canal")) }, onClick = {
                                    menu = false
                                    share(tr("Sigue el canal %s (@%s) en NexusChat: %s", c.name, c.handle, ChannelRepository.linkFor(c.handle)))
                                })
                            }
                            if (subscribed || isOwner) {
                                DropdownMenuItem(
                                    text = { Text(if (muted) tr("Activar avisos") else tr("Silenciar avisos")) },
                                    onClick = { menu = false; viewModel.toggleMute() }
                                )
                            }
                            if (isOwner) {
                                DropdownMenuItem(text = { Text(tr("Editar canal")) }, onClick = { menu = false; editing = true })
                                DropdownMenuItem(
                                    text = { Text(tr("Eliminar canal"), color = NexusTokens.Color.Error) },
                                    onClick = { menu = false; confirmDelete = true }
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusTokens.Color.BgSurface)
            )
        },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
        ) {
            LazyColumn(
                state = list,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                channel?.let { c ->
                    item(key = "header") { ChannelHeader(c, subscribed, isOwner) { viewModel.toggleSubscription() } }
                }
                if (posts.isEmpty()) {
                    item(key = "empty") {
                        Text(
                            if (isOwner) tr("Escribe abajo tu primera publicación. Llegará a todos tus seguidores.") else tr("Aún no hay publicaciones."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexusTokens.Color.TextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                items(posts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        canDelete = isOwner,
                        onDelete = { viewModel.deletePost(post.id) },
                        onReact = { emoji -> viewModel.react(post.id, emoji) },
                        onOpenImage = { viewing = it },
                        onShare = {
                            val c = channel
                            share(
                                buildString {
                                    if (post.text.isNotBlank()) append(post.text).append("\n\n")
                                    if (c != null) append(tr("— %s en NexusChat: %s", c.name, ChannelRepository.linkFor(c.handle)))
                                }
                            )
                        }
                    )
                }
            }
            if (isOwner) {
                image?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(72.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { image = null }
                    )
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                        Icon(Icons.Outlined.AddPhotoAlternate, tr("Añadir foto"), tint = NexusTokens.Color.TextSecondary)
                    }
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { audioPicker.launch("audio/*") }) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.AudioFile,
                            tr("Añadir audio"),
                            tint = if (audio != null) MaterialTheme.colorScheme.primary else NexusTokens.Color.TextSecondary
                        )
                    }
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it.take(4_000) },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = NexusTokens.Color.TextPrimary),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 6,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(22.dp))
                            .background(NexusTokens.Color.BgElevated)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        decorationBox = { inner ->
                            if (draft.isEmpty()) Text(tr("Publicar en el canal"), color = NexusTokens.Color.TextMuted)
                            inner()
                        }
                    )
                    com.Azelmods.App.ui.components.AnimatedIconButton(
                        enabled = (draft.isNotBlank() || image != null || audio != null) && !publishing,
                        onClick = {
                            viewModel.publish(draft, image, audio)
                            draft = ""
                            image = null
                            audio = null
                        }
                    ) {
                        if (publishing) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        else Icon(Icons.AutoMirrored.Filled.Send, tr("Publicar"), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }

    viewing?.let { url ->
        // Foto a pantalla completa, con zoom y botón para guardarla en la galería.
        com.Azelmods.App.ui.components.FullScreenImageViewer(
            imageUrl = url,
            senderName = channel?.name.orEmpty(),
            onDismiss = { viewing = null }
        )
    }

    if (editing) {
        channel?.let { c ->
            EditChannelDialog(c, onDismiss = { editing = false }) { name, description, photo ->
                viewModel.edit(name, description, photo)
                editing = false
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("¿Eliminar el canal?")) },
            text = { Text(tr("Se borran todas las publicaciones y el @nombre queda libre. No se puede deshacer.")) },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.delete() }) {
                    Text(tr("Eliminar"), color = NexusTokens.Color.Error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }
}

/** Foto del canal o, si no tiene, la inicial sobre el color de la app. */
@Composable
fun ChannelAvatar(channel: Channel?, size: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.3f))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        val photo = channel?.photoUrl
        if (photo != null) {
            AsyncImage(photo, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(Icons.Outlined.Campaign, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(size * 0.5f))
        }
    }
}

/** Cabecera del canal: foto grande, nombre, @nombre, seguidores y descripción. */
@Composable
private fun ChannelHeader(channel: Channel, subscribed: Boolean, isOwner: Boolean, onFollow: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(NexusTokens.Color.BgSurface)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChannelAvatar(channel, 84.dp)
        Spacer(Modifier.height(10.dp))
        Text(channel.name, style = MaterialTheme.typography.titleLarge, color = NexusTokens.Color.TextPrimary)
        Text(
            tr("@%s · %d seguidores", channel.handle, channel.subscribers),
            style = MaterialTheme.typography.bodySmall,
            color = NexusTokens.Color.TextMuted
        )
        if (channel.description.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                channel.description,
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTokens.Color.TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
        if (!isOwner && !subscribed) {
            Spacer(Modifier.height(14.dp))
            Button(onClick = onFollow) { Text(tr("Seguir canal")) }
        }
    }
}

@Composable
private fun EditChannelDialog(channel: Channel, onDismiss: () -> Unit, onSave: (String, String, Uri?) -> Unit) {
    var name by remember { mutableStateOf(channel.name) }
    var description by remember { mutableStateOf(channel.description) }
    var photo by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { if (it != null) photo = it }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Editar canal")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(84.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ) {
                    if (photo != null) AsyncImage(photo, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    else ChannelAvatar(channel, 84.dp)
                }
                Text(tr("Toca la foto para cambiarla"), style = MaterialTheme.typography.labelSmall, color = NexusTokens.Color.TextMuted)
                OutlinedTextField(name, { name = it.take(60) }, label = { Text(tr("Nombre")) }, singleLine = true)
                OutlinedTextField(description, { description = it.take(500) }, label = { Text(tr("Descripción")) }, maxLines = 5)
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onSave(name, description, photo) }) { Text(tr("Guardar")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PostCard(
    post: ChannelPost,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onReact: (String) -> Unit,
    onShare: () -> Unit,
    onOpenImage: (String) -> Unit = {}
) {
    val time = remember(post.at) { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(post.at)) }
    var picking by remember { mutableStateOf(false) }
    var allEmojis by remember { mutableStateOf(false) }
    if (allEmojis) {
        // Cualquier emoji, no solo los seis rápidos.
        androidx.compose.ui.window.Dialog(onDismissRequest = { allEmojis = false }) {
            com.Azelmods.App.ui.components.CompleteEmojiPicker(
                onEmojiSelected = { emoji ->
                    allEmojis = false
                    onReact(emoji)
                }
            )
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(NexusTokens.Color.BgSurface)
    ) {
        post.imageUrl?.let {
            AsyncImage(
                model = it,
                contentDescription = tr("Foto"),
                contentScale = ContentScale.FillWidth,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 360.dp)
                    .clickable { onOpenImage(it) }
            )
        }
        post.audioUrl?.let { url ->
            com.Azelmods.App.ui.components.AudioMessagePlayer(
                audioUrl = url,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)
            )
        }
        if (post.text.isNotBlank()) {
            // Mismo formato que los chats: enlaces azules, *negrita*, _cursiva_…
            val mention = MaterialTheme.colorScheme.primary
            val styled = remember(post.text) {
                com.Azelmods.App.ui.screens.chat.styledMessageText(
                    raw = post.text,
                    mentionColor = mention,
                    spoilerColor = NexusTokens.Color.TextMuted,
                    revealed = true,
                    onReveal = {}
                )
            }
            Text(
                styled,
                style = MaterialTheme.typography.bodyLarge,
                color = NexusTokens.Color.TextPrimary,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp)
            )
        }
        // Reacciones: cada emoji con su número; el tuyo resaltado.
        if (post.reactions.isNotEmpty()) {
            FlowRow(
                Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                post.reactions.entries.sortedByDescending { it.value }.forEach { (emoji, count) ->
                    val mine = emoji == post.myReaction
                    Text(
                        "$emoji $count",
                        style = MaterialTheme.typography.labelLarge,
                        color = NexusTokens.Color.TextPrimary,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (mine) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f) else NexusTokens.Color.BgElevated)
                            .clickable { onReact(emoji) }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(time, style = MaterialTheme.typography.labelSmall, color = NexusTokens.Color.TextMuted, modifier = Modifier.padding(vertical = 8.dp))
            if (post.views > 0) {
                Text(
                    "  ·  👁 ${post.views}",
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusTokens.Color.TextMuted
                )
            }
            Spacer(Modifier.weight(1f))
            Box {
                com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { picking = true }, modifier = Modifier.size(32.dp)) {
                    Icon(androidx.compose.material.icons.Icons.Outlined.AddReaction, tr("Reaccionar"), tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(18.dp))
                }
                DropdownMenu(expanded = picking, onDismissRequest = { picking = false }, containerColor = NexusTokens.Color.BgSurface) {
                    Row(Modifier.padding(horizontal = 8.dp)) {
                        Text(
                            "+",
                            style = MaterialTheme.typography.headlineSmall,
                            color = NexusTokens.Color.TextSecondary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { picking = false; allEmojis = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                        QUICK_REACTIONS.forEach { emoji ->
                            Text(
                                emoji,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { picking = false; onReact(emoji) }
                                    .padding(6.dp)
                            )
                        }
                    }
                }
            }
            com.Azelmods.App.ui.components.AnimatedIconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                Icon(androidx.compose.material.icons.Icons.Outlined.Share, tr("Compartir"), tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(18.dp))
            }
            if (canDelete) {
                com.Azelmods.App.ui.components.AnimatedIconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.DeleteOutline, tr("Eliminar"), tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
