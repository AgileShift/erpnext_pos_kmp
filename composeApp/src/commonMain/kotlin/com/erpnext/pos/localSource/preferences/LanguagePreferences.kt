package com.erpnext.pos.localSource.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.erpnext.pos.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LanguagePreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val languageKey = stringPreferencesKey("app_language")
    }

    val language: Flow<AppLanguage> = dataStore.data.map { prefs ->
        AppLanguage.fromCode(prefs[languageKey])
    }

    suspend fun setLanguage(language: AppLanguage) {
        dataStore.edit { prefs ->
            prefs[languageKey] = language.code
        }
    }
}
