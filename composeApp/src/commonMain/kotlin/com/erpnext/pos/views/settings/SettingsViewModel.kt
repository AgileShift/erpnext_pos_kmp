package com.erpnext.pos.views.settings

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.localSource.preferences.LanguagePreferences
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val syncPreferences: SyncPreferences,
    private val languagePreferences: LanguagePreferences,
    private val syncManager: SyncManager
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<POSSettingState> =
        MutableStateFlow(POSSettingState.Loading)
    val uiState = _uiState.asStateFlow()

    private var currentSyncSettings =
        SyncSettings(autoSync = true, syncOnStartup = true, wifiOnly = false, lastSyncAt = null)
    private var currentSyncState: SyncState = SyncState.IDLE
    private var currentLanguage: AppLanguage = AppLanguage.Spanish

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
    }

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
                language = currentLanguage
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
}
