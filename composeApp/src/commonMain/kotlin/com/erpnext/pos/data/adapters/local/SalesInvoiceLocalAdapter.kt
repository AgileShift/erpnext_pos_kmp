package com.erpnext.pos.data.adapters.local

import com.erpnext.pos.domain.ports.local.SalesInvoiceLocalPort
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity
import com.erpnext.pos.localSource.relations.v2.SalesInvoiceWithItemsAndPayments
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SalesInvoiceLocalAdapter(
    private val dao: SalesInvoiceDao
) : SalesInvoiceLocalPort {
    override suspend fun getPendingOutbox(
        instanceId: String,
        companyId: String
    ): List<SalesInvoiceWithItemsAndPayments> {
        return dao.getPendingOutbox(instanceId, companyId)
    }

    override suspend fun markOutboxSynced(
        instanceId: String,
        companyId: String,
        localInvoiceId: String,
        remoteName: String,
        remoteModified: String?
    ) {
        val now = Clock.System.now().toEpochMilliseconds()
        return dao.markSynced(
            instanceId = instanceId,
            companyId = companyId,
            localInvoiceId = localInvoiceId,
            remoteName = remoteName,
            remoteModified = remoteModified,
            syncedAd = now
        )
    }

    override suspend fun upsertFromServer(
        instanceId: String,
        companyId: String,
        invoices: List<SalesInvoiceEntity>
    ): Boolean {
        var changed = false
        val toUpsert = ArrayList<SalesInvoiceEntity>(invoices.size)

        for (inv in invoices) {
            val remoteName = inv.remoteName ?: inv.invoiceId
            val localModified = dao.getRemoteModified(instanceId, companyId, remoteName)

            if (localModified == null || localModified != inv.remoteModified) {
                toUpsert.add(inv)
                changed = true
            }
        }

        if (toUpsert.isNotEmpty())
            dao.upsertInvoices(toUpsert)

        return changed
    }

    override suspend fun insertInvoiceWithItemsAndPayments(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<SalesInvoicePaymentEntity>
    ) {
        dao.insertInvoiceWithItemsAndPayments(invoice, items, payments)
    }
}