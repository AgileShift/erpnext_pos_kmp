package com.erpnext.pos.views.settings

import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.sync.SyncState
import AppColorTheme
import AppThemeMode

data class POSSettingBO(
    val company: String,
    val posProfile: String,
    val warehouse: String,
    val priceList: String,
    val taxesIncluded: Boolean,
    val offlineMode: Boolean,
    val printerEnabled: Boolean,
    val cashDrawerEnabled: Boolean,
    val allowNegativeStock: Boolean
)

sealed class POSSettingState {
    object Loading : POSSettingState()
    data class Success(
        val settings: POSSettingBO,
        val syncSettings: SyncSettings,
        val syncState: SyncState,
        val language: AppLanguage,
        val theme: AppColorTheme,
        val themeMode: AppThemeMode
    ) : POSSettingState()
    data class Error(val message: String) : POSSettingState()
}

data class POSSettingAction(
    val loadSettings: () -> Unit = {},
    val onTaxesIncludedChanged: (Boolean) -> Unit = {},
    val onOfflineModeChanged: (Boolean) -> Unit = {},
    val onPrinterEnabledChanged: (Boolean) -> Unit = {},
    val onCashDrawerEnabledChanged: (Boolean) -> Unit = {},
    val onSelect: (String) -> Unit = {},
    val onSyncNow: () -> Unit = {},
    val onAutoSyncChanged: (Boolean) -> Unit = {},
    val onSyncOnStartupChanged: (Boolean) -> Unit = {},
    val onWifiOnlyChanged: (Boolean) -> Unit = {},
    val onLanguageSelected: (AppLanguage) -> Unit = {},
    val onThemeSelected: (AppColorTheme) -> Unit = {},
    val onThemeModeSelected: (AppThemeMode) -> Unit = {}
)
