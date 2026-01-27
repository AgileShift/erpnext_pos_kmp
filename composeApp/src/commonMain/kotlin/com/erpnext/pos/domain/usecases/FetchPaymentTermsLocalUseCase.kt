package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.PaymentTermsRepository
import com.erpnext.pos.domain.models.PaymentTermBO

class FetchPaymentTermsLocalUseCase(
    private val repository: PaymentTermsRepository
) : UseCase<Unit?, List<PaymentTermBO>>() {
    override suspend fun useCaseFunction(input: Unit?): List<PaymentTermBO> {
        return repository.getLocalPaymentTerms()
    }
}
