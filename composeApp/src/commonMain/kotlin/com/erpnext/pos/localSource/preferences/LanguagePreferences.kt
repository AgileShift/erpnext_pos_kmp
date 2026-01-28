package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import com.erpnext.pos.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LanguagePreferences(
    private val store: ConfigurationStore
) {
    companion object {
        private const val languageKey = "app_language"
    }

    val language: Flow<AppLanguage> = store.observeRaw(languageKey).map { AppLanguage.fromCode(it) }

    suspend fun setLanguage(language: AppLanguage) {
        store.saveRaw(languageKey, language.code)
    }
}
