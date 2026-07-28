package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.Summarize
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.Azelmods.App.ui.theme.DarkSurface

/**
 * Los seis interruptores de funciones de IA.
 *
 * Se muestran siempre, pero desactivados mientras no haya proveedor
 * configurado: esconderlos dejaba al usuario sin saber que existen, y permitir
 * activarlos sin clave produciría funciones que fallan en silencio al usarlas.
 *
 * Mejorar fotos y dictado por voz no necesitan proveedor —ocurren en el propio
 * dispositivo— así que están siempre disponibles.
 */
@Composable
fun AiFeatureToggles(viewModel: AiFeaturesViewModel) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Funciones sobre tus chats",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = if (state.providerReady) {
                "Las que usan IA envían parte de la conversación al proveedor que has configurado. Por eso vienen apagadas."
            } else {
                "Para las que usan IA, configura primero un proveedor y su clave más abajo en esta pantalla."
            },
            color = Color.Gray,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        AiFeatureToggle(
            icon = Icons.Default.QuestionAnswer,
            title = "Respuestas sugeridas",
            description = "Tres respuestas rápidas sobre el teclado cuando te escriben.",
            checked = state.smartReplies,
            enabled = state.providerReady,
            onCheckedChange = viewModel::setSmartReplies
        )
        AiFeatureToggle(
            icon = Icons.Default.Translate,
            title = "Traducir lo que recibo",
            description = "Traduce los mensajes entrantes a tu idioma sin tocar el original.",
            checked = state.autoTranslate,
            enabled = state.providerReady,
            onCheckedChange = viewModel::setAutoTranslate
        )
        AiFeatureToggle(
            icon = Icons.Default.Summarize,
            title = "Resumen de conversación",
            description = "Añade «Resumir chat» al menú de la conversación.",
            checked = state.conversationSummary,
            enabled = state.providerReady,
            onCheckedChange = viewModel::setConversationSummary
        )
        AiFeatureToggle(
            icon = Icons.Default.EditNote,
            title = "Sugerencias de tono",
            description = "Reescribe tu borrador en tono cercano, formal, más corto o más amable.",
            checked = state.toneSuggestions,
            enabled = state.providerReady,
            onCheckedChange = viewModel::setToneSuggestions
        )
        AiFeatureToggle(
            icon = Icons.Default.AutoFixHigh,
            title = "Mejorar fotos al enviar",
            description = "Realce de contraste y color en tu dispositivo. La foto no sale de aquí.",
            checked = state.photoEnhance,
            enabled = true,
            onCheckedChange = viewModel::setPhotoEnhance
        )
        AiFeatureToggle(
            icon = Icons.Default.KeyboardVoice,
            title = "Dictado por voz",
            description = "Botón de micrófono para dictar. Usa el reconocimiento del propio Android.",
            checked = state.voiceTranscription,
            enabled = true,
            onCheckedChange = viewModel::setVoiceTranscription
        )
    }
}

@Composable
private fun AiFeatureToggle(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        color = DarkSurface,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.primary else Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = if (enabled) Color.White else Color.Gray,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled
            )
        }
    }
}
