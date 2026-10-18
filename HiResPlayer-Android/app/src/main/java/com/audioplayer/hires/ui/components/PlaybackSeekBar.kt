package com.audioplayer.hires.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.audioplayer.hires.ui.theme.StudioColors

/**
 * Direct replacement for LuxNote v3.4.2's PlaybackSeekBar.
 *
 * Crucial behavior: display drag progress locally, but only trigger the expensive
 * decoder/AudioTrack seek on gesture completion. Disable seeking for unknown duration.
 * The playback owner must still serialize seeks and flush AudioTrack in PAUSED state.
 */
@Composable
fun PlaybackSeekBar(
    positionMs: Long,
    durationMs: Long,
    trackId: Long = 0L,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val duration = durationMs.coerceAtLeast(0L)
    val playable = duration > 0L
    val safePosition = positionMs.coerceIn(0L, duration)

    // Reset local gesture state if track identity or duration changes across tracks.
    var isDragging by remember(trackId, durationMs) { mutableStateOf(false) }
    var dragValue by remember(trackId, durationMs) { mutableFloatStateOf(0f) }
    val sliderMax = duration.coerceAtLeast(1L).toFloat()
    val shownValue = if (isDragging) dragValue.coerceIn(0f, sliderMax)
                     else safePosition.toFloat()
    val shownPosition = if (isDragging) shownValue.toLong().coerceIn(0L, duration)
                        else safePosition

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = shownValue,
            onValueChange = { value ->
                if (playable) {
                    isDragging = true
                    dragValue = value.coerceIn(0f, sliderMax)
                }
            },
            onValueChangeFinished = {
                if (playable && isDragging) {
                    val target = dragValue.toLong().coerceIn(0L, duration)
                    isDragging = false
                    onSeek(target)
                }
            },
            valueRange = 0f..sliderMax,
            enabled = playable,
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
            Text(formatSeekTime(shownPosition), color = StudioColors.TextSecondary)
            Spacer(Modifier.weight(1f))
            Text(formatSeekTime(duration), color = StudioColors.TextSecondary)
        }
    }
}

private fun formatSeekTime(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0L) / 1000L
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}
