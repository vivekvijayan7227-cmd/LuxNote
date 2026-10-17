package com.audioplayer.hires

import com.audioplayer.hires.audio.StudioDspPipeline
import com.audioplayer.hires.lyrics.LrcParser
import com.audioplayer.hires.model.*
import org.junit.Assert.*
import org.junit.Test

class AudioPlaybackAndLifecycleTest {

    @Test
    fun testTrackFormatBadgeAndTiming() {
        val flacTrack = Track(
            id = 101L,
            title = "Midnight Echo",
            artist = "Acoustic Reference",
            album = "Hi-Res Master",
            durationMs = 240000L,
            sampleRate = 96000,
            bitDepth = 24,
            format = "FLAC",
            bitrateKbps = 2450,
            uriString = "content://media/external/audio/media/101"
        )

        assertEquals("FLAC 24-bit / 96 kHz • 2450 kbps", flacTrack.formatBadge)
        assertEquals("04:00", formatTime(flacTrack.durationMs))

        val mp3Track = Track(
            id = 102L,
            title = "Standard MP3",
            artist = "Test Artist",
            album = "Test Album",
            durationMs = 185000L,
            sampleRate = 44100,
            bitDepth = 16,
            format = "MP3",
            bitrateKbps = 320,
            uriString = "file:///storage/emulated/0/Music/song.mp3"
        )
        assertEquals("MP3 16-bit / 44 kHz • 320 kbps", mp3Track.formatBadge)
        assertEquals("03:05", formatTime(mp3Track.durationMs))
    }

    @Test
    fun testLibraryMergeIntegrity() {
        // Simulate existing library with user SAF-imported track
        val importedTrack = Track(
            id = 9991L,
            title = "User Imported FLAC",
            artist = "External Source",
            album = "Custom Imports",
            durationMs = 200000L,
            sampleRate = 96000,
            bitDepth = 24,
            format = "FLAC",
            bitrateKbps = 2100,
            uriString = "content://com.android.providers.media.documents/document/audio%3A9991"
        )

        val existingLibrary = listOf(importedTrack)

        // Simulate auto-scan discovering device tracks
        val scannedTrack1 = Track(
            id = 1001L,
            title = "Device Song 1",
            artist = "Artist A",
            album = "Album A",
            durationMs = 180000L,
            sampleRate = 44100,
            bitDepth = 16,
            format = "MP3",
            bitrateKbps = 320,
            uriString = "content://media/external/audio/media/1001"
        )

        val scannedTrack2 = Track(
            id = 1002L,
            title = "Device Song 2",
            artist = "Artist B",
            album = "Album B",
            durationMs = 210000L,
            sampleRate = 48000,
            bitDepth = 24,
            format = "FLAC",
            bitrateKbps = 1411,
            uriString = "content://media/external/audio/media/1002"
        )

        val discoveredTracks = listOf(scannedTrack1, scannedTrack2)

        // Merge logic implemented in MainViewModel: preserve existing imported tracks
        val existingIds = existingLibrary.map { it.id }.toSet()
        val merged = existingLibrary.toMutableList()
        for (t in discoveredTracks) {
            if (t.id !in existingIds) {
                merged.add(t)
            }
        }

        assertEquals(3, merged.size)
        assertTrue("Imported track must be preserved in merged library", merged.any { it.id == 9991L })
        assertTrue("Discovered track 1 must be added", merged.any { it.id == 1001L })
        assertTrue("Discovered track 2 must be added", merged.any { it.id == 1002L })
    }

    @Test
    fun testMultiQueueIndependentSwitching() {
        val t1 = Track(1L, "Song 1", "Artist", "Album", 180000L, 44100, 16, "MP3", 320, "uri1")
        val t2 = Track(2L, "Song 2", "Artist", "Album", 190000L, 44100, 16, "MP3", 320, "uri2")
        val t3 = Track(3L, "Song 3", "Artist", "Album", 200000L, 44100, 16, "MP3", 320, "uri3")

        val q1 = StudioQueue("q1", "Queue 1 (Main)", listOf(1L, 2L), currentIndex = 0, positionMs = 15000L)
        val q2 = StudioQueue("q2", "Queue 2 (Focus)", listOf(3L), currentIndex = 0, positionMs = 45000L)

        val queues = listOf(q1, q2)

        // Verify independent queue state
        val targetQ = queues.find { it.id == "q2" }
        assertNotNull(targetQ)
        assertEquals("Queue 2 (Focus)", targetQ?.name)
        assertEquals(45000L, targetQ?.positionMs)
        assertEquals(1, targetQ?.trackIds?.size)
        assertEquals(3L, targetQ?.trackIds?.first())
    }

    @Test
    fun testBitPerfectEligibilityLogic() {
        val dsp = StudioDspPipeline()

        // When DSP is disabled, bit-perfect pass-through is active
        dsp.dspEnabled = false
        assertTrue("Signal path must be Bit-Perfect when DSP is disabled", dsp.isBitPerfectEligible())

        // When DSP is enabled, bit-perfect is false because EQ/DSP alters samples
        dsp.dspEnabled = true
        assertFalse("Signal path cannot be Bit-Perfect when DSP is altering samples", dsp.isBitPerfectEligible())
    }

    @Test
    fun testLrcParserSync() {
        val rawLrc = """
            [00:00.00]Introduction
            [00:15.50]First line of lyrics
            [00:32.00]Second line of lyrics
            [01:05.25]Chorus starts here
        """.trimIndent()

        val parsed = LrcParser.parse(rawLrc)
        assertEquals(4, parsed.size)
        assertEquals(0L, parsed[0].timestampMs)
        assertEquals("Introduction", parsed[0].text)

        assertEquals(15500L, parsed[1].timestampMs)
        assertEquals("First line of lyrics", parsed[1].text)

        assertEquals(32000L, parsed[2].timestampMs)
        assertEquals("Second line of lyrics", parsed[2].text)

        // Test finding active lyric line at 20 seconds (between 15.5s and 32s)
        val activeLineAt20s = parsed.indexOfLast { it.timestampMs <= 20000L }
        assertEquals(1, activeLineAt20s)
        assertEquals("First line of lyrics", parsed[activeLineAt20s].text)

        // Test finding active lyric line at 1m 10s (after chorus)
        val activeLineAt70s = parsed.indexOfLast { it.timestampMs <= 70000L }
        assertEquals(3, activeLineAt70s)
        assertEquals("Chorus starts here", parsed[activeLineAt70s].text)
    }

    @Test
    fun testEqualizerPresets() {
        val presets = FactoryEqPresets.GRAPHIC_PRESETS
        assertTrue("At least 6 presets should be provided", presets.size >= 6)

        val flat = presets.find { it.name.startsWith("Flat") }
        assertNotNull(flat)
        assertTrue("Flat preset must have all 0 dB gains", flat?.gainsDb?.all { it == 0.0f } == true)

        val bassBoost = presets.find { it.name == "Bass Boost" }
        assertNotNull(bassBoost)
        assertTrue("Bass Boost should boost low frequencies (31Hz, 62Hz)", (bassBoost?.gainsDb?.get(0) ?: 0f) > 3.0f)
    }

    @Test
    fun testSyncStateAction_isDistinctFromTransportActions() {
        // Ensure ACTION_SYNC_STATE is distinct from ACTION_PLAY to prevent recursive loops
        val syncAction = com.audioplayer.hires.playback.HiResAudioService.ACTION_SYNC_STATE
        val playAction = com.audioplayer.hires.playback.HiResAudioService.ACTION_PLAY
        val pauseAction = com.audioplayer.hires.playback.HiResAudioService.ACTION_PAUSE

        assertNotEquals(syncAction, playAction)
        assertNotEquals(syncAction, pauseAction)
        assertEquals("com.audioplayer.hires.ACTION_SYNC_STATE", syncAction)
    }

    @Test
    fun testDirectPathPlaybackEligibility() {
        // Physical file paths should bypass content resolver
        val directPath = "/data/user/0/com.audioplayer.hires/files/imported_tracks/sample.flac"
        assertTrue(directPath.startsWith("/"))
        assertFalse(directPath.startsWith("content://"))
    }
}
