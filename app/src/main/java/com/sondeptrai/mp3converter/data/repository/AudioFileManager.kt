package com.sondeptrai.mp3converter.data.repository

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import androidx.core.content.FileProvider
import com.sondeptrai.mp3converter.data.model.AudioFormat
import com.sondeptrai.mp3converter.data.model.AudioTrack
import com.sondeptrai.mp3converter.data.model.TranscriptSegment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class AudioFileManager(private val context: Context) {

    private val _savedTracks = MutableStateFlow<List<AudioTrack>>(listOf(AudioTrack.demoTrack))
    val savedTracks: StateFlow<List<AudioTrack>> = _savedTracks.asStateFlow()

    val exportsDir: File
        get() {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Exports")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    suspend fun reloadLibrary() = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        
        // Read app files
        val files = exportsDir.listFiles() ?: emptyArray()
        for (file in files) {
            val ext = file.extension.lowercase()
            val format = AudioFormat.entries.find { it.extension == ext } ?: continue
            val retriever = MediaMetadataRetriever()
            var durationMs = 0L
            try {
                retriever.setDataSource(file.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            } catch (_: Exception) {}
            finally {
                retriever.release()
            }

            val validDuration = if (durationMs > 0) durationMs else 20000L
            val defaultSegs = AudioTrack.generateDefaultSegments(file.nameWithoutExtension, validDuration)

            tracks.add(
                AudioTrack(
                    title = file.nameWithoutExtension,
                    uri = Uri.fromFile(file),
                    filePath = file.absolutePath,
                    durationMs = validDuration,
                    format = format,
                    fileSizeBytes = file.length(),
                    createdAt = file.lastModified(),
                    waveformSamples = AudioTrack.placeholderSamples(65),
                    transcriptSegments = defaultSegs
                )
            )
        }

        if (tracks.isEmpty()) {
            tracks.add(AudioTrack.demoTrack)
        }
        tracks.sortByDescending { it.createdAt }
        _savedTracks.value = tracks
    }

    fun generateOutputPath(baseName: String, format: AudioFormat): File {
        var name = baseName.trim().ifEmpty { "Audio_${System.currentTimeMillis()}" }
        var file = File(exportsDir, "$name.${format.extension}")
        var count = 1
        while (file.exists()) {
            file = File(exportsDir, "${name}_$count.${format.extension}")
            count++
        }
        return file
    }

    fun deleteTrack(track: AudioTrack) {
        val file = File(track.filePath)
        if (file.exists()) file.delete()
        val current = _savedTracks.value.toMutableList()
        current.removeAll { it.id == track.id }
        _savedTracks.value = current
    }

    fun shareTrack(track: AudioTrack) {
        val file = File(track.filePath)
        if (!file.exists()) return

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = track.format.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Chia sẻ âm thanh").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }
}
