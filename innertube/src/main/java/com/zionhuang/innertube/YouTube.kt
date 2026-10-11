package com.zionhuang.innertube

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.net.Proxy

/**
 * YouTube Music の高レベルAPI。InnerTube のレスポンスを [Models.kt] の小さな
 * ドメインモデルへ解析する。ワイヤ形式は深く頻繁に変わるため、パスを決め打ちせず
 * renderer を名前で集めて分類する。
 *
 * 手法は OuterTune/InnerTune から移植（GPL-3.0）。
 */
object YouTube {

    private val innerTube = InnerTube()

    var locale: YouTubeLocale
        get() = innerTube.locale
        set(value) { innerTube.locale = value }

    var visitorData: String?
        get() = innerTube.visitorData
        set(value) { innerTube.visitorData = value }

    var proxy: Proxy?
        get() = innerTube.proxy
        set(value) { innerTube.proxy = value }

    // ---- 検索 --------------------------------------------------------------

    suspend fun search(query: String): Result<SearchResult> = runCatching {
        parseSearch(innerTube.search(YouTubeClient.WEB_REMIX, query = query))
    }

    suspend fun searchContinuation(continuation: String): Result<SearchResult> = runCatching {
        parseSearch(innerTube.search(YouTubeClient.WEB_REMIX, continuation = continuation))
    }

    // ---- 閲覧 --------------------------------------------------------------

    suspend fun album(browseId: String): Result<AlbumPage> = runCatching {
        val root = innerTube.browse(YouTubeClient.WEB_REMIX, browseId = browseId)
        val header = root.collectObjects("musicResponsiveHeaderRenderer").firstOrNull()
        val playlistId = root.obj("microformat")
            ?.obj("microformatDataRenderer")
            ?.string("urlCanonical")
            ?.substringAfterLast('=')
        val songs = if (playlistId != null) {
            albumSongs(playlistId).getOrDefault(emptyList())
        } else {
            root.collectObjects("musicResponsiveListItemRenderer").mapNotNull(::parseSongItem)
        }
        AlbumPage(
            album = AlbumItem(
                browseId = browseId,
                playlistId = playlistId ?: browseId.removePrefix("MPRE"),
                title = header?.obj("title")?.runsText() ?: "Unknown Album",
                artist = header?.obj("straplineTextOne")?.runsText(),
                thumbnail = header?.thumbnailUrl(),
            ),
            songs = songs,
        )
    }

    suspend fun albumSongs(playlistId: String): Result<List<SongItem>> = runCatching {
        val root = innerTube.browse(YouTubeClient.WEB_REMIX, browseId = "VL$playlistId")
        root.collectObjects("musicResponsiveListItemRenderer").mapNotNull(::parseSongItem)
    }

    suspend fun playlist(playlistId: String): Result<PlaylistPage> = runCatching {
        val root = innerTube.browse(YouTubeClient.WEB_REMIX, browseId = "VL$playlistId")
        val header = root.collectObjects("musicResponsiveHeaderRenderer").firstOrNull()
        PlaylistPage(
            playlist = PlaylistItem(
                id = playlistId,
                title = header?.obj("title")?.runsText() ?: "Unknown Playlist",
                author = header?.obj("straplineTextOne")?.runsText(),
                thumbnail = header?.thumbnailUrl(),
            ),
            songs = root.collectObjects("musicResponsiveListItemRenderer").mapNotNull(::parseSongItem),
        )
    }

    suspend fun artistSongs(browseId: String): Result<List<SongItem>> = runCatching {
        val root = innerTube.browse(YouTubeClient.WEB_REMIX, browseId = browseId)
        root.collectObjects("musicResponsiveListItemRenderer").mapNotNull(::parseSongItem)
    }

    // ---- プレイヤー -----------------------------------------------------------

    /**
     * [videoId] の生の `player` レスポンス。クライアントによってはストリームURLが
     * 署名で保護されているため、[StreamFormat] を [NewPipeUtils.getStreamUrl] に渡す。
     */
    suspend fun player(
        videoId: String,
        client: YouTubeClient = YouTubeClient.ANDROID_VR_NO_AUTH,
        playlistId: String? = null,
    ): Result<PlayerResult> = runCatching {
        val signatureTimestamp =
            if (client.useSignatureTimestamp) NewPipeUtils.getSignatureTimestamp(videoId).getOrNull() else null
        parsePlayer(innerTube.player(client, videoId, playlistId, signatureTimestamp))
    }

    // ---- 解析 --------------------------------------------------------------

    private sealed interface ParsedItem {
        data class Song(val item: SongItem) : ParsedItem
        data class Album(val item: AlbumItem) : ParsedItem
        data class Playlist(val item: PlaylistItem) : ParsedItem
        data class Artist(val item: ArtistItem) : ParsedItem
    }

    private fun parseSearch(root: JsonElement): SearchResult {
        val songs = mutableListOf<SongItem>()
        val albums = mutableListOf<AlbumItem>()
        val artists = mutableListOf<ArtistItem>()
        val playlists = mutableListOf<PlaylistItem>()

        root.collectObjects("musicResponsiveListItemRenderer").forEach { renderer ->
            when (val parsed = classify(renderer)) {
                is ParsedItem.Song -> songs.addIfNew(parsed.item) { it.videoId }
                is ParsedItem.Album -> albums.addIfNew(parsed.item) { it.browseId }
                is ParsedItem.Artist -> artists.addIfNew(parsed.item) { it.id }
                is ParsedItem.Playlist -> playlists.addIfNew(parsed.item) { it.id }
                null -> Unit
            }
        }
        return SearchResult(songs, albums, artists, playlists, topResult = null)
    }

    private inline fun <T> MutableList<T>.addIfNew(item: T, key: (T) -> String) {
        if (none { key(it) == key(item) }) add(item)
    }

    private fun classify(renderer: JsonObject): ParsedItem? {
        val videoId = renderer.obj("playlistItemData")?.string("videoId")
        if (videoId != null) {
            return parseSongItem(renderer)?.let(ParsedItem::Song)
        }
        val browseId = renderer.obj("navigationEndpoint")
            ?.obj("browseEndpoint")
            ?.string("browseId")
            ?: return null
        val columns = renderer.flexColumnTexts()
        val title = columns.getOrNull(0) ?: return null
        val thumbnail = renderer.thumbnailUrl()
        return when {
            browseId.startsWith("MPRE") -> ParsedItem.Album(
                AlbumItem(browseId, browseId.removePrefix("MPRE"), title, columns.getOrNull(1), thumbnail),
            )
            browseId.startsWith("VL") -> ParsedItem.Playlist(
                PlaylistItem(browseId.removePrefix("VL"), title, columns.getOrNull(1), thumbnail),
            )
            browseId.startsWith("UC") -> ParsedItem.Artist(ArtistItem(browseId, title, thumbnail))
            else -> null
        }
    }

    private fun parseSongItem(renderer: JsonObject): SongItem? {
        val videoId = renderer.obj("playlistItemData")?.string("videoId")
            ?: renderer.obj("overlay")
                ?.obj("musicItemThumbnailOverlayRenderer")
                ?.obj("content")
                ?.obj("musicPlayButtonRenderer")
                ?.obj("playNavigationEndpoint")
                ?.obj("watchEndpoint")
                ?.string("videoId")
            ?: return null
        val columns = renderer.flexColumnTexts()
        val title = columns.getOrNull(0) ?: "Unknown Title"
        val duration = columns.asReversed().firstNotNullOfOrNull(::parseDuration)
        val album = columns.getOrNull(2)?.takeIf { it.isNotBlank() && parseDuration(it) == null }
        return SongItem(
            videoId = videoId,
            title = title,
            artists = parseArtists(renderer, columns.getOrNull(1)),
            album = album,
            durationSeconds = duration,
            thumbnail = renderer.thumbnailUrl(),
        )
    }

    private fun parseArtists(renderer: JsonObject, fallback: String?): List<Artist> {
        val text = renderer.array("flexColumns")
            .getOrNull(1)
            ?.let { it as? JsonObject }
            ?.obj("musicResponsiveListItemFlexColumnRenderer")
            ?.obj("text")
        val linked = text?.array("runs").orEmpty()
            .mapNotNull { it as? JsonObject }
            .mapNotNull { run ->
                val name = run.string("text") ?: return@mapNotNull null
                val id = run.obj("navigationEndpoint")?.obj("browseEndpoint")?.string("browseId")
                    ?: return@mapNotNull null
                if (name.isBlank() || name == " • " || name == "," || name == " & ") null
                else Artist(id, name)
            }
        if (linked.isNotEmpty()) return linked
        return fallback?.takeIf { it.isNotBlank() }
            ?.let { listOf(Artist(null, it)) }
            .orEmpty()
    }

    private fun parsePlayer(root: JsonElement): PlayerResult {
        val status = root.obj("playabilityStatus")
        val streaming = root.obj("streamingData")
        val formats = streaming?.array("adaptiveFormats").orEmpty().mapNotNull { element ->
            val format = element as? JsonObject ?: return@mapNotNull null
            val mimeType = format.string("mimeType") ?: return@mapNotNull null
            if (!mimeType.startsWith("audio")) return@mapNotNull null
            StreamFormat(
                itag = format.int("itag") ?: 0,
                mimeType = mimeType,
                bitrate = format.long("bitrate") ?: 0L,
                url = format.string("url"),
                signatureCipher = format.string("signatureCipher") ?: format.string("cipher"),
            )
        }
        return PlayerResult(
            playabilityStatus = status?.string("status") ?: "UNKNOWN",
            playabilityReason = status?.string("reason"),
            expiresInSeconds = streaming?.int("expiresInSeconds") ?: 0,
            formats = formats,
        )
    }
}
