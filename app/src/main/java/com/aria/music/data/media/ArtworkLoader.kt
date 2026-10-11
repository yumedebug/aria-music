package com.aria.music.data.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.LruCache
import com.aria.music.data.model.Track
import com.zionhuang.innertube.YouTubeClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * アートワークをメモリキャッシュ付きで読み込む。
 *
 * ローカル曲は音声ファイル内蔵の画像を使う。`content://media/external/audio/albumart/...`
 * のようなアートワークAPIは Android 10 で非推奨になり、最近の Pixel では信頼できないため、
 * [MediaMetadataRetriever] で音声ファイルから直接取り出し、メモリに載せやすいサイズへ
 * ダウンサンプリングする。オンライン曲はサムネイルURLから取得する。
 */
object ArtworkLoader {

    private const val MAX_TARGET_PX = 1024
    private const val CACHE_SIZE = 48

    private val httpClient by lazy { OkHttpClient() }

    private val cache = object : LruCache<Long, Bitmap>(CACHE_SIZE) {
        override fun sizeOf(key: Long, value: Bitmap): Int = 1
    }

    suspend fun load(context: Context, track: Track): Bitmap? {
        cache.get(track.id)?.let { return it }
        val bitmap = withContext(Dispatchers.IO) {
            cache.get(track.id) ?: if (track.isOnline) {
                loadRemote(track.thumbnailUrl)
            } else {
                loadEmbedded(context, track)
            }
        }
        return bitmap?.also { cache.put(track.id, it) }
    }

    fun cached(trackId: Long): Bitmap? = cache.get(trackId)

    /** オンライン曲のサムネイルをURLから取得してダウンサンプリングする。 */
    private fun loadRemote(url: String?): Bitmap? {
        if (url.isNullOrBlank()) return null
        return runCatching {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", YouTubeClient.USER_AGENT_WEB)
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@runCatching null
                val bytes = response.body?.bytes() ?: return@runCatching null
                decodeDownsampled(bytes)
            }
        }.getOrNull()
    }

    /** ローカル音声ファイルに埋め込まれた画像を取り出す。 */
    private fun loadEmbedded(context: Context, track: Track): Bitmap? = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.parse(track.uri))
            val raw = retriever.embeddedPicture ?: return@runCatching null
            decodeDownsampled(raw)
        } finally {
            runCatching { retriever.release() }
        }
    }.getOrNull()

    /**
     * [bytes] をデコードし、どの辺も [MAX_TARGET_PX] を超えないよう縮小する。
     * 大きな埋め込みアートでヒープが膨らむのを防ぐ。
     */
    fun decodeDownsampled(bytes: ByteArray, maxPx: Int = MAX_TARGET_PX): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val longest = maxOf(bounds.outWidth, bounds.outHeight)
        var sample = 1
        while (longest / sample > maxPx) sample *= 2
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }
}
