package com.aria.music.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Robolectric is used here only because the accent normaliser touches android.graphics.Color. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PaletteTest {

    @Test
    fun emptyPixelsFallBackToDefaultAccent() {
        assertEquals(Palette.DEFAULT_ACCENT, Palette.accentFromPixels(IntArray(0)))
    }

    @Test
    fun greyPixelsFallBackToDefaultAccent() {
        val greys = IntArray(64) { Palette.argb(255, 128, 128, 128) }
        assertEquals(Palette.DEFAULT_ACCENT, Palette.accentFromPixels(greys))
    }

    @Test
    fun saturatedPixelsProduceThatHue() {
        val reds = IntArray(64) { Palette.argb(255, 220, 40, 60) }
        val accent = Palette.accentFromPixels(reds)
        val r = (accent shr 16) and 0xFF
        val g = (accent shr 8) and 0xFF
        val b = accent and 0xFF
        assertTrue("red channel should dominate", r > g && r > b)
    }

    @Test
    fun computesSaturation() {
        assertEquals(0f, Palette.saturation(100, 100, 100), 0.001f)
        assertEquals(1f, Palette.saturation(255, 0, 0), 0.001f)
    }

    @Test
    fun similarityDetectsNearbyColours() {
        assertTrue(Palette.isSimilar(0xFF102030.toInt(), 0xFF122132.toInt()))
        assertTrue(!Palette.isSimilar(0xFFFF0000.toInt(), 0xFF00FF00.toInt()))
    }
}
