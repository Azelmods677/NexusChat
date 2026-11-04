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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.BuildConfig
import com.Azelmods.App.data.preferences.TutorialPreferences
import com.Azelmods.App.data.tutorials.AppFeature
import com.Azelmods.App.i18n.AppLanguage
import com.Azelmods.App.i18n.I18n
import com.Azelmods.App.ui.components.AutoTutorial
import com.Azelmods.App.ui.components.NexusDivider
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.NexusTopBar
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

    val userId = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    var userName by remember { mutableStateOf("") }
    var userHandle by remember { mutableStateOf("") }
    var userPhoto by remember { mutableStateOf<String?>(null) }
    var userColor by remember { mutableIntStateOf(-1) }

    LaunchedEffect(Unit) {
        if (userId.isBlank()) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            runCatching {
                val snapshot = com.google.firebase.database.FirebaseDatabase.getInstance().reference
                    .child("users").child(userId).get().await()
                userName = snapshot.child("displayName").getValue(String::class.java).orEmpty()
                userHandle = snapshot.child("username").getValue(String::class.java).orEmpty()
                userPhoto = snapshot.child("photoUrl").getValue(String::class.java)?.takeIf { it.isNotBlank() }
                userColor = snapshot.child("profileColor").getValue(Int::class.java) ?: -1
            }.onFailure { android.util.Log.e("SettingsScreen", "No se pudo cargar el perfil", it) }
        }
    }

    AutoTutorial(feature = AppFeature.SETTINGS, tutorialPreferences = tutorialPreferences)

    Scaffold(
        topBar = { NexusTopBar(title = tr("Ajustes"), onBack = { navController.popBackStack() }) },
        containerColor = NexusTokens.Color.BgBase
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsDashboard(
                userName = userName,
                userHandle = userHandle,
                userPhoto = userPhoto,
                userColor = userColor,
                userId = userId,
                onOpenProfile = { navController.navigate(Screen.Profile.createRoute(userId)) },
                onOpenAbout = { navController.navigate(Screen.SettingsAbout.route) },
                onOpenSecurity = { navController.navigate(Screen.Security.route) }
            )

            SettingsSection(title = tr("Cuenta y privacidad")) {
                SettingsEntry(Icons.Outlined.WorkspacePremium, TileAmber, "Premium", "Insignia, colores exclusivos y galería sin límite") {
                    navController.navigate(Screen.Premium.route)
                }
                SettingsEntry(Icons.Outlined.Brush, TilePink, "Personalizar perfil", "Color, aro y portada") {
                    navController.navigate(Screen.ProfileCustomize.route)
                }
                SettingsEntry(Icons.Outlined.Person, TileBlue, "Cuenta", "Nombre, usuario, teléfono y correo") {
                    navController.navigate(Screen.SettingsAccount.route)
                }
                SettingsEntry(Icons.Outlined.Lock, TileGreen, "Privacidad", "Última conexión, confirmaciones, bloqueados") {
                    navController.navigate(Screen.SettingsPrivacy.route)
                }
                SettingsEntry(Icons.Outlined.Security, TileViolet, "Seguridad y Tor", "Modo anónimo, fantasma, bloqueo y copias") {
                    navController.navigate(Screen.Security.route)
                }
            }

            SettingsSection(title = tr("Preferencias")) {
                SettingsEntry(Icons.Outlined.Notifications, TileOrange, "Notificaciones", "Sonido, vibración y vista previa") {
                    navController.navigate(Screen.SettingsNotifications.route)
                }
                SettingsEntry(Icons.Outlined.Palette, TilePink, "Apariencia", "Color de acento, aros, letra y fondo") {
                    navController.navigate(Screen.SettingsAppearance.route)
                }
                SettingsEntry(Icons.Outlined.Language, TileCyan, "Idioma de la app", I18n.selected(context).label) {
                    showLanguageDialog = true
                }
                SettingsEntry(Icons.Outlined.Translate, TileCyan, "Idioma de traducción", "A qué idioma se traducen los mensajes") {
                    navController.navigate("translation_language")
                }
                SettingsEntry(Icons.Outlined.Storage, TileAmber, "Almacenamiento y datos", "Descarga automática y espacio usado") {
                    navController.navigate(Screen.SettingsStorage.route)
                }
            }

            SettingsSection(title = tr("Herramientas")) {
                SettingsEntry(Icons.Outlined.SmartToy, TileViolet, "Inteligencia artificial", "Proveedor, modelo y funciones en los chats") {
                    navController.navigate(Screen.AiFeatures.route)
                }
                SettingsEntry(Icons.Outlined.Code, TileGreen, "Editor de código", "Pestañas, búsqueda y ejecución") {
                    navController.navigate("code_editor")
                }
                SettingsEntry(Icons.Outlined.Terminal, TileSlate, "Terminal", "Sesión de shell real del dispositivo") {
                    navController.navigate("terminal")
                }
            }

            SettingsSection(title = tr("Información")) {
                SettingsEntry(Icons.AutoMirrored.Outlined.Help, TileBlue, "Ayuda", "Guías de cada función") {
                    navController.navigate(Screen.SettingsHelp.route)
                }
                SettingsEntry(Icons.Outlined.Info, TileSlate, "Acerca de", "Versión ${BuildConfig.VERSION_NAME} y datos del dispositivo") {
                    navController.navigate(Screen.SettingsAbout.route)
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
