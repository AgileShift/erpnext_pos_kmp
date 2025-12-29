package com.erpnext.pos.domain.repositories

import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.erpnext.pos.base.Resource
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.usecases.PendingInvoiceInput
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.dto.SalesInvoiceCreatedDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import kotlinx.coroutines.flow.Flow

interface ISaleInvoiceRepository {
    suspend fun getPendingInvoices(info: PendingInvoiceInput): Flow<PagingData<SalesInvoiceBO>>
    suspend fun getInvoiceDetail(invoiceId: String): SalesInvoiceBO

    suspend fun getAllLocalInvoicesPaged(): PagingSource<Int, SalesInvoiceWithItemsAndPayments>
    suspend fun getAllLocalInvoices() : List<SalesInvoiceWithItemsAndPayments>
    suspend fun getInvoiceByName(invoiceName: String): SalesInvoiceWithItemsAndPayments?
    suspend fun saveInvoiceLocally(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<POSInvoicePaymentEntity> = emptyList()
    )
    suspend fun applyLocalPayment(
        invoice: SalesInvoiceEntity,
        payments: List<POSInvoicePaymentEntity>
    )

    suspend fun markAsSynced(invoiceName: String)
    suspend fun markAsFailed(invoiceName: String)
    suspend fun getPendingSyncInvoices(): List<SalesInvoiceWithItemsAndPayments>
    suspend fun fetchRemoteInvoices(): List<SalesInvoiceDto>

    suspend fun createRemoteInvoice(invoice: SalesInvoiceDto): SalesInvoiceDto
    suspend fun updateRemoteInvoice(invoiceName: String, invoice: SalesInvoiceDto): SalesInvoiceDto
    suspend fun deleteRemoteInvoice(invoiceId: String)

    suspend fun syncPendingInvoices()

    suspend fun sync(): Flow<Resource<List<SalesInvoiceBO>>>

    suspend fun countPending(): Int
}
