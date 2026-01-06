package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.localSource.entities.SalesInvoiceEntity

class FetchSalesInvoiceLocalUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<String, SalesInvoiceEntity?>() {
    override suspend fun useCaseFunction(input: String): SalesInvoiceEntity? {
        return repository.getInvoiceByName(input)?.invoice
    }
}
