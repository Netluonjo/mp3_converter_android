package com.sondeptrai.mp3converter.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.ui.theme.CoralRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioScrubberBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalDuration = durationMs.coerceAtLeast(1L)
    var isUserDragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(0f) }

    val actualProgress = (currentPositionMs.toFloat() / totalDuration).coerceIn(0f, 1f)
    val displayProgress = if (isUserDragging) dragProgress else actualProgress
    val displayPositionMs = if (isUserDragging) (dragProgress * totalDuration).toLong() else currentPositionMs

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Slider(
            value = displayProgress,
            onValueChange = { newValue ->
                isUserDragging = true
                dragProgress = newValue
            },
            onValueChangeFinished = {
                val targetMs = (dragProgress * totalDuration).toLong()
                onSeek(targetMs)
                isUserDragging = false
            },
            colors = SliderDefaults.colors(
                thumbColor = CoralRed,
                activeTrackColor = CoralRed,
                inactiveTrackColor = Color(0xFFE5E5EA)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = formatTime(displayPositionMs),
                fontSize = 13.sp,
                color = if (isUserDragging) CoralRed else Color.Gray,
                fontWeight = if (isUserDragging) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
            )
            Text(
                text = formatTime(totalDuration),
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