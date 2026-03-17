package com.droidamp.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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

    // Notification permission is required on Android 13+ for the media notification to appear
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* no-op */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        setContent { DroidampRoot(playerViewModel, themeViewModel) }
    }
}

@Composable
fun DroidampRoot(playerViewModel: PlayerViewModel, themeViewModel: ThemeViewModel) {
    val navController = rememberNavController()
    DroidampNavGraph(navController = navController, playerViewModel = playerViewModel, themeViewModel = themeViewModel)
}
