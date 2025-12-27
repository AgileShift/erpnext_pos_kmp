package com.erpnext.pos.domain.usecases

import com.erpnext.pos.domain.repositories.ISaleInvoiceRepository
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class RegisterInvoicePaymentInput(
    val invoiceId: String,
    val modeOfPayment: String,
    val amount: Double
)

@OptIn(ExperimentalTime::class)
class RegisterInvoicePaymentUseCase(
    private val repository: ISaleInvoiceRepository
) : UseCase<RegisterInvoicePaymentInput, Unit>() {

    override suspend fun useCaseFunction(input: RegisterInvoicePaymentInput) {
        require(input.amount > 0.0) { "Payment amount must be greater than zero" }

        val invoiceSnapshot = requireNotNull(
            repository.getInvoiceByName(input.invoiceId)
        ) { "Invoice not found: ${input.invoiceId}" }

        val invoice = invoiceSnapshot.invoice
        require(invoice.outstandingAmount >= input.amount) {
            "Payment exceeds outstanding amount"
        }

        val updatedOutstanding = invoice.outstandingAmount - input.amount
        val updatedPaid = invoice.paidAmount + input.amount

        invoice.outstandingAmount = updatedOutstanding
        invoice.paidAmount = updatedPaid
        invoice.status = if (updatedOutstanding <= 0.0001) "Paid" else "Unpaid"
        invoice.syncStatus = "Pending"
        invoice.modifiedAt = Clock.System.now().toEpochMilliseconds()

        val payment = POSInvoicePaymentEntity(
            parentInvoice = invoice.invoiceName ?: input.invoiceId,
            modeOfPayment = input.modeOfPayment,
            amount = input.amount
        )

        repository.applyLocalPayment(invoice, listOf(payment))
    }
}
