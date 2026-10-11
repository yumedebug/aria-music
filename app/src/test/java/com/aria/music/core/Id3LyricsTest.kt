package com.aria.music.core

import com.aria.music.core.lyrics.Id3Lyrics
import com.aria.music.core.lyrics.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Id3LyricsTest {

    @Test
    fun extractsUsltFromId3v23() {
        val bytes = buildTag(version = 3, lyrics = "[00:01.00]Hello\n[00:02.00]World")
        val text = Id3Lyrics.extract(bytes)
        assertEquals("[00:01.00]Hello\n[00:02.00]World", text)

        val parsed = LrcParser.parse(text)
        assertTrue(parsed.synced)
        assertEquals(2, parsed.lines.size)
    }

    @Test
    fun extractsUsltFromId3v24() {
        val bytes = buildTag(version = 4, lyrics = "plain lyrics")
        assertEquals("plain lyrics", Id3Lyrics.extract(bytes))
    }

    @Test
    fun returnsNullWithoutId3Header() {
        assertNull(Id3Lyrics.extract(byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)))
        assertNull(Id3Lyrics.extract(byteArrayOf()))
    }

    @Test
    fun decodesSyncSafeIntegers() {
        // 0x00000080 in sync-safe form is 00 00 01 00.
        assertEquals(128, Id3Lyrics.syncSafeToInt(byteArrayOf(0, 0, 1, 0), 0))
        assertNull(Id3Lyrics.syncSafeToInt(byteArrayOf(0x80.toByte(), 0, 0, 0), 0))
    }

    @Test
    fun reversesUnsynchronisation() {
        val input = byteArrayOf(0xFF.toByte(), 0x00, 0x10, 0xFF.toByte(), 0x00, 0x20)
        val output = Id3Lyrics.deunsynchronise(input)
        assertEquals(4, output.size)
        assertEquals(0xFF.toByte(), output[0])
        assertEquals(0x10.toByte(), output[1])
    }

    /** Builds a minimal ID3v2 tag containing a single USLT frame. */
    private fun buildTag(version: Int, lyrics: String): ByteArray {
        val text = lyrics.toByteArray(Charsets.ISO_8859_1)
        val uslt = ByteArray(1 + 3 + 1 + text.size)
        uslt[0] = 0 // ISO-8859-1
        uslt[1] = 'e'.code.toByte()
        uslt[2] = 'n'.code.toByte()
        uslt[3] = 'g'.code.toByte()
        uslt[4] = 0 // content descriptor terminator
        text.copyInto(uslt, 5)

        val frame = ByteArray(10 + uslt.size)
        "USLT".toByteArray(Charsets.ISO_8859_1).copyInto(frame, 0)
        val size = uslt.size
        if (version >= 4) {
            syncSafe(size).copyInto(frame, 4)
        } else {
            frame[4] = ((size ushr 24) and 0xFF).toByte()
            frame[5] = ((size ushr 16) and 0xFF).toByte()
            frame[6] = ((size ushr 8) and 0xFF).toByte()
            frame[7] = (size and 0xFF).toByte()
        }
        uslt.copyInto(frame, 10)

        val out = ByteArray(10 + frame.size)
        out[0] = 'I'.code.toByte()
        out[1] = 'D'.code.toByte()
        out[2] = '3'.code.toByte()
        out[3] = version.toByte()
        out[4] = 0
        syncSafe(frame.size).copyInto(out, 6)
        frame.copyInto(out, 10)
        return out
    }

    private fun syncSafe(value: Int): ByteArray = byteArrayOf(
        ((value ushr 21) and 0x7F).toByte(),
        ((value ushr 14) and 0x7F).toByte(),
        ((value ushr 7) and 0x7F).toByte(),
        (value and 0x7F).toByte(),
    )
}
