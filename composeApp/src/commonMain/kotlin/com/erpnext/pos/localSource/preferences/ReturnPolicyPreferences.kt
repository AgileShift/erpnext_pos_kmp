package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.domain.models.ReturnPolicySettings
import com.erpnext.pos.localSource.configuration.ConfigurationStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ReturnPolicyPreferences(
    private val store: ConfigurationStore
) {
    companion object {
        private const val returnPolicyKey = "settings_return_policy"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    val settings: Flow<ReturnPolicySettings> =
        store.observeRaw(returnPolicyKey).map { raw ->
            if (raw.isNullOrBlank()) {
                ReturnPolicySettings()
            } else {
                runCatching { json.decodeFromString<ReturnPolicySettings>(raw) }
                    .getOrDefault(ReturnPolicySettings())
            }
        }

    suspend fun get(): ReturnPolicySettings {
        val raw = store.loadRaw(returnPolicyKey)
        return if (raw.isNullOrBlank()) {
            ReturnPolicySettings()
        } else {
            runCatching { json.decodeFromString<ReturnPolicySettings>(raw) }
                .getOrDefault(ReturnPolicySettings())
        }
    }

    suspend fun save(settings: ReturnPolicySettings) {
        store.saveRaw(returnPolicyKey, json.encodeToString(settings))
    }
}

