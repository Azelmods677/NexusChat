package com.Azelmods.App.ui.screens.settings

import com.Azelmods.App.i18n.tr

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val lastSeenEnabled by viewModel.lastSeenEnabled.collectAsState()
    val profilePhotoVisible by viewModel.profilePhotoVisible.collectAsState()
    val readReceiptsEnabled by viewModel.readReceiptsEnabled.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 2FA real (TOTP), respaldado por AppLockPreferences y aplicado en el bloqueo.
    val appLockPrefs = remember {
        dagger.hilt.android.EntryPointAccessors.fromApplication(
            context.applicationContext,
            TwoFactorEntryPoint::class.java
        ).appLockPreferences()
    }
    var twoFaEnabled by remember { mutableStateOf(false) }
    var show2faSetup by remember { mutableStateOf(false) }
    var show2faDisable by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appLockPrefs.is2faEnabled.collect { twoFaEnabled = it }
    }

    var showBlockedUsersDialog by remember { mutableStateOf(false) }
    var showActiveSessionsDialog by remember { mutableStateOf(false) }
    var showDownloadDataDialog by remember { mutableStateOf(false) }
    var showDeleteDataDialog by remember { mutableStateOf(false) }
    var deleteDataConfirm by remember { mutableStateOf("") }
    // Lista real desde Firebase (antes era una lista local vacía que nunca se llenaba).
    val blockedContacts by viewModel.blockedContacts.collectAsState()
    val isLoadingBlocked by viewModel.isLoadingBlocked.collectAsState()
    var isLoadingData by remember { mutableStateOf(false) }
    var dataExportResult by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Privacidad y seguridad"), color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Atrás"), tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
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
        ) {
            // Privacy section
            Text(
                text = tr("Privacidad"),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            SettingsSwitchItem(
                title = tr("Última conexión"),
                subtitle = tr("Mostrar cuándo estuviste conectado por última vez"),
                icon = Icons.Default.Visibility,
                checked = lastSeenEnabled,
                onCheckedChange = { viewModel.setLastSeenEnabled(it) }
            )

            SettingsSwitchItem(
                title = tr("Foto de perfil"),
                subtitle = tr("Quién puede ver tu foto de perfil"),
                icon = Icons.Default.Photo,
                checked = profilePhotoVisible,
                onCheckedChange = { viewModel.setProfilePhotoVisible(it) }
            )

            SettingsSwitchItem(
                title = tr("Confirmaciones de lectura"),
                subtitle = tr("Mostrar cuándo has leído los mensajes"),
                icon = Icons.Default.DoneAll,
                checked = readReceiptsEnabled,
                onCheckedChange = { viewModel.setReadReceiptsEnabled(it) }
            )

            SettingsItem(
                title = tr("Contactos bloqueados"),
                subtitle = if (blockedContacts.isEmpty()) tr("Ninguno")
                           else "${blockedContacts.size} bloqueado(s)",
                icon = Icons.Default.Block,
                onClick = {
                    viewModel.loadBlockedContacts()
                    showBlockedUsersDialog = true
                }
            )

            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))

            // Security section
            Text(
                text = tr("Seguridad"),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            SettingsSwitchItem(
                title = tr("Verificación en dos pasos (2FA)"),
                subtitle = if (twoFaEnabled) tr("Activa: se pide un código al desbloquear")
                           else tr("Añade un código TOTP sobre el bloqueo de la app"),
                icon = Icons.Default.Security,
                checked = twoFaEnabled,
                onCheckedChange = { turnOn ->
                    if (turnOn) show2faSetup = true else show2faDisable = true
                }
            )

            SettingsItem(
                title = tr("Sesiones activas"),
                subtitle = tr("Gestiona tus sesiones activas"),
                icon = Icons.Default.Devices,
                onClick = { showActiveSessionsDialog = true }
            )

            SettingsItem(
                title = tr("Bloqueo con PIN"),
                subtitle = tr("Pedir código al abrir la app"),
                icon = Icons.Default.Lock,
                onClick = { navController.navigate(Screen.Security.route) }
            )

            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))

            // Data section
            Text(
                text = tr("Datos"),
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            SettingsItem(
                title = tr("Descargar mis datos"),
                subtitle = tr("Pide una copia de tus datos"),
                icon = Icons.Default.Download,
                onClick = { showDownloadDataDialog = true }
            )

            SettingsItem(
                title = tr("Borrar mis datos"),
                subtitle = tr("Borra para siempre todos tus datos"),
                icon = Icons.Default.DeleteForever,
                iconTint = Color.Red,
                onClick = { showDeleteDataDialog = true }
            )
        }
    }

    // 2FA: alta (genera secreto y pide confirmar un código) y baja.
    if (show2faSetup) {
        TwoFactorSetupDialog(
            appLockPreferences = appLockPrefs,
            onDismiss = { show2faSetup = false }
        )
    }
    if (show2faDisable) {
        TwoFactorDisableDialog(
            appLockPreferences = appLockPrefs,
            onDismiss = { show2faDisable = false }
        )
    }

    // Blocked Users Dialog
    if (showBlockedUsersDialog) {
        AlertDialog(
            onDismissRequest = { showBlockedUsersDialog = false },
            title = { Text(tr("Contactos bloqueados"), color = Color.White) },
            text = {
                Column {
                    when {
                        isLoadingBlocked -> {
                            Text(tr("Cargando…"), color = Color.Gray)
                        }
                        blockedContacts.isEmpty() -> {
                            Text(tr("No has bloqueado a nadie."), color = Color.Gray)
                        }
                        else -> {
                            blockedContacts.forEach { contact ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                                ) {
                                    Text(
                                        contact.name,
                                        color = Color.White,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = {
                                            viewModel.unblockContact(contact.chatId, contact.uid)
                                        }
                                    ) {
                                        Text(tr("Desbloquear"), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp)
                                    }
                                }
                                HorizontalDivider(color = DarkSurface)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        tr("Para bloquear a alguien, mantén pulsada su conversación en la " +
                            "lista de chats y elige Bloquear. Quien esté bloqueado no podrá " +
                            "escribirte en ese chat."),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showBlockedUsersDialog = false }) {
                    Text(tr("Cerrar"), color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Active Sessions Dialog
    if (showActiveSessionsDialog) {
        AlertDialog(
            onDismissRequest = { showActiveSessionsDialog = false },
            title = { Text(tr("Sesiones activas"), color = Color.White) },
            text = {
                Column {
                    Text(tr("Este dispositivo (actual)"), color = Color.White)
                    Text(tr("Android - NexusChat"), color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(tr("Las sesiones las gestiona Firebase Auth."), color = Color.Gray, fontSize = 12.sp)
                }
            },
            confirmButton = {
                TextButton(onClick = { showActiveSessionsDialog = false }) {
                    Text(tr("Cerrar"), color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Download Data Dialog
    if (showDownloadDataDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDataDialog = false },
            title = { Text(tr("Descargar mis datos"), color = Color.White) },
            text = {
                Column {
                    if (isLoadingData) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    } else if (dataExportResult != null) {
                        Text(tr("Datos exportados"), color = TerminalGreen)
                        Text(dataExportResult ?: "", color = Color.Gray, fontSize = 12.sp)
                    } else {
                        Text(tr("Exporta tu perfil, chats y ajustes en un archivo JSON."), color = Color.Gray)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            isLoadingData = true
                            try {
                                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                val snapshot = FirebaseDatabase.getInstance().reference
                                    .child("users").child(uid).get().await()
                                val json = JSONObject()
                                snapshot.children.forEach { child ->
                                    json.put(child.key ?: "", child.value?.toString() ?: "")
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, "My NexusChat Data")
                                    putExtra(Intent.EXTRA_TEXT, json.toString(2))
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Share Data"))
                                dataExportResult = "JSON shared successfully"
                            } catch (e: Exception) {
                                dataExportResult = "Error: ${e.message}"
                            }
                            isLoadingData = false
                        }
                    },
                    enabled = !isLoadingData
                ) {
                    Text(tr("Exportar"), color = MaterialTheme.colorScheme.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDataDialog = false }) {
                    Text(tr("Cancelar"), color = Color.Gray)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Delete Data Dialog
    if (showDeleteDataDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDataDialog = false },
            title = { Text(tr("Borrar mis datos"), color = ErrorRed) },
            text = {
                Column {
                    Text(
                        tr("Esto borrará todos tus mensajes, chats y ajustes de este dispositivo. Tu cuenta seguirá activa."),
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(tr("Escribe BORRAR para confirmar:"), color = Color.White)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = deleteDataConfirm,
                        onValueChange = { deleteDataConfirm = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = ErrorRed,
                            unfocusedBorderColor = Color.Gray
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteDataConfirm == "BORRAR") {
                            scope.launch {
                                try {
                                    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                                    // Clear local preferences
                                    viewModel.clearAllData()
                                    // Clear userChats index
                                    FirebaseDatabase.getInstance().reference
                                        .child("userChats").child(uid).removeValue().await()
                                    showDeleteDataDialog = false
                                    deleteDataConfirm = ""
                                } catch (e: Exception) {
                                    // handle error
                                }
                            }
                        }
                    },
                    enabled = deleteDataConfirm == "BORRAR"
                ) {
                    Text(tr("Eliminar"), color = ErrorRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDataDialog = false }) {
                    Text(tr("Cancelar"), color = Color.Gray)
                }
            },
            containerColor = DarkSurface
        )
    }
}

/** Acceso a [AppLockPreferences] desde un Composable sin pasar por el ViewModel. */
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface TwoFactorEntryPoint {
    fun appLockPreferences(): com.Azelmods.App.data.preferences.AppLockPreferences
}

/**
 * Alta de 2FA: genera un secreto TOTP, lo muestra para que el usuario lo añada a su
 * app de autenticación (o lo copie), y exige un código válido antes de activarlo.
 * Confirmar con un código demuestra que el secreto quedó bien guardado en ambos lados.
 *
 * Requisito: debe existir ya un PIN de bloqueo, porque el 2FA se exige en la pantalla
 * de desbloqueo (sin primer factor, no hay dónde pedir el segundo).
 */
@Composable
private fun TwoFactorSetupDialog(
    appLockPreferences: com.Azelmods.App.data.preferences.AppLockPreferences,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var hasPin by remember { mutableStateOf<Boolean?>(null) }
    val secret = remember { com.Azelmods.App.data.security.TotpAuthenticator.generateSecret() }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { hasPin = appLockPreferences.hasPin() }

    val account = remember {
        FirebaseAuth.getInstance().currentUser?.let { it.email ?: it.phoneNumber ?: it.uid } ?: "cuenta"
    }
    val otpauthUri = remember(secret) {
        com.Azelmods.App.data.security.TotpAuthenticator.provisioningUri(secret, account)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Activar verificación en dos pasos"), color = Color.White) },
        text = {
            Column {
                when (hasPin) {
                    false -> Text(
                        tr("Primero activa el bloqueo con PIN (Passcode Lock). El segundo " +
                            "factor se pide en esa pantalla de desbloqueo."),
                        color = Color.Gray, fontSize = 13.sp
                    )
                    else -> {
                        Text(
                            tr("1) Añade esta clave a tu app de autenticación (Google " +
                                "Authenticator, Aegis…). 2) Escribe el código que te muestre."),
                            color = Color.Gray, fontSize = 13.sp
                        )
                        Spacer(Modifier.height(12.dp))
                        // Clave en grupos de 4 para poder teclearla sin errores.
                        Text(
                            secret.chunked(4).joinToString(" "),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = {
                            val clip = context.getSystemService(Context.CLIPBOARD_SERVICE)
                                as android.content.ClipboardManager
                            clip.setPrimaryClip(android.content.ClipData.newPlainText("2FA", secret))
                        }) { Text(tr("Copiar clave"), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) }
                        TextButton(onClick = {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, android.net.Uri.parse(otpauthUri))
                                )
                            }
                        }) { Text(tr("Abrir en app de autenticación"), color = MaterialTheme.colorScheme.primary, fontSize = 13.sp) }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = code,
                            onValueChange = { code = it.filter { c -> c.isDigit() }.take(6); error = null },
                            label = { Text(tr("Código de 6 dígitos")) },
                            singleLine = true,
                            isError = error != null,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                            )
                        )
                        if (error != null) {
                            Spacer(Modifier.height(4.dp))
                            Text(error!!, color = ErrorRed, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hasPin == true) {
                TextButton(
                    enabled = code.length == 6,
                    onClick = {
                        if (com.Azelmods.App.data.security.TotpAuthenticator.verify(secret, code)) {
                            scope.launch {
                                appLockPreferences.enable2fa(secret)
                                onDismiss()
                            }
                        } else {
                            error = "Código incorrecto. Revisa la hora del teléfono e inténtalo otra vez."
                        }
                    }
                ) { Text(tr("Activar"), color = MaterialTheme.colorScheme.primary) }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Cancelar"), color = Color.Gray) }
        },
        containerColor = DarkSurface
    )
}

/** Baja de 2FA: confirma y borra el secreto. */
@Composable
private fun TwoFactorDisableDialog(
    appLockPreferences: com.Azelmods.App.data.preferences.AppLockPreferences,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(tr("Desactivar 2FA"), color = Color.White) },
        text = {
            Text(
                tr("Se dejará de pedir el código al desbloquear y se borrará la clave " +
                    "guardada. Podrás volver a activarlo cuando quieras."),
                color = Color.Gray, fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    appLockPreferences.disable2fa()
                    onDismiss()
                }
            }) { Text(tr("Desactivar"), color = ErrorRed) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(tr("Cancelar"), color = Color.Gray) }
        },
        containerColor = DarkSurface
    )
}

@Composable
fun SettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}
