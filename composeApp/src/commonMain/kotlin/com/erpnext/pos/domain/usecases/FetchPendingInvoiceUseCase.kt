package com.erpnext.pos.domain.usecases

import androidx.paging.PagingData
import com.erpnext.pos.data.repositories.SalesInvoiceRepository
import com.erpnext.pos.domain.models.SalesInvoiceBO
import kotlinx.coroutines.flow.Flow

data class PendingInvoiceInput(
    val query: String? = null,
    val date: String? = null,
)

class FetchPendingInvoiceUseCase(
    private val repo: SalesInvoiceRepository
) : UseCase<PendingInvoiceInput, Flow<PagingData<SalesInvoiceBO>>>() {
    override suspend fun useCaseFunction(input: PendingInvoiceInput): Flow<PagingData<SalesInvoiceBO>> {
        return repo.getPendingInvoices(input)
    }
}