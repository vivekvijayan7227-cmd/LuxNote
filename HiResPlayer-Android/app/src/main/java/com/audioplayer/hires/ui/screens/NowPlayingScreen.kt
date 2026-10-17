package com.audioplayer.hires.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.audioplayer.hires.model.PlayerStatus
import com.audioplayer.hires.model.StudioQueue
import com.audioplayer.hires.ui.MainViewModel
import com.audioplayer.hires.ui.StudioScreen
import com.audioplayer.hires.ui.components.FftVisualizer
import com.audioplayer.hires.ui.components.PlaybackSeekBar
import com.audioplayer.hires.ui.components.TrackArtwork
import com.audioplayer.hires.ui.theme.*

/**
 * Obsidian Studio Now Playing Screen
 * Implements Image 1 ("Midnight Echo"), Image 2 ("Space Oddity"), and Image 3 ("Five Hundred Miles").
 */
@Composable
fun NowPlayingScreen(viewModel: MainViewModel) {
    val track by viewModel.currentTrack.collectAsState()
    val status by viewModel.playerStatus.collectAsState()
    val positionMs by viewModel.positionMs.collectAsState()
    val lyrics by viewModel.lyrics.collectAsState()
    val currentLyricIndex = remember(lyrics, positionMs) {
        if (lyrics.isEmpty()) 0
        else lyrics.indexOfLast { it.timestampMs <= positionMs }.coerceAtLeast(0)
    }
    val shuffleEnabled by viewModel.shuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val sleepTimerRemainingSeconds by viewModel.sleepTimerRemainingSeconds.collectAsState()
    val isBitPerfect by viewModel.isBitPerfect.collectAsState()
    val signalPathDescription by viewModel.signalPathDescription.collectAsState()
    val fftBands by viewModel.fftBands.collectAsState()
    val queues by viewModel.queues.collectAsState()
    val activeQueueId by viewModel.activeQueueId.collectAsState()
    val playlists by viewModel.playlists.collectAsState()

    var showQueueSwitcherDialog by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    var showAddToPlaylistDialog by remember { mutableStateOf(false) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showLyricsMode by remember { mutableStateOf(false) }

    val activeQueueObj = queues.find { it.id == activeQueueId }
    val isFavorite = track?.let { viewModel.isFavorite(it.id) } ?: false

    // Smooth vinyl rotation when playing
    val infiniteTransition = rememberInfiniteTransition(label = "vinyl")
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "angle"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StudioColors.Background)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. Top Bar (Image 1, 2, 3): Back button on left, Title, Favorite Heart & Menu on right
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { viewModel.navigateBack() },
                modifier = Modifier.size(42.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = StudioColors.TextPrimary
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "NOW PLAYING",
                    color = StudioColors.TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp
                )
                if (activeQueueObj != null) {
                    Text(
                        text = activeQueueObj.name.uppercase(),
                        color = StudioColors.Gold,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.clickable { showQueueSwitcherDialog = true }
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { track?.let { viewModel.toggleFavorite(it.id) } },
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) StudioColors.Gold else StudioColors.TextPrimary
                    )
                }

                Box {
                    IconButton(
                        onClick = { showMoreMenu = true },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More Options",
                            tint = StudioColors.TextPrimary
                        )
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(StudioColors.Surface)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Playback Speed (${playbackSpeed}x)", color = StudioColors.TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Speed, contentDescription = null, tint = StudioColors.Gold) },
                            onClick = {
                                showSpeedDialog = true
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (sleepTimerRemainingSeconds > 0) "Sleep Timer (${sleepTimerRemainingSeconds / 60}m)" else "Sleep Timer", color = StudioColors.TextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = StudioColors.Gold) },
                            onClick = {
                                showSleepTimerDialog = true
                                showMoreMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Switch Playback Queue", color = StudioColors.TextPrimary) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, tint = StudioColors.Blue) },
                            onClick = {
                                showQueueSwitcherDialog = true
                                showMoreMenu = false
                            }
                        )
                    }
                }
            }
        }

        // 2. Center Stage (Image 1, 2, 3): Vinyl Disc / Artwork OR Synchronized Lyrics View
        if (!showLyricsMode) {
            Surface(
                modifier = Modifier
                    .sizeIn(minWidth = 200.dp, maxWidth = 260.dp, minHeight = 200.dp, maxHeight = 260.dp)
                    .aspectRatio(1f)
                    .shadow(16.dp, RoundedCornerShape(20.dp), spotColor = StudioColors.Blue.copy(alpha = 0.2f))
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, StudioColors.Divider, RoundedCornerShape(20.dp))
                    .clickable { showLyricsMode = true },
                color = StudioColors.SurfaceElevated
            ) {
                TrackArtwork(
                    artworkUri = track?.artworkUri,
                    trackUri = track?.uriString,
                    contentDescription = "Album art for ${track?.title}",
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Stylized Vinyl Record Fallback
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        StudioColors.SurfaceElevated,
                                        StudioColors.Surface,
                                        StudioColors.Background
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(210.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF202738),
                                            Color(0xFF111622),
                                            Color(0xFF07090E)
                                        )
                                    )
                                )
                                .border(1.dp, StudioColors.Divider, CircleShape)
                                .rotate(if (status == PlayerStatus.PLAYING) angle else 0f),
                            contentAlignment = Alignment.Center
                        ) {
                            // Concentric vinyl grooves
                            Box(modifier = Modifier.size(170.dp).border(0.8.dp, Color(0xFF2A3448).copy(alpha = 0.5f), CircleShape))
                            Box(modifier = Modifier.size(130.dp).border(0.8.dp, Color(0xFF2A3448).copy(alpha = 0.4f), CircleShape))
                            Box(modifier = Modifier.size(90.dp).border(0.8.dp, Color(0xFF2A3448).copy(alpha = 0.3f), CircleShape))

                            // Center Gold Acoustic Hub
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(StudioColors.GoldHighlight, StudioColors.Gold)
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("HI-RES", color = StudioColors.Background, fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    Text(track?.format ?: "FLAC", color = StudioColors.Background, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Synchronized Lyrics View with tap-to-seek (Image 1, Image 3)
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showLyricsMode = false },
                color = StudioColors.Surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider)
            ) {
                if (lyrics.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.MicNone, contentDescription = null, tint = StudioColors.TextMuted, modifier = Modifier.size(36.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No synchronized lyrics found", color = StudioColors.TextMuted, fontSize = 13.sp)
                            Text("Tap to return to album art", color = StudioColors.Gold, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp))
                        }
                    }
                } else {
                    val listState = rememberLazyListState()
                    LaunchedEffect(currentLyricIndex) {
                        if (currentLyricIndex in lyrics.indices) {
                            listState.animateScrollToItem((currentLyricIndex - 1).coerceAtLeast(0))
                        }
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(lyrics.indices.toList()) { i ->
                            val line = lyrics[i]
                            val isCurrent = i == currentLyricIndex
                            Text(
                                text = line.text,
                                color = if (isCurrent) StudioColors.Gold else StudioColors.TextMuted,
                                fontSize = if (isCurrent) 17.sp else 13.sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.seekTo(line.timestampMs) }
                            )
                        }
                    }
                }
            }
        }

        // 3. Track Details: Title, Artist, and Format Pill (Image 1)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
        ) {
            Text(
                text = track?.title ?: "No Track Selected",
                style = MaterialTheme.typography.headlineMedium,
                color = StudioColors.TextPrimary,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = track?.artist ?: "Unknown Artist",
                    style = MaterialTheme.typography.bodyLarge,
                    color = StudioColors.TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Format Pill
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = StudioColors.SurfaceElevated,
                    border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Divider)
                ) {
                    Text(
                        text = "${track?.format ?: "FLAC"} · $signalPathDescription",
                        color = if (isBitPerfect) StudioColors.Success else StudioColors.Blue,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }

        // 4. Live Audio Waveform & Precision Seek Bar (Image 1, 2)
        Column(modifier = Modifier.fillMaxWidth()) {
            // Live Waveform Visualizer
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                FftVisualizer(
                    bands = fftBands,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Seek Bar
            val durationMs = track?.durationMs ?: 0L
            PlaybackSeekBar(
                positionMs = positionMs,
                durationMs = durationMs,
                onSeek = { viewModel.seekTo(it) }
            )
        }

        // 5. Primary Transport Controls (Image 1, 2): Shuffle, Prev, 72dp Gold Play, Next, Repeat
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Shuffle
            IconButton(
                onClick = { viewModel.toggleShuffle() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Shuffle,
                    contentDescription = if (shuffleEnabled) "Shuffle: Enabled" else "Shuffle: Disabled",
                    tint = if (shuffleEnabled) StudioColors.Blue else StudioColors.TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Previous
            IconButton(
                onClick = { viewModel.playPrevious() },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Track",
                    tint = StudioColors.TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Prominent 72dp Champagne Gold Play/Pause Button
            Surface(
                shape = CircleShape,
                color = StudioColors.Gold,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .clickable { viewModel.togglePlayPause() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (status == PlayerStatus.PLAYING) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (status == PlayerStatus.PLAYING) "Pause playback" else "Start playback",
                        tint = StudioColors.Background,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Next
            IconButton(
                onClick = { viewModel.playNext() },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Track",
                    tint = StudioColors.TextPrimary,
                    modifier = Modifier.size(34.dp)
                )
            }

            // Repeat
            IconButton(
                onClick = { viewModel.toggleRepeatMode() },
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Repeat,
                    contentDescription = "Repeat mode: ${repeatMode.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    tint = if (repeatMode != com.audioplayer.hires.model.RepeatMode.OFF) StudioColors.Blue else StudioColors.TextMuted,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 6. Bottom Action Pills (Image 1): Add to Playlist, Lyrics, EQ Studio, Queue
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Queue Pill
            ActionPillButton(
                icon = Icons.AutoMirrored.Filled.QueueMusic,
                label = "Queue",
                onClick = { showQueueSwitcherDialog = true }
            )

            // Lyrics Pill
            ActionPillButton(
                icon = Icons.Default.Lyrics,
                label = "Lyrics",
                isActive = showLyricsMode,
                onClick = { showLyricsMode = !showLyricsMode }
            )

            // EQ Studio Pill (Image 1)
            ActionPillButton(
                icon = Icons.Default.GraphicEq,
                label = "EQ Studio",
                onClick = { viewModel.setScreen(StudioScreen.EQUALIZER) }
            )

            // Add to Playlist Pill
            ActionPillButton(
                icon = Icons.Default.PlaylistAdd,
                label = "Add to Playlist",
                onClick = { if (track != null) showAddToPlaylistDialog = true }
            )
        }
    }

    // Modal: Multi-Queue Switcher Dialog
    if (showQueueSwitcherDialog) {
        AlertDialog(
            onDismissRequest = { showQueueSwitcherDialog = false },
            title = {
                Text(
                    text = "INDEPENDENT PLAYBACK QUEUES",
                    color = StudioColors.Gold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Independent queues preserve their own tracklists, shuffle modes, and playback positions.",
                        color = StudioColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(queues) { q ->
                            val isSelected = q.id == activeQueueId
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        viewModel.switchQueue(q.id)
                                        showQueueSwitcherDialog = false
                                    },
                                color = if (isSelected) StudioColors.SurfaceSelected else StudioColors.SurfaceElevated,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) StudioColors.Gold else StudioColors.Divider
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(q.name, color = if (isSelected) StudioColors.Gold else StudioColors.TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                        Text("${q.trackIds.size} songs", color = StudioColors.TextSecondary, style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Active", tint = StudioColors.Gold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQueueSwitcherDialog = false }) {
                    Text("Close", color = StudioColors.Gold)
                }
            },
            containerColor = StudioColors.Surface
        )
    }

    // Modal: Playback Speed Selector
    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text("PLAYBACK SPEED", color = StudioColors.Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                        val isSelected = playbackSpeed == speed
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setPlaybackSpeed(speed)
                                    showSpeedDialog = false
                                },
                            color = if (isSelected) StudioColors.SurfaceSelected else StudioColors.SurfaceElevated
                        ) {
                            Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${speed}x", color = if (isSelected) StudioColors.Gold else StudioColors.TextPrimary, fontWeight = FontWeight.Bold)
                                if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = StudioColors.Gold)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialog = false }) { Text("Cancel", color = StudioColors.TextMuted) }
            },
            containerColor = StudioColors.Surface
        )
    }

    // Modal: Sleep Timer Selector
    if (showSleepTimerDialog) {
        AlertDialog(
            onDismissRequest = { showSleepTimerDialog = false },
            title = { Text("STUDIO SLEEP TIMER", color = StudioColors.Gold, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(15, 30, 45, 60, 90).forEach { mins ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    viewModel.setSleepTimer(mins)
                                    showSleepTimerDialog = false
                                },
                            color = StudioColors.SurfaceElevated
                        ) {
                            Text("$mins Minutes", color = StudioColors.TextPrimary, modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                        }
                    }
                    if (sleepTimerRemainingSeconds > 0) {
                        Button(
                            onClick = {
                                viewModel.cancelSleepTimer()
                                showSleepTimerDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StudioColors.Error),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) {
                            Text("Cancel Active Timer")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSleepTimerDialog = false }) { Text("Cancel", color = StudioColors.TextMuted) }
            },
            containerColor = StudioColors.Surface
        )
    }

    // Modal: Add Current Track to Playlist
    if (showAddToPlaylistDialog && track != null) {
        val currentT = track!!
        AlertDialog(
            onDismissRequest = { showAddToPlaylistDialog = false },
            title = {
                Text(
                    text = "ADD TO PLAYLIST",
                    color = StudioColors.Gold,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose playlist for '${currentT.title}':",
                        color = StudioColors.TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            showAddToPlaylistDialog = false
                            showCreatePlaylistDialog = true
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StudioColors.SurfaceElevated,
                            contentColor = StudioColors.Gold
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, StudioColors.Gold.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create New Playlist", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                        items(playlists) { pl ->
                            val isAlreadyIn = pl.trackIds.contains(currentT.id)
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        if (!isAlreadyIn) {
                                            viewModel.addTrackToPlaylist(pl.id, currentT.id)
                                        }
                                        showAddToPlaylistDialog = false
                                    },
                                color = StudioColors.SurfaceElevated
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(pl.name, color = StudioColors.TextPrimary, style = MaterialTheme.typography.titleMedium)
                                    if (isAlreadyIn) {
                                        Text("Added", color = StudioColors.Success, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddToPlaylistDialog = false }) {
                    Text("Cancel", color = StudioColors.TextMuted)
                }
            },
            containerColor = StudioColors.Surface
        )
    }

    // Modal: Create New Playlist from Now Playing
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
                        val name = newPlaylistName.trim()
                        if (name.isNotBlank()) {
                            viewModel.createPlaylist(name)
                            if (track != null) {
                                val latestPl = viewModel.playlists.value.find { it.name == name }
                                latestPl?.let { viewModel.addTrackToPlaylist(it.id, track!!.id) }
                            }
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
}

@Composable
private fun ActionPillButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(100.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(100.dp),
        color = if (isActive) StudioColors.SurfaceSelected else StudioColors.Surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isActive) StudioColors.Gold else StudioColors.Divider
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isActive) StudioColors.Gold else StudioColors.TextSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = if (isActive) StudioColors.Gold else StudioColors.TextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
