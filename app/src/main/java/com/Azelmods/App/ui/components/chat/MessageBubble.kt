package com.Azelmods.App.ui.components.chat

import com.Azelmods.App.i18n.tr

import android.content.Intent
import android.net.Uri
import android.view.WindowManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bookmark
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.automirrored.outlined.InsertDriveFile
import androidx.compose.material.icons.automirrored.outlined.Forward
import androidx.compose.material.icons.automirrored.outlined.Reply
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import coil3.compose.AsyncImage
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.ui.components.ReadReceiptIndicator
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BubbleMaxWidth = 300.dp
private val QuickReactions = listOf("❤️", "👍", "😂", "😮", "😢", "🙏")

/**
 * Burbuja de mensaje. Plana y legible: el color distingue lo enviado de lo recibido y no
 * hay sombras, brillos ni animaciones por mensaje.
 *
 * Tocar un medio lo abre. Mantener pulsado abre las acciones (reaccionar, responder,
 * copiar, traducir, editar, eliminar).
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    repliedMessage: Message? = null,
    autoplayVideos: Boolean = false,
    serverNow: () -> Long = { System.currentTimeMillis() },
    onReply: () -> Unit = {},
    onPin: (() -> Unit)? = null,
    onSave: (() -> Unit)? = null,
    onForward: (() -> Unit)? = null,
    onSelect: (() -> Unit)? = null,
    onReactionClick: (String) -> Unit = {},
    onImageClick: ((String, String, String) -> Unit)? = null,
    onDeleteClick: ((Boolean) -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onMessageViewed: (() -> Unit)? = null,
    onViewOnceClosed: (() -> Unit)? = null,
    translatedText: String? = null,
    isTranslating: Boolean = false,
    onTranslate: (() -> Unit)? = null
) {
    if (message.mediaType == "SYSTEM") {
        SystemNotice(message.content)
        return
    }

    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var showActions by remember { mutableStateOf(false) }
    var confirmDeleteForAll by remember { mutableStateOf(false) }
    var viewOnceOpen by remember { mutableStateOf(false) }
    val readableContent = remember(message.content) { com.Azelmods.App.ui.screens.chat.withoutLinkPreview(message.content) }

    val now = System.currentTimeMillis()
    val canEdit = isOwnMessage && now - message.timestamp < 15 * 60 * 1000 &&
        message.mediaType == null && !message.deletedForEveryone && !message.isEphemeral
    val canDeleteForEveryone = isOwnMessage && now - message.timestamp < 48 * 60 * 60 * 1000 &&
        !message.deletedForEveryone

    // Un mensaje temporal de texto o medio empieza su cuenta atrás cuando se ve en pantalla.
    // Los de una sola vista no: esos se marcan al abrirlos a propósito.
    val me = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
    LaunchedEffect(message.messageId, message.viewedAt) {
        if (message.isEphemeral && !message.isViewOnce && !isOwnMessage && message.viewedAt == 0L && me !in message.viewedBy) {
            delay(800)
            onMessageViewed?.invoke()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        if (message.deletedForEveryone) {
            Row(
                modifier = Modifier
                    .widthIn(max = BubbleMaxWidth)
                    .clip(RoundedCornerShape(NexusTokens.Radius.lg))
                    .background(NexusTokens.Color.BgSurface)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.Block, null, tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    tr("Mensaje eliminado"),
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = FontStyle.Italic,
                    color = NexusTokens.Color.TextMuted
                )
            }
            return@Column
        }

        val bubbleStyle by com.Azelmods.App.ui.components.ChatLook.bubbleStyle.collectAsState()
        val ownColor by com.Azelmods.App.ui.components.ChatLook.ownBubbleColor.collectAsState()
        val peerColor by com.Azelmods.App.ui.components.ChatLook.peerBubbleColor.collectAsState()
        val textScale by com.Azelmods.App.ui.components.ChatLook.textScale.collectAsState()
        val shape = com.Azelmods.App.ui.components.chatBubbleShape(bubbleStyle, isOwnMessage)
        val background = if (isOwnMessage) {
            ownColor?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
        } else {
            peerColor?.let { Color(it) } ?: NexusTokens.Color.BgElevated
        }
        val isEmojiOnly = message.mediaType == null && !message.isViewOnce && message.content.isEmojiOnly()
        val isSticker = message.mediaType == "STICKER" && !message.isViewOnce
        val edgeToEdgeMedia = message.mediaType in setOf("IMAGE", "GIF", "VIDEO", "GIF_VIDEO") && !message.isViewOnce

        Column(
            modifier = Modifier
                .widthIn(max = BubbleMaxWidth)
                .then(
                    if (isEmojiOnly || isSticker) {
                        Modifier.clip(shape)
                    } else {
                        com.Azelmods.App.ui.components.chatBubbleSurface(
                            bubbleStyle, shape, background, MaterialTheme.colorScheme.primary
                        )
                    }
                )
                .combinedClickable(
                    onClick = {
                        if (message.isViewOnce && !isOwnMessage) {
                            viewOnceOpen = true
                            onMessageViewed?.invoke()
                        }
                    },
                    onLongClick = { showActions = true }
                )
                .padding(if (edgeToEdgeMedia) 3.dp else 0.dp)
        ) {
            // En un grupo hay que saber quién habla, cada persona con su color.
            val groupSender = com.Azelmods.App.ui.components.LocalBubbleSender.current
            if (groupSender != null && !isOwnMessage) {
                val senderColor = remember(groupSender.first) {
                    com.Azelmods.App.ui.components.ProfileColors.forUser(groupSender.first).first()
                }
                Text(
                    groupSender.second,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    color = senderColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp)
                )
            }
            if (repliedMessage != null) ReplyQuote(repliedMessage, isOwnMessage)
            if (message.replyTo?.startsWith("story:") == true) StoryReplyQuote()

            if (message.isViewOnce) {
                ViewOncePlaceholder(isOwnMessage = isOwnMessage)
            } else {
                MediaContent(
                    message = message,
                    autoplayVideos = autoplayVideos,
                    onImageClick = { url ->
                        onImageClick?.invoke(
                            url,
                            if (isOwnMessage) tr("Tú") else tr("Contacto"),
                            formatTime(message.timestamp)
                        )
                    },
                    onLongPress = { showActions = true }
                )

                val poll = remember(message.content) { com.Azelmods.App.ui.screens.chat.decodePoll(message.content) }
                val sharedContact = remember(message.content) { com.Azelmods.App.ui.screens.chat.decodeContact(message.content) }
                val text = if (message.mediaType in setOf("DOCUMENT", "LOCATION", "STICKER")) "" else readableContent
                val linkPreview = remember(message.content) { com.Azelmods.App.ui.screens.chat.decodeLinkPreview(message.content) }
                if (linkPreview != null) com.Azelmods.App.ui.screens.chat.LinkPreviewCard(linkPreview)
                if (sharedContact != null) {
                    com.Azelmods.App.ui.screens.chat.ContactCard(sharedContact)
                } else if (poll != null) {
                    com.Azelmods.App.ui.screens.chat.PollCard(
                        poll = poll,
                        chatId = com.Azelmods.App.service.ActiveChat.chatId.orEmpty(),
                        messageId = message.messageId
                    )
                } else if (isSticker) {
                    com.Azelmods.App.ui.components.StickerImage(
                        sticker = message.mediaUrl?.takeIf { it.isNotBlank() } ?: message.content,
                        size = 140.dp,
                        modifier = Modifier.padding(4.dp)
                    )
                } else if (isEmojiOnly) {
                    // Hasta tres emojis se animan; más serían una fila enorme de animaciones.
                    val emojis = remember(text) { com.Azelmods.App.ui.components.splitGraphemes(text) }
                    if (emojis.size <= 3) {
                        Row(Modifier.padding(start = 6.dp, end = 6.dp, top = 6.dp)) {
                            emojis.forEach { com.Azelmods.App.ui.components.AnimatedEmoji(it, if (emojis.size == 1) 96.dp else 64.dp) }
                        }
                    } else {
                        Text(
                            text = text,
                            style = MaterialTheme.typography.displaySmall.copy(fontSize = 40.sp, lineHeight = 46.sp),
                            color = NexusTokens.Color.TextPrimary,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)
                        )
                    }
                } else if (text.isNotEmpty()) {
                    var expanded by remember(message.messageId) { mutableStateOf(false) }
                    val isLong = text.length > LONG_MESSAGE_CHARS
                    val mentionColor = MaterialTheme.colorScheme.primary
                    val styledText = remember(text, mentionColor) {
                        com.Azelmods.App.ui.screens.chat.highlightMentions(text, mentionColor)
                    }
                    Text(
                        text = styledText,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = (15 * textScale).sp, lineHeight = (21 * textScale).sp),
                        color = NexusTokens.Color.TextPrimary,
                        maxLines = if (isLong && !expanded) COLLAPSED_LINES else Int.MAX_VALUE,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)
                    )
                    if (isLong) {
                        Text(
                            if (expanded) tr("Leer menos") else tr("Leer más"),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(start = 12.dp, end = 12.dp, top = 4.dp)
                                .clickable { expanded = !expanded }
                        )
                    }
                }

                if (isTranslating) {
                    Text(
                        tr("Traduciendo…"),
                        style = MaterialTheme.typography.labelSmall,
                        color = NexusTokens.Color.TextMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                } else if (!translatedText.isNullOrBlank()) {
                    Text(
                        text = translatedText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = FontStyle.Italic,
                        color = NexusTokens.Color.TextSecondary,
                        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 6.dp)
                    )
                }
            }

            // Pie: temporizador, hora y confirmación de lectura
            Row(
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(start = 12.dp, end = 8.dp, top = 2.dp, bottom = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (message.isEphemeral && !message.isViewOnce) {
                    EphemeralCountdown(message, serverNow)
                    Spacer(Modifier.width(6.dp))
                }
                if (message.edited) {
                    Text(
                        tr("editado "),
                        style = MaterialTheme.typography.labelSmall,
                        color = NexusTokens.Color.TextMuted
                    )
                }
                Text(
                    text = formatTime(message.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = NexusTokens.Color.TextMuted
                )
                if (isOwnMessage) {
                    Spacer(Modifier.width(3.dp))
                    ReadReceiptIndicator(status = message.status)
                }
                val showBubblePhoto by com.Azelmods.App.ui.components.ChatLook.bubblePhoto.collectAsState()
                if (showBubblePhoto) {
                    val peer = com.Azelmods.App.ui.components.LocalBubblePeer.current
                    val me = com.Azelmods.App.data.profile.rememberMyProfile()
                    val author = if (isOwnMessage) me.name to me.photoUrl else peer
                    author?.let { (name, photo) ->
                        Spacer(Modifier.width(5.dp))
                        com.Azelmods.App.ui.components.UserAvatar(name, photo, size = 26.dp)
                    }
                }
            }
        }

        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                message.reactions.values.groupingBy { it }.eachCount().forEach { (emoji, count) ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(NexusTokens.Radius.pill))
                            .background(NexusTokens.Color.BgHigh)
                            .clickable { onReactionClick(emoji) }
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.Azelmods.App.ui.components.AnimatedEmoji(emoji, 18.dp, iterations = 1)
                        if (count > 1) {
                            Spacer(Modifier.width(3.dp))
                            Text(
                                count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = NexusTokens.Color.TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }

    if (showActions) {
        ModalBottomSheet(
            onDismissRequest = { showActions = false },
            containerColor = NexusTokens.Color.BgSurface,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickReactions.forEach { emoji ->
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .clickable {
                                onReactionClick(emoji)
                                showActions = false
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(emoji, fontSize = 26.sp)
                    }
                }
            }
            HorizontalDivider(
                color = NexusTokens.Color.Hairline,
                modifier = Modifier.padding(vertical = 6.dp)
            )
            ActionRow(Icons.AutoMirrored.Outlined.Reply, "Responder") {
                showActions = false
                onReply()
            }
            if (message.mediaType == "STICKER") {
                val stickers: com.Azelmods.App.ui.components.StickerViewModel = androidx.hilt.navigation.compose.hiltViewModel()
                ActionRow(Icons.Outlined.Bookmark, "Añadir a mis stickers") {
                    stickers.saveReceived(message.mediaUrl?.takeIf { it.isNotBlank() } ?: message.content)
                    showActions = false
                    android.widget.Toast.makeText(context, tr("Añadido a tus stickers"), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            if (message.content.isNotEmpty() && !message.isViewOnce) {
                ActionRow(Icons.Outlined.ContentCopy, "Copiar") {
                    clipboard.setText(AnnotatedString(readableContent))
                    showActions = false
                    android.widget.Toast.makeText(context, tr("Copiado"), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            if (onTranslate != null && message.content.isNotEmpty() && !message.isViewOnce) {
                ActionRow(Icons.Outlined.Translate, "Traducir") {
                    showActions = false
                    onTranslate()
                }
            }
            if (onForward != null && !message.isViewOnce) {
                ActionRow(Icons.AutoMirrored.Outlined.Forward, "Reenviar") {
                    showActions = false
                    onForward()
                }
            }
            if (onSelect != null) {
                ActionRow(Icons.Outlined.CheckCircle, "Seleccionar") {
                    showActions = false
                    onSelect()
                }
            }
            if (onPin != null && !message.isViewOnce) {
                ActionRow(Icons.Outlined.PushPin, "Fijar") {
                    showActions = false
                    onPin()
                }
            }
            if (onSave != null && message.content.isNotEmpty() && !message.isViewOnce) {
                ActionRow(Icons.Outlined.BookmarkBorder, "Guardar") {
                    showActions = false
                    onSave()
                    android.widget.Toast.makeText(context, tr("Guardado en Mensajes guardados"), android.widget.Toast.LENGTH_SHORT).show()
                }
            }
            if (canEdit) {
                ActionRow(Icons.Outlined.Edit, "Editar") {
                    showActions = false
                    onEditClick?.invoke()
                }
            }
            ActionRow(Icons.Outlined.DeleteOutline, "Eliminar para mí") {
                showActions = false
                onDeleteClick?.invoke(false)
            }
            if (canDeleteForEveryone) {
                ActionRow(Icons.Outlined.Delete, "Eliminar para todos", NexusTokens.Color.Error) {
                    showActions = false
                    confirmDeleteForAll = true
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }

    if (confirmDeleteForAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteForAll = false },
            title = { Text(tr("¿Eliminar para todos?")) },
            text = { Text(tr("Desaparecerá para todos en este chat. No se puede deshacer."), color = NexusTokens.Color.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteForAll = false
                    onDeleteClick?.invoke(true)
                }) { Text(tr("Eliminar"), color = NexusTokens.Color.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteForAll = false }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }

    if (viewOnceOpen) {
        ViewOnceViewer(
            message = message,
            onClose = {
                viewOnceOpen = false
                onViewOnceClosed?.invoke()
            }
        )
    }
}

@Composable
private fun SystemNotice(text: String) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = NexusTokens.Color.TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .clip(RoundedCornerShape(NexusTokens.Radius.pill))
                .background(NexusTokens.Color.BgSurface)
                .padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/** Tiempo restante; antes de que lo abran, cuánto durará. */
@Composable
private fun EphemeralCountdown(message: Message, serverNow: () -> Long) {
    var remaining by remember(message.messageId, message.expiresAt) {
        mutableLongStateOf(((message.expiresAt - serverNow()) / 1000).coerceAtLeast(0))
    }
    LaunchedEffect(message.messageId, message.expiresAt, message.isCountingDown) {
        if (!message.isCountingDown) return@LaunchedEffect
        while (remaining > 0) {
            delay(1000)
            remaining = ((message.expiresAt - serverNow()) / 1000).coerceAtLeast(0)
        }
    }
    Icon(
        Icons.Default.Timer,
        contentDescription = tr("Mensaje temporal"),
        tint = NexusTokens.Color.TextMuted,
        modifier = Modifier.size(11.dp)
    )
    Spacer(Modifier.width(2.dp))
    Text(
        text = formatCountdown(if (message.isCountingDown) remaining else message.selfDestructDuration),
        style = MaterialTheme.typography.labelSmall,
        color = NexusTokens.Color.TextMuted
    )
}

@Composable
private fun ViewOncePlaceholder(isOwnMessage: Boolean) {
    Row(
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            if (isOwnMessage) Icons.Default.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = null,
            tint = NexusTokens.Color.TextSecondary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = if (isOwnMessage) tr("Una sola vista") else tr("Toca para ver una vez"),
            style = MaterialTheme.typography.bodyMedium,
            color = NexusTokens.Color.TextPrimary
        )
    }
}

/**
 * Visor de una sola vista: a pantalla completa y con capturas bloqueadas. Al cerrarlo el
 * mensaje se borra para todos.
 */
@Composable
private fun ViewOnceViewer(message: Message, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        val view = LocalView.current
        DisposableEffect(view) {
            (view.parent as? DialogWindowProvider)?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            onDispose { }
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            when {
                message.mediaType == "VIDEO" || message.mediaType == "GIF_VIDEO" ->
                    message.mediaUrl?.let { InlineVideo(url = it, muted = false, showControls = true, modifier = Modifier.fillMaxSize()) }
                message.mediaUrl != null -> AsyncImage(
                    model = message.mediaUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                else -> Text(
                    message.content,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(32.dp)
                )
            }
            Text(
                tr("Se eliminará al cerrar"),
                style = MaterialTheme.typography.labelMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 16.dp)
                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(NexusTokens.Radius.pill))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
    }
}

@Composable
private fun MediaContent(
    message: Message,
    autoplayVideos: Boolean,
    onImageClick: (String) -> Unit,
    onLongPress: () -> Unit
) {
    val context = LocalContext.current
    when (message.mediaType) {
        "IMAGE", "GIF" -> message.mediaUrl?.let { url ->
            Box {
                AsyncImage(
                    model = url,
                    contentDescription = if (message.mediaType == "GIF") "GIF" else tr("Foto"),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 90.dp, max = 360.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .tapAndHold(onTap = { onImageClick(url) }, onHold = onLongPress),
                    contentScale = ContentScale.FillWidth
                )
                if (message.mediaType == "GIF") MediaTag("GIF", Modifier.align(Alignment.TopStart))
            }
        }

        "GIF_VIDEO" -> message.mediaUrl?.let { url ->
            var fullscreen by remember { mutableStateOf(false) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(11.dp))
            ) {
                InlineVideo(url = url, muted = true, showControls = false, modifier = Modifier.fillMaxSize())
                Box(Modifier.matchParentSize().tapAndHold(onTap = { fullscreen = true }, onHold = onLongPress))
                MediaTag("GIF", Modifier.align(Alignment.TopStart))
            }
            if (fullscreen) FullscreenVideo(url = url) { fullscreen = false }
        }

        "VIDEO" -> message.mediaUrl?.let { url ->
            var fullscreen by remember { mutableStateOf(false) }
            var playInline by remember { mutableStateOf(autoplayVideos) }
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                if (playInline) {
                    // Sin sonido y en bucle mientras está en pantalla; al tocarlo se abre.
                    InlineVideo(url = url, muted = true, showControls = false, modifier = Modifier.fillMaxSize())
                    Box(Modifier.matchParentSize().tapAndHold(onTap = { fullscreen = true }, onHold = onLongPress))
                } else {
                    Box(
                        Modifier
                            .matchParentSize()
                            .tapAndHold(onTap = { fullscreen = true }, onHold = onLongPress)
                    )
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.PlayArrow, "Reproducir vídeo", tint = Color.White, modifier = Modifier.size(30.dp))
                    }
                }
                if (!playInline) {
                    Text(
                        tr("Reproducir aquí"),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(NexusTokens.Radius.xs))
                            .clickable { playInline = true }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }
            if (fullscreen) FullscreenVideo(url = url) { fullscreen = false }
        }

        "AUDIO" -> message.mediaUrl?.let { url ->
            Box(Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp)) {
                com.Azelmods.App.ui.components.AudioMessagePlayer(
                    audioUrl = url,
                    accentColor = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Antes se subían pero no había manera de abrirlos: solo se veía el nombre.
        "DOCUMENT" -> AttachmentRow(
            icon = Icons.AutoMirrored.Outlined.InsertDriveFile,
            title = message.content.ifBlank { "Documento" },
            subtitle = tr("Toca para abrir"),
            onTap = {
                message.mediaUrl?.let { url -> openExternal(context, Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            },
            onHold = onLongPress
        )

        "LOCATION" -> AttachmentRow(
            icon = Icons.Outlined.LocationOn,
            title = tr("Ubicación"),
            subtitle = message.latitude?.let { lat ->
                "%.5f, %.5f".format(Locale.ROOT, lat, message.longitude ?: 0.0)
            } ?: message.content,
            onTap = {
                val lat = message.latitude
                val lon = message.longitude
                if (lat != null && lon != null) {
                    openExternal(context, Intent(Intent.ACTION_VIEW, Uri.parse("geo:$lat,$lon?q=$lat,$lon")))
                }
            },
            onHold = onLongPress
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun Modifier.tapAndHold(onTap: () -> Unit, onHold: () -> Unit): Modifier =
    combinedClickable(onClick = onTap, onLongClick = onHold)

private fun openExternal(context: android.content.Context, intent: Intent) {
    runCatching { context.startActivity(intent) }.onFailure {
        android.widget.Toast.makeText(context, tr("No hay ninguna app para abrirlo"), android.widget.Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun AttachmentRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onTap: () -> Unit,
    onHold: () -> Unit
) {
    Row(
        modifier = Modifier
            .tapAndHold(onTap, onHold)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(NexusTokens.Color.BgHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = NexusTokens.Color.TextPrimary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTokens.Color.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = NexusTokens.Color.TextMuted
            )
        }
    }
}

@Composable
private fun MediaTag(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = Color.White,
        modifier = modifier
            .padding(6.dp)
            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(NexusTokens.Radius.xs))
            .padding(horizontal = 5.dp, vertical = 2.dp)
    )
}

/** Vídeo dentro de la conversación. El reproductor vive solo mientras está en pantalla. */
@Composable
fun InlineVideo(url: String, muted: Boolean, showControls: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(url) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(url))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            volume = if (muted) 0f else 1f
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                this.player = player
                useController = showControls
                resizeMode = if (showControls) {
                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                } else {
                    androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            }
        }
    )
}

@Composable
private fun FullscreenVideo(url: String, onClose: () -> Unit) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            InlineVideo(url = url, muted = false, showControls = true, modifier = Modifier.fillMaxSize())
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.Close, "Cerrar", tint = Color.White)
            }
        }
    }
}

@Composable
private fun ReplyQuote(replied: Message, isOwnMessage: Boolean) {
    val preview = when {
        replied.isViewOnce -> tr("Mensaje de una sola vista")
        replied.mediaType == "IMAGE" -> tr("Foto")
        replied.mediaType == "VIDEO" -> tr("Vídeo")
        replied.mediaType == "GIF" || replied.mediaType == "GIF_VIDEO" -> "GIF"
        replied.mediaType == "AUDIO" -> tr("Audio")
        replied.mediaType == "DOCUMENT" -> tr("Documento")
        replied.mediaType == "LOCATION" -> tr("Ubicación")
        else -> replied.content
    }
    Row(
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.sm))
            .background(if (isOwnMessage) Color.Black.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.05f))
            .height(IntrinsicSize.Min)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = preview,
            style = MaterialTheme.typography.bodySmall,
            color = NexusTokens.Color.TextSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

/**
 * Cita de una respuesta a historia. La historia puede haber caducado, así que la cita no
 * intenta cargar el medio original.
 */
@Composable
private fun StoryReplyQuote() {
    Row(
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.sm))
            .background(Color.Black.copy(alpha = 0.18f))
            .height(IntrinsicSize.Min)
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
        Text(
            text = tr("Respuesta a una historia"),
            style = MaterialTheme.typography.bodySmall,
            color = NexusTokens.Color.TextSecondary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    color: Color = NexusTokens.Color.TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            tint = if (color == NexusTokens.Color.TextPrimary) NexusTokens.Color.TextSecondary else color,
            modifier = Modifier.size(NexusTokens.IconSize.md)
        )
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, color = color)
    }
}

/** Hasta cinco emojis sueltos se muestran grandes y sin burbuja. */
private fun String.isEmojiOnly(): Boolean {
    if (isBlank()) return false
    var count = 0
    var i = 0
    while (i < length) {
        val cp = codePointAt(i)
        i += Character.charCount(cp)
        when {
            Character.isWhitespace(cp) || cp == 0xFE0F || cp == 0x200D -> Unit
            cp in 0x1F000..0x1FAFF || cp in 0x2600..0x27BF || cp in 0x2300..0x23FF -> count++
            else -> return false
        }
    }
    return count in 1..5
}

private fun formatTime(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

private fun formatCountdown(seconds: Long): String = when {
    seconds >= 86_400 -> "${seconds / 86_400} d"
    seconds >= 3_600 -> "${seconds / 3_600} h"
    seconds >= 60 -> "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
    else -> "$seconds s"
}

private const val LONG_MESSAGE_CHARS = 1_200
private const val COLLAPSED_LINES = 24
