package com.erpnext.pos.views.settings

import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.domain.models.SyncLogEntry
import com.erpnext.pos.domain.models.ReturnPolicySettings
import AppColorTheme
import AppThemeMode

data class POSSettingBO(
    val company: String,
    val posProfile: String,
    val openingEntryId: String,
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
        val hasContext: Boolean,
        val syncSettings: SyncSettings,
        val syncState: SyncState,
        val language: AppLanguage,
        val theme: AppColorTheme,
        val themeMode: AppThemeMode,
        val returnPolicy: ReturnPolicySettings,
        val inventoryAlertsEnabled: Boolean,
        val inventoryAlertHour: Int,
        val inventoryAlertMinute: Int,
        val salesTargetMonthly: Double,
        val salesTargetWeekly: Double,
        val salesTargetDaily: Double,
        val salesTargetBaseCurrency: String,
        val salesTargetSecondaryCurrency: String?,
        val salesTargetConvertedMonthly: Double?,
        val salesTargetConvertedWeekly: Double?,
        val salesTargetConvertedDaily: Double?,
        val salesTargetConversionStale: Boolean,
        val salesTargetFromContext: Boolean,
        val syncLog: List<SyncLogEntry>
    ) : POSSettingState()
    data class Error(val message: String) : POSSettingState()
}

data class POSSettingAction(
    val loadSettings: () -> Unit = {},
    val onTaxesIncludedChanged: (Boolean) -> Unit = {},
    val onOfflineModeChanged: (Boolean) -> Unit = {},
    val onPrinterEnabledChanged: (Boolean) -> Unit = {},
    val onCashDrawerEnabledChanged: (Boolean) -> Unit = {},
    val onInventoryAlertsEnabledChanged: (Boolean) -> Unit = {},
    val onInventoryAlertTimeChanged: (Int, Int) -> Unit = { _, _ -> },
    val onSalesTargetChanged: (Double) -> Unit = {},
    val onReturnPolicyChanged: (ReturnPolicySettings) -> Unit = {},
    val onSyncSalesTarget: () -> Unit = {},
    val onSelect: (String) -> Unit = {},
    val onSyncNow: () -> Unit = {},
    val onCancelSync: () -> Unit = {},
    val onAutoSyncChanged: (Boolean) -> Unit = {},
    val onSyncOnStartupChanged: (Boolean) -> Unit = {},
    val onWifiOnlyChanged: (Boolean) -> Unit = {},
    val onUseTtlChanged: (Boolean) -> Unit = {},
    val onTtlHoursChanged: (Int) -> Unit = {},
    val onLanguageSelected: (AppLanguage) -> Unit = {},
    val onThemeSelected: (AppColorTheme) -> Unit = {},
    val onThemeModeSelected: (AppThemeMode) -> Unit = {}
)
