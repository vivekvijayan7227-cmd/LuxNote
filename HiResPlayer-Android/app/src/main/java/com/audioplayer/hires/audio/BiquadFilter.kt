package com.audioplayer.hires.audio

import kotlin.math.*

/**
 * Standard Biquad filter types adhering to Robert Bristow-Johnson (RBJ) Audio EQ Cookbook.
 */
enum class BiquadType {
    Peaking,
    LowShelf,
    HighShelf,
    LowPass,
    HighPass,
    Notch,
    BandPass
}

/**
 * High-precision Direct Form II Transposed biquad filter.
 * Uses 64-bit float math for internal states to completely eliminate numerical truncation noise,
 * zipper noise, and filter instability at low frequencies or high sample rates.
 */
class BiquadFilter {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0

    // Direct Form II Transposed filter states for Left & Right channels
    private var s1L = 0.0
    private var s2L = 0.0
    private var s1R = 0.0
    private var s2R = 0.0

    private var sampleRate = 44100.0f
    private var isBypassed = true

    fun reset() {
        s1L = 0.0
        s2L = 0.0
        s1R = 0.0
        s2R = 0.0
    }

    fun isBypassed(): Boolean = isBypassed

    /**
     * Configures the biquad filter coefficients.
     * @param type Filter type
     * @param sr Current sample rate (Hz)
     * @param frequency Center or corner frequency (Hz)
     * @param gainDb Gain in decibels (for Peaking and Shelving filters)
     * @param q Quality factor (resonance / bandwidth)
     */
    fun configure(
        type: BiquadType,
        sr: Float,
        frequency: Float,
        gainDb: Float,
        q: Double = 0.7071
    ) {
        sampleRate = max(8000.0f, sr)
        val nyquist = sampleRate * 0.499f
        val f0 = frequency.coerceIn(10.0f, nyquist)
        val qClamped = q.coerceIn(0.05, 20.0)

        // Bypass check for peaking/shelving with negligible gain
        if ((type == BiquadType.Peaking || type == BiquadType.LowShelf || type == BiquadType.HighShelf)
            && abs(gainDb) < 0.001f
        ) {
            b0 = 1.0; b1 = 0.0; b2 = 0.0
            a1 = 0.0; a2 = 0.0
            isBypassed = true
            return
        }

        isBypassed = false
        val omega = 2.0 * PI * (f0 / sampleRate)
        val cosOmega = cos(omega)
        val sinOmega = sin(omega)
        val alpha = sinOmega / (2.0 * qClamped)
        val aLinear = 10.0.pow(gainDb / 40.0) // sqrt of gain for shelving & peaking

        var a0 = 1.0

        when (type) {
            BiquadType.Peaking -> {
                b0 = 1.0 + alpha * aLinear
                b1 = -2.0 * cosOmega
                b2 = 1.0 - alpha * aLinear
                a0 = 1.0 + alpha / aLinear
                a1 = -2.0 * cosOmega
                a2 = 1.0 - alpha / aLinear
            }
            BiquadType.LowShelf -> {
                val sqrtA = sqrt(aLinear)
                val twoSqrtAAlpha = 2.0 * sqrtA * alpha
                b0 = aLinear * ((aLinear + 1.0) - (aLinear - 1.0) * cosOmega + twoSqrtAAlpha)
                b1 = 2.0 * aLinear * ((aLinear - 1.0) - (aLinear + 1.0) * cosOmega)
                b2 = aLinear * ((aLinear + 1.0) - (aLinear - 1.0) * cosOmega - twoSqrtAAlpha)
                a0 = (aLinear + 1.0) + (aLinear - 1.0) * cosOmega + twoSqrtAAlpha
                a1 = -2.0 * ((aLinear - 1.0) + (aLinear + 1.0) * cosOmega)
                a2 = (aLinear + 1.0) + (aLinear - 1.0) * cosOmega - twoSqrtAAlpha
            }
            BiquadType.HighShelf -> {
                val sqrtA = sqrt(aLinear)
                val twoSqrtAAlpha = 2.0 * sqrtA * alpha
                b0 = aLinear * ((aLinear + 1.0) + (aLinear - 1.0) * cosOmega + twoSqrtAAlpha)
                b1 = -2.0 * aLinear * ((aLinear - 1.0) + (aLinear + 1.0) * cosOmega)
                b2 = aLinear * ((aLinear + 1.0) + (aLinear - 1.0) * cosOmega - twoSqrtAAlpha)
                a0 = (aLinear + 1.0) - (aLinear - 1.0) * cosOmega + twoSqrtAAlpha
                a1 = 2.0 * ((aLinear - 1.0) - (aLinear + 1.0) * cosOmega)
                a2 = (aLinear + 1.0) - (aLinear - 1.0) * cosOmega - twoSqrtAAlpha
            }
            BiquadType.LowPass -> {
                b0 = (1.0 - cosOmega) / 2.0
                b1 = 1.0 - cosOmega
                b2 = (1.0 - cosOmega) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosOmega
                a2 = 1.0 - alpha
            }
            BiquadType.HighPass -> {
                b0 = (1.0 + cosOmega) / 2.0
                b1 = -(1.0 + cosOmega)
                b2 = (1.0 + cosOmega) / 2.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosOmega
                a2 = 1.0 - alpha
            }
            BiquadType.Notch -> {
                b0 = 1.0
                b1 = -2.0 * cosOmega
                b2 = 1.0
                a0 = 1.0 + alpha
                a1 = -2.0 * cosOmega
                a2 = 1.0 - alpha
            }
            BiquadType.BandPass -> {
                b0 = alpha
                b1 = 0.0
                b2 = -alpha
                a0 = 1.0 + alpha
                a1 = -2.0 * cosOmega
                a2 = 1.0 - alpha
            }
        }

        // Normalize by a0
        val invA0 = 1.0 / a0
        b0 *= invA0
        b1 *= invA0
        b2 *= invA0
        a1 *= invA0
        a2 *= invA0
    }

    /**
     * Process a single stereo sample frame in place.
     */
    fun processFrame(samples: FloatArray, offset: Int) {
        if (isBypassed) return

        val inL = samples[offset].toDouble()
        val inR = samples[offset + 1].toDouble()

        // Direct Form II Transposed for Left
        val outL = b0 * inL + s1L
        s1L = b1 * inL - a1 * outL + s2L
        s2L = b2 * inL - a2 * outL

        // Direct Form II Transposed for Right
        val outR = b0 * inR + s1R
        s1R = b1 * inR - a1 * outR + s2R
        s2R = b2 * inR - a2 * outR

        samples[offset] = outL.toFloat()
        samples[offset + 1] = outR.toFloat()
    }

    /**
     * Process a single mono sample without allocating any array.
     */
    fun processMonoSample(sample: Double): Double {
        if (isBypassed) return sample
        val out = b0 * sample + s1L
        s1L = b1 * sample - a1 * out + s2L
        s2L = b2 * sample - a2 * out
        return out
    }


    /**
     * Evaluates the continuous complex transfer function magnitude in dB at frequency f.
     * Used for real-time visual EQ curve rendering.
     */
    fun evaluateMagnitudeDb(f: Float): Float {
        if (isBypassed) return 0.0f
        val w = 2.0 * PI * (f / sampleRate)
        val phi = 4.0 * (sin(w / 2.0)).pow(2.0)

        // H(e^jw) = B(z) / A(z)
        // |H|^2 = ((b0+b1+b2)^2 + (b0*b2*phi - (b1*(b0+b2)+4*b0*b2))*phi) /
        //         ((1+a1+a2)^2 + (a2*phi - (a1*(1+a2)+4*a2))*phi)
        val num = (b0 + b1 + b2).pow(2.0) + (b0 * b2 * phi - (b1 * (b0 + b2) + 4.0 * b0 * b2)) * phi
        val den = (1.0 + a1 + a2).pow(2.0) + (a2 * phi - (a1 * (1.0 + a2) + 4.0 * a2)) * phi

        if (den <= 1e-12 || num <= 1e-12) return 0.0f
        val magSq = num / den
        return (10.0 * log10(magSq)).toFloat()
    }
}
