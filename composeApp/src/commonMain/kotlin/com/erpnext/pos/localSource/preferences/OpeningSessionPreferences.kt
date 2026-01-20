package com.erpnext.pos.localSource.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.erpnext.pos.domain.models.OpeningSessionDraft
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class OpeningSessionPreferences(
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val draftKey = stringPreferencesKey("opening_session_draft")
        private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    }

    val draft: Flow<OpeningSessionDraft?> = dataStore.data.map { prefs ->
        prefs[draftKey]?.let { payload ->
            runCatching { json.decodeFromString<OpeningSessionDraft>(payload) }.getOrNull()
        }
    }

    suspend fun saveDraft(draft: OpeningSessionDraft) {
        dataStore.edit { prefs ->
            prefs[draftKey] = json.encodeToString(draft)
        }
    }

    suspend fun clearDraft() {
        dataStore.edit { prefs ->
            prefs.remove(draftKey)
        }
    }
}
