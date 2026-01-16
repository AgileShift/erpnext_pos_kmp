package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.domain.models.SalesInvoiceBO

/**
 * Local-only outstanding invoices used by the Customer view.
 * Data sources:
 * - SalesInvoiceRepository (local invoices persisted in SalesInvoiceDao)
 */
class FetchOutstandingInvoicesLocalForCustomerUseCase(
    private val salesInvoiceRepository: SalesInvoiceRepository
) : UseCase<String, List<SalesInvoiceBO>>() {
    override suspend fun useCaseFunction(input: String): List<SalesInvoiceBO> {
        return salesInvoiceRepository.getOutstandingInvoicesForCustomerLocal(input)
    }
}
