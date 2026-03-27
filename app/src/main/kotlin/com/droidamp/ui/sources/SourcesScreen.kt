package com.droidamp.ui.sources

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidamp.data.local.LocalMediaRepository
import com.droidamp.ui.library.LibraryViewModel
import com.droidamp.ui.settings.SettingsViewModel
import com.droidamp.ui.theme.ThemeViewModel

@Composable
fun SourcesScreen(
    settingsViewModel:        SettingsViewModel,
    libraryViewModel:         LibraryViewModel,
    themeViewModel:           ThemeViewModel,
    onNavigateToLocalLibrary: () -> Unit,
) {
    val theme      by themeViewModel.theme.collectAsState()
    val url        by settingsViewModel.url.collectAsState()
    val pingStatus by settingsViewModel.pingStatus.collectAsState()
    val libState   by libraryViewModel.uiState.collectAsState()

    val isConnected = pingStatus?.startsWith("✓") == true
    val isPending   = pingStatus == "connecting…"

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onNavigateToLocalLibrary()
        else libraryViewModel.refreshLocalPermission()
    }

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
                text       = "SOURCES",
                color      = theme.accent,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier   = Modifier.weight(1f),
            )
        }
        HorizontalDivider(color = theme.border, thickness = 0.5.dp)

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            // ── Navidrome row ─────────────────────────────────────
            item {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(
                            when {
                                isPending   -> theme.yellow
                                isConnected -> theme.green
                                else        -> theme.red.copy(alpha = 0.6f)
                            }
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Navidrome",
                            color      = theme.fg,
                            fontSize   = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            url.ifBlank { "not configured" },
                            color      = theme.fg2,
                            fontSize   = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines   = 1,
                            overflow   = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        pingStatus ?: "",
                        color      = if (isConnected) theme.green else theme.yellow,
                        fontSize   = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
                HorizontalDivider(color = theme.border, thickness = 0.5.dp)
            }
            // ── Local Storage row ─────────────────────────────────
            item {
                Row(
                    modifier          = Modifier
                        .fillMaxWidth()
                        .clickable {
                            when {
                                libState.isLocalScanning    -> { /* wait */ }
                                libState.localHasPermission -> onNavigateToLocalLibrary()
                                else -> permissionLauncher.launch(LocalMediaRepository.REQUIRED_PERMISSION)
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(8.dp).clip(CircleShape).background(
                            if (libState.localHasPermission) theme.green else theme.fg2.copy(alpha = 0.3f)
                        )
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Local Storage",
                            color      = theme.fg,
                            fontSize   = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            when {
                                libState.isLocalScanning     -> "scanning…"
                                !libState.localHasPermission -> "tap to grant permission"
                                libState.localTrackCount > 0 -> "${libState.localTrackCount} tracks · tap to browse"
                                else                         -> "tap to browse"
                            },
                            color      = theme.fg2,
                            fontSize   = 9.sp,
                            fontFamily = FontFamily.Monospace,
                        )
                    }
                    Text(
                        "LOCAL",
                        color    = theme.accent.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .background(theme.accent.copy(alpha = 0.08f), RoundedCornerShape(3.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    )
                }
                HorizontalDivider(color = theme.border, thickness = 0.5.dp)
            }
            // ── Placeholder rows ──────────────────────────────────
            listOf("SoundCloud" to "SC", "Bandcamp" to "BC", "Internet Radio" to "RADIO").forEach { (name, badge) ->
                item {
                    Row(
                        modifier          = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(theme.fg2.copy(alpha = 0.2f)))
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, color = theme.fg2.copy(alpha = 0.5f), fontSize = 13.sp, fontFamily = FontFamily.Monospace)
                            Text("coming soon", color = theme.fg2.copy(alpha = 0.3f), fontSize = 9.sp, fontFamily = FontFamily.Monospace)
                        }
                        Text(
                            badge,
                            color    = theme.fg2.copy(alpha = 0.3f),
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .background(theme.fg2.copy(alpha = 0.06f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp),
                        )
                    }
                    HorizontalDivider(color = theme.border, thickness = 0.5.dp)
                }
            }
        }
    }
}
