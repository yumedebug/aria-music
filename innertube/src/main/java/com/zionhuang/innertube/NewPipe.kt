package com.zionhuang.innertube

import com.zionhuang.innertube.YouTubeClient.Companion.USER_AGENT_WEB
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager
import io.ktor.http.URLBuilder
import io.ktor.http.parseQueryString
import java.net.Proxy

/**
 * NewPipeExtractor のネットワーク実装。YouTube 音声URLの `signatureCipher`
 * （`s`/`sp`）と `n` スロットリングパラメータの解除にのみ使う。これは
 * YouTube の player JS を実行しないと解けない部分。
 */
private class NewPipeDownloaderImpl(proxy: Proxy?) : Downloader() {

    private val client = OkHttpClient.Builder()
        .apply { proxy?.let { proxy(it) } }
        .build()

    override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): Response {
        val builder = Request.Builder()
            .method(request.httpMethod(), request.dataToSend()?.toRequestBody())
            .url(request.url())
            .addHeader("User-Agent", USER_AGENT_WEB)

        request.headers().forEach { (name, values) ->
            when {
                values.size > 1 -> {
                    builder.removeHeader(name)
                    values.forEach { builder.addHeader(name, it) }
                }
                values.size == 1 -> builder.header(name, values[0])
            }
        }

        val response = client.newCall(builder.build()).execute()
        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", request.url())
        }
        val body = response.body?.string()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            body,
            response.request.url.toString(),
        )
    }
}

object NewPipeUtils {

    @Volatile
    private var initialized = false

    private fun ensureInitialized() {
        if (initialized) return
        synchronized(this) {
            if (!initialized) {
                NewPipe.init(NewPipeDownloaderImpl(YouTube.proxy))
                initialized = true
            }
        }
    }

    fun getSignatureTimestamp(videoId: String): Result<Int> = runCatching {
        ensureInitialized()
        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId)
    }

    /** 署名で保護された [format] を、直接再生できるURLに変換する。 */
    fun getStreamUrl(videoId: String, format: StreamFormat): Result<String> = runCatching {
        ensureInitialized()
        val url = format.url ?: format.signatureCipher?.let { cipher ->
            val params = parseQueryString(cipher)
            val obfuscated = params["s"] ?: error("Could not parse cipher signature")
            val signatureParam = params["sp"] ?: "signature"
            val raw = params["url"] ?: error("Could not parse cipher url")
            val builder = URLBuilder(raw)
            builder.parameters[signatureParam] =
                YoutubeJavaScriptPlayerManager.deobfuscateSignature(videoId, obfuscated)
            builder.buildString()
        } ?: error("Could not find format url")

        YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(videoId, url)
    }
}
