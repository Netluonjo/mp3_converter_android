package com.sondeptrai.mp3converter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.sondeptrai.mp3converter.ui.theme.CoralRed

@Composable
fun WaveformTrimmerOverlay(
    startRatio: Float,
    endRatio: Float,
    durationMs: Long,
    onTrimChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val touchRatio = (change.position.x / size.width).coerceIn(0f, 1f)
                    val distStart = kotlin.math.abs(touchRatio - startRatio)
                    val distEnd = kotlin.math.abs(touchRatio - endRatio)
                    if (distStart < distEnd) {
                        val newStart = touchRatio.coerceAtMost(endRatio - 0.05f)
                        onTrimChange(newStart, endRatio)
                    } else {
                        val newEnd = touchRatio.coerceAtLeast(startRatio + 0.05f)
                        onTrimChange(startRatio, newEnd)
                    }
                }
            }
    ) {
        val width = size.width
        val height = size.height

        val startX = startRatio * width
        val endX = endRatio * width

        // Dimmed area before start
        drawRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(0f, 0f),
            size = Size(startX, height)
        )

        // Selected active window
        drawRect(
            color = CoralRed.copy(alpha = 0.08f),
            topLeft = Offset(startX, 0f),
            size = Size(endX - startX, height)
        )
        drawRect(
            color = CoralRed,
            topLeft = Offset(startX, 0f),
            size = Size(endX - startX, height),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
        )

        // Dimmed area after end
        drawRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(endX, 0f),
            size = Size(width - endX, height)
        )

        // Left Handle
        drawRoundRect(
            color = CoralRed,
            topLeft = Offset(startX - 12f, 20f),
            size = Size(24f, height - 40f),
            cornerRadius = CornerRadius(8f, 8f)
        )

        // Right Handle
        drawRoundRect(
            color = CoralRed,
            topLeft = Offset(endX - 12f, 20f),
            size = Size(24f, height - 40f),
            cornerRadius = CornerRadius(8f, 8f)
        )
    }
}
