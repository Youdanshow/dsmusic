package com.example.dsmusic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun DSMusicTheme(
    accentColor: Color = AccentColorDefault,
    content: @Composable () -> Unit
) {
    val colorScheme = darkColorScheme(
        primary = accentColor,
        secondary = accentColor,
        background = BackgroundBlack,
        surface = BackgroundBlack,
        onPrimary = TextWhite,
        onSecondary = TextWhite,
        onBackground = TextWhite,
        onSurface = TextWhite
    )
    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
