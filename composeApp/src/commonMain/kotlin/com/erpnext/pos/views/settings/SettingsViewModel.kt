package com.erpnext.pos.views.settings

import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel : BaseViewModel() {

    private val _uiState: MutableStateFlow<POSSettingState> =
        MutableStateFlow(POSSettingState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            delay(500)
            _uiState.update {
                POSSettingState.Success(
                    POSSettingBO(
                        "Cloting Center",
                        "Princial - NIO/USD",
                        "Almacen Principal",
                        "Standar Price List",
                        taxesIncluded = false,
                        offlineMode = true,
                        printerEnabled = true,
                        cashDrawerEnabled = true
                    )
                )
            }
        }
    }
}