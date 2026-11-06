package com.Azelmods.App.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.Azelmods.App.data.update.AppUpdateManager
import com.Azelmods.App.data.update.UpdateDownload
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.theme.NexusTokens

/** Aparece en cuanto hay una versión nueva, también con la app ya abierta. */
@Composable
fun AppUpdateDialog(manager: AppUpdateManager) {
    val update by manager.update.collectAsState()
    val download by manager.download.collectAsState()
    var dismissedVersion by rememberSaveable { mutableStateOf<String?>(null) }
    val uriHandler = LocalUriHandler.current

    val current = update ?: return
    if (!current.mandatory && dismissedVersion == current.versionName) return

    val isRunning = download is UpdateDownload.Running
    Dialog(
        onDismissRequest = { if (!isRunning && !current.mandatory) dismissedVersion = current.versionName },
        // Obligatoria: ni atrás ni tocar fuera la cierran.
        properties = androidx.compose.ui.window.DialogProperties(
            dismissOnBackPress = !current.mandatory,
            dismissOnClickOutside = !current.mandatory
        )
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(NexusTokens.Radius.xxl))
                .background(NexusTokens.Color.BgSurface)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(12.dp))
            Text(
                tr("NexusChat %s ya está aquí", current.versionName),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = NexusTokens.Color.TextPrimary
            )
            if (current.mandatory) {
                Spacer(Modifier.height(6.dp))
                Text(
                    tr("Hay que actualizar para seguir usando la app."),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (current.notes.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    current.notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextSecondary,
                    modifier = Modifier
                        .heightIn(max = 220.dp)
                        .verticalScroll(rememberScrollState())
                )
            }
            Spacer(Modifier.height(18.dp))

            when (val state = download) {
                is UpdateDownload.Running -> {
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        tr("Descargando… %d %%", (state.progress * 100).toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = NexusTokens.Color.TextMuted
                    )
                }
                is UpdateDownload.Ready -> NexusButton(
                    text = tr("Instalar"),
                    onClick = { manager.install(state.file) }
                )
                is UpdateDownload.Failed -> {
                    Text(
                        tr("No se pudo descargar. Revisa tu conexión."),
                        style = MaterialTheme.typography.bodySmall,
                        color = NexusTokens.Color.Error
                    )
                    Spacer(Modifier.height(10.dp))
                    NexusButton(text = tr("Reintentar"), onClick = { manager.startDownload(current) })
                    TextButton(onClick = { uriHandler.openUri(current.pageUrl) }) { Text(tr("Abrir en el navegador")) }
                }
                UpdateDownload.Idle -> NexusButton(
                    text = tr("Actualizar ahora"),
                    onClick = {
                        if (manager.apkUrlFor(current) != null) {
                            manager.startDownload(current)
                        } else {
                            uriHandler.openUri(current.pageUrl)
                        }
                    }
                )
            }

            if (!isRunning && !current.mandatory) {
                TextButton(onClick = { dismissedVersion = current.versionName }) {
                    Text(tr("Más tarde"), color = NexusTokens.Color.TextMuted)
                }
            }
        }
    }
}
