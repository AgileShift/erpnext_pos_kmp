package com.erpnext.pos.localSource.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SyncPreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val autoSyncKey = booleanPreferencesKey("sync_auto")
        private val syncOnStartupKey = booleanPreferencesKey("sync_on_startup")
        private val wifiOnlyKey = booleanPreferencesKey("sync_wifi_only")
        private val lastSyncAtKey = longPreferencesKey("sync_last_at")
    }

    val settings: Flow<SyncSettings> = dataStore.data.map { prefs ->
        SyncSettings(
            autoSync = prefs[autoSyncKey] ?: true,
            syncOnStartup = prefs[syncOnStartupKey] ?: true,
            wifiOnly = prefs[wifiOnlyKey] ?: false,
            lastSyncAt = prefs[lastSyncAtKey]
        )
    }

    suspend fun setAutoSync(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[autoSyncKey] = enabled
        }
    }

    suspend fun setSyncOnStartup(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[syncOnStartupKey] = enabled
        }
    }

    suspend fun setWifiOnly(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[wifiOnlyKey] = enabled
        }
    }

    suspend fun setLastSyncAt(epochMillis: Long) {
        dataStore.edit { prefs ->
            prefs[lastSyncAtKey] = epochMillis
        }
    }
}

data class SyncSettings(
    val autoSync: Boolean,
    val syncOnStartup: Boolean,
    val wifiOnly: Boolean,
    val lastSyncAt: Long?
)
