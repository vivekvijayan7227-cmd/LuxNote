package com.audioplayer.hires.audio

/**
 * Pure Kotlin contract for matching decoded PCM frames to the AudioTrack input.
 * This class is deliberately independent of Android framework objects for JVM tests.
 *
 * IMPORTANT: This is the decoded stream rate, not the physical output-device rate.
 * Android may resample stream PCM to a headset or Bluetooth sink without changing pitch.
 */
object PlaybackFormatPolicy {
    const val DEFAULT_PCM_SAMPLE_RATE_HZ = 44100
    const val DEFAULT_CHANNEL_COUNT = 2

    data class DecodedFormat(
        val sampleRateHz: Int,
        val channelCount: Int,
        val bytesPerSample: Int
    ) {
        init {
            require(sampleRateHz in 8_000..768_000) { "Invalid decoded sample rate: $sampleRateHz" }
            require(channelCount in 1..32) { "Invalid decoded channel count: $channelCount" }
            require(bytesPerSample in setOf(1, 2, 3, 4)) {
                "Unsupported decoded PCM stride: $bytesPerSample"
            }
        }

        val bytesPerFrame: Int get() = channelCount * bytesPerSample

        fun frameCountFromBytes(byteCount: Int): Int {
            require(byteCount >= 0) { "Negative PCM byte count" }
            require(byteCount % bytesPerFrame == 0) {
                "Decoder output has incomplete PCM frame: $byteCount bytes for $bytesPerFrame B/frame"
            }
            return byteCount / bytesPerFrame
        }
    }

    /** An AudioTrack must receive stereo PCM using the decoded frame cadence. */
    fun mustRecreateAudioTrack(
        currentTrackRateHz: Int?,
        decodedRateHz: Int,
        initialized: Boolean
    ): Boolean {
        require(decodedRateHz in 8_000..768_000)
        return !initialized || currentTrackRateHz == null || currentTrackRateHz != decodedRateHz
    }

    /** For logging only; a mismatch directly changes musical pitch/tempo. */
    fun apparentPitchRatio(writtenRateHz: Int, playbackRateHz: Int): Double {
        require(writtenRateHz > 0 && playbackRateHz > 0)
        return playbackRateHz.toDouble() / writtenRateHz.toDouble()
    }
}
