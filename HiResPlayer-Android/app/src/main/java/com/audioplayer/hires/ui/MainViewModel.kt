package com.audioplayer.hires.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audioplayer.hires.audio.AudioTrackPlayer
import com.audioplayer.hires.audio.BiquadType
import com.audioplayer.hires.audio.CenterChannelProcessor
import com.audioplayer.hires.audio.StudioDspPipeline
import com.audioplayer.hires.audio.UsbAudioInspector
import com.audioplayer.hires.audio.UsbDacInfo
import com.audioplayer.hires.data.AudioLibraryRepository
import com.audioplayer.hires.data.DiagnosticsManager
import com.audioplayer.hires.data.DiagnosticsReport
import com.audioplayer.hires.data.ScanResult
import com.audioplayer.hires.lyrics.LrcParser
import com.audioplayer.hires.model.*
import com.audioplayer.hires.playback.HiResAudioService
import com.audioplayer.hires.playback.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Stack
import java.util.UUID

enum class StudioScreen {
    NOW_PLAYING,
    LIBRARY,
    QUEUES,
    FAVORITES,
    SETTINGS,
    EQUALIZER,
    VOCAL_STUDIO,
    DEVICE_INSPECTOR
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val repository = AudioLibraryRepository(application)
    val playbackManager = PlaybackManager.getInstance(application)
    val dspPipeline get() = playbackManager.dspPipeline
    val audioPlayer get() = playbackManager.audioPlayer
    val usbInspector = UsbAudioInspector(application)

    // Navigation Stack
    private val screenStack = Stack<StudioScreen>()
    private val _currentScreen = MutableStateFlow(StudioScreen.LIBRARY)
    val currentScreen: StateFlow<StudioScreen> = _currentScreen.asStateFlow()

    // Import / Scan Progress State (prevents UI freeze & blackouts)
    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    private val _importProgress = MutableStateFlow("")
    val importProgress: StateFlow<String> = _importProgress.asStateFlow()

    // Library, Folders & Playlists State
    private val _libraryTracks = MutableStateFlow<List<Track>>(emptyList())
    val libraryTracks: StateFlow<List<Track>> = _libraryTracks.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _folders = MutableStateFlow<List<MusicFolder>>(emptyList())
    val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()

    // Studio Multiple Independent Queues
    private val _queues = MutableStateFlow<List<StudioQueue>>(emptyList())
    val queues: StateFlow<List<StudioQueue>> = _queues.asStateFlow()

    val activeQueueId: StateFlow<String> get() = playbackManager.activeQueueId
    val activeQueue: StateFlow<List<Track>> get() = playbackManager.activeQueue

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOption = MutableStateFlow(SortOption.TITLE_ASC)
    val sortOption: StateFlow<SortOption> = _sortOption.asStateFlow()
    val currentSort: StateFlow<SortOption> get() = sortOption

    // Synchronized Lyrics Engine
    private val _currentLyrics = MutableStateFlow<List<LyricLine>>(emptyList())
    val currentLyrics: StateFlow<List<LyricLine>> = _currentLyrics.asStateFlow()
    val lyrics: StateFlow<List<LyricLine>> get() = currentLyrics

    // Listening Bookmarks & Notes
    private val _bookmarks = MutableStateFlow<List<TrackBookmark>>(emptyList())
    val bookmarks: StateFlow<List<TrackBookmark>> = _bookmarks.asStateFlow()

    // Audiophile Sleep Timer
    private val _sleepTimerRemainingSec = MutableStateFlow(0)
    val sleepTimerRemainingSec: StateFlow<Int> = _sleepTimerRemainingSec.asStateFlow()
    val sleepTimerRemainingSeconds: StateFlow<Int> get() = sleepTimerRemainingSec

    private val _sleepTimerEndOfTrack = MutableStateFlow(false)
    val sleepTimerEndOfTrack: StateFlow<Boolean> = _sleepTimerEndOfTrack.asStateFlow()
    private var sleepTimerJob: Job? = null

    // Playback Speed & Transport
    val playbackSpeed: StateFlow<Float> get() = playbackManager.playbackSpeed
    val shuffleEnabled: StateFlow<Boolean> get() = playbackManager.shuffleEnabled
    val repeatMode: StateFlow<RepeatMode> get() = playbackManager.repeatMode

    // Playback State
    val currentTrack: StateFlow<Track?> get() = playbackManager.currentTrack
    val playerStatus: StateFlow<PlayerStatus> get() = playbackManager.playerStatus
    val positionMs: StateFlow<Long> get() = playbackManager.positionMs

    // Hardware USB DAC & Bit-Perfect Info
    private val _usbDacInfo = MutableStateFlow(usbInspector.inspectAudioHardware())
    val usbDacInfo: StateFlow<UsbDacInfo> = _usbDacInfo.asStateFlow()

    // DSP States & Truthful Signal Path (Finding 16)
    val dspEnabled: StateFlow<Boolean> get() = playbackManager.dspEnabled
    val isBitPerfect: StateFlow<Boolean> get() = playbackManager.isBitPerfect
    val signalPathDescription: StateFlow<String> get() = playbackManager.signalPathDescription
    val preampGainDb: StateFlow<Float> get() = playbackManager.preampGainDb
    val hasClipped: StateFlow<Boolean> get() = playbackManager.hasClipped
    val graphicGains: StateFlow<FloatArray> get() = playbackManager.graphicGains
    val vocalFocusMode: StateFlow<Int> get() = playbackManager.vocalFocusMode
    val vocalFocusAmount: StateFlow<Float> get() = playbackManager.vocalFocusAmount

    // Settings (Finding 10)
    val isGaplessEnabled: StateFlow<Boolean> get() = playbackManager.isGaplessEnabled
    val isRestoreSessionEnabled: StateFlow<Boolean> get() = playbackManager.isRestoreSessionEnabled
    val isResumeAfterInterruptionEnabled: StateFlow<Boolean> get() = playbackManager.isResumeAfterInterruptionEnabled
    val isAutoResumeEnabled: StateFlow<Boolean> get() = playbackManager.isAutoResumeEnabled
    val playbackError: StateFlow<AudioTrackPlayer.PlaybackError?> get() = playbackManager.playbackError

    // Real-Time Spectrum Visualizer (64 bands)
    private val _fftBands = MutableStateFlow(FloatArray(64) { 0.05f })
    val fftBands: StateFlow<FloatArray> = _fftBands.asStateFlow()

    init {
        screenStack.push(StudioScreen.LIBRARY)

        viewModelScope.launch {
            playbackManager.currentTrack.collect { track ->
                loadLyricsForTrack(track)
            }
        }

        // Asynchronously load library, playlists, multi-queues, and bookmarks
        viewModelScope.launch(Dispatchers.IO) {
            val savedTracks = repository.loadLibrary()
            val savedPlaylists = repository.loadPlaylists()
            val savedBookmarks = repository.loadBookmarks()

            withContext(Dispatchers.Main) {
                _playlists.value = savedPlaylists
                _bookmarks.value = savedBookmarks
            }

            val initialTracks = savedTracks
            val loadedQueues = repository.loadQueues(initialTracks)
            val firstQ = loadedQueues.firstOrNull() ?: StudioQueue("queue_1", "Queue 1 (Main)", initialTracks.map { it.id })
            val trackMap = initialTracks.associateBy { it.id }
            val qTracks = firstQ.trackIds.mapNotNull { trackMap[it] }.ifEmpty { initialTracks }
            val initialIndex = if (qTracks.isNotEmpty()) firstQ.currentIndex.coerceIn(0, qTracks.size - 1) else 0

            withContext(Dispatchers.Main) {
                _libraryTracks.value = initialTracks
                _folders.value = repository.extractFolders(initialTracks)
                _queues.value = loadedQueues
                if (playbackManager.activeQueue.value.isEmpty()) {
                    if (playbackManager.isRestoreSessionEnabled.value && qTracks.isNotEmpty()) {
                        val restoredTrack = qTracks[initialIndex]
                        playbackManager.restoreSessionState(
                            queueId = firstQ.id,
                            tracks = qTracks,
                            index = initialIndex,
                            track = restoredTrack,
                            positionMs = firstQ.positionMs
                        )
                    } else {
                        playbackManager.setActiveQueue(firstQ.id, qTracks, initialIndex)
                    }
                }
                playbackManager.onVerifiedMetadataUpdated = { verified ->
                    val currentList = _libraryTracks.value
                    val idx = currentList.indexOfFirst { it.id == verified.id }
                    if (idx >= 0) {
                        val updated = currentList.toMutableList()
                        updated[idx] = verified
                        _libraryTracks.value = updated
                        viewModelScope.launch(Dispatchers.IO) {
                            repository.saveLibraryAsync(updated)
                        }
                    }
                }
                registerRealTimeObserver()
                scanDeviceMusic(showIndicator = false)
                refreshDiagnostics()
            }
        }

        // Dynamic 60 FPS Spectrum Polling Loop
        viewModelScope.launch {
            while (isActive) {
                if (playbackManager.playerStatus.value == PlayerStatus.PLAYING) {
                    _fftBands.value = dspPipeline.spectrumAnalyzer.computeBands()
                    playbackManager.pollSpectrumAndClipping()
                } else {
                    val current = _fftBands.value
                    var hasEnergy = false
                    for (i in current.indices) {
                        current[i] = (current[i] * 0.88f).coerceAtLeast(0.01f)
                        if (current[i] > 0.02f) hasEnergy = true
                    }
                    if (hasEnergy) _fftBands.value = current.clone()
                }
                delay(16) // ~60 FPS
            }
        }
    }

    fun getNativeEngineDetails(): String = dspPipeline.getEngineDetails()

    fun refreshUsbHardware() {
        viewModelScope.launch(Dispatchers.IO) {
            val info = usbInspector.inspectAudioHardware()
            withContext(Dispatchers.Main) {
                _usbDacInfo.value = info
            }
        }
    }

    val diagnosticsManager = DiagnosticsManager(getApplication())
    private val _diagnosticsReport = MutableStateFlow<DiagnosticsReport?>(null)
    val diagnosticsReport: StateFlow<DiagnosticsReport?> = _diagnosticsReport.asStateFlow()

    fun refreshDiagnostics() {
        viewModelScope.launch(Dispatchers.IO) {
            val report = diagnosticsManager.runFullDiagnostics()
            withContext(Dispatchers.Main) {
                _diagnosticsReport.value = report
            }
        }
    }

    fun probePlayback(track: Track, onResult: (String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = diagnosticsManager.runPlaybackSelfTest(track)
            withContext(Dispatchers.Main) {
                onResult(res)
            }
        }
    }

    private fun loadLyricsForTrack(track: Track?) {
        if (track == null) {
            _currentLyrics.value = emptyList()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val lyrics = LrcParser.findLyricsForTrack(getApplication(), track)
            withContext(Dispatchers.Main) {
                _currentLyrics.value = lyrics
            }
        }
    }

    // Navigation
    fun setScreen(screen: StudioScreen) {
        if (_currentScreen.value != screen) {
            screenStack.push(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (screenStack.size > 1) {
            screenStack.pop()
            _currentScreen.value = screenStack.peek()
            return true
        } else if (_currentScreen.value != StudioScreen.LIBRARY) {
            _currentScreen.value = StudioScreen.LIBRARY
            screenStack.clear()
            screenStack.push(StudioScreen.LIBRARY)
            return true
        }
        return false
    }

    // Studio Multiple Queues Management
    fun switchQueue(queueId: String) {
        if (activeQueueId.value == queueId) return

        val updatedQueues = _queues.value.map { q ->
            if (q.id == activeQueueId.value) {
                q.copy(currentIndex = playbackManager.currentQueueIndex.value, positionMs = positionMs.value, isShuffle = shuffleEnabled.value)
            } else q
        }

        val target = updatedQueues.find { it.id == queueId } ?: return
        _queues.value = updatedQueues
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQueuesAsync(updatedQueues)
        }

        val trackMap = _libraryTracks.value.associateBy { it.id }
        val targetTracks = target.trackIds.mapNotNull { trackMap[it] }
        val targetIndex = if (targetTracks.isNotEmpty()) target.currentIndex.coerceIn(0, targetTracks.size - 1) else 0

        playbackManager.setActiveQueue(queueId, targetTracks, targetIndex)
        playbackManager.setShuffle(target.isShuffle)

        if (targetTracks.isNotEmpty()) {
            val trackToPlay = targetTracks[targetIndex]
            playbackManager.playTrack(trackToPlay, targetTracks, targetIndex)
            if (target.positionMs > 0) {
                playbackManager.seekTo(target.positionMs)
            }
        }
    }

    fun createQueue(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val newQueue = StudioQueue(
            id = "queue_${UUID.randomUUID()}",
            name = trimmed,
            trackIds = emptyList()
        )
        val updated = _queues.value + newQueue
        _queues.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQueuesAsync(updated)
        }
    }

    fun renameQueue(queueId: String, newName: String) {
        val updated = _queues.value.map { if (it.id == queueId) it.copy(name = newName) else it }
        _queues.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQueuesAsync(updated)
        }
    }

    fun clearQueue(queueId: String) {
        val updated = _queues.value.map { if (it.id == queueId) it.copy(trackIds = emptyList(), currentIndex = 0, positionMs = 0L) else it }
        _queues.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQueuesAsync(updated)
        }
        if (activeQueueId.value == queueId) {
            playbackManager.setActiveQueue(queueId, emptyList(), 0)
            stop()
        }
    }

    fun addTrackToQueue(queueId: String, track: Track) {
        val updated = _queues.value.map { q ->
            if (q.id == queueId) {
                q.copy(trackIds = q.trackIds + track.id)
            } else q
        }
        _queues.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQueuesAsync(updated)
        }
        if (activeQueueId.value == queueId) {
            playbackManager.setActiveQueue(queueId, activeQueue.value + track, playbackManager.currentQueueIndex.value)
        }
    }

    fun addTrackNextInQueue(track: Track) {
        val current = activeQueue.value.toMutableList()
        val insertIdx = (playbackManager.currentQueueIndex.value + 1).coerceAtMost(current.size)
        current.add(insertIdx, track)
        playbackManager.setActiveQueue(activeQueueId.value, current, playbackManager.currentQueueIndex.value)

        val updated = _queues.value.map { q ->
            if (q.id == activeQueueId.value) {
                val list = q.trackIds.toMutableList()
                val idx = (q.currentIndex + 1).coerceAtMost(list.size)
                list.add(idx, track.id)
                q.copy(trackIds = list)
            } else q
        }
        _queues.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQueuesAsync(updated)
        }
    }

    // Physical Folder Browser
    fun playFolder(folder: MusicFolder) {
        val tracksInFolder = _libraryTracks.value.filter { track ->
            val path = if (track.uriString.startsWith("file://")) Uri.parse(track.uriString).path ?: "" else track.uriString
            path.startsWith(folder.path) || folder.path.contains(track.album)
        }
        if (tracksInFolder.isNotEmpty()) {
            playQueue(tracksInFolder, 0)
        }
    }

    // High-Speed Non-Blocking Real-Time Scanning & Differential Reconciliation (Finding 09)
    fun scanDeviceMusic(showIndicator: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (showIndicator) {
                withContext(Dispatchers.Main) {
                    _isImporting.value = true
                    _importProgress.value = "Scanning local storage for audio files..."
                }
            }

            val scanResult = repository.scanMediaStoreWithResultAsync()
            val currentTracks = _libraryTracks.value
            val context = getApplication<Application>()

            // If scan encountered an explicit error, DO NOT delete tracks or modify playlists
            if (scanResult is ScanResult.Failure) {
                android.util.Log.w("MainViewModel", "Scan failed: ${scanResult.error}. Retaining existing library to prevent data loss.")
                if (showIndicator) {
                    withContext(Dispatchers.Main) {
                        _isImporting.value = false
                        _importProgress.value = ""
                    }
                }
                return@launch
            }

            val discoveredTracks = when (scanResult) {
                is ScanResult.Success -> scanResult.tracks
                is ScanResult.Partial -> scanResult.tracks
                is ScanResult.Failure -> emptyList()
            }

            // Fallback to direct physical storage if MediaStore returned empty
            val finalDiscovered = if (discoveredTracks.isEmpty()) {
                repository.scanDirectStorageFoldersAsync()
            } else {
                discoveredTracks
            }

            // If both returned 0 but we previously had tracks, DO NOT wipe user's library
            if (finalDiscovered.isEmpty() && currentTracks.isNotEmpty()) {
                android.util.Log.w("MainViewModel", "Scan returned 0 tracks while library has ${currentTracks.size} tracks. Retaining library snapshot.")
                if (showIndicator) {
                    withContext(Dispatchers.Main) {
                        _isImporting.value = false
                        _importProgress.value = ""
                    }
                }
                return@launch
            }

            val discoveredMap = finalDiscovered.associateBy { it.id }
            val reconciledTracks = mutableListOf<Track>()
            val processedIds = mutableSetOf<Long>()

            // 1. Reconcile existing tracks: mark missing as isAvailable = false rather than deleting
            for (t in currentTracks) {
                processedIds.add(t.id)
                val isMediaStore = t.uriString.startsWith("content://media/")
                if (isMediaStore) {
                    val fresh = discoveredMap[t.id]
                    if (fresh != null) {
                        // Preserve verified metadata if already collected
                        val preserved = if (t.isMetadataVerified && !fresh.isMetadataVerified) {
                            fresh.copy(
                                sampleRate = t.sampleRate,
                                bitDepth = t.bitDepth,
                                bitrateKbps = t.bitrateKbps,
                                isMetadataVerified = true,
                                isAvailable = true
                            )
                        } else {
                            fresh.copy(isAvailable = true)
                        }
                        reconciledTracks.add(preserved)
                    } else {
                        // Mark unavailable rather than destructive deletion
                        reconciledTracks.add(t.copy(isAvailable = false))
                    }
                } else {
                    // Check SAF / direct file accessibility
                    val accessible = try {
                        val uri = Uri.parse(t.uriString)
                        if (uri.scheme == "file") {
                            java.io.File(uri.path ?: "").exists()
                        } else {
                            context.contentResolver.openFileDescriptor(uri, "r")?.use { true } ?: false
                        }
                    } catch (_: Exception) {
                        false
                    }
                    reconciledTracks.add(t.copy(isAvailable = accessible))
                }
            }

            // 2. Add newly discovered tracks
            for (dt in finalDiscovered) {
                if (dt.id !in processedIds) {
                    reconciledTracks.add(dt)
                    processedIds.add(dt.id)
                }
            }

            repository.saveLibraryAsync(reconciledTracks)

            withContext(Dispatchers.Main) {
                _libraryTracks.value = reconciledTracks
                _folders.value = repository.extractFolders(reconciledTracks)
                if (playbackManager.activeQueue.value.isEmpty() && reconciledTracks.isNotEmpty()) {
                    playbackManager.setActiveQueue("queue_1", reconciledTracks.filter { it.isAvailable }, 0)
                }
                if (playbackManager.currentTrack.value == null && reconciledTracks.isNotEmpty()) {
                    loadLyricsForTrack(reconciledTracks[0])
                }
                if (showIndicator) {
                    _isImporting.value = false
                    _importProgress.value = ""
                }
            }
        }
    }

    fun registerRealTimeObserver() {
        repository.registerMediaStoreObserver {
            scanDeviceMusic(showIndicator = false)
        }
    }

    fun importAudioUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val newTracks = mutableListOf<Track>()
            for (uri in uris) {
                try {
                    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                    getApplication<Application>().contentResolver.takePersistableUriPermission(uri, flags)
                } catch (_: Exception) {}
                val track = repository.extractTrackFromUriAsync(uri)
                newTracks.add(track)
            }

            if (newTracks.isNotEmpty()) {
                val currentList = _libraryTracks.value
                val existingIds = currentList.map { it.id }.toSet()
                val merged = currentList.toMutableList()
                for (t in newTracks) {
                    if (t.id !in existingIds) {
                        merged.add(t)
                    }
                }
                repository.saveLibraryAsync(merged)

                withContext(Dispatchers.Main) {
                    _libraryTracks.value = merged
                    _folders.value = repository.extractFolders(merged)
                    if (playbackManager.activeQueue.value.isEmpty()) {
                        playbackManager.setActiveQueue("queue_1", merged, 0)
                    }
                    if (playbackManager.currentTrack.value == null && newTracks.isNotEmpty()) {
                        loadLyricsForTrack(newTracks[0])
                    }
                }
            }
        }
    }

    fun importUris(uris: List<Uri>) = importAudioUris(uris)

    fun deleteTrack(trackId: Long) {
        val updated = _libraryTracks.value.filter { it.id != trackId }
        _libraryTracks.value = updated
        _folders.value = repository.extractFolders(updated)

        viewModelScope.launch(Dispatchers.IO) {
            repository.saveLibraryAsync(updated)
        }

        val updatedPl = _playlists.value.map { pl ->
            pl.copy(trackIds = pl.trackIds.filter { it != trackId })
        }
        _playlists.value = updatedPl
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePlaylistsAsync(updatedPl)
        }

        val updatedQ = _queues.value.map { q ->
            q.copy(trackIds = q.trackIds.filter { it != trackId })
        }
        _queues.value = updatedQ
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQueuesAsync(updatedQ)
        }

        if (playbackManager.activeQueue.value.any { it.id == trackId }) {
            val q = playbackManager.activeQueue.value.filter { it.id != trackId }
            playbackManager.setActiveQueue(playbackManager.activeQueueId.value, q, playbackManager.currentQueueIndex.value)
        }

        if (currentTrack.value?.id == trackId) {
            stop()
        }
    }

    // Search and Sort
    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOption(option: SortOption) {
        _sortOption.value = option
    }

    fun getFilteredTracks(): List<Track> {
        val q = _searchQuery.value.trim().lowercase()
        val list = if (q.isBlank()) {
            _libraryTracks.value
        } else {
            _libraryTracks.value.filter {
                it.title.lowercase().contains(q) ||
                it.artist.lowercase().contains(q) ||
                it.album.lowercase().contains(q)
            }
        }

        return when (_sortOption.value) {
            SortOption.TITLE_ASC -> list.sortedBy { it.title.lowercase() }
            SortOption.ARTIST_ASC -> list.sortedBy { it.artist.lowercase() }
            SortOption.ALBUM_ASC -> list.sortedBy { it.album.lowercase() }
            SortOption.DURATION_DESC -> list.sortedByDescending { it.durationMs }
            SortOption.DATE_ADDED -> list.reversed()
        }
    }

    fun getFilteredAndSortedTracks(): List<Track> = getFilteredTracks()

    fun getFilteredAlbums(): List<String> {
        val q = _searchQuery.value.trim().lowercase()
        val all = _libraryTracks.value.map { it.album }.filter { it.isNotBlank() }.distinct().sorted()
        return if (q.isBlank()) all else all.filter { it.lowercase().contains(q) }
    }

    fun getFilteredArtists(): List<String> {
        val q = _searchQuery.value.trim().lowercase()
        val all = _libraryTracks.value.map { it.artist }.filter { it.isNotBlank() }.distinct().sorted()
        return if (q.isBlank()) all else all.filter { it.lowercase().contains(q) }
    }

    fun getFilteredFolders(): List<MusicFolder> {
        val q = _searchQuery.value.trim().lowercase()
        val all = _folders.value
        return if (q.isBlank()) all else all.filter { it.name.lowercase().contains(q) || it.path.lowercase().contains(q) }
    }

    fun getFilteredPlaylists(): List<Playlist> {
        val q = _searchQuery.value.trim().lowercase()
        val all = _playlists.value
        return if (q.isBlank()) all else all.filter { it.name.lowercase().contains(q) }
    }

    // Playlists Management
    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val newPl = Playlist(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            trackIds = emptyList()
        )
        val updated = _playlists.value + newPl
        _playlists.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePlaylistsAsync(updated)
        }
    }

    fun deletePlaylist(playlistId: String) {
        if (playlistId == "favorites") return
        val updated = _playlists.value.filter { it.id != playlistId }
        _playlists.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePlaylistsAsync(updated)
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: Long) {
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                if (trackId !in pl.trackIds) {
                    pl.copy(trackIds = pl.trackIds + trackId)
                } else pl
            } else pl
        }
        _playlists.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePlaylistsAsync(updated)
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: Long) {
        val updated = _playlists.value.map { pl ->
            if (pl.id == playlistId) {
                pl.copy(trackIds = pl.trackIds.filter { it != trackId })
            } else pl
        }
        _playlists.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.savePlaylistsAsync(updated)
        }
    }

    fun toggleFavorite(trackId: Long) {
        val fav = _playlists.value.find { it.id == "favorites" }
        if (fav != null) {
            if (trackId in fav.trackIds) {
                removeTrackFromPlaylist("favorites", trackId)
            } else {
                addTrackToPlaylist("favorites", trackId)
            }
        } else {
            createPlaylist("Favorites")
            addTrackToPlaylist("favorites", trackId)
        }
    }

    fun isFavorite(trackId: Long): Boolean {
        val fav = _playlists.value.find { it.id == "favorites" } ?: return false
        return trackId in fav.trackIds
    }

    fun getFavoriteTracks(): List<Track> {
        val fav = _playlists.value.find { it.id == "favorites" } ?: return emptyList()
        val trackMap = _libraryTracks.value.associateBy { it.id }
        return fav.trackIds.mapNotNull { trackMap[it] }
    }

    fun playFavorites(shuffle: Boolean = false) {
        val favTracks = getFavoriteTracks()
        if (favTracks.isNotEmpty()) {
            if (shuffle) {
                playQueue(favTracks.shuffled(), 0)
            } else {
                playQueue(favTracks, 0)
            }
        }
    }

    fun playPlaylist(playlist: Playlist, startIndex: Int = 0) {
        val trackMap = _libraryTracks.value.associateBy { it.id }
        val plTracks = playlist.trackIds.mapNotNull { trackMap[it] }
        if (plTracks.isNotEmpty()) {
            playQueue(plTracks, startIndex)
        }
    }

    // Audio Bookmarks and Notes
    fun addBookmark(trackId: Long, timestampMs: Long, note: String) {
        val newBookmark = TrackBookmark(
            id = UUID.randomUUID().toString(),
            trackId = trackId,
            timestampMs = timestampMs,
            note = note.ifBlank { "Bookmark at ${formatTime(timestampMs)}" }
        )
        val updated = _bookmarks.value + newBookmark
        _bookmarks.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveBookmarksAsync(updated)
        }
    }

    fun deleteBookmark(bookmarkId: String) {
        val updated = _bookmarks.value.filter { it.id != bookmarkId }
        _bookmarks.value = updated
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveBookmarksAsync(updated)
        }
    }

    fun getBookmarksForCurrentTrack(): List<TrackBookmark> {
        val currentId = currentTrack.value?.id ?: return emptyList()
        return _bookmarks.value.filter { it.trackId == currentId }.sortedBy { it.timestampMs }
    }

    // Audiophile Sleep Timer
    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepTimerEndOfTrack.value = false
        val totalSec = minutes * 60
        _sleepTimerRemainingSec.value = totalSec

        sleepTimerJob = viewModelScope.launch {
            var remaining = totalSec
            while (remaining > 0 && isActive) {
                delay(1000)
                remaining--
                _sleepTimerRemainingSec.value = remaining
            }
            if (isActive) {
                _sleepTimerRemainingSec.value = 0
                stop()
            }
        }
    }

    fun setSleepTimerEndOfTrack(enabled: Boolean) {
        sleepTimerJob?.cancel()
        _sleepTimerRemainingSec.value = 0
        _sleepTimerEndOfTrack.value = enabled
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingSec.value = 0
        _sleepTimerEndOfTrack.value = false
    }

    fun setSleepTimer(minutes: Int) = startSleepTimer(minutes)

    // Playback Controls
    fun playQueue(queue: List<Track>, startIndex: Int = 0) {
        if (queue.isEmpty()) return
        playbackManager.playQueue(queue, startIndex)
    }

    fun playTrack(track: Track) {
        val q = activeQueue.value
        if (q.any { it.id == track.id }) {
            playbackManager.playTrack(track)
        } else {
            playbackManager.playTrack(track, _libraryTracks.value)
        }
    }

    fun togglePlayPause() {
        if (playerStatus.value == PlayerStatus.PLAYING) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun pausePlayback() = playbackManager.pausePlayback()

    fun resumePlayback() = playbackManager.resumePlayback()

    fun playNext() = playbackManager.playNext()

    fun playPrevious() = playbackManager.playPrevious()

    fun toggleShuffle() = playbackManager.setShuffle(!shuffleEnabled.value)

    fun cycleRepeatMode() {
        val nextMode = when (repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        playbackManager.setRepeatMode(nextMode)
    }

    fun toggleRepeatMode() = cycleRepeatMode()

    fun setPlaybackSpeed(speed: Float) = playbackManager.setPlaybackSpeed(speed)

    fun stop() = playbackManager.stopPlayback()

    fun seekTo(posMs: Long) = playbackManager.seekTo(posMs)

    // DSP Controls
    fun toggleDspEnabled(enabled: Boolean) = playbackManager.toggleDspEnabled(enabled)

    fun setPreampGain(gainDb: Float) = playbackManager.setPreampGain(gainDb)

    fun setGraphicBandGain(band: Int, gainDb: Float) = playbackManager.setGraphicBandGain(band, gainDb)

    fun applyGraphicPreset(preset: GraphicEqPreset) = playbackManager.applyGraphicPreset(preset)

    fun setVocalFocus(mode: Int, amount: Float) = playbackManager.setVocalFocus(mode, amount)

    val vocalBalance: StateFlow<Float> = playbackManager.vocalBalance
    val instrumentBalance: StateFlow<Float> = playbackManager.instrumentBalance

    fun setParametricBand(band: Int, enabled: Boolean, type: BiquadType, freq: Float, gainDb: Float, q: Float) =
        playbackManager.setParametricBand(band, enabled, type, freq, gainDb, q)

    fun setStemBalance(vocalGain: Float, instrumentGain: Float) =
        playbackManager.setStemBalance(vocalGain, instrumentGain)

    fun resetVocalStudio() = playbackManager.resetVocalStudio()

    fun setGaplessEnabled(enabled: Boolean) = playbackManager.setGaplessEnabled(enabled)
    fun setRestoreSessionEnabled(enabled: Boolean) = playbackManager.setRestoreSessionEnabled(enabled)
    fun setResumeAfterInterruptionEnabled(enabled: Boolean) = playbackManager.setResumeAfterInterruptionEnabled(enabled)
    fun setAutoResumeEnabled(enabled: Boolean) = setResumeAfterInterruptionEnabled(enabled)

    fun clearPlaybackError() = playbackManager.clearPlaybackError()

    fun resetDsp() = playbackManager.resetDsp()
    fun resetClipping() = playbackManager.resetClipping()

    fun updateBitPerfectStatus() = playbackManager.updateBitPerfectStatus()

    override fun onCleared() {
        super.onCleared()
        repository.unregisterMediaStoreObserver()
        // Audio playback and DSP engine are owned by PlaybackManager and HiResAudioService
        // so background playback continues seamlessly even if UI activity is trimmed.
    }
}
