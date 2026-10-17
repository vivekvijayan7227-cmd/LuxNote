package com.audioplayer.hires.audio

import kotlin.math.abs

/**
 * 10-band ISO standard audiophile graphic equalizer.
 * Features:
 * - Low-Shelf sub-bass band at 31.25 Hz for controlled deep sub-extension
 * - High-Shelf silky air band at 16.0 kHz for smooth upper harmonics without Nyquist reflection
 * - Constant-Q Peaking filters (Q = 1.85) for the intermediate bands
 */
class GraphicEqualizer {
    companion object {
        const val BAND_COUNT = 10
        val ISO_FREQUENCIES = floatArrayOf(
            31.25f, 62.5f, 125.0f, 250.0f, 500.0f,
            1000.0f, 2000.0f, 4000.0f, 8000.0f, 16000.0f
        )
    }

    private val gainsDb = FloatArray(BAND_COUNT) { 0.0f }
    private val filters = Array(BAND_COUNT) { BiquadFilter() }
    private var sampleRate = 44100.0f

    fun setSampleRate(sr: Float) {
        sampleRate = sr
        for (i in 0 until BAND_COUNT) {
            filters[i].reset()
        }
        recompute()
    }

    fun setBandGain(band: Int, gainDb: Float) {
        if (band !in 0 until BAND_COUNT) return
        gainsDb[band] = gainDb.coerceIn(-12.0f, 12.0f)
        if (abs(gainsDb[band]) < 0.001f) {
            filters[band].reset()
        }
        recomputeBand(band)
    }

    fun getBandGain(band: Int): Float {
        return if (band in 0 until BAND_COUNT) gainsDb[band] else 0.0f
    }

    fun getAllGains(): FloatArray = gainsDb.clone()

    fun isFlat(): Boolean {
        for (g in gainsDb) {
            if (abs(g) > 0.001f) return false
        }
        return true
    }

    fun reset() {
        for (i in 0 until BAND_COUNT) {
            gainsDb[i] = 0.0f
            filters[i].reset()
        }
        recompute()
    }

    fun processFrame(samples: FloatArray, offset: Int) {
        if (isFlat()) return
        for (i in 0 until BAND_COUNT) {
            if (abs(gainsDb[i]) > 0.001f) {
                filters[i].processFrame(samples, offset)
            }
        }
    }

    fun evaluateMagnitudeDb(f: Float): Float {
        var totalDb = 0.0f
        for (i in 0 until BAND_COUNT) {
            totalDb += filters[i].evaluateMagnitudeDb(f)
        }
        return totalDb
    }

    private fun recompute() {
        for (i in 0 until BAND_COUNT) {
            recomputeBand(i)
        }
    }

    private fun recomputeBand(i: Int) {
        when (i) {
            0 -> {
                // Low shelf for deep solid sub-bass
                filters[i].configure(
                    type = BiquadType.LowShelf,
                    sr = sampleRate,
                    frequency = ISO_FREQUENCIES[i],
                    gainDb = gainsDb[i],
                    q = 0.7071
                )
            }
            BAND_COUNT - 1 -> {
                // High shelf for silky air
                val highFreq = ISO_FREQUENCIES[i].coerceAtMost(sampleRate * 0.46f)
                filters[i].configure(
                    type = BiquadType.HighShelf,
                    sr = sampleRate,
                    frequency = highFreq,
                    gainDb = gainsDb[i],
                    q = 0.7071
                )
            }
            else -> {
                // Constant-Q peaking filter with Q = 1.85 for surgical note separation
                filters[i].configure(
                    type = BiquadType.Peaking,
                    sr = sampleRate,
                    frequency = ISO_FREQUENCIES[i],
                    gainDb = gainsDb[i],
                    q = 1.85
                )
            }
        }
    }
}
