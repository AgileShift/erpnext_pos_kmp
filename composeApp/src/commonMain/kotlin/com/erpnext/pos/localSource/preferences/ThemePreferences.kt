package com.erpnext.pos.localSource.preferences

import AppColorTheme
import AppThemeMode
import com.erpnext.pos.localSource.configuration.ConfigurationStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemePreferences(
    private val store: ConfigurationStore
) {
    companion object {
        private const val themeKey = "app_theme"
        private const val themeModeKey = "app_theme_mode"
        private const val defaultTheme = "Noir"
        private const val defaultThemeMode = "System"
    }

    val theme: Flow<AppColorTheme> = store.observeRaw(themeKey).map { value ->
        val key = value ?: defaultTheme
        AppColorTheme.entries.firstOrNull { it.name == key } ?: AppColorTheme.Noir
    }

    val themeMode: Flow<AppThemeMode> = store.observeRaw(themeModeKey).map { value ->
        val key = value ?: defaultThemeMode
        AppThemeMode.entries.firstOrNull { it.name == key } ?: AppThemeMode.System
    }

    suspend fun setTheme(theme: AppColorTheme) {
        store.saveRaw(themeKey, theme.name)
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        store.saveRaw(themeModeKey, mode.name)
    }
}
