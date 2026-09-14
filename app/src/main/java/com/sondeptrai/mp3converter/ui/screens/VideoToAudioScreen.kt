package com.sondeptrai.mp3converter.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.model.AudioBitrate
import com.sondeptrai.mp3converter.data.model.AudioFormat
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioProcessingEngine
import com.sondeptrai.mp3converter.engine.FFmpegCommandBridge
import com.sondeptrai.mp3converter.ui.theme.CoralRed
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoToAudioScreen(
    fileManager: AudioFileManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var videoName by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(AudioFormat.MP3) }
    var selectedBitrate by remember { mutableStateOf(AudioBitrate.KBPS_320) }
    var isProcessing by remember { mutableStateOf(false) }
    var showSuccessDialog by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedVideoUri = uri
            videoName = "Video_${System.currentTimeMillis()}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trích xuất từ Video") },
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
            // Picker Box
            if (selectedVideoUri == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF2F2F7))
                        .clickable { videoPickerLauncher.launch("video/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.VideoCall,
                            contentDescription = null,
                            tint = CoralRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Chọn Video trong máy", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Hỗ trợ MP4, MKV, MOV, 3GP", fontSize = 13.sp, color = Color.Gray)
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.Black),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(videoName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Video đã chọn sẵn sàng bóc tách", fontSize = 13.sp, color = Color.Gray)
                        }
                        TextButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                            Text("Đổi", color = CoralRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Format Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ĐỊNH DẠNG ÂM THANH XUẤT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioFormat.entries.take(4).forEach { fmt ->
                        val isSelected = selectedFormat == fmt
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedFormat = fmt },
                            label = { Text(fmt.displayName) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoralRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // Bitrate Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("CHẤT LƯỢNG BITRATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AudioBitrate.entries.forEach { b ->
                        val isSelected = selectedBitrate == b
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedBitrate = b },
                            label = { Text("${b.kbps}k") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = CoralRed,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            // FFmpeg Command Box
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("LỆNH FFMPEG THỰC THI:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
                    val cmd = FFmpegCommandBridge.buildVideoToAudioCommand(
                        inputVideoPath = "input_video.mp4",
                        outputAudioPath = "output.${selectedFormat.extension}",
                        format = selectedFormat,
                        bitrateKbps = selectedBitrate.kbps
                    )
                    Text(cmd, fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = Color.Green)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button
            Button(
                onClick = {
                    val uri = selectedVideoUri ?: return@Button
                    isProcessing = true
                    scope.launch {
                        try {
                            val tempVideo = File(context.cacheDir, "temp_video_${System.currentTimeMillis()}.mp4")
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                FileOutputStream(tempVideo).use { output -> input.copyTo(output) }
                            }
                            val outputFile = fileManager.generateOutputPath(videoName, selectedFormat)
                            AudioProcessingEngine.extractAudioFromVideo(
                                videoFile = tempVideo,
                                outputAudioFile = outputFile,
                                format = selectedFormat,
                                bitrateKbps = selectedBitrate.kbps
                            )
                            fileManager.reloadLibrary()
                            showSuccessDialog = true
                        } finally {
                            isProcessing = false
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                enabled = selectedVideoUri != null && !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = CoralRed),
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isProcessing) {
                    CircularProgressView(modifier = Modifier.size(24.dp), color = Color.White)
                } else {
                    Icon(Icons.Default.Bolt, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Bắt đầu trích xuất âm thanh", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("Trích xuất hoàn tất!") },
            text = { Text("File âm thanh đã được lưu vào thư mục Music của thiết bị.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onBack()
                }) {
                    Text("Xem trong Thư viện", color = CoralRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
