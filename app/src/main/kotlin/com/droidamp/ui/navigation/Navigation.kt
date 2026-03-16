package com.droidamp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.droidamp.ui.player.PlayerScreen
import com.droidamp.ui.player.PlayerViewModel
import com.droidamp.ui.library.LibraryScreen
import com.droidamp.ui.library.LibraryViewModel

sealed class Screen(val route: String) {
    object Player  : Screen("player")
    object Library : Screen("library")
}

@Composable
fun DroidampNavGraph(navController: NavHostController, playerViewModel: PlayerViewModel) {
    NavHost(navController = navController, startDestination = Screen.Player.route) {
        composable(Screen.Player.route) {
            PlayerScreen(
                viewModel = playerViewModel,
                onNavigateToLibrary = { navController.navigate(Screen.Library.route) },
            )
        }
        composable(Screen.Library.route) {
            val libraryViewModel: LibraryViewModel = hiltViewModel()
            LibraryScreen(
                libraryViewModel = libraryViewModel,
                playerViewModel  = playerViewModel,
                onNavigateBack   = { navController.popBackStack() },
            )
        }
    }
}
