package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class SyncPreferences(
    private val store: ConfigurationStore
) {
    companion object {
        private const val autoSyncKey = "sync_auto"
        private const val syncOnStartupKey = "sync_on_startup"
        private const val wifiOnlyKey = "sync_wifi_only"
        private const val lastSyncAtKey = "sync_last_at"
        private const val useTtlKey = "sync_use_ttl"
        private const val ttlHoursKey = "sync_ttl_hours"
        private const val defaultTtlHours = 6
    }

    private fun observeBoolean(key: String, default: Boolean) =
        store.observeRaw(key).map { it?.toBooleanStrictOrNull() ?: default }

    private fun observeLong(key: String) =
        store.observeRaw(key).map { it?.toLongOrNull() }

    private fun observeInt(key: String, default: Int) =
        store.observeRaw(key).map { it?.toIntOrNull() ?: default }

    val settings: Flow<SyncSettings> = combine(
        observeBoolean(autoSyncKey, true),
        observeBoolean(syncOnStartupKey, true),
        observeBoolean(wifiOnlyKey, false),
        observeLong(lastSyncAtKey),
        observeBoolean(useTtlKey, false),
        observeInt(ttlHoursKey, defaultTtlHours)
    ) { autoSync, syncOnStartup, wifiOnly, lastSyncAt, useTtl, ttlHours ->
        SyncSettings(
            autoSync = autoSync,
            syncOnStartup = syncOnStartup,
            wifiOnly = wifiOnly,
            lastSyncAt = lastSyncAt,
            useTtl = useTtl,
            ttlHours = ttlHours.coerceIn(1, 168)
        )
    }

    suspend fun setAutoSync(enabled: Boolean) {
        store.saveRaw(autoSyncKey, enabled.toString())
    }

    suspend fun setSyncOnStartup(enabled: Boolean) {
        store.saveRaw(syncOnStartupKey, enabled.toString())
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        store.saveRaw(wifiOnlyKey, enabled.toString())
    }

    suspend fun setLastSyncAt(epochMillis: Long) {
        store.saveRaw(lastSyncAtKey, epochMillis.toString())
    }

    suspend fun setUseTtl(enabled: Boolean) {
        store.saveRaw(useTtlKey, enabled.toString())
    }

    suspend fun setTtlHours(hours: Int) {
        store.saveRaw(ttlHoursKey, hours.coerceIn(1, 168).toString())
    }
}

data class SyncSettings(
    val autoSync: Boolean,
    val syncOnStartup: Boolean,
    val wifiOnly: Boolean,
    val lastSyncAt: Long?,
    val useTtl: Boolean,
    val ttlHours: Int
)
