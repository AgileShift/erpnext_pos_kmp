package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.localSource.entities.SalesInvoiceWithItemsAndPayments

class SyncSalesInvoiceFromRemoteUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<String, SalesInvoiceWithItemsAndPayments?>() {
    override suspend fun useCaseFunction(input: String): SalesInvoiceWithItemsAndPayments? {
        return repository.refreshInvoiceFromRemote(input)
    }
}
