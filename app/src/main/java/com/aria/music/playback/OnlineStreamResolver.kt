package com.aria.music.playback

import android.net.Uri
import android.util.Log
import com.zionhuang.innertube.NewPipeUtils
import com.zionhuang.innertube.YouTube
import com.zionhuang.innertube.YouTubeClient
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

/**
 * YouTube Music の videoId を、直接再生できる音声URLへ解決する。
 *
 * Media3 の [androidx.media3.datasource.ResolvingDataSource] から呼ばれ、
 * 読み込みスレッドで動くためここでのブロッキングは問題ない。YouTube は一部の
 * クライアントにしか使えるストリームURLを返さないため順に試し、署名解除できる
 * フォーマットを返した最初のクライアントを採用する。
 */
object OnlineStreamResolver {

    private const val SCHEME = "ytmusic"
    private const val TAG = "OnlineStreamResolver"

    private val cache = ConcurrentHashMap<String, String>()

    private val clients = listOf(
        YouTubeClient.ANDROID_VR_NO_AUTH,
        YouTubeClient.IOS,
        YouTubeClient.ANDROID,
        YouTubeClient.WEB_REMIX,
    )

    /** Extracts the video id from a `ytmusic:<videoId>` URI, or null otherwise. */
    fun videoIdOf(uri: Uri): String? =
        if (uri.scheme == SCHEME) uri.schemeSpecificPart?.takeIf { it.isNotEmpty() } else null

    fun resolve(videoId: String): String {
        cache[videoId]?.let { return it }
        val url = runBlocking { resolveStream(videoId) }
            ?: error("Could not resolve a stream for video $videoId")
        cache[videoId] = url
        return url
    }

    private suspend fun resolveStream(videoId: String): String? {
        for (client in clients) {
            val result = YouTube.player(videoId, client)
            val player = result.getOrNull()
            if (player == null) {
                Log.w(TAG, "player($client) threw for $videoId: ${result.exceptionOrNull()?.message}")
                continue
            }
            if (player.playabilityStatus != "OK") {
                Log.w(TAG, "player($client) not playable for $videoId: " +
                    "${player.playabilityStatus} ${player.playabilityReason.orEmpty()}")
                continue
            }
            // 帯域の高い順に試す。1本でも再生できればそれを採用する。
            for (format in player.formats.sortedByDescending { it.bitrate }) {
                val url = NewPipeUtils.getStreamUrl(videoId, format).getOrNull()
                if (url != null) {
                    Log.i(TAG, "resolved $videoId with $client itag=${format.itag}")
                    return url
                }
            }
            Log.w(TAG, "player($client) returned no usable stream for $videoId")
        }
        Log.e(TAG, "Could not resolve a stream for video $videoId with clients=$clients")
        return null
    }
}
