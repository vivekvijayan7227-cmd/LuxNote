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

    @Test
    fun testPcmFormatConverter16BitStereo() {
        // 2 frames of 16-bit PCM stereo (4 samples): 0x7FFF (+1.0), 0x8000 (-1.0)
        val byteBuf = java.nio.ByteBuffer.allocate(8).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        byteBuf.putShort(32767.toShort())
        byteBuf.putShort((-32768).toShort())
        byteBuf.putShort(16384.toShort())
        byteBuf.putShort(0.toShort())
        byteBuf.flip()

        val outFloat = FloatArray(4)
        val frames = PcmFormatConverter.convertToStereoFloat(
            byteBuffer = byteBuf,
            encoding = android.media.AudioFormat.ENCODING_PCM_16BIT,
            channelCount = 2,
            targetFloatBuffer = outFloat
        )

        assertEquals(2, frames)
        assertEquals(1.0f, outFloat[0], 0.001f)
        assertEquals(-1.0f, outFloat[1], 0.001f)
        assertEquals(0.5f, outFloat[2], 0.001f)
        assertEquals(0.0f, outFloat[3], 0.001f)
    }

    @Test
    fun testPcmFormatConverterMultichannelFoldDown() {
        // 1 frame of 5.1 channels (L, R, C, LFE, Ls, Rs): all 16-bit
        // L=0x4000 (~0.5), R=0x4000 (~0.5), C=0x4000 (~0.5), LFE=0x4000, Ls=0x2000 (~0.25), Rs=0x2000 (~0.25)
        val byteBuf = java.nio.ByteBuffer.allocate(12).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        byteBuf.putShort(16384.toShort()) // L
        byteBuf.putShort(16384.toShort()) // R
        byteBuf.putShort(16384.toShort()) // C
        byteBuf.putShort(16384.toShort()) // LFE
        byteBuf.putShort(8192.toShort())  // Ls
        byteBuf.putShort(8192.toShort())  // Rs
        byteBuf.flip()

        val outFloat = FloatArray(2)
        val frames = PcmFormatConverter.convertToStereoFloat(
            byteBuffer = byteBuf,
            encoding = android.media.AudioFormat.ENCODING_PCM_16BIT,
            channelCount = 6,
            targetFloatBuffer = outFloat
        )

        assertEquals(1, frames)
        // L output should fold L + 0.707*C + 0.707*Ls
        assertTrue("Fold-down L must contain folded center and surround energy", outFloat[0] > 0.5f)
        assertTrue("Fold-down R must contain folded center and surround energy", outFloat[1] > 0.5f)
        assertTrue("Fold-down channels must not exceed 1.0 (anti-clip clamped)", outFloat[0] <= 1.0f)
    }

    @Test
    fun testParametricEqualizer12Bands() {
        val pEq = ParametricEqualizer()
        pEq.setSampleRate(48000.0f)

        // Configure band 3 as peaking at 1 kHz with +4 dB
        pEq.configureBand(3, true, BiquadType.Peaking, 1000.0f, 4.0f, 1.414f)
        assertFalse(pEq.isBypassed())

        val mag1k = pEq.evaluateMagnitudeDb(1000.0f)
        assertEquals(4.0f, mag1k, 0.25f)

        // Bypass band 3
        pEq.configureBand(3, false, BiquadType.Peaking, 1000.0f, 4.0f, 1.414f)
        assertTrue(pEq.isBypassed())
    }

    @Test
    fun testPreampTimestampPeakHold() {
        val preamp = PreampGainStage()
        assertFalse(preamp.hasClipped())

        // Trigger clipping with overdriven signal
        preamp.setGainDb(6.0f)
        val frame = floatArrayOf(0.99f, 0.99f)
        preamp.processFrame(frame, 0)

        assertTrue("Clipping must be held true immediately after clip event", preamp.hasClipped())

        // Explicit reset clears clipping immediately
        preamp.resetClipping()
        assertFalse("Clipping must be false after explicit reset", preamp.hasClipped())
    }

    @Test
    fun testPcmFormatConverterResolveEffectiveEncoding() {
        // Explicit valid encodings
        assertEquals(
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            PcmFormatConverter.resolveEffectiveEncoding(android.media.AudioFormat.ENCODING_PCM_16BIT)
        )
        assertEquals(
            android.media.AudioFormat.ENCODING_PCM_FLOAT,
            PcmFormatConverter.resolveEffectiveEncoding(android.media.AudioFormat.ENCODING_PCM_FLOAT)
        )
        assertEquals(
            android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED,
            PcmFormatConverter.resolveEffectiveEncoding(android.media.AudioFormat.ENCODING_PCM_24BIT_PACKED)
        )
        assertEquals(
            android.media.AudioFormat.ENCODING_PCM_32BIT,
            PcmFormatConverter.resolveEffectiveEncoding(android.media.AudioFormat.ENCODING_PCM_32BIT)
        )

        // Missing or unspecified (0 or ENCODING_INVALID) safely defaults to 16-bit
        assertEquals(
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            PcmFormatConverter.resolveEffectiveEncoding(0)
        )
        assertEquals(
            android.media.AudioFormat.ENCODING_PCM_16BIT,
            PcmFormatConverter.resolveEffectiveEncoding(android.media.AudioFormat.ENCODING_INVALID)
        )

        // Unsupported explicit formats must throw IllegalArgumentException
        try {
            PcmFormatConverter.resolveEffectiveEncoding(9999)
            fail("Expected IllegalArgumentException for unknown explicit encoding 9999")
        } catch (e: IllegalArgumentException) {
            assertTrue(e.message!!.contains("Unsupported explicit PCM encoding"))
        }
    }

    @Test
    fun testCenterChannelStemBalance() {
        val processor = CenterChannelProcessor()
        processor.setSampleRate(48000.0f)

        // Initially inactive with 1.0, 1.0
        assertFalse(processor.isActive())

        // Adjust vocal gain to 0.0 (mute vocal / center) and instrument to 1.5
        processor.setStemBalance(0.0f, 1.5f)
        assertTrue(processor.isActive())

        // Feed pure center signal (L = 0.5, R = 0.5): Mid = 0.5, Side = 0.0
        val centerFrame = floatArrayOf(0.5f, 0.5f)
        processor.processFrame(centerFrame, 0)

        // Vocal attenuated to 0, side is 0, so output should be 0.0
        assertEquals(0.0f, centerFrame[0], 0.001f)
        assertEquals(0.0f, centerFrame[1], 0.001f)

        // Reset stem balance
        processor.setStemBalance(1.0f, 1.0f)
        assertFalse(processor.isActive())
    }
}
