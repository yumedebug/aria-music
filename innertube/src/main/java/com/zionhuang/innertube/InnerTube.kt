package com.zionhuang.innertube

import io.ktor.client.HttpClient
import io.ktor.client.engine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.plugins.compression.deflate
import io.ktor.client.plugins.compression.gzip
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.client.request.userAgent
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.net.Proxy

/**
 * InnerTube の生のトランスポート層。リクエストの context とヘッダを組み立て、
 * `music.youtube.com/youtubei/v1/…` に POST する。レスポンス解析は [YouTube] 側。
 */
internal class InnerTube {

    var locale: YouTubeLocale = YouTubeLocale.default()
    var visitorData: String? = null
    var proxy: Proxy? = null

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    private var httpClient: HttpClient = buildClient()

    private fun buildClient(): HttpClient = HttpClient(OkHttp) {
        expectSuccess = true
        install(ContentEncoding) {
            gzip(0.9F)
            deflate(0.8F)
        }
        proxy?.let { configured -> engine { proxy = configured } }
        defaultRequest { url(YouTubeClient.API_URL_YOUTUBE_MUSIC) }
    }

    private fun HttpRequestBuilder.ytClient(client: YouTubeClient) {
        contentType(ContentType.Application.Json)
        headers {
            append("X-Goog-Api-Format-Version", "1")
            append("X-YouTube-Client-Name", client.clientId)
            append("X-YouTube-Client-Version", client.clientVersion)
            append("X-Origin", YouTubeClient.ORIGIN_YOUTUBE_MUSIC)
            append("Referer", YouTubeClient.REFERER_YOUTUBE_MUSIC)
        }
        userAgent(client.userAgent)
        parameter("prettyPrint", false)
    }

    private fun context(client: YouTubeClient): JsonObject = buildJsonObject {
        putJsonObject("client") {
            put("clientName", client.clientName)
            put("clientVersion", client.clientVersion)
            client.osVersion?.let { put("osVersion", it) }
            put("gl", locale.gl)
            put("hl", locale.hl)
            visitorData?.let { put("visitorData", it) }
        }
    }

    private suspend fun post(
        path: String,
        client: YouTubeClient,
        body: JsonObject,
        continuation: String? = null,
    ): JsonElement {
        val text = httpClient.post(path) {
            ytClient(client)
            if (continuation != null) {
                parameter("continuation", continuation)
                parameter("ctoken", continuation)
            }
            setBody(json.encodeToString(JsonObject.serializer(), body))
        }.bodyAsText()
        return json.parseToJsonElement(text)
    }

    suspend fun search(
        client: YouTubeClient,
        query: String? = null,
        params: String? = null,
        continuation: String? = null,
    ): JsonElement = post(
        path = "search",
        client = client,
        continuation = continuation,
        body = buildJsonObject {
            put("context", context(client))
            query?.let { put("query", it) }
            params?.let { put("params", it) }
        },
    )

    suspend fun browse(
        client: YouTubeClient,
        browseId: String? = null,
        params: String? = null,
        continuation: String? = null,
    ): JsonElement = post(
        path = "browse",
        client = client,
        continuation = continuation,
        body = buildJsonObject {
            put("context", context(client))
            browseId?.let { put("browseId", it) }
            params?.let { put("params", it) }
        },
    )

    suspend fun player(
        client: YouTubeClient,
        videoId: String,
        playlistId: String? = null,
        signatureTimestamp: Int? = null,
    ): JsonElement = post(
        path = "player",
        client = client,
        body = buildJsonObject {
            put("context", context(client))
            put("videoId", videoId)
            playlistId?.let { put("playlistId", it) }
            if (client.useSignatureTimestamp && signatureTimestamp != null) {
                putJsonObject("playbackContext") {
                    putJsonObject("contentPlaybackContext") {
                        put("signatureTimestamp", signatureTimestamp)
                    }
                }
            }
        },
    )
}
