package com.droidamp.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import com.droidamp.ui.navigation.DroidampNavGraph
import com.droidamp.ui.player.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val playerViewModel: PlayerViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { DroidampRoot(playerViewModel) }
    }
}

@Composable
fun DroidampRoot(playerViewModel: PlayerViewModel) {
    val navController = rememberNavController()
    DroidampNavGraph(navController = navController, playerViewModel = playerViewModel)
}
