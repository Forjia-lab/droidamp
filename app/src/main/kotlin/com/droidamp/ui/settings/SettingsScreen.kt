package com.droidamp.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidamp.ui.player.EqBand
import com.droidamp.ui.player.PlayerViewModel
import com.droidamp.ui.theme.DroidTheme
import com.droidamp.ui.theme.DroidThemes
import com.droidamp.ui.theme.ThemeViewModel

private val EQ_LABELS = listOf("60", "170", "310", "600", "1K", "3K", "6K", "12K", "14K", "16K")

@Composable
fun SettingsScreen(
    viewModel:       SettingsViewModel,
    themeViewModel:  ThemeViewModel,
    playerViewModel: PlayerViewModel,
) {
    val theme        by themeViewModel.theme.collectAsState()
    val url          by viewModel.url.collectAsState()
    val username     by viewModel.username.collectAsState()
    val password     by viewModel.password.collectAsState()
    val pingStatus   by viewModel.pingStatus.collectAsState()
    val eqBands      by playerViewModel.eqBands.collectAsState()
    val activePreset by playerViewModel.activePreset.collectAsState()

    var showPassword by remember { mutableStateOf(false) }

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
                text       = "SETTINGS",
                color      = theme.accent,
                fontSize   = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier   = Modifier.weight(1f),
            )
        }

        Column(
            modifier            = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── EQ section ────────────────────────────────────
            Label("EQUALIZER", theme)
            EqSection(
                bands        = eqBands,
                theme        = theme,
                activePreset = activePreset,
                onBandChange = { idx, level -> playerViewModel.setEqBand(idx, level) },
                onPreset     = { playerViewModel.applyPreset(it) },
            )

            Spacer(Modifier.height(4.dp))
            Divider(color = theme.border)
            Spacer(Modifier.height(4.dp))

            // ── ReplayGain section (placeholder) ──────────────
            Label("REPLAYGAIN", theme)
            PlaceholderSection(
                lines = listOf("Mode — off / track / album", "Pre-amp — ±0.0 dB", "Prevent clipping"),
                theme = theme,
            )

            Spacer(Modifier.height(4.dp))
            Divider(color = theme.border)
            Spacer(Modifier.height(4.dp))

            // ── Audio section (placeholder) ───────────────────
            Label("AUDIO", theme)
            PlaceholderSection(
                lines = listOf("Output device", "Sample rate", "Buffer size"),
                theme = theme,
            )

            Spacer(Modifier.height(4.dp))
            Divider(color = theme.border)
            Spacer(Modifier.height(4.dp))

            // ── Themes section ────────────────────────────────
            Label("THEMES", theme)
            Spacer(Modifier.height(4.dp))
            DroidThemes.all.chunked(3).forEach { row ->
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    row.forEach { t ->
                        val isActive = t.id == theme.id
                        Column(
                            modifier            = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(t.bg)
                                .then(
                                    if (isActive) Modifier.border(2.dp, t.accent, RoundedCornerShape(8.dp))
                                    else Modifier
                                )
                                .clickable { themeViewModel.setTheme(t) }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Row(
                                modifier              = Modifier.fillMaxWidth().height(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                listOf(t.accent, t.green, t.yellow, t.red).forEach { c ->
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(c, RoundedCornerShape(2.dp)))
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text      = t.displayName,
                                color     = t.fg,
                                fontSize  = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                textAlign = TextAlign.Center,
                                maxLines  = 2,
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(4.dp))
            Divider(color = theme.border)
            Spacer(Modifier.height(4.dp))

            // ── Server section ────────────────────────────────
            Label("SERVER URL", theme)
            SettingsTextField(
                value       = url,
                onValue     = { viewModel.setUrl(it) },
                placeholder = "http://192.168.1.x:4533",
                theme       = theme,
                keyboard    = KeyboardType.Uri,
            )

            Label("USERNAME", theme)
            SettingsTextField(
                value       = username,
                onValue     = { viewModel.setUsername(it) },
                placeholder = "admin",
                theme       = theme,
            )

            Label("PASSWORD", theme)
            SettingsTextField(
                value       = password,
                onValue     = { viewModel.setPassword(it) },
                placeholder = "••••••••",
                theme       = theme,
                keyboard    = KeyboardType.Password,
                visual      = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing    = {
                    Text(
                        text     = if (showPassword) "🙈" else "👁",
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { showPassword = !showPassword }.padding(4.dp),
                    )
                },
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(theme.accent.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                    .clickable { viewModel.save() }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text       = "SAVE",
                    color      = theme.accent,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                )
            }

            pingStatus?.let { status ->
                val isOk = status.startsWith("✓")
                Text(
                    text       = status,
                    color      = if (isOk) theme.green else if (status == "connecting…") theme.fg2 else theme.red,
                    fontSize   = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }
    }
}

// ─── EQ section ───────────────────────────────────────────────

@Composable
private fun EqSection(
    bands:        List<EqBand>,
    theme:        DroidTheme,
    activePreset: String,
    onBandChange: (Int, Short) -> Unit,
    onPreset:     (String) -> Unit,
) {
    val chipShape = RoundedCornerShape(6.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.panel, RoundedCornerShape(8.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Preset chips
        LazyRow(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(PlayerViewModel.EQ_PRESETS.keys.toList()) { name ->
                val active = name == activePreset
                Box(
                    modifier = Modifier
                        .clip(chipShape)
                        .background(if (active) theme.accent else theme.surface)
                        .clickable { onPreset(name) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Text(
                        text       = name,
                        color      = if (active) theme.bg else theme.fg2,
                        fontSize   = 9.sp,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }
        }
        // EQ bars
        if (bands.isNotEmpty()) {
            Row(
                modifier              = Modifier.fillMaxWidth().height(80.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Bottom,
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
        } else {
            Box(
                modifier = Modifier.fillMaxWidth().height(80.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Play a track to enable EQ", color = theme.fg2, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
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
    val barHeightDp = 52.dp
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
                        val newNorm    = (1f - change.position.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val newLevel   = (band.minLevel + newNorm * levelRange).toInt()
                            .coerceIn(band.minLevel.toInt(), band.maxLevel.toInt()).toShort()
                        onLevelChange(newLevel)
                    }
                }
                .pointerInput(band.minLevel, band.maxLevel) {
                    detectTapGestures { offset ->
                        val levelRange = (band.maxLevel - band.minLevel).toFloat()
                        val newNorm    = (1f - offset.y / size.height.toFloat()).coerceIn(0f, 1f)
                        val newLevel   = (band.minLevel + newNorm * levelRange).toInt()
                            .coerceIn(band.minLevel.toInt(), band.maxLevel.toInt()).toShort()
                        onLevelChange(newLevel)
                    }
                },
        ) {
            drawRect(color = theme.surface)
            val centerY = size.height / 2f
            drawLine(
                color       = theme.accent.copy(alpha = 0.30f),
                start       = Offset(0f, centerY),
                end         = Offset(size.width, centerY),
                strokeWidth = 2f,
            )
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
                drawRect(color = theme.accent, topLeft = Offset(0f, barTop), size = Size(size.width, barBottom - barTop))
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, color = theme.fg2, fontSize = 6.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
    }
}

// ─── Placeholder section ──────────────────────────────────────

@Composable
private fun PlaceholderSection(lines: List<String>, theme: DroidTheme) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(theme.panel, RoundedCornerShape(8.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        lines.forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text       = line,
                    color      = theme.fg2.copy(alpha = 0.5f),
                    fontSize   = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier   = Modifier.weight(1f),
                )
                Text("—", color = theme.fg2.copy(alpha = 0.25f), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
            }
        }
    }
}

// ─── Shared components ────────────────────────────────────────

@Composable
private fun Label(text: String, theme: DroidTheme) {
    Text(text = text, color = theme.fg2, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
}

@Composable
private fun SettingsTextField(
    value: String,
    onValue: (String) -> Unit,
    placeholder: String,
    theme: DroidTheme,
    keyboard: KeyboardType = KeyboardType.Text,
    visual: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value         = value,
        onValueChange = onValue,
        modifier      = Modifier.fillMaxWidth(),
        placeholder   = {
            Text(placeholder, color = theme.fg2.copy(alpha = 0.4f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        },
        textStyle = androidx.compose.ui.text.TextStyle(
            color      = theme.fg,
            fontSize   = 12.sp,
            fontFamily = FontFamily.Monospace,
        ),
        singleLine           = true,
        keyboardOptions      = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = visual,
        trailingIcon         = trailing,
        colors               = OutlinedTextFieldDefaults.colors(
            focusedBorderColor   = theme.accent,
            unfocusedBorderColor = theme.fg2.copy(alpha = 0.3f),
            cursorColor          = theme.accent,
        ),
    )
}
