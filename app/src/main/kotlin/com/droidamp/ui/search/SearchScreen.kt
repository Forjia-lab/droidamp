package com.droidamp.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.droidamp.ui.player.PlayerViewModel
import com.droidamp.ui.theme.DroidTheme
import com.droidamp.ui.theme.DroidThemes

@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    playerViewModel: PlayerViewModel,
    onNavigateToPlayer: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val theme = DroidThemes.Catppuccin

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.bg),
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.panel)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "SEARCH",
                color = theme.accent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
            )
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

        when {
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
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("♪", color = theme.accent, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
                                Column {
                                    Text(artist.name, color = theme.fg, fontSize = 13.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${artist.albumCount} albums", color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
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
