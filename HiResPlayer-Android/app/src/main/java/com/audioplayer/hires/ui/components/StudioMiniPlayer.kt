package com.audioplayer.hires.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.model.PlayerStatus
import com.audioplayer.hires.ui.MainViewModel
import com.audioplayer.hires.ui.StudioScreen
import com.audioplayer.hires.ui.theme.StudioColors

/**
 * Obsidian Studio Docked Mini-Player above the bottom navigation bar.
 * Matches Image 4 and UI/UX design specifications.
 */
@Composable
fun StudioMiniPlayer(viewModel: MainViewModel) {
    val currentTrack by viewModel.currentTrack.collectAsState()
    val playerStatus by viewModel.playerStatus.collectAsState()
    val currentScreen by viewModel.currentScreen.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()

    val isVisible = currentTrack != null && currentScreen != StudioScreen.NOW_PLAYING

    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        val track = currentTrack ?: return@AnimatedVisibility
        val progress = if (track.durationMs > 0) (positionMs.toFloat() / track.durationMs.toFloat()).coerceIn(0f, 1f) else 0f

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { viewModel.setScreen(StudioScreen.NOW_PLAYING) },
            color = StudioColors.Surface,
            shadowElevation = 8.dp,
            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider)
        ) {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Mini artwork thumbnail & Title / Artist
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            color = StudioColors.SurfaceElevated,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider)
                        ) {
                            TrackArtwork(
                                artworkUri = track.artworkUri,
                                trackUri = track.uriString,
                                contentDescription = "Artwork for ${track.title}",
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = StudioColors.Gold,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = track.title,
                                color = StudioColors.TextPrimary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${track.artist} · ${track.formatBadge}",
                                color = StudioColors.TextSecondary,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Transport Actions: Previous, Play/Pause, Next (Image 4)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.playPrevious() },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = StudioColors.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.togglePlayPause() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(StudioColors.Gold)
                        ) {
                            Icon(
                                imageVector = if (playerStatus == PlayerStatus.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (playerStatus == PlayerStatus.PLAYING) "Pause" else "Play",
                                tint = StudioColors.Background,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.playNext() },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = StudioColors.TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                // Precision Progress Line along bottom edge (Champagne Gold)
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp),
                    color = StudioColors.Gold,
                    trackColor = StudioColors.ProgressTrack
                )
            }
        }
    }
}
