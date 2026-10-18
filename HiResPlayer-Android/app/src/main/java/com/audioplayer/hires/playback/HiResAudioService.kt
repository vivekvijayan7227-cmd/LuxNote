package com.audioplayer.hires.playback

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Binder
import android.os.Build
import android.os.IBinder
import com.audioplayer.hires.MainActivity
import com.audioplayer.hires.model.PlayerStatus
import com.audioplayer.hires.model.Track

/**
 * Dedicated Android Foreground Playback Service.
 * Features:
 * - High-priority background playback that survives screen lock, doze, and process backgrounding
 * - Native Android MediaSession for lock-screen controls, Bluetooth headsets, and Android Auto
 * - Rich Notification with MediaStyle and interactive transport controls (Prev, Play/Pause, Next)
 * - Audio Focus management (pauses for incoming calls and notifications)
 * - Becoming-Noisy receiver (automatically pauses when headphones or Bluetooth disconnect)
 */
class HiResAudioService : Service() {

    companion object {
        const val CHANNEL_ID = "hires_studio_playback_channel"
        const val NOTIFICATION_ID = 1001

        const val ACTION_PLAY = "com.audioplayer.hires.ACTION_PLAY"
        const val ACTION_PAUSE = "com.audioplayer.hires.ACTION_PAUSE"
        const val ACTION_PREV = "com.audioplayer.hires.ACTION_PREV"
        const val ACTION_NEXT = "com.audioplayer.hires.ACTION_NEXT"
        const val ACTION_STOP = "com.audioplayer.hires.ACTION_STOP"
        const val ACTION_SYNC_STATE = "com.audioplayer.hires.ACTION_SYNC_STATE"

        var instance: HiResAudioService? = null
            private set

        var onTransportAction: ((String) -> Unit)? = null
    }

    private lateinit var mediaSession: MediaSession
    private lateinit var audioManager: AudioManager

    private var currentTrack: Track? = null
    private var isPlaying: Boolean = false

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): HiResAudioService = this@HiResAudioService
    }

    private fun dispatchTransport(action: String) {
        onTransportAction?.invoke(action)
        try {
            PlaybackManager.getInstance(applicationContext).handleTransportAction(action)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                // Auto-pause when headphones unplugged or Bluetooth disconnected
                dispatchTransport(ACTION_PAUSE)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
        initMediaSession()

        val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        registerReceiver(noisyReceiver, filter)

        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
            serviceWakeLock = pm?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "HiResStudio:ServiceWakeLock")
            serviceWakeLock?.setReferenceCounted(false)
        } catch (_: Exception) {}
    }

    private var serviceWakeLock: android.os.PowerManager.WakeLock? = null

    fun isWakeLockHeld(): Boolean = serviceWakeLock?.isHeld == true

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        when (action) {
            ACTION_PLAY -> dispatchTransport(ACTION_PLAY)
            ACTION_PAUSE -> {
                dispatchTransport(ACTION_PAUSE)
                return START_STICKY
            }
            ACTION_PREV -> dispatchTransport(ACTION_PREV)
            ACTION_NEXT -> dispatchTransport(ACTION_NEXT)
            ACTION_STOP -> {
                dispatchTransport(ACTION_STOP)
                stopForegroundService()
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_SYNC_STATE -> {
                // Internal sync from PlaybackManager/ViewModel: DO NOT dispatch back to avoid loop
            }
        }

        val trackTitle = intent?.getStringExtra("EXTRA_TITLE")
        val isPlayingExtra = intent?.getBooleanExtra("EXTRA_PLAYING", true) ?: true
        val posExtra = intent?.getLongExtra("EXTRA_POSITION", 0L) ?: 0L

        if (!trackTitle.isNullOrBlank()) {
            val track = Track(
                id = intent.getLongExtra("EXTRA_TRACK_ID", 0L),
                title = trackTitle,
                artist = intent.getStringExtra("EXTRA_ARTIST") ?: "Unknown Artist",
                album = intent.getStringExtra("EXTRA_ALBUM") ?: "Unknown Album",
                durationMs = intent.getLongExtra("EXTRA_DURATION", 0L),
                sampleRate = 44100,
                bitDepth = 24,
                format = intent.getStringExtra("EXTRA_FORMAT") ?: "FLAC",
                bitrateKbps = 1411,
                uriString = ""
            )
            updatePlaybackState(track, if (isPlayingExtra) PlayerStatus.PLAYING else PlayerStatus.PAUSED, posExtra)
        } else if (currentTrack != null) {
            updatePlaybackState(currentTrack, if (isPlaying) PlayerStatus.PLAYING else PlayerStatus.PAUSED, 0L)
        } else {
            // Guarantee immediate startForeground so Android foreground service constraints are always met
            val defaultTrack = Track(
                id = 0L,
                title = "LuxNote Audio Engine",
                artist = "Ready for playback",
                album = "LuxNote",
                durationMs = 0L,
                sampleRate = 44100,
                bitDepth = 24,
                format = "Hi-Res",
                bitrateKbps = 1411,
                uriString = ""
            )
            val notification = buildNotification(defaultTrack, false)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    private fun initMediaSession() {
        mediaSession = MediaSession(this, "HiResStudioAudioSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    dispatchTransport(ACTION_PLAY)
                }

                override fun onPause() {
                    dispatchTransport(ACTION_PAUSE)
                }

                override fun onSkipToNext() {
                    dispatchTransport(ACTION_NEXT)
                }

                override fun onSkipToPrevious() {
                    dispatchTransport(ACTION_PREV)
                }

                override fun onStop() {
                    dispatchTransport(ACTION_STOP)
                    stopForegroundService()
                }

                override fun onSeekTo(pos: Long) {
                    dispatchTransport("SEEK:$pos")
                }
            })

            val state = PlaybackState.Builder()
                .setActions(
                    PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_STOP or
                    PlaybackState.ACTION_SEEK_TO
                )
                .setState(PlaybackState.STATE_STOPPED, 0L, 1.0f)
                .build()

            setPlaybackState(state)
            isActive = true
        }
    }

    /**
     * Centralized audio focus delegation via PlaybackManager.audioFocusManager.
     */
    fun requestAudioFocus(): Boolean {
        return try {
            PlaybackManager.getInstance(applicationContext).audioFocusManager.requestAudioFocus(isPlaying)
        } catch (_: Exception) { true }
    }

    fun abandonAudioFocus() {
        try {
            PlaybackManager.getInstance(applicationContext).audioFocusManager.abandonAudioFocus()
        } catch (_: Exception) {}
    }

    /**
     * Update state and notification when track or playback status changes.
     */
    fun updatePlaybackState(track: Track?, status: PlayerStatus, positionMs: Long) {
        currentTrack = track
        isPlaying = status == PlayerStatus.PLAYING

        if (track == null || status == PlayerStatus.STOPPED) {
            val state = PlaybackState.Builder()
                .setState(PlaybackState.STATE_STOPPED, 0L, 1.0f)
                .build()
            mediaSession.setPlaybackState(state)
            stopForegroundService()
            return
        }

        // 1. Update MediaSession State & Metadata
        val stateInt = if (isPlaying) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED
        val playbackState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                PlaybackState.ACTION_PAUSE or
                PlaybackState.ACTION_PLAY_PAUSE or
                PlaybackState.ACTION_SKIP_TO_NEXT or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_STOP or
                PlaybackState.ACTION_SEEK_TO
            )
            .setState(stateInt, positionMs, 1.0f)
            .build()
        mediaSession.setPlaybackState(playbackState)

        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadata.METADATA_KEY_DURATION, track.durationMs)
            .build()
        mediaSession.setMetadata(metadata)

        // 2. Build MediaStyle Notification
        val notification = buildNotification(track, isPlaying)

        // 3. Promote to Foreground Service safely
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (isPlaying) {
            requestAudioFocus()
            try {
                if (serviceWakeLock?.isHeld != true) {
                    serviceWakeLock?.acquire(12 * 60 * 60 * 1000L)
                }
            } catch (_: Exception) {}
        } else {
            try {
                if (serviceWakeLock?.isHeld == true) {
                    serviceWakeLock?.release()
                }
            } catch (_: Exception) {}
        }
    }

    private fun buildNotification(track: Track, playing: Boolean): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            this, 0, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val prevIntent = Intent(this, HiResAudioService::class.java).apply { action = ACTION_PREV }
        val prevPending = PendingIntent.getService(this, 1, prevIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIntent = Intent(this, HiResAudioService::class.java).apply { action = if (playing) ACTION_PAUSE else ACTION_PLAY }
        val playPausePending = PendingIntent.getService(this, 2, playPauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val nextIntent = Intent(this, HiResAudioService::class.java).apply { action = ACTION_NEXT }
        val nextPending = PendingIntent.getService(this, 3, nextIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val stopIntent = Intent(this, HiResAudioService::class.java).apply { action = ACTION_STOP }
        val stopPending = PendingIntent.getService(this, 4, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val playPauseIcon = if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle(track.title)
            .setContentText("${track.artist} • ${track.album}")
            .setSubText(track.formatBadge)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(contentPendingIntent)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setOngoing(playing)
            .addAction(android.R.drawable.ic_media_previous, "Previous", prevPending)
            .addAction(playPauseIcon, if (playing) "Pause" else "Play", playPausePending)
            .addAction(android.R.drawable.ic_media_next, "Next", nextPending)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
            .setStyle(
                Notification.MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
    }

    private fun stopForegroundService() {
        abandonAudioFocus()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "LuxNote Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Controls and ongoing playback notification for LuxNote"
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        try {
            unregisterReceiver(noisyReceiver)
        } catch (_: Exception) {}
        mediaSession.release()
        abandonAudioFocus()
        try {
            if (serviceWakeLock?.isHeld == true) {
                serviceWakeLock?.release()
            }
        } catch (_: Exception) {}
    }
}
