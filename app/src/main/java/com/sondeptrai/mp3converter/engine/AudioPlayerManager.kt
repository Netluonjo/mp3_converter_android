package com.sondeptrai.mp3converter.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import com.sondeptrai.mp3converter.data.model.AudioTrack
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class AudioPlayerManager(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
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

    fun loadTrack(track: AudioTrack) {
        _currentTrack.value = track
        _durationMs.value = track.durationMs
        _currentPositionMs.value = 0L

        mediaPlayer?.release()
        mediaPlayer = null

        val file = File(track.filePath)
        if (file.exists()) {
            try {
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(track.filePath)
                    prepare()
                    this@AudioPlayerManager._durationMs.value = duration.toLong()
                    isLooping = this@AudioPlayerManager._isLooping.value
                    setOnCompletionListener {
                        if (!isLooping) {
                            _isPlaying.value = false
                            _currentPositionMs.value = _durationMs.value
                            stopProgressPolling()
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    fun play() {
        if (mediaPlayer != null) {
            applySpeed()
            mediaPlayer?.start()
        }
        _isPlaying.value = true
        startProgressPolling()
    }

    fun pause() {
        mediaPlayer?.pause()
        _isPlaying.value = false
        stopProgressPolling()
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _durationMs.value)
        _currentPositionMs.value = clamped
        mediaPlayer?.seekTo(clamped.toInt())
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
        applySpeed()
    }

    fun toggleLoop() {
        _isLooping.value = !_isLooping.value
        mediaPlayer?.isLooping = _isLooping.value
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
        val volume = if (_isMuted.value) 0f else 1f
        mediaPlayer?.setVolume(volume, volume)
    }

    private fun applySpeed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && mediaPlayer != null) {
            try {
                val params = mediaPlayer?.playbackParams ?: PlaybackParams()
                params.speed = _playbackSpeed.value
                mediaPlayer?.playbackParams = params
            } catch (_: Exception) {}
        }
    }

    private fun startProgressPolling() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive && _isPlaying.value) {
                val mp = mediaPlayer
                if (mp != null) {
                    _currentPositionMs.value = mp.currentPosition.toLong()
                } else {
                    // Virtual simulation
                    val next = _currentPositionMs.value + (33 * _playbackSpeed.value).toLong()
                    if (next >= _durationMs.value) {
                        if (_isLooping.value) {
                            _currentPositionMs.value = 0L
                        } else {
                            _currentPositionMs.value = _durationMs.value
                            pause()
                        }
                    } else {
                        _currentPositionMs.value = next
                    }
                }
                delay(33) // ~30Hz update rate
            }
        }
    }

    private fun stopProgressPolling() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressPolling()
        mediaPlayer?.release()
        mediaPlayer = null
        scope.cancel()
    }
}
