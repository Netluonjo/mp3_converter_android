package com.sondeptrai.mp3converter.engine

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.sondeptrai.mp3converter.data.model.AudioTrack
import com.sondeptrai.mp3converter.data.model.TranscriptSegment
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioPlayerManager(private val context: Context) {

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        repeatMode = Player.REPEAT_MODE_OFF
    }

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    private val _currentTrack = MutableStateFlow<AudioTrack>(AudioTrack.demoTrack)
    val currentTrack: StateFlow<AudioTrack> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(21000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private val _isLooping = MutableStateFlow(false)
    val isLooping: StateFlow<Boolean> = _isLooping.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    val availableSpeeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)

    val progress: Float
        get() {
            val dur = _durationMs.value
            return if (dur > 0) (_currentPositionMs.value.toFloat() / dur).coerceIn(0f, 1f) else 0f
        }

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                _isPlaying.value = playing
                if (playing) {
                    startProgressPolling()
                } else {
                    stopProgressPolling()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    val dur = exoPlayer.duration
                    if (dur > 0) {
                        _durationMs.value = dur
                    }
                } else if (state == Player.STATE_ENDED) {
                    if (!_isLooping.value) {
                        _isPlaying.value = false
                        _currentPositionMs.value = _durationMs.value
                        stopProgressPolling()
                    }
                }
            }
        })

        // Ensure default demo audio file exists and is loaded
        val demoFile = createDemoAudioFileIfNeeded()
        val readyDemoTrack = AudioTrack.demoTrack.copy(
            filePath = demoFile.absolutePath,
            uri = Uri.fromFile(demoFile)
        )
        loadTrack(readyDemoTrack)
    }

    fun loadTrack(track: AudioTrack) {
        // Ensure track has transcript segments so lyrics are always available
        val preparedTrack = if (track.transcriptSegments.isEmpty()) {
            track.copy(transcriptSegments = AudioTrack.generateDefaultSegments(track.title, track.durationMs))
        } else {
            track
        }

        _currentTrack.value = preparedTrack
        _durationMs.value = if (preparedTrack.durationMs > 0) preparedTrack.durationMs else 21000L
        _currentPositionMs.value = 0L

        exoPlayer.stop()
        exoPlayer.clearMediaItems()

        try {
            val mediaItem = if (preparedTrack.filePath.isNotEmpty() && File(preparedTrack.filePath).exists()) {
                MediaItem.fromUri(Uri.fromFile(File(preparedTrack.filePath)))
            } else if (preparedTrack.uri != Uri.EMPTY) {
                MediaItem.fromUri(preparedTrack.uri)
            } else {
                val demoFile = createDemoAudioFileIfNeeded()
                MediaItem.fromUri(Uri.fromFile(demoFile))
            }

            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.repeatMode = if (_isLooping.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
            exoPlayer.playbackParameters = PlaybackParameters(_playbackSpeed.value)
            exoPlayer.volume = if (_isMuted.value) 0f else 1f
            exoPlayer.prepare()
        } catch (_: Exception) {}
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    fun play() {
        if (exoPlayer.playbackState == Player.STATE_ENDED) {
            exoPlayer.seekTo(0L)
            _currentPositionMs.value = 0L
        }
        exoPlayer.play()
        _isPlaying.value = true
        startProgressPolling()
    }

    fun pause() {
        exoPlayer.pause()
        _isPlaying.value = false
        stopProgressPolling()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _durationMs.value)
        _currentPositionMs.value = clamped
        exoPlayer.seekTo(clamped)
    }

    fun seekToProgress(progress: Float) {
        seekTo((progress * _durationMs.value).toLong())
    }

    fun skipBackward10() {
        seekTo((_currentPositionMs.value - 10000L).coerceAtLeast(0L))
    }

    fun skipForward10() {
        seekTo((_currentPositionMs.value + 10000L).coerceAtMost(_durationMs.value))
    }

    fun cycleSpeed() {
        val current = _playbackSpeed.value
        val idx = availableSpeeds.indexOf(current)
        val next = if (idx >= 0 && idx < availableSpeeds.size - 1) availableSpeeds[idx + 1] else availableSpeeds[0]
        _playbackSpeed.value = next
        exoPlayer.playbackParameters = PlaybackParameters(next)
    }

    fun toggleLoop() {
        _isLooping.value = !_isLooping.value
        exoPlayer.repeatMode = if (_isLooping.value) Player.REPEAT_MODE_ONE else Player.REPEAT_MODE_OFF
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        exoPlayer.volume = if (_isMuted.value) 0f else 1f
    }

    fun updateCurrentTrackTranscript(segments: List<TranscriptSegment>) {
        _currentTrack.value = _currentTrack.value.copy(transcriptSegments = segments)
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val current = exoPlayer.currentPosition
                if (current >= 0) {
                    _currentPositionMs.value = current
                }
                val dur = exoPlayer.duration
                if (dur > 0 && dur != _durationMs.value) {
                    _durationMs.value = dur
                }
                delay(33) // ~30Hz
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressPolling()
        exoPlayer.release()
        scope.cancel()
    }

    private fun createDemoAudioFileIfNeeded(): File {
        val file = File(context.cacheDir, "demo_track.wav")
        if (file.exists() && file.length() > 4096) return file

        try {
            val sampleRate = 44100
            val durationSec = 21
            val numSamples = sampleRate * durationSec
            val buffer = ShortArray(numSamples)

            val chords = listOf(
                listOf(261.63, 329.63, 392.00), // C major
                listOf(196.00, 246.94, 293.66), // G major
                listOf(220.00, 261.63, 329.63), // A minor
                listOf(174.61, 220.00, 261.63)  // F major
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
}
