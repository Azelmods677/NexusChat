package com.Azelmods.App.ui.screens.security

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Security Disclaimer Dialog
 * 
 * Displays legal disclaimer and requires user acceptance before
 * allowing access to advanced security tools.
 * 
 * Requirements: 25.1, 25.2, 25.3, 25.4, 25.5, 39.1, 39.2, 39.3, 39.4, 39.5
 */
@Composable
fun SecurityDisclaimerDialog(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    var hasReadDisclaimer by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDecline,
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Advertencia Legal - Herramientas de Seguridad",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DisclaimerSection(
                    title = "⚠️ USO AUTORIZADO ÚNICAMENTE",
                    content = """
                        Las herramientas de seguridad incluidas en esta aplicación están diseñadas exclusivamente para:
                        
                        • Pruebas de penetración autorizadas
                        • Investigación de seguridad legítima
                        • Educación en ciberseguridad
                        • Auditorías de seguridad con permiso explícito
                    """.trimIndent()
                )
                
                HorizontalDivider()
                
                DisclaimerSection(
                    title = "🚫 PROHIBICIONES",
                    content = """
                        El uso de estas herramientas para:
                        
                        • Acceso no autorizado a sistemas
                        • Distribución de malware
                        • Violación de privacidad
                        • Cualquier actividad ilegal
                        
                        Está ESTRICTAMENTE PROHIBIDO y puede resultar en consecuencias legales graves.
                    """.trimIndent()
                )
                
                HorizontalDivider()
                
                DisclaimerSection(
                    title = "⚖️ RESPONSABILIDAD LEGAL",
                    content = """
                        Al usar estas herramientas, usted acepta que:
                        
                        • Es el único responsable de sus acciones
                        • Cumplirá con todas las leyes aplicables
                        • Obtendrá autorización explícita antes de realizar pruebas
                        • Los desarrolladores NO son responsables del uso indebido
                    """.trimIndent()
                )
                
                HorizontalDivider()
                
                DisclaimerSection(
                    title = "📋 REQUISITOS",
                    content = """
                        Antes de usar estas herramientas, debe:
                        
                        • Tener autorización por escrito del propietario del sistema
                        • Comprender las leyes de ciberseguridad de su jurisdicción
                        • Tener conocimientos técnicos adecuados
                        • Usar únicamente en entornos de prueba controlados
                    """.trimIndent()
                )
                
                HorizontalDivider()
                
                // Checkbox to confirm reading
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = hasReadDisclaimer,
                        onCheckedChange = { hasReadDisclaimer = it }
                    )
                    Text(
                        "He leído y comprendo esta advertencia legal",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onAccept,
                enabled = hasReadDisclaimer,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Acepto y Comprendo")
            }
        },
        dismissButton = {
            TextButton(onClick = onDecline) {
                Text("Rechazar")
            }
        }
    )
}

@Composable
private fun DisclaimerSection(
    title: String,
    content: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Text(
            content,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
