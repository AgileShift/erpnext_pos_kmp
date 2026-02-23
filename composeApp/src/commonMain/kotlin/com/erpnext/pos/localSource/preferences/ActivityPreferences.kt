package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import com.erpnext.pos.views.activity.ActivityEntry
import kotlin.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ActivityPreferences(
    private val store: ConfigurationStore
) {
    companion object {
        private const val activityKey = "activity_feed_entries"
        private const val maxEntries = 120
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    val entries: Flow<List<ActivityEntry>> = store.observeRaw(activityKey).map { payload ->
        payload?.let { runCatching { json.decodeFromString<List<ActivityEntry>>(it) }.getOrNull() }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    suspend fun append(entry: ActivityEntry): Boolean {
        val current = currentEntries()
        if (current.any { it.id == entry.id }) return false
        val updated = (listOf(entry) + current)
            .sortedByDescending { it.createdAt }
            .take(maxEntries)
        store.saveRaw(activityKey, json.encodeToString(updated))
        return true
    }

    suspend fun upsertFromApi(apiEntries: List<ActivityEntry>): List<ActivityEntry> {
        if (apiEntries.isEmpty()) return emptyList()
        val current = currentEntries()
        val currentById = current.associateBy { it.id }

        val merged = apiEntries
            .map { incoming ->
                val existing = currentById[incoming.id]
                if (existing == null) {
                    incoming
                } else {
                    incoming.copy(readAt = existing.readAt)
                }
            }
            .sortedByDescending { it.createdAt }
            .take(maxEntries)

        val inserted = merged.filter { currentById[it.id] == null }
        val hasChanged = merged.size != current.size ||
            merged.zip(current).any { (next, prev) -> next != prev }
        if (hasChanged) {
            store.saveRaw(activityKey, json.encodeToString(merged))
        }
        return inserted
    }

    suspend fun markRead(id: String) {
        val now = Clock.System.now().toEpochMilliseconds()
        val updated = currentEntries().map { item ->
            if (item.id == id && !item.isRead) item.copy(readAt = now) else item
        }
        store.saveRaw(activityKey, json.encodeToString(updated))
    }

    suspend fun markAllRead() {
        val now = Clock.System.now().toEpochMilliseconds()
        val updated = currentEntries().map { item ->
            if (item.isRead) item else item.copy(readAt = now)
        }
        store.saveRaw(activityKey, json.encodeToString(updated))
    }

    private suspend fun currentEntries(): List<ActivityEntry> {
        return entries.firstOrNull().orEmpty()
    }
}
