package com.erpnext.pos.localSource.preferences

import AppColorTheme
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
        private const val defaultTheme = "Noir"
    }

    val theme: Flow<AppColorTheme> = dataStore.data.map { prefs ->
        val value = prefs[themeKey] ?: defaultTheme
        AppColorTheme.entries.firstOrNull { it.name == value } ?: AppColorTheme.Noir
    }

    suspend fun setTheme(theme: AppColorTheme) {
        dataStore.edit { prefs ->
            prefs[themeKey] = theme.name
        }
    }
}
