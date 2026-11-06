package com.Azelmods.App.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CreateNewFolder
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil3.compose.AsyncImage
import com.Azelmods.App.data.stickers.StickerPackData
import com.Azelmods.App.data.stickers.StickerRepository
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.theme.NexusTokens
import com.airbnb.lottie.compose.LottieConstants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.io.File
import javax.inject.Inject

private class AnimatedPack(val title: String, emojis: String) {
    val stickers: List<String> = splitGraphemes(emojis).map { ANIMATED_STICKER_PREFIX + it }
}

private val ANIMATED_PACKS = listOf(
    AnimatedPack("Caras", "😀😃😄😁😆😅🤣😂🙂😉😊😇🥰😍🤩😘😋😜🤪🤔🤫😴😎🥳🥺😢😭😤😡🤯😱🤗🤡💀👻👽🤖💩"),
    AnimatedPack("Gestos", "👍👎👏🙌🙏💪👋✌️🤞🤟🤘👌👀🔥✨🎉🎊💯"),
    AnimatedPack("Amor", "❤️🧡💛💚💙💜🖤🤍💔💕💖💘💝"),
    AnimatedPack("Animales", "🐶🐱🦊🐼🐵🦄🐸🐧🦋🐝🐍🦖")
)

@HiltViewModel
class StickerViewModel @Inject constructor(
    @param:ApplicationContext context: Context,
    private val stickers: StickerRepository
) : ViewModel() {
    private val prefs = context.getSharedPreferences("stickers", Context.MODE_PRIVATE)

    val packs: StateFlow<List<StickerPackData>> = stickers.observePacks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _recent = MutableStateFlow(readRecent())
    val recent = _recent.asStateFlow()
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message = _message.asStateFlow()

    private fun readRecent(): List<String> = runCatching {
        val array = JSONArray(prefs.getString("recent", "[]"))
        List(array.length()) { array.getString(it) }
    }.getOrDefault(emptyList())

    fun markUsed(sticker: String) {
        val next = (listOf(sticker) + _recent.value.filter { it != sticker }).take(MAX_RECENT)
        _recent.value = next
        prefs.edit().putString("recent", JSONArray(next).toString()).apply()
    }

    fun saveEdited(file: File, packId: String?, onDone: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            _saving.value = true
            runCatching {
                val target = packId ?: stickers.defaultPack() ?: error("Sin sesión")
                stickers.addImage(target, file.toUri())
            }.onSuccess {
                file.delete()
                kotlinx.coroutines.withContext(Dispatchers.Main) { onDone() }
            }.onFailure { _message.value = tr("No se pudo guardar el sticker") }
            _saving.value = false
        }
    }

    fun createPack(name: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { stickers.createPack(name) } }
    }

    fun removeSticker(packId: String, sticker: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { stickers.removeSticker(packId, sticker) } }
    }

    /** Guarda en "Mis stickers" uno recibido en un chat. */
    fun saveReceived(sticker: String) {
        viewModelScope.launch(Dispatchers.IO) { runCatching { stickers.saveReceived(sticker) } }
    }

    fun share(pack: StickerPackData, onCode: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val code = runCatching { stickers.publish(pack) }.getOrNull()
            if (code == null) {
                _message.value = tr("No se pudo compartir el paquete")
            } else {
                kotlinx.coroutines.withContext(Dispatchers.Main) { onCode(code) }
            }
        }
    }

    fun install(code: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val name = runCatching { stickers.install(code) }.getOrNull()
            _message.value = if (name == null) tr("No existe ningún paquete con ese código") else tr("Añadido: %s", name)
        }
    }

    private companion object {
        const val MAX_RECENT = 24
    }
}

/** Dibuja un sticker: animado de la colección, o imagen o GIF subido por alguien. */
@Composable
fun StickerImage(sticker: String, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, playOnce: Boolean = false) {
    when {
        sticker.startsWith(ANIMATED_STICKER_PREFIX) -> AnimatedEmoji(
            emoji = sticker.removePrefix(ANIMATED_STICKER_PREFIX),
            size = size,
            modifier = modifier,
            iterations = if (playOnce) 1 else LottieConstants.IterateForever
        )
        sticker.startsWith("http") -> AsyncImage(
            model = sticker,
            contentDescription = tr("Sticker"),
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size)
        )
        // Los enviados por versiones antiguas eran simplemente un emoji.
        else -> AnimatedEmoji(sticker, size, modifier, if (playOnce) 1 else LottieConstants.IterateForever)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickerPicker(
    onStickerSelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: StickerViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val recent by viewModel.recent.collectAsState()
    val packs by viewModel.packs.collectAsState()
    val saving by viewModel.saving.collectAsState()
    val message by viewModel.message.collectAsState()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var source by remember { mutableStateOf<Uri?>(null) }
    var askName by remember { mutableStateOf(false) }
    var askCode by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { source = it }

    // Recientes, luego los paquetes propios y al final los animados de la app.
    val titles = listOf(tr("Recientes")) + packs.map { it.name } + ANIMATED_PACKS.map { tr(it.title) }
    val safeTab = tab.coerceIn(titles.indices)
    val ownPack = packs.getOrNull(safeTab - 1)
    val items: List<String> = when {
        safeTab == 0 -> recent
        ownPack != null -> ownPack.stickers
        else -> ANIMATED_PACKS[safeTab - 1 - packs.size].stickers
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(400.dp),
        color = NexusTokens.Color.BgSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 4.dp, top = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(tr("Stickers"), style = MaterialTheme.typography.titleMedium, color = NexusTokens.Color.TextPrimary, modifier = Modifier.weight(1f))
                com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }) {
                    Icon(Icons.Outlined.AddPhotoAlternate, tr("Crear sticker"), tint = MaterialTheme.colorScheme.primary)
                }
                com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { askName = true }) {
                    Icon(Icons.Outlined.CreateNewFolder, tr("Nuevo paquete"), tint = NexusTokens.Color.TextSecondary)
                }
                com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { askCode = true }) {
                    Icon(Icons.Outlined.Download, tr("Añadir paquete con código"), tint = NexusTokens.Color.TextSecondary)
                }
                if (ownPack != null) {
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = {
                        viewModel.share(ownPack) { code ->
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Stickers", code))
                            android.widget.Toast.makeText(context, tr("Código copiado: %s", code), android.widget.Toast.LENGTH_LONG).show()
                        }
                    }) { Icon(Icons.Outlined.Share, tr("Compartir paquete"), tint = NexusTokens.Color.TextSecondary) }
                }
                com.Azelmods.App.ui.components.AnimatedIconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, tr("Cerrar"), tint = NexusTokens.Color.TextSecondary)
                }
            }
            ScrollableTabRow(
                selectedTabIndex = safeTab,
                containerColor = NexusTokens.Color.BgSurface,
                edgePadding = 8.dp,
                divider = {}
            ) {
                titles.forEachIndexed { index, title ->
                    Tab(selected = safeTab == index, onClick = { tab = index }, text = { Text(title, maxLines = 1) })
                }
            }
            message?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextSecondary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(76.dp),
                contentPadding = PaddingValues(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (items.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            if (safeTab == 0) tr("Aquí aparecerán los últimos que uses.")
                            else tr("Aún está vacío. Toca el icono de la foto para crear tu primer sticker."),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexusTokens.Color.TextMuted,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
                items(items, key = { it }) { sticker ->
                    Box(
                        Modifier
                            .padding(4.dp)
                            .size(68.dp)
                            .clip(RoundedCornerShape(NexusTokens.Radius.md))
                            .combinedClickable(
                                onClick = {
                                    viewModel.markUsed(sticker)
                                    onStickerSelected(sticker)
                                },
                                onLongClick = { ownPack?.let { viewModel.removeSticker(it.id, sticker) } }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        // Se reproducen una vez: decenas moviéndose a la vez gastarían batería.
                        StickerImage(sticker, 60.dp, playOnce = true)
                    }
                }
            }
        }
    }

    source?.let { uri ->
        StickerEditorDialog(
            source = uri,
            saving = saving,
            onDismiss = { source = null },
            onSave = { file -> viewModel.saveEdited(file, ownPack?.id) { source = null } }
        )
    }

    if (askName) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { askName = false },
            title = { Text(tr("Nuevo paquete")) },
            text = { OutlinedTextField(name, { name = it.take(30) }, label = { Text(tr("Nombre")) }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    viewModel.createPack(name)
                    askName = false
                }) { Text(tr("Crear")) }
            },
            dismissButton = { TextButton(onClick = { askName = false }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }

    if (askCode) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { askCode = false },
            title = { Text(tr("Añadir paquete con código")) },
            text = { OutlinedTextField(code, { code = it.trim() }, label = { Text(tr("Código")) }, singleLine = true) },
            confirmButton = {
                TextButton(enabled = code.isNotBlank(), onClick = {
                    viewModel.install(code)
                    askCode = false
                }) { Text(tr("Añadir")) }
            },
            dismissButton = { TextButton(onClick = { askCode = false }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }
}
