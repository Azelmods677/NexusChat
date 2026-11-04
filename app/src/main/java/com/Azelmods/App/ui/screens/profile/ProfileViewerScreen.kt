package com.Azelmods.App.ui.screens.profile

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth

/** Perfil de otra persona, al tocar su avatar: el mismo diseño que el propio. */
@Composable
fun ProfileViewerScreen(
    navController: NavController,
    userId: String,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isOwn = userId == FirebaseAuth.getInstance().currentUser?.uid
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(userId) {
        if (userId.isNotEmpty()) viewModel.loadUserProfile(userId)
    }

    ProfileContent(
        state = state,
        seed = userId,
        isOwn = isOwn,
        actions = ProfileActions(
            onBack = { navController.popBackStack() },
            onMessage = {
                FirebaseAuth.getInstance().currentUser?.uid?.let { currentUid ->
                    val chatId = com.Azelmods.App.data.chat.ChatId.create(currentUid, userId)
                    navController.navigate("chat/$chatId") {
                        popUpTo("profile_viewer/$userId") { inclusive = true }
                    }
                }
            },
            onCall = { video -> navController.navigate("active_call/$userId/${if (video) "video" else "audio"}") },
            onEdit = {
                navController.popBackStack()
                navController.navigate("profile/$userId")
            },
            onCustomize = { navController.navigate(Screen.ProfileCustomize.route) },
            onSettings = { navController.navigate(Screen.Settings.route) },
            onPremium = { navController.navigate(Screen.Premium.route) },
            // Solo se ven al abrir el perfil propio desde aquí: los selectores viven en el perfil.
            onAddMedia = { navController.navigate("profile/$userId") },
            onChangeMain = { navController.navigate("profile/$userId") },
            onRemoveVideo = viewModel::removeProfileVideo
        ),
        onOpenViewer = { viewerIndex = it }
    )

    viewerIndex?.let { index ->
        ProfileMediaViewer(
            media = heroSlides(state.user, state.media).toViewerMedia(),
            startIndex = index,
            canDelete = false,
            onDelete = {},
            onDismiss = { viewerIndex = null }
        )
    }
}
