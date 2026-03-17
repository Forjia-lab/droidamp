package com.droidamp.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
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
import com.droidamp.ui.player.PlayerViewModel
import com.droidamp.ui.theme.DroidTheme
import com.droidamp.ui.theme.ThemeViewModel
import com.droidamp.ui.visualizer.VisualizerCanvas

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    playerViewModel: PlayerViewModel,
    themeViewModel: ThemeViewModel,
    onNavigateToPlayer: () -> Unit,
) {
    val uiState  by viewModel.uiState.collectAsState()
    val theme    by themeViewModel.theme.collectAsState()
    val fftData  by playerViewModel.fftData.collectAsState()
    val vizMode  by playerViewModel.vizMode.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg),
    ) {
        // Header
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .background(theme.panel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text       = "SEARCH",
                color      = theme.accent,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier   = Modifier.weight(1f),
            )
            when {
                uiState.selectedAlbum != null ->
                    Text("← Back", color = theme.fg2, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { viewModel.clearAlbumSelection() })
                uiState.selectedArtist != null ->
                    Text("← Back", color = theme.fg2, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.clickable { viewModel.clearArtistSelection() })
            }
        }

        // Search input
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.surface)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌕", color = theme.fg2, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
            BasicTextField(
                value = uiState.query,
                onValueChange = { viewModel.setQuery(it) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = theme.fg,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                ),
                decorationBox = { inner ->
                    Box {
                        if (uiState.query.isEmpty()) Text(
                            "artists, albums, tracks…",
                            color = theme.fg2.copy(alpha = 0.4f),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                        inner()
                    }
                },
                modifier = Modifier.weight(1f),
            )
            if (uiState.query.isNotEmpty()) {
                Text(
                    "✕", color = theme.fg2, fontSize = 12.sp,
                    modifier = Modifier.clickable { viewModel.setQuery("") }.padding(start = 8.dp),
                )
            }
        }

        // Visualizer strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(theme.panel),
        ) {
            VisualizerCanvas(
                fftData         = fftData,
                mode            = vizMode,
                accentColor     = theme.vizBar,
                secondaryColor  = theme.red,
                backgroundColor = theme.panel,
                modifier        = Modifier.fillMaxSize(),
                onSwipeNext     = { playerViewModel.nextVizMode() },
                onSwipePrev     = { playerViewModel.prevVizMode() },
            )
        }

        when {
            uiState.selectedAlbum != null -> {
                SearchAlbumTrackList(
                    album        = uiState.selectedAlbum!!,
                    tracks       = uiState.selectedAlbumTracks,
                    isLoading    = uiState.isLoadingTracks,
                    theme        = theme,
                    onTrackClick = { idx ->
                        playerViewModel.playTracks(uiState.selectedAlbumTracks, idx)
                        onNavigateToPlayer()
                    },
                )
            }
            uiState.selectedArtist != null -> {
                SearchArtistAlbumGrid(
                    albums       = uiState.selectedArtistAlbums,
                    isLoading    = uiState.isLoadingTracks,
                    theme        = theme,
                    onAlbumClick = { viewModel.loadAlbumTracks(it) },
                )
            }
            uiState.isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = theme.accent)
                }
            }
            uiState.query.length < 2 -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("type at least 2 characters", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
            uiState.results.artists.isEmpty() && uiState.results.albums.isEmpty() && uiState.results.tracks.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("no results", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    if (uiState.results.artists.isNotEmpty()) {
                        item { SectionHeader("ARTISTS", theme) }
                        items(uiState.results.artists) { artist ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadArtistAlbums(artist) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("♪", color = theme.accent, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(artist.name, color = theme.fg, fontSize = 13.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${artist.albumCount} albums", color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                                Text("›", color = theme.fg2, fontSize = 16.sp)
                            }
                            Divider(color = theme.border, thickness = 0.5.dp)
                        }
                    }

                    if (uiState.results.albums.isNotEmpty()) {
                        item { SectionHeader("ALBUMS", theme) }
                        items(uiState.results.albums) { album ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.loadAlbumTracks(album) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(theme.surface),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (album.coverArtId != null) {
                                        AsyncImage(model = album.coverArtId, contentDescription = null, modifier = Modifier.fillMaxSize())
                                    } else {
                                        Text("♫", color = theme.accent, fontSize = 16.sp)
                                    }
                                }
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(album.name, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(album.artist, color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("›", color = theme.fg2, fontSize = 16.sp)
                            }
                            Divider(color = theme.border, thickness = 0.5.dp)
                        }
                    }

                    if (uiState.results.tracks.isNotEmpty()) {
                        item { SectionHeader("TRACKS", theme) }
                        items(uiState.results.tracks.withIndex().toList()) { (idx, track) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        playerViewModel.playTracks(uiState.results.tracks, idx)
                                        onNavigateToPlayer()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(track.artist, color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
            }
        }
    }
}

@Composable
private fun SearchArtistAlbumGrid(
    albums:       List<Album>,
    isLoading:    Boolean,
    theme:        DroidTheme,
    onAlbumClick: (Album) -> Unit,
) {
    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }
    LazyVerticalGrid(
        columns               = GridCells.Fixed(2),
        contentPadding        = PaddingValues(10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement   = Arrangement.spacedBy(8.dp),
        modifier              = Modifier.fillMaxSize(),
    ) {
        gridItems(albums) { album ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(theme.panel)
                    .clickable { onAlbumClick(album) }
                    .padding(bottom = 8.dp),
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(theme.surface),
                ) {
                    if (album.coverArtId != null) {
                        AsyncImage(model = album.coverArtId, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("♫", color = theme.accent, fontSize = 28.sp, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(album.name, color = theme.fg, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
                Text(album.artist, color = theme.fg2, fontSize = 9.sp, fontFamily = FontFamily.Monospace,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 6.dp))
            }
        }
    }
}

@Composable
private fun SearchAlbumTrackList(
    album: com.droidamp.domain.model.Album,
    tracks: List<Track>,
    isLoading: Boolean,
    theme: DroidTheme,
    onTrackClick: (Int) -> Unit,
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = theme.accent)
        }
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
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
                modifier          = Modifier.fillMaxWidth().clickable { onTrackClick(idx) }.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    if (track.trackNumber > 0) "%02d".format(track.trackNumber) else "  ",
                    color = theme.fg2, fontSize = 11.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.width(24.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                    Text(track.title, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(track.suffix.uppercase(), color = theme.yellow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                }
                Text("%d:%02d".format(track.duration / 60000, (track.duration % 60000) / 1000), color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
            Divider(color = theme.border, thickness = 0.5.dp)
        }
    }
}

@Composable
private fun SectionHeader(title: String, theme: DroidTheme) {
    Text(
        text = title,
        color = theme.accent,
        fontSize = 9.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.panel)
            .padding(horizontal = 16.dp, vertical = 6.dp),
    )
}
