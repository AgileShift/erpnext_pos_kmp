package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.domain.models.SyncLogEntry
import com.erpnext.pos.localSource.configuration.ConfigurationStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SyncLogPreferences(
    private val store: ConfigurationStore
) {
    companion object {
        private const val logKey = "sync_log_entries"
        private const val maxEntries = 20
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    val log: Flow<List<SyncLogEntry>> = store.observeRaw(logKey).map { payload ->
        payload?.let { runCatching { json.decodeFromString<List<SyncLogEntry>>(it) }.getOrNull() }
            ?: emptyList()
    }

    suspend fun append(entry: SyncLogEntry) {
        val current = store.observeRaw(logKey).map { payload ->
            payload?.let {
                runCatching { json.decodeFromString<List<SyncLogEntry>>(it) }.getOrNull()
            }
        }.map { it ?: emptyList() }.firstOrNull().orEmpty()
        val updated = (listOf(entry) + current).take(maxEntries)
        store.saveRaw(logKey, json.encodeToString(updated))
    }

    suspend fun clear() {
        store.delete(logKey)
    }
}
