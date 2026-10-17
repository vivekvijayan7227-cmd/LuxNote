package com.audioplayer.hires.model

data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val sampleRate: Int = 44100,
    val bitDepth: Int = 16,
    val format: String = "FLAC",
    val bitrateKbps: Int = 1411,
    val uriString: String = "",
    val artworkUri: String? = null
) {
    val formatBadge: String
        get() = "$format $bitDepth-bit / ${sampleRate / 1000} kHz • $bitrateKbps kbps"
}

enum class PlayerStatus {
    STOPPED,
    PLAYING,
    PAUSED
}

enum class RepeatMode {
    OFF,
    ALL,
    ONE
}

enum class SortOption(val label: String) {
    TITLE_ASC("Title (A-Z)"),
    ARTIST_ASC("Artist"),
    ALBUM_ASC("Album"),
    DURATION_DESC("Longest First"),
    DATE_ADDED("Recently Added")
}

data class Playlist(
    val id: String,
    val name: String,
    val trackIds: List<Long> = emptyList()
)

data class StudioQueue(
    val id: String,
    val name: String,
    val trackIds: List<Long> = emptyList(),
    val currentIndex: Int = 0,
    val positionMs: Long = 0L,
    val isShuffle: Boolean = false
)

data class LyricLine(
    val timestampMs: Long,
    val text: String
)

data class TrackBookmark(
    val id: String,
    val trackId: Long,
    val timestampMs: Long,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class MusicFolder(
    val path: String,
    val name: String,
    val trackCount: Int
)

data class PlaybackInfo(
    val currentTrack: Track? = null,
    val status: PlayerStatus = PlayerStatus.STOPPED,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isBitPerfect: Boolean = true,
    val dspActive: Boolean = false,
    val volume: Float = 1.0f
)

data class AudioOutputDevice(
    val name: String,
    val type: String, // "Internal Speaker", "Wired Headphones", "Bluetooth LDAC", "USB Audio DAC"
    val isUsbDac: Boolean = false,
    val isBitPerfectCapable: Boolean = true,
    val supportedSampleRates: List<Int> = listOf(44100, 48000, 96000, 192000),
    val currentSampleRate: Int = 44100,
    val currentBitDepth: Int = 24
)

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%02d:%02d", minutes, seconds)
}
