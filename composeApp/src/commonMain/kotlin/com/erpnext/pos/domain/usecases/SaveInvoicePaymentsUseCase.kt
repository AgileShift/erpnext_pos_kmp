package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.localSource.entities.POSInvoicePaymentEntity

data class SaveInvoicePaymentsInput(
    val invoiceName: String,
    val payments: List<POSInvoicePaymentEntity>
)

class SaveInvoicePaymentsUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<SaveInvoicePaymentsInput, Unit>() {
    override suspend fun useCaseFunction(input: SaveInvoicePaymentsInput) {
        val existing = repository.getInvoiceByName(input.invoiceName) ?: return
        repository.applyLocalPayment(existing.invoice, input.payments)
    }
}
