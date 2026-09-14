package com.sondeptrai.mp3converter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.sondeptrai.mp3converter.data.model.AudioTrack
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioPlayerManager
import com.sondeptrai.mp3converter.ui.theme.CoralRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryScreen(
    fileManager: AudioFileManager,
    playerManager: AudioPlayerManager,
    onSelectTrackToPlay: (AudioTrack) -> Unit,
    modifier: Modifier = Modifier
) {
    val savedTracks by fileManager.savedTracks.collectAsState()
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tệp của tôi (Library)", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "TỆP ÂM THANH ĐÃ LƯU (${savedTracks.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(savedTracks) { track ->
                val isCurrent = currentTrack.id == track.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            playerManager.loadTrack(track)
                            onSelectTrackToPlay(track)
                        },
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Play button
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (isCurrent && isPlaying) CoralRed else Color.White)
                                .clickable {
                                    if (isCurrent) {
                                        playerManager.togglePlayPause()
                                    } else {
                                        playerManager.loadTrack(track)
                                        playerManager.play()
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isCurrent && isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isCurrent && isPlaying) Color.White else CoralRed,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Track Details
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isCurrent) CoralRed else Color.Black,
                                fontSize = 16.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = track.format.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Gray
                                )
                                Text("•", fontSize = 11.sp, color = Color.Gray)
                                Text(track.formattedDuration, fontSize = 12.sp, color = Color.Gray)
                                Text("•", fontSize = 11.sp, color = Color.Gray)
                                Text(track.formattedFileSize, fontSize = 12.sp, color = Color.Gray)
                            }
                        }

                        // Share
                        IconButton(onClick = { fileManager.shareTrack(track) }) {
                            Icon(Icons.Default.Share, contentDescription = "Chia sẻ", tint = Color.Gray)
                        }
                    }
                }
            }
        }
    }
}


