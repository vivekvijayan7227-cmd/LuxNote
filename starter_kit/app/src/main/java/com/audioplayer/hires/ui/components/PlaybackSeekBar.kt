package com.audioplayer.hires.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.audioplayer.hires.ui.theme.StudioColors

@Composable
fun PlaybackSeekBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val safeDuration = durationMs.coerceAtLeast(1L)
    val safePosition = positionMs.coerceIn(0L, safeDuration)
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = safePosition.toFloat(),
            onValueChange = { onSeek(it.toLong()) },
            valueRange = 0f..safeDuration.toFloat(),
            colors = SliderDefaults.colors(
                thumbColor = StudioColors.Gold,
                activeTrackColor = StudioColors.Gold,
                inactiveTrackColor = StudioColors.ProgressTrack
            ),
            modifier = Modifier.semantics {
                contentDescription = "Playback position"
            }
        )
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
            Text(formatTime(safePosition), color = StudioColors.TextSecondary)
            androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            Text(formatTime(safeDuration), color = StudioColors.TextSecondary)
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
