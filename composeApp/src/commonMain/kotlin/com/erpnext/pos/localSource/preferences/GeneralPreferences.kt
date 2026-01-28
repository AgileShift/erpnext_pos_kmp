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
    }

    val taxesIncluded: Flow<Boolean> = store.observeRaw(taxesKey).map { it?.toBooleanStrictOrNull() ?: false }
    val offlineMode: Flow<Boolean> = store.observeRaw(offlineKey).map { it?.toBooleanStrictOrNull() ?: true }
    val printerEnabled: Flow<Boolean> = store.observeRaw(printerKey).map { it?.toBooleanStrictOrNull() ?: true }
    val cashDrawerEnabled: Flow<Boolean> = store.observeRaw(cashDrawerKey).map { it?.toBooleanStrictOrNull() ?: true }

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
}
