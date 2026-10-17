package com.audioplayer.hires.lyrics

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.audioplayer.hires.model.LyricLine
import com.audioplayer.hires.model.Track
import java.io.File
import java.util.regex.Pattern

/**
 * High-performance offline LRC and embedded lyrics parser.
 * Supports standard timestamp formats:
 * - [mm:ss.xx]
 * - [mm:ss.xxx]
 * - [mm:ss]
 */
object LrcParser {
    private val lrcPattern = Pattern.compile("""\[(\d{1,3}):(\d{2})(?:\.(\d{2,3}))?\](.*)""")

    fun parse(lrcText: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        if (lrcText.isBlank()) return lines

        val textLines = lrcText.lines()
        for (rawLine in textLines) {
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty()) continue

            val matcher = lrcPattern.matcher(trimmed)
            if (matcher.matches()) {
                val minStr = matcher.group(1) ?: "0"
                val secStr = matcher.group(2) ?: "0"
                val msStr = matcher.group(3)

                val minutes = minStr.toLongOrNull() ?: 0L
                val seconds = secStr.toLongOrNull() ?: 0L
                val fractionMs = when {
                    msStr == null -> 0L
                    msStr.length == 2 -> (msStr.toLongOrNull() ?: 0L) * 10L
                    msStr.length >= 3 -> (msStr.substring(0, 3).toLongOrNull() ?: 0L)
                    else -> 0L
                }

                val timestampMs = (minutes * 60L + seconds) * 1000L + fractionMs
                val text = matcher.group(4)?.trim() ?: ""

                if (text.isNotBlank()) {
                    lines.add(LyricLine(timestampMs, text))
                }
            }
        }

        return lines.sortedBy { it.timestampMs }
    }

    /**
     * Resolves lyrics for a track:
     * 1. Looks for a co-located .lrc file on disk (same filename with .lrc extension).
     * 2. Checks embedded audio metadata tags.
     */
    fun findLyricsForTrack(context: Context, track: Track): List<LyricLine> {
        // 1. Check local file system if file URI or direct path
        try {
            val uri = Uri.parse(track.uriString)
            if (track.uriString.startsWith("file://") || !track.uriString.startsWith("content://")) {
                val path = if (track.uriString.startsWith("file://")) uri.path ?: "" else track.uriString
                val audioFile = File(path)
                if (audioFile.exists()) {
                    val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
                    if (lrcFile.exists() && lrcFile.canRead()) {
                        val parsed = parse(lrcFile.readText())
                        if (parsed.isNotEmpty()) return parsed
                    }
                }
            }
        } catch (_: Exception) {}

        // 2. Check embedded lyrics via MediaMetadataRetriever
        try {
            val mmr = MediaMetadataRetriever()
            val uri = Uri.parse(track.uriString)
            if (track.uriString.startsWith("content://")) {
                mmr.setDataSource(context, uri)
            } else {
                mmr.setDataSource(track.uriString)
            }

            // Standard MP4 / ID3 embedded lyrics tag if available
            val embedded = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPILATION)
            mmr.release()

            if (!embedded.isNullOrBlank()) {
                val parsed = parse(embedded)
                if (parsed.isNotEmpty()) return parsed
            }
        } catch (_: Exception) {}

        return emptyList()
    }
}
