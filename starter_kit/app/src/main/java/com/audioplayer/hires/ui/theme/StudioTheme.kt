package com.audioplayer.hires.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val StudioDarkScheme = darkColorScheme(
    primary = StudioColors.Gold,
    onPrimary = StudioColors.Background,
    secondary = StudioColors.Blue,
    onSecondary = StudioColors.Background,
    background = StudioColors.Background,
    onBackground = StudioColors.TextPrimary,
    surface = StudioColors.Surface,
    onSurface = StudioColors.TextPrimary,
    surfaceVariant = StudioColors.SurfaceElevated,
    onSurfaceVariant = StudioColors.TextSecondary,
    outline = StudioColors.Divider,
    error = StudioColors.Error,
    onError = StudioColors.Background
)

@Composable
fun StudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StudioDarkScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content
    )
}
