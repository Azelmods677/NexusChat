package com.Azelmods.App.ui.screens.profile

import com.Azelmods.App.i18n.tr

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.components.AvatarRing
import com.Azelmods.App.ui.components.LoopingVideo
import com.Azelmods.App.ui.components.NexusListRow
import com.Azelmods.App.ui.components.NexusSectionHeader
import com.Azelmods.App.ui.components.ProfileColors
import com.Azelmods.App.ui.components.UserAvatar
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.theme.NexusTokens
import com.google.firebase.auth.FirebaseAuth

/**
 * Todo lo que cambia el aspecto del perfil, en un solo sitio y con vista previa en directo.
 * Antes los colores estaban en medio del perfil, a la vista de quien lo visitaba.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCustomizeScreen(
    navController: NavController,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val uid = remember { FirebaseAuth.getInstance().currentUser?.uid.orEmpty() }
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(uid) { if (uid.isNotEmpty()) viewModel.loadUserProfile(uid) }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            viewModel.clearError()
        }
    }

    val context = LocalContext.current
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            if (context.contentResolver.getType(uri)?.startsWith("video/") == true) {
                viewModel.uploadCoverVideo(uri)
            } else {
                navController.navigate("image_crop?uri=${Uri.encode(uri.toString())}&type=cover")
            }
        }
    }
    val savedState = navController.currentBackStackEntry?.savedStateHandle
    val cropUri = savedState?.get<String>("crop_uri")
    val cropType = savedState?.get<String>("crop_type")
    LaunchedEffect(cropUri) {
        cropUri?.let { uriString ->
            if (cropType == "cover") {
                viewModel.uploadCoverPhoto(
                    Uri.parse(uriString),
                    savedState.get<Float>("crop_scale") ?: 1f,
                    savedState.get<Float>("crop_offset_x") ?: 0f,
                    savedState.get<Float>("crop_offset_y") ?: 0f
                )
            }
            savedState.remove<String>("crop_uri")
            savedState.remove<String>("crop_type")
        }
    }

    val user = state.user
    val name = user?.displayName?.ifBlank { user.name }?.ifBlank { null } ?: tr("Tu perfil")
    val isPremium = state.premium.isActiveNow()
    val current = ProfileColors.indexFor(user?.uid ?: uid, user?.profileColor ?: -1)
    val colors = ProfileColors.palettes[current]
    val hasCover = !user?.coverUrl.isNullOrBlank() || !user?.coverVideoUrl.isNullOrBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(tr("Personalizar perfil")) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, tr("Volver"))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NexusTokens.Color.BgBase)
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = NexusTokens.Color.BgBase
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Vista previa: lo que van a ver los demás, en directo. Se queda fija arriba para
            // que se siga viendo al bajar por la lista de colores.
            Box(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
                    .height(190.dp)
                    .clip(RoundedCornerShape(NexusTokens.Radius.xxl))
                    .background(Brush.linearGradient(colors))
            ) {
                val coverVideo = user?.coverVideoUrl
                if (!coverVideo.isNullOrBlank()) {
                    LoopingVideo(coverVideo, Modifier.fillMaxSize())
                } else user?.coverUrl?.takeIf { it.isNotBlank() }?.let {
                    AsyncImage(
                        model = it,
                        contentDescription = tr("Portada"),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                )
                if (state.isUploadingProfile) {
                    CircularProgressIndicator(
                        color = Color.White,
                        strokeWidth = 2.dp,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(28.dp)
                    )
                }
                Row(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserAvatar(
                        name = name,
                        photoUrl = user?.photoUrl,
                        size = 72.dp,
                        ring = AvatarRing(colors, width = 3.dp, rotating = true)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(name, color = Color.White, style = MaterialTheme.typography.titleLarge)
                        Text(tr("Vista previa"), color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                NexusSectionHeader(tr("Portada"))
                NexusListRow(
                    title = if (!hasCover) tr("Añadir portada") else tr("Cambiar portada"),
                    subtitle = tr("Una foto o un vídeo, arriba de tu perfil"),
                    icon = Icons.Outlined.Image,
                    onClick = {
                        coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo))
                    }
                )
                if (hasCover) {
                    NexusListRow(
                        title = tr("Quitar portada"),
                        icon = Icons.Outlined.DeleteOutline,
                        onClick = viewModel::removeCover
                    )
                }

                NexusSectionHeader(tr("Color del perfil"))
                PaletteGrid(
                    indices = ProfileColors.freeIndices,
                    selected = current,
                    locked = false,
                    onSelect = viewModel::setProfileColor
                )

                NexusSectionHeader(tr("Colores Premium"))
                PaletteGrid(
                    indices = ProfileColors.premiumIndices,
                    selected = current,
                    locked = !isPremium,
                    onSelect = { index ->
                        if (isPremium) viewModel.setProfileColor(index) else navController.navigate(Screen.Premium.route)
                    }
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun PaletteGrid(indices: List<Int>, selected: Int, locked: Boolean, onSelect: (Int) -> Unit) {
    Column(
        Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        indices.chunked(4).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { index ->
                    PaletteCell(index, index == selected, locked, Modifier.weight(1f)) { onSelect(index) }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun PaletteCell(index: Int, isSelected: Boolean, locked: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val scale by animateFloatAsState(
        if (isSelected) 1.1f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "escalaColor"
    )
    val border by animateColorAsState(
        if (isSelected) NexusTokens.Color.TextPrimary else Color.Transparent,
        label = "bordeColor"
    )
    val palette = ProfileColors.palettes[index]
    // Sobre los colores más claros, un icono blanco no se distingue.
    val iconTint = if (palette.map { it.luminance() }.average() > 0.42) Color.Black else Color.White
    Column(modifier.clickable(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(54.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Brush.linearGradient(palette))
                .border(2.dp, border, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when {
                isSelected -> Icon(Icons.Default.Check, tr("Elegido"), tint = iconTint, modifier = Modifier.size(22.dp))
                locked -> Icon(Icons.Default.Lock, tr("Solo Premium"), tint = iconTint.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            tr(ProfileColors.names[index]),
            color = NexusTokens.Color.TextSecondary,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}
