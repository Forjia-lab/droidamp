package com.droidamp.ui.player

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.droidamp.domain.model.PlayerState
import com.droidamp.domain.model.RepeatMode
import com.droidamp.ui.theme.DroidTheme
import com.droidamp.ui.theme.DroidThemes
import com.droidamp.ui.visualizer.VisualizerCanvas
import com.droidamp.ui.visualizer.VisualizerMode

// ─────────────────────────────────────────────────────────────
//  PlayerScreen — the main now-playing view.
//
//  Layout (top → bottom):
//  1. Header bar (logo + library nav)
//  2. Visualizer — 140dp tall, full-screen mode available
//  3. Album art + track info
//  4. Seek bar
//  5. Transport controls (prev / play-pause / next)
//  6. Secondary controls (shuffle, repeat, volume, viz mode)
//  7. Source + format badge
//  8. Theme switcher strip (17 themes)
// ─────────────────────────────────────────────────────────────

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val playerState by viewModel.playerState.collectAsState()
    val fftData     by viewModel.fftData.collectAsState()
    val vizMode     by viewModel.vizMode.collectAsState()
    val vizFull     by viewModel.isVizFullScreen.collectAsState()

    var currentTheme by remember { mutableStateOf(DroidThemes.Catppuccin) }
    var showThemePicker by remember { mutableStateOf(false) }

    val track = playerState.currentTrack

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(currentTheme.bg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 8.dp),
        ) {
            // ── Header bar ────────────────────────────────────
            PlayerHeader(
                theme = currentTheme,
                onLibraryClick  = onNavigateToLibrary,
                onThemeClick    = { showThemePicker = !showThemePicker },
                onSettingsClick = onNavigateToSettings,
            )

            // ── Visualizer ────────────────────────────────────
            val vizHeight = if (vizFull) 0.dp else 100.dp
            if (!vizFull) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(currentTheme.panel)
                ) {
                    VisualizerCanvas(
                        fftData         = fftData,
                        mode            = vizMode,
                        accentColor     = currentTheme.vizBar,
                        secondaryColor  = currentTheme.red,
                        backgroundColor = currentTheme.panel,
                        modifier        = Modifier.fillMaxSize(),
                        onSwipeNext     = { viewModel.nextVizMode() },
                        onSwipePrev     = { viewModel.prevVizMode() },
                    )
                    // Viz mode label
                    Text(
                        text       = vizMode.label,
                        color      = currentTheme.fg2,
                        fontSize   = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier   = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(currentTheme.bg.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                    // Full-screen toggle
                    Text(
                        text     = "⛶",
                        color    = currentTheme.fg2,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clickable { viewModel.toggleVizFullScreen() }
                            .padding(4.dp),
                    )
                }
            }

            // ── Album art + track info ─────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Album art
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(currentTheme.surface)
                ) {
                    if (track?.coverArtId != null) {
                        AsyncImage(
                            model = track.coverArtId, // full URL built by repo
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text(
                            text     = "♫",
                            color    = currentTheme.accent,
                            fontSize = 48.sp,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Track title
                Text(
                    text       = track?.title ?: "No track loaded",
                    color      = currentTheme.fg,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = TextAlign.Center,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text       = track?.artist ?: "",
                    color      = currentTheme.fg2,
                    fontSize   = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = TextAlign.Center,
                )
                Text(
                    text       = track?.album ?: "",
                    color      = currentTheme.fg2.copy(alpha = 0.6f),
                    fontSize   = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = TextAlign.Center,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )

                // Source + format badge
                if (track != null) {
                    Spacer(Modifier.height(5.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Badge(text = track.source.name, color = currentTheme.green)
                        if (track.suffix.isNotEmpty()) Badge(text = track.suffix.uppercase(), color = currentTheme.yellow)
                    }
                }
            }

            // ── Seek bar ─────────────────────────────────────
            SeekBar(
                position   = playerState.positionMs,
                duration   = playerState.durationMs,
                accent     = currentTheme.accent,
                trackColor = currentTheme.surface,
                textColor  = currentTheme.fg2,
                onSeek     = { viewModel.seekTo(it) },
                modifier   = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(4.dp))

            // ── Transport controls ────────────────────────────
            TransportControls(
                playerState = playerState,
                theme       = currentTheme,
                onPrev      = { viewModel.prev() },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext      = { viewModel.next() },
                onRepeat    = { viewModel.toggleRepeat() },
                onShuffle   = { viewModel.toggleShuffle() },
            )

            Spacer(Modifier.height(4.dp))

            // ── Volume slider ─────────────────────────────────
            VolumeRow(
                volume    = playerState.volume,
                theme     = currentTheme,
                onVolume  = { viewModel.setVolume(it) },
                modifier  = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            )

            Spacer(Modifier.weight(1f))

            // ── Theme switcher strip ──────────────────────────
            ThemeStrip(
                themes       = DroidThemes.all,
                currentTheme = currentTheme,
                onTheme      = { currentTheme = it },
            )
        }

        // ── Full-screen visualizer overlay ────────────────────
        if (vizFull) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(currentTheme.bg)
            ) {
                VisualizerCanvas(
                    fftData         = fftData,
                    mode            = vizMode,
                    accentColor     = currentTheme.vizBar,
                    secondaryColor  = currentTheme.red,
                    backgroundColor = currentTheme.bg,
                    modifier        = Modifier.fillMaxSize(),
                    onSwipeNext     = { viewModel.nextVizMode() },
                    onSwipePrev     = { viewModel.prevVizMode() },
                )
                Text(
                    text     = "✕",
                    color    = currentTheme.fg2,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .clickable { viewModel.toggleVizFullScreen() },
                )
                Text(
                    text       = vizMode.label,
                    color      = currentTheme.fg2,
                    fontSize   = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(20.dp),
                )
            }
        }
    }
}

// ─── Sub-components ───────────────────────────────────────────

@Composable
private fun PlayerHeader(theme: DroidTheme, onLibraryClick: () -> Unit, onThemeClick: () -> Unit, onSettingsClick: () -> Unit) {
    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .background(theme.panel)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment   = Alignment.CenterVertically,
    ) {
        Text(
            text       = "DROIDAMP",
            color      = theme.accent,
            fontSize   = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier   = Modifier.weight(1f),
        )
        Text("☰", color = theme.fg2, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onLibraryClick).padding(8.dp))
        Text("◑", color = theme.fg2, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onThemeClick).padding(8.dp))
        Text("⚙", color = theme.fg2, fontSize = 16.sp, modifier = Modifier.clickable(onClick = onSettingsClick).padding(8.dp))
    }
}

@Composable
private fun Badge(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text       = text,
        color      = color,
        fontSize   = 8.sp,
        fontFamily = FontFamily.Monospace,
        modifier   = Modifier
            .background(color.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun SeekBar(
    position: Long, duration: Long,
    accent: androidx.compose.ui.graphics.Color,
    trackColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
    onSeek: (Long) -> Unit, modifier: Modifier = Modifier,
) {
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
    Column(modifier = modifier) {
        Slider(
            value = progress,
            onValueChange = { onSeek((it * duration).toLong()) },
            colors = SliderDefaults.colors(
                thumbColor            = accent,
                activeTrackColor      = accent,
                inactiveTrackColor    = trackColor,
            ),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(msToTime(position), color = textColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text(msToTime(duration), color = textColor, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

private fun msToTime(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

@Composable
private fun TransportControls(
    playerState: PlayerState, theme: DroidTheme,
    onPrev: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit,
    onRepeat: () -> Unit, onShuffle: () -> Unit,
) {
    Row(
        modifier            = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Shuffle
        val shuffleColor = if (playerState.isShuffled) theme.accent else theme.fg2
        Text("⇌", color = shuffleColor, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onShuffle))

        // Prev
        Text("⏮", color = theme.fg, fontSize = 24.sp, modifier = Modifier.clickable(onClick = onPrev))

        // Play / Pause — larger, accented
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(theme.playBg)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = if (playerState.isPlaying) "⏸" else "▶",
                color    = theme.accent,
                fontSize = 26.sp,
            )
        }

        // Next
        Text("⏭", color = theme.fg, fontSize = 24.sp, modifier = Modifier.clickable(onClick = onNext))

        // Repeat
        val repeatIcon = when (playerState.repeatMode) {
            RepeatMode.OFF -> "↻"
            RepeatMode.ALL -> "🔁"
            RepeatMode.ONE -> "🔂"
        }
        val repeatColor = if (playerState.repeatMode != RepeatMode.OFF) theme.accent else theme.fg2
        Text(repeatIcon, color = repeatColor, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onRepeat))
    }
}

@Composable
private fun VolumeRow(
    volume: Float, theme: DroidTheme,
    onVolume: (Float) -> Unit, modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text("🔈", color = theme.fg2, fontSize = 12.sp)
        Slider(
            value           = volume,
            onValueChange   = onVolume,
            modifier        = Modifier.weight(1f).padding(horizontal = 6.dp),
            colors          = SliderDefaults.colors(
                thumbColor         = theme.volBar,
                activeTrackColor   = theme.volBar,
                inactiveTrackColor = theme.surface,
            ),
        )
        Text("🔊", color = theme.fg2, fontSize = 12.sp)
    }
}

@Composable
private fun ThemeStrip(
    themes: List<DroidTheme>,
    currentTheme: DroidTheme,
    onTheme: (DroidTheme) -> Unit,
) {
    LazyRow(
        modifier            = Modifier
            .fillMaxWidth()
            .background(currentTheme.panel)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        contentPadding       = PaddingValues(horizontal = 12.dp),
    ) {
        items(themes) { theme ->
            val isActive = theme.id == currentTheme.id
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable { onTheme(theme) },
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(theme.bg)
                        .then(
                            if (isActive) Modifier.padding(1.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(theme.accent)
                            else Modifier
                        ),
                ) {
                    Box(
                        modifier = Modifier
                            .size(if (isActive) 16.dp else 20.dp)
                            .align(Alignment.Center)
                            .clip(RoundedCornerShape(if (isActive) 3.dp else 4.dp))
                            .background(theme.bg),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(theme.accent),
                        )
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    text       = theme.displayName.take(6),
                    color      = if (isActive) currentTheme.accent else currentTheme.fg2,
                    fontSize   = 6.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = TextAlign.Center,
                )
            }
        }
    }
}
