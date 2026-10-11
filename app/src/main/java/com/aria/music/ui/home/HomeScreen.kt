package com.aria.music.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.music.data.db.PlaylistSummary
import com.aria.music.data.model.Album
import com.aria.music.data.model.Track
import com.aria.music.data.online.OnlineCollection
import com.aria.music.data.online.asTrack
import com.aria.music.ui.ariaViewModel
import com.aria.music.ui.components.ArtworkImage
import com.aria.music.ui.components.EmptyState
import com.aria.music.ui.components.SectionHeader
import com.aria.music.ui.components.rememberArtwork

/**
 * "Home" — the landing tab, laid out like Apple Music: a large title, a single featured
 * hero card, then horizontal shelves. The top shelves come from the YouTube Music
 * recommendation feed; the rest are real library data and disappear when empty.
 */
@Composable
fun HomeScreen(
    onOpenAlbum: (Album) -> Unit,
    onOpenPlaylist: (Long) -> Unit,
    onOpenCollection: (OnlineCollection) -> Unit,
    onOpenSearch: () -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    val viewModel = ariaViewModel {
        HomeViewModel(it.libraryRepository, it.playback, it.youTubeMusic)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            Text(
                text = "Home",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
        }

        // Featured hero: the first recommendation, rendered as a large Apple Music card.
        val hero = state.onlineShelves.firstNotNullOfOrNull { it.collections.firstOrNull() }
        if (hero != null) {
            item(key = "hero") {
                FeaturedCard(
                    collection = hero,
                    onClick = { onOpenCollection(hero) },
                )
            }
        }

        // YouTube Music recommendations.
        state.onlineShelves.forEachIndexed { index, shelf ->
            item(key = "online:$index:${shelf.title}") {
                Shelf(title = shelf.title) {
                    items(shelf.collections, key = { "oc:${it.kind}:${it.id}" }) { collection ->
                        CollectionCard(
                            collection = collection,
                            onClick = { onOpenCollection(collection) },
                        )
                    }
                    items(shelf.tracks, key = { "ot:${it.id}" }) { track ->
                        TrackCard(track = track, onClick = { viewModel.play(track, shelf.tracks) })
                    }
                }
            }
        }

        // Local library shelves.
        if (state.recentlyPlayed.isNotEmpty()) {
            item(key = "recent") {
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
            item(key = "favourites") {
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
            item(key = "albums") {
                Shelf(title = "Albums") {
                    items(state.albums, key = { it.id }) { album ->
                        AlbumCard(album = album, onClick = { onOpenAlbum(album) })
                    }
                }
            }
        }

        if (state.playlists.isNotEmpty()) {
            item(key = "playlists") {
                Shelf(title = "Playlists") {
                    items(state.playlists, key = { it.id }) { playlist ->
                        PlaylistCard(playlist = playlist, onClick = { onOpenPlaylist(playlist.id) })
                    }
                }
            }
        }

        if (state.loaded && state.onlineLoaded && !state.hasAnything) {
            item(key = "empty") {
                EmptyState(
                    icon = Icons.Rounded.LibraryMusic,
                    title = "No music yet",
                    subtitle = "Add audio files to this device to see them here. " +
                        "You can also search YouTube Music from the Search tab.",
                )
            }
        }
    }
}

/** A tall, artwork-backed hero card with a gradient scrim and overlaid metadata. */
@Composable
private fun FeaturedCard(collection: OnlineCollection, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .aspectRatio(1.55f)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        ArtworkImage(
            bitmap = rememberArtwork(collection.asTrack()),
            modifier = Modifier.fillMaxSize(),
            cornerRadius = 0.dp,
            iconSize = 64.dp,
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.70f)),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
        ) {
            Text(
                text = collection.kind.label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = collection.title,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (collection.subtitle.isNotBlank()) {
                Text(
                    text = collection.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Shelf(title: String, content: LazyListScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeader(title = title)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            content = content,
        )
    }
}

private val cardWidth = 158.dp

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
private fun CollectionCard(collection: OnlineCollection, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(cardWidth)
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
    ) {
        ArtworkImage(
            bitmap = rememberArtwork(collection.asTrack()),
            modifier = Modifier.size(cardWidth),
            cornerRadius = 10.dp,
            iconSize = 40.dp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = collection.title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            // 種別（アルバム / プレイリスト / アーティスト）を必ず出し、
            // プレイリストがアルバムに紛れて見えないようにする。
            text = buildString {
                append(collection.kind.label)
                if (collection.subtitle.isNotBlank()) {
                    append(" · ")
                    append(collection.subtitle)
                }
            },
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
