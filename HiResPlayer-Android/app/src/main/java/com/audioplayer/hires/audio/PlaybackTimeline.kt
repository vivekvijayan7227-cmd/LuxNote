package com.audioplayer.hires.audio

/**
 * A segment-aware playback clock for PCM AudioTrack playback-head positions.
 *
 * Begin a new segment AFTER seek/flush or sink recreation. A single counter should
 * not span an AudioTrack release/recreation, because playback-head positions reset.
 *
 * Use the MediaCodec output presentation timestamps to refine seek rounding and
 * encoded delay/padding handling where appropriate; this is a PCM-head clock only.
 */
class PlaybackTimeline {
    private val mask = 0xffff_ffffL
    private var startPositionMs: Long = 0L
    private var startingPlaybackHead: Long = 0L
    private var sampleRateHz: Int = 44_100
    private var initialized: Boolean = false

    fun beginSegment(positionMs: Long, playbackHeadFrames: Long, pcmRateHz: Int) {
        require(positionMs >= 0L) { "Negative media position" }
        require(pcmRateHz in 8_000..768_000) { "Invalid PCM rate" }
        startPositionMs = positionMs
        startingPlaybackHead = playbackHeadFrames and mask
        sampleRateHz = pcmRateHz
        initialized = true
    }

    fun positionMs(playbackHeadFrames: Long): Long {
        check(initialized) { "Call beginSegment before querying the playback clock" }
        // Android's position API is an unsigned 32-bit hardware frame counter.
        val elapsedFrames = ((playbackHeadFrames and mask) - startingPlaybackHead) and mask
        return startPositionMs + (elapsedFrames * 1000L) / sampleRateHz
    }

    /**
     * Call just before releasing the previous AudioTrack. Use the old head.
     * Then call beginSegment with this returned position and the NEW head.
     */
    fun checkpointBeforeSinkRecreate(oldPlaybackHeadFrames: Long): Long =
        positionMs(oldPlaybackHeadFrames)

    fun resetForSeek(seekPositionMs: Long, playbackHeadFramesAfterFlush: Long, pcmRateHz: Int) {
        beginSegment(seekPositionMs, playbackHeadFramesAfterFlush, pcmRateHz)
    }
}
