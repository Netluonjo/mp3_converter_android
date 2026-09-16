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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.sondeptrai.mp3converter.data.model.AudioBitrate
import com.sondeptrai.mp3converter.data.model.AudioFormat
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioProcessingEngine
import com.sondeptrai.mp3converter.engine.FFmpegCommandBridge
import com.sondeptrai.mp3converter.ui.theme.CoralRed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var extractionProgress by remember { mutableFloatStateOf(0.01f) }
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
                title = { Text("TrĂ­ch xuáº¥t tá»« Video") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Quay láº¡i")
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
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = CoralRed,
                            modifier = Modifier.size(48.dp)
                        )
                        Text("Chá»n Video tá»« thiáº¿t bá»‹", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Há»— trá»£ Ä‘á»‹nh dáº¡ng MP4, MKV, MOV, 3GP, AVI", fontSize = 13.sp, color = Color.Gray)
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
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.White)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(videoName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text("Video Ä‘Ă£ chá»n sáºµn sĂ ng trĂ­ch xuáº¥t", fontSize = 13.sp, color = Color.Gray)
                        }
                        TextButton(onClick = { videoPickerLauncher.launch("video/*") }) {
                            Text("Äá»•i video", color = CoralRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Format Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Äá»NH Dáº NG Ă‚M THANH XUáº¤T", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
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
                Text("CHáº¤T LÆ¯á»¢NG BITRATE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
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
                    Text("Lá»†NH Xá»¬ LĂ FFMPEG:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.LightGray)
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
                    extractionProgress = 0.01f
                    scope.launch {
                        try {
                            var isFinished = false
                            
                            launch(Dispatchers.IO) {
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
                                } finally {
                                    isFinished = true
                                }
                            }
                            
                            // Smooth count up from 1% to 92%
                            var curPercent = 1
                            while (curPercent < 92) {
                                delay(30)
                                curPercent += 1
                                extractionProgress = curPercent / 100f
                            }
                            
                            // Await background process completion
                            while (!isFinished) {
                                delay(60)
                                if (curPercent < 96) {
                                    curPercent += 1
                                    extractionProgress = curPercent / 100f
                                }
                            }
                            
                            // Smoothly advance 93% to 100%
                            while (curPercent < 100) {
                                delay(25)
                                curPercent += 1
                                extractionProgress = curPercent / 100f
                            }
                            extractionProgress = 1.0f
                            
                            // Hold at 100% so user sees completion
                            delay(450)
                            
                            isProcessing = false
                            showSuccessDialog = true
                        } catch (e: Exception) {
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
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Báº¯t Ä‘áº§u trĂ­ch xuáº¥t Ă¢m thanh", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }

    // Circular Percentage Progress Dialog (1% to 100%)
    if (isProcessing) {
        val pct = (extractionProgress * 100).toInt().coerceIn(1, 100)
        Dialog(onDismissRequest = {}) {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
                        CircularProgressIndicator(
                            progress = { extractionProgress.coerceIn(0.01f, 1f) },
                            modifier = Modifier.fillMaxSize(),
                            color = CoralRed,
                            trackColor = Color(0xFFE5E5EA),
                            strokeWidth = 10.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = CoralRed,
                                modifier = Modifier.size(28.dp)
                            )
                            Text(
                                text = "$pct%",
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            )
                        }
                    }
                    Text(
                        "Äang trĂ­ch xuáº¥t Ă¢m thanh",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    val statusText = when {
                        pct < 25 -> "Äang náº¡p video vĂ  phĂ¢n tĂ­ch dá»¯ liá»‡u..."
                        pct < 65 -> "Äang trĂ­ch xuáº¥t dáº£i Ă¢m thanh tá»« video..."
                        pct < 90 -> "Äang mĂ£ hĂ³a sang Ä‘á»‹nh dáº¡ng " + selectedFormat.displayName + "..."
                        pct < 100 -> "Äang lÆ°u tá»‡p Ă¢m thanh vĂ o thÆ° viá»‡n..."
                        else -> "TrĂ­ch xuáº¥t hoĂ n táº¥t! (100%)"
                    }
                    Text(
                        statusText,
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSuccessDialog = false },
            title = { Text("TrĂ­ch xuáº¥t hoĂ n táº¥t!") },
            text = { Text("File Ă¢m thanh Ä‘Ă£ Ä‘Æ°á»£c lÆ°u thĂ nh cĂ´ng vĂ o thÆ° má»¥c Music cá»§a thiáº¿t bá»‹.") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccessDialog = false
                    onBack()
                }) {
                    Text("Xem trong ThÆ° viá»‡n", color = CoralRed, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}