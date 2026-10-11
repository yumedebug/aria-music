package com.aria.music.data.media

import android.content.Context
import android.net.Uri
import android.util.LruCache
import com.aria.music.core.lyrics.Id3Lyrics
import com.aria.music.core.lyrics.LrcParser
import com.aria.music.core.lyrics.Lyrics
import com.aria.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Resolves lyrics for a track from the file's own metadata.
 *
 * There is deliberately no network lyrics provider: this build only shows lyrics that are
 * genuinely present in the user's files (an ID3 `USLT` frame, which may itself contain
 * timestamped LRC text). When nothing is embedded, the UI shows an explicit empty state.
 */
class LyricsRepository(private val context: Context) {

    private val cache = LruCache<String, Lyrics>(CACHE_SIZE)

    suspend fun load(track: Track): Lyrics {
        val key = "${track.id}:${track.uri}"
        cache.get(key)?.let { return it }
        val lyrics = withContext(Dispatchers.IO) { readLyrics(track) }
        cache.put(key, lyrics)
        return lyrics
    }

    private fun readLyrics(track: Track): Lyrics = runCatching {
        context.contentResolver.openInputStream(Uri.parse(track.uri))?.use { stream ->
            val buffer = ByteArray(MAX_BYTES)
            var read = 0
            while (read < MAX_BYTES) {
                val count = stream.read(buffer, read, MAX_BYTES - read)
                if (count <= 0) break
                read += count
            }
            LrcParser.parse(Id3Lyrics.extract(buffer.copyOf(read)))
        } ?: Lyrics.EMPTY
    }.getOrDefault(Lyrics.EMPTY)

    /** True when the track's filesystem metadata contains any lyrics at all. */
    suspend fun hasLyrics(track: Track): Boolean = !load(track).isEmpty

    private companion object {
        const val MAX_BYTES = 1_500_000
        const val CACHE_SIZE = 24
    }
}
