package com.Azelmods.App.ui.screens.profile

import com.Azelmods.App.i18n.tr

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.Azelmods.App.ui.components.NexusListRow
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.navigation.Screen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(
    navController: NavController,
    userId: String,
    showBack: Boolean = true,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var viewerIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(userId) { viewModel.loadUserProfile(userId) }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    // PickVisualMedia no necesita permisos: abrir el perfil no pide acceso a nada.
    val mainPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            if (context.contentResolver.getType(uri)?.startsWith("video/") == true) {
                viewModel.uploadProfileVideo(uri)
            } else {
                navController.navigate("image_crop?uri=${Uri.encode(uri.toString())}&type=profile")
            }
        }
    }
    val galleryPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        uri?.let(viewModel::addProfileMedia)
    }
    val imagesAndVideos = remember { PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo) }

    // Resultado de ImageCropScreen
    val savedState = navController.currentBackStackEntry?.savedStateHandle
    val cropUri = savedState?.get<String>("crop_uri")
    val cropType = savedState?.get<String>("crop_type")
    val cropScale = savedState?.get<Float>("crop_scale") ?: 1f
    val cropOffsetX = savedState?.get<Float>("crop_offset_x") ?: 0f
    val cropOffsetY = savedState?.get<Float>("crop_offset_y") ?: 0f

    LaunchedEffect(cropUri) {
        cropUri?.let { uriString ->
            if (cropType == "profile") {
                viewModel.uploadProfilePhoto(Uri.parse(uriString), cropScale, cropOffsetX, cropOffsetY)
            }
            savedState.remove<String>("crop_uri")
            savedState.remove<String>("crop_type")
        }
    }

    if (state.isLoading && state.user == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp,
                modifier = Modifier.size(28.dp)
            )
        }
        return
    }

    val actions = ProfileActions(
        onBack = if (showBack) ({ navController.popBackStack() }) else null,
        onMessage = {
            FirebaseAuth.getInstance().currentUser?.uid?.let { currentUid ->
                navController.navigate("chat/${com.Azelmods.App.data.chat.ChatId.create(currentUid, userId)}")
            }
        },
        onCall = { video -> navController.navigate("active_call/$userId/${if (video) "video" else "audio"}") },
        onEdit = { navController.navigate(Screen.EditProfile.route) },
        onCustomize = { navController.navigate(Screen.ProfileCustomize.route) },
        onSettings = { navController.navigate(Screen.Settings.route) },
        onPremium = { navController.navigate(Screen.Premium.route) },
        onAddMedia = { galleryPicker.launch(imagesAndVideos) },
        onChangeMain = { mainPicker.launch(imagesAndVideos) },
        onRemoveVideo = viewModel::removeProfileVideo
    )

    Box(Modifier.fillMaxSize()) {
        ProfileContent(
            state = state,
            seed = userId,
            isOwn = state.isOwnProfile,
            actions = actions,
            onOpenViewer = { viewerIndex = it }
        ) {
            if (state.isOwnProfile) {
                NexusSectionHeader(tr("Accesos"))
                NexusListRow(
                    title = tr("Azel IA"),
                    subtitle = tr("Asistente y agentes"),
                    icon = Icons.Outlined.SmartToy,
                    onClick = { navController.navigate(Screen.AzelAI.route) }
                )
                NexusListRow(
                    title = tr("Seguridad y Tor"),
                    subtitle = tr("Modo anónimo, bloqueo y copias cifradas"),
                    icon = Icons.Outlined.Security,
                    onClick = { navController.navigate(Screen.Security.route) }
                )
                NexusListRow(
                    title = tr("Acerca de"),
                    subtitle = tr("Versión, novedades y dispositivo"),
                    icon = Icons.Outlined.Info,
                    onClick = { navController.navigate(Screen.SettingsAbout.route) }
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding())
    }

    viewerIndex?.let { index ->
        ProfileMediaViewer(
            media = heroSlides(state.user, state.media).toViewerMedia(),
            startIndex = index,
            canDelete = state.isOwnProfile,
            onDelete = viewModel::deleteProfileMedia,
            onSetMain = if (state.isOwnProfile) viewModel::setMainFromMedia else null,
            onDismiss = { viewerIndex = null }
        )
    }
}
