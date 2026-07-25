package com.Azelmods.App.ui.screens.settings

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
import androidx.navigation.NavController
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.*

/**
 * Ayuda y soporte.
 *
 * Revisión de la v5: esta pantalla enlazaba a un ecosistema que NO EXISTE —
 * `azelmods.com`, `docs.azelmods.com`, `community.azelmods.com`,
 * `feedback.azelmods.com`, `support@azelmods.com`, `bugs@azelmods.com`—. Todos
 * esos botones abrían un navegador o un cliente de correo hacia la nada, que es
 * peor que no ofrecer soporte: el usuario cree que hay un canal y no lo hay.
 *
 * Ahora todo apunta al único canal real del proyecto: **el repositorio de GitHub**.
 * Las guías se leen dentro de la app, sin conexión.
 */
private const val REPO_URL = "https://github.com/Azelmods677/NexusChat"
private const val ISSUES_URL = "$REPO_URL/issues"
private const val LICENSE_URL = "$REPO_URL/blob/main/LICENSE"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    navController: NavController
) {
    val context = LocalContext.current

    /** Abre una URL sin dejar que un dispositivo sin navegador tumbe la pantalla. */
    fun open(url: String) {
        try {
            context.startActivity(
                android.content.Intent(
                    android.content.Intent.ACTION_VIEW,
                    android.net.Uri.parse(url)
                )
            )
        } catch (e: Exception) {
            android.util.Log.e("HelpSupport", "No se pudo abrir $url", e)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ayuda y soporte") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
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
        ) {
            // ── GUÍAS ────────────────────────────────────────────────────────
            SectionHeader("Guías y aprendizaje")

            Text(
                text = "Se leen dentro de la app, sin conexión. Describen únicamente " +
                    "funciones que existen en esta versión.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )

            // Los ids DEBEN coincidir con TutorialContent.tutorials. Antes se navegaba
            // a "tutorial/gestures" mientras el tutorial se llamaba "touch_gestures":
            // el enlace abría una pantalla vacía.
            SettingsItem(
                title = "🚀 Primeros pasos",
                subtitle = "Crear tu cuenta y moverte por la app",
                icon = Icons.Default.School,
                onClick = { navController.navigate("tutorial/getting_started") }
            )
            SettingsItem(
                title = "💬 Mensajería",
                subtitle = "Chats, multimedia, cifrado y mensajes temporales",
                icon = Icons.AutoMirrored.Filled.Message,
                onClick = { navController.navigate("tutorial/messaging") }
            )
            SettingsItem(
                title = "📸 Historias",
                subtitle = "Crear historias con música y dibujo",
                icon = Icons.Default.PhotoLibrary,
                onClick = { navController.navigate("tutorial/stories") }
            )
            SettingsItem(
                title = "📞 Llamadas",
                subtitle = "Voz y vídeo P2P con WebRTC",
                icon = Icons.Default.Call,
                onClick = { navController.navigate("tutorial/calls") }
            )
            SettingsItem(
                title = "🤖 Azel IA",
                subtitle = "Elegir proveedor, modelo y clave propia",
                icon = Icons.Default.Psychology,
                onClick = { navController.navigate("tutorial/ai_features") }
            )
            SettingsItem(
                title = "🔒 Privacidad",
                subtitle = "Cifrado, bloqueo biométrico y copias cifradas",
                icon = Icons.Default.Security,
                onClick = { navController.navigate("tutorial/privacy") }
            )
            SettingsItem(
                title = "🧅 Tor y Orbot",
                subtitle = "Navegación anónima y sitios .onion",
                icon = Icons.Default.Shield,
                onClick = { navController.navigate("tutorial/tor") }
            )
            SettingsItem(
                title = "🎨 Apariencia",
                subtitle = "25 acentos, tamaño de texto y fondos",
                icon = Icons.Default.Palette,
                onClick = { navController.navigate("tutorial/appearance") }
            )
            SettingsItem(
                title = "⚙️ Editor y terminal",
                subtitle = "Editor con resaltado y emulador de terminal",
                icon = Icons.Default.DeveloperMode,
                onClick = { navController.navigate("tutorial/tools") }
            )
            SettingsItem(
                title = "👆 Gestos",
                subtitle = "Atajos táctiles en chats, historias y fotos",
                icon = Icons.Default.TouchApp,
                onClick = { navController.navigate("tutorial/touch_gestures") }
            )

            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))

            // ── SOPORTE ──────────────────────────────────────────────────────
            SectionHeader("Soporte")

            Text(
                text = "Nexus Chat es un proyecto de un solo desarrollador. El canal de " +
                    "soporte es el repositorio: ahí quedan registradas las incidencias y su estado.",
                color = Color.Gray,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)
            )

            SettingsItem(
                title = "Reportar un error",
                subtitle = "Abre una incidencia en GitHub",
                icon = Icons.Default.BugReport,
                onClick = { open(ISSUES_URL) }
            )
            SettingsItem(
                title = "Proponer una función",
                subtitle = "Comparte tu idea en el repositorio",
                icon = Icons.Default.Lightbulb,
                onClick = { open(ISSUES_URL) }
            )
            SettingsItem(
                title = "Código fuente",
                subtitle = "github.com/Azelmods677/NexusChat",
                icon = Icons.Default.Code,
                onClick = { open(REPO_URL) }
            )

            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))

            // ── PROYECTO ─────────────────────────────────────────────────────
            SectionHeader("Sobre el proyecto")

            SettingsItem(
                title = "Acerca de Nexus Chat",
                subtitle = "Versión, stack técnico y autoría",
                icon = Icons.Default.Info,
                // Ruta tipada, no un literal: la pantalla vive en "settings_about" y
                // escribir "about" a mano habría sido otro enlace roto.
                onClick = { navController.navigate(Screen.SettingsAbout.route) }
            )
            SettingsItem(
                title = "Licencia MIT",
                subtitle = "Puedes usar y modificar el código libremente",
                icon = Icons.Default.Description,
                onClick = { open(LICENSE_URL) }
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(16.dp)
    )
}
