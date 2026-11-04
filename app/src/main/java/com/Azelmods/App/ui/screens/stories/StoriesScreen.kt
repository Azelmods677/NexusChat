package com.Azelmods.App.ui.screens.stories

import com.Azelmods.App.i18n.tr

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.Azelmods.App.data.preferences.TutorialPreferences
import com.Azelmods.App.data.tutorials.AppFeature
import com.Azelmods.App.ui.components.AutoTutorial
import com.Azelmods.App.ui.components.NexusDivider
import com.Azelmods.App.ui.components.NexusEmptyState
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Historias en forma de lista: tu historia arriba y, debajo, las de tus contactos
 * separadas en recientes y vistas. Cada anillo dice una sola cosa: si queda algo por ver.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesScreen(
    navController: NavController,
    viewModel: StoriesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val tutorialPreferences = remember { TutorialPreferences(context) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    AutoTutorial(feature = AppFeature.STORIES, tutorialPreferences = tutorialPreferences)

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
    val others = state.stories.filter { it.userId != currentUserId }
    val mine = state.stories.firstOrNull { it.userId == currentUserId }
    val recent = others.filterNot { it.isViewed }
    val viewed = others.filter { it.isViewed }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        tr("Historias"),
                        style = MaterialTheme.typography.headlineMedium,
                        color = NexusTokens.Color.TextPrimary
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.CreateStory.route) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                elevation = FloatingActionButtonDefaults.elevation(2.dp, 2.dp, 2.dp, 2.dp)
            ) {
                Icon(Icons.Default.PhotoCamera, contentDescription = tr("Crear historia"))
            }
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            item(key = "my_story") {
                MyStoryRow(
                    photoUrl = state.currentUserPhotoUrl,
                    hasStory = mine != null,
                    onClick = {
                        if (mine != null) navController.navigate(Screen.StoryViewer.createRoute(mine.userId))
                        else navController.navigate(Screen.CreateStory.route)
                    },
                    onAdd = { navController.navigate(Screen.CreateStory.route) }
                )
            }

            when {
                state.isLoading -> item(key = "loading") {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                others.isEmpty() -> item(key = "empty") {
                    Box(Modifier.fillParentMaxHeight(0.7f)) {
                        NexusEmptyState(
                            icon = Icons.Outlined.AutoStories,
                            title = tr("Sin historias nuevas"),
                            body = "Cuando tus contactos publiquen algo, aparecerá aquí durante 24 horas."
                        )
                    }
                }

                else -> {
                    if (recent.isNotEmpty()) {
                        item(key = "h_recent") { NexusSectionHeader(tr("Recientes")) }
                        items(recent, key = { "r_${it.storyId}" }) { story ->
                            StoryRow(story) {
                                navController.navigate(Screen.StoryViewer.createRoute(story.userId))
                            }
                        }
                    }
                    if (viewed.isNotEmpty()) {
                        item(key = "h_viewed") { NexusSectionHeader(tr("Vistas")) }
                        items(viewed, key = { "v_${it.storyId}" }) { story ->
                            StoryRow(story) {
                                navController.navigate(Screen.StoryViewer.createRoute(story.userId))
                            }
                        }
                    }
                }
            }
            item(key = "bottom_space") { Spacer(Modifier.height(88.dp)) }
        }
    }
}

@Composable
private fun MyStoryRow(
    photoUrl: String?,
    hasStory: Boolean,
    onClick: () -> Unit,
    onAdd: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(58.dp)) {
                StoryAvatar(name = "Tú", photoUrl = photoUrl, ringColor = if (hasStory) NexusTokens.Color.Hairline else null)
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .border(2.dp, NexusTokens.Color.BgBase, CircleShape)
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = tr("Añadir historia"),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    tr("Mi historia"),
                    style = MaterialTheme.typography.titleMedium,
                    color = NexusTokens.Color.TextPrimary
                )
                Text(
                    if (hasStory) tr("Toca para verla") else tr("Comparte algo durante 24 horas"),
                    style = MaterialTheme.typography.bodySmall,
                    color = NexusTokens.Color.TextMuted
                )
            }
        }
        NexusDivider()
    }
}

@Composable
private fun StoryRow(story: StoryItemData, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        StoryAvatar(
            name = story.userName,
            photoUrl = story.userPhotoUrl,
            ringColor = if (story.isViewed) NexusTokens.Color.BgHigh else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = story.userName.ifBlank { "Usuario" },
                style = MaterialTheme.typography.titleMedium,
                color = NexusTokens.Color.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatStoryTime(story.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = NexusTokens.Color.TextMuted
            )
        }
    }
}

@Composable
private fun StoryAvatar(name: String, photoUrl: String?, ringColor: Color?) {
    Box(modifier = Modifier.size(58.dp), contentAlignment = Alignment.Center) {
        if (ringColor != null) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, ringColor, CircleShape)
            )
        }
        UserAvatar(name = name.ifBlank { "?" }, photoUrl = photoUrl, size = 50.dp)
    }
}

private fun formatStoryTime(timestamp: Long): String {
    val minutes = (System.currentTimeMillis() - timestamp) / 60_000
    return when {
        minutes < 1 -> tr("Ahora mismo")
        minutes < 60 -> "Hace $minutes min"
        minutes < 24 * 60 -> "Hace ${minutes / 60} h"
        else -> SimpleDateFormat("d MMM, HH:mm", Locale.forLanguageTag("es")).format(Date(timestamp))
    }
}
