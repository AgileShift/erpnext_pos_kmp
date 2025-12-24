package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.repositories.v2.ISyncRepository
import com.erpnext.pos.domain.sync.SyncDocType
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao
import com.erpnext.pos.localSource.dao.v2.SyncStatusDao
import com.erpnext.pos.localSource.entities.v2.SyncStateEntity
import com.erpnext.pos.remoteSource.dto.v2.SyncDocTypeStateSnapshot
import com.erpnext.pos.remoteSource.dto.v2.SyncStatusSnapshot
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SyncRepository(
    private val syncDao: SyncStatusDao,
    private val invoiceDao: SalesInvoiceDao
) : ISyncRepository {

    @OptIn(ExperimentalTime::class)
    override suspend fun getOrCreate(
        instanceId: String,
        companyId: String
    ): SyncStatusSnapshot {

        val states = syncDao.getAll(instanceId, companyId).ifEmpty {
            ensureDocTypeState(instanceId, companyId, SyncDocType.SALES_INVOICE.value)
            syncDao.getAll(instanceId, companyId)
        }
        val snapshots = states.map { it.toSnapshot() }
        return SyncStatusSnapshot(
            lastFullSyncAt = snapshots.maxOfOrNull { it.lastPullAt ?: 0 }?.takeIf { it > 0 },
            pendingInvoices = invoiceDao.countPendingInvoices(instanceId, companyId),
            failedInvoices = invoiceDao.countFailedInvoices(instanceId, companyId),
            isSyncInProgress = snapshots.any { it.isSyncInProgress },
            docTypes = snapshots
        )
    }

    suspend fun setInProgress(
        instanceId: String,
        companyId: String,
        docType: String,
        inProgress: Boolean
    ) {
        ensureDocTypeState(instanceId, companyId, docType)
        syncDao.setInProgress(instanceId, companyId, docType, inProgress)
    }

    @OptIn(ExperimentalTime::class)
    suspend fun refreshCounters(
        instanceId: String,
        companyId: String,
        docType: String,
        lastFullSyncAt: Long? = null
    ) {
        ensureDocTypeState(instanceId, companyId, docType)
        val (pending, failed) = if (docType == SyncDocType.SALES_INVOICE.value) {
            invoiceDao.countPendingInvoices(instanceId, companyId) to
                invoiceDao.countFailedInvoices(instanceId, companyId)
        } else {
            0 to 0
        }
        syncDao.updateCounters(
            instanceId,
            companyId,
            docType,
            pending,
            failed,
            lastFullSyncAt
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun markPullSuccess(instanceId: String, companyId: String, docType: String) {
        ensureDocTypeState(instanceId, companyId, docType)
        syncDao.updatePullState(
            instanceId,
            companyId,
            docType,
            lastPullAt = Clock.System.now().epochSeconds,
            lastError = null
        )
    }

    @OptIn(ExperimentalTime::class)
    suspend fun markPushSuccess(instanceId: String, companyId: String, docType: String) {
        ensureDocTypeState(instanceId, companyId, docType)
        syncDao.updatePushState(
            instanceId,
            companyId,
            docType,
            lastPushAt = Clock.System.now().epochSeconds,
            lastError = null
        )
    }

    suspend fun markFailure(
        instanceId: String,
        companyId: String,
        docType: String,
        error: Throwable
    ) {
        ensureDocTypeState(instanceId, companyId, docType)
        syncDao.updatePushState(
            instanceId,
            companyId,
            docType,
            lastPushAt = null,
            lastError = error.message ?: error::class.simpleName
        )
    }

    override suspend fun update(entity: SyncStateEntity) {
        syncDao.update(entity)
    }

    private fun SyncStateEntity.toSnapshot() =
        SyncDocTypeStateSnapshot(
            docType = docType,
            lastPullAt = lastPullAt,
            lastPushAt = lastPushAt,
            pendingCount = pendingCount,
            failedCount = failedCount,
            lastError = lastError,
            isSyncInProgress = isSyncInProgress
        )

    private suspend fun ensureDocTypeState(
        instanceId: String,
        companyId: String,
        docType: String
    ) {
        val entity = syncDao.get(instanceId, companyId, docType)
            ?: SyncStateEntity(
                docType = docType,
                lastFullSyncAt = null,
                pendingCount = 0,
                failedCount = 0,
                lastPullAt = null,
                lastPushAt = null,
                lastError = null,
                isSyncInProgress = false
            ).apply {
                this.instanceId = instanceId
                this.companyId = companyId
            }.also { syncDao.insert(it) }
    }
}
