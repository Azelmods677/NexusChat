@file:androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
package com.Azelmods.App.ui.screens.stories

import com.Azelmods.App.i18n.tr

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.StickyNote2
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import com.Azelmods.App.ui.theme.Pink
import com.Azelmods.App.ui.theme.Purple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoryViewerScreen(
    navController: NavController,
    userId: String,
    viewModel: StoryViewerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    var progress by remember { mutableStateOf(0f) }
    var isPaused by remember { mutableStateOf(false) }
    var showReplySheet by remember { mutableStateOf(false) }
    var replyText by remember { mutableStateOf("") }
    var bottomBarHeight by remember { mutableIntStateOf(0) }
    var showViewersSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid }

    // ── Initial load ─────────────────────────────────────────────────────────
    LaunchedEffect(userId) {
        viewModel.loadStoriesForUser(userId)
    }

    val currentStory = state.stories.getOrNull(state.currentIndex)

    // ── Auto-advance progress timer (images and fallback) ────────────────────
    LaunchedEffect(currentStory?.storyId) { progress = 0f }
    LaunchedEffect(currentStory?.storyId, isPaused) {
        if (currentStory == null || currentStory.type.uppercase() == "VIDEO") {
            // Video progress is handled by ExoPlayer sync, not this timer
            return@LaunchedEffect
        }

        val duration = 5000L // 5 seconds for images
        val step = 50L
        val totalSteps = duration / step
        
        while (progress < 1f && !isPaused) {
            delay(step)
            progress += 1f / totalSteps
        }

        if (progress >= 1f) {
            if (state.currentIndex < state.stories.size - 1) viewModel.nextStory()
            else navController.popBackStack()
        }
    }

    // ── Snackbar on reply sent ───────────────────────────────────────────────
    LaunchedEffect(state.replySent) {
        if (state.replySent) {
            snackbarHostState.showSnackbar(tr("Respuesta enviada"))
            viewModel.clearReplySent()
        }
    }

    // ── Una acción falló: se avisa y la historia sigue en pantalla ───────────
    LaunchedEffect(state.actionError) {
        state.actionError?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearActionError()
        }
    }

    // ── Snackbar on reaction sent ────────────────────────────────────────────
    LaunchedEffect(state.reactionSent) {
        state.reactionSent?.let { emoji ->
            snackbarHostState.showSnackbar("Reaccionaste $emoji")
            viewModel.clearReactionSent()
        }
    }

    // ── Close the viewer after deleting your own story ───────────────────────
    LaunchedEffect(state.deleted) {
        if (state.deleted) {
            viewModel.clearDeleted()
            navController.popBackStack()
        }
    }

    // ── Loading ──────────────────────────────────────────────────────────────
    if (state.isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    // ── Error / Empty ────────────────────────────────────────────────────────
    if (state.stories.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HideImage,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = Color.Gray
                )
                Text(
                    text = state.error ?: tr("No hay historias que ver"),
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                // Decir que "caducaron" solo es cierto cuando no hay ningún error.
                if (state.error == null) {
                    Text(
                        text = tr("Puede que sus historias hayan caducado o se hayan borrado."),
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(tr("Volver"))
                }
            }
        }
        return
    }

    // Safety guard — should never happen after the checks above
    if (currentStory == null) {
        navController.popBackStack()
        return
    }

    // ────────────────────────────────────────────────────────────────────────
    // Música de la historia. En un vídeo con música, la música sustituye a su sonido.
    //
    // Vive aquí, fuera del `when` de tipos, para que haya un único reproductor por
    // historia. Se recrea al cambiar de pista y se libera en onDispose: sin eso el
    // audio seguiría sonando después de salir del visor.
    // ────────────────────────────────────────────────────────────────────────
    val storyMusicUrl = currentStory.musicUrl
    if (!storyMusicUrl.isNullOrBlank()) {
        val musicPlayer = remember(storyMusicUrl) {
            androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
                setMediaItem(androidx.media3.common.MediaItem.fromUri(storyMusicUrl))
                repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
                prepare()
            }
        }
        DisposableEffect(musicPlayer) {
            onDispose { musicPlayer.release() }
        }
        // Respeta la pausa por pulsación larga, igual que hace el vídeo.
        LaunchedEffect(musicPlayer, isPaused) {
            musicPlayer.playWhenReady = !isPaused
            if (isPaused) musicPlayer.pause() else musicPlayer.play()
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Main story viewer
    // ────────────────────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {

        // ── 1. STORY CONTENT ─────────────────────────────────────────────────
        when (currentStory.type.uppercase()) {

            // ── IMAGE ────────────────────────────────────────────────────────
            "IMAGE" -> {
                if (!currentStory.mediaUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(currentStory.mediaUrl)
                            .crossfade(300)
                            .build(),
                        contentDescription = tr("Imagen de la historia"),
                        modifier = Modifier.fillMaxSize(),
                        // Entera, sin recortar: el editor ya publica en 9:16 lo que el autor
                        // encuadró, y en pantallas más altas el resto se ve en negro.
                        contentScale = ContentScale.Fit,
                        loading = {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            }
                        },
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.BrokenImage,
                                        contentDescription = null,
                                        modifier = Modifier.size(56.dp),
                                        tint = Color.Gray
                                    )
                                    Text(
                                        text = tr("No se pudo cargar la imagen"),
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkBackground),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.Gray
                        )
                    }
                }
            }

            // ── VIDEO ────────────────────────────────────────────────────────
            "VIDEO" -> androidx.compose.runtime.key(currentStory.storyId) {
                val videoUrl = currentStory.mediaUrl ?: ""
                var isBuffering by remember { mutableStateOf(true) }
                var videoError by remember { mutableStateOf<String?>(null) }
                var videoEnded by remember(videoUrl) { mutableStateOf(false) }

                val exoPlayer = remember(videoUrl) {
                    ExoPlayer.Builder(context)
                        .setLoadControl(
                            DefaultLoadControl.Builder()
                                .setBufferDurationsMs(15_000, 30_000, 1_500, 3_000)
                                .build()
                        )
                        .build()
                        .apply {
                            if (videoUrl.isNotBlank()) {
                                setMediaItem(MediaItem.fromUri(videoUrl))
                                prepare()
                                playWhenReady = true
                            }
                            repeatMode = Player.REPEAT_MODE_OFF
                            if (!storyMusicUrl.isNullOrBlank()) volume = 0f
                            addListener(object : Player.Listener {
                                override fun onPlaybackStateChanged(playbackState: Int) {
                                    when (playbackState) {
                                        Player.STATE_BUFFERING -> isBuffering = true
                                        Player.STATE_READY -> {
                                            isBuffering = false
                                            // Ensure it plays when ready
                                            if (!isPaused) play()
                                        }
                                        Player.STATE_ENDED -> videoEnded = true
                                        else -> Unit
                                    }
                                }

                                override fun onPlayerError(
                                    error: androidx.media3.common.PlaybackException
                                ) {
                                    videoError = error.message ?: tr("Error de reproducción")
                                    isBuffering = false
                                }
                            })
                        }
                }

                // Synchronize progress bar with video playback
                LaunchedEffect(exoPlayer, isPaused) {
                    while (true) {
                        if (exoPlayer.duration > 0) {
                            progress = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
                        }
                        delay(33) // ~30fps update
                    }
                }

                // Advance when video finishes
                LaunchedEffect(videoEnded) {
                    if (videoEnded) {
                        if (state.currentIndex < state.stories.size - 1) viewModel.nextStory()
                        else navController.popBackStack()
                    }
                }

                // Pause / resume in sync with the global isPaused flag
                LaunchedEffect(isPaused) {
                    exoPlayer.playWhenReady = !isPaused
                    if (!isPaused) exoPlayer.play() else exoPlayer.pause()
                }

                DisposableEffect(videoUrl) {
                    onDispose {
                        exoPlayer.stop()
                        exoPlayer.release()
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    when {
                        videoError != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(DarkBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp),
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = Color.Gray
                                    )
                                    Text(
                                        text = tr("No se pudo cargar el vídeo"),
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = videoError ?: "",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall,
                                        textAlign = TextAlign.Center
                                    )
                                    Button(
                                        onClick = {
                                            if (state.currentIndex < state.stories.size - 1)
                                                viewModel.nextStory()
                                            else
                                                navController.popBackStack()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) { Text(tr("Omitir")) }
                                }
                            }
                        }

                        else -> {
                            AndroidView(
                                factory = { ctx ->
                                    PlayerView(ctx).apply {
                                        player = exoPlayer
                                        useController = false
                                        setShowBuffering(PlayerView.SHOW_BUFFERING_NEVER)
                                    }
                                },
                                update = { view -> view.player = exoPlayer },
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isBuffering) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.45f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }

            // ── TEXT ─────────────────────────────────────────────────────────
            "TEXT" -> {
                val parsedBgColor = remember(currentStory.backgroundColor) {
                    runCatching {
                        currentStory.backgroundColor?.let {
                            Color(android.graphics.Color.parseColor(it))
                        }
                    }.getOrNull()
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            parsedBgColor?.let {
                                Brush.radialGradient(listOf(it, Color.Black))
                            } ?: Brush.linearGradient(
                                listOf(
                                    Purple,
                                    Pink,
                                    DarkBackground
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = currentStory.text ?: "",
                        fontSize = 32.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }

            // ── Fallback ─────────────────────────────────────────────────────
            else -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(DarkBackground),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Unsupported story type: ${currentStory.type}",
                        color = Color.Gray,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        // ── 2. CAPTION OVERLAY ───────────────────────────────────────────────
        if (!currentStory.caption.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.78f))
                        )
                    )
                    .padding(
                        bottom = with(androidx.compose.ui.platform.LocalDensity.current) { bottomBarHeight.toDp() } + 12.dp,
                        start = 16.dp,
                        end = 16.dp,
                        top = 40.dp
                    )
            ) {
                Text(
                    text = currentStory.caption,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // ── 2b. INDICADOR DE MÚSICA ──────────────────────────────────────────
        // Si suena una pista, el usuario tiene que poder ver cuál.
        if (!storyMusicUrl.isNullOrBlank()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 72.dp)
                    .background(
                        Color.Black.copy(alpha = 0.45f),
                        RoundedCornerShape(50)
                    )
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = currentStory.musicName?.takeIf { it.isNotBlank() } ?: tr("Audio"),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
        }

        // ── 3. TOUCH ZONES ───────────────────────────────────────────────────
        // Placed before the top/bottom bars so the bars' clickable elements
        // sit on top and capture their taps first.
        Row(modifier = Modifier.fillMaxSize()) {
            // Left zone → previous story
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(80.dp)
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { viewModel.previousStory() })
                    }
            )
            // Right zone → next story / close; long-press toggles pause
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onTap = {
                                if (state.currentIndex < state.stories.size - 1)
                                    viewModel.nextStory()
                                else
                                    navController.popBackStack()
                            },
                            onLongPress = { isPaused = !isPaused }
                        )
                    }
            )
        }

        // ── 4. TOP BAR (progress + user info) ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Progress bars — one per story
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                state.stories.forEachIndexed { index, _ ->
                    LinearProgressIndicator(
                        progress = {
                            when {
                                index < state.currentIndex -> 1f
                                index == state.currentIndex -> progress
                                else -> 0f
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User info row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                UserAvatar(
                    name = currentStory.userName.ifBlank { "?" },
                    photoUrl = currentStory.userPhotoUrl,
                    size = 40.dp
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Name + time
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = currentStory.userName.ifBlank { "Desconocido" },
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Text(
                        text = getTimeAgo(currentStory.timestamp),
                        color = Color.White.copy(alpha = 0.72f),
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                // Story count "2 / 5"
                Text(
                    text = "${state.currentIndex + 1} / ${state.stories.size}",
                    color = Color.White.copy(alpha = 0.82f),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.width(2.dp))

                // Close button — larger touch target sits above touch zones
                IconButton(
                    onClick = { navController.popBackStack() },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = tr("Cerrar"),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // ── 5. BOTTOM BAR (reply / viewers) + snackbar ──────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))))
                .onSizeChanged { bottomBarHeight = it.height }
        ) {

            // Snackbar floats just above the bar
            SnackbarHost(hostState = snackbarHostState)

            // ── Quick emoji reactions (solo en historias ajenas) ─────────────
            if (currentStory.userId != currentUserId) {
                QuickReactionRow(
                    selectedEmoji = state.myReactions[currentStory.storyId],
                    onReact = { emoji ->
                        viewModel.addReaction(
                            storyOwnerId = currentStory.userId,
                            storyId = currentStory.storyId,
                            emoji = emoji
                        )
                    }
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (currentStory.userId != currentUserId) {
                    // Cápsula de respuesta: la foto de quien publicó y a quién respondes.
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(24.dp))
                            .clickable {
                                isPaused = true
                                showReplySheet = true
                            }
                            .padding(start = 6.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.Azelmods.App.ui.components.UserAvatar(
                            name = currentStory.userName.ifBlank { "?" },
                            photoUrl = currentStory.userPhotoUrl,
                            size = 36.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = tr("Responder a %s…", currentStory.userName.ifBlank { tr("la historia") }),
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable {
                                isPaused = true
                                showReplySheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = tr("Enviar respuesta"),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // ── Own story: viewers count + delete ─────────────────────
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = {
                            viewModel.loadViewers(currentStory.storyId)
                            showViewersSheet = true
                        }
                    ) {
                        Text(
                            text = "👁 ${currentStory.views.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    IconButton(
                        onClick = {
                            isPaused = true
                            showDeleteDialog = true
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = tr("Eliminar historia"),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    } // end main Box

    // ────────────────────────────────────────────────────────────────────────
    // REPLY BOTTOM SHEET
    // ────────────────────────────────────────────────────────────────────────
    if (showReplySheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showReplySheet = false
                replyText = ""
            },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                // Qué historia respondes: miniatura, autor y su texto.
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(width = 44.dp, height = 60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                currentStory.backgroundColor?.let { runCatching { Color(android.graphics.Color.parseColor(it)) }.getOrNull() }
                                    ?: Color.White.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentStory.type == "IMAGE" && !currentStory.mediaUrl.isNullOrBlank()) {
                            SubcomposeAsyncImage(
                                model = currentStory.mediaUrl,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else if (currentStory.type == "VIDEO") {
                            Icon(Icons.Default.PlayArrow, null, tint = Color.White)
                        } else {
                            Text(currentStory.text.orEmpty().take(2), color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            currentStory.userName.ifBlank { tr("Historia") },
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            (currentStory.caption ?: currentStory.text).orEmpty().ifBlank { tr("Historia") },
                            color = Color.White.copy(alpha = 0.6f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                QuickReactionRow(
                    selectedEmoji = state.myReactions[currentStory.storyId],
                    onReact = { emoji ->
                        viewModel.addReaction(storyOwnerId = currentStory.userId, storyId = currentStory.storyId, emoji = emoji)
                        showReplySheet = false
                    }
                )
                Spacer(Modifier.height(10.dp))

                var replyPanel by remember { mutableStateOf(0) }
                val sendReply = {
                    val trimmed = replyText.trim()
                    if (trimmed.isNotBlank()) {
                        viewModel.sendReply(storyOwnerId = currentStory.userId, storyId = currentStory.storyId, text = trimmed)
                        replyText = ""
                        showReplySheet = false
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Row(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.08f)),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { replyPanel = if (replyPanel == 1) 0 else 1 }) {
                            Icon(
                                if (replyPanel == 1) Icons.Outlined.Keyboard else Icons.Outlined.EmojiEmotions,
                                contentDescription = tr("Emojis"),
                                tint = if (replyPanel == 1) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                            )
                        }
                        androidx.compose.foundation.text.BasicTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            maxLines = 4,
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 12.dp),
                            decorationBox = { inner ->
                                if (replyText.isEmpty()) {
                                    Text(tr("Escribe tu respuesta…"), color = Color.White.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyLarge)
                                }
                                inner()
                            }
                        )
                        com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { replyPanel = if (replyPanel == 2) 0 else 2 }) {
                            Icon(
                                Icons.AutoMirrored.Outlined.StickyNote2,
                                contentDescription = tr("Stickers"),
                                tint = if (replyPanel == 2) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (replyText.isNotBlank()) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.1f))
                            .clickable(enabled = replyText.isNotBlank()) { sendReply() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = tr("Enviar respuesta"),
                            tint = if (replyText.isNotBlank()) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                when (replyPanel) {
                    1 -> Box(Modifier.padding(top = 8.dp).height(260.dp)) {
                        com.Azelmods.App.ui.components.CompleteEmojiPicker(onEmojiSelected = { replyText += it })
                    }
                    2 -> Box(Modifier.padding(top = 8.dp)) {
                        com.Azelmods.App.ui.components.StickerPicker(
                            onStickerSelected = { sticker ->
                                viewModel.sendStickerReply(currentStory.userId, currentStory.storyId, sticker)
                                replyText = ""
                                showReplySheet = false
                            },
                            onDismiss = { replyPanel = 0 }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // VIEWERS BOTTOM SHEET
    // ────────────────────────────────────────────────────────────────────────
    if (showViewersSheet) {
        ModalBottomSheet(
            onDismissRequest = { showViewersSheet = false },
            containerColor = DarkSurface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
            ) {
                // Handle indicator
                Box(
                    modifier = Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                        .align(Alignment.CenterHorizontally)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = tr("Vistas"),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = state.viewers.size.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (state.viewers.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tr("Aún no la ha visto nadie"),
                            color = Color.Gray,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 420.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(state.viewers) { viewer ->
                            val viewerName = viewer["displayName"] as? String
                                ?: viewer["name"] as? String
                                ?: viewer["username"] as? String
                                ?: viewer["uid"] as? String
                                ?: tr("Anónimo")
                            val viewerPhoto = viewer["photoUrl"] as? String
                            val viewerUid = viewer["uid"] as? String
                            val viewerReaction = viewerUid?.let { state.reactions[it] }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                UserAvatar(
                                    name = viewerName,
                                    photoUrl = viewerPhoto,
                                    size = 42.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = viewerName,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                if (viewerReaction != null) {
                                    Text(text = viewerReaction, fontSize = 22.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // DELETE CONFIRMATION DIALOG (own story)
    // ────────────────────────────────────────────────────────────────────────
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                isPaused = false
            },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text(tr("Eliminar historia")) },
            text = {
                Text(tr("¿Seguro que quieres eliminar esta historia? Esta acción no se puede deshacer."))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.deleteStory(currentStory)
                    }
                ) {
                    Text(
                        text = tr("Eliminar"),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        isPaused = false
                    }
                ) {
                    Text(tr("Cancelar"))
                }
            },
            containerColor = DarkSurface
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Quick reactions
// ─────────────────────────────────────────────────────────────────────────────

private val QuickReactionEmojis = listOf("❤️", "😂", "😮", "😢", "🔥", "👍")

@Composable
private fun QuickReactionRow(
    selectedEmoji: String?,
    onReact: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        QuickReactionEmojis.forEach { emoji ->
            val isSelected = emoji == selectedEmoji
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.25f else 1f,
                animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
                label = "reaction_scale"
            )
            Text(
                text = emoji,
                fontSize = 26.sp,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White.copy(alpha = 0.22f)
                        else Color.Transparent
                    )
                    .clickable { onReact(emoji) }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

fun getTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    return when {
        diff < 60_000L -> tr("Ahora mismo")
        diff < 3_600_000L -> "${diff / 60_000}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000}h ago"
        else -> "${diff / 86_400_000}d ago"
    }
}
