package com.aria.music.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aria.music.core.formatDuration
import com.aria.music.core.lyrics.Lyrics
import com.aria.music.playback.PlaybackState
import com.aria.music.ui.LocalAppContainer
import com.aria.music.ui.components.ArtworkImage
import com.aria.music.ui.components.Scrubber
import com.aria.music.ui.components.TransportControls
import com.aria.music.ui.components.rememberArtwork
import com.aria.music.ui.library.PlaylistPickerSheet
import com.aria.music.ui.theme.LocalIsDarkTheme
import kotlinx.coroutines.launch

/**
 * The full-screen player — the most detailed screen in the app.
 *
 * Layout follows the reference player: artwork is the hero, the title/artist block sits
 * beneath it with a clear hierarchy, the scrubber and transport controls anchor the bottom,
 * and secondary actions (lyrics, queue, overflow) stay out of the way. The backdrop is
 * tinted from the artwork's own accent so each track feels distinct.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    state: PlaybackState,
    accent: Color,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val isDark = LocalIsDarkTheme.current
    val track = state.current
    val scope = rememberCoroutineScope()

    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showPlaylistPicker by remember { mutableStateOf(false) }
    var lyrics by remember { mutableStateOf(Lyrics.EMPTY) }
    var lyricsLoading by remember { mutableStateOf(false) }

    val favoriteIds by container.libraryRepository.favoriteIds
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val isFavorite = track != null && favoriteIds.contains(track.id)

    val queueSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val menuSheetState = rememberModalBottomSheetState()
    val pickerSheetState = rememberModalBottomSheetState()

    LaunchedEffect(track?.id) {
        val current = track ?: return@LaunchedEffect
        lyricsLoading = true
        lyrics = container.lyricsRepository.load(current)
        lyricsLoading = false
    }

    val background = MaterialTheme.colorScheme.background
    val topTint = lerp(background, accent, if (isDark) 0.45f else 0.30f)
    val backdrop = Brush.verticalGradient(listOf(topTint, background, background))

    Box(modifier = modifier.fillMaxSize().background(backdrop)) {
        if (track == null) {
            EmptyPlayer(onCollapse = onCollapse, modifier = Modifier.fillMaxSize())
        } else {
            AnimatedContent(
                targetState = showLyrics,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(200)) },
                label = "lyricsToggle",
            ) { lyricsVisible ->
                if (lyricsVisible) {
                    LyricsPage(
                        lyrics = lyrics,
                        isLoading = lyricsLoading,
                        positionMs = state.positionMs,
                        accent = accent,
                        onBack = { showLyrics = false },
                        onCollapse = onCollapse,
                    )
                } else {
                    PlayerPage(
                        state = state,
                        accent = accent,
                        isFavorite = isFavorite,
                        onCollapse = onCollapse,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeek,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        onShowLyrics = { showLyrics = true },
                        onShowQueue = { showQueue = true },
                        onShowMenu = { showMenu = true },
                        onToggleFavorite = {
                            scope.launch {
                                container.libraryRepository.setFavorite(track, !isFavorite)
                            }
                        },
                    )
                }
            }
        }
    }

    if (showQueue) {
        QueueSheet(
            state = state,
            sheetState = queueSheetState,
            onDismiss = { showQueue = false },
            onPlayIndex = { container.playback.skipToQueueItem(it) },
            onRemoveIndex = { container.playback.removeQueueItem(it) },
            onMove = { from, to -> container.playback.moveQueueItem(from, to) },
        )
    }

    if (showMenu && track != null) {
        ModalBottomSheet(onDismissRequest = { showMenu = false }, sheetState = menuSheetState) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
            )
            ListItem(
                headlineContent = { Text("Play Next") },
                leadingContent = { Icon(Icons.Rounded.QueueMusic, contentDescription = null) },
                modifier = Modifier.clickable {
                    container.playback.enqueueNext(track)
                    showMenu = false
                },
            )
            ListItem(
                headlineContent = { Text("Play Last") },
                leadingContent = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                modifier = Modifier.clickable {
                    container.playback.enqueueEnd(track)
                    showMenu = false
                },
            )
            ListItem(
                headlineContent = { Text("Add to Playlist") },
                leadingContent = { Icon(Icons.Rounded.PlaylistAdd, contentDescription = null) },
                modifier = Modifier.clickable {
                    showMenu = false
                    showPlaylistPicker = true
                },
            )
        }
    }

    if (showPlaylistPicker && track != null) {
        PlaylistPickerSheet(
            track = track,
            sheetState = pickerSheetState,
            onDismiss = { showPlaylistPicker = false },
        )
    }
}

@Composable
private fun PlayerPage(
    state: PlaybackState,
    accent: Color,
    isFavorite: Boolean,
    onCollapse: () -> Unit,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onShowLyrics: () -> Unit,
    onShowQueue: () -> Unit,
    onShowMenu: () -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val track = state.current ?: return
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = statusBarPadding),
    ) {
        // Apple Music-style grabber.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp, bottom = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse player",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    contentDescription = "Favourite",
                    tint = if (isFavorite) accent else MaterialTheme.colorScheme.onSurface,
                )
            }
            IconButton(onClick = onShowMenu) {
                Icon(
                    Icons.Rounded.MoreHoriz,
                    contentDescription = "More options",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val maxArtwork = (maxWidth - 56.dp).coerceAtLeast(120.dp)
            var grown by remember { mutableStateOf(false) }
            LaunchedEffect(track.id) { grown = true }
            val artworkSize by animateDpAsState(
                targetValue = if (grown) maxArtwork else 96.dp,
                animationSpec = tween(420, easing = FastOutSlowInEasing),
                label = "artworkSize",
            )
            val corner by animateDpAsState(
                targetValue = if (grown) 12.dp else 8.dp,
                animationSpec = tween(420),
                label = "artworkCorner",
            )
            ArtworkImage(
                bitmap = rememberArtwork(track),
                modifier = Modifier
                    .size(artworkSize)
                    .shadow(20.dp, RoundedCornerShape(corner), clip = false),
                cornerRadius = corner,
                iconSize = 64.dp,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
        ) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = track.artist,
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(16.dp))

            Scrubber(
                positionMs = state.positionMs,
                durationMs = state.durationMs,
                bufferedMs = state.bufferedMs,
                accent = MaterialTheme.colorScheme.onSurface,
                onSeek = onSeek,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = formatDuration(state.positionMs),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "-${formatDuration((state.durationMs - state.positionMs).coerceAtLeast(0L))}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(12.dp))

            TransportControls(
                isPlaying = state.isPlaying,
                shuffle = state.shuffle,
                repeatMode = state.repeatMode,
                accent = accent,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onPrevious = onPrevious,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                enabled = state.current != null,
            )

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onShowLyrics) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = "Lyrics",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onShowQueue) {
                    Icon(
                        Icons.Rounded.QueueMusic,
                        contentDescription = "Playing next",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(navBarPadding + 8.dp))
    }
}

@Composable
private fun LyricsPage(
    lyrics: Lyrics,
    isLoading: Boolean,
    positionMs: Long,
    accent: Color,
    onBack: () -> Unit,
    onCollapse: () -> Unit,
) {
    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
    Column(modifier = Modifier.fillMaxSize().padding(top = statusBarPadding)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onCollapse) {
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Collapse player",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = "Back to artwork",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        LyricsView(
            lyrics = lyrics,
            positionMs = positionMs,
            isLoading = isLoading,
            accent = accent,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.height(navBarPadding))
    }
}

@Composable
private fun EmptyPlayer(onCollapse: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Nothing playing",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
