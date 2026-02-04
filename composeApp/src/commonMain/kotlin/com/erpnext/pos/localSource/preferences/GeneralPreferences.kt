package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GeneralPreferences(
    private val store: ConfigurationStore
) {
    companion object {
        private const val taxesKey = "settings_taxes_included"
        private const val offlineKey = "settings_offline_mode"
        private const val printerKey = "settings_printer_enabled"
        private const val cashDrawerKey = "settings_cash_drawer_enabled"
        private const val allowNegativeStockKey = "settings_allow_negative_stock"
        private const val inventoryAlertDateKey = "inventory_alert_last_date"
        private const val inventoryAlertsEnabledKey = "inventory_alerts_enabled"
        private const val inventoryAlertHourKey = "inventory_alert_hour"
        private const val inventoryAlertMinuteKey = "inventory_alert_minute"
    }

    val taxesIncluded: Flow<Boolean> = store.observeRaw(taxesKey).map { it?.toBooleanStrictOrNull() ?: false }
    val offlineMode: Flow<Boolean> = store.observeRaw(offlineKey).map { it?.toBooleanStrictOrNull() ?: true }
    val printerEnabled: Flow<Boolean> = store.observeRaw(printerKey).map { it?.toBooleanStrictOrNull() ?: true }
    val cashDrawerEnabled: Flow<Boolean> = store.observeRaw(cashDrawerKey).map { it?.toBooleanStrictOrNull() ?: true }
    val allowNegativeStock: Flow<Boolean> =
        store.observeRaw(allowNegativeStockKey).map { it?.toBooleanStrictOrNull() ?: false }

    val inventoryAlertsEnabled: Flow<Boolean> =
        store.observeRaw(inventoryAlertsEnabledKey).map { it?.toBooleanStrictOrNull() ?: true }
    val inventoryAlertHour: Flow<Int> =
        store.observeRaw(inventoryAlertHourKey).map { it?.toIntOrNull() ?: 9 }
    val inventoryAlertMinute: Flow<Int> =
        store.observeRaw(inventoryAlertMinuteKey).map { it?.toIntOrNull() ?: 0 }

    suspend fun setTaxesIncluded(enabled: Boolean) {
        store.saveRaw(taxesKey, enabled.toString())
    }

    suspend fun setOfflineMode(enabled: Boolean) {
        store.saveRaw(offlineKey, enabled.toString())
    }

    suspend fun setPrinterEnabled(enabled: Boolean) {
        store.saveRaw(printerKey, enabled.toString())
    }

    suspend fun setCashDrawerEnabled(enabled: Boolean) {
        store.saveRaw(cashDrawerKey, enabled.toString())
    }

    suspend fun setAllowNegativeStock(enabled: Boolean) {
        store.saveRaw(allowNegativeStockKey, enabled.toString())
    }

    suspend fun getInventoryAlertDate(): String? = store.loadRaw(inventoryAlertDateKey)

    suspend fun setInventoryAlertDate(value: String) {
        store.saveRaw(inventoryAlertDateKey, value)
    }

    suspend fun setInventoryAlertsEnabled(enabled: Boolean) {
        store.saveRaw(inventoryAlertsEnabledKey, enabled.toString())
    }

    suspend fun setInventoryAlertHour(value: Int) {
        store.saveRaw(inventoryAlertHourKey, value.coerceIn(0, 23).toString())
    }

    suspend fun setInventoryAlertMinute(value: Int) {
        store.saveRaw(inventoryAlertMinuteKey, value.coerceIn(0, 59).toString())
    }

    suspend fun getInventoryAlertsEnabled(): Boolean =
        store.loadRaw(inventoryAlertsEnabledKey)?.toBooleanStrictOrNull() ?: true

    suspend fun getInventoryAlertHour(): Int =
        store.loadRaw(inventoryAlertHourKey)?.toIntOrNull() ?: 9

    suspend fun getInventoryAlertMinute(): Int =
        store.loadRaw(inventoryAlertMinuteKey)?.toIntOrNull() ?: 0
}
