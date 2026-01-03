package com.erpnext.pos.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.erpnext.pos.localSource.preferences.LanguagePreferences
import org.koin.compose.koinInject

@Composable
fun ProvideAppStrings(
    content: @Composable () -> Unit
) {
    val languagePreferences = koinInject<LanguagePreferences>()
    val language by languagePreferences.language.collectAsState(AppLanguage.Spanish)
    val strings = remember(language) { AppStringsFactory.forLanguage(language) }

    CompositionLocalProvider(LocalAppStrings provides strings) {
        content()
    }
}
