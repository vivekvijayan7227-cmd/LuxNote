package com.audioplayer.hires.audio

import kotlin.math.abs
import kotlin.math.max

/**
 * Mid/side vocal/instrument focus processor.
 *
 * Aligned with native C++ engine contract:
 * - Vocal Focus: Peaking at 2.6 kHz (+0..+6 dB) and High Shelf at 6 kHz (+0..+3 dB).
 * - Instrumental Focus: Linkwitz-Riley low-pass at 140 Hz and high-pass at 6 kHz.
 * - Center channel separation and stem balancing.
 */
class CenterChannelProcessor {
    companion object {
        const val MODE_OFF = 0
        const val MODE_VOCAL_FOCUS = 1
        const val MODE_INSTRUMENTAL_FOCUS = 2

        const val VOCAL_PRESENCE_HZ = 2600.0f
        const val VOCAL_PRESENCE_MAX_DB = 6.0f
        const val VOCAL_AIR_HZ = 6000.0f
        const val VOCAL_AIR_MAX_DB = 3.0f
        const val INSTRUMENT_SUB_BASS_HZ = 140.0f
        const val INSTRUMENT_AIR_HZ = 6000.0f
    }

    private var mode = MODE_OFF
    private var amount = 0.0f
    private var sampleRate = 44100.0f

    private val vocalBoost = BiquadFilter()
    private val vocalShelf = BiquadFilter()
    private val lpBass = BiquadFilter()
    private val hpAir = BiquadFilter()

    private var vocalGain = 1.0f
    private var instrumentGain = 1.0f

    fun setSampleRate(sr: Float) {
        sampleRate = max(8000.0f, sr)
        resetFilterState()
        recompute()
    }

    fun configure(m: Int, amt: Float) {
        require(m in MODE_OFF..MODE_INSTRUMENTAL_FOCUS) { "Unsupported center-channel mode: $m" }
        mode = m
        amount = amt.coerceIn(0.0f, 1.0f)
        recompute()
    }

    fun setStemBalance(vocal: Float, instrument: Float) {
        vocalGain = vocal.coerceIn(0.0f, 2.0f)
        instrumentGain = instrument.coerceIn(0.0f, 2.0f)
    }

    fun getVocalGain(): Float = vocalGain
    fun getInstrumentGain(): Float = instrumentGain
    fun getMode(): Int = mode
    fun getAmount(): Float = amount

    fun isActive(): Boolean = (mode != MODE_OFF && amount > 0.001f) ||
        abs(vocalGain - 1.0f) > 0.001f || abs(instrumentGain - 1.0f) > 0.001f

    /** Reset filter state without changing user-set stem gain. */
    fun resetFilterState() {
        vocalBoost.reset()
        vocalShelf.reset()
        lpBass.reset()
        hpAir.reset()
    }

    /** Reset the complete processor to its neutral state. */
    fun reset() {
        vocalGain = 1.0f
        instrumentGain = 1.0f
        mode = MODE_OFF
        amount = 0.0f
        resetFilterState()
        recompute()
    }

    /** Process one interleaved stereo frame in place. Caller must ensure offset+1 is valid. */
    fun processFrame(samples: FloatArray, offset: Int) {
        val activeVocal = mode == MODE_VOCAL_FOCUS && amount > 0.001f
        val activeInstrument = mode == MODE_INSTRUMENTAL_FOCUS && amount > 0.001f
        val stemBalanceChanged = abs(vocalGain - 1.0f) > 0.001f ||
            abs(instrumentGain - 1.0f) > 0.001f
        if (!activeVocal && !activeInstrument && !stemBalanceChanged) return

        val inL = samples[offset]
        val inR = samples[offset + 1]
        val mid = 0.5f * (inL + inR) * vocalGain
        var side = 0.5f * (inL - inR) * instrumentGain

        val midOut = when {
            activeVocal -> {
                val presence = vocalBoost.processMonoSample(mid.toDouble())
                val air = vocalShelf.processMonoSample(presence)
                (air * (1.0 + 0.35 * amount)).toFloat().also {
                    side *= max(0.20f, 1.0f - amount * 0.65f)
                }
            }
            activeInstrument -> {
                val subBass = lpBass.processMonoSample(mid.toDouble()).toFloat()
                val air = hpAir.processMonoSample(mid.toDouble()).toFloat()
                val vocalBand = mid - subBass - air
                val vocalAtten = max(0.063f, 1.0f - amount * 0.937f)
                subBass + air + vocalBand * vocalAtten
            }
            else -> mid
        }

        samples[offset] = midOut + side
        samples[offset + 1] = midOut - side
    }

    private fun recompute() {
        when (mode) {
            MODE_VOCAL_FOCUS -> {
                vocalBoost.configure(
                    type = BiquadType.Peaking,
                    sr = sampleRate,
                    frequency = VOCAL_PRESENCE_HZ,
                    gainDb = amount * VOCAL_PRESENCE_MAX_DB,
                    q = 1.2
                )
                // Aligned with native C++ calcHighShelf(snap.vocalShelfCoeffs, snap.sampleRate, 6000.0f, snap.vocalAmount * 3.0f, 0.707f)
                vocalShelf.configure(
                    type = BiquadType.HighShelf,
                    sr = sampleRate,
                    frequency = VOCAL_AIR_HZ,
                    gainDb = amount * VOCAL_AIR_MAX_DB,
                    q = 0.707
                )
            }
            MODE_INSTRUMENTAL_FOCUS -> {
                lpBass.configure(
                    type = BiquadType.LowPass,
                    sr = sampleRate,
                    frequency = INSTRUMENT_SUB_BASS_HZ,
                    gainDb = 0.0f,
                    q = 0.7071
                )
                hpAir.configure(
                    type = BiquadType.HighPass,
                    sr = sampleRate,
                    frequency = INSTRUMENT_AIR_HZ,
                    gainDb = 0.0f,
                    q = 0.7071
                )
            }
        }
    }
}
