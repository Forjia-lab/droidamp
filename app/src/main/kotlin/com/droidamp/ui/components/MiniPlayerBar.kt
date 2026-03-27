package com.droidamp.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.droidamp.domain.model.PlayerState
import com.droidamp.ui.theme.DroidTheme

// ─────────────────────────────────────────────────────────────
//  MiniPlayerBar — sticky bar at the bottom of Library, Search,
//  Sources, and Settings screens.
//
//  Shows: album art · track title · artist · play/pause · next
//  Tap anywhere (not on controls) → navigate back to PlayerScreen
//  Long-press → open "Add to Gig Bag" sheet
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MiniPlayerBar(
    playerState:  PlayerState,
    theme:        DroidTheme,
    coverArtUrl:  String?,
    onTap:        () -> Unit,
    onPlayPause:  () -> Unit,
    onNext:       () -> Unit,
    onLongPress:  () -> Unit = {},
    modifier:     Modifier = Modifier,
) {
    val track = playerState.currentTrack ?: return   // nothing playing → don't show

    AnimatedVisibility(
        visible = true,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(theme.panel)
                .combinedClickable(onClick = onTap, onLongClick = onLongPress)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Album art
            AsyncImage(
                model              = coverArtUrl,
                contentDescription = null,
                modifier           = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(theme.surface),
            )

            Spacer(Modifier.width(10.dp))

            // Track info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = track.title,
                    color      = theme.fg,
                    fontSize   = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    text       = track.artist,
                    color      = theme.fg2,
                    fontSize   = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
            }

            // Progress dot strip (thin, 40dp wide)
            val progress = if (playerState.durationMs > 0)
                (playerState.positionMs.toFloat() / playerState.durationMs).coerceIn(0f, 1f)
            else 0f

            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(theme.surface),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .background(theme.accent),
                )
            }

            Spacer(Modifier.width(8.dp))

            // Play / Pause
            Box(
                modifier         = Modifier.size(36.dp).combinedClickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                if (playerState.isPlaying) {
                    Canvas(modifier = Modifier.size(16.dp)) {
                        val barW = size.width * 0.28f
                        val barH = size.height * 0.75f
                        val top  = (size.height - barH) / 2f
                        drawRect(color = theme.accent, topLeft = Offset(0f, top), size = Size(barW, barH))
                        drawRect(color = theme.accent, topLeft = Offset(size.width - barW, top), size = Size(barW, barH))
                    }
                } else {
                    Text(text = "▶\uFE0E", color = theme.accent, fontSize = 16.sp, fontFamily = FontFamily.Monospace)
                }
            }

            // Next
            Box(
                modifier         = Modifier.size(36.dp).combinedClickable(onClick = onNext),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "▶▶", color = theme.fg2, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}
