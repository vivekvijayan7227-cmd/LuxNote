package com.audioplayer.hires.playback

import android.media.AudioManager
import org.junit.Assert.*
import org.junit.Test

class AudioFocusManagerTest {

    @Test
    fun testInitialState() {
        val focusManager = AudioFocusManager(
            context = null,
            onPauseRequested = {},
            onResumeRequested = {}
        )
        assertEquals(AudioFocusManager.FocusState.NONE, focusManager.focusState)
        assertFalse(focusManager.wasPlayingBeforeFocusLoss)
    }

    @Test
    fun testTransientFocusLossAndAutoResume() {
        var pauseCount = 0
        var resumeCount = 0

        val focusManager = AudioFocusManager(
            context = null,
            onPauseRequested = { pauseCount++ },
            onResumeRequested = { resumeCount++ }
        )

        // Simulate incoming phone call or navigation prompt
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)

        assertEquals(1, pauseCount)
        assertEquals(0, resumeCount)
        assertEquals(AudioFocusManager.FocusState.LOST_TRANSIENT, focusManager.focusState)
        assertTrue(focusManager.wasPlayingBeforeFocusLoss)

        // Call ended, system grants focus back
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        assertEquals(1, pauseCount)
        assertEquals(1, resumeCount)
        assertEquals(AudioFocusManager.FocusState.GRANTED, focusManager.focusState)
        assertFalse(focusManager.wasPlayingBeforeFocusLoss)
    }

    @Test
    fun testManualPausePreventsAutoResumeOnFocusGain() {
        var pauseCount = 0
        var resumeCount = 0

        val focusManager = AudioFocusManager(
            context = null,
            onPauseRequested = { pauseCount++ },
            onResumeRequested = { resumeCount++ }
        )

        // Phone call starts
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertEquals(1, pauseCount)
        assertTrue(focusManager.wasPlayingBeforeFocusLoss)

        // User explicitly paused playback during the call
        focusManager.onUserPaused()
        assertFalse(focusManager.wasPlayingBeforeFocusLoss)

        // Call ends: system grants focus back
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)

        // Because user manually paused, playback MUST NOT resume!
        assertEquals(0, resumeCount)
        assertEquals(AudioFocusManager.FocusState.GRANTED, focusManager.focusState)
        assertFalse(focusManager.wasPlayingBeforeFocusLoss)
    }

    @Test
    fun testPermanentFocusLossDoesNotAutoResume() {
        var pauseCount = 0
        var resumeCount = 0

        val focusManager = AudioFocusManager(
            context = null,
            onPauseRequested = { pauseCount++ },
            onResumeRequested = { resumeCount++ }
        )

        // Another player took exclusive focus
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS)
        assertEquals(1, pauseCount)
        assertEquals(AudioFocusManager.FocusState.NONE, focusManager.focusState)
        assertFalse(focusManager.wasPlayingBeforeFocusLoss)

        // Focus subsequently gained: must not resume
        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_GAIN)
        assertEquals(0, resumeCount)
    }

    @Test
    fun testAbandonFocusResetsState() {
        val focusManager = AudioFocusManager(
            context = null,
            onPauseRequested = {},
            onResumeRequested = {}
        )

        focusManager.onAudioFocusChange(AudioManager.AUDIOFOCUS_LOSS_TRANSIENT)
        assertTrue(focusManager.wasPlayingBeforeFocusLoss)

        focusManager.abandonAudioFocus()
        assertEquals(AudioFocusManager.FocusState.NONE, focusManager.focusState)
        assertFalse(focusManager.wasPlayingBeforeFocusLoss)
    }
}
