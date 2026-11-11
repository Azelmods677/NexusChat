package com.Azelmods.App.ui.screens.settings

import com.Azelmods.App.i18n.tr

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Help
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.preferences.TutorialPreferences
import com.Azelmods.App.data.tutorials.AppFeature
import com.Azelmods.App.i18n.AppLanguage
import com.Azelmods.App.i18n.I18n
import com.Azelmods.App.ui.components.AutoTutorial
import com.Azelmods.App.ui.components.NexusDivider
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.NexusTopBar
import com.Azelmods.App.ui.components.pressScale
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens

private val TileBlue = Color(0xFF60A5FA)
private val TileGreen = Color(0xFF34D399)
private val TileViolet = Color(0xFFA78BFA)
private val TileOrange = Color(0xFFFB923C)
private val TilePink = Color(0xFFF472B6)
private val TileCyan = Color(0xFF22D3EE)
private val TileAmber = Color(0xFFFBBF24)
private val TileSlate = Color(0xFF94A3B8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val tutorialPreferences = remember { TutorialPreferences(context) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    val me = com.Azelmods.App.data.profile.rememberMyProfile()
    val userId = me.uid
    val style by com.Azelmods.App.ui.components.ChatLook.settingsStyle.collectAsState()
    var query by rememberSaveable { mutableStateOf("") }

    AutoTutorial(feature = AppFeature.SETTINGS, tutorialPreferences = tutorialPreferences)

    Scaffold(
        topBar = {
            NexusTopBar(
                // El diseño con encabezado ya lleva su propio título grande.
                title = if (style == com.Azelmods.App.ui.components.SettingsStyle.HEADER) "" else tr("Ajustes"),
                onBack = { navController.popBackStack() },
                actions = {
                    // Engranaje que gira despacio: acceso directo al estudio de diseño.
                    val spin = androidx.compose.animation.core.rememberInfiniteTransition(label = "engranaje")
                    val angle by spin.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            androidx.compose.animation.core.tween(6_000, easing = androidx.compose.animation.core.LinearEasing)
                        ),
                        label = "giroEngranaje"
                    )
                    androidx.compose.material3.IconButton(onClick = { navController.navigate("chat_studio") }) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = tr("Abrir el estudio de diseño"),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.graphicsLayer { rotationZ = angle }
                        )
                    }
                }
            )
        },
        containerColor = NexusTokens.Color.BgBase
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            val links = rememberSettingsLinks(navController, context) { showLanguageDialog = true }
            SettingsSearch(query, onChange = { query = it })
            val term = query.trim()
            when {
                term.isNotEmpty() -> {
                    val found = links.filter { link ->
                        tr(link.title).contains(term, ignoreCase = true) ||
                            link.subtitle?.let { tr(it).contains(term, ignoreCase = true) } == true
                    }
                    if (found.isEmpty()) {
                        Text(
                            tr("Ningún ajuste coincide con tu búsqueda"),
                            style = MaterialTheme.typography.bodyMedium,
                            color = NexusTokens.Color.TextMuted,
                            modifier = Modifier.padding(24.dp)
                        )
                    } else {
                        SettingsGroup { found.forEach { SettingsEntry(it) } }
                    }
                }

                else -> {
                    when (style) {
                        com.Azelmods.App.ui.components.SettingsStyle.PANEL -> SettingsDashboard(
                            userName = me.name,
                            userHandle = me.handle,
                            userPhoto = me.photoUrl,
                            userColor = me.color,
                            userId = userId,
                            onOpenProfile = { navController.navigate(Screen.Profile.createRoute(userId)) },
                            onOpenAbout = { navController.navigate(Screen.SettingsAbout.route) },
                            onOpenSecurity = { navController.navigate(Screen.Security.route) }
                        )

                        com.Azelmods.App.ui.components.SettingsStyle.HEADER -> SettingsHeader(
                            me = me,
                            onProfile = { navController.navigate(Screen.Profile.createRoute(userId)) },
                            onStudio = { navController.navigate("chat_studio") },
                            onSecurity = { navController.navigate(Screen.Security.route) },
                            onAbout = { navController.navigate(Screen.SettingsAbout.route) }
                        )

                        com.Azelmods.App.ui.components.SettingsStyle.CARDS -> SettingsProfileRight(me) {
                            navController.navigate(Screen.Profile.createRoute(userId))
                        }

                        else -> SettingsProfileCard(me) { navController.navigate(Screen.Profile.createRoute(userId)) }
                    }
                    val sections = links.groupBy { it.section }
                    if (style == com.Azelmods.App.ui.components.SettingsStyle.COMPACT) {
                        SettingsCompactLayout(sections)
                    } else sections.forEach { (section, items) ->
                        when (style) {
                            com.Azelmods.App.ui.components.SettingsStyle.LIST -> {
                                NexusSectionHeader(tr(section))
                                items.forEach { SettingsEntry(it) }
                            }

                            com.Azelmods.App.ui.components.SettingsStyle.GRID,
                            com.Azelmods.App.ui.components.SettingsStyle.CARDS -> {
                                NexusSectionHeader(tr(section))
                                SettingsGrid(items)
                            }

                            com.Azelmods.App.ui.components.SettingsStyle.GROUPS -> {
                                NexusSectionHeader(tr(section))
                                SettingsCardStack(items)
                            }

                            com.Azelmods.App.ui.components.SettingsStyle.PANEL -> {
                                Spacer(Modifier.height(10.dp))
                                SettingsGroup { items.forEach { SettingsCompactRow(it) } }
                            }

                            else -> SettingsSection(title = tr(section)) { items.forEach { SettingsEntry(it) } }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            SettingsGroup {
                SettingsEntry(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    color = NexusTokens.Color.Error,
                    title = tr("Cerrar sesión"),
                    subtitle = null,
                    titleColor = NexusTokens.Color.Error,
                    onClick = { showLogoutDialog = true }
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            current = I18n.selected(context),
            onSelect = { language ->
                showLanguageDialog = false
                (context as? Activity)?.let { I18n.select(it, language) }
            },
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(tr("¿Cerrar sesión?")) },
            text = {
                Text(
                    tr("Dejarás de recibir mensajes en este dispositivo hasta que vuelvas a entrar."),
                    color = NexusTokens.Color.TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    // Antes este botón solo navegaba al login: la sesión seguía abierta y
                    // al reabrir la app se entraba como si nada.
                    viewModel.logout {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }) {
                    Text(tr("Cerrar sesión"), color = NexusTokens.Color.Error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(tr("Cancelar")) }
            },
            containerColor = NexusTokens.Color.BgSurface
        )
    }
}

@Composable
private fun LanguageDialog(current: AppLanguage, onSelect: (AppLanguage) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Idioma de la app")) },
        text = {
            Column {
                AppLanguage.entries.forEach { language ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(NexusTokens.Radius.sm))
                            .clickable { onSelect(language) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == current,
                            onClick = { onSelect(language) },
                            colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(language.label, color = NexusTokens.Color.TextPrimary)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(tr("Cerrar")) } },
        containerColor = NexusTokens.Color.BgSurface
    )
}

@Composable
fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        NexusSectionHeader(title)
        SettingsGroup(content)
    }
}

/** Secciones agrupadas en una tarjeta. */
@Composable
private fun SettingsGroup(content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.BgSurface),
        content = content
    )
}

private class SettingsLink(
    val section: String,
    val icon: ImageVector,
    val color: Color,
    val title: String,
    val subtitle: String?,
    val onClick: () -> Unit
)

/** Todos los accesos en una lista: cada diseño y la búsqueda parten de la misma. */
@Composable
private fun rememberSettingsLinks(
    navController: NavController,
    context: android.content.Context,
    onLanguage: () -> Unit
): List<SettingsLink> = remember(navController) {
    fun go(route: String): () -> Unit = { navController.navigate(route) }
    val look = "Personalización"
    val account = "Cuenta y privacidad"
    val prefs = "Preferencias"
    val tools = "Herramientas"
    val info = "Información"
    listOf(
        SettingsLink(look, Icons.Outlined.Style, TilePink, "Estudio de diseño", "Filas, burbujas, campo de escribir, inicio y ajustes", go("chat_studio")),
        SettingsLink(look, Icons.Outlined.Palette, TileViolet, "Color y fondo", "Color de acento, fondo, letra y animación de entrada", go(Screen.SettingsAppearance.route)),
        SettingsLink(look, Icons.Outlined.Brush, TileAmber, "Mi perfil", "Color, foto y portada", go(Screen.ProfileCustomize.route)),
        SettingsLink(account, Icons.Outlined.WorkspacePremium, TileAmber, "Premium", "Insignia, colores exclusivos y galería sin límite", go(Screen.Premium.route)),
        SettingsLink(account, Icons.Outlined.Person, TileBlue, "Cuenta", "Nombre, usuario, teléfono y correo", go(Screen.SettingsAccount.route)),
        SettingsLink(account, Icons.Outlined.Lock, TileGreen, "Privacidad", "Última conexión, confirmaciones, bloqueados", go(Screen.SettingsPrivacy.route)),
        SettingsLink(account, Icons.Outlined.Security, TileViolet, "Seguridad y Tor", "Modo anónimo, fantasma, bloqueo y copias", go(Screen.Security.route)),
        SettingsLink(prefs, Icons.Outlined.Notifications, TileOrange, "Notificaciones", "Sonido, vibración y vista previa", go(Screen.SettingsNotifications.route)),
        SettingsLink(prefs, Icons.Outlined.Language, TileCyan, "Idioma de la app", I18n.selected(context).label, onLanguage),
        SettingsLink(prefs, Icons.Outlined.Translate, TileCyan, "Idioma de traducción", "A qué idioma se traducen los mensajes", go("translation_language")),
        SettingsLink(prefs, Icons.Outlined.Storage, TileAmber, "Almacenamiento y datos", "Descarga automática y espacio usado", go(Screen.SettingsStorage.route)),
        SettingsLink(tools, Icons.Outlined.SmartToy, TileViolet, "Inteligencia artificial", "Proveedor, modelo y funciones en los chats", go(Screen.AiFeatures.route)),
        SettingsLink(tools, Icons.Outlined.Code, TileGreen, "Editor de código", "Pestañas, búsqueda y ejecución", go("code_editor")),
        SettingsLink(tools, Icons.Outlined.Terminal, TileSlate, "Terminal", "Shell del teléfono y Kali Linux", go("terminal")),
        SettingsLink(info, Icons.AutoMirrored.Outlined.Help, TileBlue, "Ayuda", "Guías de cada función", go(Screen.SettingsHelp.route)),
        SettingsLink(info, Icons.Outlined.Lock, TileGreen, "Política de privacidad", "Qué datos usa la app y para qué", go("privacy_policy")),
        SettingsLink(info, Icons.Outlined.Info, TileSlate, "Acerca de", "Versión, licencias, historia y tu dispositivo", go(Screen.SettingsAbout.route))
    )
}

@Composable
private fun SettingsSearch(query: String, onChange: (String) -> Unit) {
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusTokens.Radius.pill))
            .background(NexusTokens.Color.BgSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, null, tint = NexusTokens.Color.TextMuted, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = onChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = NexusTokens.Color.TextPrimary),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
            modifier = Modifier.weight(1f),
            decorationBox = { field ->
                if (query.isEmpty()) {
                    Text(tr("Buscar en ajustes"), style = MaterialTheme.typography.bodyMedium, color = NexusTokens.Color.TextMuted)
                }
                field()
            }
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Outlined.Close,
                tr("Borrar"),
                tint = NexusTokens.Color.TextMuted,
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onChange("") }
            )
        }
    }
}

@Composable
private fun SettingsProfileCard(
    me: com.Azelmods.App.data.profile.MyProfileData,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
            .background(NexusTokens.Color.BgSurface)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.Azelmods.App.ui.components.UserAvatar(
            name = me.name,
            photoUrl = me.photoUrl,
            size = 64.dp,
            ring = com.Azelmods.App.ui.components.AvatarRing(
                com.Azelmods.App.ui.components.ProfileColors.forUser(me.uid.ifBlank { me.name }, me.color),
                width = 2.5.dp
            )
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                me.name.ifBlank { tr("Tu perfil") },
                style = MaterialTheme.typography.titleLarge,
                color = NexusTokens.Color.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (me.handle.isNotBlank()) {
                Text(
                    "@${me.handle.removePrefix("@")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = NexusTokens.Color.TextMuted)
    }
}

/** Título grande con un punto de color, tu foto y una cápsula con los accesos que más se usan. */
@Composable
private fun SettingsHeader(
    me: com.Azelmods.App.data.profile.MyProfileData,
    onProfile: () -> Unit,
    onStudio: () -> Unit,
    onSecurity: () -> Unit,
    onAbout: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(Modifier.padding(start = 28.dp, end = 28.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .padding(end = 10.dp)
                .size(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accent)
        )
        Text(
            tr("Ajustes"),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = NexusTokens.Color.TextPrimary
        )
    }
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        com.Azelmods.App.ui.components.UserAvatar(
            name = me.name,
            photoUrl = me.photoUrl,
            size = 56.dp,
            modifier = Modifier.clickable(onClick = onProfile)
        )
        Spacer(Modifier.width(10.dp))
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(100.dp))
                .background(NexusTokens.Color.BgSurface)
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf(
                Triple(Icons.Outlined.Person, tr("Perfil"), onProfile),
                Triple(Icons.Outlined.Style, tr("Estudio de diseño"), onStudio),
                Triple(Icons.Outlined.Security, tr("Seguridad y Tor"), onSecurity),
                Triple(Icons.Outlined.Info, tr("Acerca de"), onAbout)
            ).forEach { (icon, label, action) ->
                com.Azelmods.App.ui.components.AnimatedIconButton(onClick = action) {
                    Icon(icon, label, tint = accent)
                }
            }
        }
    }
}

/** Título grande y una tarjeta de 75 dp con el nombre a la izquierda y la foto a la derecha. */
@Composable
private fun SettingsProfileRight(
    me: com.Azelmods.App.data.profile.MyProfileData,
    onClick: () -> Unit
) {
    Text(
        tr("Ajustes"),
        style = MaterialTheme.typography.headlineMedium,
        color = NexusTokens.Color.TextSecondary,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
    )
    Row(
        Modifier
            .padding(horizontal = 12.dp)
            .fillMaxWidth()
            .height(75.dp)
            .clip(RoundedCornerShape(23.dp))
            .background(NexusTokens.Color.BgSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                me.name.ifBlank { tr("Tu perfil") },
                style = MaterialTheme.typography.titleLarge,
                color = NexusTokens.Color.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (me.handle.isNotBlank()) {
                Text(
                    "@${me.handle.removePrefix("@")}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        com.Azelmods.App.ui.components.UserAvatar(name = me.name, photoUrl = me.photoUrl, size = 56.dp)
    }
}

/** Una tarjeta por entrada: redondeadas por fuera del grupo y casi rectas por dentro. */
@Composable
private fun SettingsCardStack(items: List<SettingsLink>) {
    Column(
        Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        items.forEachIndexed { index, link ->
            val outer = 23.dp
            val inner = 2.dp
            val shape = RoundedCornerShape(
                topStart = if (index == 0) outer else inner,
                topEnd = if (index == 0) outer else inner,
                bottomStart = if (index == items.lastIndex) outer else inner,
                bottomEnd = if (index == items.lastIndex) outer else inner
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(NexusTokens.Color.BgSurface)
            ) {
                SettingsEntry(link)
            }
        }
    }
}

@Composable
private fun SettingsGrid(items: List<SettingsLink>) {
    Column(
        Modifier.padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.chunked(2).forEach { row ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { link ->
                    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pressScale(interaction)
                            .clip(RoundedCornerShape(NexusTokens.Radius.xl))
                            .background(NexusTokens.Color.BgSurface)
                            .clickable(interactionSource = interaction, indication = null, onClick = link.onClick)
                            .padding(14.dp)
                    ) {
                        SettingsIconTile(link.icon, link.color)
                        Spacer(Modifier.height(10.dp))
                        Text(tr(link.title), style = MaterialTheme.typography.titleSmall, color = NexusTokens.Color.TextPrimary)
                        link.subtitle?.let {
                            Spacer(Modifier.height(2.dp))
                            Text(
                                tr(it),
                                style = MaterialTheme.typography.bodySmall,
                                color = NexusTokens.Color.TextMuted,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                // Una fila impar no debe estirar su única tarjeta a todo el ancho.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Lo principal en dos columnas pequeñas dentro de una tarjeta; herramientas e información, en filas. */
@Composable
private fun SettingsCompactLayout(sections: Map<String, List<SettingsLink>>) {
    val entries = sections.entries.toList()
    val main = entries.dropLast(2).flatMap { it.value }
    val rest = entries.takeLast(2).flatMap { it.value }
    NexusSectionHeader(tr("Configuración"))
    SettingsGroup {
        main.chunked(2).forEach { row ->
            Row(Modifier.height(IntrinsicSize.Min)) {
                row.forEach { link -> SettingsCompactCell(link, Modifier.weight(1f).fillMaxHeight()) }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    Spacer(Modifier.height(10.dp))
    SettingsGroup { rest.forEach { SettingsCompactRow(it) } }
}

@Composable
private fun SettingsCompactCell(link: SettingsLink, modifier: Modifier) {
    Row(
        modifier
            .clickable(onClick = link.onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(link.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            tr(link.title),
            style = MaterialTheme.typography.bodyMedium,
            color = NexusTokens.Color.TextPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Fila de una sola línea, sin descripción. */
@Composable
private fun SettingsCompactRow(link: SettingsLink) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = link.onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(link.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Text(
            tr(link.title),
            style = MaterialTheme.typography.bodyLarge,
            color = NexusTokens.Color.TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = NexusTokens.Color.TextDisabled,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SettingsEntry(link: SettingsLink) {
    SettingsEntry(link.icon, link.color, tr(link.title), link.subtitle?.let { tr(it) }, onClick = link.onClick)
}

/** Entrada de ajustes con descripción: dice qué hay dentro sin tener que abrirla. */
@Composable
private fun SettingsEntry(
    icon: ImageVector,
    color: Color,
    title: String,
    subtitle: String?,
    titleColor: Color = NexusTokens.Color.TextPrimary,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsIconTile(icon, color)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = titleColor)
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = NexusTokens.Color.TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            contentDescription = null,
            tint = NexusTokens.Color.TextDisabled,
            modifier = Modifier.size(20.dp)
        )
    }
}
