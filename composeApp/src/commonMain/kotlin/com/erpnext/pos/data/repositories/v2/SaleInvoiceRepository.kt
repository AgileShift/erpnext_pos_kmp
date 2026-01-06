package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.ports.local.SalesInvoiceLocalPort
import com.erpnext.pos.domain.repositories.v2.ISalesInvoiceRepository
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity
import com.erpnext.pos.localSource.relations.v2.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.api.v2.APIServiceV2
import com.erpnext.pos.remoteSource.dto.v2.SalesInvoiceSnapshot
import com.erpnext.pos.remoteSource.mapper.v2.toEntity
import com.erpnext.pos.remoteSource.sdk.v2.ERPDocType
import com.erpnext.pos.remoteSource.sdk.v2.IncrementalSyncFilters
import com.erpnext.pos.remoteSource.sdk.v2.getFields
import com.erpnext.pos.utils.RepoTrace
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SalesInvoiceRepository(
    private val dao: SalesInvoiceDao,
    private val local: SalesInvoiceLocalPort,
    private val remote: SalesInvoiceRemoteRepository,
    private val api: APIServiceV2
) : ISalesInvoiceRepository {

    @OptIn(ExperimentalTime::class)
    suspend fun syncOutbox(ctx: SyncContext): Boolean {
        RepoTrace.breadcrumb("SalesInvoiceRepositoryV2", "syncOutbox")
        val pending = local.getPendingOutbox(ctx.instanceId, ctx.companyId)
        if (pending.isEmpty()) return false

        val now = Clock.System.now().epochSeconds
        pending.forEach { invoice ->
            val res = remote.submitInvoice(invoice)
            if (res.isSuccess) {
                val response = res.getOrNull()
                if (response != null) {
                    local.markOutboxSynced(
                        ctx.instanceId,
                        ctx.companyId,
                        invoice.invoice.invoiceId,
                        response.name,
                        response.modified
                    )
                } else {
                    dao.updateSyncStatus(
                        ctx.instanceId,
                        ctx.companyId,
                        invoice.invoice.invoiceId,
                        syncStatus = "SYNCED",
                        lastSyncedAt = now,
                        updatedAt = now
                    )
                }
            } else {
                dao.updateSyncStatus(
                    ctx.instanceId,
                    ctx.companyId,
                    invoice.invoice.invoiceId,
                    syncStatus = "FAILED",
                    lastSyncedAt = null,
                    updatedAt = now
                )
            }
        }
        return true
    }

    suspend fun pullInvoices(ctx: SyncContext): Boolean {
        RepoTrace.breadcrumb("SalesInvoiceRepositoryV2", "pullInvoices")
        val invoices = api.list<SalesInvoiceSnapshot>(
            doctype = ERPDocType.SalesInvoice,
            fields = ERPDocType.SalesInvoice.getFields() + "modified",
            filters = IncrementalSyncFilters.salesInvoice(ctx)
        )
        if (invoices.isEmpty()) return false
        val entities = invoices.map { it.toEntity(ctx.instanceId, ctx.companyId) }
        return local.upsertFromServer(ctx.instanceId, ctx.companyId, entities)
    }

    override suspend fun insertInvoiceWithItemsAndPayments(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<SalesInvoicePaymentEntity>
    ) {
        RepoTrace.breadcrumb("SalesInvoiceRepositoryV2", "insertInvoiceWithItemsAndPayments")
        dao.insertInvoiceWithItemsAndPayments(invoice, items, payments)
    }

    override suspend fun getPendingInvoices(
        instanceId: String,
        companyId: String
    ): List<SalesInvoiceWithItemsAndPayments> {
        RepoTrace.breadcrumb("SalesInvoiceRepositoryV2", "getPendingInvoices")
        return dao.getPendingInvoicesWithDetails(instanceId, companyId)
    }

    override suspend fun getRelevantInvoices(
        instanceId: String,
        companyId: String,
        territoryId: String,
        fromDate: Long
    ) = dao.getRelevantInvoicesForTerritory(
        instanceId,
        companyId,
        territoryId,
        fromDate
    )

    override suspend fun countPending(instanceId: String, companyId: String) =
        dao.countPendingInvoices(instanceId, companyId)

    override suspend fun countFailed(instanceId: String, companyId: String) =
        dao.countFailedInvoices(instanceId, companyId)
}
