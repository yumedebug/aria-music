package com.aria.music.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.aria.music.data.model.Track

/**
 * A snapshot of track metadata. Tracks are cached here whenever they are saved into a
 * favourite or playlist, so those lists keep working even before the MediaStore scan
 * finishes (or if a file temporarily disappears).
 */
@Entity(tableName = "track_cache")
data class CachedTrackEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val trackNumber: Int,
    val year: Int,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val trackId: Long,
    val addedAt: Long,
)

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val createdAt: Long,
)

@Entity(tableName = "playlist_items", primaryKeys = ["playlistId", "trackId"])
data class PlaylistItemEntity(
    val playlistId: Long,
    val trackId: Long,
    val position: Int,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val trackId: Long,
    val playedAt: Long,
)

data class PlaylistSummary(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val trackCount: Int,
)

fun CachedTrackEntity.toTrack(): Track = Track(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    uri = uri,
    trackNumber = trackNumber,
    year = year,
)

fun Track.toEntity(): CachedTrackEntity = CachedTrackEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    uri = uri,
    trackNumber = trackNumber,
    year = year,
)

fun PlaylistEntity.toSummary(trackCount: Int): PlaylistSummary =
    PlaylistSummary(id = id, name = name, createdAt = createdAt, trackCount = trackCount)
