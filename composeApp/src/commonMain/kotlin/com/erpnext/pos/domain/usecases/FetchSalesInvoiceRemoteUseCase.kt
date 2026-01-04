package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.remoteSource.dto.SalesInvoiceDto

class FetchSalesInvoiceRemoteUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<String, SalesInvoiceDto?>() {
    override suspend fun useCaseFunction(input: String): SalesInvoiceDto? {
        return repository.fetchRemoteInvoice(input)
    }
}
