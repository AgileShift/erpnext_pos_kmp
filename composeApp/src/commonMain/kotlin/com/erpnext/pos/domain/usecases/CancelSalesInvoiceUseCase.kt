package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.dao.PosProfilePaymentMethodDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.SalesInvoiceItemDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.PaymentEntryReferenceCreateDto
import com.erpnext.pos.remoteSource.mapper.toEntity
import com.erpnext.pos.remoteSource.mapper.toDto
import com.erpnext.pos.utils.NetworkMonitor
import com.erpnext.pos.utils.toErpDate
import com.erpnext.pos.utils.toErpDateTime
import kotlinx.coroutines.flow.firstOrNull
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class InvoiceCancellationAction {
    CANCEL,
    RETURN
}

data class CancelSalesInvoiceInput(
    val invoiceName: String,
    val action: InvoiceCancellationAction = InvoiceCancellationAction.CANCEL,
    val reason: String? = null,
    val refundModeOfPayment: String? = null,
    val refundReferenceNo: String? = null,
    val applyRefund: Boolean = true
)

data class CancelSalesInvoiceResult(
    val creditNoteName: String? = null,
    val cancelled: Boolean = false
)

@OptIn(ExperimentalTime::class)
class CancelSalesInvoiceUseCase(
    private val invoiceRepository: SalesInvoiceRepository,
    private val paymentEntryUseCase: CreatePaymentEntryUseCase,
    private val modeOfPaymentDao: ModeOfPaymentDao,
    private val posProfilePaymentMethodDao: PosProfilePaymentMethodDao,
    private val networkMonitor: NetworkMonitor
) : UseCase<CancelSalesInvoiceInput, CancelSalesInvoiceResult>() {

    override suspend fun useCaseFunction(input: CancelSalesInvoiceInput): CancelSalesInvoiceResult {
        val invoice = invoiceRepository.getInvoiceByName(input.invoiceName)
            ?: throw IllegalArgumentException("Invoice not found: ${input.invoiceName}")
        val isOnline = networkMonitor.isConnected.firstOrNull() == true
        return when (input.action) {
            InvoiceCancellationAction.CANCEL -> handleCancel(invoice, isOnline)
            InvoiceCancellationAction.RETURN -> handleReturn(
                invoice = invoice,
                reason = input.reason,
                isOnline = isOnline,
                refundModeOfPayment = input.refundModeOfPayment,
                refundReferenceNo = input.refundReferenceNo,
                applyRefund = input.applyRefund
            )
        }
    }

    private suspend fun handleCancel(
        invoice: SalesInvoiceWithItemsAndPayments,
        isOnline: Boolean
    ): CancelSalesInvoiceResult {
        val normalizedStatus = invoice.invoice.status.trim().lowercase()
        val hasPayments = invoice.invoice.paidAmount > 0.0 ||
                invoice.payments.any { it.amount > 0.0 }
        if (normalizedStatus == "paid" || normalizedStatus == "partly paid" || hasPayments) {
            throw IllegalStateException("La factura tiene pagos; usa retorno o reembolso.")
        }
        val name = invoice.invoice.invoiceName ?: return CancelSalesInvoiceResult()
        invoiceRepository.cancelInvoice(name, isReturn = false)
        if (isOnline) {
            runCatching { invoiceRepository.cancelRemoteInvoice(name) }
                .onSuccess { invoiceRepository.markAsSynced(name) }
        }
        return CancelSalesInvoiceResult(cancelled = true)
    }

    private suspend fun handleReturn(
        invoice: SalesInvoiceWithItemsAndPayments,
        reason: String?,
        isOnline: Boolean,
        refundModeOfPayment: String?,
        refundReferenceNo: String?,
        applyRefund: Boolean
    ): CancelSalesInvoiceResult {
        val originalName = invoice.invoice.invoiceName ?: return CancelSalesInvoiceResult()
        if (!isOnline) {
            invoiceRepository.cancelInvoice(originalName, isReturn = true)
            return CancelSalesInvoiceResult(creditNoteName = null, cancelled = true)
        }

        val returnItems = buildRemainingReturnItems(invoice)
        if (returnItems.isEmpty()) {
            throw IllegalStateException("La factura ya fue retornada por completo.")
        }
        val creditNoteDto = buildCreditNoteDto(invoice, returnItems, reason)
        val created = invoiceRepository.createRemoteInvoice(creditNoteDto)
        val entity = created.toEntity()
        invoiceRepository.saveInvoiceLocally(
            invoice = entity.invoice,
            items = entity.items,
            payments = entity.payments
        )
        if (applyRefund) {
            createReturnPaymentEntries(
                invoice = invoice,
                creditNote = created,
                refundModeOfPayment = refundModeOfPayment,
                refundReferenceNo = refundReferenceNo
            )
        }
        invoiceRepository.refreshInvoiceFromRemote(created.name ?: originalName)
        return CancelSalesInvoiceResult(creditNoteName = created.name, cancelled = true)
    }

    private suspend fun buildRemainingReturnItems(
        invoice: SalesInvoiceWithItemsAndPayments
    ): List<SalesInvoiceItemDto> {
        val parent = invoice.invoice
        val originalName = parent.invoiceName ?: return emptyList()
        val remoteParent = invoiceRepository.fetchRemoteInvoice(originalName)
        val baseItems = remoteParent?.items?.takeIf { it.isNotEmpty() }
            ?: invoice.items.map { it.toDto(parent) }
        val returnInvoices = invoiceRepository.fetchRemoteReturnInvoices(
            returnAgainst = originalName
        )
        val returnedByItem = mutableMapOf<String, Double>()
        returnInvoices.forEach { credit ->
            credit.items.forEach { item ->
                val qty = abs(item.qty)
                if (qty > 0.0) {
                    returnedByItem[item.itemCode] =
                        (returnedByItem[item.itemCode] ?: 0.0) + qty
                }
            }
        }
        return baseItems.mapNotNull { item ->
            val originalQty = abs(item.qty)
            val returnedQty = returnedByItem[item.itemCode] ?: 0.0
            val remaining = (originalQty - returnedQty).coerceAtLeast(0.0)
            if (remaining <= 0.0) null else createReturnItemFromDto(item, remaining)
        }
    }

    private fun createReturnItemFromDto(
        item: SalesInvoiceItemDto,
        qtyToReturn: Double
    ): SalesInvoiceItemDto {
        val perUnit =
            if (item.qty != 0.0) item.amount / item.qty else item.rate.takeIf { it != 0.0 }
                ?: 0.0
        val absAmount = abs(perUnit) * qtyToReturn
        return item.copy(
            qty = -qtyToReturn,
            amount = -absAmount
        )
    }

    private fun buildCreditNoteDto(
        invoice: SalesInvoiceWithItemsAndPayments,
        items: List<SalesInvoiceItemDto>,
        reason: String?
    ): SalesInvoiceDto {
        val parent = invoice.invoice
        val now = Clock.System.now().toEpochMilliseconds()
        val isPos = parent.isPos
        val totalAmount = items.sumOf { -it.amount }

        return SalesInvoiceDto(
            customer = parent.customer,
            customerName = parent.customerName ?: "",
            customerPhone = parent.customerPhone,
            company = parent.company,
            postingDate = now.toErpDateTime(),
            dueDate = now.toErpDate(),
            status = "Draft",
            grandTotal = totalAmount,
            outstandingAmount = totalAmount,
            totalTaxesAndCharges = 0.0,
            netTotal = totalAmount,
            paidAmount = 0.0,
            items = items,
            payments = emptyList(),
            remarks = reason ?: parent.remarks,
            updateStock = true,
            posProfile = parent.profileId,
            currency = parent.currency,
            conversionRate = parent.conversionRate,
            partyAccountCurrency = parent.partyAccountCurrency,
            returnAgainst = parent.invoiceName,
            posOpeningEntry = parent.posOpeningEntry,
            debitTo = parent.debitTo,
            docStatus = 0,
            isReturn = 1,
            isPos = isPos,
            doctype = "Sales Invoice"
        )
    }

    private suspend fun createReturnPaymentEntries(
        invoice: SalesInvoiceWithItemsAndPayments,
        creditNote: SalesInvoiceDto,
        refundModeOfPayment: String?,
        refundReferenceNo: String?
    ) {
        val company = invoice.invoice.company
        val profileId = invoice.invoice.profileId
        val creditName = creditNote.name ?: return
        val postingDate = Clock.System.now().toEpochMilliseconds().toErpDateTime()
        val modes = modeOfPaymentDao.getAllModes(company)
        val receivableAccount = invoice.invoice.debitTo
        val totalReturn = creditNote.grandTotal
        if (totalReturn <= 0.0) return
        val resolvedMode = refundModeOfPayment?.takeIf { it.isNotBlank() }
            ?: invoice.payments.firstOrNull()?.modeOfPayment
            ?: return
        if (!isRefundModeAllowed(profileId, resolvedMode)) {
            throw IllegalStateException("El modo de reembolso no está habilitado para retornos.")
        }
        val paidToCurrency = creditNote.partyAccountCurrency
            ?: creditNote.currency
            ?: invoice.invoice.currency

        val entry = PaymentEntryCreateDto(
            company = company,
            postingDate = postingDate,
            paymentType = "Pay",
            partyType = "Customer",
            modeOfPayment = resolvedMode,
            paidAmount = totalReturn,
            receivedAmount = totalReturn,
            paidFrom = resolveAccount(resolvedMode, modes, receivableAccount),
            paidTo = resolveAccount(resolvedMode, modes, receivableAccount),
            paidToAccountCurrency = paidToCurrency,
            referenceNo = refundReferenceNo?.takeIf { it.isNotBlank() },
            referenceDate = postingDate,
            references = listOf(
                PaymentEntryReferenceCreateDto(
                    referenceDoctype = "Sales Invoice",
                    referenceName = creditName,
                    totalAmount = totalReturn,
                    outstandingAmount = creditNote.outstandingAmount ?: totalReturn,
                    allocatedAmount = totalReturn
                )
            ),
            partyId = invoice.invoice.customer,
            sourceExchangeRate = null,
            targetExchangeRate = null,
            docStatus = 0
        )
        paymentEntryUseCase(CreatePaymentEntryInput(entry))
    }

    private fun resolveAccount(
        mode: String,
        cachedModes: List<ModeOfPaymentEntity>,
        fallback: String?
    ): String? {
        return cachedModes.firstOrNull {
            it.modeOfPayment.equals(mode, ignoreCase = true) || it.name.equals(mode, ignoreCase = true)
        }?.account ?: fallback
    }

    private suspend fun isRefundModeAllowed(
        profileId: String?,
        mode: String
    ): Boolean {
        if (profileId.isNullOrBlank()) return false
        val resolved = posProfilePaymentMethodDao.getResolvedMethodsForProfile(profileId)
        val matched = resolved.firstOrNull {
            it.mopName.equals(mode, ignoreCase = true)
        }
        return matched?.allowInReturns == true && matched.enabledInProfile
    }
}
