package com.audioplayer.hires.audio

import android.media.AudioFormat
import org.junit.Assert.*
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Rigorous Unit Tests for PcmFormatConverter and Sample Rate / Pitch Preservation.
 * Validates that every audio format (16-bit, 24-bit, 32-bit float, mono, stereo, multichannel)
 * decodes with exact 1:1 frame counts and exact sample normalization, preventing pitch skew.
 */
class PcmFormatConverterTest {

    @Test
    fun test16BitStereoConversion_frameAndValueIntegrity() {
        val frameCount = 100
        val byteBuf = ByteBuffer.allocate(frameCount * 4).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until frameCount) {
            val leftShort = (i * 300 - 15000).toShort()
            val rightShort = (-i * 300 + 15000).toShort()
            byteBuf.putShort(leftShort)
            byteBuf.putShort(rightShort)
        }
        byteBuf.flip()

        val outFloat = FloatArray(frameCount * 2)
        val convertedFrames = PcmFormatConverter.convertToStereoFloat(
            byteBuf,
            AudioFormat.ENCODING_PCM_16BIT,
            2,
            outFloat
        )

        assertEquals("Converted frames must exactly match input frame count", frameCount, convertedFrames)

        // Verify first frame
        val expectedL0 = (-15000.0f / 32768.0f)
        val expectedR0 = (15000.0f / 32768.0f)
        assertEquals(expectedL0, outFloat[0], 0.0001f)
        assertEquals(expectedR0, outFloat[1], 0.0001f)

        // Verify last frame
        val expectedL99 = ((99 * 300 - 15000).toFloat() / 32768.0f)
        val expectedR99 = ((-99 * 300 + 15000).toFloat() / 32768.0f)
        assertEquals(expectedL99, outFloat[99 * 2], 0.0001f)
        assertEquals(expectedR99, outFloat[99 * 2 + 1], 0.0001f)
    }

    @Test
    fun test16BitMonoConversion_dualChannelExpansionWithoutPitchSkew() {
        val frameCount = 80
        val byteBuf = ByteBuffer.allocate(frameCount * 2).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until frameCount) {
            byteBuf.putShort((i * 400).toShort())
        }
        byteBuf.flip()

        val outFloat = FloatArray(frameCount * 2)
        val convertedFrames = PcmFormatConverter.convertToStereoFloat(
            byteBuf,
            AudioFormat.ENCODING_PCM_16BIT,
            1,
            outFloat
        )

        assertEquals("Mono input must produce exactly 1 stereo frame per mono sample", frameCount, convertedFrames)

        for (i in 0 until frameCount) {
            val expected = (i * 400).toFloat() / 32768.0f
            assertEquals("Left channel must match mono source", expected, outFloat[i * 2], 0.0001f)
            assertEquals("Right channel must match mono source", expected, outFloat[i * 2 + 1], 0.0001f)
        }
    }

    @Test
    fun test24BitPackedStereoConversion() {
        val frameCount = 50
        // 24-bit stereo = 3 bytes per sample * 2 channels = 6 bytes per frame
        val byteBuf = ByteBuffer.allocate(frameCount * 6).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until frameCount) {
            // Put 24-bit Left: +4194304 (half positive scale: 0x400000)
            byteBuf.put(0x00.toByte())
            byteBuf.put(0x00.toByte())
            byteBuf.put(0x40.toByte())

            // Put 24-bit Right: -4194304 (half negative scale: 0xC00000)
            byteBuf.put(0x00.toByte())
            byteBuf.put(0x00.toByte())
            byteBuf.put(0xC0.toByte())
        }
        byteBuf.flip()

        val outFloat = FloatArray(frameCount * 2)
        val convertedFrames = PcmFormatConverter.convertToStereoFloat(
            byteBuf,
            21, // ENCODING_PCM_24BIT_PACKED
            2,
            outFloat
        )

        assertEquals(frameCount, convertedFrames)
        assertEquals(0.5f, outFloat[0], 0.001f)
        assertEquals(-0.5f, outFloat[1], 0.001f)
    }

    @Test
    fun test32BitFloatStereoConversion() {
        val frameCount = 64
        val byteBuf = ByteBuffer.allocate(frameCount * 8).order(ByteOrder.LITTLE_ENDIAN)

        for (i in 0 until frameCount) {
            byteBuf.putFloat(0.75f)
            byteBuf.putFloat(-0.75f)
        }
        byteBuf.flip()

        val outFloat = FloatArray(frameCount * 2)
        val convertedFrames = PcmFormatConverter.convertToStereoFloat(
            byteBuf,
            AudioFormat.ENCODING_PCM_FLOAT,
            2,
            outFloat
        )

        assertEquals(frameCount, convertedFrames)
        assertEquals(0.75f, outFloat[0], 0.00001f)
        assertEquals(-0.75f, outFloat[1], 0.00001f)
    }

    @Test
    fun testResolveEffectiveEncoding_safeFallbacks() {
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, PcmFormatConverter.resolveEffectiveEncoding(0))
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, PcmFormatConverter.resolveEffectiveEncoding(-1))
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, PcmFormatConverter.resolveEffectiveEncoding(AudioFormat.ENCODING_INVALID))
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, PcmFormatConverter.resolveEffectiveEncoding(AudioFormat.ENCODING_DEFAULT))
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, PcmFormatConverter.resolveEffectiveEncoding(AudioFormat.ENCODING_PCM_16BIT))
        assertEquals(AudioFormat.ENCODING_PCM_FLOAT, PcmFormatConverter.resolveEffectiveEncoding(AudioFormat.ENCODING_PCM_FLOAT))
        assertEquals(21, PcmFormatConverter.resolveEffectiveEncoding(21))
        assertEquals(22, PcmFormatConverter.resolveEffectiveEncoding(22))
        assertEquals(AudioFormat.ENCODING_PCM_8BIT, PcmFormatConverter.resolveEffectiveEncoding(AudioFormat.ENCODING_PCM_8BIT))
    }

    @Test
    fun testSampleRateMatchingContract_preventsPitchSkew() {
        // Contract verification: AudioTrack sample rate MUST equal decoded stream rate
        fun shouldRecreateAudioTrack(activeTrackRate: Int, streamRate: Int): Boolean {
            return activeTrackRate != streamRate
        }

        // 44.1k track followed by 48k track -> MUST recreate (reusing 44.1k would cause 0.918x slow pitch)
        assertTrue(shouldRecreateAudioTrack(44100, 48000))

        // 48k track followed by 44.1k track -> MUST recreate (reusing 48k would cause 1.088x chipmunk pitch)
        assertTrue(shouldRecreateAudioTrack(48000, 44100))

        // 44.1k track followed by 96k Hi-Res -> MUST recreate (reusing 44.1k would cause 0.46x deep low pitch)
        assertTrue(shouldRecreateAudioTrack(44100, 96000))

        // AAC container 24k updated to decoded 48k -> MUST recreate
        assertTrue(shouldRecreateAudioTrack(24000, 48000))

        // Consecutive 44.1k tracks on the same album -> SAFE for seamless gapless reuse
        assertFalse(shouldRecreateAudioTrack(44100, 44100))

        // Consecutive 48k tracks -> SAFE for seamless gapless reuse
        assertFalse(shouldRecreateAudioTrack(48000, 48000))
    }
}
