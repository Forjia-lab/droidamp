package com.droidamp.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidamp.ui.theme.DroidTheme
import com.droidamp.ui.theme.DroidThemes
import com.droidamp.ui.theme.ThemeViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel, themeViewModel: ThemeViewModel) {
    val theme      by themeViewModel.theme.collectAsState()

    val url        by viewModel.url.collectAsState()
    val username   by viewModel.username.collectAsState()
    val password   by viewModel.password.collectAsState()
    val pingStatus by viewModel.pingStatus.collectAsState()

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
                value    = password,
                onValue  = { viewModel.setPassword(it) },
                placeholder = "••••••••",
                theme    = theme,
                keyboard = KeyboardType.Password,
                visual   = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailing = {
                    Text(
                        text     = if (showPassword) "🙈" else "👁",
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { showPassword = !showPassword }.padding(4.dp),
                    )
                },
            )

            Spacer(Modifier.height(8.dp))

            // Save button
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

            // Ping status
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

            Spacer(Modifier.height(8.dp))
            Divider(color = theme.border)
            Spacer(Modifier.height(8.dp))

            // Themes section
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
                            // Mini color strip
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
                    // fill remaining slots in last row
                    repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

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
