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

@Composable
fun VisualizerCanvas(
    fftData: FloatArray,
    mode: VisualizerMode,
    accentColor: Color,
    secondaryColor: Color,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    onSwipeNext: () -> Unit = {},
    onSwipePrev: () -> Unit = {},
) {
    var swipeAccum by remember { mutableFloatStateOf(0f) }
    val wavePhase = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(fftData) {
        wavePhase.value += 0.06f
    }

    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragEnd = { swipeAccum = 0f },
                    onHorizontalDrag = { _, delta ->
                        swipeAccum += delta
                        if (swipeAccum > 60f)  { onSwipeNext(); swipeAccum = 0f }
                        if (swipeAccum < -60f) { onSwipePrev(); swipeAccum = 0f }
                    }
                )
            }
    ) {
        drawRect(color = backgroundColor)
        when (mode) {
            VisualizerMode.BARS    -> drawBars(fftData, accentColor)
            VisualizerMode.BRICKS  -> drawBricks(fftData, accentColor)
            VisualizerMode.COLUMNS -> drawColumns(fftData, accentColor)
            VisualizerMode.RETRO   -> drawRetro(fftData, accentColor)
            VisualizerMode.WAVE    -> drawWave(fftData, accentColor, secondaryColor, wavePhase.value)
            VisualizerMode.FLAME   -> drawFlame(fftData, accentColor, secondaryColor)
            VisualizerMode.SCATTER -> drawScatter(fftData, accentColor)
            VisualizerMode.NONE    -> { /* nothing */ }
        }
    }
}

private fun DrawScope.drawBars(fft: FloatArray, color: Color) {
    val n     = fft.size
    val gap   = size.width * 0.015f
    val barW  = (size.width - gap * (n - 1)) / n
    fft.forEachIndexed { i, amp ->
        val barH  = amp * size.height * 0.92f
        val left  = i * (barW + gap)
        val top   = size.height - barH
        drawRect(color = color, topLeft = Offset(left, top), size = Size(barW, barH))
    }
}

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
            val alpha  = 0.3f + (b.toFloat() / 9f) * 0.7f
            val top    = size.height - (b + 1) * brickH + brickGap
            drawRect(
                color     = color.copy(alpha = alpha),
                topLeft   = Offset(left, top),
                size      = Size(barW, brickH - brickGap),
            )
        }
    }
}

private fun DrawScope.drawColumns(fft: FloatArray, color: Color) {
    val n    = fft.size
    val colW = size.width / n
    fft.forEachIndexed { i, amp ->
        val h     = amp * size.height * 0.92f
        val alpha = 0.5f + amp * 0.5f
        drawRect(
            color     = color.copy(alpha = alpha),
            topLeft   = Offset(i * colW, size.height - h),
            size      = Size(colW - 1f, h),
        )
    }
}

private fun DrawScope.drawRetro(fft: FloatArray, color: Color) {
    val blocks   = listOf("▁", "▂", "▃", "▄", "▅", "▆", "▇", "█")
    val n        = fft.size
    val colW     = size.width / n
    val charH    = size.height / 8f
    val paint    = Paint().apply {
        isAntiAlias = true
        typeface    = Typeface.DEFAULT
        textSize    = charH * 1.05f
        textAlign   = Paint.Align.CENTER
    }
    drawIntoCanvas { canvas ->
        fft.forEachIndexed { i, amp ->
            val numChars  = (amp * 8).toInt().coerceIn(1, 8)
            val cx        = i * colW + colW / 2f
            repeat(numChars) { c ->
                val charIndex = if (c == numChars - 1) min(7, (amp * 8 % 1 * 7).toInt()) else 7
                val alpha     = (0.35f + (c.toFloat() / numChars) * 0.65f).coerceIn(0f, 1f)
                val y         = size.height - c * charH
                paint.color   = android.graphics.Color.argb(
                    (alpha * 255).toInt(),
                    (color.red * 255).toInt(),
                    (color.green * 255).toInt(),
                    (color.blue * 255).toInt(),
                )
                canvas.nativeCanvas.drawText(blocks[charIndex], cx, y, paint)
            }
        }
    }
}

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

private fun DrawScope.drawFlame(fft: FloatArray, accent: Color, secondary: Color) {
    val n    = fft.size
    val gap  = size.width * 0.015f
    val barW = (size.width - gap * (n - 1)) / n
    fft.forEachIndexed { i, amp ->
        val h    = amp * size.height * 0.92f
        val left = i * (barW + gap)
        val top  = size.height - h
        drawRect(color = secondary.copy(alpha = 0.85f), topLeft = Offset(left, top), size = Size(barW, h))
        drawRect(color = accent.copy(alpha = 0.7f), topLeft = Offset(left, top + h * 0.3f), size = Size(barW, h * 0.7f))
        drawRect(color = accent.copy(alpha = 0.95f), topLeft = Offset(left, top + h * 0.65f), size = Size(barW, h * 0.35f))
    }
}

private fun DrawScope.drawScatter(fft: FloatArray, color: Color) {
    val total = 60
    repeat(total) { i ->
        val normI = i.toFloat() / total
        val band  = (normI * (fft.size - 1)).toInt().coerceIn(0, fft.size - 1)
        val amp   = fft[band]
        val x     = ((i * 137.508f) % size.width)
        val y     = size.height - ((i * 97.3f + amp * size.height * 0.8f) % size.height)
        val r     = (2f + amp * 5f)
        val alpha = (0.3f + amp * 0.7f).coerceIn(0f, 1f)
        drawCircle(color = color.copy(alpha = alpha), radius = r, center = Offset(x, y))
    }
}
