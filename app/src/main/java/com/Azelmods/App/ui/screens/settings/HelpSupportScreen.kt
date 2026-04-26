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
                color = Purple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "FAQ",
                subtitle = "Frequently asked questions",
                icon = Icons.Default.QuestionAnswer,
                onClick = { /* TODO: FAQ screen */ }
            )
            
            SettingsItem(
                title = "Contact Support",
                subtitle = "Get help from our team",
                icon = Icons.Default.Support,
                onClick = { /* TODO: Contact support */ }
            )
            
            SettingsItem(
                title = "Report a Problem",
                subtitle = "Let us know about issues",
                icon = Icons.Default.BugReport,
                onClick = { /* TODO: Report problem */ }
            )
            
            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Resources",
                color = Purple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "User Guide",
                subtitle = "Learn how to use Nexus Chat",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                onClick = { /* TODO: User guide */ }
            )
            
            SettingsItem(
                title = "Community",
                subtitle = "Join our community forum",
                icon = Icons.Default.Forum,
                onClick = { /* TODO: Community */ }
            )
            
            SettingsItem(
                title = "Feature Requests",
                subtitle = "Suggest new features",
                icon = Icons.Default.Lightbulb,
                onClick = { /* TODO: Feature requests */ }
            )
            
            HorizontalDivider(color = DarkSurface, modifier = Modifier.padding(vertical = 8.dp))
            
            Text(
                text = "Guides & Tutorials",
                color = Purple,
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
                icon = Icons.Default.Message,
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
                text = "Feedback",
                color = Purple,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )
            
            SettingsItem(
                title = "Send Feedback",
                subtitle = "Share your thoughts",
                icon = Icons.Default.Feedback,
                onClick = { /* TODO: Send feedback */ }
            )
        }
    }
}
