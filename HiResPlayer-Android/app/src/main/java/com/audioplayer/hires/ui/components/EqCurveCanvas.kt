package com.audioplayer.hires.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.audioplayer.hires.ui.theme.StudioAiCyan
import com.audioplayer.hires.ui.theme.StudioBorder
import com.audioplayer.hires.ui.theme.StudioObsidian
import com.audioplayer.hires.ui.theme.StudioVuGreen
import kotlin.math.exp
import kotlin.math.ln

@Composable
fun EqCurveCanvas(
    evaluator: (Float) -> Float,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(StudioObsidian)
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerY = h / 2.0f

        // dB range: -15 dB to +15 dB
        val maxDb = 15.0f
        fun dbToY(db: Float): Float {
            val clamped = db.coerceIn(-maxDb, maxDb)
            return centerY - (clamped / maxDb) * (centerY * 0.9f)
        }

        val minFreq = 20.0f
        val maxFreq = 20000.0f
        val logMin = ln(minFreq)
        val logMax = ln(maxFreq)
        fun freqToX(f: Float): Float {
            val logF = ln(f.coerceIn(minFreq, maxFreq))
            return ((logF - logMin) / (logMax - logMin)) * w
        }

        // Draw horizontal grid lines: +12, +6, 0, -6, -12 dB
        val gridDbs = floatArrayOf(12f, 6f, 0f, -6f, -12f)
        for (db in gridDbs) {
            val y = dbToY(db)
            val lineCol = if (db == 0f) StudioBorder.copy(alpha = 0.8f) else StudioBorder.copy(alpha = 0.35f)
            val strokeW = if (db == 0f) 1.5f else 1.0f
            drawLine(
                color = lineCol,
                start = Offset(0f, y),
                end = Offset(w, y),
                strokeWidth = strokeW
            )
        }

        // Draw vertical frequency grid lines: 100 Hz, 1 kHz, 10 kHz
        val gridFreqs = floatArrayOf(100f, 1000f, 10000f)
        for (f in gridFreqs) {
            val x = freqToX(f)
            drawLine(
                color = StudioBorder.copy(alpha = 0.4f),
                start = Offset(x, 0f),
                end = Offset(x, h),
                strokeWidth = 1.0f
            )
        }

        // Build continuous curve path
        val curvePath = Path()
        val fillPath = Path()
        val points = 120

        var first = true
        for (i in 0..points) {
            val frac = i.toFloat() / points
            val freq = exp(logMin + (logMax - logMin) * frac)
            val db = evaluator(freq)
            val x = frac * w
            val y = dbToY(db)

            if (first) {
                curvePath.moveTo(x, y)
                fillPath.moveTo(x, centerY)
                fillPath.lineTo(x, y)
                first = false
            } else {
                curvePath.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(w, centerY)
        fillPath.close()

        // Draw gradient fill under curve
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    StudioAiCyan.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h
            )
        )

        // Draw sharp high-resolution response curve line
        drawPath(
            path = curvePath,
            brush = Brush.horizontalGradient(
                colors = listOf(
                    StudioVuGreen,
                    StudioAiCyan
                )
            ),
            style = Stroke(width = 2.5f)
        )
    }
}
