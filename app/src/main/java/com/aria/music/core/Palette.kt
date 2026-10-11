package com.aria.music.core

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Utility for turning a band of decoded pixels into one representative accent colour.
 *
 * Kept as pure functions over ARGB ints so the behaviour is unit-testable without an
 * Android graphics context. A saturation-weighted average is used so near-black and
 * near-white background pixels do not wash the accent out to grey.
 */
object Palette {

    fun accentFromPixels(pixels: IntArray): Int {
        if (pixels.isEmpty()) return DEFAULT_ACCENT
        var rSum = 0L
        var gSum = 0L
        var bSum = 0L
        var weightSum = 0L
        for (color in pixels) {
            val alpha = (color ushr 24) and 0xFF
            if (alpha < 128) continue
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            val saturation = saturation(r, g, b)
            val brightness = (r + g + b) / 3
            // Reject extremes and weight by how colourful the pixel is.
            if (brightness < 24 || brightness > 236 || saturation < 0.15f) continue
            val weight = (saturation * 1000f).toLong().coerceAtLeast(1L)
            rSum += r * weight
            gSum += g * weight
            bSum += b * weight
            weightSum += weight
        }
        if (weightSum == 0L) return DEFAULT_ACCENT
        val r = (rSum / weightSum).toInt().coerceIn(0, 255)
        val g = (gSum / weightSum).toInt().coerceIn(0, 255)
        val b = (bSum / weightSum).toInt().coerceIn(0, 255)
        return normalize(argb(255, r, g, b))
    }

    /** Raises saturation/lightness of a colour so white text stays legible on top of it. */
    fun normalize(color: Int, minSaturation: Float = 0.45f, targetLuma: Float = 0.42f): Int {
        var hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        hsv[1] = max(hsv[1], minSaturation)
        hsv[2] = max(hsv[2], 0.35f)
        hsv[2] = min(hsv[2], 0.72f)
        // Nudge overall luminance toward the target without flattening hue.
        val luma = 0.299f * hsv[2] + 0.587f * hsv[2] + 0.114f * hsv[2]
        if (luma > 0f) hsv[2] = (hsv[2] * (targetLuma / luma)).coerceIn(0.28f, 0.8f)
        return android.graphics.Color.HSVToColor(hsv)
    }

    fun saturation(r: Int, g: Int, b: Int): Float {
        val maxC = max(r, max(g, b)).toFloat()
        val minC = min(r, min(g, b)).toFloat()
        if (maxC == 0f) return 0f
        return (maxC - minC) / maxC
    }

    fun argb(a: Int, r: Int, g: Int, b: Int): Int =
        (a shl 24) or (r shl 16) or (g shl 8) or b

    /** True when two colours are close enough to be considered "the same" accent. */
    fun isSimilar(a: Int, b: Int, tolerance: Int = 24): Boolean =
        abs(((a shr 16) and 0xFF) - ((b shr 16) and 0xFF)) <= tolerance &&
            abs(((a shr 8) and 0xFF) - ((b shr 8) and 0xFF)) <= tolerance &&
            abs((a and 0xFF) - (b and 0xFF)) <= tolerance

    const val DEFAULT_ACCENT: Int = 0xFFE6244C.toInt()
}
