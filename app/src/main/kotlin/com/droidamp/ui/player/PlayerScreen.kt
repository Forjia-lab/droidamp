package com.droidamp.ui.player

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.droidamp.domain.model.PlayerState
import com.droidamp.domain.model.RepeatMode
import com.droidamp.domain.model.Track
import com.droidamp.domain.model.TrackSource
import com.droidamp.ui.gigbag.GigBagViewModel
import com.droidamp.ui.navigation.AddToGigBagSheet
import com.droidamp.ui.theme.DroidTheme
import com.droidamp.ui.theme.DroidThemes
import com.droidamp.ui.theme.ThemeViewModel
import com.droidamp.ui.visualizer.VisualizerCanvas

// ─────────────────────────────────────────────────────────────
//  PlayerScreen — PLAY tab
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playerViewModel: PlayerViewModel,
    themeViewModel:  ThemeViewModel,
    gigBagViewModel: GigBagViewModel,
) {
    val theme       by themeViewModel.theme.collectAsState()
    val playerState by playerViewModel.playerState.collectAsState()
    val fftData     by playerViewModel.fftData.collectAsState()
    val vizMode     by playerViewModel.vizMode.collectAsState()
    val vizFull     by playerViewModel.isVizFullScreen.collectAsState()

    var showThemeSheet  by remember { mutableStateOf(false) }
    var showGigBagSheet by remember { mutableStateOf(false) }

    if (showThemeSheet) {
        ThemePickerSheet(themeViewModel = themeViewModel, theme = theme, onDismiss = { showThemeSheet = false })
    }

    if (showGigBagSheet) {
        AddToGigBagSheet(
            gigBagViewModel = gigBagViewModel,
            currentTrack    = playerState.currentTrack,
            theme           = theme,
            onDismiss       = { showGigBagSheet = false },
        )
    }

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

    Column(Modifier.fillMaxSize().background(theme.bg)) {

        // 1. Top bar — 48dp ────────────────────────────────────
        TopBar(theme = theme, onMenuTap = { showThemeSheet = true })

        // 2. Mini now-playing bar — 80dp ───────────────────────
        MiniNowPlayingBar(
            track       = playerState.currentTrack,
            theme       = theme,
            onLongPress = { if (playerState.currentTrack != null) showGigBagSheet = true },
        )

        // 3. Visualizer — 100dp ────────────────────────────────
        Box(Modifier.fillMaxWidth().height(100.dp).background(theme.panel)) {
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
            onRepeat    = { playerViewModel.toggleRepeat() },
        )

        // 6. Queue — weight(1f) ────────────────────────────────
        HorizontalDivider(color = theme.border, thickness = 1.dp)
        Box(Modifier.weight(1f)) {
            PlaylistTab(playerState, theme) { playerViewModel.seekToQueueItem(it) }
        }
    }
}

// ─── 1. Top bar — 48dp ───────────────────────────────────────

@Composable
private fun TopBar(theme: DroidTheme, onMenuTap: () -> Unit) {
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
            modifier              = Modifier
                .clickable(onClick = onMenuTap)
                .padding(horizontal = 6.dp, vertical = 4.dp),
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MiniNowPlayingBar(
    track:       Track?,
    theme:       DroidTheme,
    onLongPress: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.surface)
                .combinedClickable(onClick = {}, onLongClick = onLongPress)
                .padding(horizontal = 12.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art — 92dp
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(theme.panel),
            ) {
                if (track?.coverArtId != null) {
                    AsyncImage(model = track.coverArtId, contentDescription = null, modifier = Modifier.fillMaxSize())
                } else {
                    Text("♫", color = theme.accent, fontSize = 36.sp, modifier = Modifier.align(Alignment.Center))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text       = track?.title ?: "No track loaded",
                    color      = theme.fg,
                    fontSize   = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    maxLines   = 2,
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
                        fontSize   = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines   = 1,
                        overflow   = TextOverflow.Ellipsis,
                    )
                }
                if (track != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        SourceBadge(track.source, theme)
                        if (track.suffix.isNotEmpty()) FormatBadge(track.suffix.uppercase(), theme)
                        if (!track.bpm.isNullOrBlank()) MicroBadge("${track.bpm} BPM", theme.accent)
                        if (!track.camelotKey.isNullOrBlank()) MicroBadge(track.camelotKey, theme.accent)
                    }
                }
            }
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
        modifier            = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(theme.surface)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.Center,
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
    onRepeat:    () -> Unit,
) {
    val btnShape = RoundedCornerShape(8.dp)

    @Composable
    fun HwButton(
        onClick:     () -> Unit,
        borderColor: Color = theme.border,
        bgColor:     Color = theme.surface,
        width:       androidx.compose.ui.unit.Dp = 52.dp,
        height:      androidx.compose.ui.unit.Dp = 44.dp,
        content:     @Composable () -> Unit,
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        Box(
            modifier = Modifier
                .size(width = width, height = height)
                .clip(btnShape)
                .background(if (isPressed) theme.panel else bgColor)
                .border(1.dp, borderColor, btnShape)
                .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { content() }
    }

    Row(
        modifier              = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .background(theme.bg)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment     = Alignment.CenterVertically,
    ) {
        // Shuffle
        HwButton(
            onClick     = onShuffle,
            borderColor = if (playerState.isShuffled) theme.accent else theme.border,
        ) {
            Text("⇄", color = if (playerState.isShuffled) theme.accent else theme.fg,
                fontSize = 18.sp, fontFamily = FontFamily.Monospace)
        }
        // Prev
        HwButton(onClick = onPrev) {
            Text("◀◀", color = theme.fg, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        }
        // Play/Pause — larger than prev/next
        HwButton(
            onClick     = onPlayPause,
            bgColor     = theme.playBg,
            borderColor = theme.playBorder,
            width       = 70.dp,
            height      = 54.dp,
        ) {
            if (playerState.isPlaying) {
                val accentColor = theme.accent
                Canvas(modifier = Modifier.size(16.dp)) {
                    val barW = size.width * 0.28f
                    val barH = size.height * 0.75f
                    val top  = (size.height - barH) / 2f
                    drawRect(color = accentColor, topLeft = Offset(0f, top), size = Size(barW, barH))
                    drawRect(color = accentColor, topLeft = Offset(size.width - barW, top), size = Size(barW, barH))
                }
            } else {
                Text("▶\uFE0E", color = theme.accent, fontSize = 20.sp, fontFamily = FontFamily.Monospace)
            }
        }
        // Next
        HwButton(onClick = onNext) {
            Text("▶▶", color = theme.fg, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        }
        // Repeat
        HwButton(
            onClick     = onRepeat,
            borderColor = if (playerState.repeatMode != RepeatMode.OFF) theme.accent else theme.border,
        ) {
            val (repeatIcon, repeatColor) = when (playerState.repeatMode) {
                RepeatMode.OFF -> "↻"  to theme.fg
                RepeatMode.ALL -> "↻"  to theme.accent
                RepeatMode.ONE -> "↻1" to theme.accent
            }
            Text(repeatIcon, color = repeatColor, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

// ─── Queue tab ────────────────────────────────────────────────

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

    // Auto-scroll to the active track whenever the index changes
    LaunchedEffect(playerState.queueIndex) {
        val target = playerState.queueIndex.coerceAtLeast(0)
        if (target < playerState.queue.size) {
            listState.animateScrollToItem(target)
        }
    }

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

// ─── Shared library composables (used by SearchScreen) ────────

@Composable
fun LibraryAlbumTracks(
    album:   com.droidamp.domain.model.Album,
    tracks:  List<Track>,
    theme:   DroidTheme,
    onBack:  () -> Unit,
    onTrack: (Int) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.panel)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("←", color = theme.accent, fontSize = 16.sp,
                modifier = Modifier.clickable(onClick = onBack).padding(end = 10.dp))
            Text(
                text       = album.name,
                color      = theme.fg,
                fontSize   = 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.weight(1f),
                maxLines   = 1,
                overflow   = TextOverflow.Ellipsis,
            )
        }
        HorizontalDivider(color = theme.border, thickness = 0.5.dp)
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(tracks) { idx, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onTrack(idx) }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        if (track.trackNumber > 0) "%02d".format(track.trackNumber) else "  ",
                        color = theme.fg2, fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace, modifier = Modifier.width(24.dp),
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 8.dp)) {
                        Text(track.title, color = theme.fg, fontSize = 11.sp, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(track.suffix.uppercase(), color = theme.yellow, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
                    }
                    Text(
                        "%d:%02d".format(track.duration / 60000, (track.duration % 60000) / 1000),
                        color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace,
                    )
                }
                HorizontalDivider(color = theme.border, thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun LibraryAlbumGrid(
    albums:       List<com.droidamp.domain.model.Album>,
    theme:        DroidTheme,
    onAlbumClick: (com.droidamp.domain.model.Album) -> Unit,
) {
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
            }
        }
    }
}

// ─── Theme picker sheet ───────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThemePickerSheet(
    themeViewModel: ThemeViewModel,
    theme:          DroidTheme,
    onDismiss:      () -> Unit,
) {
    val currentTheme by themeViewModel.theme.collectAsState()
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        containerColor   = theme.panel,
        contentColor     = theme.fg,
    ) {
        Text(
            text       = "THEMES",
            color      = theme.fg2,
            fontSize   = 9.sp,
            fontFamily = FontFamily.Monospace,
            modifier   = Modifier.padding(start = 16.dp, bottom = 10.dp),
        )
        LazyRow(
            modifier              = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 16.dp),
            contentPadding        = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(DroidThemes.all) { t ->
                val isActive = t.id == currentTheme.id
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier            = Modifier.clickable { themeViewModel.setTheme(t) },
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(t.bg)
                            .then(
                                if (isActive) Modifier.border(2.dp, currentTheme.accent, RoundedCornerShape(8.dp))
                                else Modifier
                            ),
                    ) {
                        Box(
                            Modifier
                                .size(22.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(t.accent)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text       = t.displayName.take(9),
                        color      = if (isActive) currentTheme.accent else currentTheme.fg2,
                        fontSize   = 7.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign  = TextAlign.Center,
                    )
                }
            }
        }
    }
}
