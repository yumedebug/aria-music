package com.zionhuang.innertube

import java.util.Locale

/** InnerTube の `context` に載せる地域・言語ペア。 */
data class YouTubeLocale(val gl: String, val hl: String) {
    companion object {
        fun default(): YouTubeLocale {
            val locale = Locale.getDefault()
            return YouTubeLocale(
                gl = locale.country.ifBlank { "US" },
                hl = locale.toLanguageTag().ifBlank { "en" },
            )
        }
    }
}

/**
 * 偽装可能な YouTube クライアント1つ分。クライアントごとに返るストリーム形式が
 * 違うため、解決側でこれらを順に切り替える。
 */
data class YouTubeClient(
    val clientName: String,
    val clientVersion: String,
    val clientId: String,
    val userAgent: String,
    val osVersion: String? = null,
    val loginSupported: Boolean = false,
    val useSignatureTimestamp: Boolean = false,
) {
    companion object {
        const val USER_AGENT_WEB =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
        const val ORIGIN_YOUTUBE_MUSIC = "https://music.youtube.com"
        const val REFERER_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/"
        const val API_URL_YOUTUBE_MUSIC = "$ORIGIN_YOUTUBE_MUSIC/youtubei/v1/"

        /** メタデータ取得と search/browse 用のメインクライアント。 */
        val WEB_REMIX = YouTubeClient(
            clientName = "WEB_REMIX",
            clientVersion = "1.20240729.01.00",
            clientId = "67",
            userAgent = USER_AGENT_WEB,
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        val ANDROID = YouTubeClient(
            clientName = "ANDROID",
            clientVersion = "19.35.36",
            clientId = "3",
            userAgent = "com.google.android.youtube/19.35.36 (Linux; U; Android 11) gzip",
            loginSupported = true,
            useSignatureTimestamp = true,
        )

        /** PO トークン無しでも直接再生できる音声URLを返しやすいクライアント。 */
        val ANDROID_VR_NO_AUTH = YouTubeClient(
            clientName = "ANDROID_VR",
            clientVersion = "1.60.19",
            clientId = "28",
            userAgent = "com.google.android.apps.youtube.vr.oculus/1.60.19 " +
                "(Linux; U; Android 12; en_US; Oculus Quest 3; Build/SQ3A.220605.009.A1; " +
                "Cronet/132.0.6808.3)",
        )

        val IOS = YouTubeClient(
            clientName = "IOS",
            clientVersion = "19.29.1",
            clientId = "5",
            userAgent = "com.google.ios.youtube/19.29.1 (iPhone16,2; U; CPU iOS 17_5_1 like Mac OS X;)",
            osVersion = "17.5.1.21F90",
            loginSupported = true,
        )
    }
}
