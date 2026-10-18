package com.audioplayer.hires.audio

/**
 * Session-scoped mailbox; a seek from track A cannot bleed into track B.
 * Safe for control-thread writes and playback-coroutine reads.
 *
 * Use the active session ID from AudioTrackPlayer.currentSessionId.
 */
class SessionSeekMailbox {
    data class Seek(val sessionId: Long, val positionMs: Long)
    private var pending: Seek? = null

    @Synchronized
    fun request(sessionId: Long, positionMs: Long) {
        require(positionMs >= 0L)
        pending = Seek(sessionId, positionMs)
    }

    @Synchronized
    fun takeFor(sessionId: Long): Seek? {
        val request = pending
        pending = null
        return request?.takeIf { it.sessionId == sessionId }
    }

    @Synchronized
    fun clear() {
        pending = null
    }
}
