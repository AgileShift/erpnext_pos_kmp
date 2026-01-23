package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.CustomerRepository
import com.erpnext.pos.domain.models.SalesInvoiceBO

data class CustomerInvoiceHistoryInput(
    val customerId: String,
    val startDate: String,
    val endDate: String
)

class FetchCustomerInvoicesForPeriodUseCase(
    private val repository: CustomerRepository
) : UseCase<CustomerInvoiceHistoryInput, List<SalesInvoiceBO>>() {
    override suspend fun useCaseFunction(input: CustomerInvoiceHistoryInput): List<SalesInvoiceBO> {
        return repository.fetchInvoicesForCustomerPeriod(
            customerId = input.customerId,
            startDate = input.startDate,
            endDate = input.endDate
        )
    }
}
