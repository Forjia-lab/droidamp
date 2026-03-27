package com.droidamp.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.droidamp.data.local.db.GigBagTrackEntity
import com.droidamp.data.local.db.GigBagWithCount
import com.droidamp.domain.model.Album
import com.droidamp.domain.model.Artist
import com.droidamp.domain.model.Track
import com.droidamp.domain.model.TrackSource
import com.droidamp.ui.gigbag.GigBagViewModel
import com.droidamp.ui.gigbag.toTrack
import com.droidamp.ui.navigation.NewBagDialog
import com.droidamp.ui.player.PlayerViewModel
import com.droidamp.ui.theme.DroidTheme
import com.droidamp.ui.theme.ThemeViewModel

// ─────────────────────────────────────────────────────────────
//  BrowseMode controls which content LibraryScreen shows:
//    All       — Navidrome + Local combined (BROWSE tab)
//    LocalOnly — Local files only (LOCAL STORAGE screen)
//    Playlists — kept for compat, treated same as All
// ─────────────────────────────────────────────────────────────

enum class BrowseMode { All, LocalOnly, Playlists }

private enum class BrowseFilter { All, Playlists, GigBags, Albums, Artists }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel:  PlayerViewModel,
    themeViewModel:   ThemeViewModel,
    gigBagViewModel:  GigBagViewModel,
    mode:             BrowseMode = BrowseMode.All,
    onNavigateBack:   () -> Unit,
    onTrackPlayed:    () -> Unit = {},
) {
    val uiState by libraryViewModel.uiState.collectAsState()
    val theme   by themeViewModel.theme.collectAsState()

    var browseTab    by remember { mutableStateOf(BrowseTab.Albums) }
    var browseFilter by remember { mutableStateOf(BrowseFilter.All) }
    var searchQuery  by remember { mutableStateOf("") }

    // Gig Bag drill-down state (local to Browse All mode)
    var selectedGigBag by remember { mutableStateOf<GigBagWithCount?>(null) }

    // Gig Bag dialog states
    var showCreateBagDialog by remember { mutableStateOf(false) }
    var renameTarget        by remember { mutableStateOf<GigBagWithCount?>(null) }
    var deleteTarget        by remember { mutableStateOf<GigBagWithCount?>(null) }

    // Tell GigBagViewModel which bag's tracks to load
    LaunchedEffect(selectedGigBag) {
        gigBagViewModel.selectBag(selectedGigBag?.bag?.id)
    }

    // Load data on first composition
    LaunchedEffect(mode) {
        when (mode) {
            BrowseMode.All, BrowseMode.Playlists -> libraryViewModel.loadBrowseData()
            BrowseMode.LocalOnly                 -> libraryViewModel.scanLocalMedia()
        }
    }

    LaunchedEffect(browseFilter) {
        if (browseFilter == BrowseFilter.Playlists) libraryViewModel.selectTab(LibraryTab.Playlists)
    }

    val combinedAlbums  = remember(uiState.albums, uiState.localAlbums) {
        (uiState.albums + uiState.localAlbums).sortedBy { it.name }
    }
    val combinedArtists = remember(uiState.artists, uiState.localArtists) {
        (uiState.artists + uiState.localArtists).sortedBy { it.name }
    }

    val screenTitle = when {
        selectedGigBag != null         -> selectedGigBag!!.bag.name
        uiState.selectedAlbum  != null -> uiState.selectedAlbum!!.name
        uiState.selectedArtist != null -> uiState.selectedArtist!!.name
        mode == BrowseMode.LocalOnly   -> "LOCAL STORAGE"
        browseFilter == BrowseFilter.GigBags -> "GIG BAGS"
        else                           -> "BROWSE"
    }

    // ── Dialogs ─────────────────────────────────────────────

    if (showCreateBagDialog) {
        NewBagDialog(
            theme     = theme,
            onConfirm = { name ->
                gigBagViewModel.createBag(name)
                showCreateBagDialog = false
            },
            onDismiss = { showCreateBagDialog = false },
        )
    }

    renameTarget?.let { bag ->
        var newName by remember(bag.bag.id) { mutableStateOf(bag.bag.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            containerColor   = theme.panel,
            titleContentColor = theme.fg,
            title = { Text("Rename", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text = {
                BasicTextField(
                    value         = newName,
                    onValueChange = { newName = it },
                    singleLine    = true,
                    textStyle     = TextStyle(color = theme.fg, fontSize = 14.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush   = SolidColor(theme.accent),
                    decorationBox = { inner ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(theme.surface, RoundedCornerShape(6.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        ) { inner() }
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) gigBagViewModel.renameBag(bag.bag, newName)
                    renameTarget = null
                }) { Text("Rename", color = theme.accent, fontFamily = FontFamily.Monospace) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancel", color = theme.fg2, fontFamily = FontFamily.Monospace)
                }
            },
        )
    }

    deleteTarget?.let { bag ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            containerColor   = theme.panel,
            titleContentColor = theme.fg,
            textContentColor  = theme.fg2,
            title = { Text("Delete Gig Bag?", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) },
            text  = { Text("\"${bag.bag.name}\" and its ${bag.trackCount} track(s) will be removed.", fontFamily = FontFamily.Monospace, fontSize = 12.sp) },
            confirmButton = {
                TextButton(onClick = {
                    gigBagViewModel.deleteBag(bag.bag.id)
                    if (selectedGigBag?.bag?.id == bag.bag.id) selectedGigBag = null
                    deleteTarget = null
                }) { Text("Delete", color = theme.red, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text("Cancel", color = theme.fg2, fontFamily = FontFamily.Monospace)
                }
            },
        )
    }

    // ── Main layout ─────────────────────────────────────────

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg),
    ) {
        // ── Header ──────────────────────────────────────────
        val showBack = selectedGigBag != null ||
                uiState.selectedAlbum != null || uiState.selectedArtist != null ||
                mode == BrowseMode.LocalOnly
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.panel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (showBack) {
                Text(
                    text     = "←",
                    color    = theme.accent,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .clickable {
                            when {
                                selectedGigBag != null         -> selectedGigBag = null
                                uiState.selectedAlbum  != null -> libraryViewModel.clearAlbumSelection()
                                uiState.selectedArtist != null -> libraryViewModel.clearArtistSelection()
                                else                           -> onNavigateBack()
                            }
                        }
                        .padding(end = 12.dp),
                )
            }
            Text(
                text       = screenTitle,
                color      = theme.accent,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier   = Modifier.weight(1f),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
            if (mode == BrowseMode.LocalOnly && uiState.localTrackCount > 0 &&
                uiState.selectedAlbum == null && uiState.selectedArtist == null) {
                Text(
                    "${uiState.localTrackCount} tracks",
                    color = theme.fg2, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                )
            }
        }

        // ── Search bar + filter chips (Browse All, not drilling down) ──
        val showSearchAndChips = mode != BrowseMode.LocalOnly &&
                uiState.selectedAlbum == null && uiState.selectedArtist == null &&
                selectedGigBag == null

        if (showSearchAndChips) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.surface)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⌕", color = theme.fg2, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
                BasicTextField(
                    value         = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier      = Modifier.weight(1f),
                    textStyle     = TextStyle(color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                    cursorBrush   = SolidColor(theme.accent),
                    singleLine    = true,
                    decorationBox = { inner ->
                        if (searchQuery.isEmpty()) {
                            Text("Search…", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                        inner()
                    },
                )
                if (searchQuery.isNotEmpty()) {
                    Text("✕", color = theme.fg2, fontSize = 12.sp,
                        modifier = Modifier.clickable { searchQuery = "" }.padding(start = 6.dp))
                }
            }

            LazyRow(
                modifier              = Modifier.fillMaxWidth().background(theme.panel),
                contentPadding        = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                val chipShape = RoundedCornerShape(6.dp)
                listOf(
                    BrowseFilter.All       to "All",
                    BrowseFilter.Playlists to "Playlists",
                    BrowseFilter.GigBags   to "Gig Bags",
                    BrowseFilter.Albums    to "Albums",
                    BrowseFilter.Artists   to "Artists",
                ).forEach { (filter, label) ->
                    item {
                        val active = browseFilter == filter
                        Box(
                            modifier = Modifier
                                .clip(chipShape)
                                .background(if (active) theme.accent else theme.surface)
                                .clickable { browseFilter = filter }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text       = label,
                                color      = if (active) theme.bg else theme.fg2,
                                fontSize   = 10.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        }

        // ── Tab bar for LocalOnly mode ───────────────────────
        val showLocalTabs = mode == BrowseMode.LocalOnly &&
                uiState.selectedAlbum == null && uiState.selectedArtist == null

        if (showLocalTabs) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.panel)
                    .padding(horizontal = 12.dp),
            ) {
                listOf("Albums" to BrowseTab.Albums, "Artists" to BrowseTab.Artists, "Tracks" to BrowseTab.Tracks)
                    .forEach { (label, tab) ->
                        val active = browseTab == tab
                        Text(
                            text       = label,
                            color      = if (active) theme.accent else theme.fg2,
                            fontSize   = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            modifier   = Modifier
                                .clickable { browseTab = tab }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                        )
                    }
            }
        }

        // ── Content ─────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                // Loading spinner
                (uiState.isLoading || uiState.isLocalScanning) &&
                        uiState.selectedAlbum == null && uiState.selectedArtist == null &&
                        selectedGigBag == null -> {
                    CircularProgressIndicator(color = theme.accent, modifier = Modifier.align(Alignment.Center))
                }

                // Gig Bag track list (drill-down)
                selectedGigBag != null -> {
                    val bagEntities by gigBagViewModel.selectedBagTracks.collectAsState()
                    val bagTracks = remember(bagEntities) { bagEntities.map { it.toTrack() } }
                    if (bagEntities.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("This bag is empty", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            itemsIndexed(bagEntities, key = { _, e -> e.id }) { idx, entity ->
                                GigBagTrackRow(
                                    track    = bagTracks[idx],
                                    theme    = theme,
                                    onTap    = {
                                        playerViewModel.playTracks(bagTracks, idx)
                                        onTrackPlayed()
                                    },
                                    onRemove = { gigBagViewModel.removeTrackEntry(entity.id) },
                                )
                            }
                        }
                    }
                }

                // Album track list (drill-down)
                uiState.selectedAlbum != null -> {
                    AlbumTrackList(
                        album        = uiState.selectedAlbum!!,
                        tracks       = uiState.selectedAlbumTracks,
                        theme        = theme,
                        onTrackClick = { idx ->
                            playerViewModel.playTracks(uiState.selectedAlbumTracks, idx)
                            onTrackPlayed()
                        },
                    )
                }

                // Artist album grid (drill-down)
                uiState.selectedArtist != null -> {
                    AlbumGrid(
                        albums          = uiState.selectedArtistAlbums,
                        theme           = theme,
                        showSourceBadge = (mode == BrowseMode.All),
                        onAlbumClick    = { libraryViewModel.loadAlbumTracks(it) },
                    )
                }

                // LocalOnly mode
                mode == BrowseMode.LocalOnly -> {
                    when {
                        uiState.localTrackCount == 0 -> {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No music found on device", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                        browseTab == BrowseTab.Albums -> {
                            AlbumGrid(albums = uiState.localAlbums, theme = theme, showSourceBadge = false) {
                                libraryViewModel.loadAlbumTracks(it)
                            }
                        }
                        browseTab == BrowseTab.Artists -> {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(uiState.localArtists) { artist ->
                                    ArtistRow(artist, theme, showSourceBadge = false) {
                                        libraryViewModel.loadArtistAlbums(artist)
                                    }
                                }
                            }
                        }
                        else -> {  // BrowseTab.Tracks
                            LazyColumn(Modifier.fillMaxSize()) {
                                itemsIndexed(uiState.localAllTracks) { idx, track ->
                                    TrackRow(track, theme, showSourceBadge = false) {
                                        playerViewModel.playTracks(uiState.localAllTracks, idx)
                                        onTrackPlayed()
                                    }
                                }
                            }
                        }
                    }
                }

                // Browse All — Playlists chip
                browseFilter == BrowseFilter.Playlists -> {
                    val filtered = if (searchQuery.isBlank()) uiState.playlists
                    else uiState.playlists.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No playlists", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(filtered) { pl ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { libraryViewModel.loadPlaylistTracks(pl) }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                ) {
                                    Text("≡", color = theme.accent, fontSize = 16.sp, modifier = Modifier.padding(end = 10.dp))
                                    Column {
                                        Text(pl.name, color = theme.fg, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                        Text("${pl.trackCount} tracks", color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                HorizontalDivider(color = theme.border, thickness = 0.5.dp)
                            }
                        }
                    }
                }

                // Browse All — Gig Bags chip
                browseFilter == BrowseFilter.GigBags -> {
                    val bags by gigBagViewModel.bagsWithCount.collectAsState()
                    val filtered = if (searchQuery.isBlank()) bags
                    else bags.filter { it.bag.name.contains(searchQuery, ignoreCase = true) }

                    Box(Modifier.fillMaxSize()) {
                        if (filtered.isEmpty()) {
                            Column(
                                modifier = Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Text("No gig bags yet", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                Spacer(Modifier.height(8.dp))
                                Text("Tap + to create one", color = theme.fg2.copy(alpha = 0.6f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                        } else {
                            LazyColumn(
                                modifier       = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 80.dp),
                            ) {
                                items(filtered, key = { it.bag.id }) { bag ->
                                    GigBagCard(
                                        bag      = bag,
                                        theme    = theme,
                                        onTap    = { selectedGigBag = bag },
                                        onRename = { renameTarget = bag },
                                        onDelete = { deleteTarget = bag },
                                    )
                                }
                            }
                        }

                        // FAB
                        FloatingActionButton(
                            onClick        = { showCreateBagDialog = true },
                            modifier       = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                            containerColor = theme.accent,
                            contentColor   = theme.bg,
                            shape          = RoundedCornerShape(14.dp),
                        ) {
                            Text("+", fontSize = 22.sp, fontFamily = FontFamily.Monospace)
                        }
                    }
                }

                // Browse All — Artists chip
                browseFilter == BrowseFilter.Artists -> {
                    val filtered = if (searchQuery.isBlank()) combinedArtists
                    else combinedArtists.filter { it.name.contains(searchQuery, ignoreCase = true) }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No artists found", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        LazyColumn(Modifier.fillMaxSize()) {
                            items(filtered) { artist ->
                                ArtistRow(artist, theme, showSourceBadge = true) {
                                    libraryViewModel.loadArtistAlbums(artist)
                                }
                            }
                        }
                    }
                }

                // Browse All — Albums + All chips
                else -> {
                    val sourceAlbums = combinedAlbums
                    val filtered = if (searchQuery.isBlank()) sourceAlbums
                    else sourceAlbums.filter {
                        it.name.contains(searchQuery, ignoreCase = true) ||
                        it.artist.contains(searchQuery, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No albums found", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        }
                    } else {
                        AlbumGrid(albums = filtered, theme = theme, showSourceBadge = true) {
                            libraryViewModel.loadAlbumTracks(it)
                        }
                    }
                }
            }
        }
    }
}

// ─── Gig Bag card ─────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GigBagCard(
    bag:      GigBagWithCount,
    theme:    DroidTheme,
    onTap:    () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onTap, onLongClick = { showMenu = true })
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(theme.accent))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text       = bag.bag.name,
                    color      = theme.fg,
                    fontSize   = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    "${bag.trackCount} tracks",
                    color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                )
            }
            Text(
                text       = "GB",
                color      = theme.green,
                fontSize   = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier   = Modifier
                    .background(theme.green.copy(alpha = 0.15f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text("›", color = theme.fg2, fontSize = 14.sp)
        }
        DropdownMenu(
            expanded         = showMenu,
            onDismissRequest = { showMenu = false },
            modifier         = Modifier.background(theme.panel),
        ) {
            DropdownMenuItem(
                text    = { Text("Rename", color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                onClick = { showMenu = false; onRename() },
            )
            DropdownMenuItem(
                text    = { Text("Delete", color = theme.red, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                onClick = { showMenu = false; onDelete() },
            )
        }
    }
    HorizontalDivider(color = theme.border, thickness = 0.5.dp)
}

// ─── Gig Bag track row (with long-press → Remove) ─────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun GigBagTrackRow(
    track:    Track,
    theme:    DroidTheme,
    onTap:    () -> Unit,
    onRemove: () -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onTap, onLongClick = { showMenu = true })
                .padding(horizontal = 16.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(track.title, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(8.dp))
            Text(
                "%d:%02d".format(track.duration / 60000, (track.duration % 60000) / 1000),
                color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
            )
        }
        DropdownMenu(
            expanded         = showMenu,
            onDismissRequest = { showMenu = false },
            modifier         = Modifier.background(theme.panel),
        ) {
            DropdownMenuItem(
                text    = { Text("Remove from Gig Bag", color = theme.red, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
                onClick = { showMenu = false; onRemove() },
            )
        }
    }
    HorizontalDivider(color = theme.border, thickness = 0.5.dp)
}

// ─── Album grid ───────────────────────────────────────────────

@Composable
private fun AlbumGrid(
    albums:          List<Album>,
    theme:           DroidTheme,
    showSourceBadge: Boolean = false,
    onAlbumClick:    (Album) -> Unit,
) {
    LazyVerticalGrid(
        columns               = GridCells.Fixed(2),
        contentPadding        = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.fillMaxSize(),
    ) {
        items(albums) { album ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(theme.panel)
                    .clickable { onAlbumClick(album) }
                    .padding(bottom = 8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(theme.surface),
                ) {
                    if (album.coverArtId != null) {
                        AsyncImage(model = album.coverArtId, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("♫", color = theme.accent, fontSize = 32.sp, modifier = Modifier.align(Alignment.Center))
                    }
                    if (showSourceBadge) {
                        val isLocal = album.id.startsWith("local_album:")
                        Text(
                            text     = if (isLocal) "LOCAL" else "NAVI",
                            color    = if (isLocal) theme.green else theme.accent,
                            fontSize = 7.sp, fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .background(
                                    if (isLocal) theme.green.copy(alpha = 0.18f) else theme.accent.copy(alpha = 0.18f),
                                    RoundedCornerShape(3.dp),
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp),
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(album.name, color = theme.fg, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
                Text(album.artist, color = theme.fg2, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
                if (album.year > 0) Text(album.year.toString(), color = theme.fg2.copy(alpha = 0.5f), fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
    }
}

// ─── Artist row ───────────────────────────────────────────────

@Composable
private fun ArtistRow(
    artist:          Artist,
    theme:           DroidTheme,
    showSourceBadge: Boolean,
    onClick:         () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("♪", color = theme.accent, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
        Column(Modifier.weight(1f)) {
            Text(artist.name, color = theme.fg, fontSize = 13.sp, fontFamily = FontFamily.Monospace,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${artist.albumCount} albums", color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        if (showSourceBadge) {
            val isLocal = artist.id.startsWith("local_artist:")
            Text(
                text     = if (isLocal) "LOCAL" else "NAVI",
                color    = if (isLocal) theme.green else theme.accent,
                fontSize = 7.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .background(
                        if (isLocal) theme.green.copy(alpha = 0.12f) else theme.accent.copy(alpha = 0.12f),
                        RoundedCornerShape(3.dp),
                    )
                    .padding(horizontal = 4.dp, vertical = 2.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text("›", color = theme.fg2, fontSize = 14.sp)
    }
    HorizontalDivider(color = theme.border, thickness = 0.5.dp)
}

// ─── Track row ────────────────────────────────────────────────

@Composable
private fun TrackRow(
    track:           Track,
    theme:           DroidTheme,
    showSourceBadge: Boolean,
    onClick:         () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(track.title, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(track.artist, color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (showSourceBadge) {
                    val isLocal = track.source == TrackSource.LOCAL
                    Text(
                        text     = if (isLocal) "LOCAL" else "NAVI",
                        color    = if (isLocal) theme.green else theme.accent,
                        fontSize = 7.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(
                                if (isLocal) theme.green.copy(alpha = 0.12f) else theme.accent.copy(alpha = 0.12f),
                                RoundedCornerShape(3.dp),
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "%d:%02d".format(track.duration / 60000, (track.duration % 60000) / 1000),
            color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
        )
    }
    HorizontalDivider(color = theme.border, thickness = 0.5.dp)
}

// ─── Album track list ─────────────────────────────────────────

@Composable
private fun AlbumTrackList(
    album: Album, tracks: List<Track>,
    theme: DroidTheme,
    onTrackClick: (Int) -> Unit,
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.size(64.dp).clip(RoundedCornerShape(6.dp)).background(theme.surface)) {
                    if (album.coverArtId != null) AsyncImage(model = album.coverArtId, contentDescription = null, modifier = Modifier.fillMaxSize())
                    else Text("♫", color = theme.accent, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(album.name, color = theme.fg, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(album.artist, color = theme.fg2, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                    if (album.year > 0) Text(album.year.toString(), color = theme.fg2.copy(alpha = 0.5f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                }
            }
            HorizontalDivider(color = theme.border)
        }
        itemsIndexed(tracks) { idx, track ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackClick(idx) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text     = if (track.trackNumber > 0) "%02d".format(track.trackNumber) else "  ",
                    color    = theme.fg2, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(24.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(track.title, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.suffix.uppercase(), color = theme.yellow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                Text(
                    text  = "%d:%02d".format(track.duration / 60000, (track.duration % 60000) / 1000),
                    color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                )
            }
            HorizontalDivider(color = theme.border, thickness = 0.5.dp)
        }
    }
}
