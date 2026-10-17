package com.audioplayer.hires.audio

import kotlin.math.pow

data class ParametricBandConfig(
    var enabled: Boolean = false,
    var type: BiquadType = BiquadType.Peaking,
    var frequency: Float = 1000.0f,
    var gainDb: Float = 0.0f,
    var q: Float = 0.707f
)

/**
 * 12-band fully parametric audiophile equalizer.
 * Supports surgical biquads: Peaking, Low Shelf, High Shelf, Low Pass, High Pass, Notch, Band Pass.
 */
class ParametricEqualizer {
    companion object {
        const val MAX_BANDS = 12
    }

    private val bands = Array(MAX_BANDS) { i ->
        ParametricBandConfig(
            enabled = false,
            type = BiquadType.Peaking,
            frequency = (100.0f * 2.0f.pow(i.toFloat())).coerceIn(20.0f, 20000.0f),
            gainDb = 0.0f,
            q = 0.707f
        )
    }

    private val filters = Array(MAX_BANDS) { BiquadFilter() }
    private var sampleRate = 44100.0f

    fun setSampleRate(sr: Float) {
        sampleRate = sr
        for (i in 0 until MAX_BANDS) {
            filters[i].reset()
        }
        recompute()
    }

    fun configureBand(
        bandIndex: Int,
        enabled: Boolean,
        type: BiquadType,
        freq: Float,
        gainDb: Float,
        q: Float
    ) {
        if (bandIndex !in 0 until MAX_BANDS) return
        val b = bands[bandIndex]
        b.enabled = enabled
        b.type = type
        b.frequency = freq.coerceIn(20.0f, sampleRate * 0.49f)
        b.gainDb = gainDb.coerceIn(-18.0f, 18.0f)
        b.q = q.coerceIn(0.1f, 10.0f)
        recomputeBand(bandIndex)
    }

    fun getBandConfig(bandIndex: Int): ParametricBandConfig {
        return if (bandIndex in 0 until MAX_BANDS) bands[bandIndex].copy() else ParametricBandConfig()
    }

    fun getAllBands(): List<ParametricBandConfig> {
        return bands.map { it.copy() }
    }

    fun isBypassed(): Boolean {
        for (b in bands) {
            if (b.enabled && kotlin.math.abs(b.gainDb) > 0.001f) return false
        }
        return true
    }

    fun reset() {
        for (i in 0 until MAX_BANDS) {
            bands[i].enabled = false
            bands[i].gainDb = 0.0f
            filters[i].reset()
        }
        recompute()
    }

    fun processFrame(samples: FloatArray, offset: Int) {
        for (i in 0 until MAX_BANDS) {
            if (bands[i].enabled) {
                filters[i].processFrame(samples, offset)
            }
        }
    }

    fun evaluateMagnitudeDb(f: Float): Float {
        var totalDb = 0.0f
        for (i in 0 until MAX_BANDS) {
            if (bands[i].enabled) {
                totalDb += filters[i].evaluateMagnitudeDb(f)
            }
        }
        return totalDb
    }

    private fun recompute() {
        for (i in 0 until MAX_BANDS) {
            recomputeBand(i)
        }
    }

    private fun recomputeBand(i: Int) {
        val b = bands[i]
        if (!b.enabled) {
            filters[i].reset()
            return
        }
        filters[i].configure(
            type = b.type,
            sr = sampleRate,
            frequency = b.frequency,
            gainDb = b.gainDb,
            q = b.q.toDouble()
        )
    }
}
