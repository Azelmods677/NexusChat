package com.Azelmods.App.ui.screens.downloader

import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.components.NexusButton
import com.Azelmods.App.ui.theme.NexusTokens
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Running(val progress: Float, val detail: String) : DownloadState
    data class Done(val uri: Uri, val fileName: String, val mime: String) : DownloadState
    /** [detail] es el error tal cual lo dio yt-dlp: se puede copiar para saber qué pasó. */
    data class Failed(val reason: String, val detail: String = "") : DownloadState
}

/**
 * Arranque de yt-dlp, compartido por toda la app. Se prepara una sola vez y se actualiza
 * como mucho una vez al día: comprobarlo en cada descarga gastaba el límite de la API de GitHub
 * (sobre todo por Tor, donde muchas salidas comparten ese límite) y la descarga fallaba.
 */
object DownloaderEngine {
    private const val TAG = "VideoDownloader"
    private val lock = Mutex()
    @Volatile private var ready = false

    suspend fun ensureReady(context: Context, onStatus: (String) -> Unit = {}) = lock.withLock {
        if (ready) return@withLock
        onStatus(tr("Instalando el descargador…"))
        try {
            YoutubeDL.getInstance().init(context)
        } catch (first: Exception) {
            // Una instalación a medias (la app se cerró mientras descomprimía Python) deja
            // el motor roto para siempre. Se borra y se instala de nuevo.
            Log.w(TAG, "init falló; se reinstala desde cero", first)
            File(context.noBackupFilesDir, "youtubedl-android").deleteRecursively()
            context.getSharedPreferences("youtubedl-android", Context.MODE_PRIVATE).edit().clear().apply()
            YoutubeDL.getInstance().init(context)
        }
        runCatching { FFmpeg.getInstance().init(context) }.onFailure { Log.w(TAG, "ffmpeg no arrancó", it) }
        ready = true
        update(context, force = false)
    }

    fun update(context: Context, force: Boolean) {
        val prefs = context.getSharedPreferences("video_downloader", Context.MODE_PRIVATE)
        val last = prefs.getLong("last_update", 0L)
        if (!force && System.currentTimeMillis() - last < 24 * 60 * 60 * 1000L) return
        runCatching { YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE) }
            .onSuccess { prefs.edit().putLong("last_update", System.currentTimeMillis()).apply() }
            .onFailure { Log.w(TAG, "No se pudo actualizar yt-dlp", it) }
    }
}

/** Descarga con yt-dlp, integrado en la app: YouTube, Facebook, X, Instagram, TikTok y más. */
@HiltViewModel
class VideoDownloaderViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state = _state.asStateFlow()
    private var job: Job? = null
    private val processId = "nexus-download"

    private data class Attempt(val update: Boolean, val alternativeClients: Boolean, val simpleFormat: Boolean)

    fun download(url: String, audioOnly: Boolean) {
        if (_state.value is DownloadState.Running) return
        val cleanUrl = url.trim()
        _state.value = DownloadState.Running(0f, tr("Preparando…"))
        job = viewModelScope.launch(Dispatchers.IO) {
            val folder = File(context.cacheDir, "downloads").apply {
                deleteRecursively()
                mkdirs()
            }
            val failures = mutableListOf<String>()
            runCatching {
                DownloaderEngine.ensureReady(context) { _state.value = DownloadState.Running(0f, it) }
                // Cada intento cambia algo: primero lo normal; luego otros clientes de YouTube y un
                // formato que no necesita unir pistas; por último, con yt-dlp recién actualizado.
                val attempts = listOf(
                    Attempt(update = false, alternativeClients = false, simpleFormat = false),
                    Attempt(update = false, alternativeClients = true, simpleFormat = true),
                    Attempt(update = true, alternativeClients = true, simpleFormat = false)
                )
                var done = false
                for ((index, attempt) in attempts.withIndex()) {
                    if (attempt.update) {
                        _state.value = DownloadState.Running(0f, tr("Actualizando el descargador…"))
                        DownloaderEngine.update(context, force = true)
                    }
                    if (index > 0) {
                        _state.value = DownloadState.Running(0f, tr("Reintentando (%s de %s)…", "${index + 1}", "${attempts.size}"))
                    }
                    folder.listFiles()?.forEach { it.deleteRecursively() }
                    val result = runCatching {
                        YoutubeDL.getInstance().execute(buildRequest(cleanUrl, audioOnly, folder, attempt), processId) { progress, _, line ->
                            _state.value = DownloadState.Running((progress / 100f).coerceIn(0f, 1f), line.take(80))
                        }
                    }
                    if (result.isSuccess && folder.hasMedia()) {
                        done = true
                        break
                    }
                    val error = result.exceptionOrNull()
                    if (error is YoutubeDL.CanceledException) throw error
                    val message = error?.message.orEmpty().ifBlank { "No se generó ningún archivo" }
                    Log.w(TAG, "Intento ${index + 1} fallido: $message", error)
                    failures += message
                    // Si el enlace no es compatible o el vídeo es privado, reintentar no sirve.
                    if (isFinal(message)) break
                }
                if (!done) error(failures.lastOrNull() ?: "Descarga fallida")
                val file = folder.listFiles()
                    ?.filter { it.isFile && !it.name.endsWith(".part") }
                    ?.maxByOrNull { it.length() }
                    ?: error("No se generó ningún archivo")
                publish(file, audioOnly)
            }.onSuccess { (uri, mime, name) ->
                _state.value = DownloadState.Done(uri, name, mime)
            }.onFailure {
                if (it is YoutubeDL.CanceledException || it is kotlinx.coroutines.CancellationException) {
                    _state.value = DownloadState.Idle
                } else {
                    Log.w(TAG, "Descarga fallida", it)
                    val detail = (failures + listOfNotNull(it.message)).distinct().joinToString("\n\n").takeLast(1500)
                    _state.value = DownloadState.Failed(friendly(detail), detail)
                }
            }
            folder.deleteRecursively()
        }
    }

    fun cancel() {
        runCatching { YoutubeDL.getInstance().destroyProcessById(processId) }
        job?.cancel()
        _state.value = DownloadState.Idle
    }

    private fun File.hasMedia() =
        listFiles()?.any { it.isFile && it.length() > 0 && !it.name.endsWith(".part") } == true

    private fun buildRequest(url: String, audioOnly: Boolean, folder: File, attempt: Attempt) = YoutubeDLRequest(url).apply {
        addOption("--no-playlist")
        addOption("--no-mtime")
        addOption("--retries", "3")
        addOption("--socket-timeout", "30")
        addOption("-o", File(folder, "%(title).80B.%(ext)s").absolutePath)
        // Con Tor activo, yt-dlp también sale por Tor: si no, la web vería la IP real.
        torProxy()?.let { addOption("--proxy", it) }
        if (attempt.alternativeClients && url.contains("youtu", ignoreCase = true)) {
            addOption("--extractor-args", "youtube:player_client=default,mweb,tv,web_embedded")
        }
        if (audioOnly) {
            addOption("-f", "ba/b")
            addOption("-x")
            addOption("--audio-format", "mp3")
        } else if (attempt.simpleFormat) {
            // Un solo archivo con vídeo y audio: no hace falta unir pistas con ffmpeg.
            addOption("-f", "b[ext=mp4][height<=1080]/b[height<=1080]/b")
        } else {
            addOption("-f", "bv*[height<=1080][ext=mp4]+ba[ext=m4a]/bv*[height<=1080]+ba/b[height<=1080]/b")
            addOption("--merge-output-format", "mp4")
        }
    }

    private fun torProxy(): String? = runCatching {
        val proxy = java.net.ProxySelector.getDefault()?.select(java.net.URI("https://www.youtube.com"))?.firstOrNull()
        val address = proxy?.address() as? java.net.InetSocketAddress
        if (proxy?.type() == java.net.Proxy.Type.SOCKS && address != null) "socks5://${address.hostString}:${address.port}" else null
    }.getOrNull()

    /** Lo mueve a Descargas/NexusChat, donde el usuario puede encontrarlo con cualquier app. */
    private fun publish(file: File, audioOnly: Boolean): Triple<Uri, String, String> {
        val mime = when {
            audioOnly || file.extension.equals("mp3", true) -> "audio/mpeg"
            file.extension.equals("webm", true) -> "video/webm"
            file.extension.equals("m4a", true) -> "audio/mp4"
            else -> "video/mp4"
        }
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, file.name)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, "Download/NexusChat")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("No se pudo crear el archivo en Descargas")
        context.contentResolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
            ?: error("No se pudo escribir el archivo")
        return Triple(uri, mime, file.name)
    }

    private fun isFinal(raw: String) = raw.contains("Unsupported URL", true) ||
        raw.contains("Private video", true) || raw.contains("is not a valid URL", true) ||
        raw.contains("Video unavailable", true)

    private fun friendly(raw: String): String = when {
        raw.contains("Unsupported URL", true) || raw.contains("is not a valid URL", true) ->
            tr("Ese enlace no es de una web compatible.")
        raw.contains("Video unavailable", true) -> tr("Ese vídeo ya no está disponible.")
        raw.contains("Sign in to confirm", true) || raw.contains("not a bot", true) ->
            tr("YouTube pide verificar que no eres un robot desde esta conexión. Prueba con otra red o desactiva Tor un momento.")
        raw.contains("Private", true) || raw.contains("login", true) || raw.contains("cookies", true) ->
            tr("Ese contenido es privado o la web exige iniciar sesión para verlo.")
        raw.contains("rate", true) && raw.contains("limit", true) ->
            tr("La web está limitando las descargas. Espera unos minutos.")
        raw.contains("No space", true) -> tr("No queda espacio en el teléfono.")
        raw.contains("Unable to download", true) || raw.contains("network", true) ||
            raw.contains("timed out", true) || raw.contains("resolve", true) ->
            tr("No se pudo conectar. Revisa tu conexión o prueba otro enlace.")
        raw.contains("python", true) || raw.contains("init", true) ->
            tr("El descargador no pudo arrancar en este teléfono.")
        else -> tr("No se pudo descargar ese enlace.")
    }

    private companion object {
        const val TAG = "VideoDownloader"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoDownloaderScreen(
    navController: NavController,
    initialUrl: String = "",
    viewModel: VideoDownloaderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    var url by remember { mutableStateOf(initialUrl) }
    val running = state is DownloadState.Running
    var showDetail by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Descargar vídeos")) },
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
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                tr("Pega el enlace de un vídeo de YouTube, Facebook, X, Instagram, TikTok y muchas más."),
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTokens.Color.TextSecondary
            )
            OutlinedTextField(
                value = url,
                onValueChange = { url = it },
                label = { Text(tr("Enlace")) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let { pasted ->
                            // Al copiar desde una app se suele colar texto alrededor: se queda solo el enlace.
                            url = Regex("""https?://\S+""").find(pasted)?.value ?: pasted.trim()
                        }
                    }) { Text(tr("Pegar")) }
                }
            )
            NexusButton(
                text = tr("Descargar vídeo"),
                onClick = { showDetail = false; viewModel.download(url, audioOnly = false) },
                enabled = url.trim().startsWith("http") && !running
            )
            OutlinedButton(
                onClick = { showDetail = false; viewModel.download(url, audioOnly = true) },
                enabled = url.trim().startsWith("http") && !running,
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("Solo el audio (MP3)")) }

            when (val current = state) {
                is DownloadState.Running -> {
                    if (current.progress > 0f) {
                        LinearProgressIndicator(progress = { current.progress }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Text(current.detail, style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted, maxLines = 2)
                    TextButton(onClick = { viewModel.cancel() }) { Text(tr("Cancelar")) }
                }
                is DownloadState.Done -> {
                    Text(
                        tr("Guardado en Descargas/NexusChat: %s", current.fileName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusTokens.Color.Online
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW)
                                        .setDataAndType(current.uri, current.mime)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                )
                            }
                        }) { Text(tr("Abrir")) }
                        TextButton(onClick = {
                            context.startActivity(
                                Intent.createChooser(
                                    Intent(Intent.ACTION_SEND)
                                        .setType(current.mime)
                                        .putExtra(Intent.EXTRA_STREAM, current.uri)
                                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION),
                                    null
                                )
                            )
                        }) { Text(tr("Compartir")) }
                    }
                }
                is DownloadState.Failed -> {
                    Text(current.reason, style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.Error)
                    if (current.detail.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showDetail = !showDetail }) {
                                Text(if (showDetail) tr("Ocultar detalles") else tr("Ver detalles"))
                            }
                            TextButton(onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("error", current.detail))
                                android.widget.Toast.makeText(context, tr("Copiado"), android.widget.Toast.LENGTH_SHORT).show()
                            }) { Text(tr("Copiar error")) }
                        }
                        if (showDetail) {
                            Text(current.detail, style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted)
                        }
                    }
                }
                DownloadState.Idle -> Unit
            }

            Text(
                tr("Descarga solo contenido tuyo o que tengas permiso para guardar."),
                style = MaterialTheme.typography.bodySmall,
                color = NexusTokens.Color.TextMuted
            )
        }
    }
}
