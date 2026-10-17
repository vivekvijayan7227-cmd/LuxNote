package com.audioplayer.hires.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaExtractor
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.audioplayer.hires.audio.AudioDiagnostics
import com.audioplayer.hires.model.Track
import com.audioplayer.hires.playback.HiResAudioService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream

data class PermissionStatus(
    val permissionName: String,
    val isGranted: Boolean,
    val isRequired: Boolean,
    val description: String
)

data class StorageAudit(
    val mediaStoreAudioCount: Int,
    val folderAudits: List<FolderAudit>,
    val internalImportedCount: Int
)

data class FolderAudit(
    val path: String,
    val exists: Boolean,
    val isReadable: Boolean,
    val audioFileCount: Int
)

data class DiagnosticsReport(
    val androidVersion: String,
    val deviceModel: String,
    val permissions: List<PermissionStatus>,
    val storageAudit: StorageAudit,
    val floatAudioTrackSupported: Boolean,
    val pcm16AudioTrackSupported: Boolean,
    val serviceActive: Boolean,
    val wakeLockActive: Boolean,
    val recentLogs: List<String>
) {
    fun toFormattedText(): String {
        val sb = StringBuilder()
        sb.appendLine("==============================================")
        sb.appendLine("  LUXNOTE SYSTEM & AUDIO DIAGNOSTICS REPORT   ")
        sb.appendLine("==============================================")
        sb.appendLine("Device: $deviceModel ($androidVersion)")
        sb.appendLine("Service Live: $serviceActive | WakeLock: $wakeLockActive")
        sb.appendLine("Float AudioTrack: $floatAudioTrackSupported | 16-bit PCM: $pcm16AudioTrackSupported")
        sb.appendLine("\n[PERMISSIONS]")
        for (p in permissions) {
            val status = if (p.isGranted) "GRANTED" else "DENIED"
            sb.appendLine("  - ${p.permissionName}: $status (${p.description})")
        }
        sb.appendLine("\n[LOCAL STORAGE AUDIT]")
        sb.appendLine("  - MediaStore Audio Items: ${storageAudit.mediaStoreAudioCount}")
        sb.appendLine("  - App Internal Storage Files: ${storageAudit.internalImportedCount}")
        for (f in storageAudit.folderAudits) {
            val read = if (f.isReadable) "Readable" else "Unreadable"
            val ex = if (f.exists) "Exists" else "Missing"
            sb.appendLine("  - ${f.path}: $ex, $read (${f.audioFileCount} audio files)")
        }
        sb.appendLine("\n[RECENT AUDIO LOGS]")
        for (l in recentLogs.takeLast(25)) {
            sb.appendLine("  $l")
        }
        sb.appendLine("==============================================")
        return sb.toString()
    }
}

class DiagnosticsManager(private val context: Context) {

    suspend fun runFullDiagnostics(): DiagnosticsReport = withContext(Dispatchers.IO) {
        val permissions = mutableListOf<PermissionStatus>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = context.checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
            permissions.add(PermissionStatus("READ_MEDIA_AUDIO", granted, true, "Access audio files on Android 13+"))

            val notifGranted = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            permissions.add(PermissionStatus("POST_NOTIFICATIONS", notifGranted, false, "Show background playback controls"))
        } else {
            val granted = context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            permissions.add(PermissionStatus("READ_EXTERNAL_STORAGE", granted, true, "Access storage on Android 12 and below"))
        }

        permissions.add(
            PermissionStatus(
                "STORAGE_ACCESS_FRAMEWORK",
                true,
                false,
                "Standard Android Scoped Storage & MediaStore (Privacy-First)"
            )
        )

        // Storage Audit
        var mediaStoreCount = 0
        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media._ID),
                "${MediaStore.Audio.Media.DURATION} >= 5000",
                null,
                null
            )
            cursor?.use { mediaStoreCount = it.count }
        } catch (_: Exception) {}

        val audioExtensions = setOf("mp3", "flac", "wav", "m4a", "aac", "ogg", "opus")
        val candidatePaths = listOf(
            "/storage/emulated/0/Music",
            "/storage/emulated/0/Download",
            "/storage/emulated/0/Audio",
            "/storage/emulated/0/Songs",
            "/storage/emulated/0/Podcasts",
            "/storage/emulated/0/Recordings"
        )
        val folderAudits = mutableListOf<FolderAudit>()
        for (path in candidatePaths) {
            val dir = File(path)
            val exists = dir.exists()
            val readable = dir.canRead()
            var count = 0
            if (exists && readable) {
                try {
                    count = dir.walkTopDown()
                        .maxDepth(3)
                        .filter { it.isFile && it.extension.lowercase() in audioExtensions }
                        .count()
                } catch (_: Exception) {}
            }
            folderAudits.add(FolderAudit(path, exists, readable, count))
        }

        val internalDir = File(context.filesDir, "imported_tracks")
        val internalCount = if (internalDir.exists()) {
            internalDir.listFiles()?.count { it.isFile } ?: 0
        } else 0

        // AudioTrack Float check
        var floatSupported = false
        try {
            val minBuf = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_FLOAT)
            floatSupported = minBuf > 0
        } catch (_: Exception) {}

        // AudioTrack 16-bit check
        var pcm16Supported = false
        try {
            val minBuf = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT)
            pcm16Supported = minBuf > 0
        } catch (_: Exception) {}

        DiagnosticsReport(
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            deviceModel = "${Build.MANUFACTURER.uppercase()} ${Build.MODEL}",
            permissions = permissions,
            storageAudit = StorageAudit(mediaStoreCount, folderAudits, internalCount),
            floatAudioTrackSupported = floatSupported,
            pcm16AudioTrackSupported = pcm16Supported,
            serviceActive = HiResAudioService.instance != null,
            wakeLockActive = (HiResAudioService.instance?.isWakeLockHeld() == true) || (com.audioplayer.hires.audio.AudioTrackPlayer.instance?.isWakeLockHeld() == true),
            recentLogs = AudioDiagnostics.logs.toList()
        )
    }

    suspend fun runPlaybackSelfTest(track: Track): String = withContext(Dispatchers.IO) {
        val sb = StringBuilder()
        sb.appendLine("Testing track: ${track.title} (${track.format})")
        sb.appendLine("URI: ${track.uriString}")

        val file = File(track.uriString)
        if (file.exists()) {
            sb.appendLine("Physical File Exists: YES (Size: ${file.length()} bytes, canRead=${file.canRead()})")
        } else {
            sb.appendLine("Physical File Exists: NO (Resolved via Android ContentProvider)")
        }

        // Test MediaExtractor
        try {
            val extractor = MediaExtractor()
            if (track.uriString.startsWith("content://")) {
                extractor.setDataSource(context, android.net.Uri.parse(track.uriString), null)
                sb.appendLine("MediaExtractor Content URI Test: SUCCESS")
            } else if (file.exists() && file.canRead()) {
                extractor.setDataSource(file.absolutePath)
                sb.appendLine("MediaExtractor File Path Test: SUCCESS")
            } else {
                extractor.setDataSource(context, android.net.Uri.parse(track.uriString), null)
                sb.appendLine("MediaExtractor URI Fallback Test: SUCCESS")
            }
            sb.appendLine("MediaExtractor Track Count: ${extractor.trackCount}")
            extractor.release()
        } catch (e: Exception) {
            sb.appendLine("MediaExtractor Test ERROR: ${e.message}")
        }

        sb.toString()
    }
}
