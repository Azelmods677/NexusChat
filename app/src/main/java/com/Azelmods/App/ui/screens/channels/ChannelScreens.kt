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
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Campaign, null, tint = MaterialTheme.colorScheme.primary)
                    }
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
    private val channels: ChannelRepository
) : ViewModel() {
    private val _channel = MutableStateFlow<Channel?>(null)
    val channel = _channel.asStateFlow()
    private val _posts = MutableStateFlow<List<ChannelPost>>(emptyList())
    val posts = _posts.asStateFlow()
    private val _subscribed = MutableStateFlow(false)
    val subscribed = _subscribed.asStateFlow()
    private val _publishing = MutableStateFlow(false)
    val publishing = _publishing.asStateFlow()
    private var id = ""

    fun open(channelId: String) {
        if (id == channelId) return
        id = channelId
        viewModelScope.launch { channels.observeChannel(channelId).collect { _channel.value = it } }
        viewModelScope.launch { channels.observePosts(channelId).collect { _posts.value = it } }
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

    fun publish(text: String, image: Uri?) {
        viewModelScope.launch(Dispatchers.IO) {
            _publishing.value = true
            runCatching { channels.publish(id, text, image) }
            _publishing.value = false
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { channels.deletePost(id, postId) } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChannelScreen(navController: NavController, channelId: String, viewModel: ChannelViewModel = hiltViewModel()) {
    LaunchedEffect(channelId) { viewModel.open(channelId) }
    val channel by viewModel.channel.collectAsState()
    val posts by viewModel.posts.collectAsState()
    val subscribed by viewModel.subscribed.collectAsState()
    val publishing by viewModel.publishing.collectAsState()
    val isOwner = channel?.ownerId == FirebaseAuth.getInstance().currentUser?.uid
    var draft by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<Uri?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { image = it }
    val list = rememberLazyListState()
    LaunchedEffect(posts.size) { if (posts.isNotEmpty()) list.animateScrollToItem(posts.lastIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
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
                },
                navigationIcon = {
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"))
                    }
                },
                actions = {
                    if (!isOwner) {
                        TextButton(onClick = viewModel::toggleSubscription) {
                            Text(if (subscribed) tr("Dejar de seguir") else tr("Seguir"))
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
                channel?.description?.takeIf { it.isNotBlank() }?.let { description ->
                    item {
                        Text(description, style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextSecondary)
                    }
                }
                items(posts, key = { it.id }) { post -> PostCard(post, isOwner) { viewModel.deletePost(post.id) } }
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
                    BasicTextField(
                        value = draft,
                        onValueChange = { draft = it },
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
                        enabled = (draft.isNotBlank() || image != null) && !publishing,
                        onClick = {
                            viewModel.publish(draft, image)
                            draft = ""
                            image = null
                        }
                    ) {
                        if (publishing) CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                        else Icon(Icons.AutoMirrored.Filled.Send, tr("Publicar"), tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
private fun PostCard(post: ChannelPost, canDelete: Boolean, onDelete: () -> Unit) {
    val time = remember(post.at) { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(post.at)) }
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
            )
        }
        if (post.text.isNotBlank()) {
            Text(
                post.text,
                style = MaterialTheme.typography.bodyLarge,
                color = NexusTokens.Color.TextPrimary,
                modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp)
            )
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 4.dp, bottom = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(time, style = MaterialTheme.typography.labelSmall, color = NexusTokens.Color.TextMuted, modifier = Modifier.weight(1f).padding(vertical = 8.dp))
            if (canDelete) {
                com.Azelmods.App.ui.components.AnimatedIconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.DeleteOutline, tr("Eliminar"), tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
