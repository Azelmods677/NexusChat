package com.Azelmods.App.ui.screens.main
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.Azelmods.App.data.preferences.ThemePreferences
import com.Azelmods.App.data.manager.AppBackgroundManager
import com.Azelmods.App.data.model.BackgroundConfig
import com.Azelmods.App.data.model.BackgroundType
import com.Azelmods.App.service.NotificationHelper
import com.Azelmods.App.ui.theme.linearGradientBrush
import com.Azelmods.App.ui.theme.parseHexColor
import com.Azelmods.App.ui.components.VideoBackgroundPlayer
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.screens.calls.CallsScreen
import com.Azelmods.App.ui.screens.home.HomeScreenRedesigned
import com.Azelmods.App.ui.screens.profile.ProfileScreen
import com.Azelmods.App.ui.screens.stories.StoriesScreen
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import coil3.compose.AsyncImage
import com.Azelmods.App.ui.theme.DarkDeep

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
    
    // Get app-wide background configuration
    val backgroundConfig by appBackgroundManager.backgroundConfig.collectAsState(
        initial = BackgroundConfig(type = BackgroundType.NONE)
    )
    
    // Define all available tabs
    val allTabs = listOf(
        TabItem(
            route = Screen.Home.route,
            label = "Chats",
            selectedIcon = Icons.AutoMirrored.Filled.Chat,
            unselectedIcon = Icons.Outlined.ChatBubbleOutline
        ),
        TabItem(
            route = Screen.Stories.route,
            label = "Stories",
            selectedIcon = Icons.Default.AutoStories,
            unselectedIcon = Icons.Outlined.AutoStories
        ),
        TabItem(
            route = Screen.Calls.route,
            label = "Calls",
            selectedIcon = Icons.Default.Call,
            unselectedIcon = Icons.Outlined.Call
        ),
        TabItem(
            route = "profile_main",
            label = "Profile",
            selectedIcon = Icons.Default.Person,
            unselectedIcon = Icons.Outlined.Person
        )
    )
    
    // Observar contador de llamadas perdidas
    val missedCallCount by NotificationHelper.missedCallCount.collectAsStateWithLifecycle()

    // Get custom tab order from preferences
    val tabOrder = remember { themePrefs.getTabOrder() }
    val tabs = remember(tabOrder) {
        tabOrder.mapNotNull { index ->
            allTabs.getOrNull(index)
        }.takeIf { it.size == allTabs.size } ?: allTabs
    }
    
    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f,
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()
    
    // Keep pager and nav bar in sync
    val currentPage = pagerState.currentPage
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            // Nexus bottom bar: el contenedor se mantiene TRANSPARENTE a propósito
            // para que los fondos animados/fotos del home se vean a través de la
            // barra. El único añadido de identidad es un hairline de 1dp con el
            // degradado de marca (primary→secondary) sobre el borde superior — no
            // tapa el fondo. Lógica de pager/badge/selección intacta.
            val navScheme = MaterialTheme.colorScheme
            Column(Modifier.fillMaxWidth()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color.Transparent,
                                    navScheme.primary.copy(alpha = 0.55f),
                                    navScheme.secondary.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp,
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                tabs.forEachIndexed { index, tab ->
                    val isCallsTab = tab.label == "Calls"
                    NavigationBarItem(
                        selected = currentPage == index,
                        onClick = {
                            // Limpiar contador al abrir Calls
                            if (isCallsTab) NotificationHelper.resetMissedCallCount()
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = {
                            if (isCallsTab && missedCallCount > 0) {
                                BadgedBox(badge = {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ) {
                                        Text(
                                            text = if (missedCallCount > 99) "99+"
                                                    else missedCallCount.toString()
                                        )
                                    }
                                }) {
                                    Icon(
                                        imageVector = if (currentPage == index)
                                            tab.selectedIcon
                                        else
                                            tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = if (currentPage == index)
                                        tab.selectedIcon
                                    else
                                        tab.unselectedIcon,
                                    contentDescription = tab.label
                                )
                            }
                        },
                        label = { Text(tab.label) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
            } // cierra NexusBottomBar Column
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().clipToBounds()) {
            // ═══ APP-WIDE BACKGROUND ═══
            when (backgroundConfig.type) {
                BackgroundType.IMAGE -> {
                    backgroundConfig.imageUri?.let { uri ->
                        AsyncImage(
                            model = uri,
                            contentDescription = "App Background",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
                BackgroundType.VIDEO -> {
                    backgroundConfig.videoUri?.let { uri ->
                        VideoBackgroundPlayer(
                            videoUri = uri,
                            modifier = Modifier.fillMaxSize(),
                            fallbackColor = DarkDeep
                        )
                    }
                }
                BackgroundType.SOLID_COLOR -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(parseHexColor(backgroundConfig.colorHex ?: "#0D0D1A"))
                    )
                }
                BackgroundType.GRADIENT -> {
                    val brush = linearGradientBrush(
                        gradientColors = backgroundConfig.gradientColors,
                        gradientAngle = backgroundConfig.gradientAngle
                    )
                    if (brush != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(brush)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(DarkDeep)
                        )
                    }
                }
                else -> {
                    // Default dark background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(DarkDeep)
                    )
                }
            }
            
            // ═══ OVERLAY LAYER — mejora legibilidad sobre fondos claros ═══
            if (backgroundConfig.type != BackgroundType.NONE) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = backgroundConfig.overlayAlpha))
                )
            }
            
            // ═══ CONTENT ON TOP OF BACKGROUND ═══
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                userScrollEnabled = true,
                beyondViewportPageCount = 1
            ) { page ->
                val originalIndex = tabOrder.getOrNull(page) ?: page
                when (originalIndex) {
                    0 -> HomeScreenRedesigned(navController = navController)
                    1 -> StoriesScreen(navController = navController)
                    2 -> CallsScreen(navController = navController)
                    3 -> {
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                        ProfileScreen(navController = navController, userId = userId)
                    }
                }
            }
        }
    }
}


