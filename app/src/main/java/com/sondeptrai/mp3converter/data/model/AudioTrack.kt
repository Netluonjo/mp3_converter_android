package com.sondeptrai.mp3converter.data.model

import android.net.Uri
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

data class TranscriptSegment(
    val id: String = UUID.randomUUID().toString(),
    val timeMs: Long,
    val text: String
) {
    val formattedTime: String
        get() {
            val totalSec = timeMs / 1000
            val m = totalSec / 60
            val s = totalSec % 60
            return "%02d:%02d".format(m, s)
        }
}

data class AudioTrack(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val uri: Uri,
    val filePath: String,
    val durationMs: Long,
    val format: AudioFormat = AudioFormat.MP3,
    val sampleRate: Int = 44100,
    val bitrateKbps: Int = 192,
    val fileSizeBytes: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val waveformSamples: List<Float> = emptyList(),
    val transcriptSegments: List<TranscriptSegment> = emptyList()
) {
    val transcript: String?
        get() = if (transcriptSegments.isNotEmpty()) {
            transcriptSegments.joinToString("\n") { "[${it.formattedTime}] ${it.text}" }
        } else null

    val formattedDuration: String
        get() {
            val totalSec = durationMs / 1000
            val m = totalSec / 60
            val s = totalSec % 60
            return "%02d:%02d".format(m, s)
        }

    val formattedFileSize: String
        get() {
            val mb = fileSizeBytes / (1024.0 * 1024.0)
            return if (mb >= 1.0) "%.1f MB".format(mb) else "%d KB".format(fileSizeBytes / 1024)
        }

    companion object {
        fun placeholderSamples(count: Int = 65): List<Float> {
            return (0 until count).map { i ->
                val progress = i.toFloat() / count.toFloat()
                val bell = sin(progress * Math.PI.toFloat())
                val wobble = sin(progress * 16f) * 0.2f + cos(progress * 8f) * 0.15f
                (bell * 0.7f + wobble * 0.3f + 0.15f).coerceIn(0.12f, 1.0f)
            }
        }

        val defaultSegments = listOf(
            TranscriptSegment(timeMs = 0L, text = "Xin chào! Chào mừng bạn đến với Studio Âm Thanh."),
            TranscriptSegment(timeMs = 3500L, text = "Đây là bản ghi âm mẫu chất lượng cao trên Android."),
            TranscriptSegment(timeMs = 8000L, text = "Lời thoại đang được đồng bộ trực tiếp theo thời gian phát."),
            TranscriptSegment(timeMs = 13000L, text = "Bạn có thể chạm vào từng câu để tua nhanh tới đoạn đó."),
            TranscriptSegment(timeMs = 17500L, text = "Hỗ trợ cắt ghép, tăng âm lượng và xuất file MP3 chuyên nghiệp.")
        )

        val demoTrack = AudioTrack(
            title = "Ghi âm 1",
            uri = Uri.EMPTY,
            filePath = "",
            durationMs = 21000L,
            format = AudioFormat.M4A,
            sampleRate = 44100,
            bitrateKbps = 192,
            fileSizeBytes = 512000L,
            waveformSamples = placeholderSamples(65),
            transcriptSegments = defaultSegments
        )
    }
}