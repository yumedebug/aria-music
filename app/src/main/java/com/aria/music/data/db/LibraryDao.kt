package com.aria.music.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {

    // ---- track cache -------------------------------------------------------

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTracks(tracks: List<CachedTrackEntity>)

    @Query("SELECT * FROM track_cache WHERE id IN (:ids)")
    suspend fun tracksByIds(ids: List<Long>): List<CachedTrackEntity>

    // ---- favourites --------------------------------------------------------

    @Query(
        "SELECT t.* FROM track_cache t INNER JOIN favorites f ON f.trackId = t.id " +
            "ORDER BY f.addedAt DESC",
    )
    fun favoriteTracks(): Flow<List<CachedTrackEntity>>

    @Query("SELECT trackId FROM favorites")
    fun favoriteIds(): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: Long)

    // ---- playlists ---------------------------------------------------------

    @Insert
    suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    suspend fun deletePlaylist(playlistId: Long)

    @Query("UPDATE playlists SET name = :name WHERE id = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String)

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    suspend fun playlist(playlistId: Long): PlaylistEntity?

    @Query(
        "SELECT p.id AS id, p.name AS name, p.createdAt AS createdAt, " +
            "COUNT(i.trackId) AS trackCount FROM playlists p " +
            "LEFT JOIN playlist_items i ON i.playlistId = p.id " +
            "GROUP BY p.id ORDER BY p.createdAt DESC",
    )
    fun playlistSummaries(): Flow<List<PlaylistSummary>>

    @Query(
        "SELECT t.* FROM track_cache t INNER JOIN playlist_items i ON i.trackId = t.id " +
            "WHERE i.playlistId = :playlistId ORDER BY i.position ASC",
    )
    fun playlistTracks(playlistId: Long): Flow<List<CachedTrackEntity>>

    @Query("SELECT COALESCE(MAX(position), -1) + 1 FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun nextPosition(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: PlaylistItemEntity)

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deletePlaylistItem(playlistId: Long, trackId: Long)

    @Query("SELECT * FROM playlist_items WHERE playlistId = :playlistId ORDER BY position ASC")
    suspend fun playlistItems(playlistId: Long): List<PlaylistItemEntity>

    @Query("DELETE FROM playlist_items WHERE playlistId = :playlistId")
    suspend fun clearPlaylistItems(playlistId: Long)

    // ---- play history ------------------------------------------------------

    @Insert
    suspend fun insertHistory(history: HistoryEntity)

    @Query(
        "SELECT t.* FROM track_cache t INNER JOIN history h ON h.trackId = t.id " +
            "ORDER BY h.playedAt DESC LIMIT :limit",
    )
    fun historyTracks(limit: Int): Flow<List<CachedTrackEntity>>

    @Query("DELETE FROM history")
    suspend fun clearHistory()

    @Query("DELETE FROM history WHERE id NOT IN (SELECT id FROM history ORDER BY playedAt DESC LIMIT :keep)")
    suspend fun trimHistory(keep: Int)

    /** Replaces the ordering of a playlist with the given (already ordered) track ids. */
    @Transaction
    suspend fun reorderPlaylist(playlistId: Long, orderedTrackIds: List<Long>) {
        clearPlaylistItems(playlistId)
        orderedTrackIds.forEachIndexed { index, trackId ->
            insertPlaylistItem(PlaylistItemEntity(playlistId, trackId, index))
        }
    }
}
