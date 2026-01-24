package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.localSource.dao.ModeOfPaymentDao
import com.erpnext.pos.localSource.entities.ModeOfPaymentEntity
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryReferenceCreateDto
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
            invoiceRepository.cancelRemoteInvoice(name)
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
        if (!isOnline) {
            throw IllegalStateException("Se requiere conexión a internet para registrar un retorno.")
        }
        val originalName = invoice.invoice.invoiceName ?: return CancelSalesInvoiceResult()
        val creditNoteDto = buildCreditNoteDto(invoice, reason)
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

    private fun buildCreditNoteDto(
        invoice: SalesInvoiceWithItemsAndPayments,
        reason: String?
    ): SalesInvoiceDto {
        val parent = invoice.invoice
        val now = Clock.System.now().toEpochMilliseconds()
        val isPos = parent.isPos

        val returnItems = invoice.items.map { item ->
            val dto = item.toDto(parent)
            dto.copy(
                qty = -abs(dto.qty),
                amount = -abs(dto.amount)
            )
        }

        return SalesInvoiceDto(
            customer = parent.customer,
            customerName = parent.customerName ?: "",
            customerPhone = parent.customerPhone,
            company = parent.company,
            postingDate = now.toErpDateTime(),
            dueDate = now.toErpDate(),
            status = "Draft",
            grandTotal = parent.grandTotal,
            outstandingAmount = 0.0,
            totalTaxesAndCharges = parent.taxTotal,
            netTotal = parent.netTotal,
            paidAmount = 0.0,
            items = returnItems,
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
            doctype = if (isPos) "POS Invoice" else "Sales Invoice"
        )
    }

    private suspend fun createReturnPaymentEntries(
        invoice: SalesInvoiceWithItemsAndPayments,
        creditNote: SalesInvoiceDto,
        refundModeOfPayment: String?,
        refundReferenceNo: String?
    ) {
        val company = invoice.invoice.company
        val creditName = creditNote.name ?: return
        val postingDate = Clock.System.now().toEpochMilliseconds().toErpDateTime()
        val modes = modeOfPaymentDao.getAllModes(company)
        val receivableAccount = invoice.invoice.debitTo
        val currency = invoice.invoice.currency

        invoice.payments
            .filter { it.amount > 0.0 }
            .forEach { payment ->
                val resolvedMode = refundModeOfPayment?.takeIf { it.isNotBlank() }
                    ?: payment.modeOfPayment

                val entry = PaymentEntryCreateDto(
                    company = company,
                    postingDate = postingDate,
                    paymentType = "Pay",
                    partyType = "Customer",
                    modeOfPayment = resolvedMode,
                    paidAmount = payment.amount,
                    receivedAmount = payment.amount,
                    paidFrom = resolveAccount(resolvedMode, modes, receivableAccount),
                    paidTo = resolveAccount(resolvedMode, modes, receivableAccount),
                    paidToAccountCurrency = payment.paymentCurrency ?: currency,
                    referenceNo = refundReferenceNo?.takeIf { it.isNotBlank() } ?: payment.paymentReference,
                    referenceDate = payment.paymentDate,
                    references = listOf(
                        PaymentEntryReferenceCreateDto(
                            referenceDoctype = "Sales Invoice",
                            referenceName = creditName,
                            totalAmount = creditNote.grandTotal,
                            outstandingAmount = creditNote.outstandingAmount ?: 0.0,
                            allocatedAmount = payment.amount
                        )
                    ),
                    partyId = invoice.invoice.customer,
                    sourceExchangeRate = null,
                    targetExchangeRate = null,
                    docStatus = 0
                )
                paymentEntryUseCase(CreatePaymentEntryInput(entry))
            }
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
}
