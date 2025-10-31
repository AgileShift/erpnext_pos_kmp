package com.erpnext.pos.localSource.datasources

import androidx.paging.PagingSource
import com.erpnext.pos.localSource.dao.PendingInvoiceDao
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.PendingSalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments

interface IInvoiceLocalSource {
    suspend fun getPendingInvoices(): PagingSource<Int, PendingSalesInvoiceEntity>
    suspend fun getInvoiceDetail(invoiceId: String): PagingSource<Int, PendingSalesInvoiceEntity>

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
}

class InvoiceLocalSource(
    private val salesInvoiceDao: SalesInvoiceDao,
    private val pendingInvoiceDao: PendingInvoiceDao
) : IInvoiceLocalSource {
    override suspend fun getPendingInvoices(): PagingSource<Int, PendingSalesInvoiceEntity> =
        pendingInvoiceDao.getAll()

    override suspend fun getInvoiceDetail(invoiceId: String): PagingSource<Int, PendingSalesInvoiceEntity> {
        return pendingInvoiceDao.getInvoiceDetails(invoiceId)
    }

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
}