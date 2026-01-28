package com.erpnext.pos.localSource.configuration

import com.erpnext.pos.localSource.dao.ConfigurationDao
import com.erpnext.pos.localSource.entities.ConfigurationEntity
import kotlinx.coroutines.flow.Flow

class ConfigurationStore(private val dao: ConfigurationDao) {
    fun observeRaw(key: String): Flow<String?> = dao.observeValue(key)

    suspend fun loadRaw(key: String): String? = dao.getValue(key)?.value

    suspend fun saveRaw(key: String, value: String) {
        dao.upsert(ConfigurationEntity(key = key, value = value))
    }

    suspend fun delete(key: String) {
        dao.delete(key)
    }
}
