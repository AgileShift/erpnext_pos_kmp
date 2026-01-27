package com.erpnext.pos.localSource.datasources

import com.erpnext.pos.localSource.dao.CustomerOutboxDao
import com.erpnext.pos.localSource.entities.CustomerOutboxEntity

class CustomerOutboxLocalSource(
    private val dao: CustomerOutboxDao
) {
    suspend fun insert(entity: CustomerOutboxEntity) = dao.insert(entity)

    suspend fun getPending(): List<CustomerOutboxEntity> = dao.getPending()

    suspend fun updateStatus(
        localId: String,
        status: String,
        error: String?,
        attemptIncrement: Int,
        attemptAt: Long
    ) = dao.updateStatus(localId, status, error, attemptIncrement, attemptAt)

    suspend fun updateRemoteId(localId: String, remoteId: String) =
        dao.updateRemoteId(localId, remoteId)

    suspend fun deleteMissingCustomerIds(ids: List<String>) {
        val safe = ids.ifEmpty { listOf("__empty__") }
        dao.deleteByCustomerIdsNotIn(safe)
    }
}
