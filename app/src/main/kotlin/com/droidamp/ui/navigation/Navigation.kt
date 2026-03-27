package com.droidamp.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.droidamp.data.local.db.GigBagWithCount
import com.droidamp.domain.model.Track
import com.droidamp.ui.components.MiniPlayerBar
import com.droidamp.ui.gigbag.GigBagViewModel
import com.droidamp.ui.library.BrowseMode
import com.droidamp.ui.library.LibraryScreen
import com.droidamp.ui.library.LibraryViewModel
import com.droidamp.ui.player.PlayerScreen
import com.droidamp.ui.player.PlayerViewModel
import com.droidamp.ui.settings.SettingsScreen
import com.droidamp.ui.settings.SettingsViewModel
import com.droidamp.ui.sources.SourcesScreen
import com.droidamp.ui.theme.ThemeViewModel
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────
//  Navigation — bottom nav: PLAY | SOURCES | BROWSE | SETTINGS
// ─────────────────────────────────────────────────────────────

sealed class Screen(val route: String, val label: String, val icon: String) {
    object Play         : Screen("play",          "PLAY",     "▶")
    object Sources      : Screen("sources",        "SOURCES",  "⊞")
    object Browse       : Screen("browse",         "BROWSE",   "≡")
    object Settings     : Screen("settings",       "SETTINGS", "⚙")
    object LocalLibrary : Screen("local_library",  "",         "")
}

private val bottomNavScreens = listOf(Screen.Play, Screen.Sources, Screen.Browse, Screen.Settings)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DroidampNavGraph(
    navController: NavHostController,
    playerViewModel: PlayerViewModel,
    themeViewModel: ThemeViewModel,
) {
    val theme        by themeViewModel.theme.collectAsState()
    val playerState  by playerViewModel.playerState.collectAsState()
    val backEntry    by navController.currentBackStackEntryAsState()
    val currentRoute = backEntry?.destination?.route

    // Shared GigBagViewModel — activity-scoped so it persists across nav destinations
    val gigBagViewModel: GigBagViewModel = hiltViewModel()

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarMsg by gigBagViewModel.snackbarMessage.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            scope.launch {
                snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
                gigBagViewModel.clearSnackbar()
            }
        }
    }

    // "Add to Gig Bag" sheet — triggered by MiniPlayerBar long-press
    var showGigBagSheet by remember { mutableStateOf(false) }

    fun navigateTo(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState    = true
        }
    }

    val showMiniBar = currentRoute != Screen.Play.route && currentRoute != Screen.LocalLibrary.route

    if (showGigBagSheet) {
        AddToGigBagSheet(
            gigBagViewModel = gigBagViewModel,
            currentTrack    = playerState.currentTrack,
            theme           = theme,
            onDismiss       = { showGigBagSheet = false },
        )
    }

    Scaffold(
        containerColor = theme.bg,
        snackbarHost   = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData    = data,
                    containerColor  = theme.panel,
                    contentColor    = theme.fg,
                    actionColor     = theme.accent,
                    shape           = RoundedCornerShape(6.dp),
                )
            }
        },
        bottomBar = {
            Column {
                if (showMiniBar) {
                    MiniPlayerBar(
                        playerState = playerState,
                        theme       = theme,
                        coverArtUrl = playerState.currentTrack?.coverArtId,
                        onTap       = { navigateTo(Screen.Play.route) },
                        onPlayPause = { playerViewModel.togglePlayPause() },
                        onNext      = { playerViewModel.next() },
                        onLongPress = { if (playerState.currentTrack != null) showGigBagSheet = true },
                    )
                }
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
                PlayerScreen(
                    playerViewModel = playerViewModel,
                    themeViewModel  = themeViewModel,
                )
            }
            composable(Screen.Sources.route) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SourcesScreen(
                    settingsViewModel        = settingsViewModel,
                    libraryViewModel         = libraryViewModel,
                    themeViewModel           = themeViewModel,
                    onNavigateToLocalLibrary = { navController.navigate(Screen.LocalLibrary.route) },
                )
            }
            composable(Screen.Browse.route) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(
                    libraryViewModel = libraryViewModel,
                    playerViewModel  = playerViewModel,
                    themeViewModel   = themeViewModel,
                    gigBagViewModel  = gigBagViewModel,
                    mode             = BrowseMode.All,
                    onNavigateBack   = {},
                    onTrackPlayed    = {
                        navController.navigate(Screen.Play.route) {
                            popUpTo(Screen.Play.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                )
            }
            composable(Screen.LocalLibrary.route) {
                val libraryViewModel: LibraryViewModel = hiltViewModel()
                LibraryScreen(
                    libraryViewModel = libraryViewModel,
                    playerViewModel  = playerViewModel,
                    themeViewModel   = themeViewModel,
                    gigBagViewModel  = gigBagViewModel,
                    mode             = BrowseMode.LocalOnly,
                    onNavigateBack   = { navController.popBackStack() },
                    onTrackPlayed    = {
                        navController.navigate(Screen.Play.route) {
                            popUpTo(Screen.Play.route) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                )
            }
            composable(Screen.Settings.route) {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(
                    viewModel       = settingsViewModel,
                    themeViewModel  = themeViewModel,
                    playerViewModel = playerViewModel,
                )
            }
        }
    }
}

// ─── "Add to Gig Bag" bottom sheet ───────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddToGigBagSheet(
    gigBagViewModel: GigBagViewModel,
    currentTrack:    Track?,
    theme:           com.droidamp.ui.theme.DroidTheme,
    onDismiss:       () -> Unit,
) {
    val bags by gigBagViewModel.bagsWithCount.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showNewBagDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = theme.panel,
        contentColor     = theme.fg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 8.dp),
        ) {
            Text(
                text       = "ADD TO GIG BAG",
                color      = theme.fg2,
                fontSize   = 9.sp,
                fontFamily = FontFamily.Monospace,
                modifier   = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            )
            currentTrack?.let { track ->
                Text(
                    text       = track.title,
                    color      = theme.fg,
                    fontSize   = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp),
                )
            }
            HorizontalDivider(color = theme.border, thickness = 0.5.dp)

            if (bags.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("No gig bags yet", color = theme.fg2, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                }
            } else {
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(bags) { bag ->
                        GigBagSheetRow(
                            bag   = bag,
                            theme = theme,
                            onClick = {
                                currentTrack?.let { track ->
                                    gigBagViewModel.addTrackToBag(bag.bag.id, bag.bag.name, track)
                                }
                                onDismiss()
                            },
                        )
                    }
                }
            }

            HorizontalDivider(color = theme.border, thickness = 0.5.dp)
            // "+ New Gig Bag"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showNewBagDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("+", color = theme.accent, fontSize = 18.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(end = 12.dp))
                Text("New Gig Bag", color = theme.accent, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold)
            }
        }
    }

    if (showNewBagDialog) {
        NewBagDialog(
            theme     = theme,
            onConfirm = { name ->
                currentTrack?.let { track ->
                    gigBagViewModel.createBagAndAddTrack(name, track)
                }
                showNewBagDialog = false
                onDismiss()
            },
            onDismiss = { showNewBagDialog = false },
        )
    }
}

@Composable
private fun GigBagSheetRow(
    bag:     GigBagWithCount,
    theme:   com.droidamp.ui.theme.DroidTheme,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(theme.accent),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(bag.bag.name, color = theme.fg, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold)
            Text("${bag.trackCount} tracks", color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Text("›", color = theme.fg2, fontSize = 14.sp)
    }
    HorizontalDivider(color = theme.border, thickness = 0.5.dp)
}

// ─── Shared dialog for creating a new bag ─────────────────────

@Composable
internal fun NewBagDialog(
    theme:     com.droidamp.ui.theme.DroidTheme,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = theme.panel,
        titleContentColor = theme.fg,
        textContentColor  = theme.fg2,
        title = {
            Text("New Gig Bag", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        },
        text = {
            BasicTextField(
                value         = name,
                onValueChange = { name = it },
                singleLine    = true,
                textStyle     = TextStyle(color = theme.fg, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                cursorBrush   = SolidColor(theme.accent),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(theme.surface, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                    ) {
                        if (name.isEmpty()) {
                            Text("Bag name…", color = theme.fg2, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                        }
                        inner()
                    }
                },
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (name.isNotBlank()) onConfirm(name) },
            ) {
                Text("Create", color = theme.accent, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = theme.fg2, fontFamily = FontFamily.Monospace)
            }
        },
    )
}
