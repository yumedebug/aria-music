package com.zionhuang.innertube

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.longOrNull

data class Artist(val id: String?, val name: String)

data class SongItem(
    val videoId: String,
    val title: String,
    val artists: List<Artist>,
    val album: String?,
    val durationSeconds: Int?,
    val thumbnail: String?,
) {
    val artistText: String
        get() = artists.joinToString(", ") { it.name }.ifBlank { "Unknown Artist" }
}

data class AlbumItem(
    val browseId: String,
    val playlistId: String,
    val title: String,
    val artist: String?,
    val thumbnail: String?,
)

data class PlaylistItem(
    val id: String,
    val title: String,
    val author: String?,
    val thumbnail: String?,
)

data class ArtistItem(
    val id: String,
    val title: String,
    val thumbnail: String?,
)

data class SearchResult(
    val songs: List<SongItem>,
    val albums: List<AlbumItem>,
    val artists: List<ArtistItem>,
    val playlists: List<PlaylistItem>,
    val topResult: SongItem?,
)

data class AlbumPage(val album: AlbumItem, val songs: List<SongItem>)

data class PlaylistPage(val playlist: PlaylistItem, val songs: List<SongItem>)

/** `player` レスポンスから取り出した音声のみのフォーマット1つ。 */
data class StreamFormat(
    val itag: Int,
    val mimeType: String,
    val bitrate: Long,
    val url: String?,
    val signatureCipher: String?,
)

data class PlayerResult(
    val playabilityStatus: String,
    val playabilityReason: String?,
    val expiresInSeconds: Int,
    val formats: List<StreamFormat>,
)

// ---------------------------------------------------------------------------
// JSON ヘルパー。InnerTube のレスポンスは深く頻繁に変わるため、40個以上の
// renderer データクラスに束縛せず、ツリーを防御的に走査して解析する。
// ---------------------------------------------------------------------------

internal fun JsonElement.obj(key: String): JsonObject? = (this as? JsonObject)?.get(key) as? JsonObject

internal fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonObject.array(key: String): List<JsonElement> =
    (this[key] as? JsonArray)?.toList().orEmpty()

internal fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull

internal fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull

internal fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull

internal fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull

/** `runs`: [{ "text": "..." }, ...] を1つの文字列に連結する。 */
internal fun JsonObject.runsText(): String? {
    val runs = array("runs")
    if (runs.isEmpty()) return null
    return runs.mapNotNull { (it as? JsonObject)?.string("text") }.joinToString("").ifBlank { null }
}

/**
 * ツリー内の `key` の出現をすべて集める。InnerTube のレスポンス形状は
 * エンドポイントやバージョンで変わるため、renderer を名前で集めて分類する方が
 * パスを決め打ちするよりはるかに堅牢。
 */
internal fun JsonElement.collectObjects(key: String): List<JsonObject> {
    val out = mutableListOf<JsonObject>()
    fun walk(element: JsonElement) {
        when (element) {
            is JsonObject -> {
                (element[key] as? JsonObject)?.let { out += it }
                element.values.forEach(::walk)
            }
            is JsonArray -> element.forEach(::walk)
            else -> Unit
        }
    }
    walk(this)
    return out
}

/** リスト renderer の `flexColumns[i]...runs` のテキストを解析する。 */
internal fun JsonObject.flexColumnTexts(): List<String> =
    array("flexColumns").mapNotNull { column ->
        val renderer = (column as? JsonObject)
            ?.obj("musicResponsiveListItemFlexColumnRenderer")
            ?: return@mapNotNull null
        renderer.obj("text")?.runsText()
    }

internal val DURATION_REGEX = Regex("""^(\d+):(\d{2})(?::(\d{2}))?$""")

internal fun parseDuration(text: String?): Int? {
    val match = DURATION_REGEX.find(text?.trim().orEmpty()) ?: return null
    val groups = match.groupValues
    return when {
        groups[3].isNotEmpty() ->
            groups[1].toInt() * 3600 + groups[2].toInt() * 60 + groups[3].toInt()
        else -> groups[1].toInt() * 60 + groups[2].toInt()
    }
}

/** `thumbnail.musicThumbnailRenderer.thumbnail.thumbnails` から最良のサムネイルURLを選ぶ。 */
internal fun JsonObject.thumbnailUrl(): String? {
    val thumbnails = obj("thumbnail")
        ?.obj("musicThumbnailRenderer")
        ?.obj("thumbnail")
        ?.array("thumbnails")
        .orEmpty()
    return thumbnails.lastOrNull()
        ?.let { it as? JsonObject }
        ?.string("url")
}
