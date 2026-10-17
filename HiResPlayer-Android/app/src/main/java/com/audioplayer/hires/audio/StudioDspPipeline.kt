package com.audioplayer.hires.audio

/**
 * Complete Studio DSP Pipeline managing the processing graph:
 * Native C++20 Studio Engine (libhires_audio.so) with Kotlin Fallback:
 * Preamp -> Graphic EQ -> Parametric EQ -> Center Channel / Vocal Focus -> Soft Limiter -> Spectrum Analyzer.
 *
 * Guarantees a zero-overhead bit-exact passthrough when dspEnabled is false.
 */
class StudioDspPipeline {
    val nativeEngine = NativeAudioEngine()

    val preamp = PreampGainStage()
    val graphicEq = GraphicEqualizer()
    val parametricEq = ParametricEqualizer()
    val centerChannelProcessor = CenterChannelProcessor()
    val spectrumAnalyzer = SpectrumAnalyzer()

    var dspEnabled: Boolean = false
        set(value) {
            field = value
            nativeEngine.setDspEnabled(value)
        }

    private var sampleRate: Float = 44100.0f

    fun setSampleRate(sr: Float) {
        sampleRate = sr
        nativeEngine.setSampleRate(sr)
        graphicEq.setSampleRate(sr)
        parametricEq.setSampleRate(sr)
        centerChannelProcessor.setSampleRate(sr)
        spectrumAnalyzer.setSampleRate(sr)
    }

    fun getSampleRate(): Float = sampleRate

    fun getEngineDetails(): String {
        return if (nativeEngine.isAvailable) {
            nativeEngine.getVersion()
        } else {
            "Studio 32-Bit Float Managed Engine"
        }
    }

    fun isBitPerfectEligible(): Boolean {
        return !dspEnabled
    }

    fun setPreampGain(gainDb: Float) {
        preamp.setGainDb(gainDb)
        nativeEngine.setPreampGain(gainDb)
    }

    fun setGraphicBandGain(band: Int, gainDb: Float) {
        graphicEq.setBandGain(band, gainDb)
        nativeEngine.setGraphicBandGain(band, gainDb)
    }

    fun setParametricBand(band: Int, enabled: Boolean, type: BiquadType, freq: Float, gainDb: Float, q: Float) {
        parametricEq.configureBand(band, enabled, type, freq, gainDb, q)
        nativeEngine.setParametricBand(band, enabled, type.ordinal, freq, gainDb, q)
    }

    fun setVocalFocus(mode: Int, amount: Float) {
        centerChannelProcessor.configure(mode, amount)
        nativeEngine.setVocalFocus(mode, amount)
    }

    fun hasClipped(): Boolean {
        return if (nativeEngine.isAvailable) nativeEngine.hasClipped() else preamp.hasClipped()
    }

    fun resetClipping() {
        nativeEngine.resetClipping()
        preamp.resetClipping()
    }

    /**
     * Process an interleaved 32-bit float stereo buffer in place.
     * Automatically dispatches to SIMD-accelerated C++20 engine if available.
     */
    fun processInterleavedStereo(samples: FloatArray, frameCount: Int) {
        val sampleCount = frameCount * 2

        if (dspEnabled) {
            if (nativeEngine.isAvailable) {
                // Native C++ processing
                nativeEngine.processInterleavedStereo(samples, frameCount)
            } else {
                // Managed fallback processing
                var i = 0
                while (i < sampleCount) {
                    preamp.processFrame(samples, i)
                    graphicEq.processFrame(samples, i)
                    parametricEq.processFrame(samples, i)
                    centerChannelProcessor.processFrame(samples, i)
                    i += 2
                }
            }
        }

        // Tap for real-time spectrum visualization
        spectrumAnalyzer.feedSamples(samples, sampleCount)
    }

    /**
     * Computes the combined frequency response curve in dB at frequency f.
     */
    fun evaluateCombinedCurveDb(f: Float): Float {
        if (!dspEnabled) return 0.0f
        var totalDb = preamp.getGainDb()
        totalDb += graphicEq.evaluateMagnitudeDb(f)
        totalDb += parametricEq.evaluateMagnitudeDb(f)
        return totalDb
    }

    fun reset() {
        preamp.setGainDb(0.0f)
        preamp.resetClipping()
        graphicEq.reset()
        parametricEq.reset()
        centerChannelProcessor.reset()
        centerChannelProcessor.configure(CenterChannelProcessor.MODE_OFF, 0.0f)
        nativeEngine.setPreampGain(0.0f)
        nativeEngine.setVocalFocus(0, 0.0f)
        nativeEngine.setDspEnabled(false)
        for (i in 0 until 10) {
            nativeEngine.setGraphicBandGain(i, 0.0f)
        }
        for (i in 0 until 12) {
            nativeEngine.setParametricBand(i, false, 0, 1000f, 0f, 0.707f)
        }
    }
}
