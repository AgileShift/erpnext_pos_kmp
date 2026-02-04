@file:OptIn(ExperimentalTime::class)

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
import com.erpnext.pos.data.repositories.SalesTargetRepository
import com.erpnext.pos.localSource.datasources.ExchangeRateLocalSource
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.sync.SyncState
import com.erpnext.pos.views.CashBoxManager
import com.erpnext.pos.views.POSContext
import com.erpnext.pos.utils.notifications.configureInventoryAlertWorker
import com.erpnext.pos.utils.normalizeCurrency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SettingsViewModel(
    private val cashBoxManager: CashBoxManager,
    private val syncPreferences: SyncPreferences,
    private val syncManager: SyncManager,
    private val generalPreferences: GeneralPreferences,
    private val languagePreferences: LanguagePreferences,
    private val themePreferences: ThemePreferences,
    private val salesTargetRepository: SalesTargetRepository,
    private val exchangeRateLocalSource: ExchangeRateLocalSource
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
                generalPreferences.allowNegativeStock,
                generalPreferences.inventoryAlertsEnabled,
                generalPreferences.inventoryAlertHour,
                generalPreferences.inventoryAlertMinute,
                generalPreferences.salesTargetMonthly
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
                val inventoryAlertsEnabled = args[11] as Boolean
                val inventoryAlertHour = args[12] as Int
                val inventoryAlertMinute = args[13] as Int
                val salesTargetMonthly = args[14] as Double

                val baseCurrency = normalizeCurrency(ctx.companyCurrency)
                val secondaryCurrency = normalizeCurrency(ctx.currency)
                    .takeIf { it != baseCurrency }
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val daysInMonth = daysInMonth(now.year, now.month.number)
                val weeksInMonth = weeksInMonth(now.year, now.month.number, daysInMonth)
                val targetWeekly = if (weeksInMonth > 0) {
                    salesTargetMonthly / weeksInMonth.toDouble()
                } else {
                    0.0
                }
                val targetDaily = if (daysInMonth > 0) {
                    salesTargetMonthly / daysInMonth.toDouble()
                } else {
                    0.0
                }
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
                    themeMode = themeMode,
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
                    salesTargetConversionStale = stale
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

    fun syncSalesTargetFromERPNext() {
        viewModelScope.launch {
            val ctx = cashBoxManager.getContext() ?: return@launch
            val target = salesTargetRepository.fetchMonthlyCompanyTarget(ctx.company)
                ?: return@launch
            generalPreferences.setSalesTargetMonthly(target)
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

    private fun weeksInMonth(year: Int, month: Int, daysInMonth: Int): Int {
        if (daysInMonth <= 0) return 0
        val firstDay = kotlinx.datetime.LocalDate(year, month, 1)
        var mondays = 0
        for (i in 0 until daysInMonth) {
            val day = firstDay.plus(kotlinx.datetime.DatePeriod(days = i))
            if (day.dayOfWeek == kotlinx.datetime.DayOfWeek.MONDAY) {
                mondays++
            }
        }
        return if (firstDay.dayOfWeek == kotlinx.datetime.DayOfWeek.MONDAY) mondays else mondays + 1
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
