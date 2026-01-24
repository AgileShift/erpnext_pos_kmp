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
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.datasources.InvoiceLocalSource
import com.erpnext.pos.localSource.dao.CustomerDao
import com.erpnext.pos.localSource.entities.*
import com.erpnext.pos.remoteSource.datasources.SalesInvoiceRemoteSource
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.mapper.toDto
import com.erpnext.pos.remoteSource.mapper.toEntities
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.utils.RepoTrace
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.buildPaymentModeDetailMap
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
    private val context: CashBoxManager,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val customerDao: CustomerDao
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
        refreshCustomerSummaryWithRates(invoice.customer)
    }

    suspend fun saveInvoiceLocallyPending(
        localInvoiceName: String,
        dto: SalesInvoiceDto
    ) {
        val entity = dto.toEntity()
        val now = Clock.System.now().toEpochMilliseconds()
        val openingEntryId = context.getActiveCashboxWithDetails()?.cashbox?.openingEntryId
        val profileId = dto.posProfile?.takeIf { it.isNotBlank() }
            ?: context.getContext()?.profileName

        entity.invoice.invoiceName = localInvoiceName
        entity.invoice.syncStatus = "Pending"
        entity.invoice.status = dto.status ?: entity.invoice.status
        entity.invoice.modifiedAt = now
        entity.invoice.profileId = profileId
        entity.invoice.posOpeningEntry = openingEntryId
        entity.payments.forEach { it.posOpeningEntry = openingEntryId }
        if (entity.invoice.profileId.isNullOrBlank() || entity.invoice.posOpeningEntry.isNullOrBlank()) {
            throw IllegalStateException("Falta POS Profile o apertura de caja activa para crear la factura.")
        }
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
        refreshCustomerSummaryWithRates(invoice.customer)
    }

    override suspend fun cancelInvoice(invoiceName: String, isReturn: Boolean) {
        val current = localSource.getInvoiceByName(invoiceName)?.invoice ?: return
        current.status = if (isReturn) "Return" else "Cancelled"
        current.docstatus = 2
        current.isReturn = isReturn
        current.syncStatus = "Pending"
        current.modifiedAt = Clock.System.now().toEpochMilliseconds()
        localSource.updateInvoice(current)
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
        val local = localSource.getInvoiceByName(name)?.invoice
        return if (local?.isPos == true) {
            remoteSource.fetchPosInvoice(name)
        } else {
            remoteSource.fetchInvoice(name)
        }
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
        // Se lee la factura local para no perder pagos locales en modo offline-first.
        val localInvoice = localSource.getInvoiceByName(localInvoiceName)?.invoice
        val remoteName = remote.name ?: return
        val now = Clock.System.now().toEpochMilliseconds()
        // Se valida si el servidor trae montos pagados reales o si aún están en cero.
        val remoteOutstanding = remote.outstandingAmount
        val remotePaid = remote.paidAmount
        val remoteTotal = remote.grandTotal
        val localPaid = localInvoice?.paidAmount ?: 0.0
        val localOutstanding = localInvoice?.outstandingAmount ?: 0.0
        val remoteHasPayments =
            remotePaid > 0.0 || (remoteOutstanding != null && remoteOutstanding < remoteTotal - 0.01)
        // Si el servidor aún no refleja pagos, preservamos lo local.
        val resolvedPaidAmount = if (remoteHasPayments) remotePaid else localPaid
        val resolvedOutstandingAmount =
            if (remoteHasPayments) (remoteOutstanding ?: 0.0) else localOutstanding
        // El status se alinea con la fuente de verdad escogida.
        val resolvedStatus = if (remoteHasPayments) remote.status ?: "Draft"
        else localInvoice?.status ?: (remote.status ?: "Draft")
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
            paidAmount = resolvedPaidAmount,
            outstandingAmount = resolvedOutstandingAmount,
            status = resolvedStatus,
            docstatus = remote.docStatus ?: resolveDocStatus(remote.status, null),
            modeOfPayment = remote.payments.firstOrNull()?.modeOfPayment,
            debitTo = remote.debitTo,
            remarks = remote.remarks,
            posOpeningEntry = remote.posOpeningEntry ?: localInvoice?.posOpeningEntry,
            isReturn = remote.isReturn == 1,
            isPos = remote.doctype.equals("POS Invoice", true) || remote.isPos,
            syncStatus = "Synced",
            modifiedAt = now
        )
        refreshCustomerSummaryWithRates(remote.customer)
    }

    override suspend fun createRemoteInvoice(invoice: SalesInvoiceDto): SalesInvoiceDto {
        val draft = ensureDraftDocStatus(enrichPaymentsWithAccount(invoice))
        val created = if (isPosInvoice(draft)) {
            remoteSource.createPosInvoice(draft)
        } else {
            remoteSource.createInvoice(draft)
        }
        val createdName = created.name!!
        if (isPosInvoice(draft)) {
            remoteSource.submitPosInvoice(createdName)
            return remoteSource.fetchPosInvoice(createdName)!!
        }
        submitSalesInvoice(createdName)
        return remoteSource.fetchInvoice(createdName)!!
    }

    override suspend fun updateRemoteInvoice(
        invoiceName: String, invoice: SalesInvoiceDto
    ): SalesInvoiceDto {
        return if (isPosInvoice(invoice)) {
            remoteSource.updatePosInvoice(invoiceName, invoice)
        } else {
            remoteSource.updateInvoice(invoiceName, invoice)
        }
    }

    override suspend fun deleteRemoteInvoice(invoiceId: String) {
        remoteSource.deleteInvoice(invoiceId)
        localSource.deleteByInvoiceId(invoiceId)
    }

    suspend fun cancelRemoteInvoice(invoiceName: String): SalesInvoiceDto? {
        val isPos = localSource.getInvoiceByName(invoiceName)?.invoice?.isPos == true
        if (isPos) {
            remoteSource.cancelPosInvoice(invoiceName)
        } else {
            remoteSource.cancelInvoice(invoiceName)
        }
        val remote = if (isPos) {
            remoteSource.fetchPosInvoice(invoiceName)
        } else {
            remoteSource.fetchInvoice(invoiceName)
        }
        if (remote != null) {
            updateLocalInvoiceFromRemote(invoiceName, remote)
        }
        return remote
    }

    override suspend fun syncPendingInvoices() {
        RepoTrace.breadcrumb("SalesInvoiceRepository", "syncPendingInvoices")
        val pending = getPendingSyncInvoices()
        pending.forEach { invoice ->
            try {
                when {
                    invoice.invoice.docstatus == 2 -> handleCancelledInvoice(invoice)
                    invoice.invoice.invoiceName?.startsWith("LOCAL-", ignoreCase = true) == true -> handleLocalInvoice(invoice)
                    else -> handleRemoteInvoice(invoice)
                }
            } catch (e: Exception) {
                RepoTrace.capture("SalesInvoiceRepository", "syncPendingInvoices", e)
                markAsFailed(invoice.invoice.invoiceName ?: "")
            }
        }
    }

    private suspend fun handleRemoteInvoice(invoice: SalesInvoiceWithItemsAndPayments) {
        val localName = invoice.invoice.invoiceName ?: return
        if (invoice.invoice.isPos) {
            remoteSource.submitPosInvoice(localName)
        } else {
            submitSalesInvoice(localName)
        }
        val remote = if (invoice.invoice.isPos) {
            remoteSource.fetchPosInvoice(localName)
        } else {
            remoteSource.fetchInvoice(localName)
        } ?: return
        updateLocalInvoiceFromRemote(localName, remote)
    }

    private suspend fun handleLocalInvoice(invoice: SalesInvoiceWithItemsAndPayments) {
        val localName = invoice.invoice.invoiceName ?: return
        val dto = ensureDraftDocStatus(enrichPaymentsWithAccount(invoice.toDto()))
        val created = if (isPosInvoice(dto)) {
            remoteSource.createPosInvoice(dto)
        } else {
            remoteSource.createInvoice(dto)
        }
        val createdName = created.name ?: return
        updateLocalInvoiceFromRemote(localName, created)
        if (isPosInvoice(dto)) {
            remoteSource.submitPosInvoice(createdName)
        } else {
            submitSalesInvoice(createdName)
        }
        val remote = if (isPosInvoice(dto)) {
            remoteSource.fetchPosInvoice(createdName)
        } else {
            remoteSource.fetchInvoice(createdName)
        } ?: return
        updateLocalInvoiceFromRemote(createdName, remote)
    }

    private suspend fun handleCancelledInvoice(invoice: SalesInvoiceWithItemsAndPayments) {
        val localName = invoice.invoice.invoiceName ?: return
        if (localName.startsWith("LOCAL-", ignoreCase = true)) {
            localSource.deleteByInvoiceId(localName)
            return
        }
        if (invoice.invoice.isPos) {
            remoteSource.cancelPosInvoice(localName)
        } else {
            remoteSource.cancelInvoice(localName)
        }
        val remote = if (invoice.invoice.isPos) {
            remoteSource.fetchPosInvoice(localName)
        } else {
            remoteSource.fetchInvoice(localName)
        }
        if (remote != null) {
            updateLocalInvoiceFromRemote(localName, remote)
        } else {
            localSource.deleteByInvoiceId(localName)
        }
    }

    private suspend fun submitSalesInvoice(name: String) {
        remoteSource.submitInvoice(name)
    }

    private fun isPosInvoice(invoice: SalesInvoiceDto): Boolean {
        return invoice.doctype.equals("POS Invoice", ignoreCase = true) || invoice.isPos
    }

    private fun resolveDocStatus(status: String?, docStatus: Int?): Int {
        val normalized = status?.lowercase()?.trim()
        return docStatus?.coerceIn(0, 2) ?: when (normalized) {
            "paid", "submitted" -> 1
            "cancelled", "return" -> 2
            else -> 0
        }
    }

    private suspend fun refreshCustomerSummaryWithRates(customerId: String) {
        val invoices = localSource.getOutstandingInvoicesForCustomer(customerId)
        if (invoices.isEmpty()) {
            customerDao.updateSummary(
                customerId = customerId,
                totalPendingAmount = 0.0,
                pendingInvoicesCount = 0,
                currentBalance = 0.0,
                availableCredit = null,
                state = "Sin Pendientes"
            )
            return
        }
        val ctx = context.getContext()
        val baseCurrency = ctx?.partyAccountCurrency ?: ctx?.currency ?: "NIO"
        var totalPending = 0.0
        invoices.forEach { wrapper ->
            val invoice = wrapper.invoice
            val currency = invoice.partyAccountCurrency ?: invoice.currency
            val outstanding = invoice.outstandingAmount.coerceAtLeast(0.0)
            val rate = when {
                currency.equals(baseCurrency, ignoreCase = true) -> 1.0
                invoice.conversionRate != null && invoice.conversionRate!! > 0.0 ->
                    invoice.conversionRate!!
                invoice.customExchangeRate != null && invoice.customExchangeRate!! > 0.0 ->
                    invoice.customExchangeRate!!
                else -> context.resolveExchangeRateBetween(currency, baseCurrency) ?: 1.0
            }
            totalPending += (outstanding * rate)
        }
        val pendingCount = invoices.count { it.invoice.outstandingAmount > 0.0 }
        val customer = customerDao.getByName(customerId)
        val creditLimit = customer?.creditLimit
        val availableCredit = creditLimit?.let { it - totalPending }
        val state = if (totalPending > 0.0) "Pendientes" else "Sin Pendientes"
        customerDao.updateSummary(
            customerId = customerId,
            totalPendingAmount = roundToCurrency(totalPending),
            pendingInvoicesCount = pendingCount,
            currentBalance = roundToCurrency(totalPending),
            availableCredit = availableCredit?.let { roundToCurrency(it) },
            state = state
        )
    }

    private suspend fun enrichPaymentsWithAccount(dto: SalesInvoiceDto): SalesInvoiceDto {
        if (dto.payments.isEmpty()) return dto
        val ctx = context.requireContext()
        val definitions = runCatching { modeOfPaymentDao.getAllModes(ctx.company) }
            .getOrElse { emptyList() }
        if (definitions.isEmpty()) return dto
        val details = buildPaymentModeDetailMap(definitions)
        val updated = dto.payments.map { payment ->
            val account = details[payment.modeOfPayment]?.account
            if (payment.account.isNullOrBlank() && !account.isNullOrBlank()) {
                payment.copy(account = account)
            } else {
                payment
            }
        }
        return dto.copy(payments = updated)
    }

    private fun ensureDraftDocStatus(dto: SalesInvoiceDto): SalesInvoiceDto {
        return if (dto.docStatus == null || dto.docStatus == 0) dto else dto.copy(docStatus = 0)
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
                it.toEntities().forEach { payload ->
                    localSource.saveInvoiceLocally(payload.invoice, payload.items, payload.payments)
                }
                reconcileOutstandingWithRemote(it)
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

    suspend fun getOutstandingInvoicesForCustomerLocal(customerName: String): List<SalesInvoiceBO> {
        return localSource.getOutstandingInvoicesForCustomer(customerName).toBO()
    }

    private suspend fun reconcileOutstandingWithRemote(remoteInvoices: List<SalesInvoiceDto>) {
        val ctx = context.getContext() ?: return
        val profileId = ctx.profileName
        val localOutstandingNames = localSource.getOutstandingInvoiceNamesForProfile(profileId)
        if (localOutstandingNames.isEmpty()) return

        val remoteNames = remoteInvoices.mapNotNull { it.name }.toSet()
        val missing = localOutstandingNames.filterNot { remoteNames.contains(it) }
        if (missing.isEmpty()) return

        missing.forEach { invoiceName ->
            val remote = runCatching { fetchRemoteInvoice(invoiceName) }.getOrNull()
            if (remote != null) {
                updateLocalInvoiceFromRemote(invoiceName, remote)
            } else {
                val customerId = localSource.getInvoiceByName(invoiceName)?.invoice?.customer
                localSource.deleteByInvoiceId(invoiceName)
                customerId?.let { refreshCustomerSummaryWithRates(it) }
                AppLogger.warn("reconcileOutstandingWithRemote: invoice not found $invoiceName")
            }
        }
    }
}
