package com.aria.music.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aria.music.data.model.Track
import com.aria.music.data.repo.LibraryRepository
import com.aria.music.playback.PlaybackConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class PlaylistDetailState(
    val id: Long,
    val name: String,
    val tracks: List<Track>,
)

class PlaylistDetailViewModel(
    private val library: LibraryRepository,
    private val playback: PlaybackConnection,
) : ViewModel() {

    fun detail(playlistId: Long): Flow<PlaylistDetailState?> =
        combine(
            library.playlists,
            library.playlistTracks(playlistId),
        ) { summaries, tracks ->
            val summary = summaries.firstOrNull { it.id == playlistId } ?: return@combine null
            PlaylistDetailState(summary.id, summary.name, tracks)
        }

    fun removeTrack(playlistId: Long, track: Track) {
        viewModelScope.launch { library.removeFromPlaylist(playlistId, track.id) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { library.deletePlaylist(playlistId) }
    }
}
