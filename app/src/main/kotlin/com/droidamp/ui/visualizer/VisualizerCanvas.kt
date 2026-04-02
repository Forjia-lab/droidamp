package com.droidamp.ui.visualizer

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.*

// ─────────────────────────────────────────────────────────────
//  VisualizerCanvas
//
//  fftData     : FloatArray(20) of 0..1 normalised band amplitudes
//  waveformData: FloatArray(128) of -1..1 time-domain samples (for Oscilloscope)
//  accentColor / secondaryColor: from the active DroidTheme
//  Swipe left/right → cycle modes
// ─────────────────────────────────────────────────────────────

@Composable
fun VisualizerCanvas(
    fftData:         FloatArray,
    mode:            VisualizerMode,
    accentColor:     Color,
    secondaryColor:  Color,
    backgroundColor: Color,
    modifier:        Modifier = Modifier,
    waveformData:    FloatArray = FloatArray(128) { 0f },
    onSwipeNext:     () -> Unit = {},
    onSwipePrev:     () -> Unit = {},
) {
    var swipeAccum by remember { mutableFloatStateOf(0f) }
    val animTime   = remember { mutableFloatStateOf(0f) }

    // VU meter peak-hold state (L = bands 0-9, R = bands 10-19)
    var vuPeakL   by remember { mutableFloatStateOf(0f) }
    var vuPeakR   by remember { mutableFloatStateOf(0f) }
    var vuCountL  by remember { mutableIntStateOf(0) }
    var vuCountR  by remember { mutableIntStateOf(0) }

    LaunchedEffect(fftData) {
        animTime.value += 0.06f

        // Update VU peak hold
        val lAmp = fftData.slice(0..9).average().toFloat()
        val rAmp = fftData.slice(10..19).average().toFloat()
        if (lAmp > vuPeakL) { vuPeakL = lAmp; vuCountL = 28 }
        else if (vuCountL > 0) vuCountL--
        else vuPeakL = (vuPeakL - 0.018f).coerceAtLeast(lAmp)

        if (rAmp > vuPeakR) { vuPeakR = rAmp; vuCountR = 28 }
        else if (vuCountR > 0) vuCountR--
        else vuPeakR = (vuPeakR - 0.018f).coerceAtLeast(rAmp)
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { swipeAccum = 0f },
                    onHorizontalDrag = { _, delta ->
                        swipeAccum += delta
                        if (swipeAccum >  60f) { onSwipeNext(); swipeAccum = 0f }
                        if (swipeAccum < -60f) { onSwipePrev(); swipeAccum = 0f }
                    }
                )
            }
    ) {
        drawRect(color = backgroundColor)
        when (mode) {
            VisualizerMode.RADIAL       -> drawRadial(fftData, accentColor, secondaryColor)
            VisualizerMode.SCATTER      -> drawScatter(fftData, accentColor)
            VisualizerMode.BARS_REBORN  -> drawBarsReborn(fftData, accentColor)
            VisualizerMode.OSCILLOSCOPE -> drawOscilloscope(waveformData, accentColor)
            VisualizerMode.PLASMA       -> drawPlasma(fftData, accentColor, animTime.value)
            VisualizerMode.VU_METERS    -> drawVuMeters(fftData, accentColor, secondaryColor, vuPeakL, vuPeakR)
        }
    }
}

// ─── RADIAL ──────────────────────────────────────────────────
//
// Circular spectrum: bars radiate outward from a central ring.
// Line width and length both scale with amplitude.
// Mirror symmetry (each band drawn twice, 180° apart) gives
// a classic spectrum-analyser look.
private fun DrawScope.drawRadial(fft: FloatArray, accent: Color, secondary: Color) {
    val cx     = size.width  / 2f
    val cy     = size.height / 2f
    val maxR   = minOf(cx, cy) * 0.92f
    val innerR = maxR * 0.28f
    val n      = fft.size

    // Soft centre glow
    drawCircle(
        brush  = Brush.radialGradient(
            colors  = listOf(accent.copy(alpha = 0.18f), accent.copy(alpha = 0f)),
            center  = Offset(cx, cy),
            radius  = innerR * 1.6f,
        ),
        radius = innerR * 1.6f,
        center = Offset(cx, cy),
    )
    // Inner ring
    drawCircle(
        color  = accent.copy(alpha = 0.35f),
        radius = innerR,
        center = Offset(cx, cy),
        style  = Stroke(width = 1f),
    )

    fft.forEachIndexed { i, amp ->
        val angle   = (i.toFloat() / n) * 2f * PI.toFloat() - PI.toFloat() / 2f
        val barLen  = amp * (maxR - innerR)
        val strokeW = (2f + amp * 7f).coerceAtMost(12f)
        val alpha   = (0.45f + amp * 0.55f).coerceAtMost(1f)
        val col     = if (amp > 0.65f) accent else accent.copy(alpha = alpha)

        // Primary bar
        drawLine(
            color       = col,
            start       = Offset(cx + cos(angle) * innerR, cy + sin(angle) * innerR),
            end         = Offset(cx + cos(angle) * (innerR + barLen), cy + sin(angle) * (innerR + barLen)),
            strokeWidth = strokeW,
            cap         = StrokeCap.Round,
        )
        // Mirror bar 180°
        val mirror = angle + PI.toFloat()
        drawLine(
            color       = secondary.copy(alpha = alpha * 0.65f),
            start       = Offset(cx + cos(mirror) * innerR, cy + sin(mirror) * innerR),
            end         = Offset(cx + cos(mirror) * (innerR + barLen * 0.85f), cy + sin(mirror) * (innerR + barLen * 0.85f)),
            strokeWidth = strokeW * 0.7f,
            cap         = StrokeCap.Round,
        )
    }
}

// ─── SCATTER ─────────────────────────────────────────────────
private fun DrawScope.drawScatter(fft: FloatArray, color: Color) {
    val total = 60
    repeat(total) { i ->
        val normI = i.toFloat() / total
        val band  = (normI * (fft.size - 1)).toInt().coerceIn(0, fft.size - 1)
        val amp   = fft[band]
        val x     = (i * 137.508f) % size.width
        val y     = size.height - ((i * 97.3f + amp * size.height * 0.8f) % size.height)
        val r     = 2f + amp * 5f
        val alpha = (0.3f + amp * 0.7f).coerceIn(0f, 1f)
        drawCircle(color = color.copy(alpha = alpha), radius = r, center = Offset(x, y))
    }
}

// ─── BARS REBORN ─────────────────────────────────────────────
//
// Single thick-bar mode: each band is a rounded-cap line.
// Two passes per bar: a wider semi-transparent bloom behind,
// then the sharp opaque bar on top.
private fun DrawScope.drawBarsReborn(fft: FloatArray, accent: Color) {
    val n     = fft.size
    val slotW = size.width / n
    val barW  = slotW * 0.72f

    fft.forEachIndexed { i, amp ->
        val barH = amp * size.height * 0.90f
        if (barH < 1f) return@forEachIndexed
        val cx  = i * slotW + slotW / 2f
        val top = size.height - barH

        // Bloom pass — wider and semi-transparent
        drawLine(
            color       = accent.copy(alpha = 0.22f),
            start       = Offset(cx, top),
            end         = Offset(cx, size.height),
            strokeWidth = barW * 2.6f,
            cap         = StrokeCap.Round,
        )
        // Sharp bar on top
        drawLine(
            color       = accent.copy(alpha = 0.92f),
            start       = Offset(cx, top),
            end         = Offset(cx, size.height),
            strokeWidth = barW,
            cap         = StrokeCap.Round,
        )
    }
}

// ─── OSCILLOSCOPE ────────────────────────────────────────────
//
// Renders actual time-domain waveform data as a single bright line
// with a phosphor glow behind it — classic analog CRT aesthetic.
private fun DrawScope.drawOscilloscope(waveform: FloatArray, accent: Color) {
    if (waveform.isEmpty()) return
    val W    = size.width
    val H    = size.height
    val midY = H / 2f

    // Centre baseline
    drawLine(
        color       = accent.copy(alpha = 0.12f),
        start       = Offset(0f, midY),
        end         = Offset(W, midY),
        strokeWidth = 0.8f,
    )

    val path = Path()
    waveform.forEachIndexed { i, sample ->
        val x = i.toFloat() / (waveform.size - 1) * W
        val y = midY - sample * (H * 0.44f)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    // Phosphor glow (wide, dim)
    drawPath(path, color = accent.copy(alpha = 0.18f), style = Stroke(width = 6f,  cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Mid glow
    drawPath(path, color = accent.copy(alpha = 0.40f), style = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Sharp trace
    drawPath(path, color = accent,                      style = Stroke(width = 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// ─── PLASMA / FLUID ──────────────────────────────────────────
//
// Canvas-grid plasma: overlapping sine wave interference per cell.
// Bass drives scale pulse, mid drives hue shift speed, beat drives brightness.
// Adaptive cell size targets >= 30 fps on lower-end devices.
private fun DrawScope.drawPlasma(fft: FloatArray, accent: Color, time: Float) {
    val bassAmp = fft.slice(0..4).average().toFloat().coerceIn(0f, 1f)
    val midAmp  = fft.slice(5..12).average().toFloat().coerceIn(0f, 1f)
    val beat    = fft.average().toFloat().coerceIn(0f, 1f)

    // Adaptive cell size: keep grid ≤ 24×16 to maintain 30 fps
    val cellSize  = (size.width / 24f).coerceAtLeast(14f)
    val cols      = (size.width  / cellSize).toInt() + 1
    val rows      = (size.height / cellSize).toInt() + 1

    // Hue base cycles over time; mid amplitude shifts hue speed
    val hueBase    = (time * 22f + midAmp * 220f) % 360f
    val brightness = 0.28f + beat * 0.62f
    // Bass zoom: scales the sampling coordinates for a pronounced pulse effect
    val scale      = 1f + bassAmp * 0.85f

    // Accent hue used to tint the palette toward the theme color
    val accentHue = colorToHue(accent)

    for (row in 0..rows) {
        for (col in 0..cols) {
            val nx = col.toFloat() / cols.toFloat() * scale
            val ny = row.toFloat() / rows.toFloat() * scale

            val v = sin(nx * 3.1f + time) +
                    sin(ny * 2.7f + time * 0.7f) +
                    sin((nx + ny) * 2.3f + time * 1.3f)

            // v in ~[-3, 3] → normalize to [0, 1]
            val norm = (v / 3f + 1f) / 2f

            val hue = (accentHue + hueBase * 0.4f + norm * 140f) % 360f
            val sat = 0.75f + norm * 0.25f

            drawRect(
                color   = hslToColor(hue, sat, brightness),
                topLeft = Offset(col * cellSize, row * cellSize),
                size    = Size(cellSize + 0.5f, cellSize + 0.5f),  // 0.5 overlap avoids grid gaps
            )
        }
    }
}

private fun colorToHue(color: Color): Float {
    val r = color.red
    val g = color.green
    val b = color.blue
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    if (delta < 0.001f) return 0f
    val h = when (max) {
        r    -> 60f * (((g - b) / delta) % 6f)
        g    -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }
    return (h + 360f) % 360f
}

private fun hslToColor(h: Float, s: Float, l: Float): Color {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f
    val (r, g, b) = when {
        h < 60f  -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else     -> Triple(c, 0f, x)
    }
    return Color(r + m, g + m, b + m)
}

// ─── VU METERS ───────────────────────────────────────────────
//
// Two tall vertical bars (L from low bands, R from high bands).
// Peak hold: indicator dot sits at the last peak for ~28 frames
// then drifts down. Top 20 % of bar uses warning/secondary color.
private fun DrawScope.drawVuMeters(
    fft:      FloatArray,
    accent:   Color,
    warning:  Color,
    peakL:    Float,
    peakR:    Float,
) {
    val lAmp = fft.slice(0..9).average().toFloat().coerceIn(0f, 1f)
    val rAmp = fft.slice(10..19).average().toFloat().coerceIn(0f, 1f)

    val barW     = size.width  * 0.28f
    val barH     = size.height * 0.90f
    val barTop   = (size.height - barH) / 2f
    val lLeft    = size.width  * 0.10f
    val rLeft    = size.width  - lLeft - barW
    val gap      = 2.5f
    val segCount = 24
    val segH     = (barH - gap * (segCount - 1)) / segCount

    fun drawMeter(left: Float, amp: Float, peak: Float) {
        val filledSegs = (amp * segCount).toInt().coerceIn(0, segCount)
        val peakSeg    = (peak * segCount).toInt().coerceIn(0, segCount - 1)
        val warnStart  = (segCount * 0.80f).toInt()  // top 20 %

        // Background track
        drawRect(
            color   = accent.copy(alpha = 0.08f),
            topLeft = Offset(left, barTop),
            size    = Size(barW, barH),
        )

        // Filled segments (bottom → top, so seg 0 = bottom)
        repeat(segCount) { seg ->
            val segTop = barTop + barH - (seg + 1) * (segH + gap)
            val color  = if (seg >= warnStart) warning.copy(alpha = 0.9f) else accent.copy(alpha = 0.85f)
            if (seg < filledSegs) {
                drawRect(
                    brush   = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.95f), color.copy(alpha = 0.65f)),
                        startY = segTop, endY = segTop + segH,
                    ),
                    topLeft = Offset(left, segTop),
                    size    = Size(barW, segH),
                )
            }
        }

        // Peak hold indicator — a bright thin line at peak segment
        val peakY = barTop + barH - (peakSeg + 1) * (segH + gap) - 1f
        drawRect(
            color   = accent,
            topLeft = Offset(left, peakY),
            size    = Size(barW, 3f),
        )
    }

    drawMeter(lLeft, lAmp, peakL)
    drawMeter(rLeft, rAmp, peakR)

    // Channel labels
    drawIntoCanvas { canvas ->
        val paint = Paint().apply {
            isAntiAlias = true
            typeface    = Typeface.MONOSPACE
            textSize    = size.height * 0.075f
            textAlign   = Paint.Align.CENTER
            color       = android.graphics.Color.argb(
                (accent.alpha * 180).toInt(),
                (accent.red   * 255).toInt(),
                (accent.green * 255).toInt(),
                (accent.blue  * 255).toInt(),
            )
        }
        canvas.nativeCanvas.drawText("L", lLeft + barW / 2f, barTop + barH + paint.textSize * 1.1f, paint)
        canvas.nativeCanvas.drawText("R", rLeft + barW / 2f, barTop + barH + paint.textSize * 1.1f, paint)
    }
}
