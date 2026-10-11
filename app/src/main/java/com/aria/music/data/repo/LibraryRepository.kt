package com.aria.music.data.repo

import com.aria.music.data.db.CachedTrackEntity
import com.aria.music.data.db.FavoriteEntity
import com.aria.music.data.db.HistoryEntity
import com.aria.music.data.db.LibraryDao
import com.aria.music.data.db.PlaylistEntity
import com.aria.music.data.db.PlaylistItemEntity
import com.aria.music.data.db.PlaylistSummary
import com.aria.music.data.db.toEntity
import com.aria.music.data.db.toTrack
import com.aria.music.data.media.MediaStoreDataSource
import com.aria.music.data.model.Album
import com.aria.music.data.model.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Single entry point the UI uses for library data.
 *
 * Tracks originate from [MediaStoreDataSource]; anything the user creates (favourites,
 * playlists, history) is persisted through Room and re-joined with the live library.
 */
class LibraryRepository(
    private val mediaStore: MediaStoreDataSource,
    private val dao: LibraryDao,
) {

    private val _libraryTracks = MutableStateFlow<List<Track>>(emptyList())
    val libraryTracks: StateFlow<List<Track>> = _libraryTracks.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded: StateFlow<Boolean> = _isLoaded.asStateFlow()

    /** Rescans the device library. Safe to call on every app start and after permissions change. */
    suspend fun refresh() {
        val tracks = mediaStore.loadTracks()
        _libraryTracks.value = tracks
        _albums.value = mediaStore.groupIntoAlbums(tracks)
        _isLoaded.value = true
    }

    fun search(query: String): Flow<List<Track>> {
        val normalized = query.trim().lowercase()
        return _libraryTracks.map { tracks ->
            if (normalized.isEmpty()) emptyList()
            else tracks.filter { track ->
                track.title.lowercase().contains(normalized) ||
                    track.artist.lowercase().contains(normalized) ||
                    track.album.lowercase().contains(normalized)
            }
        }
    }

    // ---- favourites --------------------------------------------------------

    val favoriteTracks: Flow<List<Track>> = dao.favoriteTracks().map { it.toTracks() }
    val favoriteIds: Flow<List<Long>> = dao.favoriteIds()

    suspend fun setFavorite(track: Track, favorite: Boolean) {
        if (favorite) {
            dao.upsertTracks(listOf(track.toEntity()))
            dao.addFavorite(FavoriteEntity(track.id, System.currentTimeMillis()))
        } else {
            dao.removeFavorite(track.id)
        }
    }

    // ---- playlists ---------------------------------------------------------

    val playlists: Flow<List<PlaylistSummary>> = dao.playlistSummaries()

    fun playlistTracks(playlistId: Long): Flow<List<Track>> =
        dao.playlistTracks(playlistId).map { it.toTracks() }

    suspend fun createPlaylist(name: String): Long =
        dao.insertPlaylist(PlaylistEntity(name = name, createdAt = System.currentTimeMillis()))

    suspend fun renamePlaylist(playlistId: Long, name: String) = dao.renamePlaylist(playlistId, name)

    suspend fun deletePlaylist(playlistId: Long) = dao.deletePlaylist(playlistId)

    suspend fun addToPlaylist(playlistId: Long, track: Track) {
        dao.upsertTracks(listOf(track.toEntity()))
        val position = dao.nextPosition(playlistId)
        dao.insertPlaylistItem(PlaylistItemEntity(playlistId, track.id, position))
    }

    suspend fun removeFromPlaylist(playlistId: Long, trackId: Long) {
        dao.deletePlaylistItem(playlistId, trackId)
        dao.reorderPlaylist(playlistId, dao.playlistItems(playlistId).map { it.trackId })
    }

    suspend fun movePlaylistItem(playlistId: Long, fromIndex: Int, toIndex: Int) {
        val items = dao.playlistItems(playlistId).map { it.trackId }.toMutableList()
        if (fromIndex !in items.indices || toIndex !in items.indices) return
        val moved = items.removeAt(fromIndex)
        items.add(toIndex, moved)
        dao.reorderPlaylist(playlistId, items)
    }

    // ---- history -----------------------------------------------------------

    val recentTracks: Flow<List<Track>> = dao.historyTracks(HISTORY_FETCH_LIMIT)
        .map { entities -> entities.toTracks().distinctBy { it.id } }

    suspend fun recordPlayed(track: Track) {
        dao.upsertTracks(listOf(track.toEntity()))
        dao.insertHistory(HistoryEntity(trackId = track.id, playedAt = System.currentTimeMillis()))
        dao.trimHistory(HISTORY_LIMIT)
    }

    /** Resolves cached ids into live library tracks, falling back to cached metadata. */
    suspend fun resolveTracks(ids: List<Long>): List<Track> =
        dao.tracksByIds(ids).toTracks()

    private fun List<CachedTrackEntity>.toTracks(): List<Track> = map { it.toTrack() }

    private companion object {
        const val HISTORY_FETCH_LIMIT = 200
        const val HISTORY_LIMIT = 100
    }
}
