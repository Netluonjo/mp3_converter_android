package com.sondeptrai.mp3converter.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.ui.theme.CoralRed

@Composable
fun AudioControlButtons(
    isPlaying: Boolean,
    playbackSpeed: Float,
    isLooping: Boolean,
    onTogglePlayPause: () -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onCycleSpeed: () -> Unit,
    onToggleLoop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speed pill
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { onCycleSpeed() },
            contentAlignment = Alignment.Center
        ) {
            val speedLabel = if (playbackSpeed == playbackSpeed.toInt().toFloat()) {
                "x${playbackSpeed.toInt()}"
            } else {
                "x$playbackSpeed"
            }
            Text(
                text = speedLabel,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Rewind 10s
        IconButton(onClick = onSkipBackward) {
            Icon(
                imageVector = Icons.Default.Replay10,
                contentDescription = "Tua lùi 10s",
                modifier = Modifier.size(30.dp)
            )
        }

        // Primary Play/Pause Red Button
        Box(
            modifier = Modifier
                .size(68.dp)
                .shadow(12.dp, CircleShape, spotColor = CoralRed)
                .clip(CircleShape)
                .background(CoralRed)
                .clickable { onTogglePlayPause() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )
        }

        // Forward 10s
        IconButton(onClick = onSkipForward) {
            Icon(
                imageVector = Icons.Default.Forward10,
                contentDescription = "Tua tiến 10s",
                modifier = Modifier.size(30.dp)
            )
        }

        // Loop button
        IconButton(onClick = onToggleLoop) {
            Icon(
                imageVector = Icons.Default.Repeat,
                contentDescription = "Lặp đoạn",
                tint = if (isLooping) CoralRed else Color.Black,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
