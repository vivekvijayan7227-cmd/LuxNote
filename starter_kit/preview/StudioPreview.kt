package com.audioplayer.hires.ui.preview

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.audioplayer.hires.ui.screens.LibraryScreen
import com.audioplayer.hires.ui.components.TrackUiModel
import com.audioplayer.hires.ui.theme.StudioTheme

@Preview(showBackground = true, backgroundColor = 0xFF07090E)
@Composable
fun LibraryScreenPreview() {
    StudioTheme {
        LibraryScreen(
            tracks = listOf(
                TrackUiModel("1", "Midnight Signals", "Artist Name", "4:18", true),
                TrackUiModel("2", "Glass Horizon", "Another Artist", "3:42"),
                TrackUiModel("3", "After the Rain", "Example Artist", "5:06")
            ),
            onTrackClick = {},
            onTrackMoreClick = {},
            onSearchClick = {},
            onRescanClick = {}
        )
    }
}
