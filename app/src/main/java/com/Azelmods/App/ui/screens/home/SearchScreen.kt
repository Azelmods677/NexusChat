package com.Azelmods.App.ui.screens.home

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
                        placeholder = { Text("Search chats, contacts, messages...") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Purple,
                            cursorColor = Purple,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        ),
                        singleLine = true
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
        ) {
            // Tabs
            if (searchQuery.isNotEmpty()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = Purple
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("All") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Chats") }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("Contacts") }
                    )
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Messages") }
                    )
                }
            }
            
            // Content
            when {
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
                                text = "Search for chats, contacts, or messages",
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
                        CircularProgressIndicator(color = Purple)
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
                                text = "No results found",
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "Try a different search term",
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
                        items(filteredResults) { result ->
                            when (result) {
                                is SearchResult.ChatResult -> {
                                    ChatSearchResultItem(
                                        result = result,
                                        onClick = {
                                            navController.navigate(Screen.Chat.createRoute(result.contactId))
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
                .background(Purple.copy(alpha = 0.3f)),
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
                .background(Purple.copy(alpha = 0.3f)),
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
            contentDescription = "Add contact",
            tint = Purple
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
            tint = Purple,
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
            val q = query.trim().lowercase()
            val found = mutableListOf<SearchResult>()
            try {
                val chats = repository.getUserChats(userId).first()
                chats.forEach { data ->
                    val chatId = data["chatId"] as? String ?: return@forEach
                    val members = when {
                        data["members"] is List<*> ->
                            (data["members"] as List<*>).filterIsInstance<String>()
                        data["participants"] is List<*> ->
                            (data["participants"] as List<*>).filterIsInstance<String>()
                        else -> emptyList()
                    }
                    val otherId = members.firstOrNull { it != userId } ?: return@forEach
                    val other = repository.getUserById(otherId)
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
                repository.getAllUsers().first().getOrNull()?.forEach { user ->
                    val uid = user["uid"] as? String ?: return@forEach
                    if (uid == userId) return@forEach
                    val name = (user["displayName"] as? String) ?: (user["name"] as? String) ?: ""
                    val username = user["username"] as? String ?: ""
                    if (name.lowercase().contains(q) || username.lowercase().contains(q)) {
                        if (found.none { it is SearchResult.ContactResult && it.userId == uid }) {
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
                }
                _results.value = found
            } catch (_: Exception) {
                _results.value = emptyList()
            } finally {
                _isSearching.value = false
            }
        }
    }
}
