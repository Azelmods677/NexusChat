package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.foundation.border
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material.icons.outlined.ViewCarousel
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.components.BubbleStyle
import com.Azelmods.App.ui.components.ChatEntryStyle
import com.Azelmods.App.ui.components.ChatLook
import com.Azelmods.App.ui.components.ChatRowFrame
import com.Azelmods.App.ui.components.ChatRowStyle
import com.Azelmods.App.ui.components.NavBarStyle
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.NexusSwitchRow
import com.Azelmods.App.ui.components.ProfileColors
import com.Azelmods.App.ui.components.ReceiptMark
import com.Azelmods.App.ui.components.TickStyle
import com.Azelmods.App.ui.components.chatBubbleShape
import com.Azelmods.App.ui.components.chatBubbleSurface
import com.Azelmods.App.ui.components.chatEntryModifier
import com.Azelmods.App.ui.theme.AppTheme
import com.Azelmods.App.ui.theme.NexusTokens

private enum class StudioTab(val title: String) {
    ROWS("Filas"), BUBBLES("Burbujas"), ENTRY("Campo"), HOME("Inicio"), SETTINGS("Ajustes")
}

/**
 * Todo el aspecto de los chats en un sitio. La vista previa se queda fija arriba y usa las
 * mismas piezas que la app real: lo que se ve aquí es exactamente lo que se verá en los chats.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatStudioScreen(navController: NavController) {
    var tab by rememberSaveable { mutableStateOf(StudioTab.ROWS) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Estudio de diseño")) },
                navigationIcon = {
                    com.Azelmods.App.ui.components.AnimatedIconButton(onClick = { navController.popBackStack() }) {
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
                .consumeWindowInsets(padding)
                .imePadding()
                .fillMaxSize()
        ) {
            // En Inicio y Ajustes cada opción lleva su propia miniatura; la conversación de
            // ejemplo solo quitaría sitio.
            androidx.compose.animation.AnimatedVisibility(tab != StudioTab.HOME && tab != StudioTab.SETTINGS) {
                LivePreview()
            }
            ScrollableTabRow(
                selectedTabIndex = tab.ordinal,
                containerColor = NexusTokens.Color.BgBase,
                edgePadding = 8.dp,
                divider = {}
            ) {
                StudioTab.entries.forEach {
                    Tab(selected = tab == it, onClick = { tab = it }, text = { Text(tr(it.title), maxLines = 1) })
                }
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp)
            ) {
                when (tab) {
                    StudioTab.ROWS -> RowOptions()
                    StudioTab.BUBBLES -> BubbleOptions()
                    StudioTab.ENTRY -> EntryOptions()
                    StudioTab.HOME -> HomeOptions()
                    StudioTab.SETTINGS -> SettingsOptions()
                }
            }
        }
    }
}

@Composable
private fun LivePreview() {
    val rowStyle by ChatLook.rowStyle.collectAsState()
    val bubbleStyle by ChatLook.bubbleStyle.collectAsState()
    val tickStyle by ChatLook.tickStyle.collectAsState()
    val entryStyle by ChatLook.entryStyle.collectAsState()
    val ownColor by ChatLook.ownBubbleColor.collectAsState()
    val peerColor by ChatLook.peerBubbleColor.collectAsState()
    val textScale by ChatLook.textScale.collectAsState()
    val hint by ChatLook.entryHint.collectAsState()
    val accent = MaterialTheme.colorScheme.primary
    val sample = remember { ProfileColors.palettes[2] }

    Column(Modifier.background(NexusTokens.Color.BgSurface.copy(alpha = 0.5f))) {
        ChatRowFrame(rowStyle, sample, null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                com.Azelmods.App.ui.components.UserAvatar(tr("Lucía"), null, size = 40.dp, backgroundColor = sample.first())
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(tr("Lucía"), style = MaterialTheme.typography.titleSmall, color = NexusTokens.Color.TextPrimary)
                    Text(tr("¿Vienes esta noche?"), style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextSecondary)
                }
            }
        }
        Column(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            PreviewBubble(tr("¿Vienes esta noche?"), false, bubbleStyle, peerColor?.let(::Color) ?: NexusTokens.Color.BgElevated, accent, textScale) {}
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                PreviewBubble(tr("¡Claro, voy!"), true, bubbleStyle, ownColor?.let(::Color) ?: MaterialTheme.colorScheme.primaryContainer, accent, textScale) {
                    ReceiptMark(tickStyle, accent, double = true, read = true, modifier = Modifier.size(width = 18.dp, height = 12.dp))
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val colored = entryStyle == ChatEntryStyle.SOCIAL || entryStyle == ChatEntryStyle.FLAT
                val tone = if (colored) accent else NexusTokens.Color.TextMuted
                when (entryStyle) {
                    ChatEntryStyle.PHOTO -> {
                        val me = com.Azelmods.App.data.profile.rememberMyProfile()
                        Box(
                            Modifier
                                .padding(end = 6.dp)
                                .size(38.dp)
                                .border(1.4.dp, accent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            com.Azelmods.App.ui.components.UserAvatar(me.name, me.photoUrl, size = 33.dp)
                        }
                    }

                    ChatEntryStyle.SOCIAL -> {
                        Icon(Icons.Default.Add, null, tint = tone, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Outlined.PhotoCamera, null, tint = tone, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(6.dp))
                    }

                    else -> {
                        Icon(Icons.Default.Add, null, tint = tone, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                    }
                }
                Row(
                    Modifier
                        .weight(1f)
                        .heightIn(min = 40.dp)
                        .then(chatEntryModifier(entryStyle, accent))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entryStyle != ChatEntryStyle.FLAT) {
                        Icon(Icons.Outlined.EmojiEmotions, null, tint = tone, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        hint.ifBlank { tr("Mensaje") },
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusTokens.Color.TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                    if (entryStyle == ChatEntryStyle.FLAT) {
                        Icon(Icons.Outlined.EmojiEmotions, null, tint = tone, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Default.Mic, null, tint = if (colored) accent else NexusTokens.Color.TextSecondary, modifier = Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun PreviewBubble(
    text: String,
    own: Boolean,
    style: BubbleStyle,
    color: Color,
    accent: Color,
    textScale: Float,
    trailing: @Composable () -> Unit
) {
    Row(
        Modifier
            .then(chatBubbleSurface(style, chatBubbleShape(style, own), color, accent))
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text, fontSize = (15 * textScale).sp, color = NexusTokens.Color.TextPrimary)
        Spacer(Modifier.width(6.dp))
        trailing()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> ChoiceChips(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    FlowRow(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(selected = option == selected, onClick = { onSelect(option) }, label = { Text(tr(label(option))) })
        }
    }
}

@Composable
private fun RowOptions() {
    val style by ChatLook.rowStyle.collectAsState()
    val sample = remember { ProfileColors.palettes[2] }
    val tint by ChatLook.rowTint.collectAsState()
    val effect by ChatLook.avatarEffect.collectAsState()
    NexusSectionHeader(tr("Color de las filas"))
    ChoiceChips(com.Azelmods.App.ui.components.RowTint.entries, tint, { it.title }, ChatLook::setRowTint)
    NexusSectionHeader(tr("Foto de perfil"))
    ChoiceChips(com.Azelmods.App.ui.components.AvatarEffect.entries, effect, { it.title }, ChatLook::setAvatarEffect)
    NexusSectionHeader(tr("Estilo de fila"))
    ChatRowStyle.entries.forEach { option ->
        Box(
            Modifier
                .fillMaxWidth()
                .clickable { ChatLook.setRowStyle(option) }
                .padding(vertical = 3.dp)
        ) {
            ChatRowFrame(option, sample, null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.Azelmods.App.ui.components.UserAvatar("N", null, size = 44.dp, backgroundColor = sample.first())
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(tr(option.title), style = MaterialTheme.typography.titleMedium, color = NexusTokens.Color.TextPrimary)
                        Text(tr(option.hint), style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextSecondary)
                    }
                    if (option == style) Icon(Icons.Default.Check, tr("Elegido"), tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorChoice(title: String, current: Int?, onSelect: (Int?) -> Unit) {
    NexusSectionHeader(tr(title))
    FlowRow(
        Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(NexusTokens.Color.BgElevated)
                .border(if (current == null) 2.dp else 1.dp, if (current == null) NexusTokens.Color.TextPrimary else NexusTokens.Color.TextMuted, CircleShape)
                .clickable { onSelect(null) },
            contentAlignment = Alignment.Center
        ) { Text("A", color = NexusTokens.Color.TextSecondary) }
        AppTheme.ACCENT_SWATCHES.take(17).forEach { swatch ->
            val dark = androidx.compose.ui.graphics.lerp(swatch.color, Color.Black, 0.45f)
            val argb = dark.toArgb()
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(dark)
                    .then(if (current == argb) Modifier.border(2.dp, NexusTokens.Color.TextPrimary, CircleShape) else Modifier)
                    .clickable { onSelect(argb) }
            )
        }
    }
}

@Composable
private fun BubbleOptions() {
    val bubblePhoto by ChatLook.bubblePhoto.collectAsState()
    NexusSwitchRow(
        title = tr("Foto en la burbuja"),
        subtitle = tr("Una foto pequeña junto a la hora de cada mensaje"),
        icon = Icons.Outlined.Search,
        checked = bubblePhoto,
        onCheckedChange = ChatLook::setBubblePhoto
    )
    val style by ChatLook.bubbleStyle.collectAsState()
    val ticks by ChatLook.tickStyle.collectAsState()
    val own by ChatLook.ownBubbleColor.collectAsState()
    val peer by ChatLook.peerBubbleColor.collectAsState()
    val scale by ChatLook.textScale.collectAsState()

    NexusSectionHeader(tr("Forma de las burbujas"))
    ChoiceChips(BubbleStyle.entries, style, { it.title }, ChatLook::setBubbleStyle)
    NexusSectionHeader(tr("Marcas de enviado y leído"))
    ChoiceChips(TickStyle.entries, ticks, { it.title }, ChatLook::setTickStyle)
    ColorChoice("Color de mis burbujas", own, ChatLook::setOwnBubbleColor)
    ColorChoice("Color de las otras burbujas", peer, ChatLook::setPeerBubbleColor)
    NexusSectionHeader(tr("Tamaño del texto"))
    Slider(
        value = scale,
        onValueChange = ChatLook::setTextScale,
        valueRange = 0.85f..1.4f,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun EntryOptions() {
    val style by ChatLook.entryStyle.collectAsState()
    val hint by ChatLook.entryHint.collectAsState()
    NexusSectionHeader(tr("Forma del campo"))
    ChoiceChips(ChatEntryStyle.entries, style, { it.title }, ChatLook::setEntryStyle)
    val hideEmoji by ChatLook.hideEntryEmoji.collectAsState()
    val entryTranslate by ChatLook.entryTranslate.collectAsState()
    NexusSwitchRow(
        title = tr("Quitar el emoji del campo"),
        subtitle = tr("Más sitio para escribir"),
        icon = Icons.Outlined.Search,
        checked = hideEmoji,
        onCheckedChange = ChatLook::setHideEntryEmoji
    )
    NexusSwitchRow(
        title = tr("Traducir desde el campo"),
        subtitle = tr("Traduce lo que escribes antes de enviarlo"),
        icon = Icons.Outlined.Search,
        checked = entryTranslate,
        onCheckedChange = ChatLook::setEntryTranslate
    )
    val linkPreviews by ChatLook.linkPreviews.collectAsState()
    NexusSwitchRow(
        title = tr("Vista previa de enlaces"),
        subtitle = tr("La prepara tu teléfono y va cifrada. En modo anónimo no se usa."),
        icon = Icons.Outlined.Search,
        checked = linkPreviews,
        onCheckedChange = ChatLook::setLinkPreviews
    )
    NexusSectionHeader(tr("Texto del campo"))
    OutlinedTextField(
        value = hint,
        onValueChange = { ChatLook.setEntryHint(it.take(40)) },
        placeholder = { Text(tr("Mensaje")) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    )
}

@Composable
private fun HomeOptions() {
    val nav by ChatLook.navBarStyle.collectAsState()
    val avatar by ChatLook.homeAvatar.collectAsState()
    val hideFilters by ChatLook.hideFilters.collectAsState()
    val dim by ChatLook.wallpaperDim.collectAsState()

    NexusSectionHeader(tr("Barra inferior"))
    ChoiceChips(NavBarStyle.entries, nav, { it.title }, ChatLook::setNavBarStyle)
    NexusSectionHeader(tr("Oscurecer el fondo"))
    Text(
        tr("Con un fondo de imagen o vídeo, para que los chats se lean bien."),
        style = MaterialTheme.typography.bodySmall,
        color = NexusTokens.Color.TextMuted,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    Slider(
        value = dim,
        onValueChange = ChatLook::setWallpaperDim,
        valueRange = 0f..0.85f,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
    val homeStyle by ChatLook.homeStyle.collectAsState()
    NexusSectionHeader(tr("Diseño de inicio"))
    com.Azelmods.App.ui.components.HomeStyle.entries.forEach { option ->
        LayoutOption(option.title, option.hint, option == homeStyle, { ChatLook.setHomeStyle(option) }) {
            HomeMiniature(option)
        }
    }
    val font by ChatLook.fontStyle.collectAsState()
    val searchBar by ChatLook.homeSearchBar.collectAsState()
    val quickBar by ChatLook.quickBar.collectAsState()
    val ghost by ChatLook.ghostButton.collectAsState()
    val hideArchived by ChatLook.hideArchived.collectAsState()
    val confirmSticker by ChatLook.confirmSticker.collectAsState()
    val storyCards by ChatLook.storyCards.collectAsState()

    NexusSectionHeader(tr("Tipo de letra"))
    ChoiceChips(com.Azelmods.App.ui.components.AppFontStyle.entries, font, { it.title }, ChatLook::setFontStyle)
    NexusSectionHeader(tr("Pantalla de chats"))
    NexusSwitchRow(
        title = tr("Barra de búsqueda"),
        subtitle = tr("Siempre a la vista en lugar del icono"),
        icon = Icons.Outlined.Search,
        checked = searchBar,
        onCheckedChange = ChatLook::setHomeSearchBar
    )
    NexusSwitchRow(
        title = tr("Barra de accesos"),
        subtitle = tr("Chats secretos, canales, guardados, descargas y terminal"),
        icon = Icons.Outlined.Apps,
        checked = quickBar,
        onCheckedChange = ChatLook::setQuickBar
    )
    NexusSwitchRow(
        title = tr("Botón de modo fantasma"),
        subtitle = tr("Actívalo con un toque desde la cabecera"),
        icon = Icons.Outlined.VisibilityOff,
        checked = ghost,
        onCheckedChange = ChatLook::setGhostButton
    )
    NexusSwitchRow(
        title = tr("Ocultar Archivados"),
        subtitle = tr("Quita ese filtro de la lista"),
        icon = Icons.Outlined.Archive,
        checked = hideArchived,
        onCheckedChange = ChatLook::setHideArchived
    )
    val hideStoriesTab by ChatLook.hideStoriesTab.collectAsState()
    NexusSwitchRow(
        title = tr("Ocultar la pestaña de historias"),
        subtitle = tr("Deja solo chats, llamadas y perfil abajo"),
        icon = Icons.Outlined.VisibilityOff,
        checked = hideStoriesTab,
        onCheckedChange = ChatLook::setHideStoriesTab
    )
    NexusSwitchRow(
        title = tr("Historias en tarjetas"),
        subtitle = tr("Una fila de tarjetas con su imagen, arriba de la lista"),
        icon = Icons.Outlined.ViewCarousel,
        checked = storyCards,
        onCheckedChange = ChatLook::setStoryCards
    )
    NexusSwitchRow(
        title = tr("Confirmar antes de enviar stickers"),
        subtitle = tr("Evita enviarlos sin querer"),
        icon = Icons.Outlined.TaskAlt,
        checked = confirmSticker,
        onCheckedChange = ChatLook::setConfirmSticker
    )
    NexusSwitchRow(
        title = tr("Tu foto en la cabecera"),
        subtitle = tr("Junto al título de la lista de chats"),
        icon = Icons.Outlined.AccountCircle,
        checked = avatar,
        onCheckedChange = ChatLook::setHomeAvatar
    )
    NexusSwitchRow(
        title = tr("Ocultar los filtros"),
        subtitle = tr("Quita Todos, No leídos, Grupos y Archivados"),
        icon = Icons.Outlined.FilterList,
        checked = hideFilters,
        onCheckedChange = ChatLook::setHideFilters
    )
}

@Composable
private fun SettingsOptions() {
    val style by ChatLook.settingsStyle.collectAsState()
    NexusSectionHeader(tr("Diseño de ajustes"))
    com.Azelmods.App.ui.components.SettingsStyle.entries.forEach { option ->
        LayoutOption(option.title, option.hint, option == style, { ChatLook.setSettingsStyle(option) }) {
            SettingsMiniature(option)
        }
    }
}

/** Una opción de diseño con su miniatura: se ve lo que hace antes de elegirla. */
@Composable
private fun LayoutOption(
    title: String,
    hint: String,
    selected: Boolean,
    onClick: () -> Unit,
    miniature: @Composable () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(NexusTokens.Radius.xl))
            .background(if (selected) accent.copy(alpha = 0.14f) else NexusTokens.Color.BgSurface)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        miniature()
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tr(title), style = MaterialTheme.typography.titleSmall, color = NexusTokens.Color.TextPrimary)
            Text(tr(hint), style = MaterialTheme.typography.bodySmall, color = NexusTokens.Color.TextMuted)
        }
        RadioButton(selected = selected, onClick = onClick, colors = RadioButtonDefaults.colors(selectedColor = accent))
    }
}

private val MiniatureSize = 54.dp

@Composable
private fun HomeMiniature(style: com.Azelmods.App.ui.components.HomeStyle) {
    val accent = MaterialTheme.colorScheme.primary
    val line = NexusTokens.Color.TextMuted.copy(alpha = 0.55f)
    val back = NexusTokens.Color.BgBase
    androidx.compose.foundation.Canvas(
        Modifier
            .size(MiniatureSize, MiniatureSize + 22.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(NexusTokens.Radius.md))
            .background(back)
    ) {
        val w = size.width
        val h = size.height
        fun bar(y: Float, x: Float = w * 0.1f, width: Float = w * 0.8f, height: Float = h * 0.045f, color: Color = line) {
            drawRoundRect(
                color,
                androidx.compose.ui.geometry.Offset(x, y),
                androidx.compose.ui.geometry.Size(width, height),
                androidx.compose.ui.geometry.CornerRadius(height / 2f)
            )
        }
        var listTop = h * 0.2f
        when (style) {
            com.Azelmods.App.ui.components.HomeStyle.CLASSIC -> bar(h * 0.07f, width = w * 0.4f, color = accent)
            com.Azelmods.App.ui.components.HomeStyle.PROFILE -> {
                drawCircle(accent, w * 0.08f, androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.09f))
                bar(h * 0.07f, x = w * 0.32f, width = w * 0.45f)
            }
            com.Azelmods.App.ui.components.HomeStyle.NAME -> {
                drawCircle(accent, w * 0.08f, androidx.compose.ui.geometry.Offset(w * 0.18f, h * 0.09f))
                bar(h * 0.07f, x = w * 0.32f, width = w * 0.45f)
                drawRoundRect(line.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(0f, h * 0.17f), androidx.compose.ui.geometry.Size(w, h * 0.14f), androidx.compose.ui.geometry.CornerRadius(14f))
                bar(h * 0.22f, width = w * 0.45f, color = accent)
                listTop = h * 0.36f
            }
            com.Azelmods.App.ui.components.HomeStyle.ARC -> {
                bar(h * 0.05f, width = w * 0.4f, color = accent)
                val arc = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.18f)
                    quadraticTo(w / 2f, h * 0.32f, w, h * 0.18f)
                    lineTo(w, h * 0.32f)
                    lineTo(0f, h * 0.32f)
                    close()
                }
                drawPath(arc, line.copy(alpha = 0.35f))
                drawCircle(accent, w * 0.1f, androidx.compose.ui.geometry.Offset(w / 2f, h * 0.2f))
                listTop = h * 0.36f
            }
            com.Azelmods.App.ui.components.HomeStyle.CARD -> {
                drawRoundRect(line.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(w * 0.06f, h * 0.05f), androidx.compose.ui.geometry.Size(w * 0.88f, h * 0.2f), androidx.compose.ui.geometry.CornerRadius(8f))
                drawCircle(accent, w * 0.08f, androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.15f))
                bar(h * 0.13f, x = w * 0.34f, width = w * 0.45f)
                listTop = h * 0.32f
            }
            com.Azelmods.App.ui.components.HomeStyle.COVER -> {
                drawRoundRect(accent.copy(alpha = 0.55f), androidx.compose.ui.geometry.Offset(w * 0.06f, h * 0.05f), androidx.compose.ui.geometry.Size(w * 0.88f, h * 0.28f), androidx.compose.ui.geometry.CornerRadius(8f))
                drawCircle(Color.White, w * 0.08f, androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.25f))
                bar(h * 0.235f, x = w * 0.34f, width = w * 0.45f, color = Color.White)
                listTop = h * 0.4f
            }
            com.Azelmods.App.ui.components.HomeStyle.STRIP -> {
                bar(h * 0.05f, width = w * 0.4f, color = accent)
                repeat(4) { drawCircle(accent, w * 0.055f, androidx.compose.ui.geometry.Offset(w * (0.17f + it * 0.22f), h * 0.2f)) }
                listTop = h * 0.3f
            }
        }
        var y = listTop
        while (y < h * 0.92f) {
            drawCircle(line, w * 0.06f, androidx.compose.ui.geometry.Offset(w * 0.17f, y + h * 0.02f))
            bar(y, x = w * 0.3f, width = w * 0.58f)
            y += h * 0.125f
        }
    }
}

@Composable
private fun SettingsMiniature(style: com.Azelmods.App.ui.components.SettingsStyle) {
    val accent = MaterialTheme.colorScheme.primary
    val line = NexusTokens.Color.TextMuted.copy(alpha = 0.55f)
    val card = NexusTokens.Color.TextMuted.copy(alpha = 0.2f)
    val back = NexusTokens.Color.BgBase
    androidx.compose.foundation.Canvas(
        Modifier
            .size(MiniatureSize, MiniatureSize + 22.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(NexusTokens.Radius.md))
            .background(back)
    ) {
        val w = size.width
        val h = size.height
        fun rect(x: Float, y: Float, width: Float, height: Float, color: Color = card) {
            drawRoundRect(
                color,
                androidx.compose.ui.geometry.Offset(x, y),
                androidx.compose.ui.geometry.Size(width, height),
                androidx.compose.ui.geometry.CornerRadius(6f)
            )
        }
        val left = w * 0.08f
        val full = w * 0.84f
        rect(left, h * 0.05f, full, h * 0.15f)
        drawCircle(accent, w * 0.065f, androidx.compose.ui.geometry.Offset(w * 0.2f, h * 0.125f))
        when (style) {
            com.Azelmods.App.ui.components.SettingsStyle.PANEL -> {
                rect(left, h * 0.23f, full * 0.48f, h * 0.15f)
                rect(left + full * 0.52f, h * 0.23f, full * 0.48f, h * 0.15f)
                rect(left, h * 0.42f, full, h * 0.5f)
            }
            com.Azelmods.App.ui.components.SettingsStyle.GROUPS -> {
                rect(left, h * 0.25f, full, h * 0.3f)
                rect(left, h * 0.6f, full, h * 0.32f)
            }
            com.Azelmods.App.ui.components.SettingsStyle.LIST -> {
                var y = h * 0.28f
                while (y < h * 0.92f) {
                    drawCircle(line, w * 0.05f, androidx.compose.ui.geometry.Offset(w * 0.16f, y + h * 0.015f))
                    rect(w * 0.28f, y, w * 0.6f, h * 0.035f, line)
                    y += h * 0.11f
                }
            }
            com.Azelmods.App.ui.components.SettingsStyle.HEADER -> {
                rect(left, h * 0.25f, w * 0.14f, h * 0.1f, accent)
                rect(left + w * 0.2f, h * 0.25f, full - w * 0.2f, h * 0.1f, line)
                rect(left, h * 0.42f, full, h * 0.5f)
            }

            com.Azelmods.App.ui.components.SettingsStyle.CARDS -> {
                drawCircle(accent, w * 0.065f, androidx.compose.ui.geometry.Offset(w * 0.8f, h * 0.125f))
                var y = h * 0.25f
                while (y < h * 0.8f) {
                    rect(left, y, full * 0.48f, h * 0.2f)
                    rect(left + full * 0.52f, y, full * 0.48f, h * 0.2f)
                    y += h * 0.235f
                }
            }

            com.Azelmods.App.ui.components.SettingsStyle.GRID -> {
                var y = h * 0.25f
                while (y < h * 0.8f) {
                    rect(left, y, full * 0.48f, h * 0.2f)
                    rect(left + full * 0.52f, y, full * 0.48f, h * 0.2f)
                    y += h * 0.235f
                }
            }
        }
    }
}
