package com.audioplayer.hires.playback

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.util.Log

/**
 * Centralized, single-owner Audio Focus Manager for LuxNote.
 *
 * Ensures:
 * 1. Exactly ONE [AudioManager.OnAudioFocusChangeListener] and [AudioFocusRequest] is registered
 *    with the Android system, eliminating listener competition between service and playback manager.
 * 2. Explicit focus state tracking ([FocusState]).
 * 3. Focus regain ([AudioManager.AUDIOFOCUS_GAIN]) only triggers auto-resume if the pause was
 *    caused by a transient focus loss, NEVER if the user paused playback manually.
 * 4. Safe abandonment on playback stop or release.
 */
class AudioFocusManager(
    private val context: Context? = null,
    private val onPauseRequested: () -> Unit,
    private val onResumeRequested: () -> Unit,
    private val isPlaybackActive: () -> Boolean = { true },
    private val onDelayedFocusGranted: (() -> Unit)? = null,
    audioManagerOverride: AudioManager? = null
) : AudioManager.OnAudioFocusChangeListener {

    companion object {
        private const val TAG = "AudioFocusManager"
    }

    enum class FocusState {
        NONE,
        PENDING,
        GRANTED,
        LOST_TRANSIENT
    }

    private val audioManager = audioManagerOverride ?: (context?.getSystemService(Context.AUDIO_SERVICE) as? AudioManager)
    private var audioFocusRequest: AudioFocusRequest? = null

    var focusState: FocusState = FocusState.NONE
        private set

    /**
     * True only if playback was active when a transient focus loss occurred.
     * Reset to false whenever the user manually pauses or stops playback.
     */
    var wasPlayingBeforeFocusLoss: Boolean = false
        private set

    /**
     * Request audio focus before starting audible playback.
     * Returns true only if focus is immediately GRANTED.
     */
    @Synchronized
    fun requestAudioFocus(isCurrentlyPlaying: Boolean): Boolean {
        if (audioManager == null) {
            focusState = FocusState.GRANTED
            return true
        }

        if (focusState == FocusState.GRANTED) {
            return true
        }

        val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val playbackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val request = audioFocusRequest ?: AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(playbackAttributes)
                .setAcceptsDelayedFocusGain(true)
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener(this)
                .build().also { audioFocusRequest = it }

            audioManager.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                this,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            )
        }

        return when (result) {
            AudioManager.AUDIOFOCUS_REQUEST_GRANTED -> {
                Log.d(TAG, "Audio focus GRANTED")
                focusState = FocusState.GRANTED
                wasPlayingBeforeFocusLoss = false
                true
            }
            AudioManager.AUDIOFOCUS_REQUEST_DELAYED -> {
                Log.d(TAG, "Audio focus DELAYED")
                focusState = FocusState.PENDING
                false
            }
            else -> {
                Log.w(TAG, "Audio focus DENIED (code $result)")
                focusState = FocusState.NONE
                wasPlayingBeforeFocusLoss = false
                false
            }
        }
    }

    /**
     * Called when the user explicitly pauses playback.
     * Resets [wasPlayingBeforeFocusLoss] so an external focus gain will not auto-resume.
     */
    @Synchronized
    fun onUserPaused() {
        wasPlayingBeforeFocusLoss = false
        if (focusState == FocusState.PENDING) {
            focusState = FocusState.NONE
        }
    }

    /**
     * Abandon audio focus when playback stops or service is destroyed.
     */
    @Synchronized
    fun abandonAudioFocus() {
        if (audioManager != null && focusState != FocusState.NONE) {
            Log.d(TAG, "Abandoning audio focus")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            } else {
                @Suppress("DEPRECATION")
                audioManager.abandonAudioFocus(this)
            }
        }

        focusState = FocusState.NONE
        wasPlayingBeforeFocusLoss = false
    }

    override fun onAudioFocusChange(focusChange: Int) {
        Log.d(TAG, "onAudioFocusChange: $focusChange (current state: $focusState, wasPlaying: $wasPlayingBeforeFocusLoss)")
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> {
                // Permanent focus loss (e.g., another player took over)
                focusState = FocusState.NONE
                wasPlayingBeforeFocusLoss = false
                onPauseRequested.invoke()
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // Temporary focus loss (e.g., incoming telephone call)
                focusState = FocusState.LOST_TRANSIENT
                val active = isPlaybackActive()
                wasPlayingBeforeFocusLoss = active
                if (active) {
                    onPauseRequested.invoke()
                }
            }
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // System notification: pause without ducking for high-fidelity audio
                focusState = FocusState.LOST_TRANSIENT
                val active = isPlaybackActive()
                wasPlayingBeforeFocusLoss = active
                if (active) {
                    onPauseRequested.invoke()
                }
            }
            AudioManager.AUDIOFOCUS_GAIN -> {
                // Regained focus
                val wasPending = (focusState == FocusState.PENDING)
                val shouldResume = (focusState == FocusState.LOST_TRANSIENT && wasPlayingBeforeFocusLoss)
                focusState = FocusState.GRANTED
                wasPlayingBeforeFocusLoss = false
                if (wasPending) {
                    Log.d(TAG, "Delayed audio focus granted; notifying caller")
                    onDelayedFocusGranted?.invoke()
                } else if (shouldResume) {
                    Log.d(TAG, "Auto-resuming after transient focus interruption")
                    onResumeRequested.invoke()
                } else {
                    Log.d(TAG, "Focus gained but will not auto-resume (was not playing prior to loss or user paused)")
                }
            }
        }
    }
}
