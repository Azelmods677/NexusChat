package com.Azelmods.App.ui.screens.main
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.sharp.*
import androidx.compose.material.icons.twotone.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.automirrored.sharp.*
import androidx.compose.material.icons.automirrored.twotone.*
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
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

    val navIconSet by com.Azelmods.App.ui.components.ChatLook.navIconSet.collectAsState()
    val allTabs = remember(navIconSet) { tabsFor(navIconSet) }

    val missedCallCount by NotificationHelper.missedCallCount.collectAsStateWithLifecycle()

    val savedTabOrder = remember { themePrefs.getTabOrder() }
    val hideStoriesTab by com.Azelmods.App.ui.components.ChatLook.hideStoriesTab.collectAsState()
    val tabOrder = remember(savedTabOrder, hideStoriesTab) {
        val complete = savedTabOrder.takeIf { it.size == allTabs.size } ?: allTabs.indices.toList()
        if (hideStoriesTab) complete.filter { it != 1 } else complete
    }
    val tabs = remember(tabOrder, allTabs) { tabOrder.mapNotNull { index -> allTabs.getOrNull(index) } }

    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    val currentPage = pagerState.currentPage

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        containerColor = NexusTokens.Color.BgBase,
        bottomBar = {
            val navStyle by com.Azelmods.App.ui.components.ChatLook.navBarStyle.collectAsState()
            AppBottomBar(
                navStyle = navStyle,
                tabs = tabs,
                currentPage = currentPage,
                missedCallCount = missedCallCount,
                onSelect = { index ->
                    if (tabs[index].route == Screen.Calls.route) NotificationHelper.resetMissedCallCount()
                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                },
                onSettings = { navController.navigate(Screen.Settings.route) }
            )
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

/** La barra inferior elegida. La usan la pantalla principal y la vista previa del estudio. */
@Composable
internal fun AppBottomBar(
    navStyle: com.Azelmods.App.ui.components.NavBarStyle,
    tabs: List<TabItem>,
    currentPage: Int,
    missedCallCount: Int,
    onSelect: (Int) -> Unit,
    onSettings: () -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        if (navStyle == com.Azelmods.App.ui.components.NavBarStyle.CENTER) {
            CenterNavBar(tabs, currentPage, missedCallCount, onSelect)
        } else if (navStyle == com.Azelmods.App.ui.components.NavBarStyle.PHOTO) {
            PhotoNavBar(tabs, currentPage, missedCallCount, onSelect) { onSettings() }
        } else if (navStyle == com.Azelmods.App.ui.components.NavBarStyle.ACCESS) {
            AccessNavBar(tabs, currentPage, missedCallCount, onSelect) { onSettings() }
        } else if (navStyle == com.Azelmods.App.ui.components.NavBarStyle.LINE) {
            LineNavBar(tabs, currentPage, missedCallCount, onSelect)
        } else if (navStyle == com.Azelmods.App.ui.components.NavBarStyle.PANEL) {
            PanelNavBar(tabs, currentPage, missedCallCount, onSelect)
        } else if (navStyle == com.Azelmods.App.ui.components.NavBarStyle.FLOATING) {
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
                                onSelect(index)
                            }
                            .animateContentSize()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = com.Azelmods.App.i18n.tr(tab.label),
                            tint = if (selected) MaterialTheme.colorScheme.primary else NexusTokens.Color.TextMuted
                        )
                        if (selected) {
                            Spacer(Modifier.width(8.dp))
                            Text(com.Azelmods.App.i18n.tr(tab.label), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
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
                        onSelect(index)
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
                        { Text(com.Azelmods.App.i18n.tr(tab.label), style = MaterialTheme.typography.labelMedium) }
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

/**
 * Aro central: una cápsula de texto a cada lado (36 dp, esquinas de 5 dp, negrita de 13 sp) y, en una
 * caja central de 96 x 76 dp, el botón de chats dentro de un aro de seis segmentos que gira cada 2 s.
 */
@Composable
private fun CenterNavBar(
    tabs: List<TabItem>,
    currentPage: Int,
    missedCalls: Int,
    onSelect: (Int) -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val centerIndex = tabs.indexOfFirst { it.route == Screen.Home.route }.coerceAtLeast(0)
    val sides = tabs.indices.filter { it != centerIndex && tabs[it].route != "profile_main" }
    val spin = androidx.compose.animation.core.rememberInfiniteTransition(label = "aroCentral")
    val angle by spin.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            androidx.compose.animation.core.tween(2_000, easing = androidx.compose.animation.core.LinearEasing)
        ),
        label = "giroAro"
    )
    val press = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val pressed by press.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = androidx.compose.animation.core.tween(200),
        label = "pulsarAro"
    )
    Row(
        Modifier
            .fillMaxWidth()
            .background(NexusTokens.Color.BgBase)
            .navigationBarsPadding()
            .height(76.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.weight(1f).padding(start = 13.dp), contentAlignment = Alignment.Center) {
            sides.getOrNull(0)?.let { CenterNavPill(tabs[it], currentPage == it, missedCalls) { onSelect(it) } }
        }
        Box(
            Modifier
                .size(width = 96.dp, height = 76.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .drawBehind {
                    val box = minOf(size.width, size.height)
                    val side = box * 0.8f
                    val topLeft = androidx.compose.ui.geometry.Offset((size.width - side) / 2f, (size.height - side) / 2f)
                    val arcSize = androidx.compose.ui.geometry.Size(side, side)
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = box * 0.025f)
                    drawArc(accent.copy(alpha = 0.2f), 0f, 360f, false, topLeft, arcSize, style = stroke)
                    val paint = androidx.compose.ui.graphics.Brush.linearGradient(listOf(accent, Color.White))
                    rotate(angle) {
                        repeat(6) { segment ->
                            drawArc(paint, segment * 60f, 48f, false, topLeft, arcSize, style = stroke)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(58.dp)
                    .clip(androidx.compose.foundation.shape.CircleShape)
                    .clickable(interactionSource = press, indication = null) { onSelect(centerIndex) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (currentPage == centerIndex) tabs[centerIndex].selectedIcon else tabs[centerIndex].unselectedIcon,
                    contentDescription = com.Azelmods.App.i18n.tr(tabs[centerIndex].label),
                    tint = accent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Box(Modifier.weight(1f).padding(end = 13.dp), contentAlignment = Alignment.Center) {
            sides.getOrNull(1)?.let { CenterNavPill(tabs[it], currentPage == it, missedCalls) { onSelect(it) } }
        }
    }
}

@Composable
private fun CenterNavPill(tab: TabItem, selected: Boolean, missedCalls: Int, onClick: () -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    val badge = tab.route == Screen.Calls.route && missedCalls > 0
    Box(
        Modifier
            .height(36.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(5.dp))
            .background(if (selected) accent.copy(alpha = 0.22f) else NexusTokens.Color.BgHigh)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            com.Azelmods.App.i18n.tr(tab.label) + if (badge) " · $missedCalls" else "",
            fontSize = 13.sp,
            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
            color = accent,
            maxLines = 1
        )
    }
}

/** Accesos: barra de 56 dp con iconos de 23 dp, ajustes incluidos, y un botón redondo de 41 dp al final. */
@Composable
private fun AccessNavBar(
    tabs: List<TabItem>,
    currentPage: Int,
    missedCalls: Int,
    onSelect: (Int) -> Unit,
    onSettings: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val profile = tabs.indexOfFirst { it.route == "profile_main" }
    Row(
        Modifier
            .fillMaxWidth()
            .background(NexusTokens.Color.BgHigh)
            .navigationBarsPadding()
            .padding(bottom = 2.dp)
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        tabs.indices.filter { it != profile }.forEach { index ->
            val selected = currentPage == index
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center
            ) {
                NavTabIcon(tabs[index], selected, missedCalls, if (selected) accent else accent.copy(alpha = 0.6f), 23.dp)
            }
        }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = com.Azelmods.App.i18n.tr("Ajustes"),
                tint = accent.copy(alpha = 0.6f),
                modifier = Modifier.size(23.dp)
            )
        }
        if (profile >= 0) {
            val selected = currentPage == profile
            val shape = androidx.compose.foundation.shape.CircleShape
            Box(
                Modifier
                    .padding(horizontal = 8.dp)
                    .size(41.dp)
                    .clip(shape)
                    .background(if (selected) accent else NexusTokens.Color.BgElevated)
                    .border(1.dp, NexusTokens.Color.BgElevated, shape)
                    .clickable { onSelect(profile) },
                contentAlignment = Alignment.Center
            ) {
                NavTabIcon(tabs[profile], selected, 0, if (selected) MaterialTheme.colorScheme.onPrimary else accent, 19.dp)
            }
        }
    }
}

/** Las pestañas con el juego de iconos elegido: icono de la pestaña activa e inactiva. */
internal fun tabsFor(set: com.Azelmods.App.ui.components.NavIconSet): List<TabItem> {
    val icons: List<Pair<androidx.compose.ui.graphics.vector.ImageVector, androidx.compose.ui.graphics.vector.ImageVector>> = when (set) {
        com.Azelmods.App.ui.components.NavIconSet.OUTLINED -> listOf(
            Icons.AutoMirrored.Filled.Chat to Icons.AutoMirrored.Outlined.Chat, Icons.Filled.AutoStories to Icons.Outlined.AutoStories,
            Icons.Filled.Call to Icons.Outlined.Call, Icons.Filled.Person to Icons.Outlined.Person
        )
        com.Azelmods.App.ui.components.NavIconSet.FILLED -> listOf(
            Icons.AutoMirrored.Filled.Chat to Icons.AutoMirrored.Filled.Chat, Icons.Filled.AutoStories to Icons.Filled.AutoStories,
            Icons.Filled.Call to Icons.Filled.Call, Icons.Filled.Person to Icons.Filled.Person
        )
        com.Azelmods.App.ui.components.NavIconSet.ROUNDED -> listOf(
            Icons.AutoMirrored.Rounded.Chat to Icons.AutoMirrored.Rounded.Chat, Icons.Rounded.AutoStories to Icons.Rounded.AutoStories,
            Icons.Rounded.Call to Icons.Rounded.Call, Icons.Rounded.Person to Icons.Rounded.Person
        )
        com.Azelmods.App.ui.components.NavIconSet.SHARP -> listOf(
            Icons.AutoMirrored.Sharp.Chat to Icons.AutoMirrored.Sharp.Chat, Icons.Sharp.AutoStories to Icons.Sharp.AutoStories,
            Icons.Sharp.Call to Icons.Sharp.Call, Icons.Sharp.Person to Icons.Sharp.Person
        )
        com.Azelmods.App.ui.components.NavIconSet.TWO_TONE -> listOf(
            Icons.AutoMirrored.TwoTone.Chat to Icons.AutoMirrored.TwoTone.Chat, Icons.TwoTone.AutoStories to Icons.TwoTone.AutoStories,
            Icons.TwoTone.Call to Icons.TwoTone.Call, Icons.TwoTone.Person to Icons.TwoTone.Person
        )
        com.Azelmods.App.ui.components.NavIconSet.PLANE -> listOf(
            Icons.AutoMirrored.Filled.Send to Icons.AutoMirrored.Outlined.Send, Icons.Filled.DonutLarge to Icons.Outlined.DonutLarge,
            Icons.Filled.PhoneInTalk to Icons.Outlined.PhoneInTalk, Icons.Filled.AccountCircle to Icons.Outlined.AccountCircle
        )
        com.Azelmods.App.ui.components.NavIconSet.BUBBLE -> listOf(
            Icons.Filled.ChatBubble to Icons.Outlined.ChatBubbleOutline, Icons.Filled.EditNote to Icons.Outlined.EditNote,
            Icons.Filled.Call to Icons.Outlined.Call, Icons.Filled.Workspaces to Icons.Outlined.Workspaces
        )
    }
    val base = listOf(
        Screen.Home.route to "Chats", Screen.Stories.route to "Historias",
        Screen.Calls.route to "Llamadas", "profile_main" to "Perfil"
    )
    return base.mapIndexed { index, (route, label) -> TabItem(route, label, icons[index].first, icons[index].second) }
}

/** Icono de pestaña con el aviso de llamadas perdidas. */
@Composable
private fun NavTabIcon(tab: TabItem, selected: Boolean, missedCalls: Int, tint: Color, size: androidx.compose.ui.unit.Dp) {
    val icon = if (selected) tab.selectedIcon else tab.unselectedIcon
    val label = com.Azelmods.App.i18n.tr(tab.label)
    if (tab.route == Screen.Calls.route && missedCalls > 0) {
        BadgedBox(badge = {
            Badge(containerColor = MaterialTheme.colorScheme.error) {
                Text(if (missedCalls > 99) "99+" else missedCalls.toString())
            }
        }) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size))
        }
    } else {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(size))
    }
}

/**
 * Barra de 50 dp sobre superficie, con iconos de 22 dp siempre en el color de acento y, en el centro,
 * tu foto de 34 dp dentro de un aro de 41 dp y 2 dp de grosor, 3 dp por encima del borde inferior.
 */
@Composable
private fun PhotoNavBar(
    tabs: List<TabItem>,
    currentPage: Int,
    missedCalls: Int,
    onSelect: (Int) -> Unit,
    onSettings: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val me = com.Azelmods.App.data.profile.rememberMyProfile()
    val center = tabs.indexOfFirst { it.route == "profile_main" }
    val others = tabs.indices.filter { it != center }
    val leftCount = (others.size + 1) / 2
    val shape = androidx.compose.foundation.shape.CircleShape

    @Composable
    fun cell(index: Int, modifier: Modifier) {
        val selected = currentPage == index
        Box(modifier.clickable { onSelect(index) }, contentAlignment = Alignment.Center) {
            NavTabIcon(tabs[index], selected, missedCalls, if (selected) accent else accent.copy(alpha = 0.6f), 22.dp)
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .background(NexusTokens.Color.BgHigh)
            .navigationBarsPadding()
            .height(50.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        others.take(leftCount).forEach { cell(it, Modifier.weight(1f).fillMaxHeight()) }
        if (center >= 0) {
            Box(
                Modifier
                    .padding(bottom = 3.dp)
                    .size(41.dp)
                    .clip(shape)
                    .border(2.dp, accent, shape)
                    .clickable { onSelect(center) },
                contentAlignment = Alignment.Center
            ) {
                com.Azelmods.App.ui.components.UserAvatar(name = me.name, photoUrl = me.photoUrl, size = 34.dp)
            }
        }
        others.drop(leftCount).forEach { cell(it, Modifier.weight(1f).fillMaxHeight()) }
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(onClick = onSettings),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.Settings,
                contentDescription = com.Azelmods.App.i18n.tr("Ajustes"),
                tint = accent.copy(alpha = 0.6f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Pestañas de texto de 48 dp con una línea de 4 dp bajo la elegida. */
@Composable
private fun LineNavBar(tabs: List<TabItem>, currentPage: Int, missedCalls: Int, onSelect: (Int) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    NexusDivider()
    Row(
        Modifier
            .fillMaxWidth()
            .background(NexusTokens.Color.BgBase)
            .navigationBarsPadding()
            .height(48.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = currentPage == index
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    val badge = tab.route == Screen.Calls.route && missedCalls > 0
                    Text(
                        com.Azelmods.App.i18n.tr(tab.label) + if (badge) " · $missedCalls" else "",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                        color = if (selected) accent else NexusTokens.Color.TextMuted,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (selected) accent else Color.Transparent)
                )
            }
        }
    }
}

/** Panel con las esquinas superiores de 20 dp, pestañas de texto e indicador de 4 dp. */
@Composable
private fun PanelNavBar(tabs: List<TabItem>, currentPage: Int, missedCalls: Int, onSelect: (Int) -> Unit) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(NexusTokens.Color.BgHigh)
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 12.dp)
            .height(48.dp)
    ) {
        tabs.forEachIndexed { index, tab ->
            val selected = currentPage == index
            val badge = tab.route == Screen.Calls.route && missedCalls > 0
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onSelect(index) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        com.Azelmods.App.i18n.tr(tab.label) + if (badge) " · $missedCalls" else "",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal,
                        color = if (selected) accent else NexusTokens.Color.TextSecondary,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }
                Box(
                    Modifier
                        .padding(horizontal = 14.dp)
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                        .background(if (selected) accent else Color.Transparent)
                )
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
