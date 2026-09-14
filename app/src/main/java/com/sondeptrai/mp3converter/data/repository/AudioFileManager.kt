package com.sondeptrai.mp3converter.data.repository

import android.content.Context
import android.content.Intent
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.OpenableColumns
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
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioFileManager(private val context: Context) {

    private val _savedTracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val savedTracks: StateFlow<List<AudioTrack>> = _savedTracks.asStateFlow()

    val exportsDir: File
        get() {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "Exports")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }

    val publicMusicDir: File
        get() {
            val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "MP3Converter")
            if (!dir.exists()) {
                try { dir.mkdirs() } catch (_: Exception) {}
            }
            return dir
        }

    init {
        // Prepare demo audio file and load initial library
        val demoFile = getOrCreateDemoAudioFile()
        val initialDemoTrack = AudioTrack.demoTrack.copy(
            filePath = demoFile.absolutePath,
            uri = Uri.fromFile(demoFile)
        )
        _savedTracks.value = listOf(initialDemoTrack)
    }

    fun getOrCreateDemoAudioFile(): File {
        val file = File(context.cacheDir, "demo_track.wav")
        if (file.exists() && file.length() > 4096) return file

        try {
            val sampleRate = 44100
            val durationSec = 21
            val numSamples = sampleRate * durationSec
            val buffer = ShortArray(numSamples)

            val chords = listOf(
                listOf(261.63, 329.63, 392.00),
                listOf(196.00, 246.94, 293.66),
                listOf(220.00, 261.63, 329.63),
                listOf(174.61, 220.00, 261.63)
            )

            for (i in 0 until numSamples) {
                val t = i.toDouble() / sampleRate
                val chordIndex = ((t / 3.0).toInt()) % chords.size
                val chord = chords[chordIndex]

                var sampleVal = 0.0
                for (freq in chord) {
                    val envelope = Math.exp(-((t % 1.5) * 2.2))
                    sampleVal += Math.sin(2.0 * Math.PI * freq * t) * envelope * 0.28
                }
                val melodyFreq = 523.25 * (1.0 + 0.05 * Math.sin(2.0 * Math.PI * 2.0 * t))
                val melodyEnv = Math.exp(-((t % 0.75) * 3.0))
                sampleVal += Math.sin(2.0 * Math.PI * melodyFreq * t) * melodyEnv * 0.22

                val clamped = sampleVal.coerceIn(-1.0, 1.0)
                buffer[i] = (clamped * 32767.0).toInt().toShort()
            }

            val totalDataLen = numSamples * 2
            val totalAudioLen = totalDataLen + 36
            val header = ByteArray(44)

            header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
            header[4] = (totalAudioLen and 0xff).toByte()
            header[5] = ((totalAudioLen shr 8) and 0xff).toByte()
            header[6] = ((totalAudioLen shr 16) and 0xff).toByte()
            header[7] = ((totalAudioLen shr 24) and 0xff).toByte()
            header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
            header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
            header[16] = 16; header[17] = 0; header[18] = 0; header[19] = 0
            header[20] = 1; header[21] = 0
            header[22] = 1; header[23] = 0
            header[24] = (sampleRate and 0xff).toByte()
            header[25] = ((sampleRate shr 8) and 0xff).toByte()
            header[26] = ((sampleRate shr 16) and 0xff).toByte()
            header[27] = ((sampleRate shr 24) and 0xff).toByte()
            val byteRate = sampleRate * 2
            header[28] = (byteRate and 0xff).toByte()
            header[29] = ((byteRate shr 8) and 0xff).toByte()
            header[30] = ((byteRate shr 16) and 0xff).toByte()
            header[31] = ((byteRate shr 24) and 0xff).toByte()
            header[32] = 2; header[33] = 0
            header[34] = 16; header[35] = 0
            header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
            header[40] = (totalDataLen and 0xff).toByte()
            header[41] = ((totalDataLen shr 8) and 0xff).toByte()
            header[42] = ((totalDataLen shr 16) and 0xff).toByte()
            header[43] = ((totalDataLen shr 24) and 0xff).toByte()

            val fos = FileOutputStream(file)
            fos.write(header)
            val byteBuffer = ByteBuffer.allocate(totalDataLen).order(ByteOrder.LITTLE_ENDIAN)
            for (s in buffer) {
                byteBuffer.putShort(s)
            }
            fos.write(byteBuffer.array())
            fos.close()
        } catch (_: Exception) {}
        return file
    }

    suspend fun reloadLibrary() = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()
        val scannedPaths = mutableSetOf<String>()

        // Search multiple export locations
        val searchDirs = listOf(
            exportsDir,
            publicMusicDir,
            File(context.filesDir, "Exports"),
            File(context.cacheDir, "Exports")
        )

        for (dir in searchDirs) {
            if (!dir.exists()) continue
            val files = dir.listFiles() ?: continue
            for (file in files) {
                if (file.isDirectory) continue
                if (scannedPaths.contains(file.absolutePath)) continue
                if (file.length() < 100) continue

                val ext = file.extension.lowercase()
                val format = AudioFormat.entries.find { it.extension == ext } ?: continue
                scannedPaths.add(file.absolutePath)

                val retriever = MediaMetadataRetriever()
                var durationMs = 0L
                var metaTitle = file.nameWithoutExtension
                try {
                    retriever.setDataSource(file.absolutePath)
                    durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                    val songTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                    if (!songTitle.isNullOrBlank()) {
                        metaTitle = songTitle
                    }
                } catch (_: Exception) {}
                finally {
                    retriever.release()
                }

                val validDuration = if (durationMs > 0) durationMs else 30000L
                // Extract real lyrics for this song (from .lrc file, ID3 tag, or hit song match)
                val segments = SongLyricsHelper.getLyricsForTrack(metaTitle, validDuration, file)

                tracks.add(
                    AudioTrack(
                        title = metaTitle,
                        uri = Uri.fromFile(file),
                        filePath = file.absolutePath,
                        durationMs = validDuration,
                        format = format,
                        fileSizeBytes = file.length(),
                        createdAt = file.lastModified(),
                        waveformSamples = AudioTrack.placeholderSamples(65),
                        transcriptSegments = segments
                    )
                )
            }
        }

        // Always include demo track if library is empty
        val demoFile = getOrCreateDemoAudioFile()
        val readyDemoTrack = AudioTrack.demoTrack.copy(
            filePath = demoFile.absolutePath,
            uri = Uri.fromFile(demoFile)
        )
        if (tracks.isEmpty()) {
            tracks.add(readyDemoTrack)
        } else {
            // Also keep demo track at the end so user can always test
            if (tracks.none { it.title == readyDemoTrack.title }) {
                tracks.add(readyDemoTrack)
            }
        }

        tracks.sortByDescending { it.createdAt }
        _savedTracks.value = tracks
    }

    suspend fun importAudioFromUri(uri: Uri): AudioTrack? = withContext(Dispatchers.IO) {
        try {
            var fileName = "Imported_${System.currentTimeMillis()}.mp3"
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) fileName = name
                }
            }

            val targetFile = File(exportsDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(targetFile).use { output ->
                    input.copyTo(output)
                }
            }

            val retriever = MediaMetadataRetriever()
            var durationMs = 30000L
            var title = targetFile.nameWithoutExtension
            try {
                retriever.setDataSource(targetFile.absolutePath)
                durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 30000L
                val extractedTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                if (!extractedTitle.isNullOrBlank()) title = extractedTitle
            } catch (_: Exception) {}
            finally {
                retriever.release()
            }

            val ext = targetFile.extension.lowercase()
            val format = AudioFormat.entries.find { it.extension == ext } ?: AudioFormat.MP3
            val lyrics = SongLyricsHelper.getLyricsForTrack(title, durationMs, targetFile)

            val newTrack = AudioTrack(
                title = title,
                uri = Uri.fromFile(targetFile),
                filePath = targetFile.absolutePath,
                durationMs = durationMs,
                format = format,
                fileSizeBytes = targetFile.length(),
                createdAt = System.currentTimeMillis(),
                waveformSamples = AudioTrack.placeholderSamples(65),
                transcriptSegments = lyrics
            )

            val current = _savedTracks.value.toMutableList()
            current.add(0, newTrack)
            _savedTracks.value = current
            reloadLibrary()
            newTrack
        } catch (e: Exception) {
            null
        }
    }

    fun saveCustomLyrics(track: AudioTrack, rawLyrics: String): List<TranscriptSegment> {
        val segments = SongLyricsHelper.parseLrcOrText(rawLyrics, track.durationMs)
        if (segments.isNotEmpty()) {
            val file = File(track.filePath)
            if (file.exists()) {
                SongLyricsHelper.saveLrcFile(file, segments)
            }
            val updated = _savedTracks.value.map {
                if (it.id == track.id) it.copy(transcriptSegments = segments) else it
            }
            _savedTracks.value = updated
        }
        return segments
    }

    fun generateOutputPath(baseName: String, format: AudioFormat): File {
        val dir = exportsDir
        if (!dir.exists()) dir.mkdirs()
        var name = baseName.trim().ifEmpty { "Audio_${System.currentTimeMillis()}" }
        var file = File(dir, "$name.${format.extension}")
        var count = 1
        while (file.exists()) {
            file = File(dir, "${name}_$count.${format.extension}")
            count++
        }
        return file
    }

    fun deleteTrack(track: AudioTrack) {
        val file = File(track.filePath)
        if (file.exists()) file.delete()
        val lrc = File(file.parentFile, file.nameWithoutExtension + ".lrc")
        if (lrc.exists()) lrc.delete()
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
