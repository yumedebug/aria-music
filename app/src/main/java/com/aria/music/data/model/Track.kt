package com.aria.music.data.model

/**
 * A single playable track. Mirrors the metadata we read from [android.provider.MediaStore]
 * and cache in the local database so playlists/favourites survive library rescans.
 */
data class Track(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val uri: String,
    val trackNumber: Int = 0,
    val year: Int = 0,
    /** オンライン曲のアートワークURL（YouTube Music のサムネイル）。ローカル曲は null。 */
    val thumbnailUrl: String? = null,
) {
    val hasArtwork: Boolean get() = albumId > 0

    /** [uri] が `ytmusic:<videoId>` の YouTube Music 曲なら true。 */
    val isOnline: Boolean get() = uri.startsWith(ONLINE_PREFIX)

    /** オンライン曲の YouTube videoId。ローカル曲なら null。 */
    val videoId: String?
        get() = if (isOnline) uri.removePrefix(ONLINE_PREFIX).takeIf { it.isNotEmpty() } else null

    companion object {
        /** 再生用データソースが捕まえてストリームを解決する URI スキーム。 */
        const val ONLINE_PREFIX = "ytmusic:"

        /** オンライン曲を生成する。[id] は MediaStore の id と衝突しないよう負のハッシュ。 */
        fun online(
            videoId: String,
            title: String,
            artist: String,
            album: String = "YouTube Music",
            durationMs: Long = 0L,
            thumbnailUrl: String? = null,
        ): Track = Track(
            id = (-videoId.hashCode().toLong()).let { if (it == 0L) -1L else it },
            title = title,
            artist = artist,
            album = album,
            albumId = 0L,
            durationMs = durationMs,
            uri = ONLINE_PREFIX + videoId,
            thumbnailUrl = thumbnailUrl,
        )
    }
}

/** A group of tracks with the same album identity, used for Home / Library shelves. */
data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val year: Int,
    val tracks: List<Track>,
) {
    val trackCount: Int get() = tracks.size
    val durationMs: Long get() = tracks.sumOf { it.durationMs }
}
