package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.DarkDeep
import com.Azelmods.App.ui.theme.DarkSurface

/**
 * Pantalla de tamaño de fuente.
 *
 * ANTES era código muerto: escribía en SharedPreferences "nexus_prefs" con claves
 * "Small/Medium/Large/XLarge", pero el tema lee de [UserPreferences] (fichero
 * "nexus_chat_preferences") y su tabla de escalas esperaba "EXTRA_LARGE". Así que
 * cambiar el tamaño NO tenía ningún efecto en la app.
 *
 * Ahora persiste a través de [SettingsViewModel] → [UserPreferences.setFontSize], la
 * MISMA fuente que consume `NexusChatTheme`, de modo que el cambio recompone el árbol
 * al instante y afecta a toda la app. Los valores guardados casan con
 * `getTypographyForSize`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FontSizeScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val selectedSize by viewModel.fontSize.collectAsState()

    // (etiqueta visible, clave persistida, tamaño de muestra)
    val sizes = listOf(
        Triple("Pequeño", "Small", 13.sp),
        Triple("Normal", "Medium", 15.sp),
        Triple("Grande", "Large", 17.sp),
        Triple("Muy Grande", "ExtraLarge", 19.sp)
    )

    // Tolerante a valores heredados ("XLarge", "Mediano", etc.) para marcar bien la fila.
    fun normalize(v: String) = v.uppercase().replace(" ", "").replace("_", "")
    val selectedNorm = normalize(selectedSize).let { if (it == "XLARGE" || it == "EXTRAGRANDE" || it == "MUYGRANDE") "EXTRALARGE" else it }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Tamaño de Fuente",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            tint = Color.White,
                            contentDescription = null
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkDeep
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Preview
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Vista Previa",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Hola! ¿Cómo estás?",
                        color = Color.White,
                        fontSize = when (selectedNorm) {
                            "SMALL" -> 13.sp
                            "LARGE" -> 17.sp
                            "EXTRALARGE" -> 19.sp
                            else -> 15.sp
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            sizes.forEach { (label, key, size) ->
                val isSelected = selectedNorm == normalize(key)
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setFontSize(key) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primary.copy(0.2f)
                    else
                        DarkSurface,
                    border = BorderStroke(
                        if (isSelected) 1.dp else 0.dp,
                        MaterialTheme.colorScheme.primary
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            label,
                            color = Color.White,
                            fontSize = size,
                            modifier = Modifier.weight(1f)
                        )
                        if (isSelected) {
                            Icon(
                                Icons.Default.Check,
                                tint = MaterialTheme.colorScheme.primary,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}
