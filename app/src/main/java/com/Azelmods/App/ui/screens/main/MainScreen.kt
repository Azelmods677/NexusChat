package com.Azelmods.App.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.Azelmods.App.data.preferences.ThemePreferences
import com.Azelmods.App.ui.navigation.Screen
import com.Azelmods.App.ui.screens.calls.CallsScreen
import com.Azelmods.App.ui.screens.home.HomeScreenRedesigned
import com.Azelmods.App.ui.screens.profile.ProfileScreen
import com.Azelmods.App.ui.screens.stories.StoriesScreen
import com.Azelmods.App.ui.theme.DarkSurface
import com.Azelmods.App.ui.theme.Purple
import kotlinx.coroutines.launch

data class TabItem(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

sealed class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    object Chats : BottomNavItem(Screen.Home.route, Icons.AutoMirrored.Filled.Chat, "Chats")
    object Stories : BottomNavItem(Screen.Stories.route, Icons.Default.AutoStories, "Stories")
    object Calls : BottomNavItem(Screen.Calls.route, Icons.Default.Call, "Calls")
    object Profile : BottomNavItem("profile_main", Icons.Default.Person, "Profile")
}

@Composable
fun MainScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val themePrefs = remember { ThemePreferences(context) }
    
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
    
    // Get custom tab order from preferences
    val tabOrder = remember { themePrefs.getTabOrder() }
    val tabs = remember(tabOrder) {
        tabOrder.mapNotNull { index ->
            allTabs.getOrNull(index)
        }.takeIf { it.size == allTabs.size } ?: allTabs
    }
    
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { tabs.size }
    )
    val coroutineScope = rememberCoroutineScope()
    
    // Keep pager and nav bar in sync
    val currentPage = pagerState.currentPage
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (currentPage == index)
                                    tab.selectedIcon
                                else
                                    tab.unselectedIcon,
                                contentDescription = tab.label
                            )
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
        }
    ) { innerPadding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            userScrollEnabled = true,      // ← enables swipe
            beyondViewportPageCount = 1    // preload adjacent pages
        ) { page ->
            // Map the page index to the original tab index
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
