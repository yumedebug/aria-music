package com.aria.music.playback

import android.net.Uri
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
            val player = YouTube.player(videoId, client).getOrNull() ?: continue
            if (player.playabilityStatus != "OK") continue
            val format = player.formats.maxByOrNull { it.bitrate } ?: continue
            val url = NewPipeUtils.getStreamUrl(videoId, format).getOrNull() ?: continue
            return url
        }
        return null
    }
}
