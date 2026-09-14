package com.sondeptrai.mp3converter.data.model

import android.net.Uri
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

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
    val transcript: String? = null
) {
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
            transcript = "Xin chào! Đây là bản ghi âm mẫu chất lượng cao trên Android. Bạn có thể cắt ghép nhạc, trích xuất âm thanh từ video, tăng âm lượng và đổi định dạng sang MP3/M4A/WAV/FLAC dễ dàng."
        )
    }
}
