package com.sondeptrai.mp3converter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sondeptrai.mp3converter.ui.theme.PlayheadRed
import com.sondeptrai.mp3converter.ui.theme.WaveformPlayed
import com.sondeptrai.mp3converter.ui.theme.WaveformUnplayed

@Composable
fun AudioWaveformVisualizer(
    samples: List<Float>,
    progress: Float, // 0.0f .. 1.0f
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var dragOffset by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val barWidthPx = 10f
    val barSpacingPx = 8f
    val maxBarHeightPx = 360f
    val minBarHeightPx = 16f

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                        dragOffset = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragOffset += dragAmount.x
                    },
                    onDragEnd = {
                        val totalWidth = size.width.toFloat()
                        val step = barWidthPx + barSpacingPx
                        val contentWidth = samples.size * step
                        val currentX = progress * contentWidth
                        val newX = (currentX - dragOffset).coerceIn(0f, contentWidth)
                        val finalProgress = (newX / contentWidth).coerceIn(0f, 1f)
                        dragOffset = 0f
                        isDragging = false
                        onSeek(finalProgress)
                    },
                    onDragCancel = {
                        dragOffset = 0f
                        isDragging = false
                    }
                )
            }
    ) {
        val totalWidth = size.width
        val totalHeight = size.height
        val centerX = totalWidth / 2f
        val centerY = totalHeight / 2f

        val step = barWidthPx + barSpacingPx
        val contentWidth = samples.size * step
        val currentX = progress * contentWidth
        val scrollOffset = centerX - currentX + dragOffset

        // Draw waveform bars
        samples.forEachIndexed { index, rawAmp ->
            val sampleX = scrollOffset + (index * step)
            if (sampleX in -50f..(totalWidth + 50f)) {
                val sampleProgress = index.toFloat() / (samples.size - 1).coerceAtLeast(1).toFloat()
                val isPlayed = sampleX <= centerX

                val barHeight = (rawAmp * maxBarHeightPx).coerceIn(minBarHeightPx, maxBarHeightPx)
                val topY = centerY - (barHeight / 2f)

                drawRoundRect(
                    color = if (isPlayed) WaveformPlayed else WaveformUnplayed,
                    topLeft = Offset(sampleX, topY),
                    size = Size(barWidthPx, barHeight),
                    cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f)
                )
            }
        }

        // Draw center vertical red needle with top and bottom circular pins
        val needleHeight = maxBarHeightPx + 60f
        val topPinY = centerY - (needleHeight / 2f)
        val bottomPinY = centerY + (needleHeight / 2f)

        // Top dot pin
        drawCircle(
            color = PlayheadRed,
            radius = 10f,
            center = Offset(centerX, topPinY)
        )

        // Vertical line
        drawLine(
            color = PlayheadRed,
            start = Offset(centerX, topPinY),
            end = Offset(centerX, bottomPinY),
            strokeWidth = 4f
        )

        // Bottom dot pin
        drawCircle(
            color = PlayheadRed,
            radius = 10f,
            center = Offset(centerX, bottomPinY)
        )
    }
}
