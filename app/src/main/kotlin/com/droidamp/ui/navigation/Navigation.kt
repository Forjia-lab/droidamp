package com.droidamp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.droidamp.ui.library.BrowseMode
import com.droidamp.ui.library.LibraryScreen
import com.droidamp.ui.library.LibraryViewModel
import com.droidamp.ui.player.PlayerScreen
import com.droidamp.ui.player.PlayerViewModel
import com.droidamp.ui.search.SearchScreen
import com.droidamp.ui.search.SearchViewModel
import com.droidamp.ui.settings.SettingsViewModel
import com.droidamp.ui.theme.ThemeViewModel

// ─────────────────────────────────────────────────────────────
//  Navigation — bottom nav: PLAY | BROWSE | LIBRARY | SEARCH
//  PLAY     = main now-playing screen (PlayerScreen)
//  BROWSE   = full library browser (LibraryScreen)
//  LIBRARY  = library (LibraryScreen — playlists focus)
//  SEARCH   = search screen (SearchScreen)
// ─────────────────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String, val icon: String) {
    object Play         : Screen("play",          "PLAY",    "▶")
    object Browse       : Screen("browse",         "BROWSE",  "⊞")
    object Library      : Screen("library",        "LIBRARY", "≡")
    object Search       : Screen("search",         "SEARCH",  "⌕")
    object LocalLibrary : Screen("local_library",  "",        "")
}

private val bottomNavScreens = listOf(Screen.Play, Screen.Browse, Screen.Library, Screen.Search)

@Composable
fun DroidampNavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    themeViewModel: ThemeViewModel,
) {
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
            Column {
                HorizontalDivider(color = theme.border, thickness = 0.5.dp)
                NavigationBar(
                    containerColor = theme.panel,
                    modifier       = Modifier.height(56.dp),
                    windowInsets   = WindowInsets(0),
                ) {
                    bottomNavScreens.forEach { screen ->
                        val selected = currentRoute == screen.route
                        NavigationBarItem(
                            selected = selected,
                            onClick  = { navigateTo(screen.route) },
                            icon     = { Text(screen.icon, fontSize = 16.sp) },
                            label    = {
                                Text(screen.label, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
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
        },
    ) { padding ->
        NavHost(
            navController      = navController,
            startDestination   = Screen.Play.route,
            modifier           = Modifier.padding(padding),
            enterTransition    = { fadeIn(tween(180)) },
            exitTransition     = { fadeOut(tween(180)) },
            popEnterTransition = { fadeIn(tween(180)) },
            popExitTransition  = { fadeOut(tween(180)) },
        ) {
            composable(Screen.Play.route) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                val searchViewModel:  SearchViewModel  = hiltViewModel()
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                PlayerScreen(
                    playerViewModel          = playerViewModel,
                    themeViewModel           = themeViewModel,
                    libraryViewModel         = libraryViewModel,
                    searchViewModel          = searchViewModel,
                    settingsViewModel        = settingsViewModel,
                    onNavigateToLocalLibrary = { navController.navigate(Screen.LocalLibrary.route) },
                )
            }
            composable(Screen.Browse.route) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(
                    libraryViewModel = libraryViewModel,
                    playerViewModel  = playerViewModel,
                    themeViewModel   = themeViewModel,
                    mode             = BrowseMode.All,
                    onNavigateBack   = { navigateTo(Screen.Play.route) },
                )
            }
            composable(Screen.Library.route) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(
                    libraryViewModel = libraryViewModel,
                    playerViewModel  = playerViewModel,
                    themeViewModel   = themeViewModel,
                    mode             = BrowseMode.Playlists,
                    onNavigateBack   = { navigateTo(Screen.Play.route) },
                )
            }
            composable(Screen.LocalLibrary.route) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(
                    libraryViewModel = libraryViewModel,
                    playerViewModel  = playerViewModel,
                    themeViewModel   = themeViewModel,
                    mode             = BrowseMode.LocalOnly,
                    onNavigateBack   = { navController.popBackStack() },
                )
            }
            composable(Screen.Search.route) {
                val searchViewModel: SearchViewModel = hiltViewModel()
                SearchScreen(
                    viewModel          = searchViewModel,
                    playerViewModel    = playerViewModel,
                    themeViewModel     = themeViewModel,
                    onNavigateToPlayer = { navigateTo(Screen.Play.route) },
                )
            }
        }
    }
}
