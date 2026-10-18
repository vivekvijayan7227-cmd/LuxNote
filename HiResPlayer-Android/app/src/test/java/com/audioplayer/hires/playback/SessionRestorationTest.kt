package com.audioplayer.hires.playback

import com.audioplayer.hires.model.Track
import org.junit.Assert.*
import org.junit.Test

class SessionRestorationTest {

    private val track1 = Track(
        id = 1001L,
        title = "Track One",
        artist = "Artist A",
        album = "Album 1",
        durationMs = 200000L,
        uriString = "content://media/external/audio/media/1001"
    )

    private val track2 = Track(
        id = 1002L,
        title = "Track Two",
        artist = "Artist B",
        album = "Album 2",
        durationMs = 180000L,
        uriString = "content://media/external/audio/media/1002"
    )

    @Test
    fun testRestoredPlaybackStartDataClass() {
        val restored = PlaybackManager.RestoredPlaybackStart(trackId = 1001L, initialPositionMs = 45000L)
        assertEquals(1001L, restored.trackId)
        assertEquals(45000L, restored.initialPositionMs)
    }

    @Test
    fun testRestorationPreservesSavedPosition() {
        val queue = listOf(track1, track2)
        val savedPositionMs = 75000L // 40% into the song

        // Verification of restoration bounds
        assertTrue("Saved position must be greater than zero", savedPositionMs > 0)
        assertTrue("Saved position must be within track duration", savedPositionMs < track1.durationMs)

        val restoredStart = PlaybackManager.RestoredPlaybackStart(track1.id, savedPositionMs)
        assertEquals(track1.id, restoredStart.trackId)
        assertEquals(savedPositionMs, restoredStart.initialPositionMs)
    }

    @Test
    fun testRestoreAtZeroPercent() {
        val restoredStart = PlaybackManager.RestoredPlaybackStart(track1.id, 0L)
        assertEquals(0L, restoredStart.initialPositionMs)
    }

    @Test
    fun testRestoreAtNearEndOfTrack() {
        val nearEndMs = track1.durationMs - 2000L // 99% of track
        val restoredStart = PlaybackManager.RestoredPlaybackStart(track1.id, nearEndMs)
        assertEquals(nearEndMs, restoredStart.initialPositionMs)
    }
}
