package com.Azelmods.App.ui.screens.secret

import android.app.Activity
import android.view.WindowManager
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.data.security.secret.SecretChat
import com.Azelmods.App.data.security.secret.SecretChatRepository
import com.Azelmods.App.data.security.secret.SecretChatStatus
import com.Azelmods.App.data.security.secret.SecretMessage
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SecretContact(val uid: String, val name: String, val photo: String?)

@HiltViewModel
class SecretChatsViewModel @Inject constructor(
    private val secret: SecretChatRepository,
    private val database: RealtimeDatabaseRepository
) : ViewModel() {
    val chats: StateFlow<List<SecretChat>> = secret.observeChats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _contacts = MutableStateFlow<List<SecretContact>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    /** Las personas con las que ya hay una conversación. */
    fun loadContacts() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val found = runCatching { database.getUserChats(uid).first() }.getOrDefault(emptyList())
                .filter { (it["chatId"] as? String)?.startsWith("group_") != true }
                .mapNotNull { chat -> (chat["members"] as? Map<*, *>)?.keys?.filterIsInstance<String>()?.firstOrNull { it != uid } }
                .distinct()
                .mapNotNull { peer ->
                    val user = runCatching { database.getUserById(peer) }.getOrNull() ?: return@mapNotNull null
                    SecretContact(
                        peer,
                        (user["displayName"] as? String)?.ifBlank { null } ?: (user["name"] as? String).orEmpty(),
                        user["photoUrl"] as? String
                    )
                }
            _contacts.value = found
        }
    }

    fun start(peer: String, onReady: (String, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { secret.start(peer) }
                .onSuccess { id -> kotlinx.coroutines.withContext(Dispatchers.Main) { onReady(id, peer) } }
                .onFailure {
                    android.util.Log.e("SecretChats", "No se pudo crear el chat secreto", it)
                    _error.value = tr("No se pudo crear el chat secreto")
                }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretChatsScreen(navController: NavController, viewModel: SecretChatsViewModel = hiltViewModel()) {
    val chats by viewModel.chats.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val error by viewModel.error.collectAsState()
    var picking by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Chats secretos")) },
                navigationIcon = {
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusTokens.Color.BgBase)
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                viewModel.loadContacts()
                picking = true
            }) { Icon(Icons.Default.Add, tr("Nuevo chat secreto")) }
        },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                Text(
                    tr("Sus claves solo existen en los dos teléfonos. Nada se guarda en la nube, no se pueden hacer capturas y al cambiar de teléfono el chat no se recupera."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextSecondary,
                    modifier = Modifier.padding(16.dp)
                )
                error?.let { Text(it, color = NexusTokens.Color.Error, modifier = Modifier.padding(horizontal = 16.dp)) }
            }
            items(chats, key = { it.id }) { chat ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate("secret_chat/${chat.id}/${chat.peerId}") }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(chat.peerName.ifBlank { "?" }, chat.peerPhoto, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        chat.peerName.ifBlank { tr("Sin nombre") },
                        style = MaterialTheme.typography.titleMedium,
                        color = NexusTokens.Color.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Outlined.Lock, null, tint = NexusTokens.Color.Online, modifier = Modifier.size(18.dp))
                }
            }
        }
    }

    if (picking) {
        ModalBottomSheet(onDismissRequest = { picking = false }, containerColor = NexusTokens.Color.BgSurface) {
            Column(Modifier.padding(bottom = 24.dp)) {
                Text(
                    tr("Elige con quién"),
                    style = MaterialTheme.typography.titleMedium,
                    color = NexusTokens.Color.TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                if (contacts.isEmpty()) {
                    Text(
                        tr("Empieza antes una conversación normal con esa persona."),
                        color = NexusTokens.Color.TextMuted,
                        modifier = Modifier.padding(20.dp)
                    )
                }
                contacts.forEach { contact ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                picking = false
                                viewModel.start(contact.uid) { id, peer -> navController.navigate("secret_chat/$id/$peer") }
                            }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        UserAvatar(contact.name.ifBlank { "?" }, contact.photo, size = 40.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(contact.name, color = NexusTokens.Color.TextPrimary)
                    }
                }
            }
        }
    }
}

@HiltViewModel
class SecretChatViewModel @Inject constructor(
    private val secret: SecretChatRepository
) : ViewModel() {
    private val _status = MutableStateFlow(SecretChatStatus.ESTABLISHING)
    val status = _status.asStateFlow()
    private val _timer = MutableStateFlow(0L)
    val timer = _timer.asStateFlow()
    private var chatId = ""

    fun messages(id: String): StateFlow<List<SecretMessage>> = secret.messages(id)

    fun open(id: String, peer: String) {
        if (chatId == id) return
        chatId = id
        _timer.value = secret.timerSeconds(id)
        viewModelScope.launch { secret.connect(id, peer).collect { _status.value = it } }
        // Los mensajes con temporizador desaparecen aunque la pantalla siga abierta.
        viewModelScope.launch {
            while (true) {
                secret.purgeExpired(id)
                delay(1_000)
            }
        }
    }

    fun send(text: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { secret.send(chatId, text) } }
    }

    fun setTimer(seconds: Long) {
        secret.setTimer(chatId, seconds)
        _timer.value = seconds
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            secret.delete(chatId)
            kotlinx.coroutines.withContext(Dispatchers.Main) { onDone() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecretChatScreen(
    navController: NavController,
    chatId: String,
    peerId: String,
    viewModel: SecretChatViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    // Sin capturas ni grabación de pantalla mientras esté abierto.
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
    LaunchedEffect(chatId) { viewModel.open(chatId, peerId) }

    val messages by viewModel.messages(chatId).collectAsState()
    val status by viewModel.status.collectAsState()
    val timer by viewModel.timer.collectAsState()
    var draft by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val list = rememberLazyListState()
    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Chat secreto")) },
                navigationIcon = {
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"))
                    }
                },
                actions = {
                    Box {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { menu = true }) {
                            Icon(
                                Icons.Outlined.Timer,
                                tr("Temporizador"),
                                tint = if (timer > 0) MaterialTheme.colorScheme.primary else NexusTokens.Color.TextSecondary
                            )
                        }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            listOf(0L to "Sin temporizador", 60L to "1 minuto", 3_600L to "1 hora", 86_400L to "1 día").forEach { (seconds, label) ->
                                DropdownMenuItem(text = { Text(tr(label)) }, onClick = {
                                    viewModel.setTimer(seconds)
                                    menu = false
                                })
                            }
                        }
                    }
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Outlined.DeleteOutline, tr("Eliminar chat"), tint = NexusTokens.Color.TextSecondary)
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
            if (status != SecretChatStatus.READY) {
                Text(
                    if (status == SecretChatStatus.FAILED) tr("No se pudo abrir este chat en este teléfono.")
                    else tr("Esperando a que la otra persona abra el chat para acordar las claves…"),
                    style = MaterialTheme.typography.bodySmall,
                    color = NexusTokens.Color.Warning,
                    modifier = Modifier.padding(12.dp)
                )
            }
            LazyColumn(
                state = list,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(messages, key = { it.id }) { SecretBubble(it) }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BasicTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    enabled = status == SecretChatStatus.READY,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = NexusTokens.Color.TextPrimary),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    maxLines = 5,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(22.dp))
                        .background(NexusTokens.Color.BgElevated)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (draft.isEmpty()) Text(tr("Mensaje secreto"), color = NexusTokens.Color.TextMuted)
                        inner()
                    }
                )
                com.Azelmods.App.ui.components.AnimatedIconButton(
                    onClick = {
                        viewModel.send(draft.trim())
                        draft = ""
                    },
                    enabled = draft.isNotBlank() && status == SecretChatStatus.READY
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, tr("Enviar"), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("¿Eliminar este chat secreto?")) },
            text = { Text(tr("Se borrará de este teléfono y no se podrá recuperar.")) },
            confirmButton = {
                TextButton(onClick = { viewModel.delete { navController.popBackStack() } }) {
                    Text(tr("Eliminar"), color = NexusTokens.Color.Error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }
}

@Composable
private fun SecretBubble(message: SecretMessage) {
    val time = remember(message.at) { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.at)) }
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.mine) Arrangement.End else Arrangement.Start
    ) {
        Column(
            Modifier
                .widthIn(max = 300.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (message.mine) MaterialTheme.colorScheme.primaryContainer else NexusTokens.Color.BgElevated)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(message.text, style = MaterialTheme.typography.bodyLarge, color = NexusTokens.Color.TextPrimary)
            Text(time, style = MaterialTheme.typography.labelSmall, color = NexusTokens.Color.TextMuted)
        }
    }
}
