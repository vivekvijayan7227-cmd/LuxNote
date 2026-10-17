package com.audioplayer.hires.audio

import android.media.AudioFormat
import java.nio.ByteBuffer

/**
 * Format-Aware PCM Conversion & Downmixing Layer.
 * Converts any decoder output representation (16-bit, 24-bit, 32-bit int, 32-bit float, 8-bit)
 * and channel topology (mono, stereo, 5.1, 7.1) into standard 32-bit float stereo [-1.0, 1.0].
 */
object PcmFormatConverter {

    fun getBytesPerSample(encoding: Int): Int {
        return when (encoding) {
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            21 -> 3 // AudioFormat.ENCODING_PCM_24BIT_PACKED
            22 -> 4 // AudioFormat.ENCODING_PCM_32BIT
            AudioFormat.ENCODING_PCM_8BIT -> 1
            else -> 2 // Default standard 16-bit
        }
    }

    /**
     * Converts raw PCM bytes from a decoder ByteBuffer into normalized interleaved stereo float buffer.
     * @return Number of stereo frames converted.
     */
    fun convertToStereoFloat(
        byteBuffer: ByteBuffer,
        encoding: Int,
        channelCount: Int,
        targetFloatBuffer: FloatArray
    ): Int {
        val channels = channelCount.coerceAtLeast(1)
        val bytesPerSample = getBytesPerSample(encoding)
        val frameSizeBytes = bytesPerSample * channels
        val remainingBytes = byteBuffer.remaining()
        val frames = remainingBytes / frameSizeBytes
        if (frames <= 0) return 0

        val requiredFloats = frames * 2
        if (targetFloatBuffer.size < requiredFloats) return 0

        when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> {
                decodeFloat(byteBuffer, channels, frames, targetFloatBuffer)
            }
            21 -> { // 24-bit packed
                decode24BitPacked(byteBuffer, channels, frames, targetFloatBuffer)
            }
            22 -> { // 32-bit int
                decode32BitInt(byteBuffer, channels, frames, targetFloatBuffer)
            }
            AudioFormat.ENCODING_PCM_8BIT -> {
                decode8Bit(byteBuffer, channels, frames, targetFloatBuffer)
            }
            else -> { // Default: 16-bit PCM
                decode16Bit(byteBuffer, channels, frames, targetFloatBuffer)
            }
        }

        return frames
    }

    private fun decode16Bit(
        buf: ByteBuffer,
        channels: Int,
        frames: Int,
        out: FloatArray
    ) {
        val norm = 1.0f / 32768.0f
        if (channels == 1) {
            for (f in 0 until frames) {
                val s = buf.short * norm
                out[f * 2] = s
                out[f * 2 + 1] = s
            }
        } else if (channels == 2) {
            for (f in 0 until frames) {
                out[f * 2] = buf.short * norm
                out[f * 2 + 1] = buf.short * norm
            }
        } else {
            // Multichannel folddown (L, R, C, LFE, Ls, Rs)
            for (f in 0 until frames) {
                val l = buf.short * norm
                val r = buf.short * norm
                val c = if (channels > 2) buf.short * norm else 0f
                val lfe = if (channels > 3) buf.short * norm else 0f
                val ls = if (channels > 4) buf.short * norm else 0f
                val rs = if (channels > 5) buf.short * norm else 0f
                for (extra in 6 until channels) buf.short

                out[f * 2] = (l + 0.707f * c + 0.707f * ls) * 0.7f
                out[f * 2 + 1] = (r + 0.707f * c + 0.707f * rs) * 0.7f
            }
        }
    }

    private fun decodeFloat(
        buf: ByteBuffer,
        channels: Int,
        frames: Int,
        out: FloatArray
    ) {
        if (channels == 1) {
            for (f in 0 until frames) {
                val s = buf.float
                out[f * 2] = s
                out[f * 2 + 1] = s
            }
        } else if (channels == 2) {
            for (f in 0 until frames) {
                out[f * 2] = buf.float
                out[f * 2 + 1] = buf.float
            }
        } else {
            for (f in 0 until frames) {
                val l = buf.float
                val r = buf.float
                val c = if (channels > 2) buf.float else 0f
                val lfe = if (channels > 3) buf.float else 0f
                val ls = if (channels > 4) buf.float else 0f
                val rs = if (channels > 5) buf.float else 0f
                for (extra in 6 until channels) buf.float

                out[f * 2] = (l + 0.707f * c + 0.707f * ls) * 0.7f
                out[f * 2 + 1] = (r + 0.707f * c + 0.707f * rs) * 0.7f
            }
        }
    }

    private fun decode24BitPacked(
        buf: ByteBuffer,
        channels: Int,
        frames: Int,
        out: FloatArray
    ) {
        val norm = 1.0f / 8388608.0f
        if (channels == 1) {
            for (f in 0 until frames) {
                val b0 = buf.get().toInt() and 0xFF
                val b1 = buf.get().toInt() and 0xFF
                val b2 = buf.get().toInt() // sign-extended
                val s = ((b2 shl 16) or (b1 shl 8) or b0) * norm
                out[f * 2] = s
                out[f * 2 + 1] = s
            }
        } else if (channels == 2) {
            for (f in 0 until frames) {
                val l0 = buf.get().toInt() and 0xFF
                val l1 = buf.get().toInt() and 0xFF
                val l2 = buf.get().toInt()
                val r0 = buf.get().toInt() and 0xFF
                val r1 = buf.get().toInt() and 0xFF
                val r2 = buf.get().toInt()
                out[f * 2] = ((l2 shl 16) or (l1 shl 8) or l0) * norm
                out[f * 2 + 1] = ((r2 shl 16) or (r1 shl 8) or r0) * norm
            }
        } else {
            for (f in 0 until frames) {
                val l0 = buf.get().toInt() and 0xFF
                val l1 = buf.get().toInt() and 0xFF
                val l2 = buf.get().toInt()
                val r0 = buf.get().toInt() and 0xFF
                val r1 = buf.get().toInt() and 0xFF
                val r2 = buf.get().toInt()
                val l = ((l2 shl 16) or (l1 shl 8) or l0) * norm
                val r = ((r2 shl 16) or (r1 shl 8) or r0) * norm

                var c = 0f
                if (channels > 2) {
                    val c0 = buf.get().toInt() and 0xFF
                    val c1 = buf.get().toInt() and 0xFF
                    val c2 = buf.get().toInt()
                    c = ((c2 shl 16) or (c1 shl 8) or c0) * norm
                }
                var ls = 0f
                var rs = 0f
                for (extra in 3 until channels) {
                    val x0 = buf.get().toInt() and 0xFF
                    val x1 = buf.get().toInt() and 0xFF
                    val x2 = buf.get().toInt()
                    val v = ((x2 shl 16) or (x1 shl 8) or x0) * norm
                    if (extra == 4) ls = v
                    if (extra == 5) rs = v
                }
                out[f * 2] = (l + 0.707f * c + 0.707f * ls) * 0.7f
                out[f * 2 + 1] = (r + 0.707f * c + 0.707f * rs) * 0.7f
            }
        }
    }

    private fun decode32BitInt(
        buf: ByteBuffer,
        channels: Int,
        frames: Int,
        out: FloatArray
    ) {
        val norm = 1.0f / 2147483648.0f
        if (channels == 1) {
            for (f in 0 until frames) {
                val s = buf.int * norm
                out[f * 2] = s
                out[f * 2 + 1] = s
            }
        } else if (channels == 2) {
            for (f in 0 until frames) {
                out[f * 2] = buf.int * norm
                out[f * 2 + 1] = buf.int * norm
            }
        } else {
            for (f in 0 until frames) {
                val l = buf.int * norm
                val r = buf.int * norm
                val c = if (channels > 2) buf.int * norm else 0f
                val lfe = if (channels > 3) buf.int * norm else 0f
                val ls = if (channels > 4) buf.int * norm else 0f
                val rs = if (channels > 5) buf.int * norm else 0f
                for (extra in 6 until channels) buf.int

                out[f * 2] = (l + 0.707f * c + 0.707f * ls) * 0.7f
                out[f * 2 + 1] = (r + 0.707f * c + 0.707f * rs) * 0.7f
            }
        }
    }

    private fun decode8Bit(
        buf: ByteBuffer,
        channels: Int,
        frames: Int,
        out: FloatArray
    ) {
        val norm = 1.0f / 128.0f
        if (channels == 1) {
            for (f in 0 until frames) {
                val s = ((buf.get().toInt() and 0xFF) - 128) * norm
                out[f * 2] = s
                out[f * 2 + 1] = s
            }
        } else if (channels == 2) {
            for (f in 0 until frames) {
                out[f * 2] = ((buf.get().toInt() and 0xFF) - 128) * norm
                out[f * 2 + 1] = ((buf.get().toInt() and 0xFF) - 128) * norm
            }
        } else {
            for (f in 0 until frames) {
                val l = ((buf.get().toInt() and 0xFF) - 128) * norm
                val r = ((buf.get().toInt() and 0xFF) - 128) * norm
                for (extra in 2 until channels) buf.get()
                out[f * 2] = l
                out[f * 2 + 1] = r
            }
        }
    }
}
