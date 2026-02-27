package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.PaymentOutRepository
import com.erpnext.pos.remoteSource.dto.PurchaseInvoiceOutstandingDto

class FetchSupplierOutstandingPurchaseInvoicesUseCase(
    private val repository: PaymentOutRepository
) : UseCase<String, List<PurchaseInvoiceOutstandingDto>>() {
    override suspend fun useCaseFunction(input: String): List<PurchaseInvoiceOutstandingDto> {
        return repository.fetchOutstandingPurchaseInvoicesForSupplier(input)
    }
}
