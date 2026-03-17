package com.droidamp.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.droidamp.domain.model.Album
import com.droidamp.domain.model.PlayerState
import com.droidamp.domain.model.Track
import com.droidamp.domain.model.TrackSource
import com.droidamp.ui.library.LibraryViewModel
import com.droidamp.ui.search.SearchViewModel
import com.droidamp.ui.settings.SettingsViewModel
import com.droidamp.ui.theme.DroidTheme
import com.droidamp.ui.theme.DroidThemes
import com.droidamp.ui.theme.ThemeViewModel
import com.droidamp.ui.visualizer.VisualizerCanvas

// ─────────────────────────────────────────────────────────────
//  PlayerScreen — Winamp/AIMP-style full player
// ─────────────────────────────────────────────────────────────

enum class PlayerTab { QUEUE, LIBRARY, SOURCES, SEARCH }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerViewModel:  PlayerViewModel,
    themeViewModel:   ThemeViewModel,
    libraryViewModel: LibraryViewModel,
    searchViewModel:  SearchViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val theme       by themeViewModel.theme.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val fftData     by playerViewModel.fftData.collectAsState()
    val vizMode     by playerViewModel.vizMode.collectAsState()
    val vizFull     by playerViewModel.isVizFullScreen.collectAsState()
    val eqBands     by playerViewModel.eqBands.collectAsState()
    val starredIds  by playerViewModel.starredIds.collectAsState()

    var activeTab        by remember { mutableStateOf(PlayerTab.QUEUE) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // ── Full-screen visualizer overlay ────────────────────────
    if (vizFull) {
        Box(Modifier.fillMaxSize().background(theme.bg)) {
            VisualizerCanvas(
                fftData         = fftData,
                mode            = vizMode,
                accentColor     = theme.vizBar,
                secondaryColor  = theme.red,
                backgroundColor = theme.bg,
                modifier        = Modifier.fillMaxSize(),
                onSwipeNext     = { playerViewModel.nextVizMode() },
                onSwipePrev     = { playerViewModel.prevVizMode() },
            )
            Text("✕", color = theme.fg2, fontSize = 18.sp,
                modifier = Modifier.align(Alignment.TopEnd).padding(20.dp)
                    .clickable { playerViewModel.toggleVizFullScreen() })
            Text(vizMode.label, color = theme.fg2, fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp))
        }
        return
    }

    // ── Settings bottom sheet ──────────────────────────────────
    if (showSettingsSheet) {
        SettingsSheet(
            settingsViewModel = settingsViewModel,
            themeViewModel    = themeViewModel,
            theme             = theme,
            onDismiss         = { showSettingsSheet = false },
        )
    }

    Column(Modifier.fillMaxSize().background(theme.bg)) {

        // 1. Top bar — 48dp ────────────────────────────────────
        TopBar(theme, onSettingsTap = { showSettingsSheet = true })

        // 2. Mini now-playing bar — 80dp ───────────────────────
        MiniNowPlayingBar(
            track     = playerState.currentTrack,
            isStarred = playerState.currentTrack?.id?.let { starredIds.contains(it) } ?: false,
            theme     = theme,
            onStar    = { playerState.currentTrack?.let { playerViewModel.toggleStar(it.id) } },
        )

        // 3. Visualizer — 140dp ────────────────────────────────
        Box(Modifier.fillMaxWidth().height(140.dp).background(theme.panel)) {
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
            Text(
                text       = vizMode.label,
                color      = theme.fg2,
                fontSize   = 8.sp,
                fontFamily = FontFamily.Monospace,
                modifier   = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .background(theme.bg.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp),
            )
            Text(
                text     = "⛶",
                color    = theme.fg2,
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
                    .clickable { playerViewModel.toggleVizFullScreen() },
            )
        }

        // 4. Seek bar — 40dp ───────────────────────────────────
        SeekBarSection(
            position = playerState.positionMs,
            duration = playerState.durationMs,
            theme    = theme,
            onSeek   = { playerViewModel.seekTo(it) },
        )

        // 5. Transport controls — 64dp ─────────────────────────
        TransportRow(
            playerState = playerState,
            theme       = theme,
            onPrev      = { playerViewModel.prev() },
            onPlayPause = { playerViewModel.togglePlayPause() },
            onNext      = { playerViewModel.next() },
            onShuffle   = { playerViewModel.toggleShuffle() },
            onQueue     = { activeTab = PlayerTab.QUEUE },
        )

        // 6. 10-band EQ — 72dp ─────────────────────────────────
        EqSection(
            bands        = eqBands,
            theme        = theme,
            onBandChange = { idx, level -> playerViewModel.setEqBand(idx, level) },
        )

        // 7. Tab row — 36dp ────────────────────────────────────
        PlayerTabRow(activeTab = activeTab, theme = theme, onTab = { activeTab = it })
        HorizontalDivider(color = theme.border, thickness = 0.5.dp)

        // 8. Tab content — weight(1f) ──────────────────────────
        Box(Modifier.weight(1f)) {
            AnimatedContent(
                targetState    = activeTab,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label          = "playerTabContent",
            ) { tab ->
                when (tab) {
                    PlayerTab.QUEUE   -> PlaylistTab(playerState, theme) { playerViewModel.seekToQueueItem(it) }
                    PlayerTab.LIBRARY -> LibraryTabContent(libraryViewModel, playerViewModel, theme)
                    PlayerTab.SOURCES -> SourcesTab(settingsViewModel, theme)
                    PlayerTab.SEARCH  -> SearchTabContent(searchViewModel, playerViewModel, theme)
                }
            }
        }
    }
}

// ─── 1. Top bar — 48dp ───────────────────────────────────────

@Composable
private fun TopBar(theme: DroidTheme, onSettingsTap: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(theme.panel)
            .padding(horizontal = 16.dp),
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
        Row(
            modifier              = Modifier.clickable(onClick = onSettingsTap),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(theme.accent))
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(theme.vizBar))
            Box(Modifier.size(10.dp).clip(RoundedCornerShape(2.dp)).background(theme.eqBar))
        }
    }
}

// ─── 2. Mini now-playing bar — 80dp ──────────────────────────

@Composable
private fun MiniNowPlayingBar(
    track:     Track?,
    isStarred: Boolean,
    theme:     DroidTheme,
    onStar:    () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(theme.surface)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art — 64dp
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(theme.panel),
            ) {
                if (track?.coverArtId != null) {
                    AsyncImage(model = track.coverArtId, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Text("♫", color = theme.accent, fontSize = 24.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = track?.title ?: "No track loaded",
                    color      = theme.fg,
                    fontSize   = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                val artistYear = buildString {
                    if (track?.artist?.isNotEmpty() == true) append(track.artist)
                    if ((track?.year ?: 0) > 0) append(" · ${track?.year}")
                }
                if (artistYear.isNotEmpty()) {
                    Text(
                        text       = artistYear,
                        color      = theme.fg2,
                        fontSize   = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                }
                if (track != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(top = 2.dp),
                    ) {
                        SourceBadge(track.source, theme)
                        if (track.suffix.isNotEmpty()) FormatBadge(track.suffix.uppercase(), theme)
                    }
                }
            }
            Text(
                text     = if (isStarred) "♥" else "♡",
                color    = if (isStarred) theme.red else theme.fg2,
                fontSize = 22.sp,
                modifier = Modifier.padding(start = 8.dp).clickable(onClick = onStar),
            )
        }
        HorizontalDivider(color = theme.border, thickness = 1.dp)
    }
}

@Composable
private fun SourceBadge(source: TrackSource, theme: DroidTheme) {
    val (label, color) = when (source) {
        TrackSource.NAVIDROME  -> "NAVI"  to theme.green
        TrackSource.LOCAL      -> "LOCAL" to theme.fg2
        TrackSource.SOUNDCLOUD -> "SC"    to theme.yellow
        TrackSource.BANDCAMP   -> "BC"    to theme.red
        TrackSource.RADIO      -> "RADIO" to theme.accent
    }
    MicroBadge(label, color)
}

@Composable
private fun FormatBadge(format: String, theme: DroidTheme) = MicroBadge(format, theme.yellow)

@Composable
private fun MicroBadge(text: String, color: Color) {
    Text(
        text       = text,
        color      = color,
        fontSize   = 7.sp,
        fontFamily = FontFamily.Monospace,
        modifier   = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

// ─── 4. Seek bar — 40dp ───────────────────────────────────────

@Composable
private fun SeekBarSection(
    position: Long,
    duration: Long,
    theme:    DroidTheme,
    onSeek:   (Long) -> Unit,
) {
    val progress = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f

    Column(
        modifier              = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(theme.surface)
            .padding(horizontal = 16.dp),
        verticalArrangement   = Arrangement.Center,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(msToTime(position), color = theme.fg2, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            Text("-${msToTime((duration - position).coerceAtLeast(0L))}", color = theme.fg2, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(theme.fg2.copy(alpha = 0.25f))
                .pointerInput(duration) {
                    detectTapGestures { offset ->
                        val f = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((f * duration).toLong())
                    }
                }
                .pointerInput(duration) {
                    detectHorizontalDragGestures { change, _ ->
                        val f = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onSeek((f * duration).toLong())
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress)
                    .background(theme.accent),
            )
        }
    }
}

private fun msToTime(ms: Long): String {
    val s = (ms / 1000).coerceAtLeast(0L)
    return "%d:%02d".format(s / 60, s % 60)
}

// ─── 5. Transport controls — 64dp ────────────────────────────

@Composable
private fun TransportRow(
    playerState: PlayerState,
    theme:       DroidTheme,
    onPrev:      () -> Unit,
    onPlayPause: () -> Unit,
    onNext:      () -> Unit,
    onShuffle:   () -> Unit,
    onQueue:     () -> Unit,
) {
    val pillShape = RoundedCornerShape(6.dp)
    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Shuffle
        Box(
            modifier         = Modifier
                .clip(pillShape)
                .background(theme.surface)
                .clickable(onClick = onShuffle)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text     = "⇄",
                color    = if (playerState.isShuffled) theme.accent else theme.fg2,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        // Prev
        Box(
            modifier         = Modifier
                .clip(pillShape)
                .background(theme.surface)
                .clickable(onClick = onPrev)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("◀◀", color = theme.fg, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
        }
        // Play/Pause — circle, larger
        Box(
            modifier         = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(theme.playBg)
                .border(1.dp, theme.playBorder, CircleShape)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            // \uFE0E = text variation selector — forces text rendering so color is respected
            Text(
                text       = if (playerState.isPlaying) "⏸\uFE0E" else "▶\uFE0E",
                color      = theme.accent,
                fontSize   = 24.sp,
                fontFamily = FontFamily.Monospace,
            )
        }
        // Next
        Box(
            modifier         = Modifier
                .clip(pillShape)
                .background(theme.surface)
                .clickable(onClick = onNext)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("▶▶", color = theme.fg, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
        }
        // Queue
        Box(
            modifier         = Modifier
                .clip(pillShape)
                .background(theme.surface)
                .clickable(onClick = onQueue)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("☰", color = theme.fg2, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// ─── 6. 10-band EQ — 72dp ────────────────────────────────────

private val EQ_LABELS = listOf("60", "170", "310", "600", "1K", "3K", "6K", "12K", "14K", "16K")

@Composable
private fun EqSection(
    bands:        List<EqBand>,
    theme:        DroidTheme,
    onBandChange: (Int, Short) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .background(theme.panel)
            .padding(horizontal = 14.dp, vertical = 5.dp),
    ) {
        Text(
            text       = "10 BAND EQ",
            color      = theme.fg2,
            fontSize   = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier   = Modifier.padding(bottom = 3.dp),
        )
        if (bands.isEmpty()) {
            Text(
                text       = "plays first to enable",
                color      = theme.fg2.copy(alpha = 0.4f),
                fontSize   = 8.sp,
                fontFamily = FontFamily.Monospace,
            )
        } else {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                repeat(10) { displayIdx ->
                    val deviceIdx = (displayIdx.toFloat() / 10f * bands.size).toInt().coerceIn(0, bands.size - 1)
                    val band = bands[deviceIdx]
                    EqBandColumn(
                        band          = band,
                        label         = EQ_LABELS[displayIdx],
                        theme         = theme,
                        onLevelChange = { onBandChange(band.index, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EqBandColumn(
    band:          EqBand,
    label:         String,
    theme:         DroidTheme,
    onLevelChange: (Short) -> Unit,
) {
    val barHeightDp = 32.dp
    val normalized  = if (band.maxLevel != band.minLevel)
        (band.level - band.minLevel).toFloat() / (band.maxLevel - band.minLevel)
    else 0.5f

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(
            modifier = Modifier
                .width(10.dp)
                .height(barHeightDp)
                .clip(RoundedCornerShape(5.dp))
                .pointerInput(band.minLevel, band.maxLevel) {
                    detectVerticalDragGestures { change, _ ->
                        val levelRange = (band.maxLevel - band.minLevel).toFloat()
                        val newNorm = (1f - change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val newLevel = (band.minLevel + newNorm * levelRange).toInt()
                            .coerceIn(band.minLevel.toInt(), band.maxLevel.toInt()).toShort()
                        onLevelChange(newLevel)
                    }
                }
                .pointerInput(band.minLevel, band.maxLevel) {
                    detectTapGestures { offset ->
                        val levelRange = (band.maxLevel - band.minLevel).toFloat()
                        val newNorm = (1f - offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val newLevel = (band.minLevel + newNorm * levelRange).toInt()
                            .coerceIn(band.minLevel.toInt(), band.maxLevel.toInt()).toShort()
                        onLevelChange(newLevel)
                    }
                },
        ) {
            drawRect(color = theme.surface)
            val centerY = size.height / 2f
            drawLine(color = theme.border, start = Offset(0f, centerY), end = Offset(size.width, centerY), strokeWidth = 1f)
            val barTop: Float
            val barBottom: Float
            if (normalized >= 0.5f) {
                barTop    = centerY - (normalized - 0.5f) * 2f * centerY
                barBottom = centerY
            } else {
                barTop    = centerY
                barBottom = centerY + (0.5f - normalized) * 2f * (size.height - centerY)
            }
            if (barBottom > barTop) {
                drawRect(color = theme.eqBar, topLeft = Offset(0f, barTop), size = Size(size.width, barBottom - barTop))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = theme.fg2, fontSize = 6.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
    }
}

// ─── 7. Tab row — 36dp, equal-width ──────────────────────────

@Composable
private fun PlayerTabRow(
    activeTab: PlayerTab,
    theme:     DroidTheme,
    onTab:     (PlayerTab) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(theme.panel),
    ) {
        PlayerTab.entries.forEach { tab ->
            val active = tab == activeTab
            Box(
                modifier         = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onTab(tab) },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = tab.name,
                        color      = if (active) theme.accent else theme.fg2,
                        fontSize   = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                        textAlign  = TextAlign.Center,
                    )
                    if (active) {
                        Spacer(Modifier.height(2.dp))
                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(2.dp)
                                .background(theme.accent, RoundedCornerShape(1.dp)),
                        )
                    }
                }
            }
        }
    }
}

// ─── Settings bottom sheet ────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsSheet(
    settingsViewModel: SettingsViewModel,
    themeViewModel:    ThemeViewModel,
    theme:             DroidTheme,
    onDismiss:         () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val url        by settingsViewModel.url.collectAsState()
    val username   by settingsViewModel.username.collectAsState()
    val password   by settingsViewModel.password.collectAsState()
    val pingStatus by settingsViewModel.pingStatus.collectAsState()

    var showPassword by remember { mutableStateOf(false) }

    val isConnected = pingStatus?.startsWith("✓") == true
    val isPending   = pingStatus == "connecting…"

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = theme.panel,
        contentColor     = theme.fg,
    ) {
        LazyColumn(
            modifier       = Modifier.fillMaxWidth().navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // ── Server settings ───────────────────────────────
            item {
                Text(
                    text       = "SERVER",
                    color      = theme.fg2,
                    fontSize   = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier.padding(bottom = 8.dp),
                )
            }
            item {
                SheetField("SERVER URL", url, theme) { settingsViewModel.setUrl(it) }
                Spacer(Modifier.height(6.dp))
            }
            item {
                SheetField("USERNAME", username, theme) { settingsViewModel.setUsername(it) }
                Spacer(Modifier.height(6.dp))
            }
            item {
                // Password with proper VisualTransformation
                Column {
                    Text("PASSWORD", color = theme.fg2, fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 2.dp))
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .background(theme.bg, RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value                = password,
                            onValueChange        = { settingsViewModel.setPassword(it) },
                            modifier             = Modifier.weight(1f),
                            textStyle            = TextStyle(color = theme.fg, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
                            cursorBrush          = SolidColor(theme.accent),
                            singleLine           = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        )
                        Text(
                            text     = if (showPassword) "●" else "○",
                            color    = theme.fg2,
                            fontSize = 10.sp,
                            modifier = Modifier.clickable { showPassword = !showPassword },
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(theme.accent.copy(alpha = 0.14f), RoundedCornerShape(4.dp))
                            .clickable { settingsViewModel.save() }
                            .padding(horizontal = 16.dp, vertical = 7.dp),
                    ) {
                        Text("SAVE", color = theme.accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    if (pingStatus != null) {
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text     = pingStatus!!,
                            color    = when {
                                isConnected -> theme.green
                                isPending   -> theme.yellow
                                else        -> theme.red
                            },
                            fontSize   = 9.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = theme.border, thickness = 0.5.dp)
                Spacer(Modifier.height(12.dp))
            }
            // ── Theme picker ──────────────────────────────────
            item {
                val currentTheme by themeViewModel.theme.collectAsState()
                Text(
                    text       = "THEMES",
                    color      = theme.fg2,
                    fontSize   = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier.padding(bottom = 8.dp),
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding        = PaddingValues(bottom = 8.dp),
                ) {
                    items(DroidThemes.all) { t ->
                        val isActive = t.id == currentTheme.id
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier            = Modifier.clickable { themeViewModel.setTheme(t) },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(t.bg)
                                    .then(if (isActive) Modifier.border(2.dp, currentTheme.accent, RoundedCornerShape(6.dp)) else Modifier),
                            ) {
                                Box(
                                    Modifier
                                        .size(18.dp)
                                        .align(Alignment.Center)
                                        .clip(CircleShape)
                                        .background(t.accent)
                                )
                            }
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text       = t.displayName.take(8),
                                color      = if (isActive) currentTheme.accent else currentTheme.fg2,
                                fontSize   = 7.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign  = TextAlign.Center,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SheetField(label: String, value: String, theme: DroidTheme, onValueChange: (String) -> Unit) {
    Column {
        Text(label, color = theme.fg2, fontSize = 8.sp, fontFamily = FontFamily.Monospace, modifier = Modifier.padding(bottom = 2.dp))
        BasicTextField(
            value         = value,
            onValueChange = onValueChange,
            modifier      = Modifier
                .fillMaxWidth()
                .background(theme.bg, RoundedCornerShape(4.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            textStyle     = TextStyle(color = theme.fg, fontSize = 11.sp, fontFamily = FontFamily.Monospace),
            cursorBrush   = SolidColor(theme.accent),
            singleLine    = true,
        )
    }
}

// ─── QUEUE tab ────────────────────────────────────────────────

@Composable
private fun PlaylistTab(
    playerState: PlayerState,
    theme:       DroidTheme,
    onJump:      (Int) -> Unit,
) {
    if (playerState.queue.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Queue is empty", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        }
        return
    }

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = playerState.queueIndex.coerceAtLeast(0),
    )

    LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
        itemsIndexed(playerState.queue) { idx, track ->
            val isCurrent = idx == playerState.queueIndex
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isCurrent) theme.surface else Color.Transparent)
                    .clickable { onJump(idx) }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text       = "%02d".format(idx + 1),
                    color      = if (isCurrent) theme.accent else theme.fg2,
                    fontSize   = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier.width(24.dp),
                )
                Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    Text(
                        text       = track.title,
                        color      = if (isCurrent) theme.accent else theme.fg,
                        fontSize   = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 1.dp)) {
                        SourceBadge(track.source, theme)
                        if (track.suffix.isNotEmpty()) FormatBadge(track.suffix.uppercase(), theme)
                    }
                }
                Text(
                    text       = "%d:%02d".format(track.duration / 60000, (track.duration % 60000) / 1000),
                    color      = theme.fg2,
                    fontSize   = 10.sp,
                    fontFamily = FontFamily.Monospace,
                )
            }
            HorizontalDivider(color = theme.border, thickness = 0.5.dp)
        }
    }
}

// ─── LIBRARY tab ─────────────────────────────────────────────

@Composable
private fun LibraryTabContent(
    libraryViewModel: LibraryViewModel,
    playerViewModel:  PlayerViewModel,
    theme:            DroidTheme,
) {
    val uiState by libraryViewModel.uiState.collectAsState()

    when {
        uiState.isLoading && uiState.albums.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = theme.accent)
            }
        }
        uiState.selectedAlbum != null -> {
            LibraryAlbumTracks(
                album   = uiState.selectedAlbum!!,
                tracks  = uiState.selectedAlbumTracks,
                theme   = theme,
                onBack  = { libraryViewModel.clearAlbumSelection() },
                onTrack = { idx -> playerViewModel.playTracks(uiState.selectedAlbumTracks, idx) },
            )
        }
        else -> {
            Column(Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(theme.panel)
                        .padding(horizontal = 12.dp),
                ) {
                    listOf(
                        "Albums"    to com.droidamp.ui.library.LibraryTab.Albums,
                        "Artists"   to com.droidamp.ui.library.LibraryTab.Artists,
                        "Playlists" to com.droidamp.ui.library.LibraryTab.Playlists,
                    ).forEach { (label, tab) ->
                        val active = uiState.tab::class == tab::class
                        Text(
                            text       = label,
                            color      = if (active) theme.accent else theme.fg2,
                            fontSize   = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            modifier   = Modifier
                                .clickable { libraryViewModel.selectTab(tab) }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                        )
                    }
                }
                HorizontalDivider(color = theme.border, thickness = 0.5.dp)
                Box(Modifier.weight(1f)) {
                    when {
                        uiState.tab is com.droidamp.ui.library.LibraryTab.Albums -> {
                            LibraryAlbumGrid(uiState.albums, theme) { libraryViewModel.loadAlbumTracks(it) }
                        }
                        uiState.tab is com.droidamp.ui.library.LibraryTab.Artists -> {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(uiState.artists) { artist ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { libraryViewModel.loadArtistAlbums(artist) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Text("♪", color = theme.accent, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
                                        Column {
                                            Text(artist.name, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            Text("${artist.albumCount} albums", color = theme.fg2, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    HorizontalDivider(color = theme.border, thickness = 0.5.dp)
                                }
                            }
                        }
                        uiState.tab is com.droidamp.ui.library.LibraryTab.Playlists -> {
                            LazyColumn(Modifier.fillMaxSize()) {
                                items(uiState.playlists) { pl ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { libraryViewModel.loadPlaylistTracks(pl) }
                                            .padding(horizontal = 14.dp, vertical = 10.dp),
                                    ) {
                                        Text("≡", color = theme.accent, fontSize = 16.sp, modifier = Modifier.padding(end = 10.dp))
                                        Column {
                                            Text(pl.name, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                                            Text("${pl.trackCount} tracks", color = theme.fg2, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    HorizontalDivider(color = theme.border, thickness = 0.5.dp)
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryAlbumGrid(albums: List<Album>, theme: DroidTheme, onAlbumClick: (Album) -> Unit) {
    LazyVerticalGrid(
        columns               = GridCells.Fixed(3),
        contentPadding        = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement   = Arrangement.spacedBy(6.dp),
        modifier              = Modifier.fillMaxSize(),
    ) {
        gridItems(albums) { album ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(5.dp))
                    .background(theme.panel)
                    .clickable { onAlbumClick(album) }
                    .padding(bottom = 5.dp),
            ) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f).background(theme.surface)) {
                    if (album.coverArtId != null) AsyncImage(model = album.coverArtId, contentDescription = null, modifier = Modifier.fillMaxSize())
                    else Text("♫", color = theme.accent, fontSize = 22.sp, modifier = Modifier.align(Alignment.Center))
                }
                Spacer(Modifier.height(3.dp))
                Text(album.name, color = theme.fg, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
                Text(album.artist, color = theme.fg2, fontSize = 8.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(horizontal = 4.dp))
            }
        }
    }
}

@Composable
private fun LibraryAlbumTracks(album: Album, tracks: List<Track>, theme: DroidTheme, onBack: () -> Unit, onTrack: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(theme.panel).padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("← Back", color = theme.accent, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 10.dp))
            Box(Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(theme.surface)) {
                if (album.coverArtId != null) AsyncImage(model = album.coverArtId, contentDescription = null, modifier = Modifier.fillMaxSize())
                else Text("♫", color = theme.accent, fontSize = 16.sp, modifier = Modifier.align(Alignment.Center))
            }
            Spacer(Modifier.width(8.dp))
            Column {
                Text(album.name, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(album.artist, color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
        HorizontalDivider(color = theme.border, thickness = 0.5.dp)
        LazyColumn(Modifier.weight(1f)) {
            if (tracks.isEmpty()) {
                item { Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = theme.accent) } }
            }
            itemsIndexed(tracks) { idx, track ->
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onTrack(idx) }.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (track.trackNumber > 0) "%02d".format(track.trackNumber) else "  ", color = theme.fg2, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace, modifier = Modifier.width(24.dp))
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(track.title, color = theme.fg, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.suffix.uppercase(), color = theme.yellow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text("%d:%02d".format(track.duration / 60000, (track.duration % 60000) / 1000), color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                }
                HorizontalDivider(color = theme.border, thickness = 0.5.dp)
            }
        }
    }
}

// ─── SOURCES tab ─────────────────────────────────────────────

@Composable
private fun SourcesTab(settingsViewModel: SettingsViewModel, theme: DroidTheme) {
    val url        by settingsViewModel.url.collectAsState()
    val pingStatus by settingsViewModel.pingStatus.collectAsState()

    val isConnected = pingStatus?.startsWith("✓") == true
    val isPending   = pingStatus == "connecting…"

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).clip(CircleShape).background(when {
                    isPending   -> theme.yellow
                    isConnected -> theme.green
                    else        -> theme.red.copy(alpha = 0.6f)
                }))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Navidrome", color = theme.fg, fontSize = 13.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    Text(url.ifBlank { "not configured" }, color = theme.fg2, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(pingStatus ?: "", color = if (isConnected) theme.green else theme.yellow, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            HorizontalDivider(color = theme.border, thickness = 0.5.dp)
        }
        listOf("SoundCloud" to "SC", "Bandcamp" to "BC", "Internet Radio" to "RADIO").forEach { (name, badge) ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(theme.fg2.copy(alpha = 0.2f)))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(name, color = theme.fg2.copy(alpha = 0.5f), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                        Text("coming soon", color = theme.fg2.copy(alpha = 0.3f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text(badge, color = theme.fg2.copy(alpha = 0.3f), fontSize = 8.sp, fontFamily = FontFamily.Monospace,
                        modifier = Modifier.background(theme.fg2.copy(alpha = 0.06f), RoundedCornerShape(3.dp)).padding(horizontal = 5.dp, vertical = 2.dp))
                }
                HorizontalDivider(color = theme.border, thickness = 0.5.dp)
            }
        }
    }
}

// ─── SEARCH tab ──────────────────────────────────────────────

@Composable
private fun SearchTabContent(searchViewModel: SearchViewModel, playerViewModel: PlayerViewModel, theme: DroidTheme) {
    val uiState by searchViewModel.uiState.collectAsState()

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().background(theme.surface).padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("⌕", color = theme.fg2, fontSize = 16.sp, modifier = Modifier.padding(end = 8.dp))
            BasicTextField(
                value         = uiState.query,
                onValueChange = { searchViewModel.setQuery(it) },
                modifier      = Modifier.weight(1f),
                textStyle     = TextStyle(color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace),
                cursorBrush   = SolidColor(theme.accent),
                singleLine    = true,
                decorationBox = { inner ->
                    if (uiState.query.isEmpty()) Text("Search tracks, albums, artists…", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    inner()
                },
            )
            if (uiState.query.isNotEmpty()) {
                Text("✕", color = theme.fg2, fontSize = 12.sp,
                    modifier = Modifier.clickable { searchViewModel.setQuery("") }.padding(start = 6.dp))
            }
        }
        HorizontalDivider(color = theme.border, thickness = 0.5.dp)

        when {
            uiState.selectedAlbum != null -> {
                LibraryAlbumTracks(
                    album   = uiState.selectedAlbum!!,
                    tracks  = uiState.selectedAlbumTracks,
                    theme   = theme,
                    onBack  = { searchViewModel.clearAlbumSelection() },
                    onTrack = { idx -> playerViewModel.playTracks(uiState.selectedAlbumTracks, idx) },
                )
            }
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = theme.accent) }
            }
            uiState.query.length >= 2 -> {
                val results = uiState.results
                LazyColumn(Modifier.fillMaxSize()) {
                    if (results.artists.isNotEmpty()) {
                        item { SectionHeader("ARTISTS", theme) }
                        items(results.artists) { artist ->
                            Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("♪", color = theme.accent, fontSize = 14.sp, modifier = Modifier.padding(end = 10.dp))
                                Text(artist.name, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                            HorizontalDivider(color = theme.border, thickness = 0.5.dp)
                        }
                    }
                    if (results.albums.isNotEmpty()) {
                        item { SectionHeader("ALBUMS", theme) }
                        items(results.albums) { album ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { searchViewModel.loadAlbumTracks(album) }.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(36.dp).clip(RoundedCornerShape(4.dp)).background(theme.surface)) {
                                    if (album.coverArtId != null) AsyncImage(model = album.coverArtId, contentDescription = null, modifier = Modifier.fillMaxSize())
                                    else Text("♫", color = theme.accent, fontSize = 14.sp, modifier = Modifier.align(Alignment.Center))
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(album.name, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(album.artist, color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }
                            HorizontalDivider(color = theme.border, thickness = 0.5.dp)
                        }
                    }
                    if (results.tracks.isNotEmpty()) {
                        item { SectionHeader("TRACKS", theme) }
                        itemsIndexed(results.tracks) { idx, track ->
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { playerViewModel.playTracks(results.tracks, idx) }.padding(horizontal = 14.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(track.title, color = theme.fg, fontSize = 12.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${track.artist} · ${track.album}", color = theme.fg2, fontSize = 9.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                Text("%d:%02d".format(track.duration / 60000, (track.duration % 60000) / 1000), color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            }
                            HorizontalDivider(color = theme.border, thickness = 0.5.dp)
                        }
                    }
                    if (results.artists.isEmpty() && results.albums.isEmpty() && results.tracks.isEmpty()) {
                        item {
                            Box(Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No results", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Type to search", color = theme.fg2, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, theme: DroidTheme) {
    Text(
        text       = title,
        color      = theme.fg2,
        fontSize   = 9.sp,
        fontFamily = FontFamily.Monospace,
        modifier   = Modifier.fillMaxWidth().background(theme.panel).padding(horizontal = 14.dp, vertical = 5.dp),
    )
}
