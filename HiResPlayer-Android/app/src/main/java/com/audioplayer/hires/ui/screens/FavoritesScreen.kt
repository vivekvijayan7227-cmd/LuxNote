package com.audioplayer.hires.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.model.PlayerStatus
import com.audioplayer.hires.model.Track
import com.audioplayer.hires.ui.MainViewModel
import com.audioplayer.hires.ui.components.EmptyState
import com.audioplayer.hires.ui.components.StudioPrimaryButton
import com.audioplayer.hires.ui.components.StudioSecondaryButton
import com.audioplayer.hires.ui.theme.StudioColors

@Composable
fun FavoritesScreen(viewModel: MainViewModel) {
    val playlists by viewModel.playlists.collectAsState()
    val libraryTracks by viewModel.libraryTracks.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val playerStatus by viewModel.playerStatus.collectAsState()

    val favTracks = remember(playlists, libraryTracks) {
        viewModel.getFavoriteTracks()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        // 1. Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Favorites",
                    style = MaterialTheme.typography.headlineMedium,
                    color = StudioColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${favTracks.size} LIKED TRACKS",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextMuted,
                    letterSpacing = 1.sp
                )
            }

            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = StudioColors.Gold,
                modifier = Modifier.size(24.dp)
            )
        }

        if (favTracks.isEmpty()) {
            EmptyState(
                title = "No favorites yet",
                message = "Tap the heart icon on any song while listening to collect your favorite tracks here.",
                actionLabel = "Browse Library",
                onAction = { viewModel.setScreen(com.audioplayer.hires.ui.StudioScreen.LIBRARY) },
                modifier = Modifier.weight(1f)
            )
        } else {
            // 2. Play All & Shuffle Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StudioPrimaryButton(
                    label = "Play All",
                    icon = Icons.Default.PlayArrow,
                    onClick = { viewModel.playFavorites(shuffle = false) },
                    modifier = Modifier.weight(1f)
                )

                StudioSecondaryButton(
                    label = "Shuffle",
                    icon = Icons.Default.Shuffle,
                    onClick = { viewModel.playFavorites(shuffle = true) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3. Favorite Tracks List
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(favTracks, key = { it.id }) { track ->
                    val isPlaying = currentTrack?.id == track.id && playerStatus == PlayerStatus.PLAYING
                    val isCurrent = currentTrack?.id == track.id

                    FavoriteTrackRow(
                        track = track,
                        isPlaying = isPlaying,
                        isCurrent = isCurrent,
                        onClick = { viewModel.playQueue(favTracks, favTracks.indexOf(track)) },
                        onRemoveFavorite = { viewModel.toggleFavorite(track.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteTrackRow(
    track: Track,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    onRemoveFavorite: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isCurrent) StudioColors.SurfaceSelected else StudioColors.Surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCurrent) StudioColors.Gold.copy(alpha = 0.4f) else StudioColors.Divider
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Artwork / Vinyl Icon Placeholder
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) StudioColors.Gold else StudioColors.TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Title & Artist
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) StudioColors.Gold else StudioColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${track.artist} · ${track.format}",
                    color = StudioColors.TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Duration
            Text(
                text = formatDuration(track.durationMs),
                color = StudioColors.TextMuted,
                style = MaterialTheme.typography.bodySmall
            )

            // Remove from Favorites (active gold heart)
            IconButton(
                onClick = onRemoveFavorite,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = "Remove from favorites",
                    tint = StudioColors.Gold,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
