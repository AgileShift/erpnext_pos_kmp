package com.erpnext.pos.domain.ports.local

import com.erpnext.pos.localSource.entities.v2.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity
import com.erpnext.pos.localSource.relations.v2.SalesInvoiceWithItemsAndPayments

interface SalesInvoiceLocalPort {
    suspend fun getPendingOutbox(
        instanceId: String,
        companyId: String
    ): List<SalesInvoiceWithItemsAndPayments>

    suspend fun markOutboxSynced(
        instanceId: String,
        companyId: String,
        localInvoiceId: String,
        remoteName: String,
        remoteModified: String?
    )

    // Pull
    suspend fun upsertFromServer(
        instanceId: String,
        companyId: String,
        invoices: List<SalesInvoiceEntity>
    ) : Boolean

    // Escritura offline
    suspend fun insertInvoiceWithItemsAndPayments(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<SalesInvoicePaymentEntity>
    )
}