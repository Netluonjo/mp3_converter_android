package com.sondeptrai.mp3converter.data.model

import androidx.compose.ui.graphics.Color

enum class AudioToolType(
    val titleVi: String,
    val subtitleVi: String,
    val iconName: String,
    val tintColor: Color
) {
    VIDEO_TO_AUDIO(
        "Bóc Audio từ Video",
        "Trích xuất MP3/M4A từ video trong máy",
        "movie",
        Color(0xFFFF4B4B)
    ),
    TRIMMER(
        "Cắt nhạc (Trimmer)",
        "Cắt đoạn điệp khúc bằng sóng âm trực quan",
        "content_cut",
        Color(0xFFFF9800)
    ),
    MERGER(
        "Ghép file âm thanh",
        "Nối nhiều bài hát, bản ghi âm thành 1 file duy nhất",
        "call_merge",
        Color(0xFF4CAF50)
    ),
    VOLUME_BOOSTER(
        "Tăng âm lượng",
        "Khuếch đại âm lượng lên đến 200% - 300%",
        "volume_up",
        Color(0xFF673AB7)
    ),
    FORMAT_CONVERTER(
        "Đổi định dạng",
        "Chuyển đổi MP3, M4A, WAV, AAC, FLAC",
        "sync",
        Color(0xFF2196F3)
    ),
    WIFI_TRANSFER(
        "Chuyển nhạc Wi-Fi",
        "Truyền file 2 chiều giữa Máy tính và Điện thoại",
        "wifi",
        Color(0xFF00BCD4)
    )
}
