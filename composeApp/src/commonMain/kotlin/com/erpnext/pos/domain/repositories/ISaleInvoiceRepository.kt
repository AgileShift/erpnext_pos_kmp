package com.erpnext.pos.domain.repositories

import androidx.paging.PagingData
import com.erpnext.pos.domain.models.PendingInvoiceBO
import com.erpnext.pos.domain.usecases.PendingInvoiceInput
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import kotlinx.coroutines.flow.Flow

interface ISaleInvoiceRepository {
    suspend fun getPendingInvoices(info: PendingInvoiceInput): Flow<PagingData<PendingInvoiceBO>>
    suspend fun getInvoiceDetail(invoiceId: String): PendingInvoiceBO

    suspend fun getAllLocalInvoices(): List<SalesInvoiceWithItemsAndPayments>
    suspend fun getInvoiceByName(invoiceName: String) : SalesInvoiceWithItemsAndPayments?
    suspend fun saveInvoiceLocally(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<POSInvoicePaymentEntity> = emptyList()
    )
    suspend fun markAsSynced(invoiceName: String)
    suspend fun markAsFailed(invoiceName: String)
    suspend fun getPendingSyncInvoices() : List<SalesInvoiceWithItemsAndPayments>
    suspend fun fetchRemoteInvoices(limit: Int = 50, offset: Int= 0)
    suspend fun fetchRemoteInvoices(name: String): SalesInvoiceWithItemsAndPayments

    suspend fun createRemoteInvoice(invoice: SalesInvoiceDto) : SalesInvoiceDto
    suspend fun updateRemoteInvoice(invoiceName: String, invoice: SalesInvoiceDto): SalesInvoiceDto
    suspend fun deleteRemoteInvoice(invoiceName: String)

    suspend fun syncPendingInvoices()

}