package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.domain.repositories.ISaleInvoiceRepository
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity

data class RegisterInvoicePaymentInput(
    val invoiceId: String,
    val modeOfPayment: String,
    val amount: Double
)

class RegisterInvoicePaymentUseCase(
    private val repository: SalesInvoiceRepository
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

        val payment = POSInvoicePaymentEntity(
            parentInvoice = invoice.invoiceName ?: input.invoiceId,
            modeOfPayment = input.modeOfPayment,
            amount = input.amount
        )

        repository.applyLocalPayment(invoice, listOf(payment))
    }
}
