package com.Azelmods.App.ui.screens.chat

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.data.security.tor.AnonymityPolicy
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.components.AnimatedIconButton
import com.Azelmods.App.ui.components.ChatLook
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject

// ── Vista previa de enlaces ──────────────────────────────────────────────────

/** Separador invisible: lo que va detrás no se muestra como texto del mensaje. */
private const val LINK_MARK = "⁣link:"

data class LinkPreview(val url: String, val title: String, val description: String) {
    val domain: String get() = runCatching { Uri.parse(url).host.orEmpty().removePrefix("www.") }.getOrDefault("")
}

private val URL_PATTERN = Regex("""https://[^\s<>"']+""", RegexOption.IGNORE_CASE)

fun firstUrl(text: String): String? = URL_PATTERN.find(text)?.value?.trimEnd('.', ',', ')', ']', '!', '?', ';', ':')

fun appendLinkPreview(text: String, preview: LinkPreview): String =
    text + LINK_MARK + JSONObject()
        .put("u", preview.url)
        .put("t", preview.title)
        .put("d", preview.description)
        .toString()

/** El texto del mensaje sin la vista previa. */
fun withoutLinkPreview(text: String): String = text.substringBefore(LINK_MARK)

fun decodeLinkPreview(text: String): LinkPreview? {
    if (!text.contains(LINK_MARK)) return null
    return runCatching {
        val json = JSONObject(text.substringAfter(LINK_MARK))
        LinkPreview(json.getString("u"), json.optString("t"), json.optString("d")).takeIf { it.title.isNotBlank() }
    }.getOrNull()
}

/** Saca título y descripción del HTML de la página. */
fun parseLinkPreview(url: String, html: String): LinkPreview? {
    fun meta(property: String): String? {
        val forward = Regex(
            """<meta[^>]+(?:property|name)=["']$property["'][^>]*content=["']([^"']*)["']""",
            RegexOption.IGNORE_CASE
        )
        val backward = Regex(
            """<meta[^>]+content=["']([^"']*)["'][^>]*(?:property|name)=["']$property["']""",
            RegexOption.IGNORE_CASE
        )
        return (forward.find(html) ?: backward.find(html))?.groupValues?.get(1)?.trim()?.takeIf { it.isNotEmpty() }
    }
    val titleTag = Regex("""<title[^>]*>([^<]*)</title>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)?.trim()
    val title = meta("og:title") ?: titleTag?.takeIf { it.isNotEmpty() } ?: return null
    val description = meta("og:description") ?: meta("description").orEmpty()
    return LinkPreview(url, decodeEntities(title).take(120), decodeEntities(description).take(200))
}

private fun decodeEntities(text: String): String = text
    .replace("&amp;", "&")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace("&apos;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&nbsp;", " ")

/**
 * Quien envía prepara la vista previa y va dentro del mensaje cifrado: así quien lo recibe no se
 * conecta a la web del enlace ni descubre su IP a ese sitio. En modo anónimo no se abre el enlace.
 */
object LinkPreviews {
    private const val MAX_WAIT_MS = 2_000L
    private const val MAX_BYTES = 256L * 1024

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(MAX_WAIT_MS, TimeUnit.MILLISECONDS)
            .readTimeout(MAX_WAIT_MS, TimeUnit.MILLISECONDS)
            .followSslRedirects(true)
            .followRedirects(true)
            .build()
    }

    suspend fun attachTo(text: String): String {
        if (!ChatLook.linkPreviews.value || AnonymityPolicy.active) return text
        // Encuestas y contactos llevan su propio formato.
        if (decodePoll(text) != null || decodeContact(text) != null) return text
        val url = firstUrl(text) ?: return text
        val preview = withTimeoutOrNull(MAX_WAIT_MS) {
            withContext(Dispatchers.IO) { runCatching { fetch(url) }.getOrNull() }
        } ?: return text
        return appendLinkPreview(text, preview)
    }

    private fun fetch(url: String): LinkPreview? {
        val request = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0 (compatible; NexusChat)").build()
        client.newCall(request).execute().use { response ->
            val body = response.body ?: return null
            if (!response.isSuccessful || body.contentType()?.subtype?.contains("html") != true) return null
            val html = body.source().let { source ->
                source.request(MAX_BYTES)
                source.buffer.clone().readUtf8()
            }
            return parseLinkPreview(url, html)
        }
    }
}

@Composable
fun LinkPreviewCard(preview: LinkPreview) {
    val uriHandler = LocalUriHandler.current
    Row(
        Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.md))
            .background(NexusTokens.Color.BgDeep.copy(alpha = 0.35f))
            .clickable { runCatching { uriHandler.openUri(preview.url) } }
            .height(IntrinsicSize.Min)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
        Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(
                preview.title,
                style = MaterialTheme.typography.titleSmall,
                color = NexusTokens.Color.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (preview.description.isNotBlank()) {
                Text(
                    preview.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = NexusTokens.Color.TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(preview.domain, style = MaterialTheme.typography.labelSmall, color = NexusTokens.Color.TextMuted, maxLines = 1)
        }
    }
}

// ── Compartir contacto ───────────────────────────────────────────────────────

private const val CONTACT_MARK = "⁣contact:"

data class SharedContact(val uid: String, val name: String, val photo: String?)

fun encodeContact(contact: SharedContact): String =
    CONTACT_MARK + JSONObject().put("u", contact.uid).put("n", contact.name).put("p", contact.photo.orEmpty()).toString()

fun decodeContact(text: String): SharedContact? {
    if (!text.startsWith(CONTACT_MARK)) return null
    return runCatching {
        val json = JSONObject(text.removePrefix(CONTACT_MARK))
        SharedContact(json.getString("u"), json.getString("n"), json.optString("p").ifBlank { null })
    }.getOrNull()
}

@Composable
fun ContactCard(contact: SharedContact) {
    val context = LocalContext.current
    Column(Modifier.padding(10.dp).widthIn(min = 200.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            UserAvatar(contact.name, contact.photo, size = 44.dp)
            Spacer(Modifier.width(10.dp))
            Text(
                contact.name,
                style = MaterialTheme.typography.titleSmall,
                color = NexusTokens.Color.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(onClick = {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse("nexuschat://profile/${contact.uid}")).setPackage(context.packageName)
            )
        }) { Text(tr("Ver perfil")) }
    }
}

// ── Reenviar y elegir contacto ───────────────────────────────────────────────

data class ChatTarget(val chatId: String, val title: String, val photo: String?, val peerUid: String?)

@HiltViewModel
class ChatTargetsViewModel @Inject constructor(
    private val repository: RealtimeDatabaseRepository
) : ViewModel() {
    private val _targets = MutableStateFlow<List<ChatTarget>>(emptyList())
    val targets = _targets.asStateFlow()

    fun load() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _targets.value = runCatching { repository.getUserChats(uid).first() }.getOrDefault(emptyList())
                .mapNotNull { chat ->
                    val chatId = chat["chatId"] as? String ?: return@mapNotNull null
                    if (chatId.startsWith("group_")) {
                        ChatTarget(chatId, (chat["groupName"] as? String).orEmpty().ifBlank { tr("Grupo") }, null, null)
                    } else {
                        val peer = (chat["members"] as? Map<*, *>)?.keys?.filterIsInstance<String>()?.firstOrNull { it != uid }
                            ?: return@mapNotNull null
                        val user = runCatching { repository.getUserById(peer) }.getOrNull() ?: return@mapNotNull null
                        val name = (user["displayName"] as? String)?.ifBlank { null } ?: (user["name"] as? String).orEmpty()
                        ChatTarget(chatId, name, (user["photoUrl"] as? String)?.ifBlank { null }, peer)
                    }
                }
        }
    }

    /** Se cifra de nuevo para cada destino: es un envío normal, no una copia de lo que había en el servidor. */
    fun forward(messages: List<Message>, target: ChatTarget, onDone: (Boolean) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val ok = runCatching {
                messages.sortedBy { it.timestamp }.forEach { message ->
                    val media = message.mediaUrl?.takeIf { it.isNotBlank() }
                    when {
                        media != null && message.mediaType == "STICKER" -> repository.sendStickerMessage(target.chatId, media)
                        media != null && message.mediaType != null ->
                            repository.sendMediaMessage(target.chatId, media, message.mediaType!!, message.content)
                        message.content.isNotBlank() -> repository.sendMessage(target.chatId, message.content)
                    }
                }
            }.isSuccess
            withContext(Dispatchers.Main) { onDone(ok) }
        }
    }
}

@Composable
private fun TargetList(title: String, targets: List<ChatTarget>, onDismiss: () -> Unit, onPick: (ChatTarget) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            if (targets.isEmpty()) {
                Text(tr("Todavía no hay chats a los que enviarlo"), color = NexusTokens.Color.TextSecondary)
            } else {
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(targets, key = { it.chatId }) { target ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onPick(target) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            UserAvatar(target.title, target.photo, size = 40.dp)
                            Spacer(Modifier.width(12.dp))
                            Text(target.title, color = NexusTokens.Color.TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

@Composable
fun ForwardDialog(messages: List<Message>, onDismiss: () -> Unit, viewModel: ChatTargetsViewModel = hiltViewModel()) {
    val targets by viewModel.targets.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.load() }
    TargetList(tr("Reenviar a"), targets, onDismiss) { target ->
        viewModel.forward(messages, target) { ok ->
            android.widget.Toast.makeText(
                context,
                if (ok) tr("Reenviado a %s", target.title) else tr("No se pudo reenviar"),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
        onDismiss()
    }
}

@Composable
fun ContactPickerDialog(onDismiss: () -> Unit, onPick: (SharedContact) -> Unit, viewModel: ChatTargetsViewModel = hiltViewModel()) {
    val targets by viewModel.targets.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }
    TargetList(tr("Compartir contacto"), targets.filter { it.peerUid != null }, onDismiss) { target ->
        onPick(SharedContact(target.peerUid.orEmpty(), target.title, target.photo))
    }
}

// ── Selección múltiple ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionTopBar(
    count: Int,
    onClose: () -> Unit,
    onCopy: () -> Unit,
    onForward: () -> Unit,
    onDelete: () -> Unit
) {
    TopAppBar(
        title = { Text(count.toString(), color = NexusTokens.Color.TextPrimary) },
        navigationIcon = {
            AnimatedIconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"), tint = NexusTokens.Color.TextPrimary)
            }
        },
        actions = {
            AnimatedIconButton(onClick = onCopy) {
                Icon(Icons.Outlined.ContentCopy, tr("Copiar"), tint = NexusTokens.Color.TextPrimary)
            }
            AnimatedIconButton(onClick = onForward) {
                Icon(Icons.AutoMirrored.Outlined.Forward, tr("Reenviar"), tint = NexusTokens.Color.TextPrimary)
            }
            AnimatedIconButton(onClick = onDelete) {
                Icon(Icons.Outlined.DeleteOutline, tr("Eliminar para mí"), tint = NexusTokens.Color.TextPrimary)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusTokens.Color.BgSurface)
    )
}

// ── Mensajes programados ─────────────────────────────────────────────────────

data class ScheduledEntry(val workId: String, val chatId: String, val text: String, val at: Long)

/** Lleva la cuenta de lo programado para poder enseñarlo y cancelarlo antes de que salga. */
object ScheduledMessages {
    private const val PREFS = "scheduled_messages"
    private const val KEY = "list"

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun read(context: Context): List<ScheduledEntry> = runCatching {
        val array = JSONArray(prefs(context).getString(KEY, "[]"))
        List(array.length()) { index ->
            val item = array.getJSONObject(index)
            ScheduledEntry(item.getString("w"), item.getString("c"), item.getString("t"), item.getLong("a"))
        }
    }.getOrDefault(emptyList())

    private fun write(context: Context, list: List<ScheduledEntry>) {
        val array = JSONArray()
        list.forEach { array.put(JSONObject().put("w", it.workId).put("c", it.chatId).put("t", it.text).put("a", it.at)) }
        prefs(context).edit().putString(KEY, array.toString()).apply()
    }

    fun add(context: Context, entry: ScheduledEntry) = write(context, read(context) + entry)

    fun remove(context: Context, workId: String) = write(context, read(context).filterNot { it.workId == workId })

    fun forChat(context: Context, chatId: String): List<ScheduledEntry> =
        read(context).filter { it.chatId == chatId && it.at > System.currentTimeMillis() }.sortedBy { it.at }

    fun cancel(context: Context, entry: ScheduledEntry) {
        runCatching { WorkManager.getInstance(context).cancelWorkById(UUID.fromString(entry.workId)) }
        remove(context, entry.workId)
    }
}

@Composable
fun ScheduledMessagesDialog(chatId: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var entries by remember { mutableStateOf(ScheduledMessages.forChat(context, chatId)) }
    val format = remember { SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Mensajes programados")) },
        text = {
            if (entries.isEmpty()) {
                Text(tr("No hay mensajes programados en este chat"), color = NexusTokens.Color.TextSecondary)
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(entries, key = { it.workId }) { entry ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(entry.text, color = NexusTokens.Color.TextPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                Text(format.format(Date(entry.at)), style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted)
                            }
                            AnimatedIconButton(onClick = {
                                ScheduledMessages.cancel(context, entry)
                                entries = ScheduledMessages.forChat(context, chatId)
                            }) {
                                Icon(Icons.Outlined.Close, tr("Cancelar"), tint = NexusTokens.Color.TextMuted)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

// ── Menciones ────────────────────────────────────────────────────────────────

private val MENTION_PATTERN = Regex("""(?<![\p{L}\p{N}_])@[\p{L}\p{N}_.\-]+( [\p{Lu}][\p{L}\p{N}_.\-]*)?""")

/** Colorea las menciones; el resto del texto queda igual. */
fun highlightMentions(text: String, color: androidx.compose.ui.graphics.Color): androidx.compose.ui.text.AnnotatedString {
    val builder = androidx.compose.ui.text.AnnotatedString.Builder(text)
    MENTION_PATTERN.findAll(text).forEach { match ->
        builder.addStyle(
            androidx.compose.ui.text.SpanStyle(
                color = color,
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
            ),
            match.range.first,
            match.range.last + 1
        )
    }
    return builder.toAnnotatedString()
}

/** Lo que va escrito tras la última @, si se está escribiendo una mención; si no, null. */
fun currentMentionQuery(text: String): String? {
    val at = text.lastIndexOf('@')
    if (at < 0 || (at > 0 && !text[at - 1].isWhitespace())) return null
    val query = text.substring(at + 1)
    return query.takeIf { !it.contains('\n') && it.length <= 30 && !it.contains("  ") }
}

/** Cambia la mención a medias por el nombre elegido. */
fun applyMention(text: String, name: String): String = text.substring(0, text.lastIndexOf('@')) + "@" + name + " "
