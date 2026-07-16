package com.Azelmods.App.ui.screens.security

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.Azelmods.App.data.preferences.TutorialPreferences
import com.Azelmods.App.data.security.tor.OrbotDetector
import com.Azelmods.App.data.security.tor.OrbotState
import com.Azelmods.App.data.security.tor.OrbotUiStatus
import com.Azelmods.App.data.security.tor.mapOrbotStatus
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.data.tutorials.AppFeature
import com.Azelmods.App.ui.components.AutoTutorial
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Main security screen - entry point for all security features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityScreen(
    navController: NavController,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val tutorialPreferences = remember { TutorialPreferences(context) }
    val torState by viewModel.torState.collectAsState()
    val torLogs by viewModel.torLogs.collectAsState()
    var showAppLockDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showSelfDestructInfo by remember { mutableStateOf(false) }

    // Estado de Orbot mapeado a UI clara/accionable. Se verifica en el IOThread
    // (los sockets de OrbotDetector bloquean) y nunca deja el toggle "roto".
    var orbotStatus by remember { mutableStateOf<OrbotUiStatus?>(null) }

    // Recheck del estado de Orbot al entrar y cuando cambia el estado de Tor.
    LaunchedEffect(torState) {
        orbotStatus = checkOrbotStatus(context)
    }

    // Show tutorial on first visit
    AutoTutorial(
        feature = AppFeature.SECURITY,
        tutorialPreferences = tutorialPreferences
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Security & Privacy") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Advanced Security Features",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                text = "Protect your privacy with military-grade encryption and anonymous networking",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            // ── Tor Integration ────────────────────────────────────
            SecurityFeatureCard(
                title = "Anonymous Mode (Tor)",
                description = "Route all traffic through the Tor network for maximum privacy",
                icon = Icons.Default.Security,
                isActive = torState is TorState.Connected,
                onClick = { navController.navigate(Screen.TorControl.route) }
            ) {
                AnonymousModeToggle(
                    torState = torState,
                    onToggle = { enabled ->
                        if (enabled) viewModel.enableAnonymousMode()
                        else viewModel.disableAnonymousMode()
                    },
                    orbotStatus = orbotStatus,
                    onOrbotAction = {
                        handleOrbotAction(context, orbotStatus)
                    }
                )

                // Detailed progress when actively connecting
                if (torState is TorState.Connecting) {
                    val connecting = torState as TorState.Connecting
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { connecting.progress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.DarkGray
                    )
                    if (connecting.message.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = connecting.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // ── Orbot Setup Guide ──────────────────────────────────
            SecurityFeatureCard(
                title = "Orbot Setup",
                description = "Guía para instalar y configurar Orbot para navegación anónima",
                icon = Icons.Default.FlightTakeoff,
                isActive = torState is TorState.Connected,
                onClick = { navController.navigate(Screen.OrbotWelcome.route) }
            )

            // ── Tor Browser ────────────────────────────────────────
            SecurityFeatureCard(
                title = "Tor Browser",
                description = "Browse .onion sites anonymously with DuckDuckGo integration",
                icon = Icons.Default.Language,
                isActive = false,
                onClick = { navController.navigate(Screen.TorBrowser.route) }
            )

            // ── Terminal ───────────────────────────────────────────
            SecurityFeatureCard(
                title = "Terminal",
                description = "Consola de comandos del sistema para usuarios avanzados",
                icon = Icons.Default.Terminal,
                isActive = false,
                onClick = { navController.navigate("terminal") }
            )

            SecurityFeatureCard(
                title = "Bloqueo de aplicación",
                description = "PIN o biometría. Auto-bloqueo: inmediato, 1, 5 o 30 min",
                icon = Icons.Default.Lock,
                isActive = true,
                onClick = { showAppLockDialog = true }
            )

            SecurityFeatureCard(
                title = "Copia de seguridad cifrada",
                description = "Exportar e importar chats en JSON con AES-256",
                icon = Icons.Default.CloudUpload,
                isActive = true,
                onClick = { showBackupDialog = true }
            )

            SecurityFeatureCard(
                title = "Mensajes autodestructivos",
                description = "Eliminación automática tras un tiempo configurado",
                icon = Icons.Default.Timer,
                isActive = true,
                onClick = { showSelfDestructInfo = true }
            )

            // ── Tor connection logs ────────────────────────────────
            if (torState !is TorState.Disconnected && torLogs.isNotEmpty()) {
                TorLogsCard(
                    logs = torLogs,
                    onClear = { viewModel.clearLogs() }
                )
            }

            // ── Info section ───────────────────────────────────────
            Spacer(modifier = Modifier.height(16.dp))
            InfoSection()
        }
    }

    if (showAppLockDialog) {
        AppLockSetupDialog(onDismiss = { showAppLockDialog = false })
    }
    if (showBackupDialog) {
        BackupRestoreDialog(onDismiss = { showBackupDialog = false })
    }
    if (showSelfDestructInfo) {
        AlertDialog(
            onDismissRequest = { showSelfDestructInfo = false },
            title = { Text("Mensajes autodestructivos") },
            text = { Text("Configura el temporizador en cada chat desde el menú del mensaje. Próxima versión: borrado automático en Firebase.") },
            confirmButton = {
                TextButton(onClick = { showSelfDestructInfo = false }) { Text("Entendido") }
            }
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SecurityFeatureCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SecurityFeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    isActive: Boolean,
    isEnabled: Boolean = true,
    onClick: () -> Unit,
    content: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isEnabled) Modifier.clickable(onClick = onClick)
                else Modifier
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )

                    Column {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isEnabled) Color.White else Color.Gray
                        )

                        if (isActive) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                if (isEnabled) {
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = if (isEnabled) Color.Gray else Color.DarkGray,
                fontSize = 13.sp
            )

            if (content != null) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// TorLogsCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TorLogsCard(logs: List<String>, onClear: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Connection Logs",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    TextButton(onClick = onClear) {
                        Text("Clear", color = Color.Gray, fontSize = 12.sp)
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess
                                          else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = Color.Gray
                        )
                    }
                }
            }

            if (expanded) {
                val listState = rememberLazyListState()
                LaunchedEffect(logs.size) {
                    if (logs.isNotEmpty()) listState.animateScrollToItem(logs.size - 1)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                ) {
                    items(logs) { log ->
                        Text(
                            text = log,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            fontSize = 10.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// InfoSection
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun InfoSection() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Column {
                    Text(
                        text = "About Security Features",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Nexus Chat provides advanced security features designed for users who require maximum privacy and anonymity. These features are intended for legitimate security research, privacy protection, and educational purposes only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "⚠️ Use responsibly and in accordance with local laws and regulations.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
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
        title = { Text("Bloqueo de aplicación") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it },
                    label = { Text("PIN (4+ dígitos)") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { confirmPin = it },
                    label = { Text("Confirmar PIN") },
                    singleLine = true
                )
                Text("Auto-bloqueo (minutos): $autoLock")
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
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
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
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Copia de seguridad") },
        text = {
            Column {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña de cifrado") },
                    singleLine = true
                )
                if (progress.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text(progress, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (password.isNotBlank()) {
                    scope.launch {
                        backupManager.createBackup(
                            password,
                            com.Azelmods.App.data.backup.StorageLocation.LOCAL
                        ).collect { result ->
                            when (result) {
                                is com.Azelmods.App.data.backup.BackupResult.Progress ->
                                    progress = result.message
                                is com.Azelmods.App.data.backup.BackupResult.Success ->
                                    progress = "Copia completada"
                                is com.Azelmods.App.data.backup.BackupResult.Error ->
                                    progress = "Error: ${result.message}"
                            }
                        }
                    }
                }
            }) { Text("Exportar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } }
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Orbot status helpers (Task 8.3 — Requirements 6.4, 6.5, 7.1, 7.3)
// ─────────────────────────────────────────────────────────────────────────────

private const val ORBOT_TAG = "OrbotStatus"

/**
 * Verifica el estado de Orbot en el IOThread (los sockets de [OrbotDetector]
 * bloquean) y lo mapea a un [OrbotUiStatus] consistente vía `mapOrbotStatus`.
 *
 * Ante cualquier excepción se registra y se devuelve un estado accionable
 * (tratado como "no instalado") manteniendo el toggle operable para reintentar,
 * permitiendo continuar en modo directo (degradación elegante).
 */
private suspend fun checkOrbotStatus(context: android.content.Context): OrbotUiStatus =
    withContext(Dispatchers.IO) {
        try {
            val installed = OrbotDetector.isOrbotInstalled(context)
            val active = OrbotDetector.isTorAvailable()
            mapOrbotStatus(installed = installed, active = active)
        } catch (e: Exception) {
            Log.e(ORBOT_TAG, "Fallo al comprobar el estado de Orbot", e)
            // Estado accionable; el toggle sigue operable para reintentar.
            mapOrbotStatus(installed = false, active = false)
        }
    }

/**
 * Ejecuta la acción sugerida por [orbotStatus]: descargar Orbot cuando no está
 * instalado, o abrir Orbot cuando está instalado pero inactivo.
 */
private fun handleOrbotAction(context: android.content.Context, orbotStatus: OrbotUiStatus?) {
    when (orbotStatus?.state) {
        OrbotState.NOT_INSTALLED -> openOrbotDownload(context)
        OrbotState.INSTALLED_INACTIVE -> {
            try {
                OrbotDetector.launchOrbot(context)
            } catch (e: Exception) {
                Log.e(ORBOT_TAG, "Fallo al abrir Orbot", e)
            }
        }
        else -> { /* ACTIVE o null: sin acción */ }
    }
}

/**
 * Abre la ficha de Orbot en Play Store, con respaldo a F-Droid y a la web.
 */
private fun openOrbotDownload(context: android.content.Context) {
    val targets = listOf(
        "https://play.google.com/store/apps/details?id=org.torproject.android",
        "https://f-droid.org/packages/org.torproject.android",
        "https://orbot.app"
    )
    for (url in targets) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            return
        } catch (e: Exception) {
            Log.w(ORBOT_TAG, "No se pudo abrir $url", e)
        }
    }
}
