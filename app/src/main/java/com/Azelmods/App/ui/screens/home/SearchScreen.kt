package com.Azelmods.App.ui.screens.home

import com.Azelmods.App.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.*
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SearchResult {
    data class ChatResult(
        val chatId: String,
        val contactId: String,
        val name: String,
        val lastMessage: String,
        val photoUrl: String? = null
    ) : SearchResult()
    
    data class ContactResult(
        val userId: String,
        val name: String,
        val username: String,
        val photoUrl: String? = null
    ) : SearchResult()
    
    data class MessageResult(
        val chatId: String,
        val chatName: String,
        val messageText: String,
        val timestamp: Long
    ) : SearchResult()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    navController: NavController,
    viewModel: SearchViewModel = hiltViewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val isSearching by viewModel.isSearching.collectAsState()
    val searchResults by viewModel.results.collectAsState()
    val searchError by viewModel.error.collectAsState()
    
    LaunchedEffect(searchQuery) {
        viewModel.search(searchQuery)
    }
    
    val filteredResults = remember(searchResults, selectedTab) {
        when (selectedTab) {
            1 -> searchResults.filterIsInstance<SearchResult.ChatResult>()
            2 -> searchResults.filterIsInstance<SearchResult.ContactResult>()
            3 -> searchResults.filterIsInstance<SearchResult.MessageResult>()
            else -> searchResults
        }
    }
    
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        placeholder = { Text(tr("Buscar chats, contactos y mensajes…")) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = tr("Borrar"))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            cursorColor = MaterialTheme.colorScheme.primary,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        singleLine = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = tr("Atrás"))
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
        ) {
            // Tabs
            if (searchQuery.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text(tr("Todo")) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text(tr("Chats")) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text(tr("Contactos")) }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text(tr("Mensajes")) }
                    )
                }
            }
            
            // Content
            when {
                searchQuery.isNotEmpty() && searchError != null && !isSearching -> {
                    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(text = searchError.orEmpty(), color = Color.Gray, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                searchQuery.isEmpty() -> {
                    // Recent searches or suggestions
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = tr("Busca chats, contactos o mensajes"),
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                isSearching -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                filteredResults.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Color.Gray
                            )
                            Text(
                                text = tr("Sin resultados"),
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Text(
                                text = tr("Prueba con otras palabras"),
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Los chats ocultos tampoco aparecen al buscar.
                        items(
                            filteredResults.filter { result ->
                                result !is SearchResult.ChatResult ||
                                    !com.Azelmods.App.data.chat.ChatOrganizer.isHidden(result.chatId)
                            }
                        ) { result ->
                            when (result) {
                                is SearchResult.ChatResult -> {
                                    ChatSearchResultItem(
                                        result = result,
                                        onClick = {
                                            // Use the real (canonical) chatId, not the peer uid.
                                            navController.navigate(Screen.Chat.createRoute(result.chatId))
                                        }
                                    )
                                }
                                is SearchResult.ContactResult -> {
                                    ContactSearchResultItem(
                                        result = result,
                                        onClick = {
                                            navController.navigate(Screen.Profile.createRoute(result.userId))
                                        }
                                    )
                                }
                                is SearchResult.MessageResult -> {
                                    MessageSearchResultItem(
                                        result = result,
                                        onClick = {
                                            navController.navigate(Screen.Chat.createRoute(result.chatId))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ChatSearchResultItem(
    result: SearchResult.ChatResult,
    onClick: () -> Unit
) {
    com.Azelmods.App.ui.components.ListRowFrame(result.toString(), null) {
        ChatSearchResultItemBody(result, onClick)
    }
}

@Composable
private fun ChatSearchResultItemBody(
    result: SearchResult.ChatResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = result.name.take(1).uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = result.lastMessage,
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ContactSearchResultItem(
    result: SearchResult.ContactResult,
    onClick: () -> Unit
) {
    com.Azelmods.App.ui.components.ListRowFrame(result.toString(), null) {
        ContactSearchResultItemBody(result, onClick)
    }
}

@Composable
private fun ContactSearchResultItemBody(
    result: SearchResult.ContactResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = result.name.take(1).uppercase(),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = result.username,
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        
        Icon(
            Icons.Default.PersonAdd,
            contentDescription = tr("Añadir contacto"),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
fun MessageSearchResultItem(
    result: SearchResult.MessageResult,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.AutoMirrored.Filled.Message,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = result.chatName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = result.messageText,
                color = Color.Gray,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: RealtimeDatabaseRepository
) : ViewModel() {

    private val _results = MutableStateFlow<List<SearchResult>>(emptyList())
    val results: StateFlow<List<SearchResult>> = _results.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private var searchJob: Job? = null

    /** Perfiles por uid. Se descargan una vez por pantalla: antes se pedían con cada letra. */
    private var usersCache: Map<String, Map<String, Any>>? = null

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private suspend fun users(): Map<String, Map<String, Any>> {
        usersCache?.let { return it }
        val result = repository.getAllUsers().first()
        result.exceptionOrNull()?.let { throw it }
        val map = result.getOrNull().orEmpty()
            .mapNotNull { u -> (u["uid"] as? String)?.let { it to u } }
            .toMap()
        usersCache = map
        return map
    }

    fun search(query: String) {
        searchJob?.cancel()
        if (query.length < 2) {
            _results.value = emptyList()
            _isSearching.value = false
            return
        }
        searchJob = viewModelScope.launch {
            _isSearching.value = true
            delay(300)
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
            val q = query.trim().lowercase().removePrefix("@")
            val found = mutableListOf<SearchResult>()
            try {
                val people = users()
                repository.getUserChats(userId).first().forEach { data ->
                    val chatId = data["chatId"] as? String ?: return@forEach
                    val members = when (val m = data["members"]) {
                        is List<*> -> m.filterIsInstance<String>()
                        is Map<*, *> -> m.keys.filterIsInstance<String>()
                        else -> emptyList()
                    }
                    val otherId = members.firstOrNull { it != userId } ?: return@forEach
                    val other = people[otherId]
                    val name = (other?.get("displayName") as? String)
                        ?: (other?.get("username") as? String) ?: ""
                    val lastMessage = data["lastMessage"] as? String ?: ""
                    if (name.lowercase().contains(q) || lastMessage.lowercase().contains(q)) {
                        found.add(
                            SearchResult.ChatResult(
                                chatId = chatId,
                                contactId = otherId,
                                name = name.ifBlank { "Chat" },
                                lastMessage = lastMessage,
                                photoUrl = other?.get("photoUrl") as? String
                            )
                        )
                    }
                }
                people.forEach { (uid, user) ->
                    if (uid == userId) return@forEach
                    val name = (user["displayName"] as? String) ?: (user["name"] as? String) ?: ""
                    val username = user["username"] as? String ?: ""
                    if (name.lowercase().contains(q) || username.lowercase().removePrefix("@").contains(q)) {
                        found.add(
                            SearchResult.ContactResult(
                                userId = uid,
                                name = name.ifBlank { username },
                                username = username,
                                photoUrl = user["photoUrl"] as? String
                            )
                        )
                    }
                }
                _error.value = null
                _results.value = found
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "La búsqueda falló", e)
                _error.value = com.Azelmods.App.utils.friendlyFirebaseError(e)
                _results.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
}
