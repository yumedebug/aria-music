package com.aria.music.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import com.aria.music.AriaApp
import com.aria.music.data.model.Track
import com.aria.music.data.repo.AppSettings
import com.aria.music.playback.PlaybackState
import com.aria.music.ui.components.MiniPlayer
import com.aria.music.ui.player.NowPlayingScreen
import com.aria.music.ui.theme.AriaTheme
import com.aria.music.ui.theme.BrandAccent
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Renders the player UI for real under Robolectric.
 *
 * The playback *service* cannot be bound in this environment (Robolectric hands Media3 a
 * null `ComponentName`), so these tests drive the screens with a fixed [PlaybackState]
 * instead. That is enough to verify the composition, theme and control wiring, which is
 * exactly what the screens are responsible for.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = AriaApp::class)
class PlayerUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val track = Track(
        id = 42L,
        title = "Test Song",
        artist = "Test Artist",
        album = "Test Album",
        albumId = 7L,
        durationMs = 180_000L,
        uri = "content://media/external/audio/media/42",
    )

    private val playingState = PlaybackState(
        connected = true,
        current = track,
        isPlaying = true,
        positionMs = 30_000L,
        durationMs = 180_000L,
        queue = listOf(track),
        currentIndex = 0,
    )

    @Test
    fun nowPlayingShowsTrackMetadataAndTransport() {
        val container = ApplicationProvider.getApplicationContext<AriaApp>().container
        composeRule.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                AriaTheme(settings = AppSettings()) {
                    NowPlayingScreen(
                        state = playingState,
                        accent = BrandAccent,
                        onCollapse = {},
                        onTogglePlay = {},
                        onNext = {},
                        onPrevious = {},
                        onSeek = {},
                        onToggleShuffle = {},
                        onCycleRepeat = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Test Song").assertIsDisplayed()
        composeRule.onNodeWithText("Test Artist").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Next").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Previous").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Lyrics").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Playing next").assertIsDisplayed()
        // Elapsed time and the remaining-time readout beneath the scrubber.
        composeRule.onNodeWithText("0:30").assertIsDisplayed()
        composeRule.onNodeWithText("-2:30").assertIsDisplayed()
    }

    @Test
    fun miniPlayerShowsTitleAndTransport() {
        val container = ApplicationProvider.getApplicationContext<AriaApp>().container
        composeRule.setContent {
            CompositionLocalProvider(LocalAppContainer provides container) {
                AriaTheme(settings = AppSettings()) {
                    MiniPlayer(
                        state = playingState,
                        onExpand = {},
                        onTogglePlay = {},
                        onNext = {},
                    )
                }
            }
        }

        composeRule.onNodeWithText("Test Song").assertIsDisplayed()
        composeRule.onNodeWithText("Test Artist").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Pause").assertIsDisplayed()
    }
}
