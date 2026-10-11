package com.aria.music.playback

import com.aria.music.data.model.Track

/** Immutable snapshot of everything the player UI needs, exposed as a single StateFlow. */
data class PlaybackState(
    val connected: Boolean = false,
    val current: Track? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeatMode: Int = 0,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = 0,
) {
    val hasNext: Boolean get() = currentIndex in 0 until queue.lastIndex
    val hasPrevious: Boolean get() = currentIndex > 0
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
