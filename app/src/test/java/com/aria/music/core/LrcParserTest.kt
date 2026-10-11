package com.aria.music.core

import com.aria.music.core.lyrics.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun parsesTimedLines() {
        val lyrics = LrcParser.parse(
            """
            [ar:Someone]
            [00:01.00]First line
            [00:03.50]Second line
            [00:10.25]Third line
            """.trimIndent(),
        )
        assertTrue(lyrics.synced)
        assertEquals(3, lyrics.lines.size)
        assertEquals("First line", lyrics.lines[0].text)
        assertEquals(1_000L, lyrics.lines[0].timeMs)
        assertEquals(3_500L, lyrics.lines[1].timeMs)
        assertEquals(10_250L, lyrics.lines[2].timeMs)
    }

    @Test
    fun expandsMultipleTimestampsOnOneLine() {
        val lyrics = LrcParser.parse("[00:01.00][00:05.00]Repeated")
        assertEquals(2, lyrics.lines.size)
        assertEquals("Repeated", lyrics.lines[1].text)
        assertEquals(5_000L, lyrics.lines[1].timeMs)
    }

    @Test
    fun appliesOffsetMetadata() {
        val lyrics = LrcParser.parse(
            """
            [offset:500]
            [00:01.00]Shifted
            """.trimIndent(),
        )
        assertEquals(500L, lyrics.lines.single().timeMs)
    }

    @Test
    fun treatsUntimedTextAsUnsynced() {
        val lyrics = LrcParser.parse("Just some\nplain lyrics")
        assertFalse(lyrics.synced)
        assertEquals(2, lyrics.lines.size)
        assertEquals("Just some", lyrics.lines[0].text)
    }

    @Test
    fun blankInputYieldsEmpty() {
        assertTrue(LrcParser.parse(null).isEmpty)
        assertTrue(LrcParser.parse("   ").isEmpty)
    }

    @Test
    fun findsActiveLineForPosition() {
        val lyrics = LrcParser.parse(
            """
            [00:01.00]A
            [00:03.00]B
            [00:05.00]C
            """.trimIndent(),
        )
        assertEquals(-1, lyrics.activeIndex(500))
        assertEquals(0, lyrics.activeIndex(1_000))
        assertEquals(0, lyrics.activeIndex(2_999))
        assertEquals(1, lyrics.activeIndex(3_000))
        assertEquals(2, lyrics.activeIndex(60_000))
    }

    @Test
    fun unsyncedLyricsHaveNoActiveLine() {
        val lyrics = LrcParser.parse("no timing here")
        assertEquals(-1, lyrics.activeIndex(10_000))
    }

    @Test
    fun supportsHourTimestamps() {
        val lyrics = LrcParser.parse("[01:00:00.00]Long")
        assertEquals(3_600_000L, lyrics.lines.single().timeMs)
    }
}
