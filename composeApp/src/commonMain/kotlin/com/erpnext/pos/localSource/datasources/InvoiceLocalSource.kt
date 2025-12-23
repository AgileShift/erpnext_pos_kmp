package com.erpnext.pos.localSource.datasources

import androidx.paging.PagingSource
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments

interface IInvoiceLocalSource {
    suspend fun getPendingInvoices(today: String): PagingSource<Int, SalesInvoiceWithItemsAndPayments>
    suspend fun getInvoiceDetail(invoiceId: String): SalesInvoiceWithItemsAndPayments?

    suspend fun getAllLocalInvoicesPaged(): PagingSource<Int, SalesInvoiceWithItemsAndPayments>
    suspend fun getAllLocalInvoices(): List<SalesInvoiceWithItemsAndPayments>
    suspend fun getInvoiceByName(invoiceName: String): SalesInvoiceWithItemsAndPayments?
    suspend fun saveInvoiceLocally(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<POSInvoicePaymentEntity> = emptyList()
    )

    suspend fun markAsSynced(invoiceName: String)
    suspend fun markAsFailed(invoiceName: String)
    suspend fun getPendingSyncInvoices(): List<SalesInvoiceWithItemsAndPayments>
    suspend fun countAllPendingSync(): Int
    suspend fun getOldestItem(): SalesInvoiceEntity?
    suspend fun deleteByInvoiceId(name: String)
}

class InvoiceLocalSource(
    private val salesInvoiceDao: SalesInvoiceDao
) : IInvoiceLocalSource {
    override suspend fun getPendingInvoices(today: String): PagingSource<Int, SalesInvoiceWithItemsAndPayments> =
        salesInvoiceDao.getOverdueInvoices(today)

    override suspend fun getInvoiceDetail(invoiceId: String): SalesInvoiceWithItemsAndPayments? {
        return salesInvoiceDao.getInvoiceByName(invoiceId)
    }

    override suspend fun getAllLocalInvoicesPaged(): PagingSource<Int, SalesInvoiceWithItemsAndPayments> =
        salesInvoiceDao.getAllInvoicesPaged()

    override suspend fun getAllLocalInvoices(): List<SalesInvoiceWithItemsAndPayments> =
        salesInvoiceDao.getAllInvoices()

    override suspend fun getInvoiceByName(invoiceName: String): SalesInvoiceWithItemsAndPayments? {
        return salesInvoiceDao.getInvoiceByName(invoiceName)
    }

    override suspend fun saveInvoiceLocally(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<POSInvoicePaymentEntity>
    ) {
        salesInvoiceDao.insertFullInvoice(
            invoice, items, payments
        )
    }

    override suspend fun markAsSynced(invoiceName: String) {
        return salesInvoiceDao.updateSyncStatus(invoiceName, "Synced")
    }

    override suspend fun markAsFailed(invoiceName: String) {
        return salesInvoiceDao.updateSyncStatus(invoiceName, "Failed")
    }

    override suspend fun getPendingSyncInvoices(): List<SalesInvoiceWithItemsAndPayments> {
        return salesInvoiceDao.getPendingSyncInvoices()
    }

    override suspend fun getOldestItem(): SalesInvoiceEntity? = salesInvoiceDao.getOldestItem()

    override suspend fun countAllPendingSync(): Int = salesInvoiceDao.countAllSyncPending()

    override suspend fun deleteByInvoiceId(name: String) = salesInvoiceDao.deleteByInvoiceId(name)
}