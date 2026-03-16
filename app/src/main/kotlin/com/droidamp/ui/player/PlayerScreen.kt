package com.droidamp.ui.player

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.droidamp.domain.model.PlayerState
import com.droidamp.domain.model.RepeatMode
import com.droidamp.ui.theme.DroidTheme
import com.droidamp.ui.theme.DroidThemes
import com.droidamp.ui.theme.ThemeViewModel
import com.droidamp.ui.visualizer.VisualizerCanvas

@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    themeViewModel: ThemeViewModel,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
) {
    val playerState by viewModel.playerState.collectAsState()
    val fftData     by viewModel.fftData.collectAsState()
    val vizMode     by viewModel.vizMode.collectAsState()
    val vizFull     by viewModel.isVizFullScreen.collectAsState()
    val theme       by themeViewModel.theme.collectAsState()

    val context = LocalContext.current

    // Request RECORD_AUDIO for the Visualizer; retry if previously denied
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.onPermissionGranted() }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            viewModel.onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Scroll theme strip to active swatch on theme change
    val themeStripState = rememberLazyListState()
    val activeThemeIndex = DroidThemes.all.indexOfFirst { it.id == theme.id }.coerceAtLeast(0)
    LaunchedEffect(theme.id) {
        themeStripState.animateScrollToItem(activeThemeIndex)
    }

    val track = playerState.currentTrack

    Box(modifier = Modifier.fillMaxSize().background(theme.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── 1. Header ──────────────────────────────────────
            Row(
                modifier          = Modifier
                    .fillMaxWidth()
                    .background(theme.panel)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "DROIDAMP",
                    color      = theme.accent,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier.weight(1f),
                )
                Text(
                    text     = "📚",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { onNavigateToLibrary() }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
                Text(
                    text     = "◑",
                    color    = theme.accent,
                    fontSize = 16.sp,
                    modifier = Modifier
                        .clickable { onNavigateToSettings() }
                        .padding(start = 4.dp, top = 4.dp, bottom = 4.dp),
                )
            }

            // ── 2. Visualizer (140 dp) ─────────────────────────
            if (!vizFull) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(theme.panel),
                ) {
                    VisualizerCanvas(
                        fftData         = fftData,
                        mode            = vizMode,
                        accentColor     = theme.vizBar,
                        secondaryColor  = theme.red,
                        backgroundColor = theme.panel,
                        modifier        = Modifier.fillMaxSize(),
                        onSwipeNext     = { viewModel.nextVizMode() },
                        onSwipePrev     = { viewModel.prevVizMode() },
                    )
                    // Mode label — top-right
                    Text(
                        text       = vizMode.label,
                        color      = theme.fg2,
                        fontSize   = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier   = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(theme.bg.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                    // Fullscreen toggle — bottom-right
                    Text(
                        text     = "⛶",
                        color    = theme.fg2,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                            .clickable { viewModel.toggleVizFullScreen() }
                            .padding(4.dp),
                    )
                }
            }

            // ── 3. Album art + track info ──────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Album art — 180 dp square
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (track?.coverArtId != null) {
                        AsyncImage(
                            model              = track.coverArtId,
                            contentDescription = null,
                            modifier           = Modifier.fillMaxSize(),
                        )
                    } else {
                        Text("♫", color = theme.accent, fontSize = 56.sp)
                    }
                }

                Spacer(Modifier.height(10.dp))

                // Title
                Text(
                    text       = track?.title ?: "No track loaded",
                    color      = theme.fg,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = TextAlign.Center,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(3.dp))

                // Artist — accent color per spec
                Text(
                    text       = track?.artist ?: "",
                    color      = theme.accent,
                    fontSize   = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = TextAlign.Center,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )

                // Album — muted fg2
                Text(
                    text       = track?.album ?: "",
                    color      = theme.fg2.copy(alpha = 0.6f),
                    fontSize   = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = TextAlign.Center,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )

                // Source + format badges inline
                if (track != null) {
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        // Source badge (NAVIDROME / LOCAL / etc.)
                        Text(
                            text       = track.source.name,
                            color      = theme.green,
                            fontSize   = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier   = Modifier
                                .background(theme.green.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                        // Format badge (MP3 / FLAC / etc.)
                        if (track.suffix.isNotEmpty()) {
                            Text(
                                text       = track.suffix.uppercase(),
                                color      = theme.yellow,
                                fontSize   = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier   = Modifier
                                    .background(theme.yellow.copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }

            // ── 4. Seek bar ────────────────────────────────────
            SeekBar(
                position   = playerState.positionMs,
                duration   = playerState.durationMs,
                accent     = theme.accent,
                trackColor = theme.surface,
                textColor  = theme.fg2,
                onSeek     = { viewModel.seekTo(it) },
                modifier   = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            )

            Spacer(Modifier.height(6.dp))

            // ── 5. Transport controls ──────────────────────────
            TransportControls(
                playerState = playerState,
                theme       = theme,
                onPrev      = { viewModel.prev() },
                onPlayPause = { viewModel.togglePlayPause() },
                onNext      = { viewModel.next() },
                onRepeat    = { viewModel.toggleRepeat() },
                onShuffle   = { viewModel.toggleShuffle() },
            )

            Spacer(Modifier.weight(1f))

            // ── 6. Theme strip ─────────────────────────────────
            ThemeStrip(
                activeTheme   = theme,
                listState     = themeStripState,
                onSelectTheme = { themeViewModel.setTheme(it) },
            )
        }

        // ── Full-screen visualizer overlay ─────────────────────
        if (vizFull) {
            Box(modifier = Modifier.fillMaxSize().background(theme.bg)) {
                VisualizerCanvas(
                    fftData         = fftData,
                    mode            = vizMode,
                    accentColor     = theme.vizBar,
                    secondaryColor  = theme.red,
                    backgroundColor = theme.bg,
                    modifier        = Modifier.fillMaxSize(),
                    onSwipeNext     = { viewModel.nextVizMode() },
                    onSwipePrev     = { viewModel.prevVizMode() },
                )
                Text(
                    text     = "✕",
                    color    = theme.fg2,
                    fontSize = 18.sp,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(20.dp)
                        .clickable { viewModel.toggleVizFullScreen() },
                )
                Text(
                    text       = vizMode.label,
                    color      = theme.fg2,
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

// ─── Theme strip ─────────────────────────────────────────────
@Composable
private fun ThemeStrip(
    activeTheme: DroidTheme,
    listState: androidx.compose.foundation.lazy.LazyListState,
    onSelectTheme: (DroidTheme) -> Unit,
) {
    LazyRow(
        state                  = listState,
        modifier               = Modifier
            .fillMaxWidth()
            .background(activeTheme.panel)
            .padding(vertical = 8.dp),
        contentPadding         = PaddingValues(horizontal = 10.dp),
        horizontalArrangement  = Arrangement.spacedBy(8.dp),
    ) {
        items(DroidThemes.all) { t ->
            val isActive = t.id == activeTheme.id
            Column(
                modifier            = Modifier
                    .width(52.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(t.bg)
                    .then(
                        if (isActive)
                            Modifier.border(1.5.dp, t.accent, RoundedCornerShape(5.dp))
                        else
                            Modifier
                    )
                    .clickable { onSelectTheme(t) }
                    .padding(5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Mini 4-color swatch row
                Row(
                    modifier              = Modifier
                        .fillMaxWidth()
                        .height(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                ) {
                    listOf(t.accent, t.green, t.yellow, t.red).forEach { c ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .background(c, RoundedCornerShape(1.dp)),
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text       = t.displayName,
                    color      = t.fg,
                    fontSize   = 6.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = TextAlign.Center,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

// ─── Seek bar ─────────────────────────────────────────────────
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
            value         = progress,
            onValueChange = { onSeek((it * duration).toLong()) },
            colors        = SliderDefaults.colors(
                thumbColor         = accent,
                activeTrackColor   = accent,
                inactiveTrackColor = trackColor,
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

// ─── Transport controls ───────────────────────────────────────
@Composable
private fun TransportControls(
    playerState: PlayerState, theme: DroidTheme,
    onPrev: () -> Unit, onPlayPause: () -> Unit, onNext: () -> Unit,
    onRepeat: () -> Unit, onShuffle: () -> Unit,
) {
    Row(
        modifier              = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        val shuffleColor = if (playerState.isShuffled) theme.accent else theme.fg2
        Text("⇌", color = shuffleColor, fontSize = 18.sp,
            modifier = Modifier.clickable(onClick = onShuffle))
        Text("⏮", color = theme.fg, fontSize = 24.sp,
            modifier = Modifier.clickable(onClick = onPrev))
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
        Text("⏭", color = theme.fg, fontSize = 24.sp,
            modifier = Modifier.clickable(onClick = onNext))
        val repeatIcon  = when (playerState.repeatMode) {
            RepeatMode.OFF -> "↻"; RepeatMode.ALL -> "🔁"; RepeatMode.ONE -> "🔂"
        }
        val repeatColor = if (playerState.repeatMode != RepeatMode.OFF) theme.accent else theme.fg2
        Text(repeatIcon, color = repeatColor, fontSize = 18.sp,
            modifier = Modifier.clickable(onClick = onRepeat))
    }
}
