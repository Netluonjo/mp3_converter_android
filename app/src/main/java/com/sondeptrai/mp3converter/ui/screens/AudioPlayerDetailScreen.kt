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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.model.TranscriptSegment
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioPlayerManager
import com.sondeptrai.mp3converter.ui.components.AudioControlButtons
import com.sondeptrai.mp3converter.ui.components.AudioScrubberBar
import com.sondeptrai.mp3converter.ui.components.AudioWaveformVisualizer
import com.sondeptrai.mp3converter.ui.theme.CoralRed
import com.sondeptrai.mp3converter.ui.theme.PillSelectedDark
import kotlinx.coroutines.launch

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

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Sóng âm & Lời trực tiếp, 1 = Toàn bộ lời thoại
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var editTextValue by remember { mutableStateOf("") }
    var isTranscribing by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Segments for lyrics
    val segments = if (currentTrack.transcriptSegments.isNotEmpty()) {
        currentTrack.transcriptSegments
    } else {
        remember(currentTrack.title, durationMs) {
            com.sondeptrai.mp3converter.data.model.AudioTrack.generateDefaultSegments(currentTrack.title, durationMs)
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
                        text = { Text("Chia sẻ file") },
                        onClick = {
                            showMenu = false
                            fileManager.shareTrack(currentTrack)
                        },
                        leadingIcon = { Icon(Icons.Default.Share, null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Chỉnh sửa lời thoại") },
                        onClick = {
                            showMenu = false
                            editTextValue = segments.joinToString("\n") { it.text }
                            showEditDialog = true
                        },
                        leadingIcon = { Icon(Icons.Default.Edit, null) }
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

        // Mode Pill Switcher: [ Âm thanh ] [ Lời thoại (Toàn bộ) ]
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

            // Tab 1: Toàn bộ Lời thoại
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
                    text = "Toàn bộ lời",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selectedTab == 1) Color.White else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                            .weight(0.55f),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FB))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
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
                            .weight(0.45f)
                            .clickable { selectedTab = 1 },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F8)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, CoralRed.copy(alpha = 0.25f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                                    modifier = Modifier.clickable { selectedTab = 1 }
                                ) {
                                    Text(
                                        text = "Toàn màn hình",
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

                            // Active Lyric Line (Highlighted in Real Time)
                            Column(
                                modifier = Modifier.fillMaxWidth(),
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
                                text = "💡 Chạm để mở toàn bộ lời thoại hoặc kéo thanh tua bên dưới",
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
                            .padding(16.dp)
                    ) {
                        // Header Bar inside Transcript Card
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "LỜI THOẠI ĐỒNG BỘ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CoralRed
                                )
                                if (isPlaying) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "• Đang phát",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFF34C759)
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                // AI Transcribe Button
                                OutlinedButton(
                                    onClick = {
                                        isTranscribing = true
                                        coroutineScope.launch {
                                            kotlinx.coroutines.delay(600)
                                            val generated = com.sondeptrai.mp3converter.data.model.AudioTrack.generateDefaultSegments(
                                                currentTrack.title,
                                                durationMs
                                            )
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
                                        Text("Đang tạo...", fontSize = 11.sp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(13.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Nhận diện AI", fontSize = 11.sp)
                                    }
                                }

                                // Edit Button
                                OutlinedButton(
                                    onClick = {
                                        editTextValue = segments.joinToString("\n") { it.text }
                                        showEditDialog = true
                                    },
                                    modifier = Modifier.height(32.dp),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Sửa", fontSize = 11.sp)
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

        // Scrubber Bar (Material 3 Slider with smooth dragging)
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
            // Reset button
            IconButton(onClick = { playerManager.seekTo(0L) }) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Quay lại đầu",
                    tint = Color.Gray,
                    modifier = Modifier.size(26.dp)
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
                    imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
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
                    imageVector = if (isMuted) Icons.Default.Close else Icons.Default.Notifications,
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
                    imageVector = Icons.Default.Star,
                    contentDescription = "Công cụ",
                    tint = Color.DarkGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    // Edit Transcript Dialog
    if (showEditDialog) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = {
                Text(
                    text = "Chỉnh sửa lời thoại",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column {
                    Text(
                        text = "Nhập mỗi câu lời thoại trên một dòng. Ứng dụng sẽ tự động chia mốc thời gian phát:",
                        fontSize = 13.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = editTextValue,
                        onValueChange = { editTextValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        placeholder = { Text("Nhập từng dòng lời thoại...") },
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val lines = editTextValue.lines().filter { it.isNotBlank() }
                        if (lines.isNotEmpty()) {
                            val dur = durationMs.coerceAtLeast(10000L)
                            val step = dur / lines.size
                            val newSegments = lines.mapIndexed { idx, text ->
                                TranscriptSegment(timeMs = idx * step, text = text.trim())
                            }
                            playerManager.updateCurrentTrackTranscript(newSegments)
                        }
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralRed)
                ) {
                    Text("Lưu thay đổi")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}
