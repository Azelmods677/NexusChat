package com.Azelmods.App.ui.screens.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.Azelmods.App.data.chat.ChatId
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.screens.conversation.NewConversationViewModel
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface BroadcastState {
    data object Idle : BroadcastState
    data class Sending(val done: Int, val total: Int) : BroadcastState
    data class Done(val sent: Int, val failed: Int) : BroadcastState
}

/**
 * Difusión: el mismo mensaje a varios contactos. Cada uno lo recibe en su chat privado,
 * cifrado como cualquier otro, y no ve a quién más se envió.
 */
@HiltViewModel
class BroadcastViewModel @Inject constructor(
    private val database: RealtimeDatabaseRepository
) : ViewModel() {
    private val _state = MutableStateFlow<BroadcastState>(BroadcastState.Idle)
    val state = _state.asStateFlow()

    fun send(text: String, recipients: List<String>) {
        val me = FirebaseAuth.getInstance().currentUser?.uid ?: return
        if (text.isBlank() || recipients.isEmpty() || _state.value is BroadcastState.Sending) return
        viewModelScope.launch(Dispatchers.IO) {
            var failed = 0
            recipients.forEachIndexed { index, uid ->
                _state.value = BroadcastState.Sending(index, recipients.size)
                runCatching { database.sendMessage(ChatId.create(me, uid), text.trim()) }
                    .onFailure {
                        failed++
                        android.util.Log.w("Broadcast", "No se pudo enviar a un destinatario: ${it.message}")
                    }
            }
            _state.value = BroadcastState.Done(recipients.size - failed, failed)
        }
    }

    fun reset() { _state.value = BroadcastState.Idle }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BroadcastScreen(
    navController: NavController,
    contactsViewModel: NewConversationViewModel = hiltViewModel(),
    viewModel: BroadcastViewModel = hiltViewModel()
) {
    val contacts by contactsViewModel.state.collectAsState()
    val state by viewModel.state.collectAsState()
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var text by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    val visible = remember(contacts.contacts, query) {
        contacts.contacts.filter { user ->
            query.isBlank() || user.displayName.contains(query, true) || user.name.contains(query, true) || user.username.contains(query, true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(tr("Nueva difusión"))
                        Text(
                            if (selected.isEmpty()) tr("Elige destinatarios") else tr("%s destinatarios", selected.size.toString()),
                            style = MaterialTheme.typography.labelSmall,
                            color = NexusTokens.Color.TextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusTokens.Color.BgBase)
            )
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(NexusTokens.Color.BgSurface)
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(12.dp)
            ) {
                when (val current = state) {
                    is BroadcastState.Sending -> {
                        LinearProgressIndicator(
                            progress = { current.done.toFloat() / current.total.coerceAtLeast(1) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(tr("Enviando %s de %s…", (current.done + 1).toString(), current.total.toString()), style = MaterialTheme.typography.labelSmall)
                    }
                    is BroadcastState.Done -> {
                        Text(
                            if (current.failed == 0) tr("Enviado a %s personas.", current.sent.toString())
                            else tr("Enviado a %s; %s fallaron.", current.sent.toString(), current.failed.toString()),
                            color = if (current.failed == 0) NexusTokens.Color.Online else NexusTokens.Color.Warning,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    BroadcastState.Idle -> Unit
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text,
                        onValueChange = {
                            text = it.take(4_000)
                            if (state is BroadcastState.Done) viewModel.reset()
                        },
                        placeholder = { Text(tr("Mensaje para todos")) },
                        maxLines = 5,
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            viewModel.send(text, selected.toList())
                            text = ""
                        },
                        enabled = text.isNotBlank() && selected.isNotEmpty() && state !is BroadcastState.Sending
                    ) { Text(tr("Enviar")) }
                }
            }
        },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                Text(
                    tr("Cada persona lo recibe en su chat privado contigo y no ve a quién más se lo enviaste."),
                    style = MaterialTheme.typography.bodySmall,
                    color = NexusTokens.Color.TextSecondary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(tr("Buscar contacto")) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                )
                if (contacts.isLoading) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().padding(16.dp))
                } else if (contacts.contacts.isEmpty()) {
                    Text(
                        tr("Aún no tienes contactos a quienes enviar."),
                        color = NexusTokens.Color.TextMuted,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
            items(visible, key = { it.uid }) { user ->
                val checked = user.uid in selected
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { selected = if (checked) selected - user.uid else selected + user.uid }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(user.displayName.ifBlank { user.name }, user.photoUrl, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(user.displayName.ifBlank { user.name }, color = NexusTokens.Color.TextPrimary, maxLines = 1)
                        if (user.username.isNotBlank()) {
                            Text("@${user.username.removePrefix("@")}", style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted, maxLines = 1)
                        }
                    }
                    Checkbox(checked = checked, onCheckedChange = { selected = if (it) selected + user.uid else selected - user.uid })
                }
            }
        }
    }
}
