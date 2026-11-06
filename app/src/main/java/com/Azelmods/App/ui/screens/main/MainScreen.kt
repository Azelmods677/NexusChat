package com.Azelmods.App.ui.screens.main
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.animation.animateContentSize

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.Azelmods.App.data.manager.AppBackgroundManager
import com.Azelmods.App.data.model.BackgroundConfig
import com.Azelmods.App.data.model.BackgroundType
import com.Azelmods.App.data.preferences.ThemePreferences
import com.Azelmods.App.service.NotificationHelper
import com.Azelmods.App.ui.components.NexusDivider
import com.Azelmods.App.ui.components.VideoBackgroundPlayer
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.screens.calls.CallsScreen
import com.Azelmods.App.ui.screens.home.HomeScreenRedesigned
import com.Azelmods.App.ui.screens.profile.ProfileScreen
import com.Azelmods.App.ui.screens.stories.StoriesScreen
import com.Azelmods.App.ui.theme.NexusTokens
import com.Azelmods.App.ui.theme.linearGradientBrush
import com.Azelmods.App.ui.theme.parseHexColor
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

data class TabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun MainScreen(
    navController: NavController,
    appBackgroundManager: AppBackgroundManager = hiltViewModel<MainViewModel>().appBackgroundManager
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences(context) }

    val backgroundConfig by appBackgroundManager.backgroundConfig.collectAsState(
        initial = BackgroundConfig(type = BackgroundType.NONE)
    )

    val allTabs = listOf(
        TabItem(Screen.Home.route, "Chats", Icons.AutoMirrored.Filled.Chat, Icons.AutoMirrored.Outlined.Chat),
        TabItem(Screen.Stories.route, "Historias", Icons.Filled.AutoStories, Icons.Outlined.AutoStories),
        TabItem(Screen.Calls.route, "Llamadas", Icons.Filled.Call, Icons.Outlined.Call),
        TabItem("profile_main", "Perfil", Icons.Filled.Person, Icons.Outlined.Person)
    )

    val missedCallCount by NotificationHelper.missedCallCount.collectAsStateWithLifecycle()

    val savedTabOrder = remember { themePrefs.getTabOrder() }
    val hideStoriesTab by com.Azelmods.App.ui.components.ChatLook.hideStoriesTab.collectAsState()
    val tabOrder = remember(savedTabOrder, hideStoriesTab) {
        val complete = savedTabOrder.takeIf { it.size == allTabs.size } ?: allTabs.indices.toList()
        if (hideStoriesTab) complete.filter { it != 1 } else complete
    }
    val tabs = remember(tabOrder) { tabOrder.mapNotNull { index -> allTabs.getOrNull(index) } }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        containerColor = NexusTokens.Color.BgBase,
        bottomBar = {
            // Barra opaca con un filo superior. Sobre un fondo de imagen o vídeo una
            // barra transparente hace ilegibles las etiquetas.
            Column(Modifier.fillMaxWidth()) {
                val navStyle by com.Azelmods.App.ui.components.ChatLook.navBarStyle.collectAsState()
                if (navStyle == com.Azelmods.App.ui.components.NavBarStyle.FLOATING) {
                    // Barra flotante: una cápsula elevada; la pestaña elegida muestra su nombre.
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .shadow(10.dp, androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                            .background(NexusTokens.Color.BgElevated)
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            val selected = currentPage == index
                            Row(
                                Modifier
                                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(22.dp))
                                    .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent)
                                    .clickable {
                                        if (tab.route == Screen.Calls.route) NotificationHelper.resetMissedCallCount()
                                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                    }
                                    .animateContentSize()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (selected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = tab.label,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else NexusTokens.Color.TextMuted
                                )
                                if (selected) {
                                    Spacer(Modifier.width(8.dp))
                                    Text(tab.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                } else {
                NexusDivider()
                NavigationBar(
                    containerColor = NexusTokens.Color.BgBase,
                    tonalElevation = NexusTokens.Elevation.none,
                    windowInsets = WindowInsets.navigationBars
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val selected = currentPage == index
                        val isCallsTab = tab.route == Screen.Calls.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                if (isCallsTab) NotificationHelper.resetMissedCallCount()
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            },
                            icon = {
                                val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
                                // La pestaña elegida "salta" un poco al seleccionarse.
                                val iconScale by animateFloatAsState(
                                    targetValue = if (selected) 1.12f else 1f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "iconoPestana"
                                )
                                val iconTurn by animateFloatAsState(
                                    targetValue = if (selected) 0f else -12f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    ),
                                    label = "giroPestana"
                                )
                                val iconModifier = Modifier.graphicsLayer {
                                    scaleX = iconScale
                                    scaleY = iconScale
                                    rotationZ = if (selected) iconTurn else 0f
                                }
                                if (isCallsTab && missedCallCount > 0) {
                                    BadgedBox(badge = {
                                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                                            Text(if (missedCallCount > 99) "99+" else missedCallCount.toString())
                                        }
                                    }) {
                                        Icon(icon, contentDescription = null, modifier = iconModifier)
                                    }
                                } else {
                                    Icon(icon, contentDescription = null, modifier = iconModifier)
                                }
                            },
                            label = if (navStyle == com.Azelmods.App.ui.components.NavBarStyle.ICONS) {
                                null
                            } else {
                                { Text(tab.label, style = MaterialTheme.typography.labelMedium) }
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
                                unselectedIconColor = NexusTokens.Color.TextMuted,
                                unselectedTextColor = NexusTokens.Color.TextMuted
                            )
                        )
                    }
                }
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            AppBackgroundLayer(backgroundConfig)
            // Sobre una imagen o un vídeo, sin este velo los chats no se leen.
            val wallpaperDim by com.Azelmods.App.ui.components.ChatLook.wallpaperDim.collectAsState()
            val backgroundType = backgroundConfig?.type
            if (backgroundType != null &&
                backgroundType != com.Azelmods.App.data.model.BackgroundType.NONE &&
                backgroundType != com.Azelmods.App.data.model.BackgroundType.SOLID_COLOR
            ) {
                Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = wallpaperDim)))
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                userScrollEnabled = true,
                beyondViewportPageCount = 1
            ) { page ->
                when (tabOrder.getOrNull(page) ?: page) {
                    0 -> HomeScreenRedesigned(
                        navController = navController,
                        onOpenTab = { logical ->
                            val target = tabOrder.indexOf(logical).takeIf { it >= 0 } ?: logical
                            coroutineScope.launch { pagerState.animateScrollToPage(target) }
                        }
                    )
                    1 -> StoriesScreen(navController = navController)
                    2 -> CallsScreen(navController = navController)
                    3 -> {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        // Es una pestaña raíz: una flecha "atrás" aquí sacaba de la app.
                        ProfileScreen(navController = navController, userId = userId, showBack = false)
                    }
                }
            }
        }
    }
}

/** Fondo elegido por el usuario. Sin uno propio, el fondo sólido de la app. */
@Composable
private fun AppBackgroundLayer(config: BackgroundConfig) {
    val base = NexusTokens.Color.BgBase
    when (config.type) {
        BackgroundType.IMAGE -> config.imageUri?.let { uri ->
            AsyncImage(
                model = uri,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        BackgroundType.VIDEO -> config.videoUri?.let { uri ->
            VideoBackgroundPlayer(videoUri = uri, modifier = Modifier.fillMaxSize(), fallbackColor = base)
        }
        BackgroundType.SOLID_COLOR -> Box(
            Modifier.fillMaxSize().background(parseHexColor(config.colorHex ?: "#101214"))
        )
        BackgroundType.GRADIENT -> {
            val brush = linearGradientBrush(
                gradientColors = config.gradientColors,
                gradientAngle = config.gradientAngle
            )
            if (brush != null) Box(Modifier.fillMaxSize().background(brush))
            else Box(Modifier.fillMaxSize().background(base))
        }
        else -> Box(Modifier.fillMaxSize().background(base))
    }

    // Capa de legibilidad que controla el propio usuario.
    if (config.type != BackgroundType.NONE) {
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = config.overlayAlpha)))
    }
}
