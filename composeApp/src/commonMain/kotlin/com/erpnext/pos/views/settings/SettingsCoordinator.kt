package com.erpnext.pos.views.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.erpnext.pos.localization.AppLanguage
import AppColorTheme
import AppThemeMode
import org.koin.compose.viewmodel.koinViewModel

class SettingsCoordinator(
    val viewModel: SettingsViewModel
) {
    val screenStateFlow = viewModel.uiState

    fun loadSettings() {}

    fun onTaxesIncludedChanged(enabled: Boolean) = viewModel.setTaxesIncluded(enabled)
    fun onOfflineModeChanged(enabled: Boolean) = viewModel.setOfflineMode(enabled)
    fun onPrinterEnabledChanged(enabled: Boolean) = viewModel.setPrinterEnabled(enabled)
    fun onCashDrawerEnabledChanged(enabled: Boolean) = viewModel.setCashDrawerEnabled(enabled)
    fun onInventoryAlertsEnabledChanged(enabled: Boolean) = viewModel.setInventoryAlertsEnabled(enabled)
    fun onInventoryAlertTimeChanged(hour: Int, minute: Int) =
        viewModel.setInventoryAlertTime(hour, minute)
    fun onSalesTargetChanged(value: Double) = viewModel.setSalesTargetMonthly(value)
    fun onSyncSalesTarget() = viewModel.syncSalesTargetFromERPNext()

    fun onSelect(s: String) {}

    fun onSyncNow() = viewModel.onSyncNow()
    fun onCancelSync() = viewModel.onCancelSync()
    fun onAutoSyncChanged(enabled: Boolean) = viewModel.setAutoSync(enabled)
    fun onSyncOnStartupChanged(enabled: Boolean) = viewModel.setSyncOnStartup(enabled)
    fun onWifiOnlyChanged(enabled: Boolean) = viewModel.setWifiOnly(enabled)
    fun onUseTtlChanged(enabled: Boolean) = viewModel.setUseTtl(enabled)
    fun onLanguageSelected(language: AppLanguage) =
        viewModel.setLanguage(language)
    fun onThemeSelected(theme: AppColorTheme) = viewModel.setTheme(theme)
    fun onThemeModeSelected(mode: AppThemeMode) = viewModel.setThemeMode(mode)
}

@Composable
fun rememberSettingsCoordinator(): SettingsCoordinator {
    val viewModel: SettingsViewModel = koinViewModel()
    return remember(viewModel) {
        SettingsCoordinator(viewModel)
    }
}
