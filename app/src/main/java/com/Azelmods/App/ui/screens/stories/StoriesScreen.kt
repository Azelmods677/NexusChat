package com.Azelmods.App.ui.screens.stories

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.Azelmods.App.data.preferences.TutorialPreferences
import com.Azelmods.App.data.tutorials.AppFeature
import com.Azelmods.App.ui.components.AutoTutorial
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoriesScreen(
    navController: NavController,
    viewModel: StoriesViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val tutorialPreferences = remember { TutorialPreferences(context) }

    // ── Lifecycle-aware state collection ──────────────────────────────────────
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Show tutorial on first visit
    AutoTutorial(
        feature = AppFeature.STORIES,
        tutorialPreferences = tutorialPreferences
    )

    // Show error toast
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            android.widget.Toast.makeText(context, error, android.widget.Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Stories",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White
                ),
                modifier = Modifier.statusBarsPadding() // Edge-to-Edge: respeta barra de estado
            )
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0) // Edge-to-Edge: control manual de insets
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues) // Edge-to-Edge: consume los insets del Scaffold
                .navigationBarsPadding() // Edge-to-Edge: respeta barra de navegación
                .padding(horizontal = 16.dp) // Padding general de 16dp
        ) {
            // ── Stories horizontal scroll ──────────────────────────────────────
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp), // Aumentado para que respire más
                horizontalArrangement = Arrangement.spacedBy(16.dp), // Espaciado aumentado
                contentPadding = PaddingValues(horizontal = 0.dp) // Ya tenemos padding general
            ) {
                // Your story bubble — shows the logged-in user's own profile photo
                item {
                    YourStoryItem(
                        currentUserPhotoUrl = state.currentUserPhotoUrl,
                        onClick = {
                            navController.navigate(Screen.CreateStory.route)
                        }
                    )
                }

                // Real stories from Firebase 
                if (!state.isLoading) {
                    items(state.stories) { story ->
                        StoryItem(
                            story = story,
                            onClick = {
                                navController.navigate(
                                    Screen.StoryViewer.createRoute(story.userId)
                                )
                            }
                        )
                    }
                }
            }

            // ── Body: loading indicator OR content ──────────────────────────
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }

                    state.stories.isEmpty() -> {
                        // ── Improved empty state ────────────────────────────
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Text(
                                text = "📖",
                                fontSize = 64.sp
                            )
                            Text(
                                text = "No stories yet",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap your avatar to share a moment",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

// ───────────────────────────────────────────────────────────────────────────
// Your Story bubble
// ───────────────────────────────────────────────────────────────────────────

@Composable
fun YourStoryItem(
    currentUserPhotoUrl: String?,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Subtle gradient ring to distinguish "your" bubble
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                        )
                    )
            )

            // White gap between ring and avatar
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
            )

            // Profile photo or placeholder
            if (!currentUserPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(currentUserPhotoUrl)
                        .crossfade(300)
                        .build(),
                    contentDescription = "Your profile photo",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(DarkSurface),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You",
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Add (+) badge
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .align(Alignment.BottomEnd)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(1.5.dp, Color.Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Story",
                    tint = Color.White,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Your Story",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1
        )
    }
}

// ───────────────────────────────────────────────────────────────────────────
// Contact story bubble
// ───────────────────────────────────────────────────────────────────────────

@Composable
fun StoryItem(
    story: StoryItemData,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "story_ring")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ring_rotation"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.size(68.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated rotating rainbow ring (unviewed) or grey ring (viewed)
            if (!story.isViewed) {
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .size(68.dp)
                        .rotate(rotation)
                ) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(
                                PurpleBright,
                                CyanAccent,
                                RosePink,
                                NeonGreen,
                                GoldPremium,
                                PurpleBright
                            )
                        ),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx(),
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(Color.Gray.copy(alpha = 0.3f))
                )
            }

            // White gap
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
            )

            // Avatar: real photo or initial fallback
            if (!story.userPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(story.userPhotoUrl)
                        .crossfade(300)
                        .build(),
                    contentDescription = "Profile photo of ${story.userName}",
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                val initial = story.userName
                    .takeIf { it.isNotBlank() }
                    ?.take(1)
                    ?.uppercase()
                    ?: "?"

                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val displayName = story.userName
            .ifBlank { "User" }
            .let { if (it.length > 12) it.take(11) + "…" else it }

        Text(
            text = displayName,
            color = if (story.isViewed) Color.Gray else Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (story.isViewed) FontWeight.Normal else FontWeight.Medium,
            maxLines = 1
        )
    }
}
