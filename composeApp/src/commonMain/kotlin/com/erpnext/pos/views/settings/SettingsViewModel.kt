package com.erpnext.pos.views.settings

import AppColorTheme
import AppThemeMode
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.localSource.preferences.LanguagePreferences
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localSource.preferences.ThemePreferences
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.POSContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val cashBoxManager: CashBoxManager,
    private val syncPreferences: SyncPreferences,
    private val syncManager: SyncManager,
    private val generalPreferences: GeneralPreferences,
    private val languagePreferences: LanguagePreferences,
    private val themePreferences: ThemePreferences
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<POSSettingState> =
        MutableStateFlow(POSSettingState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                cashBoxManager.contextFlow,
                syncPreferences.settings,
                syncManager.state,
                languagePreferences.language,
                themePreferences.theme,
                themePreferences.themeMode,
                generalPreferences.taxesIncluded,
                generalPreferences.offlineMode,
                generalPreferences.printerEnabled,
                generalPreferences.cashDrawerEnabled,
                generalPreferences.allowNegativeStock
            ) { args: Array<Any?> ->
                val ctx = args[0] as POSContext
                val syncSettings = args[1] as SyncSettings
                val syncState = args[2] as SyncState
                val language = args[3] as AppLanguage
                val theme = args[4] as AppColorTheme
                val themeMode = args[5] as AppThemeMode
                val taxes = args[6] as Boolean
                val offline = args[7] as Boolean
                val printer = args[8] as Boolean
                val drawer = args[9] as Boolean
                val allowNegativeStock = args[10] as Boolean
                POSSettingState.Success(
                    settings = POSSettingBO(
                        company = ctx.company,
                        posProfile = ctx.profileName,
                        warehouse = ctx.warehouse ?: "-",
                        priceList = ctx.priceList ?: ctx.currency,
                        taxesIncluded = taxes,
                        offlineMode = offline,
                        printerEnabled = printer,
                        cashDrawerEnabled = drawer,
                        allowNegativeStock = allowNegativeStock
                    ),
                    syncSettings = syncSettings,
                    syncState = syncState,
                    language = language,
                    theme = theme,
                    themeMode = themeMode
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onSyncNow() {
        viewModelScope.launch { syncManager.fullSync(force = true) }
    }

    fun setAutoSync(enabled: Boolean) {
        viewModelScope.launch { syncPreferences.setAutoSync(enabled) }
    }

    fun setSyncOnStartup(enabled: Boolean) {
        viewModelScope.launch { syncPreferences.setSyncOnStartup(enabled) }
    }

    fun setWifiOnly(enabled: Boolean) {
        viewModelScope.launch { syncPreferences.setWifiOnly(enabled) }
    }

    fun setUseTtl(enabled: Boolean) {
        viewModelScope.launch { syncPreferences.setUseTtl(enabled) }
    }

    fun setTaxesIncluded(enabled: Boolean) {
        viewModelScope.launch { generalPreferences.setTaxesIncluded(enabled) }
    }

    fun setOfflineMode(enabled: Boolean) {
        viewModelScope.launch { generalPreferences.setOfflineMode(enabled) }
    }

    fun setPrinterEnabled(enabled: Boolean) {
        viewModelScope.launch { generalPreferences.setPrinterEnabled(enabled) }
    }

    fun setCashDrawerEnabled(enabled: Boolean) {
        viewModelScope.launch { generalPreferences.setCashDrawerEnabled(enabled) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { languagePreferences.setLanguage(language) }
    }

    fun setTheme(theme: AppColorTheme) {
        viewModelScope.launch { themePreferences.setTheme(theme) }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch { themePreferences.setThemeMode(mode) }
    }
}
