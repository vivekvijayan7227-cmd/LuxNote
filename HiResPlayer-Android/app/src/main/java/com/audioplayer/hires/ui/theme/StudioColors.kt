package com.audioplayer.hires.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object StudioColors {
    val Background = Color(0xFF07090E)
    val Surface = Color(0xFF101521)
    val SurfaceElevated = Color(0xFF182033)
    val SurfaceSelected = Color(0xFF202B40)

    val Gold = Color(0xFFF5C76A)
    val GoldHighlight = Color(0xFFFFE5A3)
    val Blue = Color(0xFF67B7FF)

    val TextPrimary = Color(0xFFE8ECF4)
    val TextSecondary = Color(0xFFA0A9BA)
    val TextMuted = Color(0xFF737F93)

    val Divider = Color(0xFF293246)
    val ProgressTrack = Color(0xFF343D50)

    val Success = Color(0xFF66D6A3)
    val Warning = Color(0xFFF2B866)
    val Error = Color(0xFFFF777F)
}

object StudioSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object StudioSizes {
    val screenPadding = 24.dp
    val touchTarget = 48.dp
    val primaryPlay = 72.dp
    val sectionTitle = 20.sp
    val trackTitle = 16.sp
    val metadata = 13.sp
}

// Backward-Compatibility Aliases for Seamless Integration
val StudioObsidian = StudioColors.Background
val StudioCardBg = StudioColors.Surface
val StudioSurfaceElevated = StudioColors.SurfaceElevated
val StudioBorder = StudioColors.Divider
val StudioGold = StudioColors.Gold
val StudioGoldHighlight = StudioColors.GoldHighlight
val StudioAiCyan = StudioColors.Blue
val StudioVuGreen = StudioColors.Success
val StudioPeakCrimson = StudioColors.Error
val StudioTextPrimary = StudioColors.TextPrimary
val StudioTextSecondary = StudioColors.TextSecondary
val StudioTextMuted = StudioColors.TextMuted
val StudioProgressTrack = StudioColors.ProgressTrack
