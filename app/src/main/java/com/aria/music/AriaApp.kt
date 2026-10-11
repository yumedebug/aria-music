package com.aria.music

import android.app.Application
import com.aria.music.core.AppContainer

class AriaApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
