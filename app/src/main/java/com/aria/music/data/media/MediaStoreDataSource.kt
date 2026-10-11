package com.aria.music.data.media

import android.content.ContentResolver
import android.content.Context
import android.provider.MediaStore
import com.aria.music.data.model.Album
import com.aria.music.data.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the device's local audio library through [MediaStore].
 *
 * This is the source of truth for "what can I play" in this build: every track is a
 * real, on-device audio file, so playback, artwork, seeking and the queue all operate
 * on genuine data instead of placeholders.
 */
class MediaStoreDataSource(private val context: Context) {

    private val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.ALBUM,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR,
    )

    suspend fun loadTracks(): List<Track> = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND " +
            "${MediaStore.Audio.Media.DURATION} > 0"
        val sortOrder = "${MediaStore.Audio.Media.ARTIST} COLLATE NOCASE ASC, " +
            "${MediaStore.Audio.Media.ALBUM} COLLATE NOCASE ASC, " +
            "${MediaStore.Audio.Media.TRACK} ASC"

        val tracks = ArrayList<Track>()
        runCatching {
            resolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder,
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val albumId = cursor.getLong(albumIdCol)
                    tracks += Track(
                        id = id,
                        title = cursor.getString(titleCol)?.takeIf { it.isNotBlank() } ?: "Unknown Title",
                        artist = cursor.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" }
                            ?: "Unknown Artist",
                        album = cursor.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Unknown Album",
                        albumId = albumId,
                        durationMs = cursor.getLong(durationCol),
                        uri = android.content.ContentUris.withAppendedId(
                            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                            id,
                        ).toString(),
                        trackNumber = cursor.getInt(trackCol) % 1000,
                        year = cursor.getInt(yearCol),
                    )
                }
            }
        }
        tracks
    }

    fun groupIntoAlbums(tracks: List<Track>): List<Album> =
        tracks.groupBy { it.albumId.takeIf { id -> id > 0 } ?: it.album.hashCode().toLong() }
            .map { (albumId, albumTracks) ->
                val sorted = albumTracks.sortedBy { it.trackNumber }
                Album(
                    id = albumId,
                    title = sorted.first().album,
                    artist = sorted.first().artist,
                    year = sorted.firstOrNull { it.year > 0 }?.year ?: 0,
                    tracks = sorted,
                )
            }
}
