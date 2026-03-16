package com.droidamp.ui.player

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.droidamp.ui.theme.ThemeViewModel
import com.droidamp.ui.visualizer.VisualizerCanvas

@Composable
fun PlayerScreen(viewModel: PlayerViewModel, themeViewModel: ThemeViewModel) {
    val playerState by viewModel.playerState.collectAsState()
    val fftData     by viewModel.fftData.collectAsState()
    val vizMode     by viewModel.vizMode.collectAsState()
    val vizFull     by viewModel.isVizFullScreen.collectAsState()
    val theme       by themeViewModel.theme.collectAsState()

    val context = LocalContext.current

    // Request RECORD_AUDIO — needed for android.media.audiofx.Visualizer.
    // Re-runs whenever the screen enters composition (covers first launch and
    // the case where the user previously denied and then returns to this screen).
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.onPermissionGranted()
    }
    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
                == PackageManager.PERMISSION_GRANTED) {
            viewModel.onPermissionGranted()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val track = playerState.currentTrack

    Box(modifier = Modifier.fillMaxSize().background(theme.bg)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────
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
                )
            }

            // ── Visualizer ────────────────────────────────────
            if (!vizFull) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(theme.panel)
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
                    Text(
                        text       = vizMode.label,
                        color      = theme.fg2,
                        fontSize   = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier   = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .background(theme.bg.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
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

            // ── Album art + track info ─────────────────────────
            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(theme.surface)
                ) {
                    if (track?.coverArtId != null) {
                        AsyncImage(model = track.coverArtId, contentDescription = null, modifier = Modifier.fillMaxSize())
                    } else {
                        Text("♫", color = theme.accent, fontSize = 56.sp, modifier = Modifier.align(Alignment.Center))
                    }
                }
                Spacer(Modifier.height(10.dp))
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
                Text(
                    text       = track?.artist ?: "",
                    color      = theme.fg2,
                    fontSize   = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = TextAlign.Center,
                )
                Text(
                    text       = track?.album ?: "",
                    color      = theme.fg2.copy(alpha = 0.6f),
                    fontSize   = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign  = TextAlign.Center,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                // Format badge only (no source badge)
                if (track != null && track.suffix.isNotEmpty()) {
                    Spacer(Modifier.height(5.dp))
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

            // ── Seek bar ──────────────────────────────────────
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

            // ── Transport controls ─────────────────────────────
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
                    modifier   = Modifier.align(Alignment.BottomCenter).padding(20.dp),
                )
            }
        }
    }
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
        Text("⇌", color = shuffleColor, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onShuffle))
        Text("⏮", color = theme.fg, fontSize = 24.sp, modifier = Modifier.clickable(onClick = onPrev))
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(theme.playBg)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (playerState.isPlaying) "⏸" else "▶", color = theme.accent, fontSize = 26.sp)
        }
        Text("⏭", color = theme.fg, fontSize = 24.sp, modifier = Modifier.clickable(onClick = onNext))
        val repeatIcon  = when (playerState.repeatMode) { RepeatMode.OFF -> "↻"; RepeatMode.ALL -> "🔁"; RepeatMode.ONE -> "🔂" }
        val repeatColor = if (playerState.repeatMode != RepeatMode.OFF) theme.accent else theme.fg2
        Text(repeatIcon, color = repeatColor, fontSize = 18.sp, modifier = Modifier.clickable(onClick = onRepeat))
    }
}
