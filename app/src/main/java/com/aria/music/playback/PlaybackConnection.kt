package com.aria.music.playback

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.aria.music.data.model.Track
import com.aria.music.data.repo.LibraryRepository
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the app's [MediaController] and mirrors the player into a single [PlaybackState].
 *
 * Living in the DI container (not a screen) means playback state, the mini player and the
 * full player all read from the same source, so switching tabs or opening the player never
 * resets or forks playback.
 */
class PlaybackConnection(
    private val context: Context,
    private val scope: CoroutineScope,
    private val library: LibraryRepository,
) {
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    private var pollJob: Job? = null
    private var lastRecordedTrackId: Long? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    fun connect() {
        if (controller != null || controllerFuture != null) return
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        controllerFuture = future
        future.addListener(
            {
                runCatching { future.get() }.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(playerListener)
                    _state.value = readState(mediaController)
                    startPolling()
                }
            },
            MoreExecutors.directExecutor(),
        )
    }

    fun release() {
        pollJob?.cancel()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        _state.value = PlaybackState()
    }

    // ---- transport ---------------------------------------------------------

    fun playTracks(tracks: List<Track>, startIndex: Int) {
        val media = controller ?: return
        val items = tracks.map(MediaItemMapper::toMediaItem)
        media.setMediaItems(items, startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)), 0L)
        media.prepare()
        media.play()
    }

    fun togglePlayPause() {
        val media = controller ?: return
        if (media.isPlaying) media.pause() else {
            if (media.playbackState == Player.STATE_IDLE) media.prepare()
            media.play()
        }
    }

    fun play() {
        val media = controller ?: return
        if (media.playbackState == Player.STATE_IDLE) media.prepare()
        media.play()
    }

    fun pause() = controller?.pause()

    fun next() {
        controller?.seekToNextMediaItem()
    }

    fun previous() {
        val media = controller ?: return
        // Matches platform behaviour: restart the current track if we're past 3 seconds.
        if (media.currentPosition > RESTART_THRESHOLD_MS) media.seekTo(0) else media.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun seekToIndex(index: Int) {
        val media = controller ?: return
        if (index in 0 until media.mediaItemCount) {
            media.seekTo(index, 0L)
            media.play()
        }
    }

    fun toggleShuffle() {
        val media = controller ?: return
        media.shuffleModeEnabled = !media.shuffleModeEnabled
    }

    fun cycleRepeat() {
        val media = controller ?: return
        media.repeatMode = when (media.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
    }

    // ---- queue editing -----------------------------------------------------

    fun enqueueNext(track: Track) {
        val media = controller ?: return
        val index = (media.currentMediaItemIndex + 1).coerceAtMost(media.mediaItemCount)
        media.addMediaItem(index, MediaItemMapper.toMediaItem(track))
    }

    fun enqueueEnd(track: Track) {
        controller?.addMediaItem(MediaItemMapper.toMediaItem(track))
    }

    fun removeQueueItem(index: Int) {
        val media = controller ?: return
        if (index in 0 until media.mediaItemCount) media.removeMediaItem(index)
    }

    fun moveQueueItem(from: Int, to: Int) {
        val media = controller ?: return
        if (from == to) return
        if (from in 0 until media.mediaItemCount && to in 0 until media.mediaItemCount) {
            media.moveMediaItem(from, to)
        }
    }

    fun skipToQueueItem(index: Int) = seekToIndex(index)

    fun clearQueue() {
        controller?.clearMediaItems()
    }

    // ---- internals ---------------------------------------------------------

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            _state.value = readState(player)
            recordHistory(player)
        }
    }

    private fun recordHistory(player: Player) {
        val track = currentTrack(player) ?: return
        // オンライン曲はローカル（Room）ライブラリに行が無いため記録しない。
        if (track.isOnline) return
        if (track.id == lastRecordedTrackId) return
        lastRecordedTrackId = track.id
        scope.launch { library.recordPlayed(track) }
    }

    private fun currentTrack(player: Player): Track? =
        player.currentMediaItem?.let(MediaItemMapper::toTrack)

    private fun readState(player: Player): PlaybackState {
        val count = player.mediaItemCount
        val queue = buildList {
            for (i in 0 until count) {
                MediaItemMapper.toTrack(player.getMediaItemAt(i))?.let { add(it) }
            }
            // Degrade gracefully: keep the current item even if its extras are missing.
            if (isEmpty() && count > 0) currentTrack(player)?.let { add(it) }
        }
        val index = if (count > 0) player.currentMediaItemIndex.coerceIn(0, count - 1) else 0
        return PlaybackState(
            connected = true,
            current = currentTrack(player) ?: queue.getOrNull(index),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0L } ?: queue.getOrNull(index)?.durationMs ?: 0L,
            bufferedMs = player.bufferedPosition.coerceAtLeast(0L),
            shuffle = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queue = queue,
            currentIndex = index,
        )
    }

    private fun startPolling() {
        pollJob?.cancel()
        // MediaController のメソッドは生成元スレッド（この場合はメイン）からしか
        // 呼べない。バックグラウンドで触ると IllegalStateException で即クラッシュ
        // するため、ポーリング自体を必ずメインスレッドで回す。
        pollJob = scope.launch(Dispatchers.Main) {
            while (isActive) {
                controller?.let { media ->
                    if (media.isPlaying || media.playbackState == Player.STATE_BUFFERING) {
                        val current = _state.value
                        // Only push a new frame when the position actually moved, so the
                        // UI does not recompose on every tick while paused.
                        if (current.positionMs != media.currentPosition) {
                            _state.value = current.copy(
                                positionMs = media.currentPosition.coerceAtLeast(0L),
                                bufferedMs = media.bufferedPosition.coerceAtLeast(0L),
                                isPlaying = media.isPlaying,
                            )
                        }
                    }
                }
                delay(POLL_INTERVAL_MS)
            }
        }
    }

    private companion object {
        const val POLL_INTERVAL_MS = 500L
        const val RESTART_THRESHOLD_MS = 3_000L
    }
}
