package com.Azelmods.App.ui.screens.conversation
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.data.model.User
import com.Azelmods.App.ui.components.safeClickable
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.navigation.Screen
import kotlinx.coroutines.launch
import com.Azelmods.App.ui.theme.Teal
import com.Azelmods.App.ui.theme.EmeraldGreen
import com.Azelmods.App.ui.theme.NeonGreen
import com.Azelmods.App.ui.theme.ErrorRed
import com.Azelmods.App.ui.theme.DarkBorder
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationScreen(
    navController: NavController,
    viewModel: NewConversationViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var showAddContactSheet by remember { mutableStateOf(false) }
    var showNewGroupSheet by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Surface state.error so tapping a contact never goes without a response.
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(message = error, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Conversation", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = DarkBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    viewModel.searchContacts(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text("Search contacts...", color = Color.Gray) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                shape = RoundedCornerShape(24.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            
            // Quick actions
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickActionCard(
                    icon = Icons.Default.Group,
                    text = "New Group",
                    onClick = { showNewGroupSheet = true }
                )
                
                QuickActionCard(
                    icon = Icons.Default.PersonAdd,
                    text = "Add Contact",
                    onClick = { showAddContactSheet = true }
                )
            }
            
            // Demo Chat - Special card with avatar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .safeClickable { viewModel.createDemoChat(navController) },
                shape = RoundedCornerShape(16.dp),
                color = DarkSurface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bot avatar with animated ring
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🤖", fontSize = 24.sp)
                        }
                    }
                    
                    Spacer(Modifier.width(12.dp))
                    
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Demo Chat",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            "Prueba el chat sin contactos",
                            color = Color.White.copy(0.5f),
                            fontSize = 13.sp
                        )
                    }
                    
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = NeonGreen.copy(0.15f)
                    ) {
                        Text(
                            "DEMO",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = NeonGreen,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            
            // Contacts list
            Text(
                text = "CONTACTS",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            when {
                state.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.filteredContacts.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.searchQuery.isBlank())
                                "No registered users found"
                            else
                                "No results for \"${state.searchQuery}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = state.filteredContacts,
                            key = { contact ->
                                // CAUSA RAÍZ del crash: LazyColumn exige keys únicas; con datos
                                // duplicados de Firebase (mismo uid dos veces) Compose crasheaba
                                // con "Key was already used". La lista ya llega desduplicada
                                // (distinctBy { it.uid } en el ViewModel); este fallback solo
                                // cubre UIDs vacíos por datos corruptos.
                                if (contact.uid.isNotBlank()) {
                                    contact.uid
                                } else {
                                    "contact_${contact.email?.hashCode() ?: contact.hashCode()}"
                                }
                            }
                        ) { contact ->
                            ContactRow(
                                contact = contact,
                                onClick = {
                                    viewModel.startConversation(contact.uid, navController)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    // Add Contact Bottom Sheet
    if (showAddContactSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAddContactSheet = false },
            containerColor = DarkSurface
        ) {
            AddContactSheet(
                onDismiss = { showAddContactSheet = false },
                onSuccess = { contactName ->
                    showAddContactSheet = false
                    // BUG ORIGINAL: el comentario "// Show success snackbar" nunca se implementó,
                    // así que el sheet se cerraba sin ningún feedback y el usuario no sabía
                    // si la acción funcionó. Ahora se confirma vía snackbar mientras
                    // startConversation resuelve la navegación al chat en segundo plano.
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "Contacto encontrado: $contactName — abriendo chat…",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                viewModel = viewModel,
                navController = navController
            )
        }
    }
    
    // New Group Bottom Sheet
    if (showNewGroupSheet) {
        ModalBottomSheet(
            onDismissRequest = { showNewGroupSheet = false },
            containerColor = DarkSurface
        ) {
            NewGroupSheet(
                contacts = state.contacts,
                onDismiss = { showNewGroupSheet = false },
                onGroupCreated = { groupId ->
                    showNewGroupSheet = false
                    // BUG ORIGINAL: el grupo se creaba en Firebase pero el comentario
                    // "// Navigate to group chat" nunca se implementó: el usuario quedaba
                    // en la misma pantalla sin saber si el grupo existía. Los grupos viven
                    // en /chats/{groupId}, así que la ruta de chat normal los abre.
                    navController.navigate(Screen.Chat.createRoute(groupId))
                },
                viewModel = viewModel
            )
        }
    }
}

@Composable
fun RowScope.QuickActionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .height(80.dp)
            .weight(1f)
            .safeClickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun ContactRow(
    contact: User,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .safeClickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            UserAvatar(
                name = contact.name,
                photoUrl = contact.photoUrl,
                size = 48.dp
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = contact.username,
                    color = Teal,
                    fontSize = 13.sp
                )
            }
            
            if (contact.isOnline) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(EmeraldGreen, CircleShape)
                )
            }
        }
    }
}

@Composable
fun AddContactSheet(
    onDismiss: () -> Unit,
    onSuccess: (contactName: String) -> Unit,
    viewModel: NewConversationViewModel,
    navController: NavController
) {
    val scope = rememberCoroutineScope()
    var username by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var isSending by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = "Add Contact",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = username,
            onValueChange = { 
                username = it
                errorMessage = null
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username", color = Color.Gray) },
            placeholder = { Text("@username", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.AlternateEmail, null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = DarkBorder
            ),
            isError = errorMessage != null
        )
        
        errorMessage?.let { error ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = error,
                color = ErrorRed,
                fontSize = 12.sp
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { 
                    navController.navigate("qr_scanner")
                },
                modifier = Modifier.weight(1f),
                enabled = true,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = BorderStroke(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary))
                )
            ) {
                Icon(Icons.Default.QrCodeScanner, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Scan QR")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                scope.launch {
                    isSearching = true
                    val user = viewModel.searchUserByUsername(username)
                    isSearching = false
                    
                    if (user != null) {
                        val userId = user["uid"] as? String ?: return@launch
                        val name = user["name"] as? String ?: username
                        viewModel.startConversation(userId, navController)
                        onSuccess(name)
                    } else {
                        errorMessage = "Could not find user. Check the username."
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            enabled = username.isNotBlank() && !isSearching && !isSending
        ) {
            if (isSearching || isSending) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isSearching) "Searching..." else "Starting...")
            } else {
                Text("Start Chat")
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun NewGroupSheet(
    contacts: List<User>,
    onDismiss: () -> Unit,
    onGroupCreated: (groupId: String) -> Unit,
    viewModel: NewConversationViewModel
) {
    val scope = rememberCoroutineScope()
    var selectedContacts by remember { mutableStateOf(setOf<String>()) }
    var groupName by remember { mutableStateOf("") }
    var showNameInput by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        if (!showNameInput) {
            Text(
                text = "New Group",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Select at least 2 contacts",
                color = Color.Gray,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            ) {
                items(
                    items = contacts,
                    key = { contact ->
                        // Misma causa raíz que la lista de contactos: keys duplicadas
                        // crasheaban esta LazyColumn. Lista desduplicada en el ViewModel
                        // (distinctBy { it.uid }); fallback solo para UIDs vacíos.
                        if (contact.uid.isNotBlank()) {
                            contact.uid
                        } else {
                            "group_contact_${contact.email?.hashCode() ?: contact.hashCode()}"
                        }
                    }
                ) { contact ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .safeClickable {
                                selectedContacts = if (selectedContacts.contains(contact.uid)) {
                                    selectedContacts - contact.uid
                                } else {
                                    selectedContacts + contact.uid
                                }
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = selectedContacts.contains(contact.uid),
                            onCheckedChange = null,
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = contact.name,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { showNameInput = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = selectedContacts.size >= 2,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Next")
            }
        } else {
            Text(
                text = "Group Name",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = groupName,
                onValueChange = { groupName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Enter group name", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = DarkBorder
                )
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    isCreating = true
                    scope.launch {
                        val groupId = viewModel.createGroup(groupName, selectedContacts.toList())
                        isCreating = false
                        if (groupId != null) {
                            onGroupCreated(groupId)
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = groupName.isNotBlank() && !isCreating,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                if (isCreating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Create Group")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
