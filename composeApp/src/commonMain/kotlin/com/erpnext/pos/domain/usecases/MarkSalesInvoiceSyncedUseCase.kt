package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository

class MarkSalesInvoiceSyncedUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<String, Unit>() {
    override suspend fun useCaseFunction(input: String) {
        repository.markAsSynced(input)
    }
}
