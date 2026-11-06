package com.Azelmods.App.ui.screens.security

import com.Azelmods.App.i18n.tr

import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.net.Uri
import com.Azelmods.App.data.preferences.TutorialPreferences
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.data.tutorials.AppFeature
import com.Azelmods.App.ui.components.AutoTutorial
import com.Azelmods.App.ui.components.NexusListRow
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.NexusTopBar
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens

/** Punto de entrada a la seguridad: una lista corta en la que cada fila hace algo real. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    navController: NavController,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val tutorialPreferences = remember { TutorialPreferences(context) }
    val torState by viewModel.torState.collectAsState()
    var showAppLockDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }

    AutoTutorial(feature = AppFeature.SECURITY, tutorialPreferences = tutorialPreferences)

    Scaffold(
        topBar = { NexusTopBar(title = tr("Seguridad"), onBack = { navController.popBackStack() }) },
        containerColor = NexusTokens.Color.BgBase
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            NexusSectionHeader(tr("Red"))
            NexusListRow(
                title = tr("Modo anónimo"),
                subtitle = when (val state = torState) {
                    is TorState.Disconnected -> tr("Desactivado")
                    is TorState.Connecting -> "Conectando: ${state.progress} %"
                    is TorState.Bootstrapping -> "Conectando: ${state.progress} %"
                    is TorState.Connected -> state.circuitInfo.exitIp?.let { "Activo. IP visible: $it" } ?: tr("Activo")
                    is TorState.Error -> tr("Sin conexión con Tor")
                },
                icon = Icons.Outlined.Security,
                iconTint = if (torState is TorState.Connected) NexusTokens.Color.Online else NexusTokens.Color.TextSecondary,
                onClick = { navController.navigate(Screen.TorControl.route) }
            )
            NexusListRow(
                title = tr("Navegador privado"),
                subtitle = tr("Sitios .onion y navegación sin rastro"),
                icon = Icons.Outlined.Language,
                onClick = { navController.navigate(Screen.TorBrowser.route) }
            )

            NexusSectionHeader(tr("Datos"))
            NexusListRow(
                title = tr("Bloqueo de la app"),
                subtitle = tr("PIN o huella al abrirla"),
                icon = Icons.Outlined.Lock,
                onClick = { showAppLockDialog = true }
            )
            NexusListRow(
                title = tr("Copia de seguridad cifrada"),
                subtitle = tr("Exporta tus chats protegidos con una contraseña"),
                icon = Icons.Outlined.CloudUpload,
                onClick = { showBackupDialog = true }
            )

            NexusSectionHeader(tr("Mensajes temporales"))
            Text(
                tr("Se activan en cada chat con el icono del reloj. Los mensajes se borran para todos " +
                    "al cumplirse el tiempo desde que se leen, y el chat bloquea las capturas de pantalla."),
                style = MaterialTheme.typography.bodyMedium,
                color = NexusTokens.Color.TextSecondary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )

            NexusSectionHeader(tr("Actividad"))
            val ghostMode by viewModel.ghostModeEnabled.collectAsState()
            com.Azelmods.App.ui.components.NexusSwitchRow(
                title = tr("Modo fantasma"),
                subtitle = tr("No publica si estás conectado, si escribes ni si has leído. Tu IP no cambia."),
                icon = Icons.Outlined.VisibilityOff,
                checked = ghostMode,
                onCheckedChange = viewModel::setGhostMode
            )

            NexusSectionHeader(tr("Herramientas"))
            NexusListRow(
                title = tr("Terminal"),
                subtitle = tr("Sesión de shell real del dispositivo"),
                icon = Icons.Outlined.Terminal,
                onClick = { navController.navigate("terminal") }
            )
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showAppLockDialog) AppLockSetupDialog(onDismiss = { showAppLockDialog = false })
    if (showBackupDialog) BackupRestoreDialog(onDismiss = { showBackupDialog = false })
}

@Composable
private fun AppLockSetupDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { com.Azelmods.App.data.preferences.AppLockPreferences(context) }
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var autoLock by remember { mutableIntStateOf(5) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Bloqueo de aplicación")) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text(tr("PIN (4+ dígitos)")) },
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text(tr("Confirmar PIN")) },
                    singleLine = true
                )
                Text(tr("Auto-bloqueo (minutos): %s", autoLock.toString()))
                Slider(
                    value = autoLock.toFloat(),
                    onValueChange = { autoLock = it.toInt() },
                    valueRange = 0f..30f,
                    steps = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (pin.length >= 4 && pin == confirmPin) {
                    scope.launch {
                        prefs.setPin(pin)
                        prefs.setLockEnabled(true)
                        prefs.setAutoLockMinutes(if (autoLock <= 0) 0 else if (autoLock <= 2) 1 else if (autoLock <= 7) 5 else 30)
                        onDismiss()
                    }
                }
            }) { Text(tr("Guardar")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(tr("Cancelar")) } }
    )
}

@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface BackupEntryPoint {
    fun backupManager(): com.Azelmods.App.data.backup.BackupManager
}

@Composable
private fun BackupRestoreDialog(onDismiss: () -> Unit) {
    val backupManager = dagger.hilt.android.EntryPointAccessors.fromApplication(
        LocalContext.current.applicationContext,
        BackupEntryPoint::class.java
    ).backupManager()
    var password by remember { mutableStateOf("") }
    var progress by remember { mutableStateOf("") }
    var working by remember { mutableStateOf(false) }
    // Id de la copia ya creada, a la espera de que el usuario elija dónde guardarla.
    var pendingBackupId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // Selector del sistema "Guardar como…". Aquí es donde la copia sale de la app a
    // un sitio que el usuario controla (Descargas, Drive, etc.): eso es "exportar".
    val saveLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri: Uri? ->
        val backupId = pendingBackupId
        if (uri == null) {
            // El usuario canceló el selector. La copia sigue guardada dentro de la app.
            progress = "Copia creada, pero no se exportó (cancelaste el guardado)."
            working = false
        } else if (backupId != null) {
            scope.launch {
                progress = "Exportando…"
                val ok = backupManager.exportBackupTo(backupId, uri)
                progress = if (ok) tr("✅ Copia exportada correctamente.") else tr("No se pudo exportar el archivo.")
                working = false
                pendingBackupId = null
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!working) onDismiss() },
        title = { Text(tr("Copia de seguridad")) },
        text = {
            Column {
                Text(
                    tr("Se crea una copia CIFRADA de tus chats, contactos y perfil, y eliges " +
                        "dónde guardarla. Necesitarás esta contraseña para restaurarla."),
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(tr("Contraseña de cifrado")) },
                    singleLine = true,
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    enabled = !working
                )
                if (progress.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    if (working) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                    Text(progress, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !working && password.length >= 4,
                onClick = {
                    working = true
                    scope.launch {
                        backupManager.createBackup(
                            password,
                            com.Azelmods.App.data.backup.StorageLocation.LOCAL
                        ).collect { result ->
                            when (result) {
                                is com.Azelmods.App.data.backup.BackupResult.Progress ->
                                    progress = result.message
                                is com.Azelmods.App.data.backup.BackupResult.Success -> {
                                    // La copia ya existe dentro de la app: ahora se
                                    // pide al usuario dónde exportarla realmente.
                                    pendingBackupId = result.backupId
                                    progress = "Elige dónde guardar el archivo…"
                                    saveLauncher.launch(backupManager.suggestedExportName())
                                }
                                is com.Azelmods.App.data.backup.BackupResult.Error -> {
                                    progress = "Error: ${result.message}"
                                    working = false
                                }
                            }
                        }
                    }
                }
            ) { Text(tr("Exportar")) }
        },
        dismissButton = {
            TextButton(enabled = !working, onClick = onDismiss) { Text(tr("Cerrar")) }
        }
    )
}
