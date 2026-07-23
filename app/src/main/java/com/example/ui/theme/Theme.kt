package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    secondary = NeonPurple,
    tertiary = NeonMagenta,
    background = SpaceBackground,
    surface = PanelSolid,
    onPrimary = Color.Black,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = SpaceText,
    onSurface = SpaceText,
    primaryContainer = PanelSolid,
    secondaryContainer = NeonCyanDim
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force our custom dark space theme
    dynamicColor: Boolean = false, // Disable system pastels to keep the custom theme
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
