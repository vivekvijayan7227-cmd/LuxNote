package com.audioplayer.hires.audio

import org.junit.Assert.*
import org.junit.Test

class PlaybackPoliciesTest {
    @Test fun outputRateChangesRequireRecreate() {
        assertTrue(PlaybackFormatPolicy.mustRecreateAudioTrack(44100, 48000, true))
        assertTrue(PlaybackFormatPolicy.mustRecreateAudioTrack(48000, 44100, true))
        assertTrue(PlaybackFormatPolicy.mustRecreateAudioTrack(44100, 96000, true))
        assertTrue(PlaybackFormatPolicy.mustRecreateAudioTrack(24000, 48000, true))
        assertTrue(PlaybackFormatPolicy.mustRecreateAudioTrack(null, 44100, false))
        assertFalse(PlaybackFormatPolicy.mustRecreateAudioTrack(44100, 44100, true))
    }

    @Test fun frameStrideDetectsMisread24BitFlac() {
        val pcm16 = PlaybackFormatPolicy.DecodedFormat(96000, 2, 2)
        val pcm24 = PlaybackFormatPolicy.DecodedFormat(96000, 2, 3)
        assertEquals(240, pcm16.frameCountFromBytes(960))
        assertEquals(160, pcm24.frameCountFromBytes(960))
    }

    @Test(expected = IllegalArgumentException::class)
    fun incompleteFramesFailFast() {
        PlaybackFormatPolicy.DecodedFormat(48000, 2, 3).frameCountFromBytes(100)
    }

    @Test fun playbackRateMismatchesExplainOldPitchSymptom() {
        assertEquals(0.91875, PlaybackFormatPolicy.apparentPitchRatio(48000, 44100), 0.00001)
        assertEquals(1.088435374, PlaybackFormatPolicy.apparentPitchRatio(44100, 48000), 0.00001)
    }

    @Test fun audioTrackRecreationDoesNotResetTimelineToZero() {
        val t = PlaybackTimeline()
        t.beginSegment(0L, 0L, 44100)
        assertEquals(1000L, t.positionMs(44100L))
        val checkpoint = t.checkpointBeforeSinkRecreate(88200L)
        assertEquals(2000L, checkpoint)
        t.beginSegment(checkpoint, 0L, 48000)
        assertEquals(3000L, t.positionMs(48000L))
    }

    @Test fun playbackHeadWrapAndSeekWork() {
        val t = PlaybackTimeline()
        t.beginSegment(1500L, 0xffff_ff00L, 48000)
        assertEquals(2500L, t.positionMs(0x0000_ba80L)) // 48000 frames after wrap
        t.resetForSeek(70000L, 0L, 44100)
        assertEquals(71000L, t.positionMs(44100L))
    }

    @Test fun seekBelongsOnlyToCurrentTrack() {
        val box = SessionSeekMailbox()
        box.request(17L, 45000L)
        assertNull(box.takeFor(18L))
        box.request(18L, 10000L)
        assertEquals(10000L, box.takeFor(18L)?.positionMs)
        assertNull(box.takeFor(18L))
    }

    @Test fun audioDiagnosticsCounterTracking() {
        AudioDiagnostics.resetMetrics()
        assertEquals(0L, AudioDiagnostics.zeroWriteCount.get())
        assertEquals(0L, AudioDiagnostics.writeErrorCount.get())
        assertEquals(0L, AudioDiagnostics.drainTimeoutCount.get())
        assertEquals(0L, AudioDiagnostics.stallCount.get())

        AudioDiagnostics.recordZeroWrite()
        AudioDiagnostics.recordZeroWrite()
        assertEquals(2L, AudioDiagnostics.zeroWriteCount.get())

        AudioDiagnostics.recordWriteError(-3)
        assertEquals(1L, AudioDiagnostics.writeErrorCount.get())

        AudioDiagnostics.recordDrainTimeout()
        assertEquals(1L, AudioDiagnostics.drainTimeoutCount.get())

        AudioDiagnostics.recordStall()
        assertEquals(1L, AudioDiagnostics.stallCount.get())

        AudioDiagnostics.resetMetrics()
        assertEquals(0L, AudioDiagnostics.zeroWriteCount.get())
    }

    @Test fun presentationTimestampSeekAnchorAlignment() {
        val t = PlaybackTimeline()
        // Seek requested to 30000ms, extractor lands at 29400ms keyframe
        val seekRequestedMs = 30000L
        val actualKeyframeEmittedMs = 29400L
        val initialHead = 10000L

        t.resetForSeek(seekRequestedMs, initialHead, 44100)
        // When first frame is decoded with presentationTimeUs = 29400ms * 1000, anchor resets to actual timestamp
        t.resetForSeek(actualKeyframeEmittedMs, initialHead, 44100)

        assertEquals(29400L, t.positionMs(initialHead))
        assertEquals(30400L, t.positionMs(initialHead + 44100L))
    }
}
