package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.repositories.v2.ISyncRepository
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.v2.SyncStatusDao
import com.erpnext.pos.localSource.entities.v2.SyncStateEntity
import com.erpnext.pos.remoteSource.dto.v2.SyncStatusSnapshot

class SyncRepository(
    private val syncDao: SyncStatusDao,
    private val invoiceDao: SalesInvoiceDao
) : ISyncRepository {

    override suspend fun getOrCreate(
        instanceId: String,
        companyId: String
    ): SyncStatusSnapshot {

        val entity = syncDao.get(instanceId, companyId)
            ?: SyncStateEntity(
                lastFullSyncAt = null,
                pendingInvoices = 0,
                failedInvoices = 0,
                isSyncInProgress = false
            ).apply {
                this.instanceId = instanceId
                this.companyId = companyId
            }.also {
                syncDao.insert(it)
            }

        return entity.toSnapshot()
    }

    suspend fun setInProgress(instanceId: String, companyId: String, inProgress: Boolean) {
        getOrCreate(instanceId, companyId) // ensure exists
        syncDao.setInProgress(instanceId, companyId, inProgress)
    }

    suspend fun refreshCounters(
        instanceId: String,
        companyId: String,
        lastFullSyncAt: Long? = null
    ) {
        getOrCreate(instanceId, companyId)
        val pending = invoiceDao.countPendingInvoices(instanceId, companyId)
        val failed = invoiceDao.countFailedInvoices(instanceId, companyId)
        syncDao.updateCounters(instanceId, companyId, pending, failed, lastFullSyncAt)
    }

    override suspend fun update(entity: SyncStateEntity) {
        syncDao.update(entity)
    }

    private fun SyncStateEntity.toSnapshot() =
        SyncStatusSnapshot(
            lastFullSyncAt = lastFullSyncAt,
            pendingInvoices = pendingInvoices,
            failedInvoices = failedInvoices,
            isSyncInProgress = isSyncInProgress
        )
}