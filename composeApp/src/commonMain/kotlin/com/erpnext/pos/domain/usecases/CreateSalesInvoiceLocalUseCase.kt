package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto

data class CreateSalesInvoiceLocalInput(
    val localInvoiceName: String,
    val invoice: SalesInvoiceDto
)

class CreateSalesInvoiceLocalUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<CreateSalesInvoiceLocalInput, String>() {
    override suspend fun useCaseFunction(input: CreateSalesInvoiceLocalInput): String {
        repository.saveInvoiceLocallyPending(input.localInvoiceName, input.invoice)
        return input.localInvoiceName
    }
}
