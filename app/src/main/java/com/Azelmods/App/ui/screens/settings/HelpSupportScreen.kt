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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpSupportScreen(
    navController: NavController
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Help & Support") },
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
        ) {
            Text(
                text = "Get Help",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "FAQ - Preguntas Frecuentes",
                subtitle = "Respuestas rápidas a dudas comunes",
                icon = Icons.Default.QuestionAnswer,
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply { 
                        data = android.net.Uri.parse("https://azelmods.com/nexuschat/faq") 
                    }
                    try { 
                        navController.context.startActivity(intent) 
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening FAQ", e)
                    }
                }
            )
            
            SettingsItem(
                title = "Contactar Soporte Técnico",
                subtitle = "support@azelmods.com | Respuesta en 24-48h",
                icon = Icons.Default.Support,
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply { 
                        data = android.net.Uri.parse("mailto:support@azelmods.com?subject=NexusChat - Soporte Técnico&body=Describe tu problema o consulta aquí...")
                    }
                    try { 
                        navController.context.startActivity(intent) 
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening email", e)
                    } 
                }
            )
            
            SettingsItem(
                title = "Reportar un Error",
                subtitle = "Ayúdanos a mejorar reportando bugs",
                icon = Icons.Default.BugReport,
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply { 
                        data = android.net.Uri.parse("mailto:bugs@azelmods.com?subject=NexusChat - Reporte de Error&body=Versión de la app: [auto]%0A%0ADescribe el error:%0A%0APasos para reproducir:%0A1. %0A2. %0A3. %0A%0AComportamiento esperado:%0A")
                    }
                    try { 
                        navController.context.startActivity(intent) 
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening bug report", e)
                    }
                }
            )
            
            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Resources",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "Guía de Usuario",
                subtitle = "Manual completo de NexusChat",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply { 
                        data = android.net.Uri.parse("https://docs.azelmods.com/nexuschat") 
                    }
                    try { 
                        navController.context.startActivity(intent) 
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening guide", e)
                    }
                }
            )
            
            SettingsItem(
                title = "Comunidad y Foro",
                subtitle = "Únete a nuestra comunidad oficial",
                icon = Icons.Default.Forum,
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply { 
                        data = android.net.Uri.parse("https://community.azelmods.com") 
                    }
                    try { 
                        navController.context.startActivity(intent) 
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening community", e)
                    }
                }
            )
            
            SettingsItem(
                title = "Solicitar Funciones",
                subtitle = "Sugiere mejoras para NexusChat",
                icon = Icons.Default.Lightbulb,
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply { 
                        data = android.net.Uri.parse("https://feedback.azelmods.com/nexuschat") 
                    }
                    try { 
                        navController.context.startActivity(intent) 
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening feedback", e)
                    }
                }
            )
            
            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Guides & Tutorials",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "🚀 Primeros Pasos",
                subtitle = "Aprende a usar Nexus Chat",
                icon = Icons.Default.School,
                onClick = { navController.navigate("tutorial/getting_started") }
            )
            
            SettingsItem(
                title = "💬 Mensajería",
                subtitle = "Enviar mensajes y multimedia",
                icon = Icons.AutoMirrored.Filled.Message,
                onClick = { navController.navigate("tutorial/messaging") }
            )
            
            SettingsItem(
                title = "📸 Historias",
                subtitle = "Crear y ver historias",
                icon = Icons.Default.PhotoLibrary,
                onClick = { navController.navigate("tutorial/stories") }
            )
            
            SettingsItem(
                title = "🤖 Funciones de IA",
                subtitle = "Usar Azel IA y asistentes",
                icon = Icons.Default.Psychology,
                onClick = { navController.navigate("tutorial/ai_features") }
            )
            
            SettingsItem(
                title = "🎨 Apariencia",
                subtitle = "Personalizar temas y fondos",
                icon = Icons.Default.Palette,
                onClick = { navController.navigate("tutorial/appearance") }
            )
            
            SettingsItem(
                title = "🔒 Privacidad",
                subtitle = "Configurar privacidad y seguridad",
                icon = Icons.Default.Security,
                onClick = { navController.navigate("tutorial/privacy") }
            )
            
            SettingsItem(
                title = "⚙️ Herramientas Avanzadas",
                subtitle = "Terminal y CyberSec",
                icon = Icons.Default.DeveloperMode,
                onClick = { navController.navigate("tutorial/framework") }
            )
            
            SettingsItem(
                title = "👆 Gestos Táctiles",
                subtitle = "Navegación por gestos y atajos",
                icon = Icons.Default.TouchApp,
                onClick = { navController.navigate("tutorial/gestures") }
            )
            
            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Acerca de Azel Mods",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "Sobre Azel Mods",
                subtitle = "Desarrollador independiente especializado en apps seguras",
                icon = Icons.Default.Info,
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply { 
                        data = android.net.Uri.parse("https://azelmods.com/about") 
                    }
                    try { 
                        navController.context.startActivity(intent) 
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening about", e)
                    }
                }
            )
            
            SettingsItem(
                title = "Compromiso de Privacidad",
                subtitle = "Cifrado end-to-end y cero acceso a tus datos",
                icon = Icons.Default.Security,
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply { 
                        data = android.net.Uri.parse("https://azelmods.com/privacy-policy") 
                    }
                    try { 
                        navController.context.startActivity(intent) 
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening privacy", e)
                    }
                }
            )
            
            SettingsItem(
                title = "Términos y Condiciones",
                subtitle = "Condiciones de uso de NexusChat",
                icon = Icons.Default.Description,
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://azelmods.com/terms-of-service")
                    }
                    try {
                        navController.context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening terms", e)
                    }
                }
            )
            
            SettingsItem(
                title = "Licencias Open Source",
                subtitle = "Componentes de código abierto utilizados",
                icon = Icons.Default.Code,
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://azelmods.com/licenses")
                    }
                    try {
                        navController.context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening licenses", e)
                    }
                }
            )
            
            SettingsItem(
                title = "Sitio Web Oficial",
                subtitle = "www.azelmods.com",
                icon = Icons.Default.Language,
                onClick = {
                    val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                        data = android.net.Uri.parse("https://www.azelmods.com")
                    }
                    try {
                        navController.context.startActivity(intent)
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error opening website", e)
                    }
                }
            )
            
            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Feedback",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "Send Feedback",
                subtitle = "Share your thoughts",
                icon = Icons.Default.Feedback,
                onClick = { 
                    val intent = android.content.Intent(android.content.Intent.ACTION_SENDTO).apply { 
                        data = android.net.Uri.parse("mailto:support@azelmods.com?subject=NexusChat Feedback") 
                    }
                    try { 
                        navController.context.startActivity(intent) 
                    } catch (e: Exception) {
                        android.util.Log.e("HelpSupport", "Error sending feedback", e)
                    }
                }
            )
        }
    }
}
