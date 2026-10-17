package com.audioplayer.hires.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.model.MusicFolder
import com.audioplayer.hires.model.Playlist
import com.audioplayer.hires.model.PlayerStatus
import com.audioplayer.hires.model.SortOption
import com.audioplayer.hires.model.Track
import com.audioplayer.hires.ui.MainViewModel
import com.audioplayer.hires.ui.StudioScreen
import com.audioplayer.hires.ui.components.EmptyState
import com.audioplayer.hires.ui.theme.*

enum class LibraryCategoryTab(val title: String) {
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    FOLDERS("Folders"),
    PLAYLISTS("Playlists")
}

@Composable
fun LibraryScreen(viewModel: MainViewModel) {
    val allTracks by viewModel.libraryTracks.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val folders by viewModel.folders.collectAsState()
    val queues by viewModel.queues.collectAsState()
    val currentTrack by viewModel.currentTrack.collectAsState()
    val playerStatus by viewModel.playerStatus.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val currentSort by viewModel.currentSort.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importProgress by viewModel.importProgress.collectAsState()

    var selectedTab by remember { mutableStateOf(LibraryCategoryTab.SONGS) }
    var showSearchBar by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    // Navigation sub-views (Folder, Artist, Album, Playlist)
    var selectedPlaylistForView by remember { mutableStateOf<Playlist?>(null) }
    var selectedFolderForView by remember { mutableStateOf<MusicFolder?>(null) }
    var selectedArtistForView by remember { mutableStateOf<String?>(null) }
    var selectedAlbumForView by remember { mutableStateOf<String?>(null) }

    val isInsideSubView = selectedPlaylistForView != null || selectedFolderForView != null || selectedArtistForView != null || selectedAlbumForView != null
    BackHandler(enabled = isInsideSubView) {
        selectedPlaylistForView = null
        selectedFolderForView = null
        selectedArtistForView = null
        selectedAlbumForView = null
    }

    // Dialog state
    var trackToAddToPlaylist by remember { mutableStateOf<Track?>(null) }
    var trackToAddToQueue by remember { mutableStateOf<Track?>(null) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var inspectedTrack by remember { mutableStateOf<Track?>(null) }

    // SAF File Picker
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            viewModel.importUris(uris)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        // 1. Top Header (Image 4): Studio logo + YOUR MUSIC LIBRARY + Search & Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (selectedPlaylistForView != null || selectedFolderForView != null || selectedArtistForView != null || selectedAlbumForView != null) {
                    IconButton(
                        onClick = {
                            selectedPlaylistForView = null
                            selectedFolderForView = null
                            selectedArtistForView = null
                            selectedAlbumForView = null
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = StudioColors.Gold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                } else {
                    // Studio Circular Gold Emblem (Image 4)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StudioColors.SurfaceElevated)
                            .border(1.dp, StudioColors.Gold.copy(alpha = 0.6f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = StudioColors.Gold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }

                Column {
                    Text(
                        text = when {
                            selectedPlaylistForView != null -> selectedPlaylistForView!!.name
                            selectedFolderForView != null -> selectedFolderForView!!.name
                            selectedArtistForView != null -> selectedArtistForView!!
                            selectedAlbumForView != null -> selectedAlbumForView!!
                            else -> "LuxNote"
                        },
                        color = StudioColors.TextPrimary,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (selectedPlaylistForView == null && selectedFolderForView == null && selectedArtistForView == null && selectedAlbumForView == null)
                            "YOUR MUSIC LIBRARY" else "${allTracks.size} TOTAL TRACKS",
                        color = StudioColors.TextMuted,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Header Actions: Search, Rescan, Add
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showSearchBar = !showSearchBar },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = if (showSearchBar || searchQuery.isNotBlank()) StudioColors.Gold else StudioColors.TextPrimary
                    )
                }

                IconButton(
                    onClick = { viewModel.scanDeviceMusic(showIndicator = true) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Rescan Storage",
                        tint = StudioColors.Gold
                    )
                }

                IconButton(
                    onClick = { viewModel.setScreen(StudioScreen.DEVICE_INSPECTOR) },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "System Diagnostics",
                        tint = StudioColors.Gold
                    )
                }

                IconButton(
                    onClick = {
                        filePickerLauncher.launch(arrayOf("audio/*", "application/ogg", "application/x-flac", "*/*"))
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Files",
                        tint = StudioColors.Blue
                    )
                }
            }
        }

        // Live Import Progress Indicator
        if (isImporting) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = StudioColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Blue),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = importProgress.ifBlank { "Processing audio files..." },
                        color = StudioColors.Blue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = StudioColors.Gold,
                        trackColor = StudioColors.ProgressTrack
                    )
                }
            }
        }

        // Search Bar (expandable)
        if (showSearchBar || searchQuery.isNotBlank()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search songs, artists, albums...", color = StudioColors.TextMuted, fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = StudioColors.Gold) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = StudioColors.TextMuted)
                        }
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = StudioColors.Surface,
                    unfocusedContainerColor = StudioColors.Surface,
                    focusedBorderColor = StudioColors.Gold,
                    unfocusedBorderColor = StudioColors.Divider,
                    focusedTextColor = StudioColors.TextPrimary,
                    unfocusedTextColor = StudioColors.TextPrimary
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .height(50.dp)
            )
        }

        // 2. Horizontal Category Filter Chips (Image 4)
        if (selectedPlaylistForView == null && selectedFolderForView == null && selectedArtistForView == null && selectedAlbumForView == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryCategoryTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .clickable { selectedTab = tab },
                        shape = RoundedCornerShape(100.dp),
                        color = if (isSelected) StudioColors.Gold else StudioColors.Surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) StudioColors.Gold else StudioColors.Divider
                        )
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) StudioColors.Background else StudioColors.TextPrimary,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // 3. Tab Content (Songs, Albums, Artists, Folders, Playlists, or Sub-view)
        when {
            selectedPlaylistForView != null -> {
                PlaylistDetailView(
                    playlist = selectedPlaylistForView!!,
                    allTracks = allTracks,
                    currentTrack = currentTrack,
                    viewModel = viewModel,
                    onInspect = { inspectedTrack = it }
                )
            }
            selectedFolderForView != null -> {
                val folder = selectedFolderForView!!
                val folderTracks = allTracks.filter { track ->
                    val path = if (track.uriString.startsWith("file://")) Uri.parse(track.uriString).path ?: "" else track.uriString
                    path.startsWith(folder.path) || folder.path.contains(track.album)
                }
                FilteredTrackListView(
                    title = "Folder: ${folder.name}",
                    tracks = folderTracks,
                    currentTrack = currentTrack,
                    viewModel = viewModel,
                    onInspect = { inspectedTrack = it },
                    onAddToPlaylist = { trackToAddToPlaylist = it }
                )
            }
            selectedArtistForView != null -> {
                val artistTracks = allTracks.filter { it.artist.equals(selectedArtistForView, ignoreCase = true) }
                FilteredTrackListView(
                    title = "Artist: $selectedArtistForView",
                    tracks = artistTracks,
                    currentTrack = currentTrack,
                    viewModel = viewModel,
                    onInspect = { inspectedTrack = it },
                    onAddToPlaylist = { trackToAddToPlaylist = it }
                )
            }
            selectedAlbumForView != null -> {
                val albumTracks = allTracks.filter { it.album.equals(selectedAlbumForView, ignoreCase = true) }
                FilteredTrackListView(
                    title = "Album: $selectedAlbumForView",
                    tracks = albumTracks,
                    currentTrack = currentTrack,
                    viewModel = viewModel,
                    onInspect = { inspectedTrack = it },
                    onAddToPlaylist = { trackToAddToPlaylist = it }
                )
            }
            selectedTab == LibraryCategoryTab.SONGS -> {
                // Tracks Tab with Sort Options
                val displayTracks = viewModel.getFilteredAndSortedTracks()

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${displayTracks.size} tracks",
                        style = MaterialTheme.typography.bodySmall,
                        color = StudioColors.TextSecondary
                    )

                    Box {
                        TextButton(
                            onClick = { showSortMenu = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, tint = StudioColors.Gold, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(currentSort.name.replace("_", " "), color = StudioColors.Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(StudioColors.Surface)
                        ) {
                            SortOption.values().forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.name.replace("_", " "), color = StudioColors.TextPrimary, fontSize = 12.sp) },
                                    onClick = {
                                        viewModel.setSortOption(opt)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (displayTracks.isEmpty()) {
                    EmptyState(
                        title = "No songs found in local storage",
                        message = "LuxNote reads music directly from your phone's storage in real time without importing. Tap Refresh to discover your tracks, or open Diagnostics to check permissions.",
                        actionLabel = "Refresh Music Library",
                        onAction = { viewModel.scanDeviceMusic(showIndicator = true) },
                        secondaryActionLabel = "System & Storage Diagnostics",
                        onSecondaryAction = { viewModel.setScreen(StudioScreen.DEVICE_INSPECTOR) },
                        modifier = Modifier.weight(1f)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(displayTracks, key = { it.id }) { track ->
                            val isPlaying = currentTrack?.id == track.id && playerStatus == PlayerStatus.PLAYING
                            val isCurrent = currentTrack?.id == track.id
                            val isFavorite = viewModel.isFavorite(track.id)

                            StudioTrackRowCard(
                                track = track,
                                isPlaying = isPlaying,
                                isCurrent = isCurrent,
                                isFavorite = isFavorite,
                                onPlayClick = { viewModel.playTrack(track) },
                                onFavoriteClick = { viewModel.toggleFavorite(track.id) },
                                onAddToPlaylistClick = { trackToAddToPlaylist = track },
                                onAddToQueueClick = { trackToAddToQueue = track },
                                onInspectClick = { inspectedTrack = track }
                            )
                        }
                    }
                }
            }
            selectedTab == LibraryCategoryTab.ALBUMS -> {
                val albums = remember(allTracks) {
                    allTracks.map { it.album }.filter { it.isNotBlank() }.distinct().sorted()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(albums) { album ->
                        val albumSongs = allTracks.filter { it.album == album }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedAlbumForView = album },
                            shape = RoundedCornerShape(12.dp),
                            color = StudioColors.Surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(StudioColors.SurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Album, contentDescription = null, tint = StudioColors.Gold, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(album, color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${albumSongs.size} tracks", color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StudioColors.TextMuted)
                            }
                        }
                    }
                }
            }
            selectedTab == LibraryCategoryTab.ARTISTS -> {
                val artists = remember(allTracks) {
                    allTracks.map { it.artist }.filter { it.isNotBlank() }.distinct().sorted()
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(artists) { artist ->
                        val artistSongs = allTracks.filter { it.artist.equals(artist, ignoreCase = true) }
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedArtistForView = artist },
                            shape = RoundedCornerShape(12.dp),
                            color = StudioColors.Surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(StudioColors.SurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = StudioColors.Blue, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(artist, color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${artistSongs.size} tracks", color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StudioColors.TextMuted)
                            }
                        }
                    }
                }
            }
            selectedTab == LibraryCategoryTab.FOLDERS -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(folders) { folder ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedFolderForView = folder },
                            shape = RoundedCornerShape(12.dp),
                            color = StudioColors.Surface,
                            border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(StudioColors.SurfaceElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Folder, contentDescription = null, tint = StudioColors.Gold, modifier = Modifier.size(24.dp))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(folder.name, color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${folder.trackCount} tracks", color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StudioColors.TextMuted)
                            }
                        }
                    }
                }
            }
            selectedTab == LibraryCategoryTab.PLAYLISTS -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Button(
                        onClick = { showCreatePlaylistDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Gold, contentColor = StudioColors.Background),
                        shape = RoundedCornerShape(100.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create New Playlist", fontWeight = FontWeight.Bold)
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playlists) { playlist ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedPlaylistForView = playlist },
                                shape = RoundedCornerShape(12.dp),
                                color = StudioColors.Surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider)
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(StudioColors.SurfaceElevated),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.PlaylistPlay, contentDescription = null, tint = StudioColors.Gold, modifier = Modifier.size(24.dp))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(playlist.name, color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${playlist.trackIds.size} songs", color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = StudioColors.TextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Track to Queue Switcher
    if (trackToAddToQueue != null) {
        val targetTrack = trackToAddToQueue!!
        AlertDialog(
            onDismissRequest = { trackToAddToQueue = null },
            title = { Text("ADD TO QUEUE", color = StudioColors.Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select destination queue for '${targetTrack.title}':", color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(queues) { q ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.addTrackToQueue(q.id, targetTrack)
                                        trackToAddToQueue = null
                                    },
                                color = StudioColors.SurfaceElevated
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(q.name, color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                                    Text("${q.trackIds.size} songs", color = StudioColors.TextMuted, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { trackToAddToQueue = null }) {
                    Text("Cancel", color = StudioColors.TextMuted)
                }
            },
            containerColor = StudioColors.Surface
        )
    }

    // Modal: Create New Playlist
    if (showCreatePlaylistDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreatePlaylistDialog = false
                newPlaylistName = ""
            },
            title = { Text("CREATE PLAYLIST", color = StudioColors.Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newPlaylistName,
                    onValueChange = { newPlaylistName = it },
                    label = { Text("Playlist Name") },
                    placeholder = { Text("e.g. Master Sessions, Jazz...") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StudioColors.Gold,
                        focusedTextColor = StudioColors.TextPrimary,
                        unfocusedTextColor = StudioColors.TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            viewModel.createPlaylist(newPlaylistName)
                            showCreatePlaylistDialog = false
                            newPlaylistName = ""
                        }
                    }
                ) {
                    Text("Create", color = StudioColors.Gold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreatePlaylistDialog = false; newPlaylistName = "" }) {
                    Text("Cancel", color = StudioColors.TextMuted)
                }
            },
            containerColor = StudioColors.Surface
        )
    }

    // Modal: Add Track to Playlist Picker
    if (trackToAddToPlaylist != null) {
        val targetTrack = trackToAddToPlaylist!!
        AlertDialog(
            onDismissRequest = { trackToAddToPlaylist = null },
            title = { Text("ADD TO PLAYLIST", color = StudioColors.Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(playlists) { pl ->
                        val isAlreadyIn = pl.trackIds.contains(targetTrack.id)
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    if (!isAlreadyIn) {
                                        viewModel.addTrackToPlaylist(pl.id, targetTrack.id)
                                    }
                                    trackToAddToPlaylist = null
                                },
                            color = StudioColors.SurfaceElevated,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = pl.name, color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                                if (isAlreadyIn) {
                                    Text("Added", color = StudioColors.Success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { trackToAddToPlaylist = null }) {
                    Text("Done", color = StudioColors.Gold)
                }
            },
            containerColor = StudioColors.Surface
        )
    }

    // Modal: Track Audio Stream Inspector
    if (inspectedTrack != null) {
        val t = inspectedTrack!!
        AlertDialog(
            onDismissRequest = { inspectedTrack = null },
            title = {
                Text("AUDIO STREAM INSPECTOR", color = StudioColors.Blue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Title: ${t.title}", color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Artist: ${t.artist}", color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    Text("Album: ${t.album}", color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(color = StudioColors.Divider)
                    Text("Format: ${t.format}", color = StudioColors.Gold, style = MaterialTheme.typography.bodySmall)
                    Text("Sample Rate: ${t.sampleRate} Hz (${t.sampleRate / 1000} kHz)", color = StudioColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
                    Text("Bit Depth: ${t.bitDepth}-bit PCM", color = StudioColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
                    Text("Bitrate: ${t.bitrateKbps} kbps", color = StudioColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
                    Text("Duration: ${formatTime(t.durationMs)}", color = StudioColors.TextPrimary, style = MaterialTheme.typography.bodySmall)
                    HorizontalDivider(color = StudioColors.Divider)
                    Text("Location: ${t.uriString}", color = StudioColors.TextMuted, fontSize = 10.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            },
            confirmButton = {
                TextButton(onClick = { inspectedTrack = null }) {
                    Text("Close", color = StudioColors.Gold)
                }
            },
            containerColor = StudioColors.Surface
        )
    }
}

/**
 * Obsidian Studio Track Row Card (Image 4)
 */
@Composable
fun StudioTrackRowCard(
    track: Track,
    isPlaying: Boolean,
    isCurrent: Boolean,
    isFavorite: Boolean,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onInspectClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

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
            .clickable(onClick = onPlayClick)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album Artwork / Vinyl Thumbnail (Image 4: 48dp with 10dp radius)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioColors.SurfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PlayArrow else Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) StudioColors.Gold else StudioColors.TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            // Track Title & Subtitle ("Artist Name · FLAC") (Image 4)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    color = if (isCurrent) StudioColors.Gold else StudioColors.TextPrimary,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
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
                text = formatTime(track.durationMs),
                color = StudioColors.TextMuted,
                style = MaterialTheme.typography.bodySmall
            )

            // Play Circle Icon Button (Image 4)
            IconButton(
                onClick = onPlayClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = "Play track",
                    tint = StudioColors.Gold,
                    modifier = Modifier.size(24.dp)
                )
            }

            // More Options (3 Dots)
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = StudioColors.TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(StudioColors.Surface)
                ) {
                    DropdownMenuItem(
                        text = { Text(if (isFavorite) "Remove from Favorites" else "Add to Favorites", color = StudioColors.TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = StudioColors.Gold) },
                        onClick = {
                            onFavoriteClick()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Queue", color = StudioColors.TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.QueueMusic, contentDescription = null, tint = StudioColors.TextSecondary) },
                        onClick = {
                            onAddToQueueClick()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Add to Playlist", color = StudioColors.TextPrimary) },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = StudioColors.TextSecondary) },
                        onClick = {
                            onAddToPlaylistClick()
                            showMenu = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Audio Stream Inspector", color = StudioColors.Blue) },
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null, tint = StudioColors.Blue) },
                        onClick = {
                            onInspectClick()
                            showMenu = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FilteredTrackListView(
    title: String,
    tracks: List<Track>,
    currentTrack: Track?,
    viewModel: MainViewModel,
    onInspect: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit
) {
    val playerStatus by viewModel.playerStatus.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = { if (tracks.isNotEmpty()) viewModel.playQueue(tracks, 0) },
                colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Gold, contentColor = StudioColors.Background),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play All (${tracks.size})", fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(tracks, key = { it.id }) { track ->
                val isPlaying = currentTrack?.id == track.id && playerStatus == PlayerStatus.PLAYING
                val isCurrent = currentTrack?.id == track.id
                val isFavorite = viewModel.isFavorite(track.id)

                StudioTrackRowCard(
                    track = track,
                    isPlaying = isPlaying,
                    isCurrent = isCurrent,
                    isFavorite = isFavorite,
                    onPlayClick = { viewModel.playQueue(tracks, tracks.indexOf(track)) },
                    onFavoriteClick = { viewModel.toggleFavorite(track.id) },
                    onAddToPlaylistClick = { onAddToPlaylist(track) },
                    onAddToQueueClick = { viewModel.addTrackToQueue(viewModel.activeQueueId.value, track) },
                    onInspectClick = { onInspect(track) }
                )
            }
        }
    }
}

@Composable
fun PlaylistDetailView(
    playlist: Playlist,
    allTracks: List<Track>,
    currentTrack: Track?,
    viewModel: MainViewModel,
    onInspect: (Track) -> Unit
) {
    val trackMap = remember(allTracks) { allTracks.associateBy { it.id } }
    val playlistTracks = remember(playlist, allTracks) {
        playlist.trackIds.mapNotNull { trackMap[it] }
    }
    val playerStatus by viewModel.playerStatus.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { if (playlistTracks.isNotEmpty()) viewModel.playPlaylist(playlist, 0) },
                colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Gold, contentColor = StudioColors.Background),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Play All", fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { if (playlistTracks.isNotEmpty()) viewModel.playQueue(playlistTracks.shuffled(), 0) },
                colors = ButtonDefaults.buttonColors(containerColor = StudioColors.SurfaceElevated, contentColor = StudioColors.TextPrimary),
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Shuffle", fontWeight = FontWeight.Bold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(playlistTracks, key = { it.id }) { track ->
                val isPlaying = currentTrack?.id == track.id && playerStatus == PlayerStatus.PLAYING
                val isCurrent = currentTrack?.id == track.id
                val isFavorite = viewModel.isFavorite(track.id)

                StudioTrackRowCard(
                    track = track,
                    isPlaying = isPlaying,
                    isCurrent = isCurrent,
                    isFavorite = isFavorite,
                    onPlayClick = { viewModel.playPlaylist(playlist, playlistTracks.indexOf(track)) },
                    onFavoriteClick = { viewModel.toggleFavorite(track.id) },
                    onAddToPlaylistClick = { viewModel.addTrackToPlaylist(playlist.id, track.id) },
                    onAddToQueueClick = { viewModel.addTrackToQueue(viewModel.activeQueueId.value, track) },
                    onInspectClick = { onInspect(track) }
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
