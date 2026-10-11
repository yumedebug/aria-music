package com.aria.music.core

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatTest {

    @Test
    fun formatsSecondsAndMinutes() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:00", formatDuration(-5))
        assertEquals("0:01", formatDuration(1_000))
        assertEquals("3:07", formatDuration(187_000))
    }

    @Test
    fun formatsHoursForLongTracks() {
        assertEquals("1:00:00", formatDuration(3_600_000))
        assertEquals("1:02:03", formatDuration(3_723_000))
    }

    @Test
    fun roundsDownPartialSeconds() {
        assertEquals("0:59", formatDuration(59_999))
    }

    @Test
    fun formatsTotalListeningTime() {
        assertEquals("0 min", formatTotalDuration(0))
        assertEquals("45 min", formatTotalDuration(45 * 60_000L))
        assertEquals("1 hr 15 min", formatTotalDuration(75 * 60_000L))
    }
}
