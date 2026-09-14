package com.sondeptrai.mp3converter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioPlayerManager
import com.sondeptrai.mp3converter.ui.components.AudioControlButtons
import com.sondeptrai.mp3converter.ui.components.AudioScrubberBar
import com.sondeptrai.mp3converter.ui.components.AudioWaveformVisualizer
import com.sondeptrai.mp3converter.ui.theme.CoralRed
import com.sondeptrai.mp3converter.ui.theme.PillSelectedDark

@Composable
fun AudioPlayerDetailScreen(
    playerManager: AudioPlayerManager,
    fileManager: AudioFileManager,
    onNavigateToTools: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val currentPositionMs by playerManager.currentPositionMs.collectAsState()
    val durationMs by playerManager.durationMs.collectAsState()
    val playbackSpeed by playerManager.playbackSpeed.collectAsState()
    val isLooping by playerManager.isLooping.collectAsState()
    val isMuted by playerManager.isMuted.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Âm thanh, 1 = Văn bản
    var showMenu by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToTools) {
                Icon(Icons.Default.ArrowBackIosNew, contentDescription = "Trở lại")
            }

            Text(
                text = currentTrack.title,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold
            )

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreHoriz, contentDescription = "Tùy chọn")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Chia sẻ file") },
                        onClick = {
                            showMenu = false
                            fileManager.shareTrack(currentTrack)
                        },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa bản ghi", color = CoralRed) },
                        onClick = {
                            showMenu = false
                            fileManager.deleteTrack(currentTrack)
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = CoralRed) }
                    )
                }
            }
        }

        // Mode Pill Switcher: [ Âm thanh ] [ Văn bản ]
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF2F2F7))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: Âm thanh
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedTab == 0) PillSelectedDark else Color.Transparent)
                    .clickable { selectedTab = 0 }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.GraphicEq,
                    contentDescription = null,
                    tint = if (selectedTab == 0) Color.White else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Âm thanh",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == 0) Color.White else Color.Gray
                )
            }

            // Tab 1: Văn bản
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedTab == 1) PillSelectedDark else Color.Transparent)
                    .clickable { selectedTab = 1 }
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = null,
                    tint = if (selectedTab == 1) Color.White else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Văn bản",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == 1) Color.White else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Center Content: Waveform or Transcript
        if (selectedTab == 0) {
            AudioWaveformVisualizer(
                samples = currentTrack.waveformSamples,
                progress = playerManager.progress,
                onSeek = { ratio -> playerManager.seekToProgress(ratio) },
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Bản ghi lời thoại tự động (AI Transcript):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = CoralRed
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentTrack.transcript ?: "Chưa có lời thoại văn bản cho tệp âm thanh này.",
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Scrubber Bar
        AudioScrubberBar(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onSeek = { pos -> playerManager.seekTo(pos) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Control Buttons
        AudioControlButtons(
            isPlaying = isPlaying,
            playbackSpeed = playbackSpeed,
            isLooping = isLooping,
            onTogglePlayPause = { playerManager.togglePlayPause() },
            onSkipBackward = { playerManager.skipBackward10() },
            onSkipForward = { playerManager.skipForward10() },
            onCycleSpeed = { playerManager.cycleSpeed() },
            onToggleLoop = { playerManager.toggleLoop() }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Reset button
            IconButton(onClick = { playerManager.seekTo(0L) }) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Quay lại đầu",
                    tint = Color.Gray,
                    modifier = Modifier.size(28.dp)
                )
            }

            // Secondary Play button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E5EA))
                    .clickable { playerManager.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Mute button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E5EA))
                    .clickable { playerManager.toggleMute() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Expand to tools button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E5EA))
                    .clickable { onNavigateToTools() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Mở rộng",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
