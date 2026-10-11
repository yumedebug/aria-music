package com.aria.music.core

import java.util.Locale

/** Formats a millisecond duration as `m:ss` (or `h:mm:ss` for long tracks). */
fun formatDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSeconds = ms / 1000L
    val seconds = totalSeconds % 60L
    val minutes = (totalSeconds / 60L) % 60L
    val hours = totalSeconds / 3600L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

/** Total listening time shown on shelf headers, e.g. `3 hr 12 min`. */
fun formatTotalDuration(ms: Long): String {
    val minutes = ms / 60000L
    return when {
        minutes <= 0L -> "0 min"
        minutes < 60L -> "$minutes min"
        else -> "${minutes / 60L} hr ${minutes % 60L} min"
    }
}
