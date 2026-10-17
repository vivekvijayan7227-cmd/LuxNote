package com.audioplayer.hires.playback

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.audioplayer.hires.audio.AudioTrackPlayer
import com.audioplayer.hires.audio.CenterChannelProcessor
import com.audioplayer.hires.audio.StudioDspPipeline
import com.audioplayer.hires.model.GraphicEqPreset
import com.audioplayer.hires.model.PlayerStatus
import com.audioplayer.hires.model.RepeatMode
import com.audioplayer.hires.model.Track
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Service-owned, application-scoped Playback Controller.
 * Provides authoritative ownership of AudioTrackPlayer, StudioDspPipeline,
 * playback queues, transport state machine, and foreground service synchronization.
 * Surpasses ViewModel lifecycle so playback is never terminated when UI activities are trimmed.
 */
class PlaybackManager private constructor(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "luxnote_playback_prefs"
        private const val KEY_GAPLESS = "key_gapless_enabled"
        private const val KEY_AUTO_RESUME = "key_auto_resume_enabled"

        @Volatile
        private var INSTANCE: PlaybackManager? = null

        fun getInstance(context: Context): PlaybackManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PlaybackManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val dspPipeline = StudioDspPipeline()
    val audioPlayer = AudioTrackPlayer(context, dspPipeline)

    // Playback State
    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _playerStatus = MutableStateFlow(PlayerStatus.STOPPED)
    val playerStatus: StateFlow<PlayerStatus> = _playerStatus.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _activeQueue = MutableStateFlow<List<Track>>(emptyList())
    val activeQueue: StateFlow<List<Track>> = _activeQueue.asStateFlow()

    private val _activeQueueId = MutableStateFlow("queue_1")
    val activeQueueId: StateFlow<String> = _activeQueueId.asStateFlow()

    private val _currentQueueIndex = MutableStateFlow(0)
    val currentQueueIndex: StateFlow<Int> = _currentQueueIndex.asStateFlow()

    private val _shuffleEnabled = MutableStateFlow(false)
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled.asStateFlow()

    private val _repeatMode = MutableStateFlow(RepeatMode.ALL)
    val repeatMode: StateFlow<RepeatMode> = _repeatMode.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    // Persistent Settings
    private val _isGaplessEnabled = MutableStateFlow(prefs.getBoolean(KEY_GAPLESS, true))
    val isGaplessEnabled: StateFlow<Boolean> = _isGaplessEnabled.asStateFlow()

    private val _isAutoResumeEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RESUME, true))
    val isAutoResumeEnabled: StateFlow<Boolean> = _isAutoResumeEnabled.asStateFlow()

    // DSP State
    private val _dspEnabled = MutableStateFlow(false)
    val dspEnabled: StateFlow<Boolean> = _dspEnabled.asStateFlow()

    private val _isBitPerfect = MutableStateFlow(false)
    val isBitPerfect: StateFlow<Boolean> = _isBitPerfect.asStateFlow()

    private val _signalPathDescription = MutableStateFlow("DSP: Bypassed")
    val signalPathDescription: StateFlow<String> = _signalPathDescription.asStateFlow()

    private val _preampGainDb = MutableStateFlow(0.0f)
    val preampGainDb: StateFlow<Float> = _preampGainDb.asStateFlow()

    private val _hasClipped = MutableStateFlow(false)
    val hasClipped: StateFlow<Boolean> = _hasClipped.asStateFlow()

    private val _graphicGains = MutableStateFlow(FloatArray(10) { 0f })
    val graphicGains: StateFlow<FloatArray> = _graphicGains.asStateFlow()

    private val _vocalFocusMode = MutableStateFlow(CenterChannelProcessor.MODE_OFF)
    val vocalFocusMode: StateFlow<Int> = _vocalFocusMode.asStateFlow()

    private val _vocalFocusAmount = MutableStateFlow(0.0f)
    val vocalFocusAmount: StateFlow<Float> = _vocalFocusAmount.asStateFlow()

    init {
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

        updateBitPerfectStatus()
    }

    fun setGaplessEnabled(enabled: Boolean) {
        _isGaplessEnabled.value = enabled
        prefs.edit().putBoolean(KEY_GAPLESS, enabled).apply()
    }

    fun setAutoResumeEnabled(enabled: Boolean) {
        _isAutoResumeEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_RESUME, enabled).apply()
    }

    fun setActiveQueue(queueId: String, tracks: List<Track>, startIndex: Int = 0) {
        _activeQueueId.value = queueId
        _activeQueue.value = tracks
        _currentQueueIndex.value = if (tracks.isNotEmpty()) startIndex.coerceIn(0, tracks.size - 1) else 0
    }

    fun playTrack(track: Track, queue: List<Track>? = null, index: Int = -1) {
        if (queue != null) {
            _activeQueue.value = queue
            _currentQueueIndex.value = if (index >= 0) index else queue.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        } else {
            val q = _activeQueue.value
            val foundIdx = q.indexOfFirst { it.id == track.id }
            if (foundIdx >= 0) {
                _currentQueueIndex.value = foundIdx
            } else {
                _activeQueue.value = listOf(track)
                _currentQueueIndex.value = 0
            }
        }

        _currentTrack.value = track
        audioPlayer.play(track)
        updateBitPerfectStatus()
        syncForegroundService()
    }

    fun playQueue(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, tracks.size - 1)
        _activeQueue.value = tracks
        _currentQueueIndex.value = clampedIndex
        val track = tracks[clampedIndex]
        _currentTrack.value = track
        audioPlayer.play(track)
        updateBitPerfectStatus()
        syncForegroundService()
    }

    fun resumePlayback() {
        if (_playerStatus.value == PlayerStatus.PAUSED) {
            audioPlayer.resume()
        } else if (_currentTrack.value != null) {
            audioPlayer.play(_currentTrack.value!!)
        } else if (_activeQueue.value.isNotEmpty()) {
            val idx = _currentQueueIndex.value.coerceIn(0, _activeQueue.value.size - 1)
            playTrack(_activeQueue.value[idx])
        }
    }

    fun pausePlayback() {
        audioPlayer.pause()
    }

    fun stopPlayback() {
        audioPlayer.stop()
        _positionMs.value = 0L
    }

    fun seekTo(positionMs: Long) {
        audioPlayer.seekTo(positionMs)
        _positionMs.value = positionMs
    }

    fun playNext() {
        val q = _activeQueue.value
        if (q.isEmpty()) return

        if (_shuffleEnabled.value && q.size > 1) {
            var nextIdx = (0 until q.size).random()
            if (nextIdx == _currentQueueIndex.value) {
                nextIdx = (nextIdx + 1) % q.size
            }
            _currentQueueIndex.value = nextIdx
            playTrack(q[nextIdx])
            return
        }

        val nextIndex = _currentQueueIndex.value + 1
        if (nextIndex < q.size) {
            _currentQueueIndex.value = nextIndex
            playTrack(q[nextIndex])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            _currentQueueIndex.value = 0
            playTrack(q[0])
        }
    }

    fun playPrevious() {
        val q = _activeQueue.value
        if (q.isEmpty()) return

        if (_positionMs.value > 3000L) {
            seekTo(0L)
            return
        }

        val prevIndex = _currentQueueIndex.value - 1
        if (prevIndex >= 0) {
            _currentQueueIndex.value = prevIndex
            playTrack(q[prevIndex])
        } else if (_repeatMode.value == RepeatMode.ALL) {
            val lastIdx = q.size - 1
            _currentQueueIndex.value = lastIdx
            playTrack(q[lastIdx])
        } else {
            seekTo(0L)
        }
    }

    fun setShuffle(enabled: Boolean) {
        _shuffleEnabled.value = enabled
    }

    fun setRepeatMode(mode: RepeatMode) {
        _repeatMode.value = mode
    }

    fun setPlaybackSpeed(speed: Float) {
        _playbackSpeed.value = speed
        audioPlayer.setSpeed(speed)
    }

    private fun handleTrackCompleted() {
        when (_repeatMode.value) {
            RepeatMode.ONE -> {
                _currentTrack.value?.let { audioPlayer.play(it) }
            }
            RepeatMode.ALL -> {
                playNext()
            }
            RepeatMode.OFF -> {
                val q = _activeQueue.value
                if (_currentQueueIndex.value + 1 < q.size) {
                    playNext()
                } else {
                    stopPlayback()
                }
            }
        }
    }

    // DSP Controls
    fun toggleDspEnabled(enabled: Boolean) {
        dspPipeline.dspEnabled = enabled
        _dspEnabled.value = enabled
        updateBitPerfectStatus()
    }

    fun setPreampGain(gainDb: Float) {
        dspPipeline.setPreampGain(gainDb)
        _preampGainDb.value = gainDb
        dspPipeline.resetClipping()
        _hasClipped.value = false
        if (gainDb != 0.0f && !_dspEnabled.value) {
            toggleDspEnabled(true)
        }
        updateBitPerfectStatus()
    }

    fun setGraphicBandGain(band: Int, gainDb: Float) {
        dspPipeline.setGraphicBandGain(band, gainDb)
        _graphicGains.value = dspPipeline.graphicEq.getAllGains()
        if (gainDb != 0.0f && !_dspEnabled.value) {
            toggleDspEnabled(true)
        }
        updateBitPerfectStatus()
    }

    fun applyGraphicPreset(preset: GraphicEqPreset) {
        for (i in preset.gainsDb.indices) {
            dspPipeline.setGraphicBandGain(i, preset.gainsDb[i])
        }
        _graphicGains.value = dspPipeline.graphicEq.getAllGains()
        if (!dspPipeline.graphicEq.isFlat() && !_dspEnabled.value) {
            toggleDspEnabled(true)
        }
        updateBitPerfectStatus()
    }

    fun setVocalFocus(mode: Int, amount: Float) {
        dspPipeline.setVocalFocus(mode, amount)
        _vocalFocusMode.value = mode
        _vocalFocusAmount.value = amount
        if (mode != CenterChannelProcessor.MODE_OFF && amount > 0.001f && !_dspEnabled.value) {
            toggleDspEnabled(true)
        }
        updateBitPerfectStatus()
    }

    fun resetClipping() {
        dspPipeline.resetClipping()
        _hasClipped.value = false
    }

    fun pollSpectrumAndClipping() {
        _hasClipped.value = dspPipeline.hasClipped()
    }

    fun updateBitPerfectStatus() {
        val dspActive = dspPipeline.dspEnabled
        val isSinkBitPerfectCapable = audioPlayer.isBitPerfectCapable && !audioPlayer.isUsingFallback
        val isBypass = !dspActive

        _isBitPerfect.value = isBypass && isSinkBitPerfectCapable

        _signalPathDescription.value = when {
            dspActive -> "DSP: Active (Studio Filter Cascade)"
            isSinkBitPerfectCapable -> "Bit-Perfect Direct"
            else -> "DSP: Bypassed"
        }
    }

    fun handleTransportAction(action: String) {
        when {
            action == HiResAudioService.ACTION_PLAY -> resumePlayback()
            action == HiResAudioService.ACTION_PAUSE -> pausePlayback()
            action == HiResAudioService.ACTION_NEXT -> playNext()
            action == HiResAudioService.ACTION_PREV -> playPrevious()
            action == HiResAudioService.ACTION_STOP -> stopPlayback()
            action.startsWith("SEEK:") -> {
                val pos = action.substringAfter("SEEK:").toLongOrNull() ?: 0L
                seekTo(pos)
            }
        }
    }

    private fun syncForegroundService() {
        try {
            val isPlaying = _playerStatus.value == PlayerStatus.PLAYING
            val track = _currentTrack.value

            val intent = Intent(context, HiResAudioService::class.java).apply {
                this.action = HiResAudioService.ACTION_SYNC_STATE
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
}
