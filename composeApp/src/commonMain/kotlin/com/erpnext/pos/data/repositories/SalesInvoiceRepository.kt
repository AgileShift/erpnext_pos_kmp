package com.erpnext.pos.data.repositories

import androidx.paging.PagingData
import androidx.paging.PagingSource
import com.erpnext.pos.base.Resource
import com.erpnext.pos.base.networkBoundResource
import com.erpnext.pos.data.mappers.toBO
import com.erpnext.pos.data.mappers.toPagingBO
import com.erpnext.pos.domain.models.SalesInvoiceBO
import com.erpnext.pos.domain.repositories.ISaleInvoiceRepository
import com.erpnext.pos.domain.usecases.PendingInvoiceInput
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.localSource.entities.*
import com.erpnext.pos.remoteSource.datasources.SalesInvoiceRemoteSource
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.mapper.toDto
import com.erpnext.pos.remoteSource.mapper.toEntities
import com.erpnext.pos.sync.SyncTTL
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class SalesInvoiceRepository(
    private val remoteSource: SalesInvoiceRemoteSource,
    private val localSource: InvoiceLocalSource,
    private val context: CashBoxManager
) : ISaleInvoiceRepository {
    override suspend fun getPendingInvoices(info: PendingInvoiceInput): Flow<PagingData<SalesInvoiceBO>> {
        val pos = context.requireContext().profileName
        return remoteSource.getAllInvoices(pos, info.query, info.date).toPagingBO()
    }

    override suspend fun getInvoiceDetail(invoiceId: String): SalesInvoiceBO {
        val invoices = localSource.getInvoiceByName(invoiceId)?.invoice
            ?: throw IllegalArgumentException("Invoice not found locally: $invoiceId")
        return invoices.toBO()
    }

    override suspend fun getAllLocalInvoicesPaged(): PagingSource<Int, SalesInvoiceWithItemsAndPayments> {
        return localSource.getAllLocalInvoicesPaged()
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
        localSource.refreshCustomerSummary(invoice.customer)
    }

    //TODO:
    @OptIn(ExperimentalTime::class)
    override suspend fun applyLocalPayment(
        invoice: SalesInvoiceEntity,
        payments: List<POSInvoicePaymentEntity>
    ) {
        val invoiceId = requireNotNull(invoice.invoiceName) {
            "Invoice name is required to apply payments."
        }
        val existingPayments = localSource.getInvoiceByName(invoiceId)?.payments ?: emptyList()
        val totalPaid = existingPayments.sumOf { it.amount } + payments.sumOf { it.amount }
        val grandTotal = invoice.grandTotal
        val newOutstanding = (grandTotal - totalPaid).coerceAtLeast(0.0)
        val epsilon = 0.0001

        invoice.outstandingAmount = newOutstanding
        invoice.paidAmount = totalPaid.coerceAtMost(grandTotal)
        invoice.status = when {
            newOutstanding <= epsilon -> "Paid"
            newOutstanding >= grandTotal - epsilon -> "Unpaid"
            else -> "Partly Paid"
        }
        invoice.syncStatus = "Pending"
        invoice.modifiedAt = Clock.System.now().toEpochMilliseconds()
        payments.lastOrNull()?.modeOfPayment?.let { invoice.modeOfPayment = it }

        localSource.applyPayments(invoice, payments)
        localSource.refreshCustomerSummary(invoice.customer)
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

    override suspend fun fetchRemoteInvoices(): List<SalesInvoiceDto> {
        val posProfile = context.requireContext().profileName
        return remoteSource.fetchInvoices(posProfile)
        /*return localSource.getAllLocalInvoices()
            ?: throw IllegalArgumentException("Invoice not found after fetch: $name")*/
    }

    suspend fun fetchRemoteInvoice(name: String): SalesInvoiceDto? {
        return remoteSource.fetchInvoice(name)
    }

    override suspend fun createRemoteInvoice(invoice: SalesInvoiceDto): SalesInvoiceDto {
        val created = remoteSource.createInvoice(invoice)
        return remoteSource.fetchInvoice(created.name!!)!!
    }

    override suspend fun updateRemoteInvoice(
        invoiceName: String, invoice: SalesInvoiceDto
    ): SalesInvoiceDto {
        return remoteSource.updateInvoice(invoiceName, invoice)
    }

    override suspend fun deleteRemoteInvoice(invoiceId: String) {
        remoteSource.deleteInvoice(invoiceId)
        localSource.deleteByInvoiceId(invoiceId)
    }

    override suspend fun syncPendingInvoices() {
        val pending = getPendingSyncInvoices()
        pending.forEach { invoice ->
            try {
                val dto = invoice.toDto()
                createRemoteInvoice(dto)
                markAsSynced(invoice.invoice.invoiceName ?: "")
            } catch (_: Exception) {
                markAsFailed(invoice.invoice.invoiceName ?: "")
            }
        }
    }

    override suspend fun sync(): Flow<Resource<List<SalesInvoiceBO>>> {
        return networkBoundResource(
            query = { flowOf(localSource.getAllLocalInvoices().toBO()) },
            fetch = { remoteSource.fetchInvoices(context.requireContext().profileName) },
            shouldFetch = {
                val first = localSource.getOldestItem()
                first == null || SyncTTL.isExpired(first.lastSyncedAt)
            },
            saveFetchResult = { it ->
                it.toEntities().map {
                    localSource.saveInvoiceLocally(it.invoice, it.items, it.payments)
                }
            },
            onFetchFailed = { e -> e.printStackTrace() }
        )
    }

    override suspend fun countPending(): Int = localSource.countAllPendingSync()

    suspend fun getOutstandingInvoicesForCustomer(customerName: String): List<SalesInvoiceBO> {
        return localSource.getOutstandingInvoicesForCustomer(customerName).toBO()
    }
}
