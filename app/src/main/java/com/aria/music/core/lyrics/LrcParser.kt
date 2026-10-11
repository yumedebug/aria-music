package com.aria.music.core.lyrics

/** One line of lyrics, optionally anchored to a playback position. */
data class LyricLine(
    val timeMs: Long,
    val text: String,
)

data class Lyrics(
    val lines: List<LyricLine>,
    val synced: Boolean,
) {
    val isEmpty: Boolean get() = lines.isEmpty()

    /** Index of the line that should be highlighted for [positionMs], or -1. */
    fun activeIndex(positionMs: Long): Int {
        if (!synced || lines.isEmpty()) return -1
        var low = 0
        var high = lines.size - 1
        var result = -1
        while (low <= high) {
            val mid = (low + high) / 2
            if (lines[mid].timeMs <= positionMs) {
                result = mid
                low = mid + 1
            } else {
                high = mid - 1
            }
        }
        return result
    }

    companion object {
        val EMPTY = Lyrics(emptyList(), synced = false)
    }
}

/**
 * Parses LRC-formatted lyrics (also handles the plain-text lyrics that many files embed,
 * simply flagging them as unsynced).
 *
 * Supported constructs:
 * - `[mm:ss]`, `[mm:ss.xx]`, `[mm:ss.xxx]` timestamps, several per line
 * - `[hh:mm:ss.xx]` for long tracks
 * - `[offset:+/-ms]` metadata
 * - `[ar:]`, `[ti:]`, `[al:]`, `[by:]` metadata (ignored)
 */
object LrcParser {

    private val timeTagRegex =
        Regex("""\[(?:(\d{1,3}):)?(\d{1,2}):(\d{1,2})(?:[.:](\d{1,3}))?]""")
    private val metadataRegex = Regex("""^\[(ar|ti|al|by|length|re|ve):.*]$""", RegexOption.IGNORE_CASE)
    private val offsetRegex = Regex("""^\[offset:\s*([+-]?\d+)\s*]$""", RegexOption.IGNORE_CASE)

    fun parse(raw: String?): Lyrics {
        if (raw.isNullOrBlank()) return Lyrics.EMPTY

        var offsetMs = 0L
        val timed = mutableListOf<LyricLine>()
        val untimed = mutableListOf<String>()

        for (rawLine in raw.lines()) {
            val line = rawLine.trim().removePrefix("\uFEFF")
            if (line.isEmpty()) continue

            offsetRegex.find(line)?.let { match ->
                offsetMs = match.groupValues[1].toLongOrNull() ?: 0L
                return@let
            }
            if (metadataRegex.matches(line)) continue

            val matches = timeTagRegex.findAll(line).toList()
            val text = line.substringAfterLast(']').trim()
            if (matches.isEmpty()) {
                if (text.isNotEmpty()) untimed += text
                continue
            }
            for (match in matches) {
                val hours = match.groupValues[1].toLongOrNull() ?: 0L
                val minutes = match.groupValues[2].toLongOrNull() ?: 0L
                val seconds = match.groupValues[3].toLongOrNull() ?: 0L
                val fractionRaw = match.groupValues[4]
                val fractionMs = when (fractionRaw.length) {
                    0 -> 0L
                    1 -> fractionRaw.toLong() * 100
                    2 -> fractionRaw.toLong() * 10
                    else -> fractionRaw.take(3).toLong()
                }
                val timeMs = hours * 3_600_000L + minutes * 60_000L +
                    seconds * 1000L + fractionMs
                timed += LyricLine(timeMs, text)
            }
        }

        return when {
            timed.isNotEmpty() -> Lyrics(
                lines = timed
                    .sortedBy { it.timeMs }
                    .map { it.copy(timeMs = (it.timeMs - offsetMs).coerceAtLeast(0L)) },
                synced = true,
            )
            untimed.isNotEmpty() -> Lyrics(untimed.map { LyricLine(0L, it) }, synced = false)
            else -> Lyrics.EMPTY
        }
    }
}
