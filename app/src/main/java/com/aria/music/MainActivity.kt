package com.aria.music

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import com.aria.music.ui.AriaRoot
import com.aria.music.ui.LocalAppContainer

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Draw behind the system bars (edge-to-edge) before the first frame.
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val container = (application as AriaApp).container
        setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                AriaRoot()
            }
        }
    }
}
