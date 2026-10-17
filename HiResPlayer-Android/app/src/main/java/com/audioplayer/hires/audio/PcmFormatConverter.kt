package com.audioplayer.hires.audio

import android.media.AudioFormat
import java.nio.ByteBuffer

/**
 * Format-Aware PCM Conversion & Downmixing Layer.
 * Converts any decoder output representation (16-bit, 24-bit, 32-bit int, 32-bit float, 8-bit)
 * and channel topology (mono, stereo, 5.1, 7.1) into standard 32-bit float stereo [-1.0, 1.0].
 */
object PcmFormatConverter {

    class UnsupportedPcmEncodingException(
        val encoding: Int,
        val channelCount: Int,
        message: String = "Unsupported explicit PCM encoding: $encoding (channels: $channelCount)"
    ) : IllegalArgumentException(message)

    /**
     * Resolves effective PCM encoding.
     * When encoding is 0, AudioFormat.ENCODING_INVALID, or AudioFormat.ENCODING_DEFAULT,
     * it safely defaults to AudioFormat.ENCODING_PCM_16BIT per Android MediaCodec specification.
     * Explicit unsupported encodings throw UnsupportedPcmEncodingException.
     */
    fun resolveEffectiveEncoding(encoding: Int, channels: Int = 2): Int {
        return when (encoding) {
            0, AudioFormat.ENCODING_INVALID, AudioFormat.ENCODING_DEFAULT -> AudioFormat.ENCODING_PCM_16BIT
            AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.ENCODING_PCM_FLOAT,
            21, // AudioFormat.ENCODING_PCM_24BIT_PACKED
            22, // AudioFormat.ENCODING_PCM_32BIT
            AudioFormat.ENCODING_PCM_8BIT -> encoding
            else -> throw UnsupportedPcmEncodingException(encoding, channels)
        }
    }

    fun getBytesPerSample(encoding: Int): Int {
        val effective = resolveEffectiveEncoding(encoding)
        return when (effective) {
            AudioFormat.ENCODING_PCM_16BIT -> 2
            AudioFormat.ENCODING_PCM_FLOAT -> 4
            21 -> 3 // AudioFormat.ENCODING_PCM_24BIT_PACKED
            22 -> 4 // AudioFormat.ENCODING_PCM_32BIT
            AudioFormat.ENCODING_PCM_8BIT -> 1
            else -> 2
        }
    }

    /**
     * Converts raw PCM bytes from a decoder ByteBuffer into normalized interleaved stereo float buffer.
     * @return Number of stereo frames converted.
     * @throws UnsupportedPcmEncodingException if explicit encoding is unsupported.
     */
    fun convertToStereoFloat(
        byteBuffer: ByteBuffer,
        encoding: Int,
        channelCount: Int,
        targetFloatBuffer: FloatArray
    ): Int {
        val channels = channelCount.coerceAtLeast(1)
        val effectiveEncoding = resolveEffectiveEncoding(encoding, channels)
        val bytesPerSample = getBytesPerSample(effectiveEncoding)
        val frameSizeBytes = bytesPerSample * channels
        val remainingBytes = byteBuffer.remaining()
        val frames = remainingBytes / frameSizeBytes
        if (frames <= 0) return 0

        val requiredFloats = frames * 2
        if (targetFloatBuffer.size < requiredFloats) return 0

        when (effectiveEncoding) {
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
            AudioFormat.ENCODING_PCM_16BIT -> {
                decode16Bit(byteBuffer, channels, frames, targetFloatBuffer)
            }
            else -> return 0
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
                val s = (buf.short * norm).coerceIn(-1.0f, 1.0f)
                out[f * 2] = s
                out[f * 2 + 1] = s
            }
        } else if (channels == 2) {
            for (f in 0 until frames) {
                out[f * 2] = (buf.short * norm).coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = (buf.short * norm).coerceIn(-1.0f, 1.0f)
            }
        } else if (channels == 6) {
            // ITU-R BS.775 standard 5.1 folddown (L, R, C, LFE, Ls, Rs)
            for (f in 0 until frames) {
                val l = buf.short * norm
                val r = buf.short * norm
                val c = buf.short * norm
                buf.short // LFE ignored in main folddown to prevent bass clipping
                val ls = buf.short * norm
                val rs = buf.short * norm

                val lMix = (l + 0.7071f * c + 0.7071f * ls) * 0.7071f
                val rMix = (r + 0.7071f * c + 0.7071f * rs) * 0.7071f
                out[f * 2] = lMix.coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = rMix.coerceIn(-1.0f, 1.0f)
            }
        } else if (channels == 8) {
            // ITU-R BS.775 standard 7.1 folddown (L, R, C, LFE, Ls, Rs, Lb, Rb)
            for (f in 0 until frames) {
                val l = buf.short * norm
                val r = buf.short * norm
                val c = buf.short * norm
                buf.short // LFE
                val ls = buf.short * norm
                val rs = buf.short * norm
                val lb = buf.short * norm
                val rb = buf.short * norm

                val lMix = (l + 0.7071f * c + 0.7071f * ls + 0.5f * lb) * 0.62f
                val rMix = (r + 0.7071f * c + 0.7071f * rs + 0.5f * rb) * 0.62f
                out[f * 2] = lMix.coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = rMix.coerceIn(-1.0f, 1.0f)
            }
        } else {
            for (f in 0 until frames) {
                val l = buf.short * norm
                val r = buf.short * norm
                val c = if (channels > 2) buf.short * norm else 0f
                for (extra in 3 until channels) buf.short

                val scale = (1.0f / kotlin.math.sqrt(channels.toFloat())).coerceIn(0.4f, 1.0f)
                out[f * 2] = ((l + 0.7071f * c) * scale).coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = ((r + 0.7071f * c) * scale).coerceIn(-1.0f, 1.0f)
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
                val s = buf.float.coerceIn(-1.0f, 1.0f)
                out[f * 2] = s
                out[f * 2 + 1] = s
            }
        } else if (channels == 2) {
            for (f in 0 until frames) {
                out[f * 2] = buf.float.coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = buf.float.coerceIn(-1.0f, 1.0f)
            }
        } else if (channels == 6) {
            for (f in 0 until frames) {
                val l = buf.float
                val r = buf.float
                val c = buf.float
                buf.float // LFE
                val ls = buf.float
                val rs = buf.float

                val lMix = (l + 0.7071f * c + 0.7071f * ls) * 0.7071f
                val rMix = (r + 0.7071f * c + 0.7071f * rs) * 0.7071f
                out[f * 2] = lMix.coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = rMix.coerceIn(-1.0f, 1.0f)
            }
        } else if (channels == 8) {
            for (f in 0 until frames) {
                val l = buf.float
                val r = buf.float
                val c = buf.float
                buf.float // LFE
                val ls = buf.float
                val rs = buf.float
                val lb = buf.float
                val rb = buf.float

                val lMix = (l + 0.7071f * c + 0.7071f * ls + 0.5f * lb) * 0.62f
                val rMix = (r + 0.7071f * c + 0.7071f * rs + 0.5f * rb) * 0.62f
                out[f * 2] = lMix.coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = rMix.coerceIn(-1.0f, 1.0f)
            }
        } else {
            for (f in 0 until frames) {
                val l = buf.float
                val r = buf.float
                val c = if (channels > 2) buf.float else 0f
                for (extra in 3 until channels) buf.float

                val scale = (1.0f / kotlin.math.sqrt(channels.toFloat())).coerceIn(0.4f, 1.0f)
                out[f * 2] = ((l + 0.7071f * c) * scale).coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = ((r + 0.7071f * c) * scale).coerceIn(-1.0f, 1.0f)
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
        } else if (channels == 6) {
            for (f in 0 until frames) {
                val l0 = buf.get().toInt() and 0xFF
                val l1 = buf.get().toInt() and 0xFF
                val l2 = buf.get().toInt()
                val r0 = buf.get().toInt() and 0xFF
                val r1 = buf.get().toInt() and 0xFF
                val r2 = buf.get().toInt()
                val l = ((l2 shl 16) or (l1 shl 8) or l0) * norm
                val r = ((r2 shl 16) or (r1 shl 8) or r0) * norm

                val c0 = buf.get().toInt() and 0xFF
                val c1 = buf.get().toInt() and 0xFF
                val c2 = buf.get().toInt()
                val c = ((c2 shl 16) or (c1 shl 8) or c0) * norm

                buf.get(); buf.get(); buf.get() // LFE

                val ls0 = buf.get().toInt() and 0xFF
                val ls1 = buf.get().toInt() and 0xFF
                val ls2 = buf.get().toInt()
                val ls = ((ls2 shl 16) or (ls1 shl 8) or ls0) * norm

                val rs0 = buf.get().toInt() and 0xFF
                val rs1 = buf.get().toInt() and 0xFF
                val rs2 = buf.get().toInt()
                val rs = ((rs2 shl 16) or (rs1 shl 8) or rs0) * norm

                val lMix = (l + 0.7071f * c + 0.7071f * ls) * 0.7071f
                val rMix = (r + 0.7071f * c + 0.7071f * rs) * 0.7071f
                out[f * 2] = lMix.coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = rMix.coerceIn(-1.0f, 1.0f)
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
                for (extra in 3 until channels) {
                    buf.get(); buf.get(); buf.get()
                }
                val scale = (1.0f / kotlin.math.sqrt(channels.toFloat())).coerceIn(0.4f, 1.0f)
                out[f * 2] = ((l + 0.7071f * c) * scale).coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = ((r + 0.7071f * c) * scale).coerceIn(-1.0f, 1.0f)
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
                val s = (buf.int * norm).coerceIn(-1.0f, 1.0f)
                out[f * 2] = s
                out[f * 2 + 1] = s
            }
        } else if (channels == 2) {
            for (f in 0 until frames) {
                out[f * 2] = (buf.int * norm).coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = (buf.int * norm).coerceIn(-1.0f, 1.0f)
            }
        } else if (channels == 6) {
            for (f in 0 until frames) {
                val l = buf.int * norm
                val r = buf.int * norm
                val c = buf.int * norm
                buf.int // LFE
                val ls = buf.int * norm
                val rs = buf.int * norm

                val lMix = (l + 0.7071f * c + 0.7071f * ls) * 0.7071f
                val rMix = (r + 0.7071f * c + 0.7071f * rs) * 0.7071f
                out[f * 2] = lMix.coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = rMix.coerceIn(-1.0f, 1.0f)
            }
        } else if (channels == 8) {
            for (f in 0 until frames) {
                val l = buf.int * norm
                val r = buf.int * norm
                val c = buf.int * norm
                buf.int // LFE
                val ls = buf.int * norm
                val rs = buf.int * norm
                val lb = buf.int * norm
                val rb = buf.int * norm

                val lMix = (l + 0.7071f * c + 0.7071f * ls + 0.5f * lb) * 0.62f
                val rMix = (r + 0.7071f * c + 0.7071f * rs + 0.5f * rb) * 0.62f
                out[f * 2] = lMix.coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = rMix.coerceIn(-1.0f, 1.0f)
            }
        } else {
            for (f in 0 until frames) {
                val l = buf.int * norm
                val r = buf.int * norm
                val c = if (channels > 2) buf.int * norm else 0f
                for (extra in 3 until channels) buf.int

                val scale = (1.0f / kotlin.math.sqrt(channels.toFloat())).coerceIn(0.4f, 1.0f)
                out[f * 2] = ((l + 0.7071f * c) * scale).coerceIn(-1.0f, 1.0f)
                out[f * 2 + 1] = ((r + 0.7071f * c) * scale).coerceIn(-1.0f, 1.0f)
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
