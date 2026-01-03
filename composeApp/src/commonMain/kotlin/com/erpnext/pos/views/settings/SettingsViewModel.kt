package com.erpnext.pos.views.settings

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
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
    private val syncManager: SyncManager
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<POSSettingState> =
        MutableStateFlow(POSSettingState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            syncPreferences.settings.collect { settings ->
                publishState(settings, syncManager.state.value)
            }
        }
        viewModelScope.launch {
            syncManager.state.collect { state ->
                val current = (uiState.value as? POSSettingState.Success)?.syncSettings
                    ?: SyncSettings(autoSync = true, syncOnStartup = true, wifiOnly = false, lastSyncAt = null)
                publishState(current, state)
            }
        }
    }

    //TODO: Esto debe de venir de API / POSContext no hardcoded
    private fun publishState(settings: SyncSettings, syncState: SyncState) {
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
                syncSettings = settings,
                syncState = syncState
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
}
