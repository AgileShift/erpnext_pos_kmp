package com.erpnext.pos.domain.usecases.v2

import com.erpnext.pos.domain.utils.UUIDGenerator
import com.erpnext.pos.localSource.dao.SyncStatus
import com.erpnext.pos.localSource.dao.v2.SalesInvoiceDao
import com.erpnext.pos.localSource.entities.v2.SalesInvoicePaymentEntity
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class RegisterInvoicePaymentOfflineInput(
    val instanceId: String,
    val companyId: String,
    val invoiceId: String,
    val payments: List<InvoicePaymentInput>
)

data class InvoicePaymentInput(
    val modeOfPayment: String,
    val amount: Double
)

@OptIn(ExperimentalTime::class)
class RegisterInvoicePaymentOfflineUseCase(
    private val salesInvoiceDao: SalesInvoiceDao,
    private val idGenerator: UUIDGenerator
) {

    suspend operator fun invoke(input: RegisterInvoicePaymentOfflineInput) {

        val invoice = requireNotNull(
            salesInvoiceDao.getInvoice(
                input.instanceId,
                input.companyId,
                input.invoiceId
            )
        ) { "Invoice not found: ${input.invoiceId}" }

        require(invoice.outstandingAmount > 0f) {
            "Invoice already fully paid"
        }

        val totalPayment = input.payments.sumOf { it.amount }

        require(totalPayment > 0f) {
            "Payment amount must be greater than zero"
        }

        require(totalPayment <= invoice.outstandingAmount + 0.0001f) {
            "Payment exceeds outstanding amount"
        }

        val paymentEntities = input.payments.map {
            SalesInvoicePaymentEntity(
                paymentId = "PAY-${idGenerator.newId()}",
                invoiceId = invoice.invoiceId,
                paymentMode = it.modeOfPayment,
                amount = it.amount
            ).apply {
                instanceId = input.instanceId
                companyId = input.companyId
            }
        }

        val newOutstanding = invoice.outstandingAmount - totalPayment

        invoice.outstandingAmount = newOutstanding
        invoice.status = if (newOutstanding <= 0.0001f) "Paid" else "Unpaid"
        invoice.syncStatus = SyncStatus.PENDING
        invoice.updatedAt = Clock.System.now().epochSeconds

        salesInvoiceDao.applyPayments(invoice, paymentEntities)
    }
}
