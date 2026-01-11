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
    suspend fun updatePaymentStatus(invoiceId: String, status: String)
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
    suspend fun applyPayments(
        invoice: SalesInvoiceEntity,
        payments: List<POSInvoicePaymentEntity>
    )
    suspend fun getPendingPayments(): List<POSInvoicePaymentEntity>
    suspend fun updatePaymentSyncStatus(paymentId: Int, status: String, syncedAt: Long)
    suspend fun getPaymentsForInvoice(invoiceName: String): List<POSInvoicePaymentEntity>
    suspend fun refreshCustomerSummary(customerId: String)

    suspend fun getOutstandingInvoicesForCustomer(
        customerName: String
    ): List<SalesInvoiceWithItemsAndPayments>
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

    override suspend fun updatePaymentStatus(invoiceId: String, status: String) {
        salesInvoiceDao.updatePaymentStatus(invoiceId, status)
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

    override suspend fun applyPayments(
        invoice: SalesInvoiceEntity,
        payments: List<POSInvoicePaymentEntity>
    ) {
        salesInvoiceDao.applyPayments(invoice, payments)
    }

    override suspend fun getPendingPayments(): List<POSInvoicePaymentEntity> {
        return salesInvoiceDao.getPendingPayments()
    }

    override suspend fun updatePaymentSyncStatus(paymentId: Int, status: String, syncedAt: Long) {
        salesInvoiceDao.updatePaymentSyncStatus(paymentId, status, syncedAt)
    }

    override suspend fun getPaymentsForInvoice(invoiceName: String): List<POSInvoicePaymentEntity> {
        return salesInvoiceDao.getPaymentsForInvoice(invoiceName)
    }

    suspend fun updateFromRemote(
        oldName: String,
        newName: String,
        customerName: String?,
        customerPhone: String?,
        postingDate: String,
        dueDate: String?,
        currency: String,
        partyAccountCurrency: String?,
        conversionRate: Double?,
        customExchangeRate: Double?,
        netTotal: Double,
        taxTotal: Double,
        grandTotal: Double,
        paidAmount: Double,
        outstandingAmount: Double,
        status: String,
        docstatus: Int,
        modeOfPayment: String?,
        debitTo: String?,
        remarks: String?,
        syncStatus: String,
        modifiedAt: Long
    ) {
        salesInvoiceDao.updateFromRemote(
            oldName = oldName,
            newName = newName,
            customerName = customerName,
            customerPhone = customerPhone,
            postingDate = postingDate,
            dueDate = dueDate,
            currency = currency,
            partyAccountCurrency = partyAccountCurrency,
            conversionRate = conversionRate,
            customExchangeRate = customExchangeRate,
            netTotal = netTotal,
            taxTotal = taxTotal,
            grandTotal = grandTotal,
            paidAmount = paidAmount,
            outstandingAmount = outstandingAmount,
            status = status,
            docstatus = docstatus,
            modeOfPayment = modeOfPayment,
            debitTo = debitTo,
            remarks = remarks,
            syncStatus = syncStatus,
            modifiedAt = modifiedAt
        )
    }

    override suspend fun refreshCustomerSummary(customerId: String) {
        salesInvoiceDao.refreshCustomerSummary(customerId)
    }

    override suspend fun getOutstandingInvoicesForCustomer(
        customerName: String
    ): List<SalesInvoiceWithItemsAndPayments> {
        return salesInvoiceDao.getOutstandingInvoicesForCustomer(customerName)
    }
}
