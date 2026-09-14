package com.sondeptrai.mp3converter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.model.AudioEffectsConfig
import com.sondeptrai.mp3converter.data.model.AudioExportConfig
import com.sondeptrai.mp3converter.data.model.AudioTrack
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioProcessingEngine
import com.sondeptrai.mp3converter.engine.FFmpegCommandBridge
import com.sondeptrai.mp3converter.ui.theme.CoralRed
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioEffectsScreen(
    track: AudioTrack,
    fileManager: AudioFileManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var volumeMultiplier by remember { mutableStateOf(1.5f) }
    var fadeInSec by remember { mutableStateOf(2.0f) }
    var fadeOutSec by remember { mutableStateOf(3.0f) }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tăng âm lượng & Fade") },
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

            // Volume Booster Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("TĂNG ÂM LƯỢNG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        Text("${(volumeMultiplier * 100).toInt()}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = CoralRed)
                    }
                    Slider(
                        value = volumeMultiplier,
                        onValueChange = { volumeMultiplier = it },
                        valueRange = 0.5f..3.0f,
                        colors = SliderDefaults.colors(thumbColor = CoralRed, activeTrackColor = CoralRed)
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1.0f, 1.5f, 2.0f, 2.5f).forEach { p ->
                            FilterChip(
                                selected = volumeMultiplier == p,
                                onClick = { volumeMultiplier = p },
                                label = { Text("${(p * 100).toInt()}%") }
                            )
                        }
                    }
                }
            }

            // Fade In / Out Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("HIỆU ỨNG MỜ DẦN (FADE IN / FADE OUT)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fade In (Đầu bài)", fontSize = 14.sp)
                            Text("%.1f giây".format(fadeInSec), fontWeight = FontWeight.Bold, color = CoralRed)
                        }
                        Slider(
                            value = fadeInSec,
                            onValueChange = { fadeInSec = it },
                            valueRange = 0f..5f,
                            colors = SliderDefaults.colors(thumbColor = CoralRed, activeTrackColor = CoralRed)
                        )
                    }

                    Column {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Fade Out (Cuối bài)", fontSize = 14.sp)
                            Text("%.1f giây".format(fadeOutSec), fontWeight = FontWeight.Bold, color = CoralRed)
                        }
                        Slider(
                            value = fadeOutSec,
                            onValueChange = { fadeOutSec = it },
                            valueRange = 0f..5f,
                            colors = SliderDefaults.colors(thumbColor = CoralRed, activeTrackColor = CoralRed)
                        )
                    }
                }
            }

            // FFmpeg command box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("LỆNH FFMPEG ÁP DỤNG BỘ LỌC VOLUME & AFADE:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                    val cmd = FFmpegCommandBridge.buildFadeCommand(
                        inputPath = track.filePath,
                        outputPath = "output_boosted.${track.format.extension}",
                        totalDurationSec = track.durationMs / 1000f,
                        fadeInSec = fadeInSec,
                        fadeOutSec = fadeOutSec
                    )
                    Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Green)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Apply Button
            Button(
                onClick = {
                    isProcessing = true
                    scope.launch {
                        try {
                            val outputFile = fileManager.generateOutputPath("${track.title}_Boosted", track.format)
                            val config = AudioExportConfig(
                                format = track.format,
                                effects = AudioEffectsConfig(
                                    volumeMultiplier = volumeMultiplier,
                                    fadeInDurationSec = fadeInSec,
                                    fadeOutDurationSec = fadeOutSec
                                )
                            )
                            AudioProcessingEngine.applyEffectsAndExport(
                                inputFile = File(track.filePath),
                                outputFile = outputFile,
                                config = config,
                                totalDurationSec = track.durationMs / 1000f
                            )
                            fileManager.reloadLibrary()
                            showSuccessDialog = true
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Áp dụng & Xuất tệp mới", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Khuếch đại thành công!") },
            text = { Text("File âm thanh đã được tăng âm lượng và lưu vào Thư viện.") },
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
