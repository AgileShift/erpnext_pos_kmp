package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.localSource.configuration.ConfigurationStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class BootstrapContextSnapshot(
    val profileName: String? = null,
    val posOpeningEntry: String? = null,
    val fromDate: String? = null,
    val lastRequestAt: Long? = null,
    val lastError: String? = null
)

class BootstrapContextPreferences(
    private val store: ConfigurationStore
) {
    companion object {
        private const val key = "bootstrap.context"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    suspend fun load(): BootstrapContextSnapshot {
        val raw = store.loadRaw(key) ?: return BootstrapContextSnapshot()
        return runCatching { json.decodeFromString<BootstrapContextSnapshot>(raw) }
            .getOrElse { BootstrapContextSnapshot() }
    }

    suspend fun update(
        profileName: String? = null,
        posOpeningEntry: String? = null,
        fromDate: String? = null,
        lastRequestAt: Long? = null,
        lastError: String? = null
    ) {
        val current = load()
        val merged = BootstrapContextSnapshot(
            profileName = profileName?.takeIf { it.isNotBlank() } ?: current.profileName,
            posOpeningEntry = posOpeningEntry?.takeIf { it.isNotBlank() } ?: current.posOpeningEntry,
            fromDate = fromDate?.takeIf { it.isNotBlank() } ?: current.fromDate,
            lastRequestAt = lastRequestAt ?: current.lastRequestAt,
            lastError = lastError ?: current.lastError
        )
        store.saveRaw(key, json.encodeToString(merged))
    }
}
