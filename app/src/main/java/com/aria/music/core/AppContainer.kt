package com.aria.music.core

import android.content.Context
import com.aria.music.data.db.AriaDatabase
import com.aria.music.data.media.LyricsRepository
import com.aria.music.data.media.MediaStoreDataSource
import com.aria.music.data.online.YouTubeMusicRepository
import com.aria.music.data.repo.LibraryRepository
import com.aria.music.data.repo.SettingsRepository
import com.aria.music.playback.PlaybackConnection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency graph, mirroring the small, framework-free DI style used across the
 * rest of the codebase. Everything is lazy so opening the app stays cheap.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val applicationScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val database: AriaDatabase by lazy { AriaDatabase.create(appContext) }

    val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(MediaStoreDataSource(appContext), database.libraryDao())
    }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val lyricsRepository: LyricsRepository by lazy { LyricsRepository(appContext) }

    /** オンラインの YouTube Music 音源。InnerTube クライアントは状態を持たないため低コスト。 */
    val youTubeMusic: YouTubeMusicRepository by lazy { YouTubeMusicRepository() }

    val playback: PlaybackConnection by lazy {
        PlaybackConnection(appContext, applicationScope, libraryRepository)
    }
}
