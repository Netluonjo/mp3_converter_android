package com.sondeptrai.mp3converter.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.model.AudioBitrate
import com.sondeptrai.mp3converter.data.model.AudioExportConfig
import com.sondeptrai.mp3converter.data.model.AudioFormat
import com.sondeptrai.mp3converter.data.model.AudioSampleRate
import com.sondeptrai.mp3converter.data.model.AudioTrack
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioProcessingEngine
import com.sondeptrai.mp3converter.engine.FFmpegCommandBridge
import com.sondeptrai.mp3converter.ui.theme.CoralRed
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ExportSettingsDialog(
    track: AudioTrack,
    fileManager: AudioFileManager,
    onDismiss: () -> Unit,
    onExportComplete: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var outputFormat by remember { mutableStateOf(AudioFormat.MP3) }
    var bitrate by remember { mutableStateOf(AudioBitrate.KBPS_320) }
    var sampleRate by remember { mutableStateOf(AudioSampleRate.RATE_44100) }
    var fileName by remember { mutableStateOf("${track.title}_Converted") }
    var isExporting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Xuất / Đổi định dạng", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Tên file") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Định dạng âm thanh:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AudioFormat.entries.take(3).forEach { fmt ->
                        FilterChip(
                            selected = outputFormat == fmt,
                            onClick = { outputFormat = fmt },
                            label = { Text(fmt.displayName) }
                        )
                    }
                }

                Text("Bitrate:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AudioBitrate.entries.forEach { b ->
                        FilterChip(
                            selected = bitrate == b,
                            onClick = { bitrate = b },
                            label = { Text("${b.kbps}k") }
                        )
                    }
                }

                // Command Preview
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    val cmd = FFmpegCommandBridge.buildVideoToAudioCommand(
                        inputVideoPath = track.filePath,
                        outputAudioPath = "output.${outputFormat.extension}",
                        format = outputFormat,
                        bitrateKbps = bitrate.kbps
                    )
                    Text(
                        text = cmd,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.Green,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isExporting = true
                    scope.launch {
                        try {
                            val targetFile = fileManager.generateOutputPath(fileName, outputFormat)
                            val config = AudioExportConfig(
                                format = outputFormat,
                                sampleRate = sampleRate,
                                bitrate = bitrate
                            )
                            AudioProcessingEngine.applyEffectsAndExport(
                                inputFile = File(track.filePath),
                                outputFile = targetFile,
                                config = config,
                                totalDurationSec = track.durationMs / 1000f
                            )
                            fileManager.reloadLibrary()
                            onExportComplete()
                        } finally {
                            isExporting = false
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                enabled = !isExporting
            ) {
                Text(if (isExporting) "Đang xuất..." else "Xuất file", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Hủy") }
        }
    )
}
