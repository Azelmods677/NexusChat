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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface DownloadState {
    data object Idle : DownloadState
    data class Running(val progress: Float, val detail: String) : DownloadState
    data class Done(val uri: Uri, val fileName: String, val mime: String) : DownloadState
    data class Failed(val reason: String) : DownloadState
}

/** Descarga con yt-dlp, integrado en la app: YouTube, Facebook, X, Instagram, TikTok y más. */
@HiltViewModel
class VideoDownloaderViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context
) : ViewModel() {
    private val _state = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val state = _state.asStateFlow()
    private var initialized = false

    fun download(url: String, audioOnly: Boolean) {
        if (_state.value is DownloadState.Running) return
        _state.value = DownloadState.Running(0f, tr("Preparando…"))
        viewModelScope.launch(Dispatchers.IO) {
            val folder = File(context.cacheDir, "downloads").apply {
                deleteRecursively()
                mkdirs()
            }
            runCatching {
                prepare()
                val request = YoutubeDLRequest(url.trim()).apply {
                    addOption("--no-playlist")
                    addOption("-o", File(folder, "%(title).80s.%(ext)s").absolutePath)
                    if (audioOnly) {
                        addOption("-x")
                        addOption("--audio-format", "mp3")
                    } else {
                        addOption("-f", "bv*[height<=1080]+ba/b[height<=1080]/b")
                        addOption("--merge-output-format", "mp4")
                    }
                }
                val onProgress: (Float, Long, String) -> Unit = { progress, _, line ->
                    _state.value = DownloadState.Running((progress / 100f).coerceIn(0f, 1f), line.take(70))
                }
                try {
                    YoutubeDL.getInstance().execute(request, null, onProgress)
                } catch (first: Exception) {
                    // Las webs cambian a menudo: se trae la versión más nueva de yt-dlp y se reintenta una vez.
                    Log.w(TAG, "Primer intento fallido; se actualiza yt-dlp", first)
                    _state.value = DownloadState.Running(0f, tr("Actualizando el descargador…"))
                    runCatching { YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.NIGHTLY) }
                        .onFailure { Log.w(TAG, "No se pudo actualizar yt-dlp", it) }
                    folder.listFiles()?.forEach { it.delete() }
                    YoutubeDL.getInstance().execute(request, null, onProgress)
                }
                val file = folder.listFiles()?.filter { it.isFile }?.maxByOrNull { it.length() } ?: error("No se generó ningún archivo")
                publish(file, audioOnly)
            }.onSuccess { (uri, mime, name) ->
                _state.value = DownloadState.Done(uri, name, mime)
            }.onFailure {
                Log.w(TAG, "Descarga fallida", it)
                _state.value = DownloadState.Failed(friendly(it.message.orEmpty()))
            }
            folder.deleteRecursively()
        }
    }

    private fun prepare() {
        if (initialized) return
        YoutubeDL.getInstance().init(context)
        FFmpeg.getInstance().init(context)
        initialized = true
        // Las webs cambian a menudo: se intenta traer la versión más reciente, sin depender de ello.
        runCatching { YoutubeDL.getInstance().updateYoutubeDL(context, YoutubeDL.UpdateChannel.STABLE) }
            .onFailure { Log.w(TAG, "No se pudo actualizar yt-dlp", it) }
    }

    /** Lo mueve a Descargas/NexusChat, donde el usuario puede encontrarlo con cualquier app. */
    private fun publish(file: File, audioOnly: Boolean): Triple<Uri, String, String> {
        val mime = if (audioOnly) "audio/mpeg" else "video/mp4"
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

    private fun friendly(raw: String): String = when {
        raw.contains("Unsupported URL", true) -> tr("Ese enlace no es de una web compatible.")
        raw.contains("Private", true) || raw.contains("login", true) -> tr("Ese vídeo es privado o exige iniciar sesión.")
        raw.contains("Unable to", true) || raw.contains("network", true) -> tr("No se pudo conectar. Revisa tu conexión o prueba otro enlace.")
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
                        clipboard.primaryClip?.getItemAt(0)?.coerceToText(context)?.toString()?.let { url = it.trim() }
                    }) { Text(tr("Pegar")) }
                }
            )
            NexusButton(
                text = tr("Descargar vídeo"),
                onClick = { viewModel.download(url, audioOnly = false) },
                enabled = url.startsWith("http") && !running
            )
            OutlinedButton(
                onClick = { viewModel.download(url, audioOnly = true) },
                enabled = url.startsWith("http") && !running,
                modifier = Modifier.fillMaxWidth()
            ) { Text(tr("Solo el audio (MP3)")) }

            when (val current = state) {
                is DownloadState.Running -> {
                    LinearProgressIndicator(progress = { current.progress }, modifier = Modifier.fillMaxWidth())
                    Text(current.detail, style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted, maxLines = 2)
                }
                is DownloadState.Done -> {
                    Text(
                        tr("Guardado en Descargas/NexusChat: %s", current.fileName),
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusTokens.Color.Online
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW)
                                    .setDataAndType(current.uri, current.mime)
                                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            )
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
                is DownloadState.Failed -> Text(current.reason, style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.Error)
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
