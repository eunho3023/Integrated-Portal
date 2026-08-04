package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.Modifier

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

@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    accentColor: Color = NeonCyan,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp)),
        colors = CardDefaults.cardColors(
            containerColor = PanelGlass
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(18.dp)
        ) {
            content()
        }
    }
}
