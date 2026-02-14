package com.erpnext.pos.domain.usecases

import androidx.paging.PagingData
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.domain.models.SalesInvoiceBO
import kotlinx.coroutines.flow.Flow

class FetchCustomerInvoicesLocalForPeriodUseCase(
    private val repository: SalesInvoiceRepository
) : UseCase<CustomerInvoiceHistoryInput, Flow<PagingData<SalesInvoiceBO>>>() {
    override suspend fun useCaseFunction(input: CustomerInvoiceHistoryInput): Flow<PagingData<SalesInvoiceBO>> {
        return repository.getInvoicesForCustomerInRangeLocalPaged(
            customerName = input.customerId,
            startDate = input.startDate,
            endDate = input.endDate
        )
    }
}
