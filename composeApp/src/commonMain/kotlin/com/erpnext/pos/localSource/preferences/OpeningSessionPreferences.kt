package com.erpnext.pos.localSource.preferences

import com.erpnext.pos.domain.models.OpeningSessionDraft
import com.erpnext.pos.localSource.configuration.ConfigurationStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OpeningSessionPreferences(
    private val store: ConfigurationStore
) {
    companion object {
        private const val draftKey = "opening_session_draft"
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    val draft: Flow<OpeningSessionDraft?> = store.observeRaw(draftKey).map { payload ->
        payload?.let { runCatching { json.decodeFromString<OpeningSessionDraft>(it) }.getOrNull() }
    }

    suspend fun saveDraft(draft: OpeningSessionDraft) {
        store.saveRaw(draftKey, json.encodeToString(draft))
    }

    suspend fun clearDraft() {
        store.delete(draftKey)
    }
}
