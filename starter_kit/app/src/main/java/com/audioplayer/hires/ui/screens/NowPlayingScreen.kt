package com.audioplayer.hires.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.audioplayer.hires.ui.components.PlaybackSeekBar
import com.audioplayer.hires.ui.theme.StudioColors

@Composable
fun NowPlayingScreen(
    title: String,
    artist: String,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    isFavorite: Boolean,
    onSeek: (Long) -> Unit,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleFavorite: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().background(StudioColors.Background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("NOW PLAYING", style = MaterialTheme.typography.labelMedium, color = StudioColors.TextSecondary)
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) StudioColors.Gold else StudioColors.TextPrimary
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxWidth(0.82f).aspectRatio(1f)
                .clip(RoundedCornerShape(18.dp))
                .background(StudioColors.SurfaceElevated),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = StudioColors.TextMuted, modifier = Modifier.size(56.dp))
        }

        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, style = MaterialTheme.typography.headlineMedium, color = StudioColors.TextPrimary, maxLines = 2)
            Text(artist, style = MaterialTheme.typography.bodyLarge, color = StudioColors.TextSecondary, maxLines = 1)
        }

        PlaybackSeekBar(positionMs = positionMs, durationMs = durationMs, onSeek = onSeek)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onShuffle) { Icon(Icons.Default.Shuffle, contentDescription = "Toggle shuffle", tint = StudioColors.Blue) }
            IconButton(onClick = onPrevious) { Icon(Icons.Default.SkipPrevious, contentDescription = "Previous track", tint = StudioColors.TextPrimary, modifier = Modifier.size(32.dp)) }
            IconButton(onClick = onPlayPause, modifier = Modifier.size(76.dp)) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = StudioColors.Background,
                    modifier = Modifier.size(42.dp)
                        .background(StudioColors.Gold, RoundedCornerShape(100.dp))
                        .padding(6.dp)
                )
            }
            IconButton(onClick = onNext) { Icon(Icons.Default.SkipNext, contentDescription = "Next track", tint = StudioColors.TextPrimary, modifier = Modifier.size(32.dp)) }
            IconButton(onClick = onRepeat) { Icon(Icons.Default.Repeat, contentDescription = "Toggle repeat", tint = StudioColors.Blue) }
        }
    }
}
