package com.Azelmods.App.ui.screens.group

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.components.AnimatedIconButton
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.security.SecureRandom
import javax.inject.Inject

data class GroupMember(val uid: String, val name: String, val handle: String, val photo: String?, val admin: Boolean)

data class GroupInfo(
    val name: String = "",
    val members: List<GroupMember> = emptyList(),
    val inviteCode: String? = null,
    val iAmAdmin: Boolean = false,
    val loading: Boolean = true
)

/** Lo que muestra el enlace antes de unirse. */
data class GroupInvite(val code: String, val chatId: String, val name: String)

object GroupLinks {
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    private const val CODE_LENGTH = 12
    private val random = SecureRandom()

    fun newCode(): String = buildString { repeat(CODE_LENGTH) { append(ALPHABET[random.nextInt(ALPHABET.length)]) } }

    fun linkFor(code: String) = "nexuschat://join/$code"

    /** Acepta el enlace entero o solo el código, como lo haya pegado la persona. */
    fun parseCode(input: String): String =
        input.trim().substringAfterLast('/').trim().uppercase().filter { it.isLetterOrDigit() }

    /** Un enlace abierto desde fuera espera aquí hasta que la pantalla de chats lo atiende. */
    val pending = MutableStateFlow<String?>(null)
}

/**
 * Grupos: miembros, administradores y enlace de invitación.
 *
 * Nunca se lee `chats/{id}` entero: ese nodo contiene todos los mensajes. El cifrado no se toca
 * aquí: GroupCrypto compara los miembros con quienes tienen la clave y crea una nueva en el
 * siguiente mensaje, así que quien sale no lee lo que venga después y quien entra no lee lo anterior.
 */
@HiltViewModel
class GroupViewModel @Inject constructor(
    private val repository: RealtimeDatabaseRepository
) : ViewModel() {
    private val root = FirebaseDatabase.getInstance().reference
    private val me get() = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    private val _info = MutableStateFlow(GroupInfo())
    val info = _info.asStateFlow()

    private val _contacts = MutableStateFlow<List<GroupMember>>(emptyList())
    val contacts = _contacts.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    fun clearMessage() { _message.value = null }

    fun load(chatId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val chat = root.child("chats").child(chatId)
                val creator = chat.child("createdBy").get().await().getValue(String::class.java)
                val admins = chat.child("admins").get().await().children.mapNotNull { it.key }.toSet()
                val adminSet = if (admins.isEmpty() && creator != null) setOf(creator) else admins
                val ids = chat.child("members").get().await().children.mapNotNull { it.key }
                val members = ids.map { uid -> memberOf(uid, uid in adminSet) }
                    .sortedWith(compareByDescending<GroupMember> { it.admin }.thenBy { it.name.lowercase() })
                _info.value = GroupInfo(
                    name = chat.child("groupName").get().await().getValue(String::class.java).orEmpty(),
                    members = members,
                    inviteCode = chat.child("inviteCode").get().await().getValue(String::class.java),
                    iAmAdmin = me in adminSet,
                    loading = false
                )
            }.onFailure {
                _info.value = _info.value.copy(loading = false)
                _message.value = tr("No se pudo cargar el grupo")
            }
        }
    }

    private suspend fun memberOf(uid: String, admin: Boolean): GroupMember {
        val user = runCatching { repository.getUserById(uid) }.getOrNull()
        return GroupMember(
            uid = uid,
            name = (user?.get("displayName") as? String)?.ifBlank { null } ?: (user?.get("name") as? String).orEmpty(),
            handle = (user?.get("username") as? String).orEmpty(),
            photo = (user?.get("photoUrl") as? String)?.ifBlank { null },
            admin = admin
        )
    }

    fun loadContacts() {
        val uid = me.ifBlank { return }
        viewModelScope.launch(Dispatchers.IO) {
            val current = _info.value.members.map { it.uid }.toSet()
            _contacts.value = runCatching { repository.getUserChats(uid).first() }.getOrDefault(emptyList())
                .filter { (it["chatId"] as? String)?.startsWith("group_") != true }
                .mapNotNull { chat -> (chat["members"] as? Map<*, *>)?.keys?.filterIsInstance<String>()?.firstOrNull { it != uid } }
                .distinct()
                .filter { it !in current }
                .map { memberOf(it, false) }
        }
    }

    private fun act(chatId: String, failure: String, block: suspend () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { block() }
                .onSuccess { load(chatId) }
                .onFailure {
                    android.util.Log.w("GroupViewModel", "$failure: ${it.message}")
                    _message.value = failure
                }
        }
    }

    fun rename(chatId: String, name: String) = act(chatId, tr("No se pudo cambiar el nombre")) {
        root.child("chats").child(chatId).child("groupName").setValue(name.trim()).await()
    }

    fun add(chatId: String, uid: String) = act(chatId, tr("No se pudo añadir a esa persona")) {
        val chat = root.child("chats").child(chatId)
        chat.child("removed").child(uid).removeValue().await()
        chat.child("members").child(uid).setValue(true).await()
        // Va después a propósito: la regla del índice comprueba que ya sea miembro.
        root.child("userChats").child(uid).child(chatId).setValue(true).await()
    }

    fun remove(chatId: String, uid: String) = act(chatId, tr("No se pudo quitar a esa persona")) {
        val chat = root.child("chats").child(chatId)
        root.updateChildren(
            mapOf<String, Any?>(
                "userChats/$uid/$chatId" to null,
                "chats/$chatId/admins/$uid" to null,
                "chats/$chatId/removed/$uid" to true,
                "chats/$chatId/joinCodes/$uid" to null
            )
        ).await()
        chat.child("members").child(uid).removeValue().await()
    }

    fun setAdmin(chatId: String, uid: String, admin: Boolean) = act(chatId, tr("No se pudo cambiar el administrador")) {
        val entry = root.child("chats").child(chatId).child("admins").child(uid)
        if (admin) ensureAdminsWritten(chatId)
        if (admin) entry.setValue(true).await() else entry.removeValue().await()
    }

    /** Los grupos antiguos solo guardan quién los creó: al primer cambio se deja escrito. */
    private suspend fun ensureAdminsWritten(chatId: String) {
        val chat = root.child("chats").child(chatId)
        if (chat.child("admins").get().await().exists()) return
        val creator = chat.child("createdBy").get().await().getValue(String::class.java) ?: return
        chat.child("admins").child(creator).setValue(true).await()
    }

    /** Crea el enlace, o lo cambia por uno nuevo: el anterior deja de servir. */
    fun resetInvite(chatId: String) = act(chatId, tr("No se pudo crear el enlace")) {
        val old = _info.value.inviteCode
        val code = GroupLinks.newCode()
        root.updateChildren(
            buildMap<String, Any?> {
                if (old != null) put("groupInvites/$old", null)
                put("groupInvites/$code", mapOf("chatId" to chatId, "name" to _info.value.name))
                put("chats/$chatId/inviteCode", code)
            }
        ).await()
    }

    fun leave(chatId: String, onLeft: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val info = _info.value
            val others = info.members.filter { it.uid != me }
            runCatching {
                val chat = root.child("chats").child(chatId)
                // Un grupo no se queda sin administrador si el único se va.
                if (info.iAmAdmin && others.isNotEmpty() && others.none { it.admin }) {
                    ensureAdminsWritten(chatId)
                    chat.child("admins").child(others.first().uid).setValue(true).await()
                }
                if (info.iAmAdmin) chat.child("admins").child(me).removeValue().await()
                root.child("userChats").child(me).child(chatId).removeValue().await()
                chat.child("members").child(me).removeValue().await()
            }.onSuccess {
                withContext(Dispatchers.Main) { onLeft() }
            }.onFailure {
                _message.value = tr("No se pudo salir del grupo")
            }
        }
    }

    suspend fun findInvite(code: String): GroupInvite? = withContext(Dispatchers.IO) {
        runCatching {
            val found = root.child("groupInvites").child(code).get().await()
            val chatId = found.child("chatId").getValue(String::class.java) ?: return@runCatching null
            GroupInvite(code, chatId, found.child("name").getValue(String::class.java).orEmpty())
        }.getOrNull()
    }

    /** La regla solo deja entrar si el código enviado es el vigente del grupo. */
    suspend fun join(invite: GroupInvite): Boolean = withContext(Dispatchers.IO) {
        val uid = me.ifBlank { return@withContext false }
        runCatching {
            root.updateChildren(
                mapOf(
                    "chats/${invite.chatId}/joinCodes/$uid" to invite.code,
                    "chats/${invite.chatId}/members/$uid" to true
                )
            ).await()
            root.child("userChats").child(uid).child(invite.chatId).setValue(true).await()
            true
        }.getOrElse {
            android.util.Log.w("GroupViewModel", "No se pudo entrar al grupo: ${it.message}")
            false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupInfoScreen(chatId: String, navController: NavController, viewModel: GroupViewModel = hiltViewModel()) {
    val info by viewModel.info.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val message by viewModel.message.collectAsState()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showRename by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }
    var showLeave by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<GroupMember?>(null) }
    val myUid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(chatId) { viewModel.load(chatId) }
    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(info.name.ifBlank { tr("Grupo") }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    AnimatedIconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"))
                    }
                },
                actions = {
                    if (info.iAmAdmin) {
                        AnimatedIconButton(onClick = { showRename = true }) {
                            Icon(Icons.Outlined.Edit, tr("Cambiar nombre"))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusTokens.Color.BgBase)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        if (info.loading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, strokeWidth = 2.dp)
            }
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize()) {
            item {
                NexusSectionHeader(tr("Enlace de invitación"))
                InviteCard(
                    code = info.inviteCode,
                    admin = info.iAmAdmin,
                    onCreate = { viewModel.resetInvite(chatId) },
                    onCopy = { code -> clipboard.setText(AnnotatedString(GroupLinks.linkFor(code))) },
                    onShare = { code ->
                        val text = tr("Únete a mi grupo de NexusChat: %s", GroupLinks.linkFor(code))
                        context.startActivity(
                            Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text), null)
                        )
                    }
                )
                NexusSectionHeader(tr("%s miembros", info.members.size.toString()))
                if (info.iAmAdmin) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.loadContacts()
                                showAdd = true
                            }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.PersonAdd, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(16.dp))
                        Text(tr("Añadir miembros"), color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            items(info.members, key = { it.uid }) { member ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (member.uid == myUid) return@clickable
                            if (info.iAmAdmin) selected = member else navController.navigate("profile_viewer/${member.uid}")
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(member.name, member.photo, size = 44.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (member.uid == myUid) tr("Tú") else member.name,
                            style = MaterialTheme.typography.bodyLarge,
                            color = NexusTokens.Color.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (member.handle.isNotBlank()) {
                            Text(
                                "@${member.handle.removePrefix("@")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = NexusTokens.Color.TextMuted,
                                maxLines = 1
                            )
                        }
                    }
                    if (member.admin) {
                        Text(
                            tr("Admin"),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .clip(RoundedCornerShape(NexusTokens.Radius.pill))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { showLeave = true }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, null, tint = NexusTokens.Color.Error)
                    Spacer(Modifier.width(16.dp))
                    Text(tr("Salir del grupo"), color = NexusTokens.Color.Error)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    if (showRename) {
        var name by remember { mutableStateOf(info.name) }
        AlertDialog(
            onDismissRequest = { showRename = false },
            title = { Text(tr("Cambiar nombre")) },
            text = { OutlinedTextField(name, { name = it.take(60) }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    viewModel.rename(chatId, name)
                    showRename = false
                }) { Text(tr("Guardar")) }
            },
            dismissButton = { TextButton(onClick = { showRename = false }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }

    if (showAdd) {
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text(tr("Añadir miembros")) },
            text = {
                if (contacts.isEmpty()) {
                    Text(tr("No hay más contactos para añadir"), color = NexusTokens.Color.TextSecondary)
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(contacts, key = { it.uid }) { contact ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.add(chatId, contact.uid)
                                        showAdd = false
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(contact.name, contact.photo, size = 40.dp)
                                Spacer(Modifier.width(12.dp))
                                Text(contact.name, color = NexusTokens.Color.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showAdd = false }) { Text(tr("Cerrar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }

    selected?.let { member ->
        AlertDialog(
            onDismissRequest = { selected = null },
            title = { Text(member.name) },
            text = {
                Column {
                    TextButton(onClick = {
                        selected = null
                        navController.navigate("profile_viewer/${member.uid}")
                    }) { Text(tr("Ver perfil")) }
                    TextButton(onClick = {
                        viewModel.setAdmin(chatId, member.uid, !member.admin)
                        selected = null
                    }) { Text(if (member.admin) tr("Quitar como administrador") else tr("Hacer administrador")) }
                    TextButton(onClick = {
                        viewModel.remove(chatId, member.uid)
                        selected = null
                    }) { Text(tr("Quitar del grupo"), color = NexusTokens.Color.Error) }
                }
            },
            confirmButton = { TextButton(onClick = { selected = null }) { Text(tr("Cerrar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }

    if (showLeave) {
        AlertDialog(
            onDismissRequest = { showLeave = false },
            title = { Text(tr("¿Salir del grupo?")) },
            text = { Text(tr("Dejarás de recibir sus mensajes y ya no podrás leerlos."), color = NexusTokens.Color.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    showLeave = false
                    viewModel.leave(chatId) { navController.popBackStack("main", inclusive = false) }
                }) { Text(tr("Salir"), color = NexusTokens.Color.Error) }
            },
            dismissButton = { TextButton(onClick = { showLeave = false }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }
}

@Composable
private fun InviteCard(
    code: String?,
    admin: Boolean,
    onCreate: () -> Unit,
    onCopy: (String) -> Unit,
    onShare: (String) -> Unit
) {
    Column(
        Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.BgSurface)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Link, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(
                code?.let(GroupLinks::linkFor)
                    ?: if (admin) tr("Crea un enlace para que entren sin añadirlos uno a uno")
                    else tr("El grupo todavía no tiene enlace"),
                style = MaterialTheme.typography.bodyMedium,
                color = if (code != null) NexusTokens.Color.TextPrimary else NexusTokens.Color.TextSecondary
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (code != null) {
                TextButton(onClick = { onCopy(code) }) {
                    Icon(Icons.Outlined.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tr("Copiar"))
                }
                TextButton(onClick = { onShare(code) }) {
                    Icon(Icons.Outlined.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(tr("Compartir"))
                }
            }
            if (admin) {
                TextButton(onClick = onCreate) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (code == null) tr("Crear") else tr("Cambiar"))
                }
            }
        }
    }
}

/** Pide el código, enseña a qué grupo pertenece y entra. Sirve también para los enlaces abiertos. */
@Composable
fun JoinGroupDialog(
    initialCode: String,
    onDismiss: () -> Unit,
    onJoined: (String) -> Unit,
    viewModel: GroupViewModel = hiltViewModel()
) {
    var input by remember { mutableStateOf(initialCode) }
    var invite by remember { mutableStateOf<GroupInvite?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun search() {
        val code = GroupLinks.parseCode(input)
        if (code.isEmpty()) return
        busy = true
        error = null
        scope.launch {
            invite = viewModel.findInvite(code)
            if (invite == null) error = tr("Ese enlace no existe o ya no sirve")
            busy = false
        }
    }
    LaunchedEffect(Unit) { if (initialCode.isNotBlank()) search() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Unirse a un grupo")) },
        text = {
            Column {
                val found = invite
                if (found != null) {
                    Text(found.name.ifBlank { tr("Grupo") }, style = MaterialTheme.typography.titleMedium, color = NexusTokens.Color.TextPrimary)
                } else {
                    OutlinedTextField(input, { input = it }, singleLine = true, label = { Text(tr("Enlace o código")) })
                }
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = NexusTokens.Color.Error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            val found = invite
            TextButton(enabled = !busy && (found != null || input.isNotBlank()), onClick = {
                if (found == null) {
                    search()
                } else {
                    busy = true
                    scope.launch {
                        if (viewModel.join(found)) {
                            onJoined(found.chatId)
                        } else {
                            error = tr("No se pudo entrar: el enlace cambió o te quitaron del grupo")
                            busy = false
                        }
                    }
                }
            }) { Text(if (found == null) tr("Buscar") else tr("Unirme")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}
