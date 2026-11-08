package com.Azelmods.App.ui.screens.chat

import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.togetherWith
import com.Azelmods.App.ui.components.staggeredEnter

import com.Azelmods.App.i18n.tr

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Poll
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.AudioFile
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Gif
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.components.coloredIcons
import com.Azelmods.App.ui.components.cameraOutside
import com.Azelmods.App.data.gif.GifResult
import com.Azelmods.App.data.gif.GifSearchService
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.ui.components.CompleteEmojiPicker
import com.Azelmods.App.ui.components.NexusNotice
import com.Azelmods.App.ui.components.StickerPicker
import com.Azelmods.App.ui.theme.NexusTokens
import com.Azelmods.App.utils.AudioRecorder
import com.Azelmods.App.utils.PermissionHelper
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay

/** Opciones del temporizador común del chat. */
private val TimerOptions = listOf(
    0L to "Desactivado",
    30L to "30 segundos",
    300L to "5 minutos",
    3_600L to "1 hora",
    28_800L to "8 horas",
    86_400L to "1 día",
    604_800L to "1 semana"
)

private fun timerShortLabel(seconds: Long): String =
    TimerOptions.firstOrNull { it.first == seconds }?.second ?: "$seconds s"

@Composable
fun TypingIndicator() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(NexusTokens.Color.BgElevated)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(600, delayMillis = index * 180), RepeatMode.Reverse),
                label = "dot$index"
            )
            Box(
                Modifier
                    .size(6.dp)
                    .graphicsLayer { this.alpha = alpha }
                    .background(NexusTokens.Color.TextSecondary, CircleShape)
            )
        }
    }
}

@Composable
fun ReplyPreviewBar(
    message: Message,
    label: String = "Responder",
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexusTokens.Color.BgSurface)
            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp)
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.primary)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Text(
                text = messagePreviewText(message),
                style = MaterialTheme.typography.bodySmall,
                color = NexusTokens.Color.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, "Cancelar", tint = NexusTokens.Color.TextMuted)
        }
    }
}

/** Temporizador del chat y envío de una sola vista. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EphemeralTimerSheet(
    currentSeconds: Long,
    viewOnceNext: Boolean,
    onSelectTimer: (Long) -> Unit,
    onViewOnceChange: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NexusTokens.Color.BgSurface,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.padding(bottom = 16.dp)) {
            Text(
                tr("Mensajes temporales"),
                style = MaterialTheme.typography.titleLarge,
                color = NexusTokens.Color.TextPrimary,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                tr("Los mensajes nuevos se borran para los dos cuando pasa este tiempo desde que " +
                    "se leen. Si no se leen, a los 7 días. Nadie puede evitar una foto a la " +
                    "pantalla desde otro dispositivo."),
                style = MaterialTheme.typography.bodySmall,
                color = NexusTokens.Color.TextMuted,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            TimerOptions.forEach { (seconds, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTimer(seconds) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = NexusTokens.Color.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    if (seconds == currentSeconds) {
                        Icon(Icons.Default.Check, "Seleccionado", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            HorizontalDivider(color = NexusTokens.Color.Hairline, modifier = Modifier.padding(vertical = 6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewOnceChange(!viewOnceNext) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Outlined.VisibilityOff, null, tint = NexusTokens.Color.TextSecondary)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(tr("Una sola vista"), style = MaterialTheme.typography.bodyLarge, color = NexusTokens.Color.TextPrimary)
                    Text(
                        tr("Solo el siguiente mensaje. Se borra al cerrarlo y bloquea las capturas."),
                        style = MaterialTheme.typography.bodySmall,
                        color = NexusTokens.Color.TextMuted
                    )
                }
                Switch(checked = viewOnceNext, onCheckedChange = onViewOnceChange)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatInputArea(
    messageText: String,
    onMessageChange: (String) -> Unit,
    onSendClick: () -> Unit,
    contactId: String,
    viewModel: ChatViewModel,
    chatTimerSeconds: Long = 0L,
    viewOnceNext: Boolean = false,
    showEphemeralPicker: Boolean = false,
    isUploading: Boolean = false
) {
    val context = LocalContext.current
    var showEmojiPicker by remember { mutableStateOf(false) }
    var showStickerPicker by remember { mutableStateOf(false) }
    var captionFor by remember { mutableStateOf<Pair<Uri, Boolean>?>(null) }
    var showPollCreator by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }
    var pendingSticker by remember { mutableStateOf<String?>(null) }
    var showScheduler by remember { mutableStateOf(false) }
    var showAttachments by remember { mutableStateOf(false) }
    var showGifPicker by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var audioRecorder by remember { mutableStateOf<AudioRecorder?>(null) }

    DisposableEffect(Unit) {
        onDispose { audioRecorder?.cancelRecording() }
    }

    fun startRecording() {
        audioRecorder = AudioRecorder(context)
        if (audioRecorder?.startRecording() != null) isRecording = true
    }
    val audioPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording()
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let { viewModel.sendImageMessage(it, contactId) }
    }
    fun launchCamera() {
        val file = java.io.File(context.cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        cameraUri = uri
        cameraLauncher.launch(uri)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    // El selector del sistema no pide permisos y deja elegir foto, vídeo o GIF.
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val type = context.contentResolver.getType(uri).orEmpty()
            when {
                type == "image/gif" -> viewModel.sendGifMessage(uri, contactId)
                type.startsWith("video/") -> captionFor = uri to true
                else -> captionFor = uri to false
            }
        }
    }
    val videoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.sendVideoMessage(it, contactId) }
    }
    val gifFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val type = context.contentResolver.getType(uri).orEmpty()
            if (type.startsWith("video/")) viewModel.sendAnimationMessage(uri, contactId)
            else viewModel.sendGifMessage(uri, contactId)
        }
    }
    val audioPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.sendAudioMessage(it, contactId) }
    }
    val documentPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            val name = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                }
            }.getOrNull() ?: tr("Documento")
            viewModel.sendDocumentMessage(uri, contactId, name)
        }
    }

    fun sendLastLocation() {
        runCatching {
            val lm = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
            val location = lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                viewModel.sendLocationMessage(location.latitude, location.longitude, "", contactId)
            } else {
                android.widget.Toast.makeText(context, tr("Aún no hay una ubicación disponible"), android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) sendLastLocation()
    }

    val contactPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickContact()) { uri: Uri? ->
        uri?.let { contactUri ->
            runCatching {
                context.contentResolver.query(contactUri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(android.provider.ContactsContract.Contacts.DISPLAY_NAME)
                    if (idx >= 0 && c.moveToFirst()) {
                        viewModel.sendMessage("Contacto: ${c.getString(idx)}", contactId)
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(NexusTokens.Color.BgBase)
    ) {
        if (isUploading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = NexusTokens.Color.BgHigh
            )
        }

        // Aviso permanente mientras el chat tiene temporizador.
        if (chatTimerSeconds > 0 || viewOnceNext) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.toggleEphemeralPicker() }
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (viewOnceNext) Icons.Outlined.VisibilityOff else Icons.Default.Timer,
                    null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (viewOnceNext) tr("El siguiente mensaje será de una sola vista")
                    else "Mensajes temporales: ${timerShortLabel(chatTimerSeconds)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = NexusTokens.Color.TextSecondary
                )
            }
        }

        AnimatedVisibility(visible = isRecording) {
            RecordingBar(
                onCancel = {
                    audioRecorder?.cancelRecording()
                    audioRecorder = null
                    isRecording = false
                },
                onSend = {
                    val file = audioRecorder?.stopRecording()
                    audioRecorder = null
                    isRecording = false
                    file?.let { viewModel.sendAudioMessage(Uri.fromFile(it), contactId, voice = true) }
                }
            )
        }

        // Sugerencias de comandos al empezar a escribir con "/".
        val commandSuggestions = remember(messageText) {
            com.Azelmods.App.data.chat.ChatCommands.suggestions(messageText)
        }
        AnimatedVisibility(visible = !isRecording && commandSuggestions.isNotEmpty()) {
            Column(Modifier.fillMaxWidth().background(NexusTokens.Color.BgSurface)) {
                commandSuggestions.forEach { command ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onMessageChange(command.name + " ") }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            command.usage,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            command.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = NexusTokens.Color.TextMuted
                        )
                    }
                }
            }
        }

        // Al escribir @ en un grupo se proponen sus miembros.
        val chatState by viewModel.state.collectAsState()
        val mentionQuery = com.Azelmods.App.ui.screens.chat.currentMentionQuery(messageText)
        val mentionMatches = remember(mentionQuery, chatState.groupMembers) {
            if (mentionQuery == null) emptyList()
            else chatState.groupMembers.values.filter { it.name.contains(mentionQuery, ignoreCase = true) }.take(5)
        }
        if (mentionMatches.isNotEmpty()) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(NexusTokens.Color.BgElevated)
            ) {
                mentionMatches.forEach { member ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onMessageChange(com.Azelmods.App.ui.screens.chat.applyMention(messageText, member.name)) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.Azelmods.App.ui.components.UserAvatar(member.name, member.photoUrl, size = 30.dp)
                        Spacer(Modifier.width(10.dp))
                        Text(member.name, color = NexusTokens.Color.TextPrimary, maxLines = 1)
                    }
                }
            }
        }

        AnimatedVisibility(visible = !isRecording) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                val entryStyle by com.Azelmods.App.ui.components.ChatLook.entryStyle.collectAsState()
                val entryHint by com.Azelmods.App.ui.components.ChatLook.entryHint.collectAsState()
                val hideEmoji by com.Azelmods.App.ui.components.ChatLook.hideEntryEmoji.collectAsState()
                val entryTranslate by com.Azelmods.App.ui.components.ChatLook.entryTranslate.collectAsState()
                val isPhotoEntry = entryStyle == com.Azelmods.App.ui.components.ChatEntryStyle.PHOTO
                val isSocialEntry = entryStyle == com.Azelmods.App.ui.components.ChatEntryStyle.SOCIAL
                val isFlatEntry = entryStyle == com.Azelmods.App.ui.components.ChatEntryStyle.FLAT
                // En Social, Plana y los estilos de CDZ los iconos llevan el color de acento y van sin círculo de fondo.
                val coloredIcons = entryStyle.coloredIcons()
                val cameraOutside = entryStyle.cameraOutside()
                val emojiRight = isFlatEntry || cameraOutside
                val openCamera = {
                    if (context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) launchCamera()
                    else cameraPermission.launch(android.Manifest.permission.CAMERA)
                }
                val iconTint = if (coloredIcons) MaterialTheme.colorScheme.primary else NexusTokens.Color.TextMuted

                val openAttachments = {
                    showAttachments = true
                    showEmojiPicker = false
                }
                val toggleEmoji = {
                    showEmojiPicker = !showEmojiPicker
                    showStickerPicker = false
                }

                when {
                    // Tu foto fuera del campo, con un aro fino de color.
                    isPhotoEntry -> {
                        val me = com.Azelmods.App.data.profile.rememberMyProfile()
                        Box(
                            Modifier
                                .padding(start = 6.dp, end = 6.dp, bottom = 2.dp)
                                .size(40.dp)
                                .border(1.4.dp, MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            com.Azelmods.App.ui.components.UserAvatar(name = me.name, photoUrl = me.photoUrl, size = 35.dp)
                        }
                    }

                    // Adjuntar y cámara fuera, a mano del pulgar.
                    isSocialEntry -> {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = openAttachments, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Default.Add, "Adjuntar", tint = iconTint, modifier = Modifier.size(26.dp))
                        }
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = openCamera, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Outlined.PhotoCamera, tr("Cámara"), tint = iconTint, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(2.dp))
                    }

                    isFlatEntry -> {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = openAttachments, modifier = Modifier.size(47.dp)) {
                            Icon(Icons.Default.Add, "Adjuntar", tint = iconTint)
                        }
                    }

                    // Estilos de CDZ: foto (con aro o cuadrada) y el clip, fuera del campo.
                    cameraOutside -> {
                        val me = com.Azelmods.App.data.profile.rememberMyProfile()
                        if (entryStyle == com.Azelmods.App.ui.components.ChatEntryStyle.RING) {
                            Box(
                                Modifier
                                    .padding(start = 4.dp, end = 2.dp, bottom = 3.dp)
                                    .size(38.dp)
                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                com.Azelmods.App.ui.components.UserAvatar(name = me.name, photoUrl = me.photoUrl, size = 32.dp)
                            }
                        } else if (entryStyle == com.Azelmods.App.ui.components.ChatEntryStyle.SQUARE) {
                            Box(
                                Modifier
                                    .padding(start = 4.dp, end = 2.dp, bottom = 3.dp)
                                    .size(width = 30.dp, height = 38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NexusTokens.Color.BgElevated),
                                contentAlignment = Alignment.Center
                            ) {
                                if (me.photoUrl.isNullOrBlank()) {
                                    Text(me.name.take(1).uppercase(), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                                } else {
                                    AsyncImage(
                                        model = me.photoUrl,
                                        contentDescription = null,
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }
                        }
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = openAttachments, modifier = Modifier.size(40.dp)) {
                            Icon(Icons.Outlined.AttachFile, tr("Adjuntar"), tint = iconTint, modifier = Modifier.size(24.dp))
                        }
                    }

                    else -> {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = openAttachments) {
                            Icon(Icons.Default.Add, "Adjuntar", tint = NexusTokens.Color.TextSecondary)
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 44.dp)
                        .then(
                            com.Azelmods.App.ui.components.chatEntryModifier(
                                entryStyle,
                                MaterialTheme.colorScheme.primary
                            )
                        )
                        .padding(start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!hideEmoji && !emojiRight) {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = toggleEmoji, modifier = Modifier.size(38.dp)) {
                            Icon(
                                if (showEmojiPicker) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions,
                                "Emoji",
                                tint = iconTint,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    } else if (hideEmoji) {
                        Spacer(Modifier.width(10.dp))
                    }
                    BasicTextField(
                        value = messageText,
                        onValueChange = onMessageChange,
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 10.dp),
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = NexusTokens.Color.TextPrimary),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        maxLines = 6,
                        decorationBox = { inner ->
                            if (messageText.isEmpty()) {
                                Text(
                                    entryHint.ifBlank { tr("Mensaje") },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = NexusTokens.Color.TextMuted
                                )
                            }
                            inner()
                        }
                    )
                    if (entryTranslate && messageText.isNotBlank()) {
                        com.Azelmods.App.ui.components.AnimatedIconButton(
                            onClick = { viewModel.translateDraft(messageText) { onMessageChange(it) } },
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Outlined.Translate, tr("Traducir"), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                    }
                    // Con tu foto, adjuntar deja la izquierda y se queda dentro del campo.
                    if (isPhotoEntry) {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = openAttachments, modifier = Modifier.size(38.dp)) {
                            Icon(Icons.Default.Add, "Adjuntar", tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(22.dp))
                        }
                    }
                    com.Azelmods.App.ui.components.AnimatedIconButton(
                        onClick = { viewModel.toggleEphemeralPicker() },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            if (chatTimerSeconds > 0 || viewOnceNext) Icons.Default.Timer else Icons.Outlined.Timer,
                            "Mensajes temporales",
                            tint = if (chatTimerSeconds > 0 || viewOnceNext) MaterialTheme.colorScheme.primary
                            else NexusTokens.Color.TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    // En la plana y en los de CDZ el emoji va a la derecha, dentro del campo.
                    if (!hideEmoji && emojiRight) {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = toggleEmoji, modifier = Modifier.size(if (isFlatEntry) 47.dp else 40.dp)) {
                            Icon(
                                if (showEmojiPicker) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions,
                                "Emoji",
                                tint = iconTint
                            )
                        }
                    }
                }

                Spacer(Modifier.width(6.dp))
                if (cameraOutside) {
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = openCamera, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Outlined.PhotoCamera, tr("Cámara"), tint = iconTint, modifier = Modifier.size(24.dp))
                    }
                }

                val hasText = messageText.isNotBlank()
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                coloredIcons -> Color.Transparent
                                hasText -> MaterialTheme.colorScheme.primary
                                else -> NexusTokens.Color.BgElevated
                            }
                        )
                        .clickable {
                            if (hasText) {
                                onSendClick()
                            } else if (PermissionHelper.hasAudioPermission(context)) {
                                startRecording()
                            } else {
                                audioPermission.launch(android.Manifest.permission.RECORD_AUDIO)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.animation.AnimatedContent(
                        targetState = hasText,
                        transitionSpec = {
                            (androidx.compose.animation.scaleIn(
                                androidx.compose.animation.core.spring(
                                    dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy
                                ),
                                initialScale = 0.4f
                            ) + androidx.compose.animation.fadeIn()) togetherWith
                                (androidx.compose.animation.scaleOut(targetScale = 0.4f) + androidx.compose.animation.fadeOut())
                        },
                        label = "enviarMicro"
                    ) { sending ->
                        if (sending) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                "Enviar",
                                tint = if (coloredIcons) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(if (coloredIcons) 24.dp else 20.dp)
                            )
                        } else {
                            Icon(
                                Icons.Default.Mic,
                                "Grabar audio",
                                tint = if (coloredIcons) MaterialTheme.colorScheme.primary else NexusTokens.Color.TextSecondary,
                                modifier = Modifier.size(if (coloredIcons) 26.dp else 22.dp)
                            )
                        }
                    }
                }
            }
        }

        // Emojis y stickers en el mismo panel, con una pestaña para cada uno.
        AnimatedVisibility(visible = showEmojiPicker || showStickerPicker) {
            var panelTab by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableIntStateOf(if (showStickerPicker) 1 else 0) }
            Column {
                androidx.compose.material3.TabRow(
                    selectedTabIndex = panelTab,
                    containerColor = NexusTokens.Color.BgSurface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    androidx.compose.material3.Tab(selected = panelTab == 0, onClick = { panelTab = 0 }, text = { Text(tr("Emojis")) })
                    androidx.compose.material3.Tab(selected = panelTab == 1, onClick = { panelTab = 1 }, text = { Text(tr("Stickers")) })
                }
                if (panelTab == 0) {
                    CompleteEmojiPicker(onEmojiSelected = { onMessageChange(messageText + it) })
                } else {
                    StickerPicker(
                        onStickerSelected = { sticker ->
                            if (com.Azelmods.App.ui.components.ChatLook.confirmSticker.value) {
                                pendingSticker = sticker
                            } else {
                                viewModel.sendStickerMessage(sticker, contactId)
                            }
                        },
                        onDismiss = {
                            showStickerPicker = false
                            showEmojiPicker = false
                        }
                    )
                }
            }
        }
    }

    captionFor?.let { (uri, isVideo) ->
        MediaCaptionDialog(
            uri = uri,
            isVideo = isVideo,
            onDismiss = { captionFor = null },
            onSend = { caption ->
                if (isVideo) viewModel.sendVideoMessage(uri, contactId, caption)
                else viewModel.sendImageMessage(uri, contactId, caption)
                captionFor = null
            }
        )
    }

    pendingSticker?.let { sticker ->
        AlertDialog(
            onDismissRequest = { pendingSticker = null },
            title = { Text(tr("¿Enviar este sticker?")) },
            text = { com.Azelmods.App.ui.components.StickerImage(sticker, 120.dp) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.sendStickerMessage(sticker, contactId)
                    pendingSticker = null
                }) { Text(tr("Enviar")) }
            },
            dismissButton = { TextButton(onClick = { pendingSticker = null }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }

    if (showContactPicker) {
        ContactPickerDialog(
            onDismiss = { showContactPicker = false },
            onPick = { contact ->
                viewModel.sendMessage(encodeContact(contact), contactId)
                showContactPicker = false
            }
        )
    }

    if (showPollCreator) {
        PollCreateDialog(
            onDismiss = { showPollCreator = false },
            onCreate = { poll ->
                viewModel.sendMessage(encodePoll(poll), contactId)
                showPollCreator = false
            }
        )
    }

    if (showScheduler) {
        if (messageText.isBlank()) {
            LaunchedEffect(Unit) {
                android.widget.Toast.makeText(context, tr("Escribe primero el mensaje que quieres programar"), android.widget.Toast.LENGTH_LONG).show()
                showScheduler = false
            }
        } else {
            ScheduleMessageDialog(
                text = messageText,
                chatId = viewModel.chatIdFor(contactId),
                onDismiss = { showScheduler = false },
                onScheduled = { time ->
                    onMessageChange("")
                    showScheduler = false
                    android.widget.Toast.makeText(context, tr("Se enviará el %s", time), android.widget.Toast.LENGTH_LONG).show()
                }
            )
        }
    }

    if (showAttachments) {
        ModalBottomSheet(
            onDismissRequest = { showAttachments = false },
            containerColor = NexusTokens.Color.BgSurface,
            tonalElevation = 0.dp
        ) {
            val options = listOf(
                Triple(Icons.Outlined.PhotoLibrary, "Galería") {
                    mediaPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                },
                Triple(Icons.Outlined.PhotoCamera, "Cámara") {
                    if (context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    ) launchCamera()
                    else cameraPermission.launch(android.Manifest.permission.CAMERA)
                },
                Triple(Icons.Outlined.Gif, "GIF") { showGifPicker = true },
                Triple(Icons.Outlined.Videocam, "Vídeo") {
                    videoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly))
                },
                Triple(Icons.Outlined.Description, "Documento") { documentPicker.launch("*/*") },
                Triple(Icons.Outlined.AudioFile, "Audio") { audioPicker.launch("audio/*") },
                Triple(Icons.Outlined.LocationOn, "Ubicación") {
                    val granted = context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED ||
                        context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                    if (granted) sendLastLocation()
                    else locationPermission.launch(
                        arrayOf(
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                },
                Triple(Icons.Outlined.Person, "Contacto") { contactPicker.launch(null) },
                Triple(Icons.Outlined.Person, "Contacto") { showContactPicker = true },
                Triple(Icons.Outlined.Poll, "Encuesta") { showPollCreator = true },
                Triple(Icons.Outlined.Schedule, "Programar") { showScheduler = true }
            )
            Column(Modifier.padding(start = 12.dp, end = 12.dp, bottom = 24.dp)) {
                options.chunked(3).forEach { row ->
                    Row(Modifier.fillMaxWidth()) {
                        row.forEach { (icon, label, action) ->
                            val tileIndex = options.indexOfFirst { it.second == label }
                            AttachmentTile(icon, label, Modifier.weight(1f).staggeredEnter(tileIndex)) {
                                showAttachments = false
                                action()
                            }
                        }
                    }
                }
            }
        }
    }

    if (showGifPicker) {
        GifPickerDialog(
            onDismiss = { showGifPicker = false },
            onPickFromDevice = {
                showGifPicker = false
                gifFilePicker.launch(arrayOf("image/gif", "video/mp4", "video/webm"))
            },
            onGifSelected = { gif ->
                showGifPicker = false
                viewModel.sendRemoteGif(gif.sendUrl, contactId)
            }
        )
    }

    if (showEphemeralPicker) {
        EphemeralTimerSheet(
            currentSeconds = chatTimerSeconds,
            viewOnceNext = viewOnceNext,
            onSelectTimer = { viewModel.setChatTimer(it) },
            onViewOnceChange = { viewModel.setViewOnceNext(it) },
            onDismiss = { viewModel.dismissEphemeralPicker() }
        )
    }
}

@Composable
private fun AttachmentTile(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(NexusTokens.Radius.md))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(NexusTokens.Color.BgHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = NexusTokens.Color.TextPrimary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = NexusTokens.Color.TextSecondary)
    }
}

/** Selector de GIF: búsqueda con GIPHY si hay clave y, siempre, elegir uno del teléfono. */
@OptIn(FlowPreview::class)
@Composable
private fun GifPickerDialog(
    onDismiss: () -> Unit,
    onPickFromDevice: () -> Unit,
    onGifSelected: (GifResult) -> Unit
) {
    val context = LocalContext.current
    val service = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext, GifEntryPoint::class.java
        ).gifSearchService()
    }
    var hasKey by remember { mutableStateOf(service.hasApiKey()) }
    var keyInput by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<GifResult>>(emptyList()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query, hasKey) {
        if (!hasKey) return@LaunchedEffect
        delay(350) // Espera a que se deje de teclear.
        loading = true
        error = null
        runCatching { service.search(query) }
            .onSuccess { results = it }
            .onFailure { error = it.message ?: tr("No se pudo buscar") }
        loading = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .background(NexusTokens.Color.BgBase)
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.Azelmods.App.ui.components.AnimatedIconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Cerrar", tint = NexusTokens.Color.TextPrimary)
                }
                if (hasKey) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(NexusTokens.Radius.md))
                            .background(NexusTokens.Color.BgElevated)
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Search, null, tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = NexusTokens.Color.TextPrimary),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { inner ->
                                if (query.isEmpty()) {
                                    Text(tr("Buscar GIF"), style = MaterialTheme.typography.bodyLarge, color = NexusTokens.Color.TextMuted)
                                }
                                inner()
                            }
                        )
                    }
                } else {
                    Text(
                        "GIF",
                        style = MaterialTheme.typography.titleLarge,
                        color = NexusTokens.Color.TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                }
                TextButton(onClick = onPickFromDevice) { Text(tr("Del teléfono")) }
            }

            if (!hasKey) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        tr("Para buscar GIF hace falta una clave gratuita de GIPHY. Mientras tanto " +
                            "puedes enviar cualquier GIF o vídeo corto de tu teléfono."),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusTokens.Color.TextSecondary
                    )
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        label = { Text(tr("Clave de GIPHY")) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        enabled = keyInput.isNotBlank(),
                        onClick = {
                            service.saveApiKey(keyInput)
                            hasKey = service.hasApiKey()
                        }
                    ) { Text(tr("Guardar clave")) }
                    NexusNotice(text = tr("Consíguela en developers.giphy.com. Lo que busques lo verá GIPHY, aunque no tu IP si el modo anónimo está activo."))
                }
            } else {
                error?.let {
                    NexusNotice(
                        text = it,
                        tone = NexusTokens.Color.Error,
                        modifier = Modifier.padding(horizontal = 12.dp),
                        actionLabel = tr("Cambiar clave"),
                        onAction = {
                            service.saveApiKey("")
                            hasKey = service.hasApiKey()
                        }
                    )
                }
                if (loading && results.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
                    }
                } else if (!loading && results.isEmpty() && error == null) {
                    Text(
                        tr("Sin resultados"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusTokens.Color.TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp)
                    )
                }
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalItemSpacing = 6.dp
                ) {
                    items(results, key = { it.id }) { gif ->
                        AsyncImage(
                            model = gif.previewUrl,
                            contentDescription = "GIF",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(gif.aspectRatio.coerceIn(0.5f, 2.5f))
                                .clip(RoundedCornerShape(NexusTokens.Radius.sm))
                                .background(NexusTokens.Color.BgElevated)
                                .clickable { onGifSelected(gif) }
                        )
                    }
                }
            }
        }
    }
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface GifEntryPoint {
    fun gifSearchService(): GifSearchService
}

/** Barra de grabación de audio: el punto y el contador indican que está grabando. */
@Composable
private fun RecordingBar(onCancel: () -> Unit, onSend: () -> Unit) {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            seconds++
        }
    }
    val alpha by rememberInfiniteTransition(label = "rec").animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700), RepeatMode.Reverse),
        label = "rec_alpha"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, "Cancelar", tint = NexusTokens.Color.TextSecondary)
        }
        Box(
            Modifier
                .size(10.dp)
                .graphicsLayer { this.alpha = alpha }
                .background(NexusTokens.Color.Error, CircleShape)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "%02d:%02d".format(seconds / 60, seconds % 60),
            style = MaterialTheme.typography.titleMedium,
            color = NexusTokens.Color.TextPrimary,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable(onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.Send, "Enviar audio", tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(20.dp))
        }
    }
}

/** Vista previa antes de enviar una foto o un vídeo, con un texto opcional debajo. */
@Composable
private fun MediaCaptionDialog(uri: Uri, isVideo: Boolean, onDismiss: () -> Unit, onSend: (String) -> Unit) {
    var caption by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isVideo) tr("Enviar vídeo") else tr("Enviar foto")) },
        text = {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(NexusTokens.Color.BgElevated),
                    contentAlignment = Alignment.Center
                ) {
                    if (isVideo) {
                        Icon(Icons.Outlined.Videocam, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(40.dp).size(48.dp))
                    } else {
                        AsyncImage(model = uri, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Fit, modifier = Modifier.fillMaxWidth())
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it.take(1_000) },
                    placeholder = { Text(tr("Añade un texto")) },
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onSend(caption.trim()) }) { Text(tr("Enviar")) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}
