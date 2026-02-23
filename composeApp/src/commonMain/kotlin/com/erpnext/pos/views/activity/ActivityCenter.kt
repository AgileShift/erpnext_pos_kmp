package com.erpnext.pos.views.activity

import com.erpnext.pos.localSource.preferences.ActivityPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ActivityCenter(
    private val preferences: ActivityPreferences,
) {

    val entries: Flow<List<ActivityEntry>> = preferences.entries
    val unreadCount: Flow<Int> = entries.map { list -> list.count { !it.isRead } }

    suspend fun markRead(id: String) {
        preferences.markRead(id)
    }

    suspend fun markAllRead() {
        preferences.markAllRead()
    }

    private fun extractActivityItems(element: JsonElement?): List<JsonObject> {
        return when (element) {
            null -> emptyList()
            is JsonArray -> element.mapNotNull { it as? JsonObject }
            is JsonObject -> {
                when {
                    element["items"] is JsonArray -> extractActivityItems(element["items"])
                    element["activity"] != null -> extractActivityItems(element["activity"])
                    element["activity_events"] != null -> extractActivityItems(element["activity_events"])
                    element["data"] != null -> extractActivityItems(element["data"])
                    element["message"] != null -> extractActivityItems(element["message"])
                    else -> emptyList()
                }
            }

            else -> emptyList()
        }
    }
}
