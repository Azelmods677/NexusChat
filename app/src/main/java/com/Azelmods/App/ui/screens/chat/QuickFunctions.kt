package com.Azelmods.App.ui.screens.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.SelectAll
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material.icons.outlined.VerticalAlignTop
import androidx.compose.material.icons.outlined.Wallpaper
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Azelmods.App.data.model.Message
import com.Azelmods.App.data.model.User
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.components.AnimatedIconButton
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens

/** Cuenta los mensajes por tipo. Solo los cargados en este chat. */
fun countMessages(messages: List<Message>): List<Pair<String, Int>> {
    val visible = messages.filterNot { it.mediaType == "SYSTEM" }
    val byType = visible.groupingBy { it.mediaType.orEmpty() }.eachCount()
    return listOf(
        "Mensajes" to (byType[""] ?: 0),
        "Fotos" to (byType["IMAGE"] ?: 0),
        "Vídeos" to (byType["VIDEO"] ?: 0),
        "Audios" to (byType["AUDIO"] ?: 0),
        "Documentos" to (byType["DOCUMENT"] ?: 0),
        "Ubicaciones" to (byType["LOCATION"] ?: 0),
        "Stickers" to (byType["STICKER"] ?: 0),
        "Total" to visible.size
    )
}

/** Botón flotante del chat: abre el panel de funciones rápidas. */
@Composable
fun QuickFunctionsButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .padding(end = 8.dp, top = 8.dp)
            .size(36.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        AnimatedIconButton(onClick = onClick, modifier = Modifier.size(36.dp)) {
            Icon(
                Icons.Outlined.AutoAwesome,
                tr("Funciones rápidas"),
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickFunctionsSheet(
    chatId: String,
    contact: User?,
    messages: List<Message>,
    navController: NavController,
    onSelectAll: () -> Unit,
    onFirstMessage: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showCounter by remember { mutableStateOf(false) }

    fun openWeb(url: String) {
        runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }

    fun go(route: String) {
        onDismiss()
        navController.navigate(route)
    }

    if (showCounter) {
        AlertDialog(
            onDismissRequest = { showCounter = false },
            title = { Text(tr("Contador de mensajes")) },
            text = {
                Column {
                    countMessages(messages).forEach { (label, count) ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(tr(label), color = NexusTokens.Color.TextSecondary, modifier = Modifier.weight(1f))
                            Text(count.toString(), color = NexusTokens.Color.TextPrimary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr("Cuenta los mensajes cargados. Sube hasta arriba del chat para incluir los antiguos."),
                        style = MaterialTheme.typography.bodySmall,
                        color = NexusTokens.Color.TextMuted
                    )
                }
            },
            confirmButton = { TextButton(onClick = { showCounter = false }) { Text(tr("Cerrar")) } },
            containerColor = NexusTokens.Color.BgSurface
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = NexusTokens.Color.BgSurface,
        tonalElevation = 0.dp
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShortcutTile(Icons.Outlined.SmartDisplay, "YouTube") { openWeb("https://www.youtube.com") }
                ShortcutTile(Icons.Outlined.PhotoCamera, "Instagram") { openWeb("https://www.instagram.com") }
                ShortcutTile(Icons.Outlined.MusicNote, "TikTok") { openWeb("https://www.tiktok.com") }
                ShortcutTile(Icons.Outlined.Public, tr("Navegador")) { openWeb("https://www.google.com") }
                contact?.takeIf { it.uid.isNotBlank() }?.let { peer ->
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(NexusTokens.Radius.md))
                            .clickable { go("profile_viewer/${peer.uid}") },
                        contentAlignment = Alignment.Center
                    ) {
                        UserAvatar(peer.displayName.ifBlank { peer.name }, peer.photoUrl, size = 40.dp)
                    }
                }
            }
            HorizontalDivider(color = NexusTokens.Color.Hairline, modifier = Modifier.padding(vertical = 6.dp))
            QuickRow(Icons.Outlined.Lock, "Privacidad") { go(Screen.SettingsPrivacy.route) }
            QuickRow(Icons.Outlined.Download, "Descargar vídeos") { go("video_download") }
            QuickRow(Icons.Outlined.AddAPhoto, "Nueva historia") { go(Screen.CreateStory.route) }
            QuickRow(Icons.Outlined.SelectAll, "Seleccionar todo") {
                onDismiss()
                onSelectAll()
            }
            QuickRow(Icons.Outlined.Wallpaper, "Fondo del chat") { go(Screen.BackgroundPicker.createRoute(chatId)) }
            QuickRow(Icons.Outlined.VerticalAlignTop, "Primer mensaje") {
                onDismiss()
                onFirstMessage()
            }
            QuickRow(Icons.Outlined.Link, "Enlace de la conversación") {
                onDismiss()
                if (chatId.startsWith("group_")) {
                    navController.navigate("group_info/$chatId")
                } else {
                    val link = contact?.uid?.takeIf { it.isNotBlank() }?.let { "nexuschat://profile/$it" }
                    if (link != null) {
                        context.startActivity(
                            Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link), null)
                        )
                    }
                }
            }
            QuickRow(Icons.Outlined.BarChart, "Contador de mensajes") { showCounter = true }
        }
    }
}

@Composable
private fun ShortcutTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(48.dp)
            .clip(RoundedCornerShape(NexusTokens.Radius.md))
            .background(NexusTokens.Color.BgElevated)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun QuickRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(tr(label), style = MaterialTheme.typography.bodyLarge, color = NexusTokens.Color.TextPrimary)
    }
}
