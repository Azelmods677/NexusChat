package com.Azelmods.App.ui.screens.conversation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.Azelmods.App.data.chat.ChatId
import com.Azelmods.App.data.model.User
import com.Azelmods.App.data.repository.RealtimeDatabaseRepository
import com.Azelmods.App.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class NewConversationState(
    val contacts: List<User> = emptyList(),
    val filteredContacts: List<User> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isCreatingGroup: Boolean = false,
    val isSearching: Boolean = false,
    val isSendingRequest: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class NewConversationViewModel @Inject constructor(
    private val databaseRepository: RealtimeDatabaseRepository,
    private val demoAccountManager: com.Azelmods.App.data.demo.DemoAccountManager
) : ViewModel() {

    private val _state = MutableStateFlow(NewConversationState())
    val state: StateFlow<NewConversationState> = _state.asStateFlow()

    init {
        // ✅ VERIFICAR AUTENTICACIÓN ANTES DE CARGAR
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            android.util.Log.d("NewConversation", "User authenticated: ${currentUser.uid}")
            loadContacts()
        } else {
            android.util.Log.e("NewConversation", "User NOT authenticated - cannot load contacts")
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Please sign in to view contacts"
            )
        }
    }

    private fun loadContacts() {
        // ✅ DOBLE VERIFICACIÓN DE AUTENTICACIÓN
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            android.util.Log.e("NewConversation", "Cannot load contacts: user not authenticated")
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Authentication required"
            )
            return
        }
        
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            databaseRepository.getAllUsers().collect { result ->
                result.onSuccess { usersData ->
                    val users = usersData.mapNotNull { userData ->
                        try {
                            User(
                                uid = userData["uid"] as? String ?: "",
                                name = userData["name"] as? String ?: userData["displayName"] as? String ?: "",
                                displayName = userData["name"] as? String ?: userData["displayName"] as? String ?: "",
                                username = userData["username"] as? String ?: "",
                                email = userData["email"] as? String ?: "",
                                phone = userData["phone"] as? String ?: "",
                                photoUrl = userData["photoUrl"] as? String,
                                coverUrl = userData["coverUrl"] as? String,
                                bio = userData["bio"] as? String ?: "",
                                status = userData["status"] as? String ?: "Hey there! I'm using Nexus Chat",
                                isOnline = userData["isOnline"] as? Boolean ?: false,
                                lastSeen = (userData["lastSeen"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                isPremium = userData["isPremium"] as? Boolean ?: false,
                                createdAt = (userData["createdAt"] as? Number)?.toLong() ?: System.currentTimeMillis(),
                                fcmToken = userData["fcmToken"] as? String,
                                messageCount = (userData["messageCount"] as? Number)?.toInt() ?: 0,
                                filesShared = (userData["filesShared"] as? Number)?.toInt() ?: 0
                            )
                        } catch (e: Exception) {
                            android.util.Log.e("NewConversation", "Error parsing user: ${e.message}", e)
                            null
                        }
                    }

                    android.util.Log.d("NewConversation", "Loaded ${users.size} users")
                    _state.value = _state.value.copy(
                        contacts = users,
                        filteredContacts = users,
                        isLoading = false
                    )
                }.onFailure { exception ->
                    android.util.Log.e("NewConversation", "Failed to load contacts: ${exception.message}", exception)
                    _state.value = _state.value.copy(
                        error = "Failed to load contacts: ${exception.message}",
                        isLoading = false
                    )
                }
            }
        }
    }

    fun searchContacts(query: String) {
        _state.value = _state.value.copy(searchQuery = query)

        if (query.isBlank()) {
            _state.value = _state.value.copy(filteredContacts = _state.value.contacts)
            return
        }

        val q = query.lowercase().removePrefix("@")
        val filtered = _state.value.contacts.filter { user ->
            user.name.lowercase().contains(q) ||
                    user.displayName.lowercase().contains(q) ||
                    user.username.lowercase().removePrefix("@").contains(q) ||
                    user.email.lowercase().contains(q)
        }

        _state.value = _state.value.copy(filteredContacts = filtered)
    }

    /**
     * Start or resume a 1-to-1 conversation.
     *
     * Chat document lives at  chats/{chatId}  where chatId is the two UIDs
     * sorted alphabetically and joined with "_".  This is the same path that
     * ChatViewModel and HomeScreen read from, so there is no double-prefixing.
     *
     * After ensuring the document exists the user is navigated to
     * "chat/{chatId}" — NOT "chat/{otherUid}".
     */
    fun startConversation(otherUid: String, navController: NavController) {
        // ✅ Validar sesión ANTES de lanzar la corrutina: si no hay usuario
        // autenticado, mostrar un error accionable en lugar de un return silencioso.
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            android.util.Log.e("NewConversation", "Cannot start conversation: user not authenticated")
            _state.value = _state.value.copy(error = "Inicia sesión para chatear")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Deterministic, collision-free chat ID (utilidad única ChatId)
                val chatId = ChatId.create(currentUid, otherUid)

                val chatRef = FirebaseDatabase.getInstance().reference
                    .child("chats")
                    .child(chatId)

                val exists = try {
                    chatRef.get().await().exists()
                } catch (e: Exception) {
                    android.util.Log.w("NewConversation", "Read denied or failed, assuming chat exists: ${e.message}")
                    true // If we get permission denied, it's because it exists and rules blocked us.
                }
                
                if (!exists) {
                    chatRef.setValue(
                        mapOf(
                            "chatId" to chatId,
                            "members" to listOf(currentUid, otherUid),
                            "participants" to listOf(currentUid, otherUid), // Added for backwards compatibility
                            "createdAt" to ServerValue.TIMESTAMP,
                            "lastMessage" to "",
                            "lastMessageTime" to ServerValue.TIMESTAMP,
                            "lastMessageSenderId" to ""
                        )
                    ).await()
                }

                // NavController must be called on the main thread
                withContext(Dispatchers.Main) {
                    navController.navigate(Screen.Chat.createRoute(chatId))
                }
            } catch (e: Exception) {
                android.util.Log.e("NewConversation", "Could not start conversation: ${e.message}", e)
                _state.value = _state.value.copy(error = "No se pudo abrir el chat. Intenta de nuevo.")
            }
        }
    }

    /**
     * Limpia el error actual tras mostrarlo en la UI.
     */
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }

    /**
     * Create a new group chat and return its ID.
     */
    suspend fun createGroup(groupName: String, selectedUserIds: List<String>): String? {
        return try {
            _state.value = _state.value.copy(isCreatingGroup = true, error = null)

            val groupId = databaseRepository.createGroup(groupName, selectedUserIds)

            _state.value = _state.value.copy(isCreatingGroup = false)
            groupId
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = _state.value.copy(
                isCreatingGroup = false,
                error = "Failed to create group: ${e.message}"
            )
            null
        }
    }

    /**
     * Search for a user by their username handle.
     */
    suspend fun searchUserByUsername(username: String): Map<String, Any>? {
        return try {
            _state.value = _state.value.copy(isSearching = true, error = null)

            val user = databaseRepository.searchUserByUsername(username)

            _state.value = _state.value.copy(isSearching = false)
            user
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = _state.value.copy(
                isSearching = false,
                error = "Failed to search user: ${e.message}"
            )
            null
        }
    }

    /**
     * Send a friend / connection request to another user.
     */
    suspend fun sendFriendRequest(toUserId: String): Boolean {
        return try {
            _state.value = _state.value.copy(isSendingRequest = true, error = null)

            databaseRepository.sendFriendRequest(toUserId)

            _state.value = _state.value.copy(isSendingRequest = false)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            _state.value = _state.value.copy(
                isSendingRequest = false,
                error = "Failed to send friend request: ${e.message}"
            )
            false
        }
    }
    
    /**
     * Create demo chat with Azel Assistant
     */
    fun createDemoChat(navController: NavController) {
        // ✅ Validar sesión ANTES de lanzar la corrutina: mostrar error accionable
        // en lugar de un return silencioso.
        val currentUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUid == null) {
            android.util.Log.e("NewConversation", "Cannot open demo chat: user not authenticated")
            _state.value = _state.value.copy(error = "Inicia sesión para abrir el Demo Chat")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Initialize demo account
                demoAccountManager.initializeDemoAccount(currentUid)

                // ChatId canónico y consistente con HomeScreen y NewConversationScreen
                val chatId = ChatId.create(currentUid, "demo_azel_assistant")

                withContext(Dispatchers.Main) {
                    navController.navigate(Screen.Chat.createRoute(chatId))
                }
            } catch (e: Exception) {
                android.util.Log.e("NewConversation", "Error creating demo chat: ${e.message}", e)
                _state.value = _state.value.copy(error = "No se pudo abrir el Demo Chat. Intenta de nuevo.")
            }
        }
    }
}
