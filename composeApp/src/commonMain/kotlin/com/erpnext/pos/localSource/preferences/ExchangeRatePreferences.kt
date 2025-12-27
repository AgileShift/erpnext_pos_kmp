package com.erpnext.pos.localSource.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.first

class ExchangeRatePreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        const val DEFAULT_MANUAL_RATE = 36.6243
        private val manualExchangeRateKey = doublePreferencesKey("manual_exchange_rate")
    }

    suspend fun loadManualRate(): Double? {
        val prefs = dataStore.data.first()
        return prefs[manualExchangeRateKey]
    }

    suspend fun saveManualRate(rate: Double) {
        dataStore.edit { prefs ->
            prefs[manualExchangeRateKey] = rate
        }
    }
}
