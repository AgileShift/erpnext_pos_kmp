package com.erpnext.pos.views.settings

data class POSSettingBO(
    val company: String,
    val posProfile: String,
    val warehouse: String,
    val priceList: String,
    val taxesIncluded: Boolean,
    val offlineMode: Boolean,
    val printerEnabled: Boolean,
    val cashDrawerEnabled: Boolean
)

sealed class POSSettingState {
    object Loading : POSSettingState()
    data class Success(val settings: POSSettingBO) : POSSettingState()
    data class Error(val message: String) : POSSettingState()
}

data class POSSettingAction(
    val loadSettings: () -> Unit = {},
    val onToggle: (String, Boolean) -> Unit = { _, _ -> },
    val onSelect: (String) -> Unit = {}
)