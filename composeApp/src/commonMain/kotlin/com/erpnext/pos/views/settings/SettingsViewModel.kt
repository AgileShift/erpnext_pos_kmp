package com.erpnext.pos.views.settings

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.localSource.preferences.LanguagePreferences
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localSource.preferences.ThemePreferences
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import AppColorTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val syncPreferences: SyncPreferences,
    private val syncManager: SyncManager,
    private val languagePreferences: LanguagePreferences,
    private val themePreferences: ThemePreferences
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<POSSettingState> =
        MutableStateFlow(POSSettingState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentSyncSettings =
        SyncSettings(autoSync = true, syncOnStartup = true, wifiOnly = true, lastSyncAt = null)
    private var currentSyncState: SyncState = SyncState.IDLE
    private var currentLanguage: AppLanguage = AppLanguage.Spanish
    private var currentTheme: AppColorTheme = AppColorTheme.Noir

    init {
        viewModelScope.launch {
            syncPreferences.settings.collect { settings ->
                currentSyncSettings = settings
                publishState()
            }
        }
        viewModelScope.launch {
            syncManager.state.collect { state ->
                currentSyncState = state
                publishState()
            }
        }
        viewModelScope.launch {
            languagePreferences.language.collect { language ->
                currentLanguage = language
                publishState()
            }
        }
        viewModelScope.launch {
            themePreferences.theme.collect { theme ->
                currentTheme = theme
                publishState()
            }
        }
    }

    //TODO: Esto debe de venir de API / POSContext no hardcoded
    private fun publishState() {
        _uiState.update {
            POSSettingState.Success(
                settings = POSSettingBO(
                    "Cloting Center",
                    "Princial - NIO/USD",
                    "Almacen Principal",
                    "Standar Price List",
                    taxesIncluded = false,
                    offlineMode = true,
                    printerEnabled = true,
                    cashDrawerEnabled = true
                ),
                syncSettings = currentSyncSettings,
                syncState = currentSyncState,
                language = currentLanguage,
                theme = currentTheme
            )
        }
    }

    fun onSyncNow() {
        syncManager.fullSync()
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

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { languagePreferences.setLanguage(language) }
    }

    fun setTheme(theme: AppColorTheme) {
        viewModelScope.launch { themePreferences.setTheme(theme) }
    }
}
