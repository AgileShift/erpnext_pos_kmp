package com.erpnext.pos.views.activity

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import com.erpnext.pos.localSource.preferences.ActivityPreferences
import com.erpnext.pos.sync.SyncManager
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.parseErpDateTimeToEpochMillis
import com.erpnext.pos.utils.view.SnackbarController
import com.erpnext.pos.utils.view.SnackbarPosition
import com.erpnext.pos.utils.view.SnackbarType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class ActivityCenter(
    private val preferences: ActivityPreferences,
    private val configurationStore: ConfigurationStore,
    private val snackbarController: SnackbarController,
    private val syncManager: SyncManager,
    private val networkMonitor: NetworkMonitor
) {
    companion object {
        private const val bootstrapActivityKey = "bootstrap.raw.activity_events"
    }

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    init {
        observeApiActivityFeed()
    }

    val entries: Flow<List<ActivityEntry>> = preferences.entries
    val unreadCount: Flow<Int> = entries.map { list -> list.count { !it.isRead } }

    suspend fun publish(event: ActivityEvent) {
        // Opcional para eventos locales puntuales. La fuente principal es API/bootstrap.
        val createdAt = Clock.System.now().toEpochMilliseconds()
        val inserted = preferences.append(
            ActivityEntry(
                id = event.id,
                title = event.title,
                message = event.message,
                priority = event.priority,
                changeType = event.changeType,
                createdAt = createdAt
            )
        )
        if (!inserted) return
        maybeReactToNewEntries(
            listOf(
                ActivityEntry(
                    id = event.id,
                    title = event.title,
                    message = event.message,
                    priority = event.priority,
                    changeType = event.changeType,
                    createdAt = createdAt
                )
            )
        )
    }

    suspend fun markRead(id: String) {
        preferences.markRead(id)
    }

    suspend fun markAllRead() {
        preferences.markAllRead()
    }

    private fun observeApiActivityFeed() {
        scope.launch {
            configurationStore.observeRaw(bootstrapActivityKey)
                .mapLatest { payload -> decodeApiEntries(payload) }
                .collect { apiEntries ->
                    if (apiEntries.isEmpty()) return@collect
                    val inserted = preferences.upsertFromApi(apiEntries)
                    if (inserted.isNotEmpty()) {
                        maybeReactToNewEntries(inserted)
                    }
                }
        }
    }

    private suspend fun maybeReactToNewEntries(entries: List<ActivityEntry>) {
        val highPriority = entries.firstOrNull { it.priority == ActivityPriority.HIGH }
        if (highPriority != null) {
            snackbarController.show(
                message = "${highPriority.title}: ${highPriority.message}",
                type = SnackbarType.Error,
                position = SnackbarPosition.Top
            )
        }

        val shouldSync = entries.any {
            it.changeType == ActivityChangeType.CREATED || it.changeType == ActivityChangeType.UPDATED
        }
        if (shouldSync && networkMonitor.isConnected.first()) {
            syncManager.fullSync(force = true)
        }
    }

    private fun decodeApiEntries(payload: String?): List<ActivityEntry> {
        if (payload.isNullOrBlank()) return emptyList()
        val parsed = runCatching { json.parseToJsonElement(payload) }.getOrNull() ?: return emptyList()
        val rawItems = extractActivityItems(parsed)
        return rawItems.mapNotNull(::mapApiItem).sortedByDescending { it.createdAt }
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

    private fun mapApiItem(item: JsonObject): ActivityEntry? {
        val actionRaw = item.string("action").orEmpty()
        val eventType = item.string("event_type").orEmpty()
        val referenceName = item.string("reference_name").orEmpty()
        val fallbackId = listOf(
            eventType.ifBlank { "activity" },
            actionRaw.ifBlank { "event" },
            referenceName.ifBlank { item.string("title").orEmpty() },
            item.string("created_on").orEmpty()
        )
            .filter { it.isNotBlank() }
            .joinToString("::")
        val id = item.string("name")?.takeIf { it.isNotBlank() }
            ?: fallbackId.takeIf { it.isNotBlank() }
            ?: return null
        val title = item.string("title")
            ?.takeIf { it.isNotBlank() }
            ?: "[ERPNext POS] ${eventType.ifBlank { "Activity" }} ${actionRaw.ifBlank { "Event" }}: $referenceName"
        val message = item.string("message")
            ?.takeIf { it.isNotBlank() }
            ?: "$eventType $referenceName ${actionRaw.lowercase()}".trim()
        val createdAt = parseBootstrapActivityTimestamp(item.string("created_on"))
            ?: Clock.System.now().toEpochMilliseconds()
        return ActivityEntry(
            id = id,
            title = title,
            message = message,
            priority = resolvePriority(actionRaw, title, message),
            changeType = resolveChangeType(actionRaw),
            createdAt = createdAt
        )
    }

    private fun parseBootstrapActivityTimestamp(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        val normalized = value
            .trim()
            .replace("T", " ")
            .substringBefore("Z")
            .substringBefore("+")
            .substringBefore(".")
        return parseErpDateTimeToEpochMillis(normalized)
    }

    private fun resolvePriority(
        action: String,
        title: String,
        message: String
    ): ActivityPriority {
        val normalized = "$action $title $message".lowercase()
        return when {
            normalized.contains("error") ||
                normalized.contains("failed") ||
                normalized.contains("fall") ||
                normalized.contains("cancel") ||
                normalized.contains("rechaz") -> ActivityPriority.HIGH
            normalized.contains("submitted") ||
                normalized.contains("created") ||
                normalized.contains("updated") ||
                normalized.contains("modific") -> ActivityPriority.MEDIUM
            else -> ActivityPriority.LOW
        }
    }

    private fun resolveChangeType(action: String): ActivityChangeType {
        val normalized = action.lowercase()
        return when {
            normalized.contains("create") -> ActivityChangeType.CREATED
            normalized.contains("update") ||
                normalized.contains("modif") ||
                normalized.contains("submit") -> ActivityChangeType.UPDATED
            else -> ActivityChangeType.NONE
        }
    }

    private fun JsonObject.string(key: String): String? =
        this[key]?.jsonPrimitive?.contentOrNull
}
