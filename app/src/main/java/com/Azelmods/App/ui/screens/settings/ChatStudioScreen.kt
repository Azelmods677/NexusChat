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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Translate
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
import com.Azelmods.App.ui.components.coloredIcons
import com.Azelmods.App.ui.components.cameraOutside
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
            // Vista previa en vivo arriba, la que toque a cada pestaña: lo que cambias se ve al momento.
            androidx.compose.animation.Crossfade(targetState = tab, label = "vistaPrevia") { current ->
                when (current) {
                    StudioTab.HOME -> HomeLivePreview()
                    StudioTab.SETTINGS -> SettingsLivePreview()
                    else -> LivePreview()
                }
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
                val colored = entryStyle.coloredIcons()
                val emojiRight = entryStyle == ChatEntryStyle.FLAT || entryStyle.cameraOutside()
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

                    ChatEntryStyle.RING, ChatEntryStyle.SQUARE, ChatEntryStyle.CAPSULE, ChatEntryStyle.FRAMED -> {
                        if (entryStyle == ChatEntryStyle.RING || entryStyle == ChatEntryStyle.SQUARE) {
                            val me = com.Azelmods.App.data.profile.rememberMyProfile()
                            if (entryStyle == ChatEntryStyle.RING) {
                                com.Azelmods.App.ui.components.AnimatedRingAvatar(me.name, me.photoUrl, size = 38.dp, modifier = Modifier.padding(end = 4.dp))
                            } else {
                                Box(
                                    Modifier
                                        .padding(end = 4.dp)
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    com.Azelmods.App.ui.components.UserAvatar(me.name, me.photoUrl, size = 36.dp)
                                }
                            }
                        }
                        Icon(Icons.Outlined.AttachFile, null, tint = tone, modifier = Modifier.size(22.dp))
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
                    if (!emojiRight) {
                        Icon(Icons.Outlined.EmojiEmotions, null, tint = tone, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        hint.ifBlank { tr("Mensaje") },
                        style = MaterialTheme.typography.bodyMedium,
                        color = NexusTokens.Color.TextMuted,
                        modifier = Modifier.weight(1f)
                    )
                    if (emojiRight) {
                        Icon(Icons.Outlined.EmojiEmotions, null, tint = tone, modifier = Modifier.size(22.dp))
                    }
                }
                Spacer(Modifier.width(6.dp))
                if (entryStyle.cameraOutside()) {
                    Icon(Icons.Outlined.PhotoCamera, null, tint = tone, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                }
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
        AppTheme.ACCENT_SWATCHES.forEach { swatch ->
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
        icon = Icons.Outlined.AccountCircle,
        checked = bubblePhoto,
        onCheckedChange = ChatLook::setBubblePhoto
    )
    val style by ChatLook.bubbleStyle.collectAsState()
    val ticks by ChatLook.tickStyle.collectAsState()
    val own by ChatLook.ownBubbleColor.collectAsState()
    val peer by ChatLook.peerBubbleColor.collectAsState()
    val scale by ChatLook.textScale.collectAsState()

    NexusSectionHeader(tr("Forma de las burbujas"))
    VisualChoices(BubbleStyle.entries, style, { it.title }, ChatLook::setBubbleStyle) { BubbleMiniature(it, own) }
    NexusSectionHeader(tr("Marcas de enviado y leído"))
    VisualChoices(TickStyle.entries, ticks, { it.title }, ChatLook::setTickStyle) {
        ReceiptMark(it, MaterialTheme.colorScheme.primary, double = true, read = true, modifier = Modifier.size(width = 30.dp, height = 20.dp))
    }
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
    VisualChoices(ChatEntryStyle.entries, style, { it.title }, ChatLook::setEntryStyle) { EntryMiniature(it) }
    val hideEmoji by ChatLook.hideEntryEmoji.collectAsState()
    val entryTranslate by ChatLook.entryTranslate.collectAsState()
    NexusSwitchRow(
        title = tr("Quitar el emoji del campo"),
        subtitle = tr("Más sitio para escribir"),
        icon = Icons.Outlined.EmojiEmotions,
        checked = hideEmoji,
        onCheckedChange = ChatLook::setHideEntryEmoji
    )
    NexusSwitchRow(
        title = tr("Traducir desde el campo"),
        subtitle = tr("Traduce lo que escribes antes de enviarlo"),
        icon = Icons.Outlined.Translate,
        checked = entryTranslate,
        onCheckedChange = ChatLook::setEntryTranslate
    )
    val quickFunctions by ChatLook.quickFunctions.collectAsState()
    NexusSwitchRow(
        title = tr("Botón de funciones rápidas"),
        subtitle = tr("Atajos, seleccionar todo, primer mensaje y contador en el chat"),
        icon = Icons.Outlined.Apps,
        checked = quickFunctions,
        onCheckedChange = ChatLook::setQuickFunctions
    )
    val linkPreviews by ChatLook.linkPreviews.collectAsState()
    NexusSwitchRow(
        title = tr("Vista previa de enlaces"),
        subtitle = tr("La prepara tu teléfono y va cifrada. En modo anónimo no se usa."),
        icon = Icons.Outlined.Link,
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
    VisualChoices(NavBarStyle.entries, nav, { it.title }, ChatLook::setNavBarStyle) { NavMiniature(it) }
    NexusSectionHeader(tr("Iconos de la barra"))
    val navIcons by ChatLook.navIconSet.collectAsState()
    VisualChoices(com.Azelmods.App.ui.components.NavIconSet.entries, navIcons, { it.title }, ChatLook::setNavIconSet) { set ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            com.Azelmods.App.ui.screens.main.tabsFor(set).forEachIndexed { index, tab ->
                Icon(
                    if (index == 0) tab.selectedIcon else tab.unselectedIcon,
                    contentDescription = null,
                    tint = if (index == 0) MaterialTheme.colorScheme.primary else NexusTokens.Color.TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    val callMotion by ChatLook.animatedCallIcons.collectAsState()
    NexusSwitchRow(
        title = tr("Iconos de llamada animados"),
        subtitle = tr("Ondas en la llamada y un aro en la videollamada, arriba en cada chat"),
        icon = Icons.Outlined.AccountCircle,
        checked = callMotion,
        onCheckedChange = ChatLook::setAnimatedCallIcons
    )
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
    val textFilters by ChatLook.textFilters.collectAsState()
    NexusSwitchRow(
        title = tr("Filtros como pestañas de texto"),
        subtitle = tr("Texto con un punto de color en lugar de cápsulas"),
        icon = Icons.Outlined.FilterList,
        checked = textFilters,
        onCheckedChange = ChatLook::setTextFilters
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
private fun HomeMiniature(style: com.Azelmods.App.ui.components.HomeStyle, large: Boolean = false) {
    val accent = MaterialTheme.colorScheme.primary
    val line = NexusTokens.Color.TextMuted.copy(alpha = 0.55f)
    val back = NexusTokens.Color.BgBase
    androidx.compose.foundation.Canvas(
        Modifier
            .size(if (large) 150.dp else MiniatureSize, if (large) 112.dp else MiniatureSize + 22.dp)
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
private fun SettingsMiniature(style: com.Azelmods.App.ui.components.SettingsStyle, large: Boolean = false) {
    val accent = MaterialTheme.colorScheme.primary
    val line = NexusTokens.Color.TextMuted.copy(alpha = 0.55f)
    val card = NexusTokens.Color.TextMuted.copy(alpha = 0.2f)
    val back = NexusTokens.Color.BgBase
    androidx.compose.foundation.Canvas(
        Modifier
            .size(if (large) 120.dp else MiniatureSize, if (large) 150.dp else MiniatureSize + 22.dp)
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

            com.Azelmods.App.ui.components.SettingsStyle.COMPACT -> {
                rect(left, h * 0.25f, full, h * 0.42f)
                var y = h * 0.3f
                while (y < h * 0.62f) {
                    rect(w * 0.14f, y, w * 0.3f, h * 0.035f, line)
                    rect(w * 0.56f, y, w * 0.3f, h * 0.035f, line)
                    y += h * 0.1f
                }
                rect(left, h * 0.72f, full, h * 0.2f)
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

/** Opciones como tarjetas con su dibujo: se ve cómo queda antes de elegirla. */
@Composable
private fun <T> VisualChoices(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    preview: @Composable (T) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    androidx.compose.foundation.lazy.LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(options.size) { index ->
            val option = options[index]
            val isSelected = option == selected
            val shape = RoundedCornerShape(16.dp)
            Column(
                Modifier
                    .width(132.dp)
                    .clip(shape)
                    .background(if (isSelected) accent.copy(alpha = 0.14f) else NexusTokens.Color.BgSurface)
                    .border(if (isSelected) 2.dp else 1.dp, if (isSelected) accent else NexusTokens.Color.TextMuted.copy(alpha = 0.15f), shape)
                    .clickable { onSelect(option) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NexusTokens.Color.BgBase),
                    contentAlignment = Alignment.Center
                ) { preview(option) }
                Spacer(Modifier.height(6.dp))
                Text(
                    tr(label(option)),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isSelected) accent else NexusTokens.Color.TextPrimary,
                    maxLines = 1
                )
            }
        }
    }
}

/** La burbuja real, con su forma y su relleno. */
@Composable
private fun BubbleMiniature(style: BubbleStyle, ownColor: Int?) {
    val accent = MaterialTheme.colorScheme.primary
    val color = ownColor?.let { Color(it) } ?: accent.copy(alpha = 0.85f)
    Box(
        Modifier
            .then(chatBubbleSurface(style, chatBubbleShape(style, true), color, accent))
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(tr("¡Hola!"), style = MaterialTheme.typography.bodySmall, color = Color.White)
    }
}

/** El campo de escribir dibujado en pequeño: forma, botones y dónde quedan. */
@Composable
private fun EntryMiniature(style: ChatEntryStyle) {
    val accent = MaterialTheme.colorScheme.primary
    val field = NexusTokens.Color.BgElevated
    val muted = NexusTokens.Color.TextMuted.copy(alpha = 0.6f)
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 8.dp)) {
        val h = 22.dp.toPx()
        val y = (size.height - h) / 2
        val send = h / 2
        val sendX = size.width - send
        val cy = y + h / 2
        fun pill(x: Float, w: Float, r: Float = h / 2) = drawRoundRect(field, androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(r))
        fun hint(x: Float) = drawRoundRect(muted, androidx.compose.ui.geometry.Offset(x, cy - 2.dp.toPx()), androidx.compose.ui.geometry.Size(26.dp.toPx(), 4.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
        val end = sendX - send - 4.dp.toPx()
        when (style) {
            ChatEntryStyle.PILL -> { pill(0f, end); hint(12.dp.toPx()) }
            ChatEntryStyle.DEPTH -> {
                drawRoundRect(Color.Black.copy(alpha = 0.45f), androidx.compose.ui.geometry.Offset(0f, y + 3.dp.toPx()), androidx.compose.ui.geometry.Size(end, h), androidx.compose.ui.geometry.CornerRadius(h / 2))
                pill(0f, end); hint(12.dp.toPx())
            }
            ChatEntryStyle.OUTLINE -> {
                drawRoundRect(accent, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Size(end, h), androidx.compose.ui.geometry.CornerRadius(h / 2), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                hint(12.dp.toPx())
            }
            ChatEntryStyle.LINE -> {
                drawLine(accent, androidx.compose.ui.geometry.Offset(0f, y + h), androidx.compose.ui.geometry.Offset(end, y + h), 2.dp.toPx())
                hint(4.dp.toPx())
            }
            ChatEntryStyle.BLOCK -> { pill(0f, end, 4.dp.toPx()); hint(10.dp.toPx()) }
            ChatEntryStyle.PHOTO -> {
                drawCircle(accent, h / 2, androidx.compose.ui.geometry.Offset(h / 2, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4.dp.toPx()))
                drawCircle(muted, h / 2 - 3.dp.toPx(), androidx.compose.ui.geometry.Offset(h / 2, cy))
                pill(h + 4.dp.toPx(), end - h - 4.dp.toPx()); hint(h + 14.dp.toPx())
            }
            ChatEntryStyle.SOCIAL -> {
                drawCircle(accent, h / 2.6f, androidx.compose.ui.geometry.Offset(h / 2, cy))
                drawRoundRect(accent, androidx.compose.ui.geometry.Offset(h + 2.dp.toPx(), cy - 6.dp.toPx()), androidx.compose.ui.geometry.Size(14.dp.toPx(), 12.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(3.dp.toPx()))
                pill(h + 20.dp.toPx(), end - h - 20.dp.toPx()); hint(h + 30.dp.toPx())
            }
            ChatEntryStyle.RING, ChatEntryStyle.SQUARE -> {
                if (style == ChatEntryStyle.RING) {
                    drawCircle(accent, h / 2, androidx.compose.ui.geometry.Offset(h / 2, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                    drawCircle(muted, h / 2 - 4.dp.toPx(), androidx.compose.ui.geometry.Offset(h / 2, cy))
                } else {
                    drawRoundRect(muted, androidx.compose.ui.geometry.Offset(2.dp.toPx(), y - 2.dp.toPx()), androidx.compose.ui.geometry.Size(h - 4.dp.toPx(), h + 4.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                }
                drawLine(accent, androidx.compose.ui.geometry.Offset(h + 6.dp.toPx(), cy + 5.dp.toPx()), androidx.compose.ui.geometry.Offset(h + 12.dp.toPx(), cy - 5.dp.toPx()), 2.dp.toPx())
                hint(h + 18.dp.toPx())
                drawCircle(accent, 4.dp.toPx(), androidx.compose.ui.geometry.Offset(end - 18.dp.toPx(), cy))
                drawRoundRect(accent, androidx.compose.ui.geometry.Offset(end - 8.dp.toPx(), cy - 4.dp.toPx()), androidx.compose.ui.geometry.Size(9.dp.toPx(), 8.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
            ChatEntryStyle.CAPSULE, ChatEntryStyle.FRAMED -> {
                drawLine(accent, androidx.compose.ui.geometry.Offset(2.dp.toPx(), cy + 5.dp.toPx()), androidx.compose.ui.geometry.Offset(8.dp.toPx(), cy - 5.dp.toPx()), 2.dp.toPx())
                val x = 14.dp.toPx()
                val w = end - x - 12.dp.toPx()
                if (style == ChatEntryStyle.CAPSULE) {
                    pill(x, w)
                } else {
                    drawRoundRect(field, androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()))
                    drawRoundRect(accent, androidx.compose.ui.geometry.Offset(x, y), androidx.compose.ui.geometry.Size(w, h), androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                    drawLine(accent, androidx.compose.ui.geometry.Offset(x + 6.dp.toPx(), y + h - 5.dp.toPx()), androidx.compose.ui.geometry.Offset(x + w - 14.dp.toPx(), y + h - 5.dp.toPx()), 1.5.dp.toPx())
                }
                hint(x + 8.dp.toPx())
                drawCircle(accent, 4.dp.toPx(), androidx.compose.ui.geometry.Offset(x + w - 8.dp.toPx(), cy))
                drawRoundRect(accent, androidx.compose.ui.geometry.Offset(end - 8.dp.toPx(), cy - 4.dp.toPx()), androidx.compose.ui.geometry.Size(9.dp.toPx(), 8.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
            }
            ChatEntryStyle.FLAT -> {
                drawRoundRect(muted, androidx.compose.ui.geometry.Offset(0f, y), androidx.compose.ui.geometry.Size(h, h), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
                drawRect(field, androidx.compose.ui.geometry.Offset(h + 3.dp.toPx(), y), androidx.compose.ui.geometry.Size(end - 2 * h - 6.dp.toPx(), h))
                drawRoundRect(muted, androidx.compose.ui.geometry.Offset(end - h, y), androidx.compose.ui.geometry.Size(h, h), androidx.compose.ui.geometry.CornerRadius(5.dp.toPx()))
                hint(h + 10.dp.toPx())
            }
        }
        val transparentSend = style.coloredIcons() && style != ChatEntryStyle.FLAT
        if (transparentSend) {
            drawCircle(accent, send, androidx.compose.ui.geometry.Offset(sendX, cy), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
        } else {
            drawCircle(accent, send, androidx.compose.ui.geometry.Offset(sendX, cy))
        }
    }
}

/** La barra inferior dibujada en pequeño. */
@Composable
private fun NavMiniature(style: NavBarStyle) {
    val accent = MaterialTheme.colorScheme.primary
    val bar = NexusTokens.Color.BgElevated
    val muted = NexusTokens.Color.TextMuted.copy(alpha = 0.6f)
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 6.dp)) {
        val w = size.width
        val top = size.height - 24.dp.toPx()
        val mid = top + 12.dp.toPx()
        val r = 3.dp.toPx()
        fun dots(n: Int, from: Float, to: Float, selected: Int = 0, labels: Boolean = false) {
            val step = (to - from) / n
            repeat(n) { i ->
                val x = from + step * (i + 0.5f)
                drawCircle(if (i == selected) accent else muted, r, androidx.compose.ui.geometry.Offset(x, if (labels) mid - 3.dp.toPx() else mid))
                if (labels) drawLine(if (i == selected) accent else muted, androidx.compose.ui.geometry.Offset(x - 5.dp.toPx(), mid + 5.dp.toPx()), androidx.compose.ui.geometry.Offset(x + 5.dp.toPx(), mid + 5.dp.toPx()), 1.5.dp.toPx())
            }
        }
        fun fullBar(radius: Float = 0f) = drawRoundRect(bar, androidx.compose.ui.geometry.Offset(0f, top), androidx.compose.ui.geometry.Size(w, 24.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(radius))
        when (style) {
            NavBarStyle.BAR -> { fullBar(); dots(4, 0f, w, labels = true) }
            NavBarStyle.ICONS -> { fullBar(); dots(4, 0f, w) }
            NavBarStyle.FLOATING -> {
                drawRoundRect(bar, androidx.compose.ui.geometry.Offset(8.dp.toPx(), top + 2.dp.toPx()), androidx.compose.ui.geometry.Size(w - 16.dp.toPx(), 20.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(10.dp.toPx()))
                drawRoundRect(accent.copy(alpha = 0.3f), androidx.compose.ui.geometry.Offset(12.dp.toPx(), top + 5.dp.toPx()), androidx.compose.ui.geometry.Size(26.dp.toPx(), 14.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(7.dp.toPx()))
                dots(4, 8.dp.toPx(), w - 8.dp.toPx())
            }
            NavBarStyle.CENTER -> {
                drawRoundRect(bar, androidx.compose.ui.geometry.Offset(4.dp.toPx(), mid - 6.dp.toPx()), androidx.compose.ui.geometry.Size(30.dp.toPx(), 12.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
                drawRoundRect(bar, androidx.compose.ui.geometry.Offset(w - 34.dp.toPx(), mid - 6.dp.toPx()), androidx.compose.ui.geometry.Size(30.dp.toPx(), 12.dp.toPx()), androidx.compose.ui.geometry.CornerRadius(2.dp.toPx()))
                repeat(6) { i ->
                    drawArc(accent, i * 60f, 48f, false, androidx.compose.ui.geometry.Offset(w / 2 - 11.dp.toPx(), mid - 11.dp.toPx()), androidx.compose.ui.geometry.Size(22.dp.toPx(), 22.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                }
                drawCircle(accent, r, androidx.compose.ui.geometry.Offset(w / 2, mid))
            }
            NavBarStyle.PHOTO -> {
                fullBar(); dots(2, 0f, w / 2 - 12.dp.toPx(), selected = 0); dots(2, w / 2 + 12.dp.toPx(), w, selected = -1)
                drawCircle(accent, 9.dp.toPx(), androidx.compose.ui.geometry.Offset(w / 2, mid), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx()))
                drawCircle(muted, 6.dp.toPx(), androidx.compose.ui.geometry.Offset(w / 2, mid))
            }
            NavBarStyle.ACCESS -> {
                fullBar(); dots(4, 0f, w - 22.dp.toPx())
                drawCircle(NexusTokens.Color.BgSurface, 9.dp.toPx(), androidx.compose.ui.geometry.Offset(w - 12.dp.toPx(), mid))
                drawCircle(accent, r, androidx.compose.ui.geometry.Offset(w - 12.dp.toPx(), mid))
            }
            NavBarStyle.LINE, NavBarStyle.PANEL -> {
                if (style == NavBarStyle.PANEL) fullBar(10.dp.toPx())
                val step = w / 4
                repeat(4) { i ->
                    val x = step * (i + 0.5f)
                    drawLine(if (i == 0) accent else muted, androidx.compose.ui.geometry.Offset(x - 8.dp.toPx(), mid - 2.dp.toPx()), androidx.compose.ui.geometry.Offset(x + 8.dp.toPx(), mid - 2.dp.toPx()), 2.dp.toPx())
                }
                drawLine(accent, androidx.compose.ui.geometry.Offset(4.dp.toPx(), top + 22.dp.toPx()), androidx.compose.ui.geometry.Offset(step - 4.dp.toPx(), top + 22.dp.toPx()), 3.dp.toPx())
            }
        }
    }
}

/** Inicio en vivo: la cabecera elegida y la barra inferior real con sus iconos. */
@Composable
private fun HomeLivePreview() {
    val home by ChatLook.homeStyle.collectAsState()
    val nav by ChatLook.navBarStyle.collectAsState()
    val icons by ChatLook.navIconSet.collectAsState()
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier
            .padding(12.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(NexusTokens.Color.BgBase)
            .border(1.dp, NexusTokens.Color.TextMuted.copy(alpha = 0.2f), shape)
            // Dentro de la vista previa la barra no debe sumar el hueco de la navegación del sistema.
            .consumeWindowInsets(WindowInsets.navigationBars)
    ) {
        Box(Modifier.fillMaxWidth().padding(top = 10.dp), contentAlignment = Alignment.Center) {
            HomeMiniature(home, large = true)
        }
        Spacer(Modifier.height(8.dp))
        com.Azelmods.App.ui.screens.main.AppBottomBar(
            navStyle = nav,
            tabs = com.Azelmods.App.ui.screens.main.tabsFor(icons),
            currentPage = 0,
            missedCallCount = 0,
            onSelect = {},
            onSettings = {}
        )
    }
}

/** Ajustes en vivo: el diseño elegido en grande. */
@Composable
private fun SettingsLivePreview() {
    val style by ChatLook.settingsStyle.collectAsState()
    Box(
        Modifier
            .padding(12.dp)
            .fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        SettingsMiniature(style, large = true)
    }
}
