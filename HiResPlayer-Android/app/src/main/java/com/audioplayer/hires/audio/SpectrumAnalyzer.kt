package com.audioplayer.hires.audio

import kotlin.math.*

/**
 * 64-band real-time audio spectrum analyzer.
 * Uses a Hann-windowed Radix-2 FFT with exponential smoothing and peak decay
 * to power the studio visualizer display at 60 FPS.
 */
class SpectrumAnalyzer(private val fftSize: Int = 1024, val bandCount: Int = 64) {
    private val buffer = FloatArray(fftSize)
    private var bufferIndex = 0

    private val real = FloatArray(fftSize)
    private val imag = FloatArray(fftSize)
    private val window = FloatArray(fftSize)

    private val magnitudes = FloatArray(bandCount)
    private val smoothedMagnitudes = FloatArray(bandCount)

    private var sampleRate = 44100.0f

    init {
        // Precompute Hann window
        for (i in 0 until fftSize) {
            window[i] = (0.5 * (1.0 - cos(2.0 * PI * i / (fftSize - 1)))).toFloat()
        }
    }

    fun setSampleRate(sr: Float) {
        sampleRate = max(8000.0f, sr)
    }

    /**
     * Feed incoming stereo samples into the circular FFT buffer.
     */
    fun feedSamples(samples: FloatArray, count: Int) {
        var i = 0
        while (i < count) {
            // Downmix to mono for spectrum analysis
            val mono = 0.5f * (samples[i] + samples[i + 1])
            buffer[bufferIndex] = mono
            bufferIndex = (bufferIndex + 1) % fftSize
            i += 2
        }
    }

    /**
     * Compute current 64-band magnitudes (normalized 0.0 to 1.0) with ballistic decay.
     */
    fun computeBands(): FloatArray {
        // Copy windowed data into FFT buffers
        val start = bufferIndex
        for (i in 0 until fftSize) {
            val idx = (start + i) % fftSize
            real[i] = buffer[idx] * window[i]
            imag[i] = 0.0f
        }

        // Perform in-place Radix-2 Cooley-Tukey FFT
        fft(real, imag, fftSize)

        // Compute bin magnitudes
        val halfFft = fftSize / 2
        val binMags = FloatArray(halfFft)
        for (i in 0 until halfFft) {
            val r = real[i]
            val im = imag[i]
            binMags[i] = sqrt(r * r + im * im) / (fftSize / 4.0f)
        }

        // Map linear FFT bins to 64 logarithmic bands (20 Hz to 20 kHz)
        val minFreq = 20.0f
        val maxFreq = min(20000.0f, sampleRate * 0.48f)
        val logMin = ln(minFreq)
        val logMax = ln(maxFreq)
        val binResolution = sampleRate / fftSize

        for (b in 0 until bandCount) {
            val fLow = exp(logMin + (logMax - logMin) * (b.toFloat() / bandCount))
            val fHigh = exp(logMin + (logMax - logMin) * ((b + 1).toFloat() / bandCount))

            val binStart = (fLow / binResolution).toInt().coerceIn(0, halfFft - 1)
            val binEnd = (fHigh / binResolution).toInt().coerceIn(binStart, halfFft - 1)

            var bandEnergy = 0.0f
            var binCount = 0
            for (bin in binStart..binEnd) {
                bandEnergy = max(bandEnergy, binMags[bin])
                binCount++
            }

            // Convert to dB (-60 dB to 0 dB) and normalize to 0.0 .. 1.0
            val db = if (bandEnergy > 1e-5f) 20.0f * log10(bandEnergy) else -60.0f
            val normalized = ((db + 60.0f) / 60.0f).coerceIn(0.0f, 1.0f)

            // Ballistic smoothing: fast attack, smooth decay
            if (normalized > smoothedMagnitudes[b]) {
                smoothedMagnitudes[b] = normalized
            } else {
                smoothedMagnitudes[b] = smoothedMagnitudes[b] * 0.82f + normalized * 0.18f
            }
        }

        return smoothedMagnitudes.clone()
    }

    private fun fft(re: FloatArray, im: FloatArray, n: Int) {
        // Bit-reversal permutation
        var j = 0
        for (i in 0 until n - 1) {
            if (i < j) {
                val tempR = re[i]; re[i] = re[j]; re[j] = tempR
                val tempI = im[i]; im[i] = im[j]; im[j] = tempI
            }
            var k = n / 2
            while (k <= j) {
                j -= k
                k /= 2
            }
            j += k
        }

        // Cooley-Tukey decimation-in-time
        var len = 2
        while (len <= n) {
            val halfLen = len / 2
            val angle = -2.0 * PI / len
            val wStepR = cos(angle).toFloat()
            val wStepI = sin(angle).toFloat()

            var i = 0
            while (i < n) {
                var wR = 1.0f
                var wI = 0.0f
                for (k in 0 until halfLen) {
                    val uR = re[i + k]
                    val uI = im[i + k]
                    val vR = re[i + k + halfLen] * wR - im[i + k + halfLen] * wI
                    val vI = re[i + k + halfLen] * wI + im[i + k + halfLen] * wR

                    re[i + k] = uR + vR
                    im[i + k] = uI + vI
                    re[i + k + halfLen] = uR - vR
                    im[i + k + halfLen] = uI - vI

                    val nextWR = wR * wStepR - wI * wStepI
                    val nextWI = wR * wStepI + wI * wStepR
                    wR = nextWR
                    wI = nextWI
                }
                i += len
            }
            len *= 2
        }
    }
}
