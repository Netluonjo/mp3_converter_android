package com.sondeptrai.mp3converter.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.model.TranscriptSegment
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.data.repository.SongLyricsHelper
import com.sondeptrai.mp3converter.engine.AudioPlayerManager
import com.sondeptrai.mp3converter.ui.components.AudioControlButtons
import com.sondeptrai.mp3converter.ui.components.AudioScrubberBar
import com.sondeptrai.mp3converter.ui.components.AudioWaveformVisualizer
import com.sondeptrai.mp3converter.ui.theme.CoralRed
import kotlinx.coroutines.launch
import java.io.File

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
    val isLoadingLyrics by playerManager.isLoadingLyrics.collectAsState()

    var showMenu by remember { mutableStateOf(false) }
    var showSearchLyricsDialog by remember { mutableStateOf(false) }
    var showPasteLyricsDialog by remember { mutableStateOf(false) }
    var searchSongQuery by remember { mutableStateOf("") }
    var pasteLyricsText by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Real segments from current track (filtered to eliminate any "null" text)
    val rawSegments = currentTrack.transcriptSegments
    val segments = remember(rawSegments, currentTrack.title) {
        val filtered = rawSegments.filter { it.text.isNotBlank() && !it.text.equals("null", ignoreCase = true) }
        if (filtered.isNotEmpty()) filtered else SongLyricsHelper.getLyricsForTrack(currentTrack.title, durationMs, null)
    }

    // Calculate currently active line being sung
    val activeIndex = segments.indexOfLast { currentPositionMs >= it.timeMs }

    // Automatically scroll to keep the currently sung line centered
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && !listState.isScrollInProgress) {
            val targetScroll = (activeIndex - 2).coerceAtLeast(0)
            listState.animateScrollToItem(targetScroll)
        }
    }

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
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateToTools) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Trở lại")
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = currentTrack.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${currentTrack.format.displayName} • ${currentTrack.formattedDuration}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }

            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Tùy chọn")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Tìm lời bài hát online") },
                        onClick = {
                            showMenu = false
                            searchSongQuery = currentTrack.title
                            showSearchLyricsDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Search, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Dán lời bài hát (Lyrics / LRC)") },
                        onClick = {
                            showMenu = false
                            pasteLyricsText = segments.joinToString("\n") { it.text }
                            showPasteLyricsDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Chia sẻ file") },
                        onClick = {
                            showMenu = false
                            fileManager.shareTrack(currentTrack)
                        },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Xóa bài này", color = CoralRed) },
                        onClick = {
                            showMenu = false
                            fileManager.deleteTrack(currentTrack)
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, null, tint = CoralRed) }
                    )
                }
            }
        }

        // Compact Waveform Visualizer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FB))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                AudioWaveformVisualizer(
                    samples = currentTrack.waveformSamples,
                    progress = playerManager.progress,
                    onSeek = { ratio -> playerManager.seekToProgress(ratio) },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Lyrics Display Card (Center Stage)
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF7F8FA)),
            border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.2f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Lyrics Card Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (isPlaying) Color(0xFF34C759) else Color.Gray)
                        )
                        Text(
                            text = "LỜI BÀI HÁT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CoralRed,
                            letterSpacing = 0.5.sp
                        )
                        if (isLoadingLyrics) {
                            Spacer(modifier = Modifier.width(6.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 1.5.dp,
                                color = CoralRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Đang tải lời online...", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    // Quick Action: Search or Paste
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "🔍 Tìm lời",
                            fontSize = 11.sp,
                            color = CoralRed,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CoralRed.copy(alpha = 0.1f))
                                .clickable {
                                    searchSongQuery = currentTrack.title
                                    showSearchLyricsDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        Text(
                            text = "Dán lời",
                            fontSize = 11.sp,
                            color = Color.DarkGray,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFE5E7EB))
                                .clickable {
                                    pasteLyricsText = segments.joinToString("\n") { it.text }
                                    showPasteLyricsDialog = true
                                }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Synchronized Lyrics Scroll View
                if (segments.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = Color.LightGray,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Chưa có lời cho bài hát này",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Gray
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Bấm '🔍 Tìm lời' để tìm lời bài hát online hoặc 'Dán lời' từ Zing MP3 / Spotify.",
                                fontSize = 12.sp,
                                color = Color.DarkGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        itemsIndexed(segments) { index, segment ->
                            val isActive = index == activeIndex
                            val backgroundColor by animateColorAsState(
                                if (isActive) CoralRed.copy(alpha = 0.12f) else Color.Transparent,
                                label = "bg"
                            )
                            val textColor by animateColorAsState(
                                if (isActive) CoralRed else Color(0xFF2C2C2E).copy(alpha = 0.75f),
                                label = "text"
                            )

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(backgroundColor)
                                    .border(
                                        width = if (isActive) 1.5.dp else 0.dp,
                                        color = if (isActive) CoralRed.copy(alpha = 0.4f) else Color.Transparent,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        playerManager.seekTo(segment.timeMs)
                                        if (!isPlaying) playerManager.play()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Timestamp badge
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isActive) CoralRed else Color(0xFFE5E5EA))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = segment.formattedTime,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = if (isActive) Color.White else Color.DarkGray
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                // Real Song Lyric text
                                Text(
                                    text = segment.text,
                                    fontSize = if (isActive) 17.sp else 14.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                    color = textColor,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                if (isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "▶",
                                        color = CoralRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Scrubber Bar
        AudioScrubberBar(
            currentPositionMs = currentPositionMs,
            durationMs = durationMs,
            onSeek = { pos -> playerManager.seekTo(pos) }
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Control Buttons
        AudioControlButtons(
            isPlaying = isPlaying,
            playbackSpeed = playbackSpeed,
            isLooping = isLooping,
            onTogglePlayPause = { playerManager.togglePlayPause() },
            onSkipBackward = { playerManager.skipBackward10() },
            onSkipForward = { playerManager.skipForward10() },
            onChangeSpeed = { playerManager.cycleSpeed() },
            onToggleLoop = { playerManager.toggleLoop() }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Bottom Action Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp)
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { playerManager.seekTo(0L) }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Quay lại đầu",
                    tint = Color.Gray,
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E5EA))
                    .clickable { playerManager.togglePlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E5EA))
                    .clickable { playerManager.toggleMute() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.Close else Icons.Default.Notifications,
                    contentDescription = null,
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE5E5EA))
                    .clickable { onNavigateToTools() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Công cụ",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Search Online Lyrics Dialog
    if (showSearchLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showSearchLyricsDialog = false },
            title = {
                Text(
                    text = "Tìm lời bài hát online",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Nhập tên bài hát hoặc ca sĩ để tải lời bài hát chính xác từ kho dữ liệu:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = searchSongQuery,
                        onValueChange = { searchSongQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ví dụ: Nơi này có anh, Shape of You...") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (searchSongQuery.isNotBlank()) {
                            playerManager.searchLyricsByTitle(searchSongQuery)
                        }
                        showSearchLyricsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Tìm & Cập nhật lời")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSearchLyricsDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }

    // Paste / Edit Lyrics Dialog
    if (showPasteLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showPasteLyricsDialog = false },
            title = {
                Text(
                    text = "Dán lời bài hát",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Dán lời bài hát dạng chữ thường hoặc định dạng LRC [00:15.20] từ Zing MP3 / Spotify. Ứng dụng sẽ tự động chia nhịp chính xác:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = pasteLyricsText,
                        onValueChange = { pasteLyricsText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp),
                        placeholder = { Text("Dán lời bài hát vào đây...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updated = fileManager.saveCustomLyrics(currentTrack, pasteLyricsText)
                        if (updated.isNotEmpty()) {
                            playerManager.updateCurrentTrackTranscript(updated)
                        }
                        showPasteLyricsDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Lưu lời bài hát")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteLyricsDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
