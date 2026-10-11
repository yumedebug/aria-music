package com.aria.music.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.aria.music.core.lyrics.Lyrics
import com.aria.music.ui.components.EmptyState

/**
 * Full-bleed lyrics page.
 *
 * Timed lyrics highlight the active line and auto-scroll, but user scrolling always wins:
 * auto-scroll only resumes once the list has settled back near the tracked line.
 */
@Composable
fun LyricsView(
    lyrics: Lyrics,
    positionMs: Long,
    isLoading: Boolean,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    if (isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = accent, modifier = Modifier.size(36.dp))
        }
        return
    }
    if (lyrics.isEmpty) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyState(
                icon = Icons.Rounded.Lyrics,
                title = "No lyrics",
                subtitle = "This track has no lyrics embedded in its file.",
            )
        }
        return
    }

    val listState = rememberLazyListState()
    val activeIndex = lyrics.activeIndex(positionMs)

    LaunchedEffect(activeIndex) {
        if (activeIndex < 0) return@LaunchedEffect
        // Only pull the list when the user is not scrolling it themselves.
        if (!listState.isScrollInProgress) {
            listState.animateScrollToItem(index = (activeIndex - 2).coerceAtLeast(0))
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 80.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(lyrics.lines) { index, line ->
            val isActive = index == activeIndex
            Text(
                text = line.text,
                style = if (isActive) {
                    MaterialTheme.typography.headlineMedium
                } else {
                    MaterialTheme.typography.titleMedium
                },
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                color = when {
                    !lyrics.synced -> MaterialTheme.colorScheme.onSurface
                    isActive -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                },
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (isActive && lyrics.synced) {
                            Modifier.background(
                                Brush.horizontalGradient(
                                    listOf(accent.copy(alpha = 0.22f), Color.Transparent),
                                ),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .padding(vertical = 2.dp),
            )
        }
    }
}
