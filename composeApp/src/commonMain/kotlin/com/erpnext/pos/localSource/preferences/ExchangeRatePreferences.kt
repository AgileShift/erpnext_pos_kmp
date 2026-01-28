package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.localSource.configuration.ConfigurationStore

class ExchangeRatePreferences(
    private val store: ConfigurationStore
) {
    companion object {
        const val DEFAULT_MANUAL_RATE = 36.6243
        private const val manualExchangeRateKey = "manual_exchange_rate"
    }

    suspend fun loadManualRate(): Double? =
        store.loadRaw(manualExchangeRateKey)?.toDoubleOrNull()

    suspend fun saveManualRate(rate: Double) {
        store.saveRaw(manualExchangeRateKey, rate.toString())
    }
}
