package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import com.erpnext.pos.utils.CurrencyPrecisionSnapshot
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class CurrencySettingsPreferences(
    private val store: ConfigurationStore
) {
    companion object {
        private const val currencySettingsKey = "currency_precision_settings"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun load(): CurrencyPrecisionSnapshot? {
        val raw = store.loadRaw(currencySettingsKey) ?: return null
        return runCatching { json.decodeFromString<CurrencyPrecisionSnapshot>(raw) }.getOrNull()
    }

    suspend fun save(snapshot: CurrencyPrecisionSnapshot) {
        store.saveRaw(currencySettingsKey, json.encodeToString(snapshot))
    }
}
