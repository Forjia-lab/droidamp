package com.droidamp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.droidamp.ui.components.MiniPlayerBar
import com.droidamp.ui.library.LibraryScreen
import com.droidamp.ui.library.LibraryViewModel
import com.droidamp.ui.player.PlayerScreen
import com.droidamp.ui.player.PlayerViewModel
import com.droidamp.ui.search.SearchScreen
import com.droidamp.ui.search.SearchViewModel
import com.droidamp.ui.settings.SettingsScreen
import com.droidamp.ui.settings.SettingsViewModel
import com.droidamp.ui.theme.ThemeViewModel

sealed class Screen(val route: String, val label: String, val icon: String) {
    object Player   : Screen("player",   "Now Playing", "🎵")
    object Library  : Screen("library",  "Library",     "📚")
    object Search   : Screen("search",   "Search",      "🔍")
    object Settings : Screen("settings", "Settings",    "⚙")
}

private val bottomNavScreens = listOf(Screen.Player, Screen.Library, Screen.Search, Screen.Settings)

@Composable
fun DroidampNavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    themeViewModel: ThemeViewModel,
) {
    val playerState  by playerViewModel.playerState.collectAsState()
    val theme        by themeViewModel.theme.collectAsState()
    val backEntry    by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }

    Scaffold(
        containerColor = theme.bg,
        bottomBar = {
            if (currentRoute != Screen.Player.route) {
                Column {
                    MiniPlayerBar(
                        playerState = playerState,
                        theme       = theme,
                        coverArtUrl = playerState.currentTrack?.coverArtId,
                        onTap       = { navigateTo(Screen.Player.route) },
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext      = { playerViewModel.next() },
                    )
                    NavigationBar(containerColor = theme.panel) {
                        bottomNavScreens.forEach { screen ->
                            val selected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = selected,
                                onClick  = { navigateTo(screen.route) },
                                icon     = { Text(screen.icon, fontSize = 16.sp) },
                                label    = {
                                    Text(screen.label, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor   = theme.accent,
                                    selectedTextColor   = theme.accent,
                                    unselectedIconColor = theme.fg2,
                                    unselectedTextColor = theme.fg2,
                                    indicatorColor      = theme.accent.copy(alpha = 0.15f),
                                ),
                            )
                        }
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController      = navController,
            startDestination   = Screen.Player.route,
            modifier           = Modifier.padding(padding),
            enterTransition    = { fadeIn(tween(180)) },
            exitTransition     = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition  = { fadeOut(tween(180)) },
        ) {
            composable(Screen.Player.route) {
                PlayerScreen(
                    viewModel          = playerViewModel,
                    themeViewModel     = themeViewModel,
                    onNavigateToLibrary  = { navigateTo(Screen.Library.route) },
                    onNavigateToSettings = { navigateTo(Screen.Settings.route) },
                )
            }
            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(
                    libraryViewModel   = libraryViewModel,
                    playerViewModel    = playerViewModel,
                    themeViewModel     = themeViewModel,
                    onNavigateToPlayer = { navigateTo(Screen.Player.route) },
                )
            }
            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = hiltViewModel()
                SearchScreen(
                    viewModel          = searchViewModel,
                    playerViewModel    = playerViewModel,
                    themeViewModel     = themeViewModel,
                    onNavigateToPlayer = { navigateTo(Screen.Player.route) },
                )
            }
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = settingsViewModel, themeViewModel = themeViewModel)
            }
        }
    }
}
