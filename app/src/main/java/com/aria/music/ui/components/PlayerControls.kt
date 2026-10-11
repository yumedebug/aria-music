package com.aria.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A scrubber that behaves like the one in the Now Playing screen: a hairline track that
 * thickens while the user drags, with the elapsed portion filled in the accent colour.
 */
@Composable
fun Scrubber(
    positionMs: Long,
    durationMs: Long,
    bufferedMs: Long,
    accent: Color,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragFraction by remember { mutableStateOf<Float?>(null) }
    val progressFraction = if (durationMs > 0L) {
        (positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f
    val bufferedFraction = if (durationMs > 0L) {
        (bufferedMs.toFloat() / durationMs).coerceIn(0f, 1f)
    } else 0f
    val fraction = dragFraction ?: progressFraction
    val thickness by animateFloatAsState(
        targetValue = if (dragFraction != null) 7f else 3f,
        label = "scrubberThickness",
    )
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(28.dp)
            .pointerInput(durationMs) {
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        dragFraction = scrubFraction(offset.x, size.width)
                    },
                    onDragEnd = {
                        dragFraction?.let { fraction ->
                            onSeek((fraction * durationMs).toLong())
                        }
                        dragFraction = null
                    },
                    onDragCancel = { dragFraction = null },
                    onHorizontalDrag = { change, _ ->
                        dragFraction = scrubFraction(change.position.x, size.width)
                    },
                )
            }
            .pointerInput(durationMs) {
                detectTapGestures { offset ->
                    onSeek((scrubFraction(offset.x, size.width) * durationMs).toLong())
                }
            },
    ) {
        val centerY = size.height / 2f
        val startX = thickness / 2f
        val trackWidth = (size.width - thickness).coerceAtLeast(1f)
        drawLine(
            color = inactiveColor,
            start = Offset(startX, centerY),
            end = Offset(startX + trackWidth, centerY),
            strokeWidth = thickness,
            cap = StrokeCap.Round,
        )
        if (bufferedFraction > 0f) {
            drawLine(
                color = inactiveColor.copy(alpha = 0.4f),
                start = Offset(startX, centerY),
                end = Offset(startX + trackWidth * bufferedFraction, centerY),
                strokeWidth = thickness,
                cap = StrokeCap.Round,
            )
        }
        drawLine(
            color = accent,
            start = Offset(startX, centerY),
            end = Offset(startX + trackWidth * fraction, centerY),
            strokeWidth = thickness,
            cap = StrokeCap.Round,
        )
        if (dragFraction != null) {
            val x = startX + trackWidth * fraction
            drawCircle(
                color = accent,
                radius = 9f,
                center = Offset(x, centerY),
            )
        }
    }
}

/** Position fraction for a pointer at [offsetX] over a track of [width] pixels. */
fun scrubFraction(offsetX: Float, width: Int): Float =
    if (width <= 0) 0f else (offsetX / width).coerceIn(0f, 1f)

/**
 * The main transport row: shuffle, previous, play/pause, next, repeat. The play button is
 * the visual anchor, exactly as in the reference layout.
 */
@Composable
fun TransportControls(
    isPlaying: Boolean,
    shuffle: Boolean,
    repeatMode: Int,
    accent: Color,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    playButtonSize: Dp = 64.dp,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SecondaryToggle(
            selected = shuffle,
            onClick = onToggleShuffle,
            enabled = enabled,
            contentDescription = "Shuffle",
        ) {
            Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(22.dp))
        }
        IconButton(onClick = onPrevious, enabled = enabled) {
            Icon(
                Icons.Rounded.SkipPrevious,
                contentDescription = "Previous",
                modifier = Modifier.size(34.dp),
            )
        }
        IconButton(
            onClick = onTogglePlay,
            enabled = enabled,
            modifier = Modifier.size(playButtonSize),
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = accent,
                modifier = Modifier.size(playButtonSize - 8.dp),
            )
        }
        IconButton(onClick = onNext, enabled = enabled) {
            Icon(
                Icons.Rounded.SkipNext,
                contentDescription = "Next",
                modifier = Modifier.size(34.dp),
            )
        }
        SecondaryToggle(
            selected = repeatMode != 0,
            onClick = onCycleRepeat,
            enabled = enabled,
            contentDescription = "Repeat",
        ) {
            Icon(
                imageVector = if (repeatMode == 1) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Composable
private fun SecondaryToggle(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    contentDescription: String,
    content: @Composable () -> Unit,
) {
    IconButton(onClick = onClick, enabled = enabled) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .alpha(if (selected) 1f else 0.55f),
            contentAlignment = Alignment.Center,
        ) { content() }
    }
}
