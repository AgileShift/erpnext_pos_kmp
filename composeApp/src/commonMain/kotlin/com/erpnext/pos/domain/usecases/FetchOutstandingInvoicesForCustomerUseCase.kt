package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.domain.models.SalesInvoiceBO

class FetchOutstandingInvoicesForCustomerUseCase(
    private val salesInvoiceRepository: SalesInvoiceRepository
) : UseCase<String, List<SalesInvoiceBO>>() {
    override suspend fun useCaseFunction(input: String): List<SalesInvoiceBO> {
        return salesInvoiceRepository.getOutstandingInvoicesForCustomer(input)
    }
}
