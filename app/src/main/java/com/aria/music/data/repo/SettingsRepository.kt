package com.aria.music.data.repo

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** How the app decides between light and dark. */
enum class ThemeMode { SYSTEM, LIGHT, DARK;

    companion object {
        fun from(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** Follow Android 12+ Material You wallpaper colours. Off by default: Apple Music look. */
    val dynamicColor: Boolean = false,
    /** Pure-black surfaces for AMOLED panels. */
    val amoled: Boolean = false,
    /** Tint the player chrome with colours pulled from the album artwork. */
    val artworkColors: Boolean = false,
)

private val Context.settingsDataStore by preferencesDataStore(name = "aria_settings")

/** Persists small, user-visible preferences. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val themeMode = stringPreferencesKey("theme_mode")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val amoled = booleanPreferencesKey("amoled")
        val artworkColors = booleanPreferencesKey("artwork_colors")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.from(prefs[Keys.themeMode]),
            dynamicColor = prefs[Keys.dynamicColor] ?: false,
            amoled = prefs[Keys.amoled] ?: false,
            artworkColors = prefs[Keys.artworkColors] ?: false,
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.themeMode] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.dynamicColor] = enabled }
    }

    suspend fun setAmoled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.amoled] = enabled }
    }

    suspend fun setArtworkColors(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.artworkColors] = enabled }
    }
}
