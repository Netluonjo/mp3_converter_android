package com.sondeptrai.mp3converter.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.ui.theme.CoralRed

@Composable
fun AudioScrubberBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val ratio = (offset.x / size.width).coerceIn(0f, 1f)
                        onSeek((ratio * durationMs).toLong())
                    }
                }
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val ratio = (change.position.x / size.width).coerceIn(0f, 1f)
                        onSeek((ratio * durationMs).toLong())
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val thumbX = progress * width

            // Background track
            drawLine(
                color = Color(0xFFE5E5EA),
                start = Offset(0f, centerY),
                end = Offset(width, centerY),
                strokeWidth = 6f
            )

            // Active track
            drawLine(
                color = CoralRed,
                start = Offset(0f, centerY),
                end = Offset(thumbX, centerY),
                strokeWidth = 6f
            )

            // Thumb
            drawCircle(
                color = CoralRed,
                radius = 16f,
                center = Offset(thumbX, centerY)
            )
            drawCircle(
                color = Color.White,
                radius = 8f,
                center = Offset(thumbX, centerY)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formatTime(currentPositionMs),
                fontSize = 13.sp,
                color = Color.Gray
            )
            Text(
                text = formatTime(durationMs),
                fontSize = 13.sp,
                color = Color.Gray
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%02d:%02d".format(m, s)
}
