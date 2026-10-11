package com.aria.music.ui.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.aria.music.data.model.Track
import com.aria.music.data.online.OnlineCollection
import com.aria.music.data.online.OnlineSearch
import com.aria.music.data.online.YouTubeMusicRepository
import com.aria.music.data.online.asTrack
import com.aria.music.data.repo.LibraryRepository
import com.aria.music.playback.PlaybackConnection
import com.aria.music.ui.ariaViewModel
import com.aria.music.ui.components.EmptyState
import com.aria.music.ui.components.TrackRow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private val EMPTY_SEARCH = OnlineSearch(emptyList(), emptyList())

/** ローカルライブラリを検索し、ある程度の長さのクエリでは YouTube Music も検索する。 */
@OptIn(ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val library: LibraryRepository,
    private val playback: PlaybackConnection,
    private val online: YouTubeMusicRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _online = MutableStateFlow(EMPTY_SEARCH)
    private var onlineJob: Job? = null

    private val localResults: Flow<List<Track>> = _query.flatMapLatest { library.search(it) }

    /** ローカルライブラリの一致を先に、その後に YouTube Music の曲を並べる。 */
    val results: StateFlow<List<Track>> =
        combine(localResults, _online) { local, remote -> local + remote.songs }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** YouTube Music のまとまり（アルバム / プレイリスト / アーティスト）。 */
    val collections: StateFlow<List<OnlineCollection>> =
        _online.map { it.collections }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun onQueryChange(value: String) {
        _query.value = value
        onlineJob?.cancel()
        val term = value.trim()
        if (term.length < MIN_ONLINE_QUERY_LENGTH) {
            _online.value = EMPTY_SEARCH
            return
        }
        onlineJob = viewModelScope.launch {
            delay(ONLINE_DEBOUNCE_MS)
            _online.value = runCatching { online.search(term) }.getOrDefault(EMPTY_SEARCH)
        }
    }

    fun play(track: Track, from: List<Track>) {
        val index = from.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
        playback.playTracks(from, index)
    }

    /** まとまり（アルバム等）の中身を解決して再生する。 */
    fun playCollection(collection: OnlineCollection) {
        viewModelScope.launch {
            val tracks = runCatching { online.tracksOf(collection) }.getOrDefault(emptyList())
            if (tracks.isNotEmpty()) playback.playTracks(tracks, 0)
        }
    }

    fun refresh() {
        viewModelScope.launch { library.refresh() }
    }

    private companion object {
        const val ONLINE_DEBOUNCE_MS = 350L
        const val MIN_ONLINE_QUERY_LENGTH = 2
    }
}

@Composable
fun SearchScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel = ariaViewModel {
        SearchViewModel(it.libraryRepository, it.playback, it.youTubeMusic)
    }
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        Text(
            text = "Search",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            placeholder = { Text("Songs, artists, albums") },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large,
        )
        Spacer(Modifier.height(12.dp))

        when {
            query.isBlank() -> EmptyState(
                icon = Icons.Rounded.Search,
                title = "Search your library",
                subtitle = "Find songs, artists and albums on this device or YouTube Music.",
            )
            results.isEmpty() && collections.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Search,
                title = "No results",
                subtitle = "Nothing matches “$query”.",
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    bottom = contentPadding.calculateBottomPadding() + 24.dp,
                ),
            ) {
                // オンラインのアルバム / プレイリスト / アーティスト（タップでまとめて再生）。
                items(collections, key = { "c:${it.kind}:${it.id}" }) { collection ->
                    TrackRow(
                        track = collection.asTrack(),
                        onClick = { viewModel.playCollection(collection) },
                    )
                }
                items(results, key = { "t:${it.id}" }) { track ->
                    TrackRow(track = track, onClick = { viewModel.play(track, results) })
                }
            }
        }
    }
}
