package com.Azelmods.App.ui.screens.bot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.Azelmods.App.data.repository.InternalBotRepository
import com.Azelmods.App.ui.theme.rememberThemeColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * InternalBotScreen - Bot features with DataStore toggles
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InternalBotScreen(
    navController: NavController,
    viewModel: InternalBotViewModel = hiltViewModel()
) {
    val themeColor = rememberThemeColor()
    val state by viewModel.state.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bot Interno") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F0F0F),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color(0xFF0A0A0A)
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Auto Respuesta",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            item {
                BotToggleCard(
                    title = "Auto respuesta privado",
                    checked = state.autoReplyPrivate,
                    onCheckedChange = { viewModel.setAutoReplyPrivate(it) },
                    themeColor = themeColor
                ) {
                    OutlinedTextField(
                        value = state.autoReplyPrivateMessage,
                        onValueChange = { viewModel.setAutoReplyPrivateMessage(it) },
                        label = { Text("Mensaje personalizado") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            focusedLabelColor = themeColor,
                            cursorColor = themeColor
                        )
                    )
                }
            }
            
            item {
                BotToggleCard(
                    title = "Auto respuesta grupos",
                    checked = state.autoReplyGroups,
                    onCheckedChange = { viewModel.setAutoReplyGroups(it) },
                    themeColor = themeColor
                ) {
                    OutlinedTextField(
                        value = state.autoReplyGroupsMessage,
                        onValueChange = { viewModel.setAutoReplyGroupsMessage(it) },
                        label = { Text("Mensaje personalizado") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = themeColor,
                            focusedLabelColor = themeColor,
                            cursorColor = themeColor
                        )
                    )
                }
            }
            
            item {
                Text(
                    text = "Privacidad",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            item {
                BotToggleCard(
                    title = "Modo Fantasma",
                    subtitle = "Oculta última vez, en línea y confirmaciones de lectura",
                    checked = state.ghostMode,
                    onCheckedChange = { viewModel.setGhostMode(it) },
                    themeColor = themeColor
                )
            }
            
            item {
                Text(
                    text = "Mensajería",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            
            item {
                BotToggleCard(
                    title = "Mensajes en masa",
                    subtitle = "Enviar mensaje a múltiples contactos",
                    checked = state.massMessages,
                    onCheckedChange = { viewModel.setMassMessages(it) },
                    themeColor = themeColor
                )
            }
            
            item {
                BotToggleCard(
                    title = "Traductor entrada",
                    checked = state.translator,
                    onCheckedChange = { viewModel.setTranslator(it) },
                    themeColor = themeColor
                ) {
                    // Language selector placeholder
                    Text(
                        text = "Idioma: ${state.translatorLanguage}",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
            
            item {
                BotToggleCard(
                    title = "Quoted personalizado",
                    subtitle = "Personalizar estilo de respuestas",
                    checked = state.customQuoted,
                    onCheckedChange = { viewModel.setCustomQuoted(it) },
                    themeColor = themeColor
                )
            }
            
            item {
                BotToggleCard(
                    title = "Mencionar todos",
                    subtitle = "Botón para mencionar a todos en grupos",
                    checked = state.mentionAll,
                    onCheckedChange = { viewModel.setMentionAll(it) },
                    themeColor = themeColor
                )
            }
        }
    }
}

@Composable
private fun BotToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    themeColor: Color,
    subtitle: String? = null,
    content: (@Composable () -> Unit)? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF111111)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = themeColor,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color(0xFF333333)
                    )
                )
            }
            
            if (content != null && checked) {
                Spacer(modifier = Modifier.height(12.dp))
                content()
            }
        }
    }
}

data class InternalBotState(
    val autoReplyPrivate: Boolean = false,
    val autoReplyPrivateMessage: String = "",
    val autoReplyGroups: Boolean = false,
    val autoReplyGroupsMessage: String = "",
    val ghostMode: Boolean = false,
    val massMessages: Boolean = false,
    val translator: Boolean = false,
    val translatorLanguage: String = "es",
    val customQuoted: Boolean = false,
    val mentionAll: Boolean = false
)

@HiltViewModel
class InternalBotViewModel @Inject constructor(
    private val repository: InternalBotRepository
) : ViewModel() {
    
    private val _state = MutableStateFlow(InternalBotState())
    val state: StateFlow<InternalBotState> = _state.asStateFlow()
    
    init {
        loadSettings()
    }
    
    private fun loadSettings() {
        viewModelScope.launch {
            repository.getAutoReplyPrivate().collect { enabled ->
                _state.value = _state.value.copy(autoReplyPrivate = enabled)
            }
        }
        viewModelScope.launch {
            repository.getAutoReplyPrivateMessage().collect { message ->
                _state.value = _state.value.copy(autoReplyPrivateMessage = message)
            }
        }
        viewModelScope.launch {
            repository.getAutoReplyGroups().collect { enabled ->
                _state.value = _state.value.copy(autoReplyGroups = enabled)
            }
        }
        viewModelScope.launch {
            repository.getAutoReplyGroupsMessage().collect { message ->
                _state.value = _state.value.copy(autoReplyGroupsMessage = message)
            }
        }
        viewModelScope.launch {
            repository.getGhostMode().collect { enabled ->
                _state.value = _state.value.copy(ghostMode = enabled)
            }
        }
        viewModelScope.launch {
            repository.getMassMessages().collect { enabled ->
                _state.value = _state.value.copy(massMessages = enabled)
            }
        }
        viewModelScope.launch {
            repository.getTranslator().collect { enabled ->
                _state.value = _state.value.copy(translator = enabled)
            }
        }
        viewModelScope.launch {
            repository.getTranslatorLanguage().collect { language ->
                _state.value = _state.value.copy(translatorLanguage = language)
            }
        }
        viewModelScope.launch {
            repository.getCustomQuoted().collect { enabled ->
                _state.value = _state.value.copy(customQuoted = enabled)
            }
        }
        viewModelScope.launch {
            repository.getMentionAll().collect { enabled ->
                _state.value = _state.value.copy(mentionAll = enabled)
            }
        }
    }
    
    fun setAutoReplyPrivate(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoReplyPrivate(enabled)
        }
    }
    
    fun setAutoReplyPrivateMessage(message: String) {
        _state.value = _state.value.copy(autoReplyPrivateMessage = message)
        viewModelScope.launch {
            repository.setAutoReplyPrivateMessage(message)
        }
    }
    
    fun setAutoReplyGroups(enabled: Boolean) {
        viewModelScope.launch {
            repository.setAutoReplyGroups(enabled)
        }
    }
    
    fun setAutoReplyGroupsMessage(message: String) {
        _state.value = _state.value.copy(autoReplyGroupsMessage = message)
        viewModelScope.launch {
            repository.setAutoReplyGroupsMessage(message)
        }
    }
    
    fun setGhostMode(enabled: Boolean) {
        viewModelScope.launch {
            repository.setGhostMode(enabled)
        }
    }
    
    fun setMassMessages(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMassMessages(enabled)
        }
    }
    
    fun setTranslator(enabled: Boolean) {
        viewModelScope.launch {
            repository.setTranslator(enabled)
        }
    }
    
    fun setCustomQuoted(enabled: Boolean) {
        viewModelScope.launch {
            repository.setCustomQuoted(enabled)
        }
    }
    
    fun setMentionAll(enabled: Boolean) {
        viewModelScope.launch {
            repository.setMentionAll(enabled)
        }
    }
}
