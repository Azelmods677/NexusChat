package com.Azelmods.App.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.Azelmods.App.data.model.Chat
import com.Azelmods.App.data.model.ChatType
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.DarkBackground
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.DarkSurfaceVariant
import com.Azelmods.App.ui.theme.Purple
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    var searchVisible by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "NexusChat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        color = Color.White
                    )
                },
                actions = {
                    // Toggle animated inline search bar
                    IconButton(
                        onClick = {
                            searchVisible = !searchVisible
                            if (!searchVisible) viewModel.onSearchQueryChange("")
                        }
                    ) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Default.Close
                            else Icons.Default.Search,
                            contentDescription = if (searchVisible) "Close search" else "Open search",
                            tint = Color.White
                        )
                    }
                    // Quick compose / new conversation
                    IconButton(
                        onClick = { navController.navigate(Screen.NewConversation.route) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "New Conversation",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.NewConversation.route) },
                containerColor = Purple,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat")
            }
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {

            // ── Animated inline search bar ─────────────────────────────────
            AnimatedVisibility(
                visible = searchVisible,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = {
                        Text(
                            text = "Search conversations…",
                            color = Color.Gray
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = Purple
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.isNotBlank()) {
                            IconButton(
                                onClick = { viewModel.onSearchQueryChange("") }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = Color.Gray
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Purple,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Purple,
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedLeadingIconColor = Purple,
                        unfocusedLeadingIconColor = Color.Gray,
                        focusedPlaceholderColor = Color.Gray,
                        unfocusedPlaceholderColor = Color.Gray
                    )
                )
            }

            // ── Filter chip row ────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChatFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = state.selectedFilter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                        label = {
                            Text(
                                text = filter.name
                                    .lowercase()
                                    .replaceFirstChar { it.titlecase(Locale.getDefault()) }
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            // ── Main content ───────────────────────────────────────────────
            when {

                // Loading spinner
                state.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Purple)
                    }
                }

                // Error state with retry
                state.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Text(
                                text = state.error ?: "Something went wrong",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                            Button(
                                onClick = { viewModel.refreshChats() },
                                colors = ButtonDefaults.buttonColors(containerColor = Purple)
                            ) {
                                Text("Retry", color = Color.White)
                            }
                        }
                    }
                }

                // Illustrated empty state
                state.filteredChats.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No conversations yet",
                                style = MaterialTheme.typography.headlineSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Tap + to start chatting",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Populated chat list
                else -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(
                            items = state.filteredChats,
                            key = { chat -> chat.chatId }
                        ) { chat ->
                            ChatItem(
                                chat = chat,
                                currentUserId = currentUserId,
                                onChatClick = {
                                    navController.navigate(
                                        Screen.Chat.createRoute(chat.chatId)
                                    )
                                },
                                onTogglePin = { viewModel.togglePin(chat.chatId) },
                                onToggleMute = { viewModel.toggleMute(chat.chatId) },
                                onArchiveChat = { viewModel.archiveChat(chat.chatId) },
                                onDeleteChat = { viewModel.deleteChat(chat.chatId) }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 84.dp),
                                color = DarkSurfaceVariant,
                                thickness = 0.5.dp
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── ChatItem ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatItem(
    chat: Chat,
    currentUserId: String,
    onChatClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleMute: () -> Unit,
    onArchiveChat: () -> Unit,
    onDeleteChat: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    val otherUserName = chat.participantNames.values.firstOrNull() ?: "Unknown"
    val otherPhotoUrl = chat.participantPhotos.values.firstOrNull()
    val unreadCount = chat.unreadCount[currentUserId] ?: 0

    // Wrap in Box so the DropdownMenu is anchored to the item
    Box(modifier = Modifier.fillMaxWidth()) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onChatClick,
                    onLongClick = { showMenu = true }
                )
                .background(DarkBackground)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // ── Avatar ─────────────────────────────────────────────────────
            Box(modifier = Modifier.size(56.dp)) {
                if (chat.chatType == ChatType.GROUP) {
                    // Group chats: dedicated group-icon circle
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(Purple.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "Group Chat",
                            tint = Purple,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                } else {
                    // Private chats: real avatar via UserAvatar (Coil + initials fallback)
                    UserAvatar(
                        name = otherUserName,
                        photoUrl = otherPhotoUrl,
                        size = 56.dp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // ── Name + last message ────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = otherUserName,
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = chat.lastMessage.ifBlank { "No messages yet" },
                    color = Color.Gray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // ── Timestamp + unread badge ───────────────────────────────────
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = formatTimestamp(chat.lastMessageTime),
                    color = if (unreadCount > 0) Purple else Color.Gray,
                    fontSize = 12.sp
                )

                if (unreadCount > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Purple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // ── Long-press context menu ────────────────────────────────────────
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(DarkSurface)
        ) {
            DropdownMenuItem(
                text = { Text("Pin Chat", color = Color.White) },
                onClick = { showMenu = false; onTogglePin() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Mute", color = Color.White) },
                onClick = { showMenu = false; onToggleMute() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Archive", color = Color.White) },
                onClick = { showMenu = false; onArchiveChat() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
            )
            HorizontalDivider(color = DarkSurfaceVariant)
            DropdownMenuItem(
                text = {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                },
                onClick = { showMenu = false; onDeleteChat() },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

/**
 * Converts a Unix-epoch millisecond [timestamp] into a human-readable string:
 *   - < 1 min   → "Just now"
 *   - < 1 hour  → "Xm"
 *   - < 1 day   → "Xh"
 *   - yesterday → "Yesterday"
 *   - < 1 week  → "Mon" / "Tue" / …
 *   - older     → "Jan 5" / "Dec 12" / …
 */
private fun formatTimestamp(timestamp: Long): String {
    if (timestamp <= 0L) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000L -> "Just now"
        diff < 3_600_000L -> "${diff / 60_000L}m"
        diff < 86_400_000L -> "${diff / 3_600_000L}h"
        diff < 172_800_000L -> "Yesterday"
        diff < 604_800_000L ->
            SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))

        else ->
            SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(timestamp))
    }
}
