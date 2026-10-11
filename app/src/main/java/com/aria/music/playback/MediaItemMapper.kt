package com.aria.music.playback

import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.aria.music.data.model.Track

/**
 * Tracks are round-tripped through [MediaItem] so the queue survives controller
 * reconnects. Everything the UI needs to render a row is carried in the metadata extras.
 */
private const val KEY_ALBUM_ID = "aria.albumId"

object MediaItemMapper {

    fun toMediaItem(track: Track): MediaItem {
        val extras = Bundle().apply {
            putLong(KEY_ALBUM_ID, track.albumId)
            putString("aria.uri", track.uri)
            putInt("aria.trackNumber", track.trackNumber)
            putInt("aria.year", track.year)
            putLong("aria.durationMs", track.durationMs)
            track.thumbnailUrl?.let { putString("aria.thumbnail", it) }
        }
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setIsBrowsable(false)
            .setIsPlayable(true)
            .setExtras(extras)
            .build()
        return MediaItem.Builder()
            .setMediaId(track.id.toString())
            .setUri(track.uri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun toTrack(item: MediaItem): Track? {
        val id = item.mediaId.toLongOrNull() ?: return null
        val metadata = item.mediaMetadata
        val extras = metadata.extras
        return Track(
            id = id,
            title = metadata.title?.toString() ?: "Unknown Title",
            artist = metadata.artist?.toString() ?: "Unknown Artist",
            album = metadata.albumTitle?.toString() ?: "Unknown Album",
            albumId = extras?.getLong(KEY_ALBUM_ID, 0L) ?: 0L,
            durationMs = extras?.getLong("aria.durationMs", 0L) ?: 0L,
            uri = extras?.getString("aria.uri") ?: item.localConfiguration?.uri?.toString().orEmpty(),
            trackNumber = extras?.getInt("aria.trackNumber", 0) ?: 0,
            year = extras?.getInt("aria.year", 0) ?: 0,
            thumbnailUrl = extras?.getString("aria.thumbnail"),
        )
    }
}
