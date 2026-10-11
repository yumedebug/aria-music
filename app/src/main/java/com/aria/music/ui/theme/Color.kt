package com.aria.music.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** The app's own brand accent — deliberately not any third party's trademarked red. */
val BrandAccent = Color(0xFFE6244C)
val BrandAccentDeep = Color(0xFFB00E33)

// Neutral surfaces tuned to read like iOS system backgrounds.
val InkBlack = Color(0xFF000000)
val NightSurface = Color(0xFF121214)
val NightElevated = Color(0xFF1C1C1E)
val NightElevatedHigh = Color(0xFF2C2C2E)
val PaperWhite = Color(0xFFFFFFFF)
val PaperSecondary = Color(0xFFF2F2F7)
val PaperElevated = Color(0xFFFFFFFF)
val SlateText = Color(0xFF1C1C1E)
val MutedTextLight = Color(0xFF8E8E93)
val DividerLight = Color(0x1F000000)
val DividerDark = Color(0x33FFFFFF)

val LightColors = lightColorScheme(
    primary = BrandAccent,
    onPrimary = Color.White,
    secondary = SlateText,
    onSecondary = Color.White,
    background = PaperWhite,
    onBackground = SlateText,
    surface = PaperElevated,
    onSurface = SlateText,
    surfaceVariant = PaperSecondary,
    onSurfaceVariant = MutedTextLight,
    outline = DividerLight,
)

val DarkColors = darkColorScheme(
    primary = BrandAccent,
    onPrimary = Color.White,
    secondary = Color.White,
    onSecondary = InkBlack,
    background = NightSurface,
    onBackground = Color.White,
    surface = NightElevated,
    onSurface = Color.White,
    surfaceVariant = NightElevatedHigh,
    onSurfaceVariant = MutedTextLight,
    outline = DividerDark,
)
