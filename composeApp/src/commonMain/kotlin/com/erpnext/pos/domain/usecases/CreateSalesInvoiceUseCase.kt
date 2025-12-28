package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto
import com.erpnext.pos.remoteSource.mapper.toEntity

data class CreateSalesInvoiceInput(val invoice: SalesInvoiceDto)

class CreateSalesInvoiceUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<CreateSalesInvoiceInput, SalesInvoiceDto>() {
    override suspend fun useCaseFunction(input: CreateSalesInvoiceInput): SalesInvoiceDto {
        val created = repository.createRemoteInvoice(input.invoice)
        val entity = created.toEntity()
        repository.saveInvoiceLocally(
            invoice = entity.invoice,
            items = entity.items,
            payments = entity.payments
        )
        return created
    }
}
