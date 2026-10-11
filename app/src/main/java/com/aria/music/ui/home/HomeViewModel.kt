package com.aria.music.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.music.data.db.PlaylistSummary
import com.aria.music.data.model.Album
import com.aria.music.data.model.Track
import com.aria.music.data.online.OnlineCollection
import com.aria.music.data.online.OnlineShelf
import com.aria.music.data.online.YouTubeMusicRepository
import com.aria.music.data.repo.LibraryRepository
import com.aria.music.playback.PlaybackConnection
import kotlinx.coroutines.flow.MutableStateFlow
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
    val onlineShelves: List<OnlineShelf> = emptyList(),
    val onlineLoaded: Boolean = false,
) {
    val isLibraryEmpty: Boolean
        get() = loaded && albums.isEmpty() && onlineShelves.isEmpty()
    val hasAnything: Boolean
        get() = recentlyPlayed.isNotEmpty() || favorites.isNotEmpty() ||
            albums.isNotEmpty() || playlists.isNotEmpty() || onlineShelves.isNotEmpty()
}

/**
 * Home shows real library data (recent plays, favourites, scanned albums, user playlists)
 * plus the YouTube Music recommendation feed, which is fetched from the same InnerTube
 * backend OuterTune uses.
 */
class HomeViewModel(
    private val library: LibraryRepository,
    private val playback: PlaybackConnection,
    private val online: YouTubeMusicRepository,
) : ViewModel() {

    private val onlineShelves = MutableStateFlow<List<OnlineShelf>>(emptyList())
    private val onlineLoaded = MutableStateFlow(false)

    private val localState = combine(
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
    }

    val state: StateFlow<HomeUiState> = combine(localState, onlineShelves, onlineLoaded) { local, shelves, loaded ->
        local.copy(onlineShelves = shelves, onlineLoaded = loaded)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh()
        refreshOnline()
    }

    fun refresh() {
        viewModelScope.launch { library.refresh() }
    }

    /** YouTube Music のおすすめを取得する。失敗してもローカル棚はそのまま表示する。 */
    fun refreshOnline() {
        viewModelScope.launch {
            val shelves = runCatching { online.home() }.getOrDefault(emptyList())
            onlineShelves.value = shelves
            onlineLoaded.value = true
        }
    }

    fun play(track: Track, from: List<Track>) {
        val index = from.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playback.playTracks(from, index)
    }

    /** おすすめ棚のアルバム / プレイリスト / アーティストを解決して頭から再生する。 */
    fun playCollection(collection: OnlineCollection) {
        viewModelScope.launch {
            val tracks = runCatching { online.tracksOf(collection) }.getOrDefault(emptyList())
            if (tracks.isNotEmpty()) playback.playTracks(tracks, 0)
        }
    }

    fun playAlbum(album: Album) = playback.playTracks(album.tracks, 0)

    fun shuffleAlbum(album: Album) {
        playback.playTracks(album.tracks.shuffled(), 0)
    }

    fun setFavorite(track: Track, favorite: Boolean) {
        viewModelScope.launch { library.setFavorite(track, favorite) }
    }
}
