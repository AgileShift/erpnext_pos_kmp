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
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.RepoTrace
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.roundToCurrency
import com.erpnext.pos.views.CashBoxManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class SalesInvoiceRepository(
    private val remoteSource: SalesInvoiceRemoteSource,
    private val localSource: InvoiceLocalSource,
    private val context: CashBoxManager
) : ISaleInvoiceRepository {
    override suspend fun getPendingInvoices(info: PendingInvoiceInput): Flow<PagingData<SalesInvoiceBO>> {
        RepoTrace.breadcrumb("SalesInvoiceRepository", "getPendingInvoices")
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
        RepoTrace.breadcrumb("SalesInvoiceRepository", "saveInvoiceLocally", invoice.invoiceName)
        if (invoice.invoiceName.isNullOrBlank()) {
            AppLogger.warn("SalesInvoiceRepository: invoice_name vacío; se omite guardar items/pagos.")
            return
        }
        localSource.saveInvoiceLocally(invoice, items, payments)
        localSource.refreshCustomerSummary(invoice.customer)
    }

    suspend fun saveInvoiceLocallyPending(
        localInvoiceName: String,
        dto: SalesInvoiceDto
    ) {
        val entity = dto.toEntity()
        val now = Clock.System.now().toEpochMilliseconds()

        entity.invoice.invoiceName = localInvoiceName
        entity.invoice.syncStatus = "Pending"
        entity.invoice.status = dto.status ?: entity.invoice.status
        entity.invoice.modifiedAt = now
        val invoiceCurrency = dto.currency?.trim()?.uppercase()
        val receivableCurrency = dto.partyAccountCurrency?.trim()?.uppercase()
        val providedRate = dto.conversionRate
            ?: dto.customExchangeRate
        if (!invoiceCurrency.isNullOrBlank() &&
            !receivableCurrency.isNullOrBlank() &&
            !invoiceCurrency.equals(receivableCurrency, ignoreCase = true)
        ) {
            val directRate = context.resolveExchangeRateBetween(invoiceCurrency, receivableCurrency)
            val fallbackRate = if (directRate == null || directRate <= 0.0) {
                val ctx = context.getContext()
                when {
                    receivableCurrency.equals("USD", true) &&
                        invoiceCurrency.equals(ctx?.currency, true) &&
                        (ctx?.exchangeRate ?: 0.0) > 0.0 ->
                        1 / (ctx?.exchangeRate ?: 1.0)
                    invoiceCurrency.equals("USD", true) &&
                        receivableCurrency.equals(ctx?.currency, true) &&
                        (ctx?.exchangeRate ?: 0.0) > 0.0 ->
                        ctx?.exchangeRate
                    else -> null
                }
            } else {
                directRate
            }
            if (fallbackRate != null && fallbackRate > 0.0) {
                entity.invoice.outstandingAmount =
                    roundToCurrency(entity.invoice.outstandingAmount * fallbackRate)
                entity.invoice.paidAmount = roundToCurrency(entity.invoice.paidAmount * fallbackRate)
                entity.invoice.conversionRate = providedRate ?: fallbackRate
                entity.invoice.customExchangeRate = dto.customExchangeRate ?: fallbackRate
            }
        }
        if (entity.invoice.conversionRate == null && providedRate != null && providedRate > 0.0) {
            entity.invoice.conversionRate = providedRate
        }
        if (entity.invoice.customExchangeRate == null &&
            dto.customExchangeRate != null &&
            dto.customExchangeRate > 0.0
        ) {
            entity.invoice.customExchangeRate = dto.customExchangeRate
        }
        entity.items.forEach { it.parentInvoice = localInvoiceName }
        entity.payments.forEach { it.parentInvoice = localInvoiceName }

        saveInvoiceLocally(entity.invoice, entity.items, entity.payments)
    }

    //TODO:
    @OptIn(ExperimentalTime::class)
    override suspend fun applyLocalPayment(
        invoice: SalesInvoiceEntity,
        payments: List<POSInvoicePaymentEntity>
    ) {
        RepoTrace.breadcrumb("SalesInvoiceRepository", "applyLocalPayment", invoice.invoiceName)
        val invoiceId = requireNotNull(invoice.invoiceName) {
            "Invoice name is required to apply payments."
        }
        val existingRefs = localSource.getPaymentsForInvoice(invoiceId)
            .mapNotNull { it.paymentReference?.trim()?.uppercase() }
            .toSet()
        val uniquePayments = payments.filter { payment ->
            val ref = payment.paymentReference?.trim()?.uppercase()
            ref.isNullOrBlank() || !existingRefs.contains(ref)
        }
        if (uniquePayments.isEmpty()) return

        val paidDelta = uniquePayments.sumOf { it.amount }
        val totalBefore = invoice.paidAmount + invoice.outstandingAmount
        val totalPaid = roundToCurrency((invoice.paidAmount + paidDelta).coerceAtLeast(0.0))
        var newOutstanding =
            roundToCurrency((invoice.outstandingAmount - paidDelta).coerceAtLeast(0.0))
        val roundingTolerance = 0.05
        val epsilon = 0.0001
        if (newOutstanding <= roundingTolerance) {
            newOutstanding = 0.0
        }

        invoice.outstandingAmount = newOutstanding
        invoice.paidAmount = totalPaid.coerceAtMost(totalBefore)
        invoice.status = when {
            newOutstanding <= 0.0 -> "Paid"
            totalPaid <= epsilon -> "Unpaid"
            else -> "Partly Paid"
        }
        invoice.syncStatus = "Pending"
        invoice.modifiedAt = Clock.System.now().toEpochMilliseconds()
        uniquePayments.lastOrNull()?.modeOfPayment?.let { invoice.modeOfPayment = it }

        localSource.applyPayments(invoice, uniquePayments)
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

    suspend fun refreshInvoiceFromRemote(invoiceName: String): SalesInvoiceWithItemsAndPayments? {
        val remote = fetchRemoteInvoice(invoiceName) ?: return null
        val entity = remote.toEntity()
        saveInvoiceLocally(entity.invoice, entity.items, entity.payments)
        return entity
    }

    @OptIn(ExperimentalTime::class)
    suspend fun updateLocalInvoiceFromRemote(
        localInvoiceName: String,
        remote: SalesInvoiceDto
    ) {
        val remoteName = remote.name ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        localSource.updateFromRemote(
            oldName = localInvoiceName,
            newName = remoteName,
            customerName = remote.customerName,
            customerPhone = remote.customerPhone,
            postingDate = remote.postingDate,
            dueDate = remote.dueDate,
            currency = remote.currency ?: "NIO",
            partyAccountCurrency = remote.partyAccountCurrency,
            conversionRate = remote.conversionRate ?: remote.customExchangeRate,
            customExchangeRate = remote.customExchangeRate,
            netTotal = remote.netTotal,
            taxTotal = remote.totalTaxesAndCharges ?: 0.0,
            grandTotal = remote.grandTotal,
            paidAmount = remote.paidAmount,
            outstandingAmount = remote.outstandingAmount ?: 0.0,
            status = remote.status ?: "Draft",
            docstatus = remote.docStatus,
            modeOfPayment = remote.payments.firstOrNull()?.modeOfPayment,
            debitTo = remote.debitTo,
            remarks = remote.remarks,
            syncStatus = "Synced",
            modifiedAt = now
        )
        localSource.refreshCustomerSummary(remote.customer)
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
        RepoTrace.breadcrumb("SalesInvoiceRepository", "syncPendingInvoices")
        val pending = getPendingSyncInvoices()
        pending.forEach { invoice ->
            try {
                val dto = invoice.toDto()
                val created = createRemoteInvoice(dto)
                val localName = invoice.invoice.invoiceName ?: ""
                if (created.name != null && created.name != localName) {
                    updateLocalInvoiceFromRemote(localName, created)
                } else {
                    markAsSynced(localName)
                }
            } catch (e: Exception) {
                RepoTrace.capture("SalesInvoiceRepository", "syncPendingInvoices", e)
                markAsFailed(invoice.invoice.invoiceName ?: "")
            }
        }
    }

    override suspend fun sync(): Flow<Resource<List<SalesInvoiceBO>>> {
        RepoTrace.breadcrumb("SalesInvoiceRepository", "sync")
        return networkBoundResource(
            query = { flowOf(localSource.getAllLocalInvoices().toBO()) },
            fetch = { remoteSource.fetchInvoices(context.requireContext().profileName) },
            shouldFetch = {
                true
                /*val first = localSource.getOldestItem()
                first == null || SyncTTL.isExpired(first.lastSyncedAt)*/
            },
            saveFetchResult = { it ->
                it.toEntities().map {
                    localSource.saveInvoiceLocally(it.invoice, it.items, it.payments)
                }
            },
            onFetchFailed = { e ->
                RepoTrace.capture("SalesInvoiceRepository", "sync", e)
                e.printStackTrace()
            }
        )
    }

    override suspend fun countPending(): Int = localSource.countAllPendingSync()

    suspend fun getOutstandingInvoicesForCustomer(customerName: String): List<SalesInvoiceBO> {
        val remoteInvoices = runCatching {
            remoteSource.fetchOutstandingInvoicesForCustomer(customerName)
        }.getOrNull()

        if (!remoteInvoices.isNullOrEmpty()) {
            val entities = remoteInvoices.toEntities()
            entities.forEach { payload ->
                saveInvoiceLocally(payload.invoice, payload.items, payload.payments)
            }
        }

        return localSource.getOutstandingInvoicesForCustomer(customerName).toBO()
    }
}
