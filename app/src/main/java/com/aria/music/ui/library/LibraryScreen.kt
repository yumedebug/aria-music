package com.aria.music.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.music.data.model.Album
import com.aria.music.ui.ariaViewModel
import com.aria.music.ui.components.ArtworkImage
import com.aria.music.ui.components.EmptyState
import com.aria.music.ui.components.SegmentedControl
import com.aria.music.ui.components.TrackRow
import com.aria.music.ui.components.rememberArtwork

private val tabs = listOf("Albums", "Playlists", "Songs", "Favourites")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onOpenAlbum: (Album) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel = ariaViewModel { LibraryViewModel(it.libraryRepository, it.playback) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = {
            if (selectedTab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showCreateDialog = true },
                    icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    text = { Text("New Playlist") },
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = contentPadding.calculateTopPadding()),
        ) {
            Text(
                text = "Library",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            SegmentedControl(
                options = tabs,
                selectedIndex = selectedTab,
                onSelect = { selectedTab = it },
            )
            Spacer(Modifier.height(12.dp))

            val bottomPadding = innerPadding.calculateBottomPadding() +
                contentPadding.calculateBottomPadding() + 24.dp

            when (selectedTab) {
                0 -> AlbumsGrid(
                    albums = state.albums,
                    onOpenAlbum = onOpenAlbum,
                    bottomPadding = bottomPadding,
                    loaded = state.loaded,
                )
                1 -> PlaylistsList(
                    playlists = state.playlists,
                    onOpenPlaylist = onOpenPlaylist,
                    bottomPadding = bottomPadding,
                )
                2 -> LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding)) {
                    items(state.songs, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            onClick = { viewModel.playTrack(track, state.songs) },
                        )
                    }
                }
                else -> LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding)) {
                    items(state.favorites, key = { it.id }) { track ->
                        TrackRow(
                            track = track,
                            onClick = { viewModel.playTrack(track, state.favorites) },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Playlist") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Name") },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.createPlaylist(newName.trim().ifBlank { "New Playlist" })
                        newName = ""
                        showCreateDialog = false
                    },
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun AlbumsGrid(
    albums: List<Album>,
    onOpenAlbum: (Album) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
    loaded: Boolean,
) {
    if (albums.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.LibraryMusic,
            title = if (loaded) "No albums" else "Scanning your library",
            subtitle = if (loaded) "Albums found on this device will appear here." else null,
        )
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = bottomPadding),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        items(albums, key = { it.id }) { album ->
            Column(modifier = Modifier.clickable { onOpenAlbum(album) }) {
                ArtworkImage(
                    bitmap = rememberArtwork(album.tracks.firstOrNull()),
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    cornerRadius = 8.dp,
                    iconSize = 40.dp,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = album.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = album.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlaylistsList(
    playlists: List<com.aria.music.data.db.PlaylistSummary>,
    onOpenPlaylist: (Long) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    if (playlists.isEmpty()) {
        EmptyState(
            icon = Icons.Rounded.PlaylistPlay,
            title = "No playlists",
            subtitle = "Create a playlist to collect the music you love.",
        )
        return
    }
    LazyColumn(contentPadding = PaddingValues(bottom = bottomPadding)) {
        items(playlists, key = { it.id }) { playlist ->
            ListItem(
                headlineContent = { Text(playlist.name) },
                supportingContent = { Text("${playlist.trackCount} songs") },
                leadingContent = {
                    Icon(Icons.Rounded.PlaylistPlay, contentDescription = null)
                },
                modifier = Modifier.clickable { onOpenPlaylist(playlist.id) },
            )
        }
    }
}
