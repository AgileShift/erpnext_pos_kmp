@file:OptIn(ExperimentalTime::class)

package com.erpnext.pos.views.settings

import AppColorTheme
import AppThemeMode
import androidx.lifecycle.viewModelScope
import com.erpnext.pos.base.BaseViewModel
import com.erpnext.pos.localSource.datasources.ExchangeRateLocalSource
import com.erpnext.pos.localSource.preferences.BootstrapContextPreferences
import com.erpnext.pos.localSource.preferences.GeneralPreferences
import com.erpnext.pos.localSource.preferences.LanguagePreferences
import com.erpnext.pos.localSource.preferences.ReturnPolicyPreferences
import com.erpnext.pos.localSource.preferences.SyncLogPreferences
import com.erpnext.pos.localSource.preferences.SyncPreferences
import com.erpnext.pos.localSource.preferences.SyncSettings
import com.erpnext.pos.localSource.preferences.ThemePreferences
import com.erpnext.pos.localization.AppLanguage
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.utils.normalizeCurrency
import com.erpnext.pos.utils.notifications.configureInventoryAlertWorker
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.POSContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SettingsViewModel(
    private val cashBoxManager: CashBoxManager,
    private val syncPreferences: SyncPreferences,
    private val syncLogPreferences: SyncLogPreferences,
    private val syncManager: SyncManager,
    private val generalPreferences: GeneralPreferences,
    private val languagePreferences: LanguagePreferences,
    private val themePreferences: ThemePreferences,
    private val returnPolicyPreferences: ReturnPolicyPreferences,
    private val exchangeRateLocalSource: ExchangeRateLocalSource,
    private val bootstrapContextPreferences: BootstrapContextPreferences
) : BaseViewModel() {

    private val _uiState: MutableStateFlow<POSSettingState> =
        MutableStateFlow(POSSettingState.Loading)
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                cashBoxManager.contextFlow,
                cashBoxManager.activeOpeningEntryId(),
                syncPreferences.settings,
                syncManager.state,
                syncLogPreferences.log,
                languagePreferences.language,
                themePreferences.theme,
                themePreferences.themeMode,
                generalPreferences.taxesIncluded,
                generalPreferences.offlineMode,
                generalPreferences.printerEnabled,
                generalPreferences.cashDrawerEnabled,
                generalPreferences.allowNegativeStock,
                returnPolicyPreferences.settings,
                generalPreferences.inventoryAlertsEnabled,
                generalPreferences.inventoryAlertHour,
                generalPreferences.inventoryAlertMinute,
                generalPreferences.salesTargetMonthly
            ) { args: Array<Any?> ->
                val ctx = args[0] as POSContext?
                val openingEntryId = args[1] as String?
                val syncSettings = args[2] as SyncSettings
                val syncState = args[3] as SyncState
                val syncLog = args[4] as List<*>
                val language = args[5] as AppLanguage
                val theme = args[6] as AppColorTheme
                val themeMode = args[7] as AppThemeMode
                val taxes = args[8] as Boolean
                val offline = args[9] as Boolean
                val printer = args[10] as Boolean
                val drawer = args[11] as Boolean
                val allowNegativeStock = args[12] as Boolean
                val returnPolicy = args[13] as com.erpnext.pos.domain.models.ReturnPolicySettings
                val inventoryAlertsEnabled = args[14] as Boolean
                val inventoryAlertHour = args[15] as Int
                val inventoryAlertMinute = args[16] as Int
                val salesTargetMonthlyLocal = args[17] as Double
                val bootstrapSnapshot = bootstrapContextPreferences.load()
                val salesTargetContext = ctx?.monthlySalesTarget?.takeIf { it > 0.0 }
                val salesTargetBootstrap = bootstrapSnapshot.monthlySalesTarget?.takeIf { it > 0.0 }
                val salesTargetFromContext = salesTargetContext != null
                val salesTargetMonthly =
                    salesTargetContext ?: salesTargetBootstrap ?: salesTargetMonthlyLocal.coerceAtLeast(0.0)

                val baseCurrency = normalizeCurrency(ctx?.companyCurrency)
                val secondaryCurrency = ctx?.currency?.let { normalizeCurrency(it) }
                    ?.takeIf { it != baseCurrency }
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val daysInMonth = daysInMonth(now.year, now.month.number)
                val targetDaily = if (daysInMonth > 0) {
                    salesTargetMonthly / daysInMonth.toDouble()
                } else {
                    0.0
                }
                val targetWeekly = targetDaily * 7.0
                val rate = if (secondaryCurrency != null) {
                    resolveLocalRate(baseCurrency, secondaryCurrency)
                } else {
                    null
                }
                val rateValue = rate?.rate
                val stale = rate?.let { isRateStale(it.lastSyncedAt) } ?: false
                val convertedMonthly = rateValue?.let { salesTargetMonthly * it }
                val convertedWeekly = rateValue?.let { targetWeekly * it }
                val convertedDaily = rateValue?.let { targetDaily * it }
                POSSettingState.Success(
                    settings = POSSettingBO(
                        company = ctx?.company ?: "-",
                        posProfile = ctx?.profileName ?: "-",
                        openingEntryId = openingEntryId ?: "-",
                        warehouse = ctx?.warehouse ?: "-",
                        priceList = ctx?.priceList ?: ctx?.currency ?: "-",
                        taxesIncluded = taxes,
                        offlineMode = offline,
                        printerEnabled = printer,
                        cashDrawerEnabled = drawer,
                        allowNegativeStock = allowNegativeStock
                    ),
                    hasContext = ctx != null,
                    syncSettings = syncSettings,
                    syncState = syncState,
                    language = language,
                    theme = theme,
                    themeMode = themeMode,
                    returnPolicy = returnPolicy,
                    inventoryAlertsEnabled = inventoryAlertsEnabled,
                    inventoryAlertHour = inventoryAlertHour,
                    inventoryAlertMinute = inventoryAlertMinute,
                    salesTargetMonthly = salesTargetMonthly,
                    salesTargetWeekly = targetWeekly,
                    salesTargetDaily = targetDaily,
                    salesTargetBaseCurrency = baseCurrency,
                    salesTargetSecondaryCurrency = secondaryCurrency,
                    salesTargetConvertedMonthly = convertedMonthly,
                    salesTargetConvertedWeekly = convertedWeekly,
                    salesTargetConvertedDaily = convertedDaily,
                    salesTargetConversionStale = stale,
                    salesTargetFromContext = salesTargetFromContext,
                    syncLog = syncLog.filterIsInstance<com.erpnext.pos.domain.models.SyncLogEntry>()
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun onSyncNow() {
        viewModelScope.launch { syncManager.fullSync(force = true) }
    }

    fun onCancelSync() {
        syncManager.cancelSync()
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

    fun setTtlHours(hours: Int) {
        viewModelScope.launch { syncPreferences.setTtlHours(hours) }
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

    fun setInventoryAlertsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            generalPreferences.setInventoryAlertsEnabled(enabled)
            updateInventoryAlertSchedule()
        }
    }

    fun setInventoryAlertTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            generalPreferences.setInventoryAlertHour(hour)
            generalPreferences.setInventoryAlertMinute(minute)
            updateInventoryAlertSchedule()
        }
    }

    fun setSalesTargetMonthly(value: Double) {
        viewModelScope.launch { generalPreferences.setSalesTargetMonthly(value) }
    }

    fun setReturnPolicy(settings: com.erpnext.pos.domain.models.ReturnPolicySettings) {
        viewModelScope.launch { returnPolicyPreferences.save(settings) }
    }

    fun syncSalesTargetFromERPNext() {
        viewModelScope.launch {
            syncManager.fullSync(force = true)
        }
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

    private suspend fun updateInventoryAlertSchedule() {
        val enabled = generalPreferences.getInventoryAlertsEnabled()
        val hour = generalPreferences.getInventoryAlertHour()
        val minute = generalPreferences.getInventoryAlertMinute()
        configureInventoryAlertWorker(enabled, hour, minute)
    }

    private fun daysInMonth(year: Int, month: Int): Int {
        return when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            2 -> if (isLeapYear(year)) 29 else 28
            else -> 30
        }
    }

    private suspend fun resolveLocalRate(from: String, to: String): RateInfo? {
        val direct = exchangeRateLocalSource.getRate(from, to)
        if (direct != null) {
            return RateInfo(direct.rate, direct.lastSyncedAt)
        }
        val reverse = exchangeRateLocalSource.getRate(to, from) ?: return null
        if (reverse.rate == 0.0) return null
        return RateInfo(1 / reverse.rate, reverse.lastSyncedAt)
    }

    private fun isRateStale(lastSyncedAt: Long): Boolean {
        val now = Clock.System.now().toEpochMilliseconds()
        val sevenDaysMs = 7 * 24 * 60 * 60 * 1000L
        return now - lastSyncedAt > sevenDaysMs
    }

    private data class RateInfo(
        val rate: Double,
        val lastSyncedAt: Long
    )

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    }
}
