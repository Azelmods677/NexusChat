package com.Azelmods.App.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.security.tor.OrbotDetector
import com.Azelmods.App.data.security.tor.OrbotState
import com.Azelmods.App.data.security.tor.OrbotUiStatus
import com.Azelmods.App.data.security.tor.TorState
import com.Azelmods.App.data.security.tor.mapOrbotStatus
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pantalla de control de Tor - simplificada para usar Orbot
 *
 * Muestra:
 * - Toggle de modo anónimo
 * - Estado de conexión
 * - Información sobre Orbot
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TorControlScreen(
    navController: NavController,
    viewModel: SecurityViewModel = hiltViewModel()
) {
    val torState by viewModel.torState.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = androidx.compose.ui.platform.LocalContext.current

    // Estado REAL de Orbot, comprobado en background (los sockets de OrbotDetector
    // bloquean). Antes esta pantalla no lo consultaba: si TorService reportaba error
    // mostraba una tarjeta roja de "Orbot no está instalado" aunque Orbot estuviera
    // instalado y conectado. Se re-comprueba cuando cambia el estado de Tor y de
    // forma periódica, para que arrancar Orbot se detecte sin salir de la pantalla.
    var orbotStatus by remember { mutableStateOf<OrbotUiStatus?>(null) }
    LaunchedEffect(torState) {
        while (true) {
            orbotStatus = withContext(Dispatchers.IO) {
                runCatching {
                    mapOrbotStatus(
                        installed = OrbotDetector.isOrbotUsable(context),
                        active = OrbotDetector.isTorAvailable()
                    )
                }.getOrElse { mapOrbotStatus(installed = false, active = false) }
            }
            kotlinx.coroutines.delay(4000)
        }
    }

    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is SecurityUiState.Success -> {
                snackbarHostState.showSnackbar(
                    message = state.message,
                    duration = SnackbarDuration.Short
                )
                viewModel.clearUiState()
            }
            is SecurityUiState.Error -> {
                snackbarHostState.showSnackbar(
                    message = "${state.message}\n${state.suggestion}",
                    duration = SnackbarDuration.Long
                )
                viewModel.clearUiState()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control Tor") },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            // Anonymous Mode Toggle
            AnonymousModeToggle(
                torState = torState,
                onToggle = { enabled ->
                    if (enabled) {
                        viewModel.enableAnonymousMode()
                    } else {
                        viewModel.disableAnonymousMode()
                    }
                },
                orbotStatus = orbotStatus,
                onOrbotAction = {
                    when (orbotStatus?.state) {
                        OrbotState.NOT_INSTALLED -> {
                            runCatching {
                                context.startActivity(
                                    android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(
                                            "https://play.google.com/store/apps/details?id=org.torproject.android"
                                        )
                                    )
                                )
                            }
                        }
                        OrbotState.INSTALLED_INACTIVE -> OrbotDetector.launchOrbot(context)
                        else -> Unit
                    }
                }
            )

            // Información sobre Orbot
            InfoCard(
                title = "¿Cómo funciona?",
                description = "El modo anónimo usa Orbot para conectar a la red Tor. " +
                        "Orbot debe estar instalado y ejecutándose. " +
                        "Descárgalo desde Play Store o F-Droid: org.torproject.android"
            )

            // Error display — solo si Orbot NO está funcionando. Con Orbot activo, un
            // TorState.Error residual no debe pintar una tarjeta roja alarmante:
            // el estado accionable de Orbot ya se muestra dentro del toggle.
            if (torState is TorState.Error && orbotStatus?.state != OrbotState.ACTIVE) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Error de conexión",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = (torState as TorState.Error).message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { viewModel.enableAnonymousMode() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    description: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }
    }
}
