package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.Azelmods.App.i18n.tr
import com.Azelmods.App.ui.components.NexusTopBar
import com.Azelmods.App.ui.theme.NexusTokens

/** Qué datos usa la app y para qué, en palabras claras. */
private val POLICY = listOf(
    "Qué datos guardamos" to "Tu nombre, usuario, foto y el correo o teléfono con el que te registras. Tus mensajes, llamadas e historias pasan por nuestros servidores (Firebase, de Google) para llegar a su destino.",
    "Cifrado" to "Los chats de dos personas, los grupos y los chats secretos se cifran de extremo a extremo: el servidor guarda el contenido cifrado y no puede leerlo. La clave privada vive solo en tu teléfono.",
    "Qué no hacemos" to "No mostramos anuncios, no vendemos tus datos y no los compartimos con terceros para publicidad.",
    "Permisos del teléfono" to "Cámara, micrófono, fotos, contactos y ubicación se usan solo cuando tú inicias la función que los necesita: enviar una foto, llamar, compartir un contacto o tu ubicación.",
    "Inteligencia artificial" to "Las funciones de IA usan la clave del proveedor que tú configures. Lo que les envías va directamente a ese proveedor y se rige por su política.",
    "Mensajes temporales y borrado" to "Los mensajes temporales y las historias se borran del servidor al caducar. Puedes vaciar un chat o borrar mensajes cuando quieras.",
    "Eliminar tu cuenta" to "Desde Ajustes, Cuenta, puedes eliminar tu cuenta. Se borran tu perfil y tus datos del servidor.",
    "Reportes y seguridad" to "Puedes bloquear y reportar a cualquier usuario desde el menú de su chat. Los reportes se revisan para mantener la comunidad segura.",
    "Contacto" to "Para cualquier duda sobre tus datos, escribe al desarrollador por el canal de Telegram de Azel Mods, enlazado en Acerca de."
)

@Composable
fun PrivacyPolicyScreen(navController: NavController) {
    Scaffold(
        topBar = { NexusTopBar(title = tr("Política de privacidad"), onBack = { navController.popBackStack() }) },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            POLICY.forEach { (title, body) ->
                Text(tr(title), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    tr(body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NexusTokens.Color.TextSecondary,
                    modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
                )
            }
        }
    }
}
