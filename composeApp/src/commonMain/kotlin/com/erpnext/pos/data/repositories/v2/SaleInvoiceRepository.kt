package com.erpnext.pos.data.repositories.v2

import com.erpnext.pos.domain.ports.local.SalesInvoiceLocalPort
import com.erpnext.pos.domain.ports.remote.SalesInvoiceRemotePort
import com.erpnext.pos.domain.repositories.v2.ISalesInvoiceRepository
import com.erpnext.pos.domain.sync.SyncContext
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity
import com.erpnext.pos.localSource.relations.v2.SalesInvoiceWithItemsAndPayments

class SalesInvoiceRepository(
    private val dao: SalesInvoiceDao,
    private val local: SalesInvoiceLocalPort,
    private val remote: SalesInvoiceRemotePort
) : ISalesInvoiceRepository {

    suspend fun syncOutbox(ctx: SyncContext): Boolean {
        return true
    }

    suspend fun pullInvoices(ctx: SyncContext): Boolean {
        /*val recent = remote.fetchInvoicesForTerritory(ctx.territoryId, ctx.fromDate)
        val outstanding = remote.fetchOutstandingInvoices()

        val merged = (recent + outstanding).distinctBy { it.name }
        val entities = merged.map { it.toEntity(ctx.instanceId, ctx.companyId) }

        return local.upsertFromServer(ctx.instanceId, ctx.companyId, entities)*/
        return true
    }

    override suspend fun insertInvoiceWithItemsAndPayments(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<SalesInvoicePaymentEntity>
    ) {
        dao.insertInvoiceWithItemsAndPayments(invoice, items, payments)
    }

    override suspend fun getPendingInvoices(
        instanceId: String,
        companyId: String
    ): List<SalesInvoiceWithItemsAndPayments> {
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
