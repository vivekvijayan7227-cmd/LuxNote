package com.audioplayer.hires.ui

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.audioplayer.hires.audio.AudioTrackPlayer
import com.audioplayer.hires.audio.CenterChannelProcessor
import com.audioplayer.hires.audio.StudioDspPipeline
import com.audioplayer.hires.audio.UsbAudioInspector
import com.audioplayer.hires.audio.UsbDacInfo
import com.audioplayer.hires.data.AudioLibraryRepository
import com.audioplayer.hires.data.DiagnosticsManager
import com.audioplayer.hires.data.DiagnosticsReport
import com.audioplayer.hires.lyrics.LrcParser
import com.audioplayer.hires.model.*
import com.audioplayer.hires.playback.HiResAudioService
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
    val dspPipeline = StudioDspPipeline()
    val audioPlayer = AudioTrackPlayer(application, dspPipeline)
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

    private val _activeQueueId = MutableStateFlow("queue_1")
    val activeQueueId: StateFlow<String> = _activeQueueId.asStateFlow()

    private val _activeQueue = MutableStateFlow<List<Track>>(emptyList())
    val activeQueue: StateFlow<List<Track>> = _activeQueue.asStateFlow()

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

    // Playback Speed
    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Playback Transport Options
    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    // Playback State
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _playerStatus = MutableStateFlow(PlayerStatus.STOPPED)
    val playerStatus: StateFlow<PlayerStatus> = _playerStatus.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    // Hardware USB DAC & Bit-Perfect Info
    private val _usbDacInfo = MutableStateFlow(usbInspector.inspectAudioHardware())
    val usbDacInfo: StateFlow<UsbDacInfo> = _usbDacInfo.asStateFlow()

    // DSP States
    private val _dspEnabled = MutableStateFlow(false)
    val dspEnabled: StateFlow<Boolean> = _dspEnabled.asStateFlow()

    private val _isBitPerfect = MutableStateFlow(true)
    val isBitPerfect: StateFlow<Boolean> = _isBitPerfect.asStateFlow()

    private val _preampGainDb = MutableStateFlow(0.0f)
    val preampGainDb: StateFlow<Float> = _preampGainDb.asStateFlow()

    private val _hasClipped = MutableStateFlow(false)
    val hasClipped: StateFlow<Boolean> = _hasClipped.asStateFlow()

    // Graphic EQ (10 bands)
    private val _graphicGains = MutableStateFlow(FloatArray(10) { 0f })
    val graphicGains: StateFlow<FloatArray> = _graphicGains.asStateFlow()

    // Vocal Studio State (100% Offline Non-AI Mid/Side DSP)
    private val _vocalFocusMode = MutableStateFlow(CenterChannelProcessor.MODE_OFF)
    val vocalFocusMode: StateFlow<Int> = _vocalFocusMode.asStateFlow()

    private val _vocalFocusAmount = MutableStateFlow(0.0f)
    val vocalFocusAmount: StateFlow<Float> = _vocalFocusAmount.asStateFlow()

    // Real-Time Spectrum Visualizer (64 bands)
    private val _fftBands = MutableStateFlow(FloatArray(64) { 0.05f })
    val fftBands: StateFlow<FloatArray> = _fftBands.asStateFlow()

    private var currentQueueIndex = 0

    init {
        screenStack.push(StudioScreen.LIBRARY)

        audioPlayer.onStatusChanged = { status ->
            _playerStatus.value = status
            _currentTrack.value = audioPlayer.currentTrack
            syncForegroundService()
        }
        audioPlayer.onPositionChanged = { pos ->
            _positionMs.value = pos
        }
        audioPlayer.onTrackCompleted = {
            handleTrackCompleted()
        }

        // Notification / Media Button Actions
        HiResAudioService.onTransportAction = { action ->
            when {
                action == HiResAudioService.ACTION_PLAY -> resumePlayback()
                action == HiResAudioService.ACTION_PAUSE -> pausePlayback()
                action == HiResAudioService.ACTION_NEXT -> playNext()
                action == HiResAudioService.ACTION_PREV -> playPrevious()
                action == HiResAudioService.ACTION_STOP -> stop()
                action.startsWith("SEEK:") -> {
                    val pos = action.substringAfter("SEEK:").toLongOrNull() ?: 0L
                    seekTo(pos)
                }
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
            val initialTrack = qTracks.getOrNull(initialIndex) ?: initialTracks.firstOrNull()

            withContext(Dispatchers.Main) {
                _libraryTracks.value = initialTracks
                _folders.value = repository.extractFolders(initialTracks)
                _queues.value = loadedQueues
                _activeQueueId.value = firstQ.id
                _activeQueue.value = qTracks
                currentQueueIndex = initialIndex
                _currentTrack.value = initialTrack
                loadLyricsForTrack(initialTrack)
                registerRealTimeObserver()
                scanDeviceMusic(showIndicator = false)
                refreshDiagnostics()
            }
        }

        // Dynamic 60 FPS Spectrum Polling Loop
        viewModelScope.launch {
            while (isActive) {
                if (_playerStatus.value == PlayerStatus.PLAYING) {
                    _fftBands.value = dspPipeline.spectrumAnalyzer.computeBands()
                    _hasClipped.value = dspPipeline.preamp.hasClipped()
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

    private fun syncForegroundService() {
        try {
            val context = getApplication<Application>()
            val isPlaying = _playerStatus.value == PlayerStatus.PLAYING
            val track = _currentTrack.value

            val intent = Intent(context, HiResAudioService::class.java).apply {
                action = HiResAudioService.ACTION_SYNC_STATE
                if (track != null) {
                    putExtra("EXTRA_TRACK_ID", track.id)
                    putExtra("EXTRA_TITLE", track.title)
                    putExtra("EXTRA_ARTIST", track.artist)
                    putExtra("EXTRA_ALBUM", track.album)
                    putExtra("EXTRA_DURATION", track.durationMs)
                    putExtra("EXTRA_FORMAT", track.format)
                    putExtra("EXTRA_PLAYING", isPlaying)
                    putExtra("EXTRA_POSITION", _positionMs.value)
                }
            }

            if (isPlaying) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } else {
                context.startService(intent)
            }

            HiResAudioService.instance?.updatePlaybackState(
                track,
                _playerStatus.value,
                _positionMs.value
            )
        } catch (e: Exception) {
            e.printStackTrace()
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
        if (_activeQueueId.value == queueId) return

        val updatedQueues = _queues.value.map { q ->
            if (q.id == _activeQueueId.value) {
                q.copy(currentIndex = currentQueueIndex, positionMs = _positionMs.value, isShuffle = _shuffleEnabled.value)
            } else q
        }

        val target = updatedQueues.find { it.id == queueId } ?: return
        _queues.value = updatedQueues
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveQueuesAsync(updatedQueues)
        }
        _activeQueueId.value = queueId

        val trackMap = _libraryTracks.value.associateBy { it.id }
        val targetTracks = target.trackIds.mapNotNull { trackMap[it] }
        _activeQueue.value = targetTracks
        _shuffleEnabled.value = target.isShuffle

        if (targetTracks.isNotEmpty()) {
            currentQueueIndex = target.currentIndex.coerceIn(0, targetTracks.size - 1)
            val trackToPlay = targetTracks[currentQueueIndex]
            _currentTrack.value = trackToPlay
            loadLyricsForTrack(trackToPlay)
            audioPlayer.play(trackToPlay)
            if (target.positionMs > 0) {
                audioPlayer.seekTo(target.positionMs)
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
        if (_activeQueueId.value == queueId) {
            _activeQueue.value = emptyList()
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
        if (_activeQueueId.value == queueId) {
            _activeQueue.value = _activeQueue.value + track
        }
    }

    fun addTrackNextInQueue(track: Track) {
        val current = _activeQueue.value.toMutableList()
        val insertIdx = (currentQueueIndex + 1).coerceAtMost(current.size)
        current.add(insertIdx, track)
        _activeQueue.value = current

        val updated = _queues.value.map { q ->
            if (q.id == _activeQueueId.value) {
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

    // High-Speed Non-Blocking Real-Time Scanning & Direct Storage Discovery
    fun scanDeviceMusic(showIndicator: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (showIndicator) {
                withContext(Dispatchers.Main) {
                    _isImporting.value = true
                    _importProgress.value = "Scanning local storage for audio files..."
                }
            }

            val discoveredTracks = repository.scanAllDeviceMusicAsync()
            val currentTracks = _libraryTracks.value
            val existingIds = currentTracks.map { it.id }.toSet()
            val merged = currentTracks.toMutableList()
            for (t in discoveredTracks) {
                if (t.id !in existingIds) {
                    merged.add(t)
                }
            }

            val finalTracks = if (merged.isEmpty()) discoveredTracks else merged

            if (finalTracks.isNotEmpty()) {
                repository.saveLibraryAsync(finalTracks)
                withContext(Dispatchers.Main) {
                    _libraryTracks.value = finalTracks
                    _folders.value = repository.extractFolders(finalTracks)
                    if (_activeQueue.value.isEmpty()) {
                        _activeQueue.value = finalTracks
                    }
                    if (_currentTrack.value == null && finalTracks.isNotEmpty()) {
                        _currentTrack.value = finalTracks[0]
                        loadLyricsForTrack(finalTracks[0])
                    }
                }
            }

            if (showIndicator) {
                withContext(Dispatchers.Main) {
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
                    if (_activeQueue.value.isEmpty()) {
                        _activeQueue.value = merged
                    }
                    if (_currentTrack.value == null || _currentTrack.value?.uriString?.isBlank() == true) {
                        _currentTrack.value = newTracks[0]
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
        _activeQueue.value = _activeQueue.value.filter { it.id != trackId }

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

        if (_currentTrack.value?.id == trackId) {
            stop()
            _currentTrack.value = updated.firstOrNull()
            loadLyricsForTrack(_currentTrack.value)
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
        val currentId = _currentTrack.value?.id ?: return emptyList()
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
        _activeQueue.value = queue
        currentQueueIndex = startIndex.coerceIn(0, queue.size - 1)
        val track = queue[currentQueueIndex]
        _currentTrack.value = track
        loadLyricsForTrack(track)
        audioPlayer.play(track)
        updateBitPerfectStatus()
        syncForegroundService()
    }

    fun playTrack(track: Track) {
        val queue = _activeQueue.value
        val idx = queue.indexOfFirst { it.id == track.id }
        if (idx >= 0) {
            currentQueueIndex = idx
        } else {
            _activeQueue.value = _libraryTracks.value
            currentQueueIndex = _libraryTracks.value.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        }
        _currentTrack.value = track
        loadLyricsForTrack(track)
        audioPlayer.play(track)
        updateBitPerfectStatus()
        syncForegroundService()
    }

    fun togglePlayPause() {
        if (_playerStatus.value == PlayerStatus.PLAYING) {
            pausePlayback()
        } else {
            resumePlayback()
        }
    }

    fun pausePlayback() {
        audioPlayer.pause()
        syncForegroundService()
    }

    fun resumePlayback() {
        if (_playerStatus.value == PlayerStatus.PAUSED) {
            audioPlayer.resume()
        } else {
            val track = _currentTrack.value ?: _libraryTracks.value.firstOrNull()
            if (track != null) {
                playTrack(track)
            }
        }
        syncForegroundService()
    }

    fun playNext() {
        val queue = _activeQueue.value
        if (queue.isEmpty()) return

        if (_shuffleEnabled.value && queue.size > 1) {
            var nextIdx: Int
            do {
                nextIdx = (0 until queue.size).random()
            } while (nextIdx == currentQueueIndex)
            currentQueueIndex = nextIdx
        } else {
            currentQueueIndex = (currentQueueIndex + 1) % queue.size
        }

        val nextTrack = queue[currentQueueIndex]
        playTrack(nextTrack)
    }

    fun playPrevious() {
        val queue = _activeQueue.value
        if (queue.isEmpty()) return

        if (_positionMs.value > 3000L) {
            audioPlayer.seekTo(0L)
            return
        }

        currentQueueIndex = if (currentQueueIndex - 1 < 0) queue.size - 1 else currentQueueIndex - 1
        val prevTrack = queue[currentQueueIndex]
        playTrack(prevTrack)
    }

    private fun handleTrackCompleted() {
        if (_sleepTimerEndOfTrack.value) {
            cancelSleepTimer()
            stop()
            return
        }

        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                val current = _currentTrack.value
                if (current != null) {
                    audioPlayer.play(current)
                }
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                val queue = _activeQueue.value
                if (currentQueueIndex < queue.size - 1) {
                    playNext()
                } else {
                    stop()
                }
            }
        }
    }

    fun toggleShuffle() {
        _shuffleEnabled.value = !_shuffleEnabled.value
    }

    fun cycleRepeatMode() {
        _repeatMode.value = when (_repeatMode.value) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
    }

    fun toggleRepeatMode() = cycleRepeatMode()

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        audioPlayer.setSpeed(speed)
    }

    fun stop() {
        audioPlayer.stop()
        syncForegroundService()
    }

    fun seekTo(posMs: Long) {
        audioPlayer.seekTo(posMs)
        syncForegroundService()
    }

    // DSP Controls
    fun toggleDspEnabled(enabled: Boolean) {
        dspPipeline.dspEnabled = enabled
        _dspEnabled.value = enabled
        updateBitPerfectStatus()
    }

    fun setPreampGain(gainDb: Float) {
        dspPipeline.preamp.setGainDb(gainDb)
        dspPipeline.nativeEngine.setPreampGain(gainDb)
        _preampGainDb.value = gainDb
        dspPipeline.preamp.resetClipping()
        _hasClipped.value = false
        if (gainDb != 0.0f && !_dspEnabled.value) {
            toggleDspEnabled(true)
        }
        updateBitPerfectStatus()
    }

    fun setGraphicBandGain(band: Int, gainDb: Float) {
        dspPipeline.graphicEq.setBandGain(band, gainDb)
        dspPipeline.nativeEngine.setGraphicBandGain(band, gainDb)
        _graphicGains.value = dspPipeline.graphicEq.getAllGains()
        if (gainDb != 0.0f && !_dspEnabled.value) {
            toggleDspEnabled(true)
        }
        updateBitPerfectStatus()
    }

    fun applyGraphicPreset(preset: GraphicEqPreset) {
        for (i in preset.gainsDb.indices) {
            dspPipeline.graphicEq.setBandGain(i, preset.gainsDb[i])
            dspPipeline.nativeEngine.setGraphicBandGain(i, preset.gainsDb[i])
        }
        _graphicGains.value = dspPipeline.graphicEq.getAllGains()
        if (!dspPipeline.graphicEq.isFlat() && !_dspEnabled.value) {
            toggleDspEnabled(true)
        }
        updateBitPerfectStatus()
    }

    fun setVocalFocus(mode: Int, amount: Float) {
        dspPipeline.centerChannelProcessor.configure(mode, amount)
        dspPipeline.nativeEngine.setVocalFocus(mode, amount)
        _vocalFocusMode.value = mode
        _vocalFocusAmount.value = amount
        if (mode != CenterChannelProcessor.MODE_OFF && amount > 0.001f && !_dspEnabled.value) {
            toggleDspEnabled(true)
        }
        updateBitPerfectStatus()
    }

    private fun updateBitPerfectStatus() {
        _isBitPerfect.value = dspPipeline.isBitPerfectEligible()
    }

    override fun onCleared() {
        super.onCleared()
        repository.unregisterMediaStoreObserver()
        audioPlayer.stop()
        dspPipeline.nativeEngine.release()
    }
}
