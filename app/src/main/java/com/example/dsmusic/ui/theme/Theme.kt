package com.example.dsmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val ColorScheme = lightColorScheme(
    primary = PinkAccent,
    secondary = PinkAccent,
    background = BackgroundBlack,
    surface = BackgroundBlack,
    onPrimary = TextBlack,
    onSecondary = TextBlack,
    onBackground = TextBlack,
    onSurface = TextBlack
)

@Composable
fun DSMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
