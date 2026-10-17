package com.audioplayer.hires.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.audioplayer.hires.ui.theme.StudioAiCyan
import com.audioplayer.hires.ui.theme.StudioPeakCrimson
import com.audioplayer.hires.ui.theme.StudioVuGreen

@Composable
fun FftVisualizer(
    bands: FloatArray,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(80.dp)
) {
    Canvas(modifier = modifier) {
        val count = bands.size
        if (count == 0) return@Canvas

        val w = size.width
        val h = size.height
        val barWidth = (w / count) * 0.75f
        val gap = (w / count) * 0.25f

        val gradient = Brush.verticalGradient(
            colors = listOf(
                StudioPeakCrimson,
                StudioAiCyan,
                StudioVuGreen
            ),
            startY = 0f,
            endY = h
        )

        for (i in 0 until count) {
            val magnitude = bands[i].coerceIn(0.02f, 1.0f)
            val barHeight = h * magnitude
            val x = i * (barWidth + gap) + gap / 2.0f
            val y = h - barHeight

            // Draw glowing bar
            drawRect(
                brush = gradient,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight)
            )

            // Draw peak cap
            drawRect(
                color = Color.White.copy(alpha = 0.8f),
                topLeft = Offset(x, y),
                size = Size(barWidth, 2.dp.toPx())
            )
        }
    }
}
