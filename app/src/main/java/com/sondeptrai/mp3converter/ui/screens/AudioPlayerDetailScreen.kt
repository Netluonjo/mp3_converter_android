package com.sondeptrai.mp3converter.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import com.sondeptrai.mp3converter.ui.theme.PillSelectedDark
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

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sóng âm & Lời trực tiếp, 1 = Toàn bộ lời bài hát
    var showMenu by remember { mutableStateOf(false) }
    var showPasteLyricsDialog by remember { mutableStateOf(false) }
    var pasteLyricsText by remember { mutableStateOf("") }
    var isTranscribing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Get current track segments or extract authentic song lyrics
    val segments = if (currentTrack.transcriptSegments.isNotEmpty()) {
        currentTrack.transcriptSegments
    } else {
        remember(currentTrack.title, durationMs) {
            val f = if (currentTrack.filePath.isNotEmpty()) File(currentTrack.filePath) else null
            SongLyricsHelper.getLyricsForTrack(currentTrack.title, durationMs, f)
        }
    }

    val activeIndex = segments.indexOfLast { currentPositionMs >= it.timeMs }
    val currentSegment = if (activeIndex >= 0 && activeIndex < segments.size) segments[activeIndex] else segments.firstOrNull()
    val nextSegment = if (activeIndex + 1 in segments.indices) segments[activeIndex + 1] else null

    // Auto-scroll transcript list to active line
    LaunchedEffect(activeIndex, selectedTab) {
        if (activeIndex >= 0 && selectedTab == 1 && !listState.isScrollInProgress) {
            listState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
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
                        text = { Text("Dán / Sửa lời bài hát") },
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

        // Mode Pill Switcher: [ Âm thanh & Lời ] [ Toàn bộ lời bài hát ]
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF2F2F7))
                .padding(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Tab 0: Âm thanh & Lời trực tiếp
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
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (selectedTab == 0) Color.White else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Âm thanh & Lời",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == 0) Color.White else Color.Gray
                )
            }

            // Tab 1: Toàn bộ Lời bài hát
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
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (selectedTab == 1) Color.White else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "Lời bài hát (Karaoke)",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == 1) Color.White else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Center Content Box
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            if (selectedTab == 0) {
                // Dual View: Waveform Visualizer on Top + Prominent Live Synced Lyrics Below
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // 1. Audio Waveform Box
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.52f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(10.dp),
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

                    // 2. Prominent Live Synced Lyrics Card (Always visible!)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(0.48f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Header of Live Lyric Card
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
                                        text = "LỜI BÀI HÁT ĐỒNG BỘ",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = CoralRed,
                                        letterSpacing = 0.5.sp
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Quick paste lyrics button
                                    Text(
                                        text = "Dán lời",
                                        fontSize = 11.sp,
                                        color = CoralRed,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.clickable {
                                            pasteLyricsText = segments.joinToString("\n") { it.text }
                                            showPasteLyricsDialog = true
                                        }
                                    )

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.clickable { selectedTab = 1 }
                                    ) {
                                        Text(
                                            text = "Mở rộng",
                                            fontSize = 11.sp,
                                            color = Color.Gray,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = null,
                                            tint = Color.Gray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }

                            // Active Lyric Line (Highlighted in Real Time)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedTab = 1 },
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                currentSegment?.let { seg ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(CoralRed)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = seg.formattedTime,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace,
                                                color = Color.White
                                            )
                                        }

                                        Text(
                                            text = seg.text,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF1C1C1E),
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            lineHeight = 22.sp
                                        )
                                    }
                                }

                                // Next upcoming line preview
                                nextSegment?.let { next ->
                                    Text(
                                        text = "Tiếp theo: ${next.text}",
                                        fontSize = 13.sp,
                                        color = Color.Gray,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                    )
                                }
                            }

                            // Bottom Hint
                            Text(
                                text = "💡 Lời bài hát khớp theo nhạc • Chạm để mở Karaoke toàn màn hình",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            } else {
                // Tab 1: Full-Screen Karaoke Transcript Card
                Card(
                    modifier = Modifier.fillMaxSize(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8FA))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                    ) {
                        // Header Bar inside Transcript Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LỜI BÀI HÁT KARAOKE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                                if (isPlaying) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• Đang hát",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF34C759)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // Paste / Edit Lyrics Button
                                OutlinedButton(
                                    onClick = {
                                        pasteLyricsText = segments.joinToString("\n") { it.text }
                                        showPasteLyricsDialog = true
                                    },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dán lời bài hát", fontSize = 11.sp)
                                }

                                // AI Transcribe Button
                                OutlinedButton(
                                    onClick = {
                                        isTranscribing = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(500)
                                            val f = if (currentTrack.filePath.isNotEmpty()) File(currentTrack.filePath) else null
                                            val generated = SongLyricsHelper.getLyricsForTrack(currentTrack.title, durationMs, f)
                                            playerManager.updateCurrentTrackTranscript(generated)
                                            isTranscribing = false
                                        }
                                    },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isTranscribing) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = CoralRed)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("...", fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Nhận diện", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Synchronized Lyrics / Transcript List
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            itemsIndexed(segments) { index, segment ->
                                val isActive = index == activeIndex
                                val backgroundColor by animateColorAsState(
                                    if (isActive) CoralRed.copy(alpha = 0.12f) else Color.White,
                                    label = "bg"
                                )
                                val textColor by animateColorAsState(
                                    if (isActive) CoralRed else Color(0xFF2C2C2E),
                                    label = "text"
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(backgroundColor)
                                        .border(
                                            width = if (isActive) 1.5.dp else 0.dp,
                                            color = if (isActive) CoralRed else Color.Transparent,
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            playerManager.seekTo(segment.timeMs)
                                            if (!isPlaying) playerManager.togglePlayPause()
                                        }
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
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

                                    // Spoken line text
                                    Text(
                                        text = segment.text,
                                        fontSize = if (isActive) 16.sp else 14.sp,
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
                                            fontSize = 12.sp
                                        )
                                    }
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

        Spacer(modifier = Modifier.height(6.dp))

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

        Spacer(modifier = Modifier.height(12.dp))

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

    // Paste / Edit Lyrics Dialog
    if (showPasteLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showPasteLyricsDialog = false },
            title = {
                Text(
                    text = "Dán lời bài hát (Lyrics / LRC)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Bạn có thể dán lời bài hát dạng văn bản thường hoặc định dạng LRC [00:15] từ Zing MP3 / Spotify. Ứng dụng sẽ tự động đồng bộ theo nhạc:",
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
