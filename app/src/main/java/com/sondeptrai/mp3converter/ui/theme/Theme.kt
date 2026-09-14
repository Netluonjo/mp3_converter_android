package com.sondeptrai.mp3converter.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = CoralRed,
    secondary = Color(0xFF5856D6),
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF2F2F7),
    onPrimary = Color.White,
    onBackground = Color(0xFF1C1C1E),
    onSurface = Color(0xFF1C1C1E)
)

private val DarkColorScheme = darkColorScheme(
    primary = CoralRed,
    secondary = Color(0xFF5856D6),
    background = Color(0xFF000000),
    surface = Color(0xFF1C1C1E),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun MP3ConverterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
