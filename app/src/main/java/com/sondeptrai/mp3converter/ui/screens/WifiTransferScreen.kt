package com.sondeptrai.mp3converter.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.data.server.WifiTransferManager
import com.sondeptrai.mp3converter.ui.theme.CoralRed

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiTransferScreen(
    fileManager: AudioFileManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val transferManager = remember { WifiTransferManager(context, fileManager) }
    val isRunning by transferManager.isServerRunning.collectAsState()
    val serverUrl by transferManager.serverUrl.collectAsState()
    val logs by transferManager.logs.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            transferManager.stopServer()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chuyển nhạc qua Wi-Fi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Main Server Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isRunning) Color(0xFF1B2A22) else Color(0xFFF4F4F8)
                ),
                border = if (isRunning) CardDefaults.outlinedCardBorder().copy(brush = androidx.compose.ui.graphics.SolidColor(Color(0xFF2E7D32))) else null
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) Color(0xFF4CAF50) else Color.Gray)
                            )
                            Text(
                                text = if (isRunning) "Máy chủ đang chạy" else "Máy chủ đã tắt",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isRunning) Color(0xFF81C784) else Color.Black
                            )
                        }

                        Switch(
                            checked = isRunning,
                            onCheckedChange = { start ->
                                if (start) {
                                    val success = transferManager.startServer()
                                    if (!success) {
                                        Toast.makeText(context, "Vui lòng kết nối Wi-Fi trước khi bật!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    transferManager.stopServer()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4CAF50)
                            )
                        )
                    }

                    if (isRunning && serverUrl != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nhập địa chỉ sau vào trình duyệt trên PC:",
                            fontSize = 13.sp,
                            color = Color(0xFFB0BEC5)
                        )

                        // Highlighted URL Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF101B15))
                                .border(1.dp, Color(0xFF388E3C), RoundedCornerShape(12.dp))
                                .padding(vertical = 14.dp, horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = serverUrl ?: "",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color(0xFF69F0AE),
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }

                        // Important HTTP hint
                        Text(
                            text = "⚠️ Lưu ý: Bắt buộc gõ đầy đủ http:// ở đầu (không dùng https://)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFFB74D),
                            textAlign = TextAlign.Center
                        )

                        // Copy & Share buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = {
                                    serverUrl?.let { url ->
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        clipboard.setPrimaryClip(ClipData.newPlainText("WiFi Transfer URL", url))
                                        Toast.makeText(context, "Đã sao chép địa chỉ vào bộ nhớ tạm!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sao chép", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    serverUrl?.let { url ->
                                        val sendIntent = Intent().apply {
                                            action = Intent.ACTION_SEND
                                            putExtra(Intent.EXTRA_TEXT, "Truy cập địa chỉ này trên máy tính để chuyển file nhạc: $url")
                                            type = "text/plain"
                                        }
                                        context.startActivity(Intent.createChooser(sendIntent, "Chia sẻ link Wi-Fi Transfer"))
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF81C784))
                            ) {
                                Text("Chia sẻ", fontSize = 13.sp)
                            }
                        }
                    } else {
                        Text(
                            text = "Gạt nút công tắc phía trên để bắt đầu chia sẻ file với máy tính qua Wi-Fi.",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Step-by-step Instructions Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9FC))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Hướng dẫn 3 bước đơn giản", fontWeight = FontWeight.Bold, fontSize = 15.sp)

                    InstructionStep(
                        step = "1",
                        title = "Chung mạng Wi-Fi",
                        desc = "Đảm bảo điện thoại và máy tính kết nối cùng 1 mạng Wi-Fi (hoặc điện thoại bật Điểm phát sóng di động)."
                    )
                    InstructionStep(
                        step = "2",
                        title = "Mở trình duyệt trên máy tính",
                        desc = "Mở Chrome, Cốc Cốc hoặc Edge trên máy tính, gõ đúng địa chỉ IP hiển thị ở khung trên."
                    )
                    InstructionStep(
                        step = "3",
                        title = "Kéo thả & Tải file 2 chiều",
                        desc = "Kéo file nhạc vào trình duyệt để chuyển vào app, hoặc bấm Tải về để copy nhạc về máy tính."
                    )
                }
            }

            // Live Activity Logs Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF18181C))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Nhật ký truyền file", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("${logs.size} sự kiện", color = Color.Gray, fontSize = 12.sp)
                    }

                    if (logs.isEmpty()) {
                        Text(
                            text = "Chưa có hoạt động truyền file nào...",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            logs.take(8).forEach { log ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = log.time,
                                        color = CoralRed,
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = log.message,
                                        color = Color(0xFFECEFF1),
                                        fontSize = 12.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun InstructionStep(step: String, title: String, desc: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(CoralRed.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(step, color = CoralRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(desc, fontSize = 12.sp, color = Color.Gray, lineHeight = 17.sp)
        }
    }
}
