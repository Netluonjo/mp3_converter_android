package com.sondeptrai.mp3converter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.model.AudioTrack
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioPlayerManager
import com.sondeptrai.mp3converter.engine.AudioProcessingEngine
import com.sondeptrai.mp3converter.engine.FFmpegCommandBridge
import com.sondeptrai.mp3converter.ui.components.AudioWaveformVisualizer
import com.sondeptrai.mp3converter.ui.components.WaveformTrimmerOverlay
import com.sondeptrai.mp3converter.ui.theme.CoralRed
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioTrimmerScreen(
    track: AudioTrack,
    playerManager: AudioPlayerManager,
    fileManager: AudioFileManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var startRatio by remember { mutableStateOf(0.15f) }
    var endRatio by remember { mutableStateOf(0.85f) }
    var isCutting by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val startMs = (startRatio * track.durationMs).toLong()
    val endMs = (endRatio * track.durationMs).toLong()
    val lengthMs = (endMs - startMs).coerceAtLeast(0L)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cắt nhạc (Trimmer)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Trở lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(track.title, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            // Waveform with Trimmer Overlay
            Box(modifier = Modifier.fillMaxWidth().height(180.dp)) {
                AudioWaveformVisualizer(
                    samples = track.waveformSamples,
                    progress = playerManager.progress,
                    onSeek = { p -> playerManager.seekToProgress(p) }
                )
                WaveformTrimmerOverlay(
                    startRatio = startRatio,
                    endRatio = endRatio,
                    durationMs = track.durationMs,
                    onTrimChange = { s, e ->
                        startRatio = s
                        endRatio = e
                    }
                )
            }

            // Info Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("BẮT ĐẦU", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("%02d:%02d".format(startMs / 60000, (startMs % 60000) / 1000), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CoralRed)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ĐOẠN CẮT", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("%02d:%02d".format(lengthMs / 60000, (lengthMs % 60000) / 1000), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("KẾT THÚC", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                        Text("%02d:%02d".format(endMs / 60000, (endMs % 60000) / 1000), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = CoralRed)
                    }
                }
            }

            // Preview button
            TextButton(
                onClick = {
                    playerManager.seekTo(startMs)
                    playerManager.play()
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CoralRed)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Nghe thử đoạn đã chọn", color = CoralRed, fontWeight = FontWeight.Bold)
            }

            // FFmpeg command box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("LỆNH FFMPEG CẮT SIÊU TỐC (-c copy):", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                    val cmd = FFmpegCommandBridge.buildTrimCommand(
                        inputAudioPath = track.filePath,
                        outputAudioPath = "output_trimmed.${track.format.extension}",
                        startSec = startMs / 1000f,
                        endSec = endMs / 1000f
                    )
                    Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Green)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Cut Button
            Button(
                onClick = {
                    isCutting = true
                    scope.launch {
                        try {
                            val targetFile = fileManager.generateOutputPath("${track.title}_Trimmed", track.format)
                            AudioProcessingEngine.trimAudio(
                                inputFile = File(track.filePath),
                                outputFile = targetFile,
                                startMs = startMs,
                                endMs = endMs
                            )
                            fileManager.reloadLibrary()
                            showSuccessDialog = true
                        } finally {
                            isCutting = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !isCutting,
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Edit, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Cắt và Lưu tệp mới", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Cắt nhạc thành công!") },
            text = { Text("Tệp âm thanh mới đã được tạo và lưu vào Thư viện.") },
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


