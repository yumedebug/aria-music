package com.aria.music.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aria.music.data.repo.AppSettings
import com.aria.music.data.repo.ThemeMode

/** Rounded corners reminiscent of iOS cards and buttons. */
val AriaShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** True when the active theme is dark; used by glassy overlays. */
val LocalIsDarkTheme = staticCompositionLocalOf { true }

/** The accent pulled from the current artwork (falls back to the brand accent). */
val LocalArtworkAccent = staticCompositionLocalOf { BrandAccent }

@Composable
fun AriaTheme(
    settings: AppSettings,
    artworkAccent: Color? = null,
    content: @Composable () -> Unit,
) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current

    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val base = when {
        settings.dynamicColor && dynamicSupported ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }

    val accent = artworkAccent ?: base.primary
    var scheme = base.copy(primary = accent)
    if (settings.amoled && dark) {
        scheme = scheme.copy(
            background = InkBlack,
            surface = InkBlack,
            surfaceVariant = NightElevated,
        )
    }

    CompositionLocalProvider(
        LocalIsDarkTheme provides dark,
        LocalArtworkAccent provides accent,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = AriaTypography,
            shapes = AriaShapes,
            content = content,
        )
    }
}
