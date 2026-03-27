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
    val wavePhase  = remember { mutableFloatStateOf(0f) }

    // VU meter peak-hold state (L = bands 0-9, R = bands 10-19)
    var vuPeakL   by remember { mutableFloatStateOf(0f) }
    var vuPeakR   by remember { mutableFloatStateOf(0f) }
    var vuCountL  by remember { mutableIntStateOf(0) }
    var vuCountR  by remember { mutableIntStateOf(0) }

    LaunchedEffect(fftData) {
        wavePhase.value += 0.06f

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
            VisualizerMode.BARS         -> drawBars(fftData, accentColor, secondaryColor)
            VisualizerMode.BRICKS       -> drawBricks(fftData, accentColor)
            VisualizerMode.COLUMNS      -> drawColumns(fftData, accentColor)
            VisualizerMode.RETRO        -> drawRetro(fftData, accentColor)
            VisualizerMode.WAVE         -> drawWave(fftData, accentColor, secondaryColor, wavePhase.value)
            VisualizerMode.FLAME        -> drawFlame(fftData, accentColor, secondaryColor)
            VisualizerMode.SCATTER      -> drawScatter(fftData, accentColor)
            VisualizerMode.RADIAL       -> drawRadial(fftData, accentColor, secondaryColor)
            VisualizerMode.OSCILLOSCOPE -> drawOscilloscope(waveformData, accentColor)
            VisualizerMode.VU_METERS    -> drawVuMeters(fftData, accentColor, secondaryColor, vuPeakL, vuPeakR)
            VisualizerMode.NONE         -> { /* nothing */ }
        }
    }
}

// ─── BARS (gradient + bass emphasis) ─────────────────────────
//
// First 5 bands are progressively wider and get a height boost
// reflecting bass energy. All bars use a vertical gradient from
// accent (top) to a dimmer secondary (bottom).
private fun DrawScope.drawBars(fft: FloatArray, accent: Color, secondary: Color) {
    val n     = fft.size
    val gap   = size.width * 0.012f

    // Bass bands 0-4 get up to 1.4× width; weight the total to keep layout flush
    val bassW = 1.4f
    val totalWeight = n + 5 * (bassW - 1f)          // 20 + 5*0.4 = 22
    val baseW = (size.width - gap * (n - 1)) / totalWeight

    var x = 0f
    fft.forEachIndexed { i, amp ->
        val bassIdx    = (4 - i).coerceAtLeast(0)   // 4,3,2,1,0 then 0
        val isBass     = i < 5
        val wMult      = if (isBass) bassW else 1f
        val hMult      = if (isBass) 1f + bassIdx * 0.07f else 1f
        val barW       = baseW * wMult
        val barH       = (amp * hMult).coerceAtMost(1f) * size.height * 0.92f
        val top        = size.height - barH

        if (barH > 0.5f) {
            drawRect(
                brush   = Brush.verticalGradient(
                    colors = listOf(accent, secondary.copy(alpha = 0.55f)),
                    startY = top,
                    endY   = size.height,
                ),
                topLeft = Offset(x, top),
                size    = Size(barW, barH),
            )
        }
        x += barW + gap
    }
}

// ─── BRICKS ──────────────────────────────────────────────────
private fun DrawScope.drawBricks(fft: FloatArray, color: Color) {
    val n        = fft.size
    val gap      = size.width * 0.015f
    val barW     = (size.width - gap * (n - 1)) / n
    val brickH   = size.height / 10f
    val brickGap = 1.5f
    fft.forEachIndexed { i, amp ->
        val filledCount = (amp * 9).toInt().coerceIn(0, 9) + 1
        val left = i * (barW + gap)
        repeat(filledCount) { b ->
            val alpha = 0.3f + (b.toFloat() / 9f) * 0.7f
            val top   = size.height - (b + 1) * brickH + brickGap
            drawRect(
                color   = color.copy(alpha = alpha),
                topLeft = Offset(left, top),
                size    = Size(barW, brickH - brickGap),
            )
        }
    }
}

// ─── COLUMNS ─────────────────────────────────────────────────
private fun DrawScope.drawColumns(fft: FloatArray, color: Color) {
    val n    = fft.size
    val colW = size.width / n
    fft.forEachIndexed { i, amp ->
        val h     = amp * size.height * 0.92f
        val alpha = 0.5f + amp * 0.5f
        drawRect(
            color   = color.copy(alpha = alpha),
            topLeft = Offset(i * colW, size.height - h),
            size    = Size(colW - 1f, h),
        )
    }
}

// ─── RETRO ───────────────────────────────────────────────────
private fun DrawScope.drawRetro(fft: FloatArray, color: Color) {
    val blocks = listOf("▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")
    val n      = fft.size
    val colW   = size.width / n
    val charH  = size.height / 8f
    val paint  = Paint().apply {
        isAntiAlias = true
        typeface    = Typeface.MONOSPACE
        textSize    = charH * 1.05f
        textAlign   = Paint.Align.CENTER
    }
    drawIntoCanvas { canvas ->
        fft.forEachIndexed { i, amp ->
            val numChars = (amp * 8).toInt().coerceIn(1, 8)
            val cx       = i * colW + colW / 2f
            repeat(numChars) { c ->
                val charIndex = if (c == numChars - 1) min(7, (amp * 8 % 1 * 7).toInt()) else 7
                val alpha     = (0.35f + (c.toFloat() / numChars) * 0.65f).coerceIn(0f, 1f)
                val y         = size.height - c * charH
                paint.color   = android.graphics.Color.argb(
                    (alpha * 255).toInt(),
                    (color.red   * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue  * 255).toInt(),
                )
                canvas.nativeCanvas.drawText(blocks[charIndex], cx, y, paint)
            }
        }
    }
}

// ─── WAVE ────────────────────────────────────────────────────
private fun DrawScope.drawWave(fft: FloatArray, color: Color, secondary: Color, phase: Float) {
    val W   = size.width
    val H   = size.height
    val pts = 120

    val path1 = Path()
    for (x in 0..pts) {
        val normX  = x.toFloat() / pts
        val ampIdx = (normX * (fft.size - 1)).toInt().coerceIn(0, fft.size - 1)
        val amp    = fft[ampIdx]
        val y      = H / 2f + sin(normX * PI.toFloat() * 4f + phase) * amp * (H / 2f - 4f)
        if (x == 0) path1.moveTo(normX * W, y) else path1.lineTo(normX * W, y)
    }
    drawPath(path1, color = color, style = Stroke(width = 2f))

    val path2 = Path()
    for (x in 0..pts) {
        val normX  = x.toFloat() / pts
        val ampIdx = (normX * (fft.size - 1)).toInt().coerceIn(0, fft.size - 1)
        val amp    = fft[ampIdx]
        val y      = H / 2f + sin(normX * PI.toFloat() * 6f + phase * 1.3f) * amp * (H / 2f - 10f)
        if (x == 0) path2.moveTo(normX * W, y) else path2.lineTo(normX * W, y)
    }
    drawPath(path2, color = secondary.copy(alpha = 0.45f), style = Stroke(width = 1.2f))
}

// ─── FLAME ───────────────────────────────────────────────────
private fun DrawScope.drawFlame(fft: FloatArray, accent: Color, secondary: Color) {
    val n    = fft.size
    val gap  = size.width * 0.015f
    val barW = (size.width - gap * (n - 1)) / n
    fft.forEachIndexed { i, amp ->
        val h    = amp * size.height * 0.92f
        val left = i * (barW + gap)
        val top  = size.height - h
        drawRect(color = secondary.copy(alpha = 0.85f), topLeft = Offset(left, top),         size = Size(barW, h))
        drawRect(color = accent.copy(alpha = 0.70f),    topLeft = Offset(left, top + h*0.3f), size = Size(barW, h*0.7f))
        drawRect(color = accent.copy(alpha = 0.95f),    topLeft = Offset(left, top + h*0.65f),size = Size(barW, h*0.35f))
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
