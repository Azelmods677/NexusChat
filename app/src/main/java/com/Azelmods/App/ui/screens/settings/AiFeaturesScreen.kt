package com.Azelmods.App.ui.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiFeaturesScreen(
    navController: NavController,
    aiKeyViewModel: AiKeyViewModel = hiltViewModel(),
    aiFeaturesViewModel: AiFeaturesViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "AI Features",
                        fontWeight = FontWeight.Bold
                    )
                },
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
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(MaterialTheme.colorScheme.primary, Teal)
                        )
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "AI-Powered Features",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Enhance your messaging experience",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // Los seis interruptores. Fueron decorativos durante varias versiones
            // (estado local con `remember`, sin persistencia ni efecto) y por eso
            // se habían retirado de la interfaz. En la v6 tienen implementación
            // real detrás y persisten en DataStore vía AiFeaturePreferences.
            AiFeatureToggles(viewModel = aiFeaturesViewModel)

            // 🔑 API Key de Gemini
            GeminiApiKeySection(viewModel = aiKeyViewModel)
            
            // Azel IA Access Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                color = DarkSurface,
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Azel IA",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Asistente de IA para programación, redacción y análisis",
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Text(
                        text = "• Asistencia de programación y revisión de código\n• Redacción, resumen y traducción de textos\n• Explicación de conceptos y respuestas a preguntas\n• Buenas prácticas de seguridad defensiva\n• Análisis y organización de información",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { navController.navigate("azel_ai") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Acceder a Azel IA", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            // NOTA(assistant-falso): aquí había un "AI Assistant" con un campo de texto
            // que NO llamaba a ningún modelo: devolvía frases fijas según palabras clave
            // ("hello", "help", "feature") y, si no casaba ninguna, repetía la pregunta
            // del usuario. Simular una IA que no existe engaña sobre lo que hace la app.
            // El asistente REAL es Azel IA (botón de arriba), que usa el proveedor,
            // modelo y clave configurados en esta misma pantalla.

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

/**
 * 🔑 SECCIÓN "API KEY DE GEMINI"
 *
 * Permite al usuario introducir, guardar y borrar su propia API key de Gemini,
 * almacenada de forma cifrada por [com.Azelmods.App.data.ai.AiKeyStore].
 * Incluye toggle mostrar/ocultar, validación mínima (no vacía), indicador de
 * estado (activa/inactiva) y mensaje de confirmación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiApiKeySection(
    viewModel: AiKeyViewModel
) {
    val hasKey by viewModel.hasKey.collectAsState()
    val feedback by viewModel.feedback.collectAsState()
    val provider by viewModel.provider.collectAsState()
    val remoteModels by viewModel.remoteModels.collectAsState()
    val isLoadingModels by viewModel.isLoadingModels.collectAsState()

    var keyInput by remember { mutableStateOf("") }
    var showKey by remember { mutableStateOf(false) }
    var providerMenuOpen by remember { mutableStateOf(false) }

    // Se reinician al cambiar de proveedor para mostrar los valores del preset nuevo
    // en vez de los del anterior.
    var baseUrlInput by remember(provider) { mutableStateOf(viewModel.currentBaseUrl()) }
    var modelInput by remember(provider) { mutableStateOf(viewModel.currentModel()) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        color = DarkSurface,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Proveedor de IA",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tú eliges el proveedor y el modelo. La clave se guarda cifrada en este dispositivo.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Selector de proveedor ────────────────────────────────────────
            // DropdownMenu plano en vez de ExposedDropdownMenuBox: este último exige
            // MenuAnchorType (Material3 1.3+) y aquí no se puede verificar la versión
            // compilando. DropdownMenu es estable desde 1.0 y hace lo mismo.
            Box(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { providerMenuOpen = true },
                    color = Color.Transparent,
                    shape = MaterialTheme.shapes.small,
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Proveedor", color = Color.Gray, fontSize = 11.sp)
                            Text(
                                text = provider.displayName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = "Elegir proveedor",
                            tint = Color.Gray
                        )
                    }
                }
                DropdownMenu(
                    expanded = providerMenuOpen,
                    onDismissRequest = { providerMenuOpen = false }
                ) {
                    viewModel.availableProviders.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                providerMenuOpen = false
                                viewModel.selectProvider(option)
                            }
                        )
                    }
                }
            }

            if (provider.hint.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = provider.hint, color = Color.Gray, fontSize = 12.sp)
            }

            // ── Endpoint: solo aplica a los proveedores OpenAI-compatible ────
            // Gemini tiene su propia ruta fija, así que mostrar estos campos ahí
            // solo confundiría.
            if (provider.isOpenAiCompatible) {
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = baseUrlInput,
                    onValueChange = { baseUrlInput = it },
                    label = { Text("URL base (…/v1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = modelInput,
                    onValueChange = { modelInput = it },
                    label = { Text("Modelo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )

                // ── Catálogo de modelos ──────────────────────────────────────
                // Sugerencias del preset + lista REAL del proveedor bajo demanda.
                // Así un modelo recién publicado se puede elegir sin actualizar la app.
                ModelCatalog(
                    suggested = viewModel.suggestedModels(),
                    remote = remoteModels,
                    isLoading = isLoadingModels,
                    selected = modelInput,
                    onPick = { picked ->
                        modelInput = picked
                        viewModel.selectModel(picked)
                    },
                    onFetch = {
                        // Se guarda antes la URL para consultar el endpoint correcto.
                        viewModel.saveEndpoint(baseUrlInput, modelInput)
                        viewModel.loadProviderModels()
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.saveEndpoint(baseUrlInput, modelInput) }) {
                    Text("Guardar proveedor y modelo")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Estado actual de la clave
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (hasKey) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (hasKey) Teal else Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        hasKey -> "API Key activa"
                        // Un servidor local no necesita clave: decir "sin API key" ahí
                        // sugeriría un problema que no existe.
                        provider.allowsEmptyKey -> "Sin clave — ${provider.displayName} no la necesita"
                        else -> "Sin API Key configurada"
                    },
                    color = if (hasKey || provider.allowsEmptyKey) Teal else Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                singleLine = true,
                placeholder = { Text("Pega aquí tu API key…") },
                label = { Text("API Key") },
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                trailingIcon = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(
                            imageVector = if (showKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (showKey) "Ocultar" else "Mostrar"
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.saveApiKey(keyInput)
                        keyInput = ""
                        showKey = false
                    },
                    enabled = keyInput.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { viewModel.clearApiKey() },
                    enabled = hasKey,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Borrar")
                }
            }

            // Mensaje de confirmación / validación
            feedback?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = message,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }
    }
}

/**
 * Catálogo de modelos del proveedor activo.
 *
 * Combina dos fuentes deliberadamente:
 *  - **Sugeridos**: atajos del preset, disponibles sin red.
 *  - **Del proveedor**: lo que responde `GET /models`, es decir el catálogo REAL y
 *    actual. Por eso no hace falta publicar una versión de la app cada vez que un
 *    proveedor saca un modelo nuevo.
 *
 * El campo "Modelo" sigue siendo texto libre: esto son atajos, no una lista cerrada.
 */
@Composable
private fun ModelCatalog(
    suggested: List<String>,
    remote: List<String>,
    isLoading: Boolean,
    selected: String,
    onPick: (String) -> Unit,
    onFetch: () -> Unit
) {
    Spacer(modifier = Modifier.height(12.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "Modelos",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            TextButton(onClick = onFetch) {
                Icon(
                    Icons.Default.CloudDownload,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Buscar modelos", fontSize = 13.sp)
            }
        }
    }

    if (suggested.isNotEmpty() && remote.isEmpty()) {
        Text(
            text = "Sugeridos — o pulsa «Buscar modelos» para ver el catálogo actual del proveedor.",
            color = Color.Gray,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        ModelChips(models = suggested, selected = selected, onPick = onPick)
    }

    if (remote.isNotEmpty()) {
        Text(
            text = "Disponibles en tu cuenta (${remote.size})",
            color = Color.Gray,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Se limita la altura: algunos proveedores devuelven cientos de modelos.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 220.dp)
                .verticalScroll(rememberScrollState())
        ) {
            ModelChips(models = remote, selected = selected, onPick = onPick)
        }
    }
}

/** Fila envolvente de chips seleccionables con el id de cada modelo. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ModelChips(
    models: List<String>,
    selected: String,
    onPick: (String) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        models.forEach { model ->
            val isSelected = model.equals(selected.trim(), ignoreCase = true)
            FilterChip(
                selected = isSelected,
                onClick = { onPick(model) },
                label = { Text(model, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = Color.White,
                    labelColor = Color.White.copy(alpha = 0.75f)
                )
            )
        }
    }
}

