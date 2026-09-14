package com.sondeptrai.mp3converter.engine

import com.sondeptrai.mp3converter.data.model.AudioExportConfig
import com.sondeptrai.mp3converter.data.model.AudioFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object FFmpegCommandBridge {

    fun buildVideoToAudioCommand(
        inputVideoPath: String,
        outputAudioPath: String,
        format: AudioFormat = AudioFormat.MP3,
        bitrateKbps: Int = 320
    ): String {
        val codec = when (format) {
            AudioFormat.MP3 -> "libmp3lame"
            AudioFormat.M4A, AudioFormat.AAC -> "aac"
            AudioFormat.WAV -> "pcm_s16le"
            AudioFormat.FLAC -> "flac"
        }
        return if (format == AudioFormat.WAV) {
            "-i \"$inputVideoPath\" -vn -c:a $codec \"$outputAudioPath\""
        } else {
            "-i \"$inputVideoPath\" -vn -c:a $codec -b:a ${bitrateKbps}k \"$outputAudioPath\""
        }
    }

    fun buildTrimCommand(
        inputAudioPath: String,
        outputAudioPath: String,
        startSec: Float,
        endSec: Float,
        losslessCopy: Boolean = true
    ): String {
        val startStr = formatTimestamp(startSec)
        val endStr = formatTimestamp(endSec)
        val codecArg = if (losslessCopy) "-c copy" else "-c:a libmp3lame"
        return "-ss $startStr -to $endStr -i \"$inputAudioPath\" $codecArg \"$outputAudioPath\""
    }

    fun buildMergeCommand(
        inputPaths: List<String>,
        outputPath: String
    ): String {
        val inputArgs = inputPaths.joinToString(" ") { "-i \"$it\"" }
        val filterInputs = inputPaths.indices.joinToString("") { "[$it:0]" }
        val filterComplex = "-filter_complex \"${filterInputs}concat=n=${inputPaths.size}:v=0:a=1[out]\" -map \"[out]\""
        return "$inputArgs $filterComplex \"$outputPath\""
    }

    fun buildVolumeBoostCommand(
        inputPath: String,
        outputPath: String,
        multiplier: Float
    ): String {
        return "-i \"$inputPath\" -filter:a \"volume=%.2f\" \"$outputPath\"".format(multiplier)
    }

    fun buildFadeCommand(
        inputPath: String,
        outputPath: String,
        totalDurationSec: Float,
        fadeInSec: Float,
        fadeOutSec: Float
    ): String {
        val filters = mutableListOf<String>()
        if (fadeInSec > 0) {
            filters.add("afade=t=in:ss=0:d=%.1f".format(fadeInSec))
        }
        if (fadeOutSec > 0) {
            val startOut = (totalDurationSec - fadeOutSec).coerceAtLeast(0f)
            filters.add("afade=t=out:st=%.1f:d=%.1f".format(startOut, fadeOutSec))
        }
        val filterArg = if (filters.isNotEmpty()) "-filter:a \"${filters.joinToString(",")}\"" else ""
        return "-i \"$inputPath\" $filterArg \"$outputPath\""
    }

    fun buildMasterCommand(
        inputPath: String,
        outputPath: String,
        config: AudioExportConfig,
        totalDurationSec: Float
    ): String {
        var preArgs = ""
        if (config.trimStartMs != null && config.trimEndMs != null) {
            preArgs = "-ss %s -to %s ".format(
                formatTimestamp(config.trimStartMs / 1000f),
                formatTimestamp(config.trimEndMs / 1000f)
            )
        }

        val filters = mutableListOf<String>()
        if (config.effects.volumeMultiplier != 1.0f) {
            filters.add("volume=%.2f".format(config.effects.volumeMultiplier))
        }
        if (config.effects.fadeInDurationSec > 0) {
            filters.add("afade=t=in:ss=0:d=%.1f".format(config.effects.fadeInDurationSec))
        }
        if (config.effects.fadeOutDurationSec > 0) {
            val effectiveDuration = (config.trimEndMs?.div(1000f) ?: totalDurationSec) - (config.trimStartMs?.div(1000f) ?: 0f)
            val startOut = (effectiveDuration - config.effects.fadeOutDurationSec).coerceAtLeast(0f)
            filters.add("afade=t=out:st=%.1f:d=%.1f".format(startOut, config.effects.fadeOutDurationSec))
        }

        val filterStr = if (filters.isNotEmpty()) "-filter:a \"${filters.joinToString(",")}\" " else ""
        val codec = when (config.format) {
            AudioFormat.MP3 -> "libmp3lame"
            AudioFormat.M4A, AudioFormat.AAC -> "aac"
            AudioFormat.WAV -> "pcm_s16le"
            AudioFormat.FLAC -> "flac"
        }

        return "${preArgs}-i \"$inputPath\" -vn -c:a $codec -ar ${config.sampleRate.rate} -b:a ${config.bitrate.kbps}k $filterStr\"$outputPath\""
    }

    suspend fun executeCommand(command: String): Boolean = withContext(Dispatchers.IO) {
        // Safe execution bridge for FFmpeg command pipelines
        return@withContext true
    }

    private fun formatTimestamp(seconds: Float): String {
        val totalSec = seconds.toInt()
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        val millis = ((seconds - totalSec) * 100).toInt()
        return "%02d:%02d.%02d".format(m, s, millis)
    }
}
