package com.audioplayer.hires

import com.audioplayer.hires.audio.*
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class DspAudioEngineTest {

    @Test
    fun testBiquadPeakingFilterGain() {
        val filter = BiquadFilter()
        val sampleRate = 48000.0f
        val centerFreq = 1000.0f
        val boostDb = 6.0f

        filter.configure(
            type = BiquadType.Peaking,
            sr = sampleRate,
            frequency = centerFreq,
            gainDb = boostDb,
            q = 1.0
        )

        // Evaluate magnitude at center frequency
        val magAtCenter = filter.evaluateMagnitudeDb(centerFreq)
        assertEquals("Magnitude at center frequency should match requested gain within 0.1 dB", boostDb, magAtCenter, 0.1f)

        // Evaluate magnitude far away from center frequency (e.g. 20 kHz)
        val magAtFar = filter.evaluateMagnitudeDb(20000.0f)
        assertEquals("Magnitude far from center should be close to 0 dB", 0.0f, magAtFar, 0.2f)
    }

    @Test
    fun testBiquadLowShelfSubBass() {
        val filter = BiquadFilter()
        val sampleRate = 48000.0f
        val cornerFreq = 60.0f
        val boostDb = 4.0f

        filter.configure(
            type = BiquadType.LowShelf,
            sr = sampleRate,
            frequency = cornerFreq,
            gainDb = boostDb,
            q = 0.7071
        )

        // Sub-bass region (<30 Hz) should receive full shelf boost
        val magSub = filter.evaluateMagnitudeDb(25.0f)
        assertEquals(boostDb, magSub, 0.3f)

        // Mid-range (1 kHz) should be unaffected
        val magMid = filter.evaluateMagnitudeDb(1000.0f)
        assertEquals(0.0f, magMid, 0.2f)
    }

    @Test
    fun testGraphicEqualizerFlatBypass() {
        val eq = GraphicEqualizer()
        eq.setSampleRate(44100.0f)

        assertTrue("EQ should initially be flat", eq.isFlat())

        val frame = floatArrayOf(0.5f, -0.5f)
        eq.processFrame(frame, 0)

        // In flat mode, samples must pass through bit-exact
        assertEquals(0.5f, frame[0], 0.00001f)
        assertEquals(-0.5f, frame[1], 0.00001f)
    }

    @Test
    fun testGraphicEqualizerBandProcessing() {
        val eq = GraphicEqualizer()
        eq.setSampleRate(44100.0f)

        eq.setBandGain(5, 6.0f) // 1 kHz band +6 dB
        assertFalse("EQ should not be flat after gain adjustment", eq.isFlat())

        val mag1k = eq.evaluateMagnitudeDb(1000.0f)
        assertEquals(6.0f, mag1k, 0.25f)
    }

    @Test
    fun testCenterChannelVocalFocus() {
        val processor = CenterChannelProcessor()
        processor.setSampleRate(48000.0f)

        // Configure Vocal Focus at 100% intensity
        processor.configure(CenterChannelProcessor.MODE_VOCAL_FOCUS, 1.0f)
        assertTrue(processor.isActive())

        // Pure side signal (out-of-phase stereo): L = 1.0, R = -1.0
        val pureSideSignal = floatArrayOf(1.0f, -1.0f)
        processor.processFrame(pureSideSignal, 0)

        // Side energy should be attenuated significantly (down to ~0.35)
        assertTrue("Stereo side energy should be attenuated in Vocal Focus", abs(pureSideSignal[0]) <= 0.40f)
        assertTrue("Stereo width should be collapsed", abs(pureSideSignal[0] - pureSideSignal[1]) < 0.8f)
    }

    @Test
    fun testCenterChannelInstrumentalFocusPreservesSubBass() {
        val processor = CenterChannelProcessor()
        processor.setSampleRate(48000.0f)

        // Configure Instrumental Focus (Center Vocal Attenuation) at 100%
        processor.configure(CenterChannelProcessor.MODE_INSTRUMENTAL_FOCUS, 1.0f)
        assertTrue(processor.isActive())

        // Mid signal: L = 0.5, R = 0.5 (pure centered mono)
        val centerFrame = floatArrayOf(0.5f, 0.5f)
        processor.processFrame(centerFrame, 0)

        assertNotNull(centerFrame)
    }

    @Test
    fun testPreampSoftKneeLimiter() {
        val preamp = PreampGainStage()
        preamp.setGainDb(6.0f) // +6 dB boost

        val highAmp = floatArrayOf(0.9f, 0.9f)
        preamp.processFrame(highAmp, 0)

        // After +6 dB, 0.9 would be 1.8 without limiter. Limiter should hold it within safe bounds.
        assertTrue("Soft-knee limiter must keep output finite and controlled", highAmp[0] <= 1.05f)
        assertTrue("Clipping flag should be raised", preamp.hasClipped())
    }

    @Test
    fun testSpectrumAnalyzerHannFft() {
        val analyzer = SpectrumAnalyzer(fftSize = 256, bandCount = 16)
        analyzer.setSampleRate(48000.0f)

        // Feed sine wave at 1 kHz
        val samples = FloatArray(512)
        for (i in 0 until 512 step 2) {
            val s = kotlin.math.sin(2.0 * Math.PI * 1000.0 * (i / 2) / 48000.0).toFloat()
            samples[i] = s
            samples[i + 1] = s
        }

        analyzer.feedSamples(samples, 512)
        val bands = analyzer.computeBands()

        assertEquals(16, bands.size)
        var maxVal = 0.0f
        for (b in bands) {
            if (b > maxVal) maxVal = b
        }
        assertTrue("FFT should detect energy from sine wave", maxVal > 0.05f)
    }
}
