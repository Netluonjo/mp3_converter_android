package com.sondeptrai.mp3converter.data.model

enum class AudioFormat(val displayName: String, val extension: String, val mimeType: String, val badge: String) {
    MP3("MP3", "mp3", "audio/mpeg", "Universal"),
    M4A("M4A", "m4a", "audio/mp4", "AAC Audio"),
    AAC("AAC", "aac", "audio/aac", "Raw AAC"),
    WAV("WAV", "wav", "audio/wav", "Lossless PCM"),
    FLAC("FLAC", "flac", "audio/flac", "Lossless Hi-Res")
}

enum class AudioSampleRate(val rate: Int, val label: String) {
    RATE_44100(44100, "44.1 kHz (CD)"),
    RATE_48000(48000, "48.0 kHz (Studio)"),
    RATE_96000(96000, "96.0 kHz (Hi-Res)")
}

enum class AudioBitrate(val kbps: Int, val label: String) {
    KBPS_128(128, "128 kbps (Tiêu chuẩn)"),
    KBPS_192(192, "192 kbps (Chất lượng cao)"),
    KBPS_256(256, "256 kbps (Rất cao)"),
    KBPS_320(320, "320 kbps (Cao nhất)")
}

data class AudioEffectsConfig(
    val volumeMultiplier: Float = 1.0f,
    val fadeInDurationSec: Float = 0f,
    val fadeOutDurationSec: Float = 0f
)

data class AudioExportConfig(
    val format: AudioFormat = AudioFormat.MP3,
    val sampleRate: AudioSampleRate = AudioSampleRate.RATE_44100,
    val bitrate: AudioBitrate = AudioBitrate.KBPS_320,
    val effects: AudioEffectsConfig = AudioEffectsConfig(),
    val trimStartMs: Long? = null,
    val trimEndMs: Long? = null
)
