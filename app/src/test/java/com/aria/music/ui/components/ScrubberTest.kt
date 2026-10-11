package com.aria.music.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class ScrubberTest {

    @Test
    fun clampsPointerToTrackBounds() {
        assertEquals(0f, scrubFraction(-40f, 200), 0.0001f)
        assertEquals(0.5f, scrubFraction(100f, 200), 0.0001f)
        assertEquals(1f, scrubFraction(400f, 200), 0.0001f)
    }

    @Test
    fun handlesZeroWidth() {
        assertEquals(0f, scrubFraction(50f, 0), 0.0001f)
    }
}
