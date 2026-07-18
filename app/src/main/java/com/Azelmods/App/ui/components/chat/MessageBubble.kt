package com.Azelmods.App.ui.components.chat

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.model.MessageStatus
import com.Azelmods.App.ui.components.ReadReceiptIndicator
import com.Azelmods.App.ui.components.safeClickable
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import com.Azelmods.App.ui.theme.PurpleLight
import com.Azelmods.App.ui.theme.ErrorRed
import com.Azelmods.App.ui.theme.DarkElevated
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.DarkBubbleLight
import com.Azelmods.App.ui.theme.DarkBubbleDeep

/**
 * MessageBubble — Componente de burbuja de mensaje premium.
 *
 * Características:
 * - Esquinas adaptativas: la punta es menos redondeada que el resto
 * - Colores diferenciados: gradiente vibrante (enviado) vs oscuro sutil (recibido)
 * - Soporte para imágenes/GIFs con Coil AsyncImage
 * - Reacciones emoji con animación spring
 * - Animación de entrada fade + slide
 * - Sombra sutil con color dinámico según dark mode
 * - Indicador de lectura, marca "editado", timestamps
 * - Opciones: responder, editar, eliminar
 */
@Composable
fun MessageBubble(
    message: Message,
    isOwnMessage: Boolean,
    onLongPress: () -> Unit = {},
    onReactionClick: (String) -> Unit = {},
    onImageClick: ((String, String, String) -> Unit)? = null,
    themeColor: Color = MaterialTheme.colorScheme.primary,
    themeSecondaryColor: Color = MaterialTheme.colorScheme.secondary,
    onDeleteClick: ((Boolean) -> Unit)? = null,
    onEditClick: (() -> Unit)? = null,
    onMessageViewed: (() -> Unit)? = null,  // Callback when ephemeral message is viewed
    translatedText: String? = null,
    isTranslating: Boolean = false,
    onTranslate: (() -> Unit)? = null
) {
    var showReactionPicker by remember { mutableStateOf(false) }
    var showMessageOptions by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val quickReactions = listOf("❤️", "👍", "😂", "😮", "😢", "🙏")

    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val hasViewed = currentUserId in message.viewedBy
    val isDeleted = message.deletedFor[currentUserId] == true
    val isDeletedForEveryone = message.deletedForEveryone

    val timeSinceMessage = System.currentTimeMillis() - message.timestamp
    val canEdit = isOwnMessage && timeSinceMessage < 15 * 60 * 1000 && message.mediaType == null && !isDeletedForEveryone
    val canDeleteForEveryone = isOwnMessage && timeSinceMessage < 48 * 60 * 60 * 1000 && !isDeletedForEveryone

    // ── Entry animation: fade + slide ──
    var visible by remember(message.messageId) { mutableStateOf(false) }
    LaunchedEffect(message.messageId) {
        visible = true
    }

    val offsetX by animateDpAsState(
        targetValue = if (visible) 0.dp else if (isOwnMessage) 50.dp else (-50).dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "slide_${message.messageId}"
    )

    val entryAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(300),
        label = "fade_${message.messageId}"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(x = offsetX)
            .graphicsLayer { this.alpha = entryAlpha },
        horizontalAlignment = if (isOwnMessage) Alignment.End else Alignment.Start
    ) {
        // ── Deleted message placeholder ──
        if (isDeletedForEveryone) {
            Surface(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .padding(vertical = 4.dp),
                shape = RoundedCornerShape(18.dp),
                color = DarkElevated.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Este mensaje fue eliminado",
                        color = Color.Gray,
                        fontSize = 13.sp,
                        fontStyle = FontStyle.Italic
                    )
                }
            }
            return@Column
        }

        Column {
            // ── Premium Message Bubble ──
            // Adaptive corners: the "tip" pointing to sender is less rounded
            val bubbleShape = if (isOwnMessage) {
                RoundedCornerShape(
                    topStart = 18.dp, topEnd = 18.dp,
                    bottomStart = 18.dp, bottomEnd = 4.dp
                )
            } else {
                RoundedCornerShape(
                    topStart = 4.dp, topEnd = 18.dp,
                    bottomStart = 18.dp, bottomEnd = 18.dp
                )
            }
            
            // ── Countdown timer for self-destructing messages ──
            var remainingSeconds by remember(message.messageId, message.selfDestructAt) {
                mutableStateOf(calculateRemainingSeconds(message.selfDestructAt))
            }
            
            LaunchedEffect(message.messageId, message.selfDestructAt) {
                if (message.isEphemeral && message.selfDestructAt > 0) {
                    while (remainingSeconds > 0) {
                        delay(1000)
                        remainingSeconds = calculateRemainingSeconds(message.selfDestructAt)
                    }
                }
            }            // ── Track view-once message being seen ──
            val isViewOnceEphemeral = message.isEphemeral && message.isViewOnce

            // Report view for view-once / ephemeral messages when they appear on screen
            LaunchedEffect(message.messageId) {
                if (message.isEphemeral && !isOwnMessage && !(currentUserId in message.viewedBy)) {
                    // Small delay so the user actually sees it
                    delay(1500)
                    onMessageViewed?.invoke()
                }
            }

            Surface(
                shape = bubbleShape,
                color = Color.Transparent,
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .shadow(
                        elevation = if (isOwnMessage) 8.dp else 6.dp,
                        shape = bubbleShape,
                        ambientColor = if (isOwnMessage) themeColor.copy(0.4f) else Color.Black.copy(0.3f),
                        spotColor = if (isOwnMessage) themeColor.copy(0.6f) else Color.Black.copy(0.3f)
                    )
                    .then(
                        // Ephemeral border glow effect
                        if (message.isEphemeral && !(currentUserId in message.viewedBy)) {
                            Modifier.border(
                                width = 1.5.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        themeColor.copy(alpha = 0.6f),
                                        themeSecondaryColor.copy(alpha = 0.3f),
                                        themeColor.copy(alpha = 0.6f)
                                    )
                                ),
                                shape = bubbleShape
                            )
                        } else if (message.isEphemeral && (currentUserId in message.viewedBy)) {
                            Modifier.border(
                                width = 1.dp,
                                color = themeColor.copy(alpha = 0.2f),
                                shape = bubbleShape
                            )
                        } else Modifier
                    )
                    .safeClickable(
                        onClick = { showReactionPicker = !showReactionPicker },
                        onLongClick = { showMessageOptions = true }
                    )
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            when {
                                message.isViewOnce && hasViewed && !isOwnMessage -> Brush.sweepGradient(
                                    listOf(DarkSurface.copy(alpha = 0.8f))
                                )
                                isOwnMessage -> Brush.linearGradient(
                                    colors = listOf(
                                        themeColor,
                                        themeSecondaryColor,
                                        themeSecondaryColor
                                    ),
                                    start = Offset(0f, 0f),
                                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                )
                                else -> Brush.linearGradient(
                                    listOf(DarkBubbleLight, DarkBubbleDeep)
                                )
                            }
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Shine overlay for 3D effect
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.White.copy(if (isOwnMessage) 0.15f else 0.08f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    Column {
                        // ── Cita de historia (mensaje enviado como respuesta a una story) ──
                        if (message.replyTo?.startsWith("story:") == true) {
                            StoryReplyQuote(isOwnMessage = isOwnMessage)
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // ── Media content ──
                        when (message.mediaType) {
                            "IMAGE" -> {
                                message.mediaUrl?.let { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Image message",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 300.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .safeClickable {
                                                onImageClick?.invoke(
                                                    url,
                                                    if (isOwnMessage) "Tú" else "Contacto",
                                                    SimpleDateFormat("HH:mm", Locale.getDefault())
                                                        .format(Date(message.timestamp))
                                                )
                                            },
                                        contentScale = ContentScale.Crop
                                    )
                                    if (message.content.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            "VIDEO" -> {
                                message.mediaUrl?.let { url ->
                                    var showVideoPlayer by remember { mutableStateOf(false) }
                                    
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black.copy(alpha = 0.3f))
                                            .safeClickable { showVideoPlayer = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = "Video thumbnail",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(Color.Black.copy(alpha = 0.3f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                Icons.Default.PlayCircle,
                                                contentDescription = "Play video",
                                                tint = Color.White,
                                                modifier = Modifier.size(64.dp)
                                            )
                                        }
                                    }
                                    
                                    // Full-screen video player dialog
                                    if (showVideoPlayer) {
                                        androidx.compose.ui.window.Dialog(
                                            onDismissRequest = { showVideoPlayer = false },
                                            properties = androidx.compose.ui.window.DialogProperties(
                                                usePlatformDefaultWidth = false
                                            )
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color.Black)
                                            ) {
                                                val context = androidx.compose.ui.platform.LocalContext.current
                                                val videoPlayer = remember {
                                                    androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                                                        setMediaItem(androidx.media3.common.MediaItem.fromUri(url))
                                                        prepare()
                                                        playWhenReady = true
                                                    }
                                                }
                                                
                                                DisposableEffect(Unit) {
                                                    onDispose {
                                                        videoPlayer.release()
                                                    }
                                                }
                                                
                                                androidx.compose.ui.viewinterop.AndroidView(
                                                    factory = { ctx ->
                                                        androidx.media3.ui.PlayerView(ctx).apply {
                                                            player = videoPlayer
                                                            useController = true
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                
                                                IconButton(
                                                    onClick = { showVideoPlayer = false },
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(16.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Default.Close,
                                                        contentDescription = "Close",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (message.content.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            "AUDIO" -> {
                                message.mediaUrl?.let { url ->
                                    com.Azelmods.App.ui.components.AudioMessagePlayer(
                                        audioUrl = url,
                                        accentColor = themeColor
                                    )
                                    if (message.content.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                            "STICKER" -> {
                                message.mediaUrl?.let { url ->
                                    AsyncImage(
                                        model = url,
                                        contentDescription = "Sticker",
                                        modifier = Modifier
                                            .size(150.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                    if (message.content.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }

                        // ── Text content ──
                        // ── Ephemeral / View-Once badge ──
                        if (message.isEphemeral) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        if (message.isViewOnce) Icons.Default.VisibilityOff else Icons.Default.Timer,
                                        contentDescription = null,
                                        tint = if (message.isViewOnce && (currentUserId in message.viewedBy) && !isOwnMessage) Color.Gray else PurpleLight,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = when {
                                            message.isViewOnce && (currentUserId in message.viewedBy) && !isOwnMessage -> "Visto una vez"
                                            message.isViewOnce -> "📷 Una vez"
                                            remainingSeconds > 0 -> "🕐 ${formatDuration(remainingSeconds)}"
                                            else -> "🕐 Expirando..."
                                        },
                                        color = if (message.isViewOnce && hasViewed && !isOwnMessage) Color.Gray else PurpleLight.copy(alpha = 0.8f),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                
                                // Countdown progress bar for timer-based ephemeral
                                if (!message.isViewOnce && remainingSeconds > 0 && message.selfDestructDuration > 0) {
                                    val progress = remainingSeconds.toFloat() / message.selfDestructDuration.toFloat()
                                    Box(
                                        modifier = Modifier
                                            .width(40.dp)
                                            .height(3.dp)
                                            .background(
                                                DarkElevated,
                                                RoundedCornerShape(2.dp)
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = progress.coerceIn(0f, 1f))
                                                .fillMaxHeight()
                                                .background(
                                                    if (progress > 0.5f) PurpleLight
                                                    else if (progress > 0.2f) Color(0xFFFF8C00)
                                                    else ErrorRed,
                                                    RoundedCornerShape(2.dp)
                                                )
                                        )
                                    }
                                }
                            }
                        }

                        // ── Text content ──
                        if (message.content.isNotEmpty()) {
                            val isEmojiOnly = message.content.all { char ->
                                char.code in 0x1F300..0x1F9FF ||
                                char.code in 0x2600..0x26FF ||
                                char.code in 0x2700..0x27BF ||
                                char.isWhitespace()
                            }
                            val emojiCount = message.content.count { !it.isWhitespace() }

                            if (isEmojiOnly && emojiCount <= 5 && message.mediaType == null) {
                                Text(
                                    text = message.content,
                                    fontSize = 44.sp,
                                    lineHeight = 48.sp
                                )
                            } else {
                                Text(
                                    text = message.content,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp
                                )
                            }

                            // ── Translated text (if available) ──
                            if (isTranslating) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = Color.White.copy(alpha = 0.6f),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Traduciendo...",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            } else if (!translatedText.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Translate,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Traducción",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = translatedText,
                                    color = Color.White.copy(alpha = 0.92f),
                                    fontSize = 15.sp,
                                    lineHeight = 20.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }
                        }

                        // ── Media content view-once placeholder ──
                        if (message.isViewOnce && (currentUserId in message.viewedBy) && !isOwnMessage && message.mediaType != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkElevated.copy(alpha = 0.5f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Has visto este mensaje",
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        
                        // ── Disabled media content for non-viewed view-once (show placeholder only)
                        if (message.isViewOnce && !(currentUserId in message.viewedBy) && !isOwnMessage && message.mediaType != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DarkElevated.copy(alpha = 0.7f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.VisibilityOff,
                                        contentDescription = null,
                                        tint = PurpleLight,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Mensaje temporal\nToca para ver",
                                        color = PurpleLight,
                                        fontSize = 14.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))

                        // ── Time & status row ──
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = formatTime(message.timestamp),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 11.sp
                            )

                            if (message.edited) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "(editado)",
                                    color = Color.White.copy(alpha = 0.5f),
                                    fontSize = 10.sp,
                                    fontStyle = FontStyle.Italic
                                )
                            }

                            if (isOwnMessage) {
                                Spacer(modifier = Modifier.width(4.dp))
                                ReadReceiptIndicator(status = message.status)
                            }
                        }
                    }
                }
            }

            // ── Quick reaction picker (animated) ──
            AnimatedVisibility(
                visible = showReactionPicker,
                enter = scaleIn(initialScale = 0.8f) + fadeIn(),
                exit = scaleOut(targetScale = 0.8f) + fadeOut()
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DarkSurface,
                    shadowElevation = 8.dp,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        quickReactions.forEach { emoji ->
                            val animScale = remember { Animatable(0f) }
                            LaunchedEffect(Unit) {
                                animScale.animateTo(
                                    1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy
                                    )
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .size(36.dp)
                                    .scale(animScale.value)
                                    .safeClickable {
                                        onReactionClick(emoji)
                                        showReactionPicker = false
                                    },
                                shape = CircleShape,
                                color = DarkElevated
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(text = emoji, fontSize = 20.sp)
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Display existing reactions ──
        if (message.reactions.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.reactions.forEach { (_, emoji) ->
                    val animScale = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        animScale.animateTo(
                            1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = DarkElevated.copy(alpha = 0.8f),
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .scale(animScale.value)
                            .safeClickable { onReactionClick(emoji) }
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                            Text(text = emoji, fontSize = 16.sp)
                        }
                    }
                }
            }
        }

        // ── Ephemeral badge below the bubble ──
        if (message.isEphemeral) {
            Row(
                modifier = Modifier.padding(top = 4.dp, start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (message.isViewOnce) Icons.Default.VisibilityOff else Icons.Default.Timer,
                    contentDescription = null,
                    tint = PurpleLight.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isOwnMessage) {
                        if (message.isViewOnce) "Enviado como vista única" else "Mensaje temporal"
                    } else {
                        if ((currentUserId in message.viewedBy) && message.isViewOnce) "Visto una vez" else "Auto-destruible"
                    },
                    color = PurpleLight.copy(alpha = 0.4f),
                    fontSize = 10.sp,
                    fontStyle = FontStyle.Italic
                )
            }
        }

        // ── Message Options Dialog ──
        if (showMessageOptions) {
            AlertDialog(
                onDismissRequest = { showMessageOptions = false },
                containerColor = DarkSurface,
                title = { Text("Opciones del mensaje", color = Color.White) },
                text = {
                    Column {
                        if (onTranslate != null && message.content.isNotEmpty()) {
                            TextButton(
                                onClick = {
                                    showMessageOptions = false
                                    onTranslate.invoke()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Translate, null, tint = PurpleLight)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Traducir mensaje", color = Color.White)
                                }
                            }
                        }

                        if (canEdit) {
                            TextButton(
                                onClick = {
                                    showMessageOptions = false
                                    onEditClick?.invoke()
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Editar", color = Color.White)
                                }
                            }
                        }

                        TextButton(
                            onClick = {
                                showMessageOptions = false
                                onDeleteClick?.invoke(false)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.DeleteOutline, null, tint = Color.White)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("Eliminar para mí", color = Color.White)
                            }
                        }

                        if (canDeleteForEveryone) {
                            TextButton(
                                onClick = {
                                    showMessageOptions = false
                                    showDeleteConfirmation = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Delete, null, tint = ErrorRed)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text("Eliminar para todos", color = ErrorRed)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showMessageOptions = false }) {
                        Text("Cancelar", color = themeColor)
                    }
                }
            )
        }

        // ── Delete Confirmation Dialog ──
        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                containerColor = DarkSurface,
                title = { Text("¿Eliminar para todos?", color = Color.White) },
                text = {
                    Text(
                        "Este mensaje será eliminado para todos en el chat. Esta acción no se puede deshacer.",
                        color = Color.Gray
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDeleteConfirmation = false
                        onDeleteClick?.invoke(true)
                    }) {
                        Text("Eliminar", color = ErrorRed)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancelar", color = Color.White)
                    }
                }
            )
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

/**
 * Calculate remaining seconds until a self-destruct timestamp.
 * Returns 0 if already expired.
 */
private fun calculateRemainingSeconds(selfDestructAt: Long): Long {
    if (selfDestructAt <= 0) return 0L
    val remaining = (selfDestructAt - System.currentTimeMillis()) / 1000
    return maxOf(0L, remaining)
}

/**
 * Format remaining seconds into a human-readable duration string.
 */
private fun formatDuration(seconds: Long): String {
    return when {
        seconds >= 86400 -> "${seconds / 86400}d"
        seconds >= 3600 -> "${seconds / 3600}h"
        seconds >= 60 -> "${seconds / 60}m"
        else -> "${seconds}s"
    }
}

/**
 * Cita compacta que se muestra dentro de la burbuja cuando el mensaje es una
 * respuesta a una historia (replyTo = "story:{storyId}"). La historia puede
 * haber expirado (24 h), por eso la cita es autónoma y no intenta cargar el
 * media original.
 */
@Composable
private fun StoryReplyQuote(isOwnMessage: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isOwnMessage) Color.Black.copy(alpha = 0.18f)
                else Color.White.copy(alpha = 0.08f)
            )
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(Color.White.copy(alpha = 0.85f))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "📖", fontSize = 18.sp)
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.padding(vertical = 6.dp)) {
            Text(
                text = "Historia",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Respuesta a una historia",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
    }
}
