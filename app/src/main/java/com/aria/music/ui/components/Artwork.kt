package com.aria.music.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aria.music.data.media.ArtworkLoader
import com.aria.music.data.model.Track

/** Loads (and caches) the embedded artwork for [track] as a Compose [ImageBitmap]. */
@Composable
fun rememberArtwork(track: Track?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(track?.id) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(track?.id) {
        val current = track ?: run {
            bitmap = null
            return@LaunchedEffect
        }
        bitmap = ArtworkLoader.cached(current.id)?.asImageBitmap()
        val loaded = ArtworkLoader.load(context, current)
        bitmap = loaded?.asImageBitmap()
    }
    return bitmap
}

/**
 * Square artwork with a soft placeholder (a subtle gradient plus a note glyph) so empty
 * libraries and tracks without embedded art still look intentional.
 */
@Composable
fun ArtworkImage(
    bitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 8.dp,
    iconSize: Dp = 28.dp,
) {
    val shape = RoundedCornerShape(cornerRadius)
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(iconSize),
            )
        }
    }
}
