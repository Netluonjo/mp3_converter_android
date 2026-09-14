package com.sondeptrai.mp3converter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioPlayerManager
import com.sondeptrai.mp3converter.ui.screens.MainTabContainer
import com.sondeptrai.mp3converter.ui.theme.MP3ConverterTheme

class MainActivity : ComponentActivity() {

    private lateinit var playerManager: AudioPlayerManager
    private lateinit var fileManager: AudioFileManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        fileManager = AudioFileManager(applicationContext)
        playerManager = AudioPlayerManager(applicationContext)

        setContent {
            MP3ConverterTheme {
                MainTabContainer(
                    playerManager = playerManager,
                    fileManager = fileManager
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        playerManager.release()
    }
}
