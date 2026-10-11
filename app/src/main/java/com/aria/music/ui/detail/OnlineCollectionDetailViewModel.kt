package com.aria.music.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.music.data.model.Track
import com.aria.music.data.online.OnlineCollection
import com.aria.music.data.online.YouTubeMusicRepository
import com.aria.music.playback.PlaybackConnection
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * YouTube Music のアルバム / プレイリスト / アーティストの詳細画面の状態。
 *
 * ローカルの [com.aria.music.ui.detail.PlaylistDetailScreen] は Room の
 * `playlistId: Long` を受け取るが、オンラインのまとまりは YouTube 側の ID で
 * しか無いため別経路にしている。曲一覧はタップ時に [YouTubeMusicRepository]
 * から解決する。
 */
data class OnlineDetailUiState(
    val collection: OnlineCollection? = null,
    val tracks: List<Track> = emptyList(),
    val loading: Boolean = true,
    val failed: Boolean = false,
)

class OnlineCollectionDetailViewModel(
    private val online: YouTubeMusicRepository,
    private val playback: PlaybackConnection,
) : ViewModel() {

    private val _state = MutableStateFlow(OnlineDetailUiState())
    val state: StateFlow<OnlineDetailUiState> = _state.asStateFlow()

    /** [collection] の曲一覧を取得する。[OnlineCollectionNav] から受け取った分を渡す。 */
    fun load(collection: OnlineCollection) {
        if (_state.value.collection?.id == collection.id &&
            _state.value.collection?.kind == collection.kind &&
            !_state.value.loading
        ) {
            return
        }
        _state.value = OnlineDetailUiState(collection = collection, loading = true)
        viewModelScope.launch {
            val tracks = runCatching { online.tracksOf(collection) }
                .getOrElse {
                    _state.value = OnlineDetailUiState(
                        collection = collection,
                        loading = false,
                        failed = true,
                    )
                    return@launch
                }
            _state.value = OnlineDetailUiState(
                collection = collection,
                tracks = tracks,
                loading = false,
            )
        }
    }

    fun playTrack(track: Track) {
        val tracks = _state.value.tracks
        val index = tracks.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        if (tracks.isNotEmpty()) playback.playTracks(tracks, index)
    }

    fun playAll() {
        _state.value.tracks.takeIf { it.isNotEmpty() }?.let { playback.playTracks(it, 0) }
    }

    fun shuffle() {
        _state.value.tracks.takeIf { it.isNotEmpty() }
            ?.let { playback.playTracks(it.shuffled(), 0) }
    }
}
