package com.Azelmods.App.ui.screens.profile

import com.Azelmods.App.i18n.tr

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material.icons.outlined.VideocamOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.Azelmods.App.data.model.User
import com.Azelmods.App.ui.components.AvatarRing
import com.Azelmods.App.ui.components.FullScreenImageViewer
import com.Azelmods.App.ui.components.LoopingVideo
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.ProfileColors
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.components.pressScale
import com.Azelmods.App.ui.components.staggeredEnter
import com.Azelmods.App.ui.theme.NexusTokens
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Identificador de la foto principal en el visor: no pertenece a la galería. */
const val MAIN_SLIDE_ID = "main"

private val COVER_HEIGHT = 196.dp
private val AVATAR_SIZE = 112.dp

/** Una foto o vídeo de perfil. */
data class HeroSlide(
    val id: String,
    val imageUrl: String?,
    val videoUrl: String?,
    /** null en la foto principal: se gestiona aparte de la galería. */
    val galleryItem: ProfileMedia? = null
)

/** Primero la foto o vídeo principal, luego la galería sin repetirlo. */
fun heroSlides(user: User?, media: List<ProfileMedia>): List<HeroSlide> = buildList {
    val video = user?.profileVideoUrl?.takeIf { it.isNotBlank() }
    val photo = user?.photoUrl?.takeIf { it.isNotBlank() }
    if (video != null || photo != null) add(HeroSlide(MAIN_SLIDE_ID, photo, video))
    media.forEach { item ->
        if (item.url != video && item.url != photo) {
            add(HeroSlide(item.id, item.thumbUrl, if (item.isVideo) item.url else null, item))
        }
    }
}

/** Lo que el visor a pantalla completa necesita de cada foto o vídeo. */
fun List<HeroSlide>.toViewerMedia(): List<ProfileMedia> = map { slide ->
    slide.galleryItem ?: ProfileMedia(
        id = MAIN_SLIDE_ID,
        url = slide.videoUrl ?: slide.imageUrl.orEmpty(),
        thumbUrl = slide.imageUrl ?: slide.videoUrl.orEmpty(),
        isVideo = slide.videoUrl != null,
        timestamp = 0L
    )
}

/** "en línea", o cuándo se le vio por última vez, dicho como lo diría una persona. */
fun presenceText(isOnline: Boolean, lastSeen: Long): String {
    if (isOnline) return tr("en línea")
    if (lastSeen <= 0L) return tr("desconectado")
    val then = Calendar.getInstance().apply { timeInMillis = lastSeen }
    val now = Calendar.getInstance()
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(lastSeen))
    val days = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    return when {
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) && days == 0 -> tr("visto hoy a las %s", time)
        now.get(Calendar.YEAR) == then.get(Calendar.YEAR) && days == 1 -> tr("visto ayer a las %s", time)
        else -> tr("visto el %s", SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(lastSeen)))
    }
}

/** Lo que se puede hacer desde un perfil. */
class ProfileActions(
    val onBack: (() -> Unit)? = null,
    val onMessage: () -> Unit = {},
    val onCall: (video: Boolean) -> Unit = {},
    val onEdit: () -> Unit = {},
    val onCustomize: () -> Unit = {},
    val onSettings: () -> Unit = {},
    val onPremium: () -> Unit = {},
    val onAddMedia: () -> Unit = {},
    val onChangeMain: () -> Unit = {},
    val onRemoveVideo: () -> Unit = {}
)

/**
 * El perfil, igual para el propio y para el de los demás: portada arriba y foto de perfil
 * encima. Tocar la foto abre todas las fotos y vídeos de la persona a pantalla completa, para
 * pasarlos deslizando, como en Telegram. Al desplazar, la portada se queda atrás más despacio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    state: ProfileState,
    seed: String,
    isOwn: Boolean,
    actions: ProfileActions,
    onOpenViewer: (index: Int) -> Unit,
    extraContent: @Composable ColumnScope.() -> Unit = {}
) {
    val user = state.user
    val name = user?.displayName?.ifBlank { user.name }?.ifBlank { null } ?: tr("Usuario")
    val colors = ProfileColors.forUser(user?.uid ?: seed, user?.profileColor ?: -1)
    val slides = remember(user?.photoUrl, user?.profileVideoUrl, state.media) { heroSlides(user, state.media) }
    val scroll = rememberScrollState()
    var showMediaMenu by remember { mutableStateOf(false) }
    var showCover by remember { mutableStateOf(false) }

    val coverPx = with(LocalDensity.current) { COVER_HEIGHT.toPx() }
    val collapse by remember { derivedStateOf { (scroll.value / (coverPx * 0.8f)).coerceIn(0f, 1f) } }

    Box(
        Modifier
            .fillMaxSize()
            .background(NexusTokens.Color.BgBase)
    ) {
        Column(Modifier.verticalScroll(scroll)) {
            // Portada y foto: la foto monta sobre el borde inferior de la portada.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(COVER_HEIGHT + AVATAR_SIZE / 2)
            ) {
                ProfileCover(
                    coverUrl = user?.coverUrl,
                    coverVideoUrl = user?.coverVideoUrl,
                    colors = colors,
                    scrollOffset = { scroll.value },
                    onClick = {
                        when {
                            !user?.coverVideoUrl.isNullOrBlank() || !user?.coverUrl.isNullOrBlank() -> showCover = true
                            isOwn -> actions.onCustomize()
                        }
                    }
                )
                ProfileAvatar(
                    name = name,
                    user = user,
                    colors = colors,
                    isOwn = isOwn,
                    isUploading = state.isUploadingProfile,
                    onClick = {
                        when {
                            slides.isNotEmpty() -> onOpenViewer(0)
                            isOwn -> showMediaMenu = true
                        }
                    },
                    onCamera = { showMediaMenu = true },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                )
                if (slides.size > 1) {
                    MediaCountChip(
                        count = slides.size,
                        onClick = { onOpenViewer(0) },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 16.dp, bottom = 8.dp)
                    )
                }
            }

            ProfileIdentity(
                name = name,
                username = user?.username.orEmpty(),
                status = if (isOwn) "" else presenceText(state.isOnline, state.lastSeen),
                online = !isOwn && state.isOnline,
                verified = state.premium.isActiveNow()
            )

            Spacer(Modifier.height(16.dp))
            ProfileActionTiles(isOwn, actions)
            ProfileInfoCard(user)

            if (isOwn && !state.premium.isActiveNow()) {
                PremiumBanner(onClick = actions.onPremium)
            }

            if (isOwn || state.media.isNotEmpty()) {
                NexusSectionHeader(
                    text = tr("Fotos y vídeos"),
                    trailing = if (state.media.isNotEmpty()) {
                        {
                            Text(
                                state.media.size.toString(),
                                style = MaterialTheme.typography.bodySmall,
                                color = NexusTokens.Color.TextMuted
                            )
                        }
                    } else {
                        null
                    }
                )
                ProfileMediaGrid(
                    media = state.media,
                    canAdd = isOwn,
                    isUploading = state.isUploadingMedia,
                    onAdd = actions.onAddMedia,
                    onOpen = { index ->
                        val target = state.media.getOrNull(index)?.id
                        onOpenViewer(slides.indexOfFirst { it.id == target }.coerceAtLeast(0))
                    }
                )
            }

            extraContent()
            Spacer(Modifier.height(32.dp))
        }

        ProfileTopBar(
            name = name,
            collapse = collapse,
            onBack = actions.onBack,
            onSettings = if (isOwn) actions.onSettings else null
        )
    }

    val coverVideo = user?.coverVideoUrl
    if (showCover && !coverVideo.isNullOrBlank()) {
        ProfileMediaViewer(
            media = listOf(
                ProfileMedia(
                    id = MAIN_SLIDE_ID,
                    url = coverVideo,
                    thumbUrl = user.coverUrl.orEmpty(),
                    isVideo = true,
                    timestamp = 0L
                )
            ),
            startIndex = 0,
            canDelete = false,
            onDelete = {},
            onDismiss = { showCover = false }
        )
    } else if (showCover) {
        FullScreenImageViewer(
            imageUrl = user?.coverUrl.orEmpty(),
            senderName = name,
            timestamp = "",
            onDismiss = { showCover = false }
        )
    }

    if (showMediaMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMediaMenu = false },
            containerColor = NexusTokens.Color.BgSurface
        ) {
            Column(Modifier.padding(bottom = 24.dp)) {
                SheetRow(Icons.Outlined.PhotoCamera, tr("Cambiar foto o vídeo de perfil")) {
                    showMediaMenu = false
                    actions.onChangeMain()
                }
                SheetRow(Icons.Outlined.AddPhotoAlternate, tr("Añadir a mis fotos y vídeos")) {
                    showMediaMenu = false
                    actions.onAddMedia()
                }
                SheetRow(Icons.Outlined.Image, tr("Cambiar portada y color")) {
                    showMediaMenu = false
                    actions.onCustomize()
                }
                if (!user?.profileVideoUrl.isNullOrBlank()) {
                    SheetRow(Icons.Outlined.VideocamOff, tr("Quitar el vídeo de perfil")) {
                        showMediaMenu = false
                        actions.onRemoveVideo()
                    }
                }
            }
        }
    }
}

@Composable
private fun SheetRow(icon: ImageVector, label: String, tint: Color = NexusTokens.Color.TextPrimary, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = tint)
        Spacer(Modifier.width(16.dp))
        Text(label, color = tint, style = MaterialTheme.typography.bodyLarge)
    }
}

// ── Portada y foto ────────────────────────────────────────────────────────

/** Se desplaza más despacio que el contenido y respira despacio: da sensación de capas. */
@Composable
private fun ProfileCover(
    coverUrl: String?,
    coverVideoUrl: String?,
    colors: List<Color>,
    scrollOffset: () -> Int,
    onClick: () -> Unit
) {
    val zoom by rememberInfiniteTransition(label = "portada").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Reverse),
        label = "zoom"
    )
    Box(
        Modifier
            .fillMaxWidth()
            .height(COVER_HEIGHT)
            .graphicsLayer {
                val offset = scrollOffset().toFloat()
                translationY = offset * 0.45f
                alpha = (1f - offset / (size.height * 1.4f)).coerceIn(0f, 1f)
                clip = true
            }
            .background(Brush.linearGradient(colors, Offset.Zero, Offset(1400f, 700f)))
            .clickable(onClick = onClick)
    ) {
        if (!coverVideoUrl.isNullOrBlank()) {
            LoopingVideo(coverVideoUrl, Modifier.fillMaxSize())
        } else if (!coverUrl.isNullOrBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = tr("Portada"),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(zoom)
            )
        }
        // Que la barra de arriba se lea sobre cualquier portada.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.45f),
                        0.5f to Color.Transparent
                    )
                )
        )
    }
}

@Composable
private fun ProfileAvatar(
    name: String,
    user: User?,
    colors: List<Color>,
    isOwn: Boolean,
    isUploading: Boolean,
    onClick: () -> Unit,
    onCamera: () -> Unit,
    modifier: Modifier = Modifier
) {
    val entrance = remember { Animatable(0.72f) }
    LaunchedEffect(Unit) {
        entrance.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val interaction = remember { MutableInteractionSource() }
    val video = user?.profileVideoUrl

    Box(modifier.size(AVATAR_SIZE)) {
        Box(
            Modifier
                .fillMaxSize()
                .scale(entrance.value)
                .pressScale(interaction, 0.94f)
                .clip(CircleShape)
                .background(NexusTokens.Color.BgBase)
                .clickable(interactionSource = interaction, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            UserAvatar(
                name = name,
                photoUrl = user?.photoUrl,
                size = AVATAR_SIZE - 4.dp,
                ring = AvatarRing(colors, width = 3.dp, rotating = true),
                content = if (!video.isNullOrBlank()) {
                    { LoopingVideo(video, Modifier.fillMaxSize()) }
                } else {
                    null
                }
            )
            if (isUploading) {
                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(28.dp))
            }
        }
        if (isOwn) {
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .scale(entrance.value)
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, NexusTokens.Color.BgBase, CircleShape)
                    .clickable(onClick = onCamera),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = tr("Cambiar foto o vídeo de perfil"),
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** Dice que hay más fotos que la que se ve, y las abre. */
@Composable
private fun MediaCountChip(count: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .staggeredEnter(2)
            .clip(RoundedCornerShape(NexusTokens.Radius.pill))
            .background(NexusTokens.Color.BgElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Collections, null, tint = NexusTokens.Color.TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(count.toString(), style = MaterialTheme.typography.labelMedium, color = NexusTokens.Color.TextPrimary)
    }
}

@Composable
private fun ProfileIdentity(name: String, username: String, status: String, online: Boolean, verified: Boolean) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(420, delayMillis = 80, easing = FastOutSlowInEasing)) }
    val rise = with(LocalDensity.current) { 16.dp.toPx() }
    Column(
        Modifier
            .padding(start = 16.dp, end = 16.dp, top = 10.dp)
            .graphicsLayer {
                alpha = progress.value
                translationY = (1f - progress.value) * rise
            }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = NexusTokens.Color.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (verified) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Filled.Verified, tr("Premium"), tint = NexusTokens.Color.Gold, modifier = Modifier.size(22.dp))
            }
        }
        val handle = username.removePrefix("@")
        if (handle.isNotBlank()) {
            Text("@$handle", style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextMuted)
        }
        if (status.isNotBlank()) {
            Row(Modifier.padding(top = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                if (online) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NexusTokens.Color.Online)
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    status,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (online) NexusTokens.Color.Online else NexusTokens.Color.TextMuted
                )
            }
        }
    }
}

/** La barra flota sobre la portada y se vuelve sólida, con el nombre, al desplazar. */
@Composable
private fun ProfileTopBar(name: String, collapse: Float, onBack: (() -> Unit)?, onSettings: (() -> Unit)?) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(NexusTokens.Color.BgSurface.copy(alpha = collapse))
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            com.Azelmods.App.ui.components.AnimatedIconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"), tint = Color.White)
            }
        } else {
            Spacer(Modifier.width(16.dp))
        }
        AnimatedVisibility(
            visible = collapse > 0.9f,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                name,
                color = NexusTokens.Color.TextPrimary,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (collapse <= 0.9f) Spacer(Modifier.weight(1f))
        if (onSettings != null) {
            com.Azelmods.App.ui.components.AnimatedIconButton(onClick = onSettings) {
                Icon(Icons.Outlined.Settings, tr("Ajustes"), tint = Color.White)
            }
        }
    }
}

// ── Información ───────────────────────────────────────────────────────────

@Composable
private fun ProfileActionTiles(isOwn: Boolean, actions: ProfileActions) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (isOwn) {
            ActionTile(Icons.Outlined.Edit, tr("Editar"), 0, Modifier.weight(1f), actions.onEdit)
            ActionTile(Icons.Outlined.Palette, tr("Personalizar"), 1, Modifier.weight(1f), actions.onCustomize)
            ActionTile(Icons.Outlined.Settings, tr("Ajustes"), 2, Modifier.weight(1f), actions.onSettings)
        } else {
            ActionTile(Icons.AutoMirrored.Outlined.Message, tr("Mensaje"), 0, Modifier.weight(1f), actions.onMessage)
            ActionTile(Icons.Outlined.Call, tr("Llamar"), 1, Modifier.weight(1f)) { actions.onCall(false) }
            ActionTile(Icons.Outlined.Videocam, tr("Vídeo"), 2, Modifier.weight(1f)) { actions.onCall(true) }
        }
    }
}

@Composable
private fun ActionTile(icon: ImageVector, label: String, index: Int, modifier: Modifier, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier
            .staggeredEnter(index)
            .pressScale(interaction, 0.94f)
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.BgSurface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = NexusTokens.Color.TextPrimary)
    }
}

@Composable
private fun ProfileInfoCard(user: User?) {
    // Las cuentas antiguas tienen guardada esta frase de relleno: no la escribió nadie.
    val bio = user?.bio.orEmpty().takeUnless { it == "Hey there! I'm using Nexus Chat" }.orEmpty()
    val createdAt = user?.createdAt ?: 0L
    if (bio.isBlank() && createdAt <= 0L) return

    Surface(
        color = NexusTokens.Color.BgSurface,
        shape = RoundedCornerShape(NexusTokens.Radius.xl),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .staggeredEnter(3)
    ) {
        Column(Modifier.padding(vertical = 6.dp)) {
            if (bio.isNotBlank()) InfoLine(bio, tr("Biografía"))
            if (createdAt > 0L) {
                InfoLine(
                    SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date(createdAt)),
                    tr("Miembro desde")
                )
            }
        }
    }
}

@Composable
private fun InfoLine(value: String, label: String) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(value, style = MaterialTheme.typography.bodyLarge, color = NexusTokens.Color.TextPrimary)
        Text(label, style = MaterialTheme.typography.labelMedium, color = NexusTokens.Color.TextMuted)
    }
}

@Composable
private fun PremiumBanner(onClick: () -> Unit) {
    val shine = rememberInfiniteTransition(label = "brillo").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1_600), RepeatMode.Reverse),
        label = "brilloValor"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.Gold.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Filled.Verified,
            null,
            tint = NexusTokens.Color.Gold,
            modifier = Modifier.alpha(shine.value)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(tr("NexusChat Premium"), style = MaterialTheme.typography.titleSmall, color = NexusTokens.Color.TextPrimary)
            Text(
                tr("Insignia, colores exclusivos y galería sin límite"),
                style = MaterialTheme.typography.bodySmall,
                color = NexusTokens.Color.TextSecondary
            )
        }
        Icon(Icons.Filled.PlayArrow, null, tint = NexusTokens.Color.Gold, modifier = Modifier.size(18.dp))
    }
}
