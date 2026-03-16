package com.droidamp.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.droidamp.domain.model.Album
import com.droidamp.domain.model.Track
import com.droidamp.ui.components.MiniPlayerBar
import com.droidamp.ui.player.PlayerViewModel
import com.droidamp.ui.theme.DroidThemes

// ─────────────────────────────────────────────────────────────
//  LibraryScreen — browse your Navidrome library.
//  Tabs: Albums · Artists · Playlists
//  Tap album → see tracks → tap track → play from that position
//  Persistent MiniPlayerBar at the bottom.
// ─────────────────────────────────────────────────────────────

@Composable
fun LibraryScreen(
    libraryViewModel: LibraryViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateBack: () -> Unit,
) {
    val uiState     by libraryViewModel.uiState.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val theme       = DroidThemes.Catppuccin  // TODO: wire up shared theme state

    var searchQuery by remember { mutableStateOf("") }
    val q = searchQuery.trim().lowercase()

    val filteredAlbums    = if (q.isEmpty()) uiState.albums
                            else uiState.albums.filter { q in it.name.lowercase() || q in it.artist.lowercase() }
    val filteredArtists   = if (q.isEmpty()) uiState.artists
                            else uiState.artists.filter { q in it.name.lowercase() }
    val filteredPlaylists = if (q.isEmpty()) uiState.playlists
                            else uiState.playlists.filter { q in it.name.lowercase() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg),
    ) {
        // ── Header ────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.panel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "←",
                color = theme.accent,
                fontSize = 18.sp,
                modifier = Modifier.clickable(onClick = onNavigateBack).padding(end = 12.dp),
            )
            Text(
                text = "LIBRARY",
                color = theme.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )
            if (uiState.selectedAlbum != null) {
                Text(
                    text = "← Back",
                    color = theme.fg2,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.clickable { libraryViewModel.clearAlbumSelection() },
                )
            }
        }

        // ── Tab bar ───────────────────────────────────────────
        if (uiState.selectedAlbum == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.panel)
                    .padding(horizontal = 12.dp),
            ) {
                listOf(
                    "Albums" to LibraryTab.Albums,
                    "Artists" to LibraryTab.Artists,
                    "Playlists" to LibraryTab.Playlists,
                ).forEach { (label, tab) ->
                    val active = uiState.tab::class == tab::class
                    Text(
                        text = label,
                        color = if (active) theme.accent else theme.fg2,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier
                            .clickable { libraryViewModel.selectTab(tab) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }

            // ── Search bar ────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.bg)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("⌕", color = theme.fg2, fontSize = 14.sp, modifier = Modifier.padding(end = 6.dp))
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = theme.fg,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    decorationBox = { inner ->
                        Box {
                            if (searchQuery.isEmpty()) Text(
                                "search…",
                                color = theme.fg2.copy(alpha = 0.4f),
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                            )
                            inner()
                        }
                    },
                    modifier = Modifier.weight(1f),
                )
                if (searchQuery.isNotEmpty()) {
                    Text(
                        "✕",
                        color = theme.fg2,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .clickable { searchQuery = "" }
                            .padding(start = 6.dp),
                    )
                }
            }
        }

        // ── Content ───────────────────────────────────────────
        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        color = theme.accent,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                uiState.selectedAlbum != null -> {
                    AlbumTrackList(
                        album  = uiState.selectedAlbum!!,
                        tracks = uiState.selectedAlbumTracks,
                        theme  = theme,
                        onTrackClick = { idx ->
                            playerViewModel.playTracks(uiState.selectedAlbumTracks, idx)
                            onNavigateBack()
                        },
                    )
                }
                uiState.tab is LibraryTab.Albums -> {
                    AlbumGrid(
                        albums = filteredAlbums,
                        theme  = theme,
                        onAlbumClick = { libraryViewModel.loadAlbumTracks(it) },
                    )
                }
                uiState.tab is LibraryTab.Artists -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredArtists) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* load artist albums */ }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("♪", color = theme.accent, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
                                Column {
                                    Text(artist.name, color = theme.fg, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                    Text("${artist.albumCount} albums", color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Divider(color = theme.border, thickness = 0.5.dp)
                        }
                    }
                }
                uiState.tab is LibraryTab.Playlists -> {
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredPlaylists) { pl ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { /* load playlist tracks */ }
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                            ) {
                                Text("≡", color = theme.accent, fontSize = 16.sp, modifier = Modifier.padding(end = 10.dp))
                                Column {
                                    Text(pl.name, color = theme.fg, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                                    Text("${pl.trackCount} tracks", color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            Divider(color = theme.border, thickness = 0.5.dp)
                        }
                    }
                }
                else -> {}
            }
        }

        // ── Mini Player Bar ───────────────────────────────────
        MiniPlayerBar(
            playerState = playerState,
            theme       = theme,
            coverArtUrl = null,  // TODO: wire up cover art URL from repo
            onTap       = onNavigateBack,
            onPlayPause = { playerViewModel.togglePlayPause() },
            onNext      = { playerViewModel.next() },
        )
    }
}

// ─── Album grid ───────────────────────────────────────────────

@Composable
private fun AlbumGrid(
    albums: List<Album>,
    theme: com.droidamp.ui.theme.DroidTheme,
    onAlbumClick: (Album) -> Unit,
) {
    LazyVerticalGrid(
        columns             = GridCells.Fixed(2),
        contentPadding      = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
        modifier            = Modifier.fillMaxSize(),
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

// ─── Album track list ─────────────────────────────────────────

@Composable
private fun AlbumTrackList(
    album: Album, tracks: List<Track>,
    theme: com.droidamp.ui.theme.DroidTheme,
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
            Divider(color = theme.border)
        }
        items(tracks.withIndex().toList()) { (idx, track) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackClick(idx) }
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (track.trackNumber > 0) "%02d".format(track.trackNumber) else "  ",
                    color = theme.fg2, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    modifier = Modifier.width(24.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(track.title, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.suffix.uppercase(), color = theme.yellow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                Text(
                    text = "%d:%02d".format(track.duration / 60000, (track.duration % 60000) / 1000),
                    color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                )
            }
            Divider(color = theme.border, thickness = 0.5.dp)
        }
    }
}
