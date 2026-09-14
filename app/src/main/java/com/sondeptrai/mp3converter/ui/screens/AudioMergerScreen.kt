package com.sondeptrai.mp3converter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlusOne
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.model.AudioFormat
import com.sondeptrai.mp3converter.data.model.AudioTrack
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioProcessingEngine
import com.sondeptrai.mp3converter.engine.FFmpegCommandBridge
import com.sondeptrai.mp3converter.ui.theme.CoralRed
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioMergerScreen(
    fileManager: AudioFileManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val savedTracks by fileManager.savedTracks.collectAsState()
    val selectedTracks = remember { mutableStateListOf<AudioTrack>() }

    var isMerging by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }
    var showSelectTrackDialog by remember { mutableStateOf(false) }

    val totalDurationMs = selectedTracks.sumOf { it.durationMs }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghép nhiều file âm thanh") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Trở lại")
                    }
                },
                actions = {
                    TextButton(onClick = { showSelectTrackDialog = true }) {
                        Text("+ Thêm tệp", color = CoralRed, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Danh sách cần ghép (${selectedTracks.size} tệp • Tổng: %02d:%02d)".format(
                    totalDurationMs / 60000,
                    (totalDurationMs % 60000) / 1000
                ),
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.SemiBold
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(selectedTracks) { index, track ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp).fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${index + 1}.", fontWeight = FontWeight.Bold, color = Color.Gray)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(track.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${track.formattedDuration} • ${track.format.displayName}", fontSize = 12.sp, color = Color.Gray)
                            }
                            IconButton(onClick = { selectedTracks.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = Color.Gray)
                            }
                        }
                    }
                }
            }

            // FFmpeg Concat preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("LỆNH FFMPEG GHÉP NỐI (Filter Concat):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                    val cmd = FFmpegCommandBridge.buildMergeCommand(
                        inputPaths = selectedTracks.map { it.filePath },
                        outputPath = "output_merged.mp3"
                    )
                    Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Green)
                }
            }

            // Action Merge
            Button(
                onClick = {
                    if (selectedTracks.size < 2) return@Button
                    isMerging = true
                    scope.launch {
                        try {
                            val outputFile = fileManager.generateOutputPath("Merged_Audio", AudioFormat.MP3)
                            AudioProcessingEngine.mergeAudioFiles(
                                inputFiles = selectedTracks.map { File(it.filePath) },
                                outputFile = outputFile
                            )
                            fileManager.reloadLibrary()
                            showSuccessDialog = true
                        } finally {
                            isMerging = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = selectedTracks.size >= 2 && !isMerging,
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.CallMerge, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ghép ${selectedTracks.size} tệp thành một", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showSelectTrackDialog) {
        AlertDialog(
            onDismissRequest = { showSelectTrackDialog = false },
            title = { Text("Chọn file để ghép") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(savedTracks.size) { i ->
                        val t = savedTracks[i]
                        TextButton(
                            onClick = {
                                selectedTracks.add(t)
                                showSelectTrackDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${t.title} (${t.formattedDuration})", color = Color.Black)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSelectTrackDialog = false }) { Text("Đóng") }
            }
        )
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Ghép file thành công!") },
            text = { Text("File âm thanh mới đã được lưu vào thư mục Music.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onBack()
                }) {
                    Text("OK", color = CoralRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
