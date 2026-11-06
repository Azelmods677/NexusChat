package com.Azelmods.App.ui.screens.stories

import com.Azelmods.App.i18n.tr

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AspectRatio
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Draw
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.components.CompleteEmojiPicker
import com.Azelmods.App.ui.components.NexusButton
import com.Azelmods.App.ui.components.NexusButtonSize
import com.Azelmods.App.ui.components.NexusListRow
import com.Azelmods.App.ui.components.NexusTopBar
import com.Azelmods.App.ui.theme.NexusTokens
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import kotlin.math.roundToInt

/** Cómo entra la foto en el marco antes de que el usuario la mueva. */
private enum class StoryFit(val label: String) {
    /** Se ve la foto entera; lo que sobra del marco lo cubre el fondo. */
    COMPLETE("Completa"),

    /** La foto cubre el marco y puede recortar los bordes. */
    FILL("Llenar")
}

/** Qué se ve detrás de la foto cuando no ocupa todo el marco. */
private enum class StoryBackdrop(val label: String, val color: Color?) {
    BLUR("Desenfoque", null),
    BLACK("Negro", Color.Black),
    GRAPHITE("Grafito", Color(0xFF23272B)),
    WHITE("Blanco", Color.White)
}

private enum class StoryMode { NONE, IMAGE, ANIMATED_IMAGE, VIDEO, TEXT }

/** Texto o emoji colocado sobre la historia. Posición en píxeles desde el centro del marco. */
private data class StoryOverlayItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isEmoji: Boolean,
    val color: Color = Color.White,
    val withBackground: Boolean = false,
    val offset: Offset = Offset.Zero,
    val scale: Float = 1f,
    val rotation: Float = 0f
)

/** Trazo libre con puntos normalizados (0..1) para que no dependa del tamaño del marco. */
data class DrawnStroke(val points: List<Offset>, val color: Color, val widthFraction: Float)

private val OverlayColors = listOf(
    Color.White, Color(0xFF101214), Color(0xFFE5534B), Color(0xFFE07A5F),
    Color(0xFFD9A441), Color(0xFF3CC68A), Color(0xFF4F8AE8), Color(0xFF9A6BBF)
)

private val TextStoryBackgrounds = listOf(
    "#14503F", "#1F3A5F", "#3B2E5A", "#5A2E3F", "#5C3D1E", "#23272B", "#101214"
)

private fun parseStoryColor(hex: String): Color =
    Color(android.graphics.Color.parseColor(hex))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateStoryScreen(
    navController: NavController,
    viewModel: CreateStoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    var mode by remember { mutableStateOf(StoryMode.NONE) }
    var mediaUri by remember { mutableStateOf<Uri?>(null) }
    var caption by remember { mutableStateOf("") }

    // Encuadre
    var fit by remember { mutableStateOf(StoryFit.COMPLETE) }
    var backdrop by remember { mutableStateOf(StoryBackdrop.BLUR) }
    var mediaScale by remember { mutableFloatStateOf(1f) }
    var mediaOffset by remember { mutableStateOf(Offset.Zero) }
    var mediaRotation by remember { mutableFloatStateOf(0f) }
    var touchedFrame by remember { mutableStateOf(false) }

    fun resetFraming() {
        mediaScale = 1f
        mediaOffset = Offset.Zero
        mediaRotation = 0f
    }

    // Capas
    val overlays = remember { mutableStateListOf<StoryOverlayItem>() }
    val strokes = remember { mutableStateListOf<DrawnStroke>() }
    var drawMode by remember { mutableStateOf(false) }
    var drawColor by remember { mutableStateOf(Color.White) }
    var editingText by remember { mutableStateOf<StoryOverlayItem?>(null) }
    var showTextEditor by remember { mutableStateOf(false) }
    var showEmojiSheet by remember { mutableStateOf(false) }
    var showBackdropMenu by remember { mutableStateOf(false) }

    // Música (imagen y texto)
    var musicUri by remember { mutableStateOf<Uri?>(null) }
    var musicName by remember { mutableStateOf("") }

    // Historia de texto
    var storyText by remember { mutableStateOf("") }
    var textBackground by remember { mutableStateOf(TextStoryBackgrounds.first()) }

    // Capturas: el marco entero para fotos; solo las capas para vídeo, que se
    // queman después sobre cada fotograma.
    val frameLayer = rememberGraphicsLayer()
    val overlayLayer = rememberGraphicsLayer()

    LaunchedEffect(state.error) {
        state.error?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            viewModel.clearError()
        }
    }
    LaunchedEffect(state.uploadSuccess) {
        if (state.uploadSuccess) {
            android.widget.Toast.makeText(context, tr("Historia publicada"), android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearSuccess()
            navController.popBackStack()
        }
    }

    fun openMedia(uri: Uri) {
        val type = context.contentResolver.getType(uri).orEmpty()
        mediaUri = uri
        mode = when {
            type.startsWith("video/") -> StoryMode.VIDEO
            type == "image/gif" -> StoryMode.ANIMATED_IMAGE
            else -> StoryMode.IMAGE
        }
        // Una foto nueva siempre empieza entera y centrada.
        fit = StoryFit.COMPLETE
        resetFraming()
        touchedFrame = false
    }

    // El selector de fotos del sistema no necesita permisos de almacenamiento.
    val mediaPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let(::openMedia)
    }

    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) cameraUri?.let(::openMedia)
    }
    fun launchCamera() {
        val file = File(context.cacheDir, "story_camera_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", file
        )
        cameraUri = uri
        cameraLauncher.launch(uri)
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    val musicPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            musicUri = uri
            musicName = runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { c ->
                    val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
                }
            }.getOrNull() ?: tr("Audio")
        }
    }

    fun discardAndChooseAgain() {
        mode = StoryMode.NONE
        mediaUri = null
        overlays.clear()
        strokes.clear()
        caption = ""
        storyText = ""
        musicUri = null
        musicName = ""
        drawMode = false
    }

    BackHandler(enabled = mode != StoryMode.NONE) {
        if (drawMode) drawMode = false else discardAndChooseAgain()
    }

    fun publish() {
        if (state.isUploading) return
        when (mode) {
            StoryMode.TEXT -> {
                if (storyText.isBlank()) return
                viewModel.createTextStory(
                    text = storyText.trim(),
                    backgroundColor = textBackground,
                    musicUri = musicUri,
                    musicName = musicName
                )
            }
            StoryMode.VIDEO -> {
                val uri = mediaUri ?: return
                if (overlays.isEmpty() && strokes.isEmpty()) {
                    viewModel.uploadVideoStory(uri, caption, musicUri, musicName)
                } else {
                    scope.launch {
                        runCatching { overlayLayer.toImageBitmap().asAndroidBitmap() }
                            .onSuccess { viewModel.uploadComposedVideoStory(it, caption, uri, musicUri, musicName) }
                            .onFailure { viewModel.uploadVideoStory(uri, caption, musicUri, musicName) }
                    }
                }
            }
            StoryMode.ANIMATED_IMAGE -> {
                // Componer el GIF lo aplanaría en una sola imagen estática.
                val uri = mediaUri ?: return
                viewModel.uploadAnimatedStory(uri, caption, musicUri, musicName)
            }
            StoryMode.IMAGE -> {
                val uri = mediaUri ?: return
                scope.launch {
                    runCatching { frameLayer.toImageBitmap().asAndroidBitmap() }
                        .onSuccess {
                            viewModel.uploadComposedImageStory(it, caption, uri, musicUri, musicName)
                        }
                        .onFailure { viewModel.uploadImageStory(uri, caption, musicUri, musicName) }
                }
            }
            StoryMode.NONE -> Unit
        }
    }

    if (mode == StoryMode.NONE) {
        Scaffold(
            topBar = { NexusTopBar(title = tr("Nueva historia"), onBack = { navController.popBackStack() }) },
            containerColor = NexusTokens.Color.BgBase
        ) { padding ->
            Column(Modifier.fillMaxSize().padding(padding)) {
                Text(
                    tr("Se verá durante 24 horas."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextMuted,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                NexusListRow(
                    title = tr("Galería"),
                    subtitle = tr("Foto, vídeo o GIF"),
                    icon = Icons.Outlined.PhotoLibrary,
                    onClick = {
                        mediaPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    }
                )
                NexusListRow(
                    title = tr("Cámara"),
                    subtitle = tr("Hacer una foto ahora"),
                    icon = Icons.Outlined.PhotoCamera,
                    onClick = {
                        if (context.checkSelfPermission(android.Manifest.permission.CAMERA) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                        ) launchCamera()
                        else cameraPermission.launch(android.Manifest.permission.CAMERA)
                    }
                )
                NexusListRow(
                    title = tr("Texto"),
                    subtitle = tr("Solo palabras sobre un color"),
                    icon = Icons.Outlined.TextFields,
                    onClick = { mode = StoryMode.TEXT }
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // ── Barra superior ───────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { if (drawMode) drawMode = false else discardAndChooseAgain() }) {
                Icon(Icons.Default.Close, "Cancelar", tint = Color.White)
            }
            Spacer(Modifier.weight(1f))
            if (drawMode) {
                IconButton(
                    onClick = { if (strokes.isNotEmpty()) strokes.removeAt(strokes.lastIndex) },
                    enabled = strokes.isNotEmpty()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Undo, "Deshacer", tint = Color.White)
                }
                TextButton(onClick = { drawMode = false }) { Text(tr("Listo"), color = Color.White) }
            } else {
                NexusButton(
                    text = when {
                        !state.isUploading -> tr("Publicar")
                        state.uploadProgress != null -> tr("Publicando %s", "${state.uploadProgress} %")
                        else -> tr("Publicando")
                    },
                    onClick = ::publish,
                    size = NexusButtonSize.Small,
                    fillWidth = false,
                    loading = state.isUploading,
                    enabled = mode != StoryMode.TEXT || storyText.isNotBlank(),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }

        // ── Marco 9:16: lo que se ve aquí es exactamente lo que se publica ───
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(9f / 16f, matchHeightConstraintsFirst = true)
                    .clip(RoundedCornerShape(NexusTokens.Radius.md))
                    .clipToBounds()
                    .background(if (mode == StoryMode.TEXT) parseStoryColor(textBackground) else Color.Black)
                    .recordInto(if (mode == StoryMode.IMAGE) frameLayer else null),
                contentAlignment = Alignment.Center
            ) {
                when (mode) {
                    StoryMode.TEXT -> BasicTextField(
                        value = storyText,
                        onValueChange = { storyText = it },
                        textStyle = MaterialTheme.typography.headlineMedium.copy(
                            color = Color.White,
                            textAlign = TextAlign.Center
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.Center) {
                                if (storyText.isEmpty()) {
                                    Text(
                                        tr("Escribe algo"),
                                        style = MaterialTheme.typography.headlineMedium,
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                                inner()
                            }
                        }
                    )

                    StoryMode.VIDEO -> mediaUri?.let { StoryVideoPreview(uri = it) }

                    else -> mediaUri?.let { uri ->
                        // Fondo: necesario porque la foto, entera, rara vez llena el marco.
                        if (backdrop == StoryBackdrop.BLUR) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .blur(28.dp)
                            )
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f)))
                        } else {
                            Box(Modifier.fillMaxSize().background(backdrop.color ?: Color.Black))
                        }

                        AsyncImage(
                            model = uri,
                            contentDescription = tr("Foto de la historia"),
                            contentScale = if (fit == StoryFit.COMPLETE) ContentScale.Fit else ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = mediaOffset.x
                                    translationY = mediaOffset.y
                                    scaleX = mediaScale
                                    scaleY = mediaScale
                                    rotationZ = mediaRotation
                                }
                        )

                        // Toda la superficie responde a pellizcar, arrastrar y girar.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .pointerInput(drawMode) {
                                    if (drawMode) return@pointerInput
                                    detectTransformGestures { _, pan, zoom, rotation ->
                                        touchedFrame = true
                                        // Mínimo 0,3×: el usuario SÍ puede alejar la foto.
                                        mediaScale = (mediaScale * zoom).coerceIn(0.3f, 6f)
                                        mediaOffset += pan
                                        mediaRotation += rotation
                                    }
                                }
                        )
                    }
                }

                // Capas del usuario (quedan dentro de la captura del marco)
                Box(
                    Modifier
                        .fillMaxSize()
                        .recordInto(if (mode == StoryMode.VIDEO) overlayLayer else null)
                ) {
                    StrokesLayer(strokes = strokes)
                    overlays.forEach { item ->
                        key(item.id) {
                            OverlayView(
                                item = item,
                                enabled = !drawMode,
                                onChange = { updated ->
                                    val i = overlays.indexOfFirst { it.id == updated.id }
                                    if (i >= 0) overlays[i] = updated
                                },
                                onEdit = {
                                    if (!item.isEmoji) {
                                        editingText = item
                                        showTextEditor = true
                                    }
                                },
                                onRemove = { overlays.removeAll { it.id == item.id } }
                            )
                        }
                    }
                }

                if (drawMode) {
                    DrawingSurface(
                        color = drawColor,
                        onStrokeFinished = { strokes.add(it) }
                    )
                }

                if (mode == StoryMode.IMAGE && !touchedFrame && !drawMode) {
                    Text(
                        tr("Pellizca para acercar o alejar · arrastra para mover"),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                            .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(NexusTokens.Radius.pill))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // ── Herramientas ─────────────────────────────────────────────────────
        if (drawMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OverlayColors.forEach { c ->
                    ColorDot(color = c, selected = c == drawColor) { drawColor = c }
                }
            }
        } else {
            if (mode == StoryMode.TEXT) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextStoryBackgrounds.forEach { hex ->
                        ColorDot(parseStoryColor(hex), hex == textBackground) { textBackground = hex }
                    }
                }
            } else if (showBackdropMenu && mode != StoryMode.VIDEO) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StoryBackdrop.entries.forEach { option ->
                        FilterChip(
                            selected = backdrop == option,
                            onClick = { backdrop = option },
                            label = { Text(option.label) }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Va la primera: al final de la fila quedaba fuera de la pantalla y parecía
                // que la opción había desaparecido.
                StoryTool(
                    Icons.Outlined.MusicNote,
                    if (musicName.isBlank()) tr("Música") else tr("Con música"),
                    selected = musicName.isNotBlank()
                ) {
                    if (musicName.isBlank()) musicPicker.launch("audio/*")
                    else {
                        musicUri = null
                        musicName = ""
                    }
                }
                if (mode == StoryMode.IMAGE || mode == StoryMode.ANIMATED_IMAGE) {
                    StoryTool(
                        icon = if (fit == StoryFit.COMPLETE) Icons.Outlined.FitScreen else Icons.Outlined.AspectRatio,
                        label = fit.label
                    ) {
                        fit = if (fit == StoryFit.COMPLETE) StoryFit.FILL else StoryFit.COMPLETE
                        resetFraming()
                    }
                    StoryTool(Icons.Outlined.Rotate90DegreesCw, "Girar") {
                        mediaRotation = ((mediaRotation / 90f).roundToInt() + 1) * 90f
                    }
                    StoryTool(Icons.Outlined.RestartAlt, "Restablecer") {
                        fit = StoryFit.COMPLETE
                        resetFraming()
                    }
                    StoryTool(Icons.Outlined.BlurOn, "Fondo", selected = showBackdropMenu) {
                        showBackdropMenu = !showBackdropMenu
                    }
                }
                if (mode == StoryMode.IMAGE || mode == StoryMode.VIDEO) {
                    StoryTool(Icons.Outlined.TextFields, "Texto") {
                        editingText = null
                        showTextEditor = true
                    }
                    StoryTool(Icons.Outlined.EmojiEmotions, "Emoji") { showEmojiSheet = true }
                    StoryTool(Icons.Outlined.Draw, "Dibujar") { drawMode = true }
                }
            }

            if (musicName.isNotBlank()) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.MusicNote, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        musicName,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
            }

            if (mode != StoryMode.TEXT) {
                BasicTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    cursorBrush = androidx.compose.ui.graphics.SolidColor(Color.White),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .background(Color.White.copy(alpha = 0.10f), RoundedCornerShape(NexusTokens.Radius.md))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { inner ->
                        if (caption.isEmpty()) {
                            Text(
                                tr("Añade una descripción"),
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        }
                        inner()
                    }
                )
            }
        }
    }

    if (showTextEditor) {
        TextOverlayDialog(
            initial = editingText,
            onDismiss = { showTextEditor = false },
            onConfirm = { text, color, withBackground ->
                val current = editingText
                if (current != null) {
                    val i = overlays.indexOfFirst { it.id == current.id }
                    if (i >= 0) overlays[i] = current.copy(text = text, color = color, withBackground = withBackground)
                } else if (text.isNotBlank()) {
                    overlays.add(StoryOverlayItem(text = text, isEmoji = false, color = color, withBackground = withBackground))
                }
                showTextEditor = false
            }
        )
    }

    if (showEmojiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEmojiSheet = false },
            containerColor = NexusTokens.Color.BgSurface
        ) {
            CompleteEmojiPicker(
                onEmojiSelected = { emoji ->
                    overlays.add(StoryOverlayItem(text = emoji, isEmoji = true))
                    showEmojiSheet = false
                }
            )
        }
    }
}

/** Dibuja el contenido normalmente y, además, lo guarda en [layer] para poder exportarlo. */
private fun Modifier.recordInto(layer: GraphicsLayer?): Modifier =
    if (layer == null) this
    else drawWithContent {
        layer.record { this@drawWithContent.drawContent() }
        drawLayer(layer)
    }

@Composable
private fun StoryVideoPreview(uri: Uri) {
    val context = LocalContext.current
    val player = remember(uri) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(uri))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ONE
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                this.player = player
                useController = false
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = { it.player = player },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun StrokesLayer(strokes: List<DrawnStroke>) {
    if (strokes.isEmpty()) return
    Canvas(Modifier.fillMaxSize()) {
        strokes.forEach { stroke ->
            val width = stroke.widthFraction * size.width
            for (i in 1 until stroke.points.size) {
                drawLine(
                    color = stroke.color,
                    start = Offset(stroke.points[i - 1].x * size.width, stroke.points[i - 1].y * size.height),
                    end = Offset(stroke.points[i].x * size.width, stroke.points[i].y * size.height),
                    strokeWidth = width,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

@Composable
private fun DrawingSurface(color: Color, onStrokeFinished: (DrawnStroke) -> Unit) {
    val current = remember { mutableStateListOf<Offset>() }
    Canvas(
        Modifier
            .fillMaxSize()
            .pointerInput(color) {
                fun norm(o: Offset) = Offset(o.x / size.width, o.y / size.height)
                detectDragGestures(
                    onDragStart = {
                        current.clear()
                        current.add(norm(it))
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        current.add(norm(change.position))
                    },
                    onDragEnd = {
                        if (current.size > 1) onStrokeFinished(DrawnStroke(current.toList(), color, 0.012f))
                        current.clear()
                    },
                    onDragCancel = { current.clear() }
                )
            }
    ) {
        val width = 0.012f * size.width
        for (i in 1 until current.size) {
            drawLine(
                color = color,
                start = Offset(current[i - 1].x * size.width, current[i - 1].y * size.height),
                end = Offset(current[i].x * size.width, current[i].y * size.height),
                strokeWidth = width,
                cap = StrokeCap.Round
            )
        }
    }
}

/** Texto o emoji: se arrastra, se pellizca, se gira. Tocar edita; mantener pulsado elimina. */
@Composable
private fun OverlayView(
    item: StoryOverlayItem,
    enabled: Boolean,
    onChange: (StoryOverlayItem) -> Unit,
    onEdit: () -> Unit,
    onRemove: () -> Unit
) {
    val latest by rememberUpdatedState(item)
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .offset { IntOffset(item.offset.x.roundToInt(), item.offset.y.roundToInt()) }
                .then(
                    if (!enabled) Modifier else Modifier
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, rotation ->
                                val now = latest
                                onChange(
                                    now.copy(
                                        offset = now.offset + pan,
                                        scale = (now.scale * zoom).coerceIn(0.4f, 6f),
                                        rotation = now.rotation + rotation
                                    )
                                )
                            }
                        }
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = { onEdit() },
                                onLongPress = { onRemove() }
                            )
                        }
                )
                .graphicsLayer {
                    scaleX = item.scale
                    scaleY = item.scale
                    rotationZ = item.rotation
                }
        ) {
            if (item.isEmoji) {
                Text(item.text, fontSize = 64.sp)
            } else {
                Text(
                    text = item.text,
                    color = item.color,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .then(
                            if (item.withBackground) {
                                Modifier.background(
                                    if (item.color == Color(0xFF101214)) Color.White else Color.Black.copy(alpha = 0.6f),
                                    RoundedCornerShape(NexusTokens.Radius.sm)
                                )
                            } else Modifier
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TextOverlayDialog(
    initial: StoryOverlayItem?,
    onDismiss: () -> Unit,
    onConfirm: (String, Color, Boolean) -> Unit
) {
    var text by remember { mutableStateOf(initial?.text.orEmpty()) }
    var color by remember { mutableStateOf(initial?.color ?: Color.White) }
    var withBackground by remember { mutableStateOf(initial?.withBackground ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) tr("Añadir texto") else tr("Editar texto")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(tr("Escribe aquí")) },
                    maxLines = 4
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OverlayColors.forEach { c ->
                        ColorDot(c, c == color, size = 26) { color = c }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { withBackground = !withBackground },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(tr("Fondo detrás del texto"), modifier = Modifier.weight(1f))
                    Switch(checked = withBackground, onCheckedChange = { withBackground = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.trim(), color, withBackground) }) { Text(tr("Listo")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

@Composable
private fun ColorDot(color: Color, selected: Boolean, size: Int = 30, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color.White else Color.White.copy(alpha = 0.25f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (selected && color == Color.White) {
            Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size((size * 0.6f).dp))
        }
    }
}

@Composable
private fun StoryTool(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    val tint = if (selected) MaterialTheme.colorScheme.primary else Color.White
    Column(
        modifier = Modifier
            .widthIn(min = 68.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.sm))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(NexusTokens.IconSize.md))
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint, fontWeight = FontWeight.Normal)
    }
}
