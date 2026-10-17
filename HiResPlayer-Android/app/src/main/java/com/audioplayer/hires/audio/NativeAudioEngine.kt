package com.audioplayer.hires.audio

import android.util.Log

/**
 * High-Resolution Native C++20 Studio DSP Engine Bridge.
 * Loads `libhires_audio.so` to execute SIMD-accelerated 32-bit float biquad filtering,
 * Mid/Side vocal focus stage, and 1024-point Radix-2 FFT spectrum analysis directly in native space.
 */
class NativeAudioEngine {

    companion object {
        private const val TAG = "NativeAudioEngine"
        var isLibraryLoaded = false
            private set

        init {
            try {
                System.loadLibrary("hires_audio")
                isLibraryLoaded = true
                try { Log.i(TAG, "Native library 'libhires_audio.so' loaded successfully.") } catch (_: Throwable) {}
            } catch (t: Throwable) {
                isLibraryLoaded = false
                try { Log.w(TAG, "Native library 'libhires_audio.so' could not be loaded: ${t.message}") } catch (_: Throwable) {}
            }
        }
    }

    private var nativeHandle: Long = 0L

    init {
        if (isLibraryLoaded) {
            try {
                nativeHandle = createEngine()
            } catch (t: Throwable) {
                Log.e(TAG, "Error initializing native engine instance: ${t.message}")
                nativeHandle = 0L
            }
        }
    }

    val isAvailable: Boolean
        get() = isLibraryLoaded && nativeHandle != 0L

    fun getVersion(): String {
        return if (isAvailable) {
            try {
                getEngineVersion()
            } catch (_: Throwable) {
                "libhires_audio.so (Native C++20)"
            }
        } else {
            "Managed Kotlin 32-Bit Float Engine (Fallback)"
        }
    }

    fun setSampleRate(sampleRate: Float) {
        if (isAvailable) setSampleRate(nativeHandle, sampleRate)
    }

    fun setGraphicBandGain(band: Int, gainDb: Float) {
        if (isAvailable) setGraphicBandGain(nativeHandle, band, gainDb)
    }

    fun setParametricBand(band: Int, enabled: Boolean, type: Int, freq: Float, gainDb: Float, q: Float) {
        if (isAvailable) setParametricBand(nativeHandle, band, enabled, type, freq, gainDb, q)
    }

    fun setPreampGain(gainDb: Float) {
        if (isAvailable) setPreampGain(nativeHandle, gainDb)
    }

    fun setVocalFocus(mode: Int, amount: Float) {
        if (isAvailable) setVocalFocus(nativeHandle, mode, amount)
    }

    fun setStemBalance(vocalGain: Float, instrumentGain: Float) {
        if (isAvailable) setStemBalance(nativeHandle, vocalGain, instrumentGain)
    }

    fun setDspEnabled(enabled: Boolean) {
        if (isAvailable) setDspEnabled(nativeHandle, enabled)
    }

    fun processInterleavedStereo(buffer: FloatArray, frameCount: Int) {
        if (isAvailable) processInterleavedStereo(nativeHandle, buffer, frameCount)
    }

    fun computeFftBands(outBands: FloatArray, bandCount: Int) {
        if (isAvailable) computeFftBands(nativeHandle, outBands, bandCount)
    }

    fun hasClipped(): Boolean {
        return if (isAvailable) hasClipped(nativeHandle) else false
    }

    fun resetClipping() {
        if (isAvailable) resetClipping(nativeHandle)
    }

    fun release() {
        if (nativeHandle != 0L) {
            destroyEngine(nativeHandle)
            nativeHandle = 0L
        }
    }

    protected fun finalize() {
        release()
    }

    // Native JNI Declarations
    private external fun createEngine(): Long
    private external fun destroyEngine(handle: Long)
    private external fun setSampleRate(handle: Long, sampleRate: Float)
    private external fun setGraphicBandGain(handle: Long, band: Int, gainDb: Float)
    private external fun setParametricBand(handle: Long, band: Int, enabled: Boolean, type: Int, freq: Float, gainDb: Float, q: Float)
    private external fun setPreampGain(handle: Long, gainDb: Float)
    private external fun setVocalFocus(handle: Long, mode: Int, amount: Float)
    private external fun setStemBalance(handle: Long, vocalGain: Float, instrumentGain: Float)
    private external fun setDspEnabled(handle: Long, enabled: Boolean)
    private external fun processInterleavedStereo(handle: Long, buffer: FloatArray, frameCount: Int)
    private external fun computeFftBands(handle: Long, outBands: FloatArray, bandCount: Int)
    private external fun hasClipped(handle: Long): Boolean
    private external fun resetClipping(handle: Long)
    private external fun getEngineVersion(): String
}
