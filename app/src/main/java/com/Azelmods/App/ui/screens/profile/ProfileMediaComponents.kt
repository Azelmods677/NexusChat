package com.Azelmods.App.ui.screens.profile

import com.Azelmods.App.i18n.tr

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.components.pressScale
import com.Azelmods.App.ui.components.staggeredEnter
import com.Azelmods.App.ui.theme.NexusTokens
import net.engawapg.lib.zoomable.rememberZoomState
import net.engawapg.lib.zoomable.zoomable

private const val GRID_COLUMNS = 3

/** Cuadrícula de fotos y vídeos. Va dentro de una pantalla desplazable, por eso no es perezosa. */
@Composable
fun ProfileMediaGrid(
    media: List<ProfileMedia>,
    canAdd: Boolean,
    isUploading: Boolean,
    onAdd: () -> Unit,
    onOpen: (Int) -> Unit
) {
    val cells: List<Int?> = buildList {
        if (canAdd) add(null)
        addAll(media.indices)
    }
    Column(
        modifier = Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        cells.chunked(GRID_COLUMNS).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEachIndexed { columnIndex, index ->
                    val cellModifier = Modifier
                        .weight(1f)
                        .aspectRatio(1f)
                        .staggeredEnter(rowIndex * GRID_COLUMNS + columnIndex)
                    if (index == null) {
                        AddMediaCell(isUploading, onAdd, cellModifier)
                    } else {
                        MediaCell(media[index], { onOpen(index) }, cellModifier)
                    }
                }
                repeat(GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun AddMediaCell(isUploading: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(NexusTokens.Radius.md))
            .background(NexusTokens.Color.BgSurface)
            .clickable(enabled = !isUploading, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isUploading) {
            CircularProgressIndicator(
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Outlined.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(4.dp))
                Text(tr("Añadir"), style = MaterialTheme.typography.labelSmall, color = NexusTokens.Color.TextMuted)
            }
        }
    }
}

@Composable
private fun MediaCell(item: ProfileMedia, onClick: () -> Unit, modifier: Modifier) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, pressedScale = 0.95f)
            .clip(RoundedCornerShape(NexusTokens.Radius.md))
            .background(NexusTokens.Color.BgElevated)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
    ) {
        AsyncImage(
            model = item.thumbUrl,
            contentDescription = if (item.isVideo) tr("Vídeo del perfil") else tr("Foto del perfil"),
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        if (item.isVideo) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** Visor a pantalla completa: deslizar para cambiar, pellizcar las fotos para ampliar. */
@Composable
fun ProfileMediaViewer(
    media: List<ProfileMedia>,
    startIndex: Int,
    canDelete: Boolean,
    onDelete: (ProfileMedia) -> Unit,
    onSetMain: ((ProfileMedia) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    if (media.isEmpty()) {
        LaunchedEffect(Unit) { onDismiss() }
        return
    }
    val pager = rememberPagerState(initialPage = startIndex.coerceIn(media.indices)) { media.size }
    var confirmDelete by remember { mutableStateOf(false) }
    val entrance = remember { Animatable(0f) }
    LaunchedEffect(Unit) { entrance.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = entrance.value
                    val scale = 0.92f + 0.08f * entrance.value
                    scaleX = scale
                    scaleY = scale
                }
                .background(Color.Black)
        ) {
            HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
                val item = media[page]
                if (item.isVideo) {
                    ViewerVideo(item.url, active = pager.currentPage == page)
                } else {
                    AsyncImage(
                        model = item.url,
                        contentDescription = tr("Foto del perfil"),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .zoomable(rememberZoomState())
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                // Una marca por foto, como en las historias: se ve cuántas hay y dónde estás.
                if (media.size > 1) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        media.indices.forEach { index ->
                            val active = pager.currentPage == index
                            val color by animateColorAsState(
                                if (active) Color.White else Color.White.copy(alpha = 0.32f),
                                label = "marcaVisor"
                            )
                            Box(
                                Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(color)
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, tr("Cerrar"), tint = Color.White) }
                    Text(
                        "${pager.currentPage + 1} / ${media.size}",
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    val current = media.getOrNull(pager.currentPage)
                    // La foto principal se cambia desde el perfil, no se borra desde aquí.
                    if (current != null && current.id != MAIN_SLIDE_ID) {
                        if (onSetMain != null) {
                            IconButton(onClick = {
                                onSetMain(current)
                                onDismiss()
                            }) {
                                Icon(Icons.Outlined.StarOutline, tr("Poner como foto de perfil"), tint = Color.White)
                            }
                        }
                        if (canDelete) {
                            IconButton(onClick = { confirmDelete = true }) {
                                Icon(Icons.Outlined.DeleteOutline, tr("Eliminar"), tint = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text(tr("¿Eliminar de tu perfil?")) },
            text = { Text(tr("Se borrará para todos. No se puede deshacer."), color = NexusTokens.Color.TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    media.getOrNull(pager.currentPage)?.let(onDelete)
                    if (media.size <= 1) onDismiss()
                }) { Text(tr("Eliminar"), color = NexusTokens.Color.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text(tr("Cancelar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }
}

@Composable
private fun ViewerVideo(url: String, active: Boolean) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }
    LaunchedEffect(active) { player.playWhenReady = active }
    DisposableEffect(player) { onDispose { player.release() } }
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { PlayerView(it).apply { this.player = player } }
    )
}
