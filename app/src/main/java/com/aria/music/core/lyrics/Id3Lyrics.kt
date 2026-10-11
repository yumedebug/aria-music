package com.aria.music.core.lyrics

/**
 * Extracts unsynchronised lyrics (`USLT`) from an ID3v2 tag.
 *
 * Many audio files carry their lyrics inside the ID3 tag, which is the only lyrics source
 * available offline without shipping a proprietary lyrics service. This parser covers
 * ID3v2.3 and v2.4 (the versions that appear in practice) and both the extended-header and
 * unsynchronisation cases.
 *
 * Pure over a byte array so it can be unit tested without touching the file system.
 */
object Id3Lyrics {

    fun extract(bytes: ByteArray): String? {
        if (bytes.size < 10) return null
        if (bytes[0] != 'I'.code.toByte() ||
            bytes[1] != 'D'.code.toByte() ||
            bytes[2] != '3'.code.toByte()
        ) {
            return null
        }
        val majorVersion = bytes[3].toInt() and 0xFF
        val flags = bytes[4].toInt() and 0xFF
        val tagSize = syncSafeToInt(bytes, 6) ?: return null

        var offset = 10
        if (flags and 0x40 != 0) {
            // Extended header: skip it (size field width differs between v2.3 and v2.4).
            val extSize = (if (majorVersion >= 4) {
                syncSafeToInt(bytes, offset)
            } else {
                intFromBytes(bytes, offset, 4)
            }) ?: return null
            offset += if (majorVersion >= 4) extSize else extSize + 4
        }

        val end = minOf(bytes.size, 10 + tagSize)
        val frameHeaderSize = if (majorVersion == 2) 6 else 10
        val idSize = if (majorVersion == 2) 3 else 4
        val lyricsFrameId = if (majorVersion == 2) "ULT" else "USLT"

        while (offset + frameHeaderSize <= end) {
            val frameId = String(bytes, offset, idSize, Charsets.ISO_8859_1)
            if (frameId.all { it == '\u0000' }) break

            val size = (if (majorVersion >= 4) {
                syncSafeToInt(bytes, offset + idSize)
            } else if (majorVersion == 2) {
                intFromBytes(bytes, offset + 3, 3)
            } else {
                intFromBytes(bytes, offset + idSize, 4)
            }) ?: return null
            if (size <= 0) break

            val frameFlags = if (majorVersion == 2) 0 else bytes[offset + idSize + 4].toInt() and 0xFF
            val dataStart = offset + frameHeaderSize
            val dataEnd = minOf(bytes.size, dataStart + size)
            if (dataStart > end) break

            if (frameId == lyricsFrameId) {
                var payload = bytes.copyOfRange(dataStart, dataEnd)
                if (majorVersion >= 4 && frameFlags and 0x02 != 0) {
                    payload = deunsynchronise(payload)
                }
                return decodeUslt(payload)
            }
            offset = dataStart + size
        }

        // Nothing found in the tag; the caller may still fall back to a sidecar file.
        return null
    }

    private fun decodeUslt(payload: ByteArray): String? {
        if (payload.size < 4) return null
        val encoding = payload[0].toInt() and 0xFF
        // Skip language (3 bytes) then the null-terminated content descriptor.
        var index = 4
        index = skipTerminatedString(payload, index, encoding)
        if (index >= payload.size) return null
        val text = decodeText(payload, index, payload.size, encoding)
        return text.trim('\u0000').trim().takeIf { it.isNotEmpty() }
    }

    private fun skipTerminatedString(bytes: ByteArray, start: Int, encoding: Int): Int {
        var i = start
        if (encoding == 1 || encoding == 2) {
            while (i + 1 < bytes.size) {
                if (bytes[i] == 0.toByte() && bytes[i + 1] == 0.toByte()) return i + 2
                i += 2
            }
            return bytes.size
        }
        while (i < bytes.size) {
            if (bytes[i] == 0.toByte()) return i + 1
            i++
        }
        return bytes.size
    }

    fun decodeText(bytes: ByteArray, start: Int, end: Int, encoding: Int): String {
        if (start >= end) return ""
        val slice = bytes.copyOfRange(start, end)
        return runCatching {
            when (encoding) {
                0 -> String(slice, Charsets.ISO_8859_1)
                1 -> decodeUtf16(slice, bigEndianDefault = false)
                2 -> decodeUtf16(slice, bigEndianDefault = true)
                3 -> String(slice, Charsets.UTF_8)
                else -> String(slice, Charsets.ISO_8859_1)
            }
        }.getOrDefault("")
    }

    private fun decodeUtf16(bytes: ByteArray, bigEndianDefault: Boolean): String {
        if (bytes.size >= 2) {
            val b0 = bytes[0].toInt() and 0xFF
            val b1 = bytes[1].toInt() and 0xFF
            if (b0 == 0xFF && b1 == 0xFE) return String(bytes, 2, bytes.size - 2, Charsets.UTF_16LE)
            if (b0 == 0xFE && b1 == 0xFF) return String(bytes, 2, bytes.size - 2, Charsets.UTF_16BE)
        }
        return String(
            bytes,
            if (bigEndianDefault) Charsets.UTF_16BE else Charsets.UTF_16LE,
        )
    }

    /** ID3v2 sizes are 7-bit "sync-safe" integers. */
    fun syncSafeToInt(bytes: ByteArray, start: Int): Int? {
        if (start + 4 > bytes.size) return null
        var value = 0
        for (i in 0 until 4) {
            val b = bytes[start + i].toInt() and 0xFF
            if (b and 0x80 != 0) return null
            value = (value shl 7) or b
        }
        return value
    }

    private fun intFromBytes(bytes: ByteArray, start: Int, length: Int): Int? {
        if (start + length > bytes.size) return null
        var value = 0
        for (i in 0 until length) {
            value = (value shl 8) or (bytes[start + i].toInt() and 0xFF)
        }
        return value
    }

    /** Reverses ID3 unsynchronisation: `FF 00` becomes `FF`. */
    fun deunsynchronise(bytes: ByteArray): ByteArray {
        val out = ByteArray(bytes.size)
        var written = 0
        var i = 0
        while (i < bytes.size) {
            out[written++] = bytes[i]
            if (bytes[i] == 0xFF.toByte() && i + 1 < bytes.size && bytes[i + 1] == 0x00.toByte()) {
                i += 2
            } else {
                i++
            }
        }
        return out.copyOf(written)
    }
}
