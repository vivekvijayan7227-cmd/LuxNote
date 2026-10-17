package com.audioplayer.hires.audio

import kotlin.math.*

/**
 * Preamp gain stage with peak monitoring and soft-knee anti-clipping limiter.
 * Gain range: -18 dB to +6 dB.
 */
class PreampGainStage {
    private var gainDb = 0.0f
    private var linearGain = 1.0f
    private var clippingOccurred = false
    private var peakLevel = 0.0f

    fun setGainDb(db: Float) {
        gainDb = db.coerceIn(-18.0f, 6.0f)
        linearGain = 10.0f.pow(gainDb / 20.0f)
    }

    fun getGainDb(): Float = gainDb
    fun getLinearGain(): Float = linearGain

    private var lastClipTimestampMs: Long = 0L
    private val CLIP_HOLD_DURATION_MS = 1500L

    fun hasClipped(): Boolean {
        val now = System.currentTimeMillis()
        return (now - lastClipTimestampMs) < CLIP_HOLD_DURATION_MS
    }

    fun resetClipping() {
        lastClipTimestampMs = 0L
        peakLevel = 0.0f
    }

    fun getPeakLevel(): Float = peakLevel

    fun applyGain(samples: FloatArray, offset: Int) {
        samples[offset] *= linearGain
        samples[offset + 1] *= linearGain
    }

    fun applyLimiter(samples: FloatArray, offset: Int) {
        var l = samples[offset]
        var r = samples[offset + 1]

        val maxAmp = max(abs(l), abs(r))
        if (maxAmp > peakLevel) {
            peakLevel = maxAmp
        }

        if (maxAmp > 0.95f) {
            lastClipTimestampMs = System.currentTimeMillis()
            l = softClip(l)
            r = softClip(r)
            samples[offset] = l
            samples[offset + 1] = r
        }
    }

    fun processFrame(samples: FloatArray, offset: Int) {
        applyGain(samples, offset)
        applyLimiter(samples, offset)
    }

    fun softClip(x: Float): Float {
        return when {
            x > 0.95f -> (0.95f + 0.05f * tanh((x - 0.95f) / 0.05f)).toFloat()
            x < -0.95f -> (-0.95f + 0.05f * tanh((x + 0.95f) / 0.05f)).toFloat()
            else -> x
        }
    }
}
