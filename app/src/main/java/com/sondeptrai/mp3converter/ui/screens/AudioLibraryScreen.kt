package com.sondeptrai.mp3converter.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.model.AudioTrack
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioPlayerManager
import com.sondeptrai.mp3converter.ui.theme.CoralRed
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioLibraryScreen(
    fileManager: AudioFileManager,
    playerManager: AudioPlayerManager,
    onSelectTrackToPlay: (AudioTrack) -> Unit,
    onNavigateToWifiTransfer: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val savedTracks by fileManager.savedTracks.collectAsState()
    val currentTrack by playerManager.currentTrack.collectAsState()
    val isPlaying by playerManager.isPlaying.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isImporting by remember { mutableStateOf(false) }

    // Always refresh library when entering this screen
    LaunchedEffect(Unit) {
        fileManager.reloadLibrary()
    }

    // Audio file picker to import music from phone
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isImporting = true
            coroutineScope.launch {
                val newTrack = fileManager.importAudioFromUri(uri)
                if (newTrack != null) {
                    playerManager.loadTrack(newTrack)
                    onSelectTrackToPlay(newTrack)
                }
                isImporting = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Thư viện bài hát", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Text("Tổng cộng ${savedTracks.size} bài ghi âm & nhạc", fontSize = 12.sp, color = Color.Gray)
                    }
                },
                actions = {
                    // Import Audio from device
                    FilledTonalButton(
                        onClick = { audioPicker.launch("audio/*") },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = CoralRed.copy(alpha = 0.12f),
                            contentColor = CoralRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thêm nhạc từ máy", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    // Refresh button
                    IconButton(onClick = {
                        coroutineScope.launch { fileManager.reloadLibrary() }
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = CoralRed)
                    }
                }
            )
        }
    ) { padding ->
        if (isImporting) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = CoralRed)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Đang nạp bài hát từ điện thoại...", color = Color.Gray, fontSize = 14.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DANH SÁCH BÀI ĐÃ LƯU (${savedTracks.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray
                        )
                        Text(
                            text = "Chạm để phát ngay",
                            fontSize = 11.sp,
                            color = CoralRed
                        )
                    }
                }

                if (savedTracks.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FA))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.Info,
                                    contentDescription = null,
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(56.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Thư viện chưa có bài hát nào",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Bạn có thể thêm nhạc từ bộ nhớ điện thoại hoặc xuất file từ các công cụ (Cắt nhạc, Video sang MP3).",
                                    color = Color.Gray,
                                    fontSize = 13.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { audioPicker.launch("audio/*") },
                                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Chọn bài hát từ máy ngay")
                                }
                            }
                        }
                    }
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
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCurrent) CoralRed.copy(alpha = 0.08f) else Color(0xFFF2F2F7)
                        ),
                        border = if (isCurrent) androidx.compose.foundation.BorderStroke(1.5.dp, CoralRed) else null
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(14.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Play/Pause circular button
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
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
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Track Metadata
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = track.title,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    color = if (isCurrent) CoralRed else Color.Black,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Format badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(CoralRed.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = track.format.displayName,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CoralRed
                                        )
                                    }

                                    Text(
                                        text = "• ${track.formattedDuration} • ${track.formattedFileSize}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                }

                                // Lyrics availability indicator
                                if (track.transcriptSegments.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "🎵 Có lời bài hát (${track.transcriptSegments.size} câu)",
                                        fontSize = 11.sp,
                                        color = Color(0xFF34C759),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // Actions: Share & Delete
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { fileManager.shareTrack(track) }) {
                                    Icon(Icons.Default.Share, contentDescription = "Chia sẻ", tint = Color.Gray, modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { fileManager.deleteTrack(track) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.LightGray, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
