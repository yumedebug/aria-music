package com.aria.music.data.online

import com.aria.music.data.model.Track
import com.zionhuang.innertube.SongItem
import com.zionhuang.innertube.YouTube

/** オンラインのまとまり（アルバム / プレイリスト / アーティスト）の種別。 */
enum class OnlineCollectionKind(val label: String) {
    ALBUM("アルバム"),
    PLAYLIST("プレイリスト"),
    ARTIST("アーティスト"),
}

/** 検索で見つかったオンラインのまとまり。[id] は再生時の解決に使う YouTube のID。 */
data class OnlineCollection(
    val kind: OnlineCollectionKind,
    val id: String,
    val title: String,
    val subtitle: String,
    val thumbnail: String?,
)

/** 検索結果のオンライン分: 曲とまとまり。 */
data class OnlineSearch(
    val songs: List<Track>,
    val collections: List<OnlineCollection>,
)

/**
 * `:innertube` モジュールの薄い窓口。YouTube Music の結果を既存の [Track] に
 * マッピングするので、現在の UI と再生パイプラインは変更不要。オンライン曲は
 * `ytmusic:<videoId>` という URI を持つだけで、再生時に
 * [com.aria.music.playback.OnlineStreamResolver] が実URLへ解決する。
 */
class YouTubeMusicRepository {

    suspend fun search(query: String): OnlineSearch {
        val result = YouTube.search(query).getOrNull()
            ?: return OnlineSearch(emptyList(), emptyList())
        return OnlineSearch(
            songs = result.songs.map(::toTrack),
            collections = buildList {
                result.albums.forEach {
                    add(OnlineCollection(OnlineCollectionKind.ALBUM, it.playlistId, it.title, it.artist.orEmpty(), it.thumbnail))
                }
                result.playlists.forEach {
                    add(OnlineCollection(OnlineCollectionKind.PLAYLIST, it.id, it.title, it.author.orEmpty(), it.thumbnail))
                }
                result.artists.forEach {
                    add(OnlineCollection(OnlineCollectionKind.ARTIST, it.id, it.title, "", it.thumbnail))
                }
            },
        )
    }

    suspend fun playlistTracks(playlistId: String): List<Track> =
        YouTube.playlist(playlistId).getOrNull()?.songs.orEmpty().map(::toTrack)

    suspend fun albumTracks(playlistId: String): List<Track> =
        YouTube.albumSongs(playlistId).getOrNull().orEmpty().map(::toTrack)

    suspend fun artistTracks(browseId: String): List<Track> =
        YouTube.artistSongs(browseId).getOrNull().orEmpty().map(::toTrack)

    /** まとまりの中身を解決する（まとまりをタップした時に再生する曲一覧）。 */
    suspend fun tracksOf(collection: OnlineCollection): List<Track> = when (collection.kind) {
        OnlineCollectionKind.ALBUM -> albumTracks(collection.id)
        OnlineCollectionKind.PLAYLIST -> playlistTracks(collection.id)
        OnlineCollectionKind.ARTIST -> artistTracks(collection.id)
    }

    private fun toTrack(song: SongItem): Track = Track.online(
        videoId = song.videoId,
        title = song.title,
        artist = song.artistText,
        album = song.album ?: "YouTube Music",
        durationMs = (song.durationSeconds ?: 0) * 1000L,
        thumbnailUrl = song.thumbnail,
    )
}

/** [OnlineCollection] を既存の [TrackRow] で描画するための表示用トラック。 */
fun OnlineCollection.asTrack(): Track = Track.online(
    videoId = id,
    title = title,
    artist = subtitle.ifBlank { kind.label },
    album = kind.label,
    thumbnailUrl = thumbnail,
)
