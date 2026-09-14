package com.sondeptrai.mp3converter.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.model.AudioToolType
import com.sondeptrai.mp3converter.ui.theme.CoralRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioToolsGridScreen(
    onSelectTool: (AudioToolType) -> Unit,
    modifier: Modifier = Modifier
) {
    var showFFmpegDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bộ công cụ (Tools)", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AudioToolType.entries.forEach { tool ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectTool(tool) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(tool.tintColor.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            val icon = when (tool) {
                                AudioToolType.VIDEO_TO_AUDIO -> Icons.Default.Movie
                                AudioToolType.TRIMMER -> Icons.Default.ContentCut
                                AudioToolType.MERGER -> Icons.Default.CallMerge
                                AudioToolType.VOLUME_BOOSTER -> Icons.Default.VolumeUp
                                AudioToolType.FORMAT_CONVERTER -> Icons.Default.Sync
                            }
                            Icon(icon, contentDescription = null, tint = tool.tintColor, modifier = Modifier.size(26.dp))
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(tool.titleVi, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(tool.subtitleVi, fontSize = 12.sp, color = Color.Gray, maxLines = 2)
                        }

                        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.LightGray)
                    }
                }
            }

            // FFmpeg reference banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showFFmpegDialog = true },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, tint = Color.Green, modifier = Modifier.size(32.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Kiến trúc FFmpeg-Kit Android", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Text("Xem 5 chuỗi command-line mẫu đã được tối ưu", color = Color.Gray, fontSize = 12.sp)
                    }
                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
                }
            }
        }
    }

    if (showFFmpegDialog) {
        AlertDialog(
            onDismissRequest = { showFFmpegDialog = false },
            title = { Text("Lệnh FFmpeg mẫu trên Android") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. Video to MP3:\n-i video.mp4 -vn -c:a libmp3lame -b:a 320k out.mp3", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CoralRed)
                    Text("2. Trimmer:\n-ss 00:15 -to 00:45 -i in.mp3 -c copy out.mp3", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CoralRed)
                    Text("3. Concat:\n-i a1.mp3 -i a2.mp3 -filter_complex concat out.mp3", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CoralRed)
                    Text("4. Volume:\n-i in.mp3 -filter:a volume=1.5 out.mp3", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CoralRed)
                    Text("5. Fade:\n-i in.mp3 -filter:a afade=t=in... out.mp3", fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = CoralRed)
                }
            },
            confirmButton = {
                TextButton(onClick = { showFFmpegDialog = false }) { Text("Đóng") }
            }
        )
    }
}
