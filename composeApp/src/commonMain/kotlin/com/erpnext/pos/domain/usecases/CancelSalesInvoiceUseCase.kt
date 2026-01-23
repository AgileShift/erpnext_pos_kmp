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
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

enum class InvoiceCancellationAction {
    CANCEL,
    RETURN
}

data class CancelSalesInvoiceInput(
    val invoiceName: String,
    val action: InvoiceCancellationAction = InvoiceCancellationAction.CANCEL,
    val reason: String? = null
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
            InvoiceCancellationAction.RETURN -> handleReturn(invoice, input.reason, isOnline)
        }
    }

    private suspend fun handleCancel(
        invoice: SalesInvoiceWithItemsAndPayments,
        isOnline: Boolean
    ): CancelSalesInvoiceResult {
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
        isOnline: Boolean
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
        createReturnPaymentEntries(invoice, created)
        invoiceRepository.refreshInvoiceFromRemote(created.name ?: originalName)
        return CancelSalesInvoiceResult(creditNoteName = created.name, cancelled = true)
    }

    private fun buildCreditNoteDto(
        invoice: SalesInvoiceWithItemsAndPayments,
        reason: String?
    ): SalesInvoiceDto {
        val parent = invoice.invoice
        val now = Clock.System.now().toEpochMilliseconds()
        return SalesInvoiceDto(
            customer = parent.customer,
            customerName = parent.customerName ?: "",
            customerPhone = parent.customerPhone,
            company = parent.company,
            postingDate = now.toErpDateTime(),
            dueDate = now.toErpDate(),
            status = "Draft",
            grandTotal = parent.grandTotal,
            outstandingAmount = parent.outstandingAmount,
            totalTaxesAndCharges = parent.taxTotal,
            netTotal = parent.netTotal,
            paidAmount = parent.paidAmount,
            items = invoice.items.map { it.toDto(parent) },
            payments = emptyList(),
            remarks = reason ?: parent.remarks,
            isPos = true,
            updateStock = true,
            posProfile = parent.profileId,
            currency = parent.currency,
            conversionRate = parent.conversionRate,
            partyAccountCurrency = parent.partyAccountCurrency,
            returnAgainst = parent.invoiceName,
            posOpeningEntry = parent.posOpeningEntry,
            debitTo = parent.debitTo,
            docStatus = 0,
            isReturn = 1
        )
    }

    private suspend fun createReturnPaymentEntries(
        invoice: SalesInvoiceWithItemsAndPayments,
        creditNote: SalesInvoiceDto
    ) {
        val company = invoice.invoice.company
        val creditName = creditNote.name ?: return
        val postingDate = Clock.System.now().toEpochMilliseconds().toErpDateTime()
        val modes = modeOfPaymentDao.getAllModes(company)
        val receivableAccount = invoice.invoice.debitTo
        val currency = invoice.invoice.currency

        invoice.payments.filter { it.amount > 0.0 }.forEach { payment ->
            val entry = PaymentEntryCreateDto(
                company = company,
                postingDate = postingDate,
                paymentType = "Pay",
                partyType = "Customer",
                modeOfPayment = payment.modeOfPayment,
                paidAmount = payment.amount,
                receivedAmount = payment.amount,
                paidFrom = resolveAccount(payment.modeOfPayment, modes, receivableAccount),
                paidTo = resolveAccount(payment.modeOfPayment, modes, receivableAccount),
                paidToAccountCurrency = payment.paymentCurrency ?: currency,
                referenceNo = payment.paymentReference,
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
                partyId =  invoice.invoice.customer,
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
