package com.droidamp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.droidamp.ui.navigation.DroidampNavGraph
import com.droidamp.ui.player.PlayerViewModel
import com.droidamp.ui.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()
    private val themeViewModel: ThemeViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // RECORD_AUDIO permission is requested in PlayerScreen via rememberLauncherForActivityResult
        setContent { DroidampRoot(playerViewModel, themeViewModel) }
    }
}

@Composable
fun DroidampRoot(playerViewModel: PlayerViewModel, themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    DroidampNavGraph(navController = navController, playerViewModel = playerViewModel, themeViewModel = themeViewModel)
}
