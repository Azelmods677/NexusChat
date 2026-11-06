package com.Azelmods.App.ui.screens.chat

import android.content.Context
import android.util.Base64
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.data.security.encryption.DecryptionResult
import com.Azelmods.App.data.security.encryption.E2EECryptoService
import com.Azelmods.App.data.security.encryption.EncryptionResult
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ── Encuestas ────────────────────────────────────────────────────────────────

/**
 * Una encuesta viaja como un mensaje de texto normal, así que va cifrada como cualquier otro.
 * Este carácter invisible al principio distingue el contenido de un texto que parezca JSON.
 */
private const val POLL_MARK = "⁣poll:"

data class Poll(val question: String, val options: List<String>)

fun encodePoll(poll: Poll): String =
    POLL_MARK + JSONObject().put("q", poll.question).put("o", JSONArray(poll.options)).toString()

fun decodePoll(text: String): Poll? {
    if (!text.startsWith(POLL_MARK)) return null
    return runCatching {
        val json = JSONObject(text.removePrefix(POLL_MARK))
        val options = json.getJSONArray("o")
        Poll(json.getString("q"), List(options.length()) { options.getString(it) })
    }.getOrNull()
}

/** Texto para la lista de chats y los avisos, en vez del contenido interno de la encuesta. */
fun pollPreview(text: String): String? = decodePoll(text)?.let { "📊 ${it.question}" }
    ?: decodeContact(text)?.let { "👤 ${it.name}" }
    ?: withoutLinkPreview(text).takeIf { it != text }

/** Encuesta dentro de un mensaje. Los votos son públicos para los del chat y se ven en vivo. */
@Composable
fun PollCard(poll: Poll, chatId: String, messageId: String) {
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val ref = remember(chatId, messageId) { FirebaseDatabase.getInstance().getReference("chats/$chatId/pollVotes/$messageId") }
    var votes by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    DisposableEffect(ref) {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                votes = snapshot.children.mapNotNull { vote ->
                    val index = vote.getValue(Int::class.java) ?: return@mapNotNull null
                    vote.key?.let { it to index }
                }.toMap()
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }
    val total = votes.size
    val mine = votes[uid]

    Column(Modifier.widthIn(min = 240.dp).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(poll.question, style = MaterialTheme.typography.titleSmall, color = NexusTokens.Color.TextPrimary)
        poll.options.forEachIndexed { index, option ->
            val count = votes.values.count { it == index }
            val share = if (total == 0) 0f else count.toFloat() / total
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        // Tocar tu voto lo retira; tocar otra opción lo cambia.
                        if (mine == index) ref.child(uid).removeValue() else ref.child(uid).setValue(index)
                    }
                    .padding(vertical = 4.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (mine == index) {
                        Icon(Icons.Default.Check, tr("Tu voto"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(option, style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextPrimary, modifier = Modifier.weight(1f))
                    Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = NexusTokens.Color.TextSecondary)
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { share }, modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)))
            }
        }
        Text(tr("%d votos", total), style = MaterialTheme.typography.labelSmall, color = NexusTokens.Color.TextMuted)
    }
}

@Composable
fun PollCreateDialog(onDismiss: () -> Unit, onCreate: (Poll) -> Unit) {
    var question by remember { mutableStateOf("") }
    var options by remember { mutableStateOf(listOf("", "")) }
    val filled = options.map { it.trim() }.filter { it.isNotEmpty() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Crear encuesta")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(question, { question = it.take(200) }, label = { Text(tr("Pregunta")) })
                options.forEachIndexed { index, value ->
                    OutlinedTextField(
                        value = value,
                        onValueChange = { new -> options = options.toMutableList().also { it[index] = new.take(100) } },
                        label = { Text(tr("Opción %d", index + 1)) },
                        singleLine = true
                    )
                }
                if (options.size < MAX_POLL_OPTIONS) {
                    TextButton(onClick = { options = options + "" }) { Text(tr("Añadir opción")) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = question.isNotBlank() && filled.size >= 2,
                onClick = { onCreate(Poll(question.trim(), filled)) }
            ) { Text(tr("Enviar")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

private const val MAX_POLL_OPTIONS = 8

// ── Mensajes programados ─────────────────────────────────────────────────────

/** Envía un mensaje a la hora elegida, aunque la app esté cerrada. */
class ScheduledMessageWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface Dependencies {
        fun databaseRepository(): RealtimeDatabaseRepository
    }

    override suspend fun doWork(): Result {
        val chatId = inputData.getString(KEY_CHAT) ?: return Result.failure()
        val text = inputData.getString(KEY_TEXT) ?: return Result.failure()
        ScheduledMessages.remove(applicationContext, id.toString())
        val repository = EntryPointAccessors.fromApplication(applicationContext, Dependencies::class.java).databaseRepository()
        return runCatching { repository.sendMessage(chatId, text) }
            .fold(onSuccess = { Result.success() }, onFailure = { if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure() })
    }

    companion object {
        private const val KEY_CHAT = "chatId"
        private const val KEY_TEXT = "text"
        private const val MAX_ATTEMPTS = 3

        fun schedule(context: Context, chatId: String, text: String, atMillis: Long) {
            val request = OneTimeWorkRequestBuilder<ScheduledMessageWorker>()
                .setInitialDelay((atMillis - System.currentTimeMillis()).coerceAtLeast(0L), TimeUnit.MILLISECONDS)
                .setInputData(Data.Builder().putString(KEY_CHAT, chatId).putString(KEY_TEXT, text).build())
                .build()
            WorkManager.getInstance(context).enqueue(request)
            ScheduledMessages.add(context, ScheduledEntry(request.id.toString(), chatId, text, atMillis))
        }
    }
}

@Composable
fun ScheduleMessageDialog(text: String, chatId: String, onDismiss: () -> Unit, onScheduled: (String) -> Unit) {
    val context = LocalContext.current
    fun at(hour: Int, nextDay: Boolean) = Calendar.getInstance().apply {
        if (nextDay) add(Calendar.DAY_OF_YEAR, 1)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
    }.timeInMillis

    val now = System.currentTimeMillis()
    val options = buildList {
        add(tr("En 1 hora") to now + TimeUnit.HOURS.toMillis(1))
        add(tr("En 3 horas") to now + TimeUnit.HOURS.toMillis(3))
        if (at(21, false) > now) add(tr("Esta noche a las 21:00") to at(21, false))
        add(tr("Mañana a las 9:00") to at(9, true))
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Programar mensaje")) },
        text = {
            Column {
                Text(text, style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextSecondary, maxLines = 3)
                Spacer(Modifier.height(8.dp))
                options.forEach { (label, time) ->
                    TextButton(onClick = {
                        ScheduledMessageWorker.schedule(context, chatId, text, time)
                        onScheduled(SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(time)))
                    }) { Text(label) }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

// ── Mensaje fijado ───────────────────────────────────────────────────────────

/** Devuelve el id del mensaje fijado de un chat y lo mantiene al día. */
@Composable
fun rememberPinnedMessageId(chatId: String): String? {
    var pinned by remember(chatId) { mutableStateOf<String?>(null) }
    DisposableEffect(chatId) {
        if (chatId.isBlank()) return@DisposableEffect onDispose { }
        val ref = FirebaseDatabase.getInstance().getReference("chats/$chatId/pinnedMessageId")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                pinned = snapshot.getValue(String::class.java)
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addValueEventListener(listener)
        onDispose { ref.removeEventListener(listener) }
    }
    return pinned
}

fun setPinnedMessage(chatId: String, messageId: String?) {
    FirebaseDatabase.getInstance().getReference("chats/$chatId/pinnedMessageId").setValue(messageId)
}

@Composable
fun PinnedMessageBanner(preview: String, onOpen: () -> Unit, onUnpin: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(NexusTokens.Color.BgSurface)
            .clickable(onClick = onOpen)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.PushPin, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("Mensaje fijado"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            Text(preview, style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextPrimary, maxLines = 1)
        }
        IconButton(onClick = onUnpin) {
            Icon(Icons.Default.Close, tr("Quitar"), tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(18.dp))
        }
    }
}

// ── Número de seguridad ──────────────────────────────────────────────────────

/**
 * Comparar este número en persona o por otra vía confirma que nadie se ha colado en medio.
 * Es el mismo en los dos teléfonos: se calcula sobre las dos claves públicas, en orden fijo.
 */
@Composable
fun SafetyNumberDialog(peerId: String, onDismiss: () -> Unit, viewModel: SafetyNumberViewModel = hiltViewModel()) {
    LaunchedEffect(peerId) { viewModel.load(peerId) }
    val number by viewModel.number.collectAsState()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Número de seguridad")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    tr("Compáralo con la otra persona, en persona o por otra vía. Si coincide, nadie está interceptando este chat."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextSecondary
                )
                Text(
                    number ?: tr("La otra persona aún no tiene claves de cifrado."),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = NexusTokens.Color.TextPrimary
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

@HiltViewModel
class SafetyNumberViewModel @Inject constructor(
    private val e2ee: E2EECryptoService
) : ViewModel() {
    private val _number = MutableStateFlow<String?>(null)
    val number = _number.asStateFlow()

    fun load(peerId: String) {
        viewModelScope.launch(Dispatchers.IO) { _number.value = runCatching { e2ee.safetyNumber(peerId) }.getOrNull() }
    }
}

// ── Mensajes guardados ───────────────────────────────────────────────────────

data class SavedMessage(val id: String, val text: String, val at: Long)

@HiltViewModel
class SavedMessagesViewModel @Inject constructor(
    private val e2ee: E2EECryptoService
) : ViewModel() {
    private val uid get() = FirebaseAuth.getInstance().currentUser?.uid
    private val _items = MutableStateFlow<List<SavedMessage>>(emptyList())
    val items = _items.asStateFlow()

    init {
        viewModelScope.launch { observe().collect { _items.value = it } }
    }

    private fun observe(): Flow<List<SavedMessage>> = callbackFlow {
        val me = uid
        if (me == null) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val ref = FirebaseDatabase.getInstance().getReference("savedMessages/$me").orderByChild("at")
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                launch(Dispatchers.IO) {
                    trySend(snapshot.children.mapNotNull { node ->
                        val id = node.key ?: return@mapNotNull null
                        val sealed = node.child("c").getValue(String::class.java) ?: return@mapNotNull null
                        val opened = e2ee.decryptFrom(me, Base64.decode(sealed, Base64.NO_WRAP))
                        SavedMessage(
                            id,
                            (opened as? DecryptionResult.Success)?.plaintext ?: tr("🔒 No se pudo descifrar"),
                            node.child("at").getValue(Long::class.java) ?: 0L
                        )
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) = Unit
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /** Se cifra para uno mismo: el servidor guarda la nota sin poder leerla. */
    fun save(text: String) {
        val me = uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                e2ee.ensureLocalKeys()
                val sealed = e2ee.encryptFor(me, text) as? EncryptionResult.Success ?: error("Sin claves")
                FirebaseDatabase.getInstance().getReference("savedMessages/$me").push().setValue(
                    mapOf("c" to Base64.encodeToString(sealed.ciphertext, Base64.NO_WRAP), "at" to ServerValue.TIMESTAMP)
                ).await()
            }
        }
    }

    fun delete(id: String) {
        val me = uid ?: return
        FirebaseDatabase.getInstance().getReference("savedMessages/$me/$id").removeValue()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedMessagesScreen(navController: NavController, viewModel: SavedMessagesViewModel = hiltViewModel()) {
    val items by viewModel.items.collectAsState()
    var draft by remember { mutableStateOf("") }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Mensajes guardados")) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusTokens.Color.BgBase)
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
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (items.isEmpty()) {
                    item {
                        Text(
                            tr("Tus notas y los mensajes que guardes. Solo tú puedes leerlos: se cifran para ti."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexusTokens.Color.TextSecondary
                        )
                    }
                }
                items(items, key = { it.id }) { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(NexusTokens.Color.BgSurface)
                            .padding(start = 12.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(item.text, style = MaterialTheme.typography.bodyLarge, color = NexusTokens.Color.TextPrimary)
                            Text(
                                SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date(item.at)),
                                style = MaterialTheme.typography.labelSmall,
                                color = NexusTokens.Color.TextMuted
                            )
                        }
                        IconButton(onClick = { viewModel.delete(item.id) }) {
                            Icon(Icons.Outlined.DeleteOutline, tr("Eliminar"), tint = NexusTokens.Color.TextMuted)
                        }
                    }
                }
            }
            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    placeholder = { Text(tr("Escribe una nota")) },
                    maxLines = 4,
                    modifier = Modifier.weight(1f)
                )
                IconButton(enabled = draft.isNotBlank(), onClick = {
                    viewModel.save(draft.trim())
                    draft = ""
                }) { Icon(Icons.AutoMirrored.Filled.Send, tr("Guardar"), tint = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}
