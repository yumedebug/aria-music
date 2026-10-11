package com.aria.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import com.aria.music.core.Palette
import com.aria.music.data.model.Track
import com.aria.music.ui.components.rememberArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val SAMPLE_SIZE = 24

/**
 * Derives a theme accent from the current track's artwork.
 *
 * The bitmap is down-scaled to a [SAMPLE_SIZE]² grid before sampling, which keeps the
 * extraction cheap enough to run on every track change without touching the main thread.
 */
@Composable
fun rememberArtworkAccent(track: Track?): Color? {
    val bitmap = rememberArtwork(track)
    var accent by remember(track?.id) { mutableStateOf<Color?>(null) }
    LaunchedEffect(bitmap) {
        accent = artworkAccent(bitmap)
    }
    return accent
}

private suspend fun artworkAccent(bitmap: ImageBitmap?): Color? {
    val android = bitmap?.asAndroidBitmap() ?: return null
    return withContext(Dispatchers.Default) {
        runCatching {
            val scaled: Bitmap = if (android.width == SAMPLE_SIZE && android.height == SAMPLE_SIZE) {
                android
            } else {
                Bitmap.createScaledBitmap(android, SAMPLE_SIZE, SAMPLE_SIZE, true)
            }
            val pixels = IntArray(SAMPLE_SIZE * SAMPLE_SIZE)
            scaled.getPixels(pixels, 0, SAMPLE_SIZE, 0, 0, SAMPLE_SIZE, SAMPLE_SIZE)
            Color(Palette.accentFromPixels(pixels))
        }.getOrNull()
    }
}
