package com.erpnext.pos.data.repositories

import androidx.paging.PagingData
import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.data.mappers.toPagingBO
import com.erpnext.pos.domain.models.PendingInvoiceBO
import com.erpnext.pos.domain.repositories.ISaleInvoiceRepository
import com.erpnext.pos.domain.usecases.PendingInvoiceInput
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.localSource.entities.*
import com.erpnext.pos.remoteSource.datasources.SalesInvoiceRemoteSource
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.mapper.toDto
import kotlinx.coroutines.flow.Flow

class SalesInvoiceRepository(
    private val remoteSource: SalesInvoiceRemoteSource,
    private val localSource: InvoiceLocalSource,
) : ISaleInvoiceRepository {

    override suspend fun getPendingInvoices(info: PendingInvoiceInput): Flow<PagingData<PendingInvoiceBO>> {
        return remoteSource.getAllInvoices(info.pos, info.query, info.date).toPagingBO()
    }

    override suspend fun getInvoiceDetail(invoiceId: String): PendingInvoiceBO {
        val invoices = localSource.getInvoiceByName(invoiceId)?.invoice
            ?: throw IllegalArgumentException("Invoice not found locally: $invoiceId")
        return invoices.toBO()
    }

    override suspend fun getAllLocalInvoices(): List<SalesInvoiceWithItemsAndPayments> {
        return localSource.getAllLocalInvoices()
    }

    override suspend fun getInvoiceByName(invoiceName: String): SalesInvoiceWithItemsAndPayments? {
        return localSource.getInvoiceByName(invoiceName)
    }

    override suspend fun saveInvoiceLocally(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<POSInvoicePaymentEntity>
    ) {
        localSource.saveInvoiceLocally(invoice, items, payments)
    }

    override suspend fun markAsSynced(invoiceName: String) {
        localSource.markAsSynced(invoiceName)
    }

    override suspend fun markAsFailed(invoiceName: String) {
        localSource.markAsFailed(invoiceName)
    }

    override suspend fun getPendingSyncInvoices(): List<SalesInvoiceWithItemsAndPayments> {
        return localSource.getPendingSyncInvoices()
    }

    override suspend fun fetchRemoteInvoices(limit: Int, offset: Int) {
        remoteSource.fetchInvoices(
            limit = limit,
            offset = offset,
        )
    }

    override suspend fun fetchRemoteInvoices(name: String): SalesInvoiceWithItemsAndPayments {
        remoteSource.fetchInvoice(name)
        return localSource.getInvoiceByName(name)
            ?: throw IllegalArgumentException("Invoice not found after fetch: $name")
    }

    override suspend fun createRemoteInvoice(invoice: SalesInvoiceDto): SalesInvoiceDto {
        return remoteSource.createInvoice(invoice)
    }

    override suspend fun updateRemoteInvoice(
        invoiceName: String, invoice: SalesInvoiceDto
    ): SalesInvoiceDto {
        return remoteSource.updateInvoice(invoiceName, invoice)
    }

    override suspend fun deleteRemoteInvoice(invoiceName: String) {
        remoteSource.deleteInvoice(invoiceName)
        //localSource.deleteInvoiceByName(invoiceName)
    }

    override suspend fun syncPendingInvoices() {
        val pending = getPendingSyncInvoices()
        pending.forEach { invoice ->
            try {
                val dto = invoice.toDto()
                createRemoteInvoice(dto)
                markAsSynced(invoice.invoice.invoiceName ?: "")
            } catch (e: Exception) {
                markAsFailed(invoice.invoice.invoiceName ?: "")
            }
        }
    }
}
