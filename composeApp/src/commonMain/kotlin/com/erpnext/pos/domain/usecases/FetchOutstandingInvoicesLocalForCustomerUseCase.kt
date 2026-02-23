package com.erpnext.pos.domain.usecases

import androidx.paging.PagingData
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.domain.models.SalesInvoiceBO
import kotlinx.coroutines.flow.Flow

/**
 * Local-only outstanding invoices used by the Customer view.
 * Data sources:
 * - SalesInvoiceRepository (local invoices persisted in SalesInvoiceDao)
 */
class FetchOutstandingInvoicesLocalForCustomerUseCase(
    private val salesInvoiceRepository: SalesInvoiceRepository
) : UseCase<String, Flow<PagingData<SalesInvoiceBO>>>() {
    override suspend fun useCaseFunction(input: String): Flow<PagingData<SalesInvoiceBO>> {
        return salesInvoiceRepository.getOutstandingInvoicesForCustomerLocalPaged(input)
    }
}
