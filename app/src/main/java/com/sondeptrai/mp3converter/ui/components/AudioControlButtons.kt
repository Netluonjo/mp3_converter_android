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
    onChangeSpeed: () -> Unit,
    onToggleLoop: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Speed Button (1x, 1.25x, 1.5x, 2x)
        IconButton(
            onClick = onChangeSpeed,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Tua lai 10s",
                    modifier = Modifier.size(20.dp)
                )
                Text(text = "10", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
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
            if (isPlaying) {
                Text(
                    text = "❚❚",
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Phát",
                    tint = Color.White,
                    modifier = Modifier.size(38.dp)
                )
            }
        }

        // Forward 10s
        IconButton(onClick = onSkipForward) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "10", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = "Tua tien 10s",
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Loop button
        IconButton(onClick = onToggleLoop) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Lap doan",
                tint = if (isLooping) CoralRed else Color.Black,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
