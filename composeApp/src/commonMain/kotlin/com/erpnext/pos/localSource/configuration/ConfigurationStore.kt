package com.erpnext.pos.localSource.configuration

import com.erpnext.pos.localSource.dao.ConfigurationDao
import com.erpnext.pos.localSource.entities.ConfigurationEntity
import com.erpnext.pos.remoteSource.oauth.AuthInfoStore
import com.erpnext.pos.utils.instanceKeyFromUrl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ConfigurationStore(
    private val dao: ConfigurationDao,
    private val authInfoStore: AuthInfoStore
) {
    fun observeRaw(key: String): Flow<String?> = flow {
        val scopedKey = scopedKey(key)
        migrateLegacyIfNeeded(rawKey = key, scopedKey = scopedKey)
        emitAll(dao.observeValue(scopedKey))
    }

    suspend fun loadRaw(key: String): String? {
        val scopedKey = scopedKey(key)
        migrateLegacyIfNeeded(rawKey = key, scopedKey = scopedKey)
        return dao.getValue(scopedKey)?.value
    }

    suspend fun saveRaw(key: String, value: String) {
        val scopedKey = scopedKey(key)
        dao.upsert(ConfigurationEntity(key = scopedKey, value = value))
        if (scopedKey != key) {
            dao.delete(key)
        }
    }

    suspend fun delete(key: String) {
        val scopedKey = scopedKey(key)
        dao.delete(scopedKey)
        if (scopedKey != key) {
            dao.delete(key)
        }
    }

    private suspend fun scopedKey(rawKey: String): String {
        val currentSite = authInfoStore.getCurrentSite()
        if (currentSite.isNullOrBlank()) return rawKey
        val instanceKey = instanceKeyFromUrl(currentSite)
        return "$instanceKey::$rawKey"
    }

    private suspend fun migrateLegacyIfNeeded(rawKey: String, scopedKey: String) {
        if (scopedKey == rawKey) return
        val scopedValue = dao.getValue(scopedKey)?.value
        if (scopedValue != null) return
        val legacyValue = dao.getValue(rawKey)?.value ?: return
        dao.upsert(ConfigurationEntity(key = scopedKey, value = legacyValue))
        dao.delete(rawKey)
    }
}
