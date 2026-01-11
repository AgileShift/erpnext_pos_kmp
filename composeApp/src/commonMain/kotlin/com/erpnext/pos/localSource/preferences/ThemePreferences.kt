package com.erpnext.pos.localSource.preferences

import AppColorTheme
import AppThemeMode
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ThemePreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val themeKey = stringPreferencesKey("app_theme")
        private val themeModeKey = stringPreferencesKey("app_theme_mode")
        private const val defaultTheme = "Noir"
        private const val defaultThemeMode = "System"
    }

    val theme: Flow<AppColorTheme> = dataStore.data.map { prefs ->
        val value = prefs[themeKey] ?: defaultTheme
        AppColorTheme.entries.firstOrNull { it.name == value } ?: AppColorTheme.Noir
    }

    val themeMode: Flow<AppThemeMode> = dataStore.data.map { prefs ->
        val value = prefs[themeModeKey] ?: defaultThemeMode
        AppThemeMode.entries.firstOrNull { it.name == value } ?: AppThemeMode.System
    }

    suspend fun setTheme(theme: AppColorTheme) {
        dataStore.edit { prefs ->
            prefs[themeKey] = theme.name
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }
}
