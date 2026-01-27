package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.CustomerRepository
import com.erpnext.pos.domain.models.SalesInvoiceBO

class FetchCustomerInvoicesLocalForPeriodUseCase(
    private val repository: CustomerRepository
) : UseCase<CustomerInvoiceHistoryInput, List<SalesInvoiceBO>>() {
    override suspend fun useCaseFunction(input: CustomerInvoiceHistoryInput): List<SalesInvoiceBO> {
        return repository.fetchLocalInvoicesForCustomerPeriod(
            customerId = input.customerId,
            startDate = input.startDate,
            endDate = input.endDate
        )
    }
}
