package com.aria.music.ui.library

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

data class LibraryUiState(
    val loaded: Boolean = false,
    val albums: List<Album> = emptyList(),
    val playlists: List<PlaylistSummary> = emptyList(),
    val songs: List<Track> = emptyList(),
    val favorites: List<Track> = emptyList(),
)

class LibraryViewModel(
    private val library: LibraryRepository,
    private val playback: PlaybackConnection,
) : ViewModel() {

    val state: StateFlow<LibraryUiState> = combine(
        library.isLoaded,
        library.albums,
        library.playlists,
        library.libraryTracks,
        library.favoriteTracks,
    ) { loaded, albums, playlists, songs, favorites ->
        LibraryUiState(loaded, albums, playlists, songs, favorites)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun playTrack(track: Track, from: List<Track>) {
        val index = from.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playback.playTracks(from, index)
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch { library.createPlaylist(name) }
    }
}
