package com.aria.music.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Apple Music を踏襲したブランド配色。アクセントは Apple Music のピンク寄りの赤
 * (#FA2D48)。ニュートラル面は iOS のシステム背景（純黒 / #1C1C1E / #2C2C2E と
 * ライトの #FFFFFF / #F2F2F7）に寄せている。
 */
val BrandAccent = Color(0xFFFA2D48)
val BrandAccentDeep = Color(0xFFC4102B)

// Apple Music のコントロールで使う補助色。
val AppleMusicRed = Color(0xFFFA2D48)
val AppleMusicPink = Color(0xFFFB5C74)

// Neutral surfaces tuned to read like iOS system backgrounds.
val InkBlack = Color(0xFF000000)
val NightSurface = Color(0xFF000000)
val NightElevated = Color(0xFF1C1C1E)
val NightElevatedHigh = Color(0xFF2C2C2E)
val PaperWhite = Color(0xFFFFFFFF)
val PaperSecondary = Color(0xFFF2F2F7)
val PaperElevated = Color(0xFFFFFFFF)
val SlateText = Color(0xFF1C1C1E)
val MutedTextLight = Color(0xFF8E8E93)
val MutedTextDark = Color(0xFF98989F)
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
    onSurfaceVariant = MutedTextDark,
    outline = DividerDark,
)
