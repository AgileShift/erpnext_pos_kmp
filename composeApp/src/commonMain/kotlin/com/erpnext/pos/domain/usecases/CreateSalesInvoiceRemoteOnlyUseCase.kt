package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto

data class CreateSalesInvoiceRemoteOnlyInput(val invoice: SalesInvoiceDto)

class CreateSalesInvoiceRemoteOnlyUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<CreateSalesInvoiceRemoteOnlyInput, SalesInvoiceDto>() {
    override suspend fun useCaseFunction(input: CreateSalesInvoiceRemoteOnlyInput): SalesInvoiceDto {
        return repository.createRemoteInvoice(input.invoice)
    }
}
