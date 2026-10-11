package com.aria.music.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.music.data.db.PlaylistSummary
import com.aria.music.data.model.Album
import com.aria.music.data.model.Track
import com.aria.music.data.repo.LibraryRepository
import com.aria.music.playback.PlaybackConnection
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val loaded: Boolean = false,
    val recentlyPlayed: List<Track> = emptyList(),
    val favorites: List<Track> = emptyList(),
    val albums: List<Album> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
) {
    val isLibraryEmpty: Boolean get() = loaded && albums.isEmpty()
    val hasAnything: Boolean
        get() = recentlyPlayed.isNotEmpty() || favorites.isNotEmpty() ||
            albums.isNotEmpty() || playlists.isNotEmpty()
}

/**
 * Home only ever shows data that genuinely exists: recent plays from the local history,
 * real favourites, albums scanned from the device, and user-created playlists. No
 * fabricated "recommendations".
 */
class HomeViewModel(
    private val library: LibraryRepository,
    private val playback: PlaybackConnection,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        library.isLoaded,
        library.recentTracks,
        library.favoriteTracks,
        library.albums,
        library.playlists,
    ) { loaded, recent, favorites, albums, playlists ->
        HomeUiState(
            loaded = loaded,
            recentlyPlayed = recent,
            favorites = favorites,
            albums = albums,
            playlists = playlists,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { library.refresh() }
    }

    fun play(track: Track, from: List<Track>) {
        val index = from.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playback.playTracks(from, index)
    }

    fun playAlbum(album: Album) = playback.playTracks(album.tracks, 0)

    fun shuffleAlbum(album: Album) {
        playback.playTracks(album.tracks.shuffled(), 0)
    }

    fun setFavorite(track: Track, favorite: Boolean) {
        viewModelScope.launch { library.setFavorite(track, favorite) }
    }
}
