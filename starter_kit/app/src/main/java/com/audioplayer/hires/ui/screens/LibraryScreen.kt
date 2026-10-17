package com.audioplayer.hires.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.audioplayer.hires.ui.components.EmptyState
import com.audioplayer.hires.ui.components.TrackRow
import com.audioplayer.hires.ui.components.TrackUiModel
import com.audioplayer.hires.ui.theme.StudioColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    tracks: List<TrackUiModel>,
    onTrackClick: (TrackUiModel) -> Unit,
    onTrackMoreClick: (TrackUiModel) -> Unit,
    onSearchClick: () -> Unit,
    onRescanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = StudioColors.Background,
        topBar = {
            TopAppBar(
                title = { Text("Library", color = StudioColors.TextPrimary) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = "Search music", tint = StudioColors.TextPrimary)
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = StudioColors.Background
                )
            )
        }
    ) { innerPadding ->
        if (tracks.isEmpty()) {
            EmptyState(
                title = "Your library is quiet",
                message = "Scan your device for audio files to get started.",
                actionLabel = "Rescan library",
                onAction = onRescanClick,
                modifier = Modifier.padding(innerPadding)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "${tracks.size} tracks",
                    style = MaterialTheme.typography.bodySmall,
                    color = StudioColors.TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(tracks, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            onClick = { onTrackClick(track) },
                            onMoreClick = { onTrackMoreClick(track) }
                        )
                    }
                }
            }
        }
    }
}
