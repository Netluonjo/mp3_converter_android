package com.sondeptrai.mp3converter.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sondeptrai.mp3converter.data.model.AudioToolType
import com.sondeptrai.mp3converter.data.repository.AudioFileManager
import com.sondeptrai.mp3converter.engine.AudioPlayerManager
import com.sondeptrai.mp3converter.ui.theme.CoralRed

@Composable
fun MainTabContainer(
    playerManager: AudioPlayerManager,
    fileManager: AudioFileManager,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(0) }
    var activeTool by remember { mutableStateOf<AudioToolType?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }

    val currentTrack by playerManager.currentTrack.collectAsState()

    // Screen navigation based on activeTool
    when (val tool = activeTool) {
        AudioToolType.VIDEO_TO_AUDIO -> {
            VideoToAudioScreen(fileManager = fileManager, onBack = { activeTool = null })
            return
        }
        AudioToolType.TRIMMER -> {
            AudioTrimmerScreen(track = currentTrack, playerManager = playerManager, fileManager = fileManager, onBack = { activeTool = null })
            return
        }
        AudioToolType.MERGER -> {
            AudioMergerScreen(fileManager = fileManager, onBack = { activeTool = null })
            return
        }
        AudioToolType.VOLUME_BOOSTER -> {
            AudioEffectsScreen(track = currentTrack, fileManager = fileManager, onBack = { activeTool = null })
            return
        }
        AudioToolType.FORMAT_CONVERTER -> {
            showExportDialog = true
            activeTool = null
        }
        null -> {
            // Main Bottom Navigation
            Scaffold(
                bottomBar = {
                    NavigationBar(containerColor = Color.White) {
                        NavigationBarItem(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            icon = { Icon(Icons.Default.GraphicEq, contentDescription = "Studio") },
                            label = { Text("Studio") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CoralRed,
                                selectedTextColor = CoralRed,
                                indicatorColor = CoralRed.copy(alpha = 0.12f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            icon = { Icon(Icons.Default.Tune, contentDescription = "Công cụ") },
                            label = { Text("Công cụ") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CoralRed,
                                selectedTextColor = CoralRed,
                                indicatorColor = CoralRed.copy(alpha = 0.12f)
                            )
                        )
                        NavigationBarItem(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Thư viện") },
                            label = { Text("Thư viện") },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = CoralRed,
                                selectedTextColor = CoralRed,
                                indicatorColor = CoralRed.copy(alpha = 0.12f)
                            )
                        )
                    }
                }
            ) { padding ->
                when (selectedTab) {
                    0 -> AudioPlayerDetailScreen(
                        playerManager = playerManager,
                        fileManager = fileManager,
                        onNavigateToTools = { selectedTab = 1 },
                        modifier = Modifier.padding(padding)
                    )
                    1 -> AudioToolsGridScreen(
                        onSelectTool = { toolSelected -> activeTool = toolSelected },
                        modifier = Modifier.padding(padding)
                    )
                    2 -> AudioLibraryScreen(
                        fileManager = fileManager,
                        playerManager = playerManager,
                        onSelectTrackToPlay = { selectedTab = 0 },
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    if (showExportDialog) {
        ExportSettingsDialog(
            track = currentTrack,
            fileManager = fileManager,
            onDismiss = { showExportDialog = false },
            onExportComplete = { showExportDialog = false }
        )
    }
}
