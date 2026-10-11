package com.aria.music.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.music.data.db.PlaylistSummary
import com.aria.music.data.model.Album
import com.aria.music.data.model.Track
import com.aria.music.ui.ariaViewModel
import com.aria.music.ui.components.ArtworkImage
import com.aria.music.ui.components.EmptyState
import com.aria.music.ui.components.SectionHeader
import com.aria.music.ui.components.rememberArtwork

/**
 * "Listen Now" — the landing tab. Every shelf renders real library data and disappears when
 * there is nothing to show, so the screen degrades to an honest empty state instead of
 * inventing content.
 */
@Composable
fun HomeScreen(
    onOpenAlbum: (Album) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSearch: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel = ariaViewModel { HomeViewModel(it.libraryRepository, it.playback) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Text(
                text = "Listen Now",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        if (state.recentlyPlayed.isNotEmpty()) {
            item {
                Shelf(title = "Recently Played") {
                    items(state.recentlyPlayed, key = { it.id }) { track ->
                        TrackCard(
                            track = track,
                            onClick = { viewModel.play(track, state.recentlyPlayed) },
                        )
                    }
                }
            }
        }

        if (state.favorites.isNotEmpty()) {
            item {
                Shelf(title = "Favourites") {
                    items(state.favorites, key = { it.id }) { track ->
                        TrackCard(
                            track = track,
                            onClick = { viewModel.play(track, state.favorites) },
                        )
                    }
                }
            }
        }

        if (state.albums.isNotEmpty()) {
            item {
                Shelf(title = "Albums") {
                    items(state.albums, key = { it.id }) { album ->
                        AlbumCard(album = album, onClick = { onOpenAlbum(album) })
                    }
                }
            }
        }

        if (state.playlists.isNotEmpty()) {
            item {
                Shelf(title = "Playlists") {
                    items(state.playlists, key = { it.id }) { playlist ->
                        PlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                    }
                }
            }
        }

        if (state.isLibraryEmpty) {
            item {
                EmptyState(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "No music found",
                    subtitle = "Add audio files to this device, or grant access to your " +
                        "music library, and they will appear here.",
                )
            }
        }
    }
}

@Composable
private fun Shelf(title: String, content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

private val cardWidth = 150.dp

@Composable
private fun TrackCard(track: Track, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        ArtworkImage(
            bitmap = rememberArtwork(track),
            modifier = Modifier.size(cardWidth),
            cornerRadius = 10.dp,
            iconSize = 40.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = track.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = track.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        ArtworkImage(
            bitmap = rememberArtwork(album.tracks.firstOrNull()),
            modifier = Modifier.size(cardWidth),
            cornerRadius = 10.dp,
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

@Composable
private fun PlaylistCard(playlist: PlaylistSummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        // Playlists have no artwork of their own; show the neutral note tile.
        ArtworkImage(
            bitmap = null,
            modifier = Modifier.size(cardWidth),
            cornerRadius = 10.dp,
            iconSize = 44.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = playlist.name,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${playlist.trackCount} songs",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
