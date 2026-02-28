package com.erpnext.pos.localSource.datasources

import androidx.paging.PagingSource
import com.erpnext.pos.localSource.dao.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceItemEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments

interface IInvoiceLocalSource {
    fun getPendingInvoices(today: String): PagingSource<Int, SalesInvoiceWithItemsAndPayments>
    suspend fun getInvoiceDetail(invoiceId: String): SalesInvoiceWithItemsAndPayments?

    fun getAllLocalInvoicesPaged(): PagingSource<Int, SalesInvoiceWithItemsAndPayments>
    suspend fun getAllLocalInvoices(): List<SalesInvoiceWithItemsAndPayments>
    suspend fun getInvoiceByName(invoiceName: String): SalesInvoiceWithItemsAndPayments?
    suspend fun updatePaymentStatus(invoiceId: String, status: String)
    suspend fun saveInvoiceLocally(
        invoice: SalesInvoiceEntity,
        items: List<SalesInvoiceItemEntity>,
        payments: List<POSInvoicePaymentEntity> = emptyList()
    )
    suspend fun updateInvoice(invoice: SalesInvoiceEntity)

    suspend fun markAsSynced(invoiceName: String)
    suspend fun markAsFailed(invoiceName: String)
    suspend fun getPendingSyncInvoices(): List<SalesInvoiceWithItemsAndPayments>
    suspend fun countAllPendingSync(): Int
    suspend fun countAllInvoices(): Int
    suspend fun getOldestItem(): SalesInvoiceEntity?
    suspend fun deleteByInvoiceId(name: String)
    suspend fun softDeleteByInvoiceId(name: String)
    suspend fun softDeleteMissingForProfile(profileId: String, invoiceNames: List<String>)
    suspend fun softDeleteMissingRemoteInvoices(invoiceNames: List<String>)
    suspend fun applyPayments(
        invoice: SalesInvoiceEntity,
        payments: List<POSInvoicePaymentEntity>
    )
    suspend fun getPendingPayments(): List<POSInvoicePaymentEntity>
    suspend fun updatePaymentSyncStatus(
        paymentId: Int,
        status: String,
        syncedAt: Long,
        remotePaymentEntry: String? = null
    )
    suspend fun deleteRemotePaymentsByRemoteEntries(remotePaymentEntries: List<String>)
    suspend fun upsertPayments(payments: List<POSInvoicePaymentEntity>)
    suspend fun getInvoiceNamesMissingItems(profileId: String, limit: Int = 50): List<String>
    suspend fun getPaymentsForInvoice(invoiceName: String): List<POSInvoicePaymentEntity>
    suspend fun refreshCustomerSummary(customerId: String)
    suspend fun getOutstandingInvoiceNamesForProfile(profileId: String): List<String>

    suspend fun getOutstandingInvoicesForCustomer(
        customerName: String
    ): List<SalesInvoiceWithItemsAndPayments>
    fun getOutstandingInvoicesForCustomerPaged(
        customerName: String
    ): PagingSource<Int, SalesInvoiceWithItemsAndPayments>
    fun getInvoicesForCustomerInRangePaged(
        customerName: String,
        startDate: String,
        endDate: String
    ): PagingSource<Int, SalesInvoiceWithItemsAndPayments>
}

class InvoiceLocalSource(
    private val salesInvoiceDao: SalesInvoiceDao
) : IInvoiceLocalSource {
    override fun getPendingInvoices(today: String): PagingSource<Int, SalesInvoiceWithItemsAndPayments> =
        salesInvoiceDao.getOverdueInvoices(today)

    override suspend fun getInvoiceDetail(invoiceId: String): SalesInvoiceWithItemsAndPayments? {
        return salesInvoiceDao.getInvoiceByName(invoiceId)
    }

    override fun getAllLocalInvoicesPaged(): PagingSource<Int, SalesInvoiceWithItemsAndPayments> =
        salesInvoiceDao.getAllInvoicesPaged()

    override suspend fun getAllLocalInvoices(): List<SalesInvoiceWithItemsAndPayments> =
        salesInvoiceDao.getAllInvoices()

    override suspend fun getInvoiceByName(invoiceName: String): SalesInvoiceWithItemsAndPayments? {
        return salesInvoiceDao.getInvoiceByName(invoiceName)
    }

    suspend fun getInvoiceByNameAny(invoiceName: String): SalesInvoiceWithItemsAndPayments? {
        return salesInvoiceDao.getInvoiceByNameAny(invoiceName)
    }

    suspend fun findRecentDebitTo(
        company: String,
        customer: String?,
        partyAccountCurrency: String?
    ): String? {
        val normalizedCustomer = customer?.trim()?.takeIf { it.isNotBlank() }
        val normalizedCurrency = partyAccountCurrency?.trim()?.uppercase()?.takeIf { it.isNotBlank() }

        return salesInvoiceDao.findLatestDebitTo(
            company = company,
            customer = normalizedCustomer,
            partyAccountCurrency = normalizedCurrency
        )?.takeIf { it.isNotBlank() }
            ?: salesInvoiceDao.findLatestDebitTo(
                company = company,
                customer = normalizedCustomer,
                partyAccountCurrency = null
            )?.takeIf { it.isNotBlank() }
            ?: salesInvoiceDao.findLatestDebitTo(
                company = company,
                customer = null,
                partyAccountCurrency = normalizedCurrency
            )?.takeIf { it.isNotBlank() }
            ?: salesInvoiceDao.findLatestDebitTo(
                company = company,
                customer = null,
                partyAccountCurrency = null
            )?.takeIf { it.isNotBlank() }
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

    override suspend fun updateInvoice(invoice: SalesInvoiceEntity) {
        salesInvoiceDao.updateInvoice(invoice)
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

    override suspend fun countAllInvoices(): Int = salesInvoiceDao.countAll()

    override suspend fun deleteByInvoiceId(name: String) =
        salesInvoiceDao.deleteInvoiceWithChildren(name)

    override suspend fun softDeleteByInvoiceId(name: String) {
        salesInvoiceDao.hardDeleteDeletedByInvoiceId(name)
        salesInvoiceDao.softDeleteByInvoiceId(name)
    }

    override suspend fun softDeleteMissingForProfile(
        profileId: String,
        invoiceNames: List<String>
    ) {
        val names = invoiceNames.ifEmpty { listOf("__empty__") }
        salesInvoiceDao.hardDeleteDeletedNotInForProfile(profileId, names)
        salesInvoiceDao.softDeleteNotInForProfile(profileId, names)
    }

    override suspend fun softDeleteMissingRemoteInvoices(invoiceNames: List<String>) {
        val names = invoiceNames.ifEmpty { listOf("__empty__") }
        salesInvoiceDao.hardDeleteDeletedNotInRemote(names)
        salesInvoiceDao.softDeleteNotInRemote(names)
    }

    override suspend fun applyPayments(
        invoice: SalesInvoiceEntity,
        payments: List<POSInvoicePaymentEntity>
    ) {
        salesInvoiceDao.applyPayments(invoice, payments)
    }

    override suspend fun getPendingPayments(): List<POSInvoicePaymentEntity> {
        return salesInvoiceDao.getPendingPayments()
    }

    override suspend fun updatePaymentSyncStatus(
        paymentId: Int,
        status: String,
        syncedAt: Long,
        remotePaymentEntry: String?
    ) {
        salesInvoiceDao.updatePaymentSyncStatus(paymentId, status, syncedAt, remotePaymentEntry)
    }

    override suspend fun deleteRemotePaymentsByRemoteEntries(remotePaymentEntries: List<String>) {
        if (remotePaymentEntries.isEmpty()) return
        salesInvoiceDao.deleteByRemotePaymentEntries(remotePaymentEntries.distinct())
    }

    override suspend fun upsertPayments(payments: List<POSInvoicePaymentEntity>) {
        if (payments.isEmpty()) return
        salesInvoiceDao.insertPayments(payments)
    }

    override suspend fun getInvoiceNamesMissingItems(
        profileId: String,
        limit: Int
    ): List<String> {
        return salesInvoiceDao.getInvoiceNamesMissingItems(profileId, limit)
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
        discountAmount: Double,
        grandTotal: Double,
        paidAmount: Double,
        outstandingAmount: Double,
        baseTotal: Double?,
        baseNetTotal: Double?,
        baseTotalTaxesAndCharges: Double?,
        baseGrandTotal: Double?,
        baseRoundingAdjustment: Double?,
        baseRoundedTotal: Double?,
        baseDiscountAmount: Double?,
        basePaidAmount: Double?,
        baseChangeAmount: Double?,
        baseWriteOffAmount: Double?,
        status: String,
        docstatus: Int,
        modeOfPayment: String?,
        debitTo: String?,
        remarks: String?,
        posOpeningEntry: String?,
        isReturn: Boolean,
        isPos: Boolean,
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
            discountAmount = discountAmount,
            grandTotal = grandTotal,
            paidAmount = paidAmount,
            outstandingAmount = outstandingAmount,
            baseTotal = baseTotal,
            baseNetTotal = baseNetTotal,
            baseTotalTaxesAndCharges = baseTotalTaxesAndCharges,
            baseGrandTotal = baseGrandTotal,
            baseRoundingAdjustment = baseRoundingAdjustment,
            baseRoundedTotal = baseRoundedTotal,
            baseDiscountAmount = baseDiscountAmount,
            basePaidAmount = basePaidAmount,
            baseChangeAmount = baseChangeAmount,
            baseWriteOffAmount = baseWriteOffAmount,
            status = status,
            docstatus = docstatus,
            modeOfPayment = modeOfPayment,
            debitTo = debitTo,
            remarks = remarks,
            posOpeningEntry = posOpeningEntry,
            isReturn = isReturn,
            isPos = isPos,
            syncStatus = syncStatus,
            modifiedAt = modifiedAt
        )
    }

    suspend fun rebindChildrenToInvoice(
        oldInvoiceName: String,
        newInvoiceName: String
    ) {
        salesInvoiceDao.rebindChildrenToInvoice(oldInvoiceName, newInvoiceName)
    }

    override suspend fun refreshCustomerSummary(customerId: String) {
        salesInvoiceDao.refreshCustomerSummary(customerId)
    }

    override suspend fun getOutstandingInvoiceNamesForProfile(profileId: String): List<String> {
        return salesInvoiceDao.getOutstandingInvoiceNamesForProfile(profileId)
    }

    override suspend fun getOutstandingInvoicesForCustomer(
        customerName: String
    ): List<SalesInvoiceWithItemsAndPayments> {
        return salesInvoiceDao.getOutstandingInvoicesForCustomer(customerName)
    }

    override fun getOutstandingInvoicesForCustomerPaged(
        customerName: String
    ): PagingSource<Int, SalesInvoiceWithItemsAndPayments> {
        return salesInvoiceDao.getOutstandingInvoicesForCustomerPaged(customerName)
    }

    override fun getInvoicesForCustomerInRangePaged(
        customerName: String,
        startDate: String,
        endDate: String
    ): PagingSource<Int, SalesInvoiceWithItemsAndPayments> {
        return salesInvoiceDao.getInvoicesForCustomerInRangePaged(customerName, startDate, endDate)
    }
}
