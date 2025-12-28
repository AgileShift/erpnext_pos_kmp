package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.PaymentEntryRepository
import com.erpnext.pos.remoteSource.dto.v2.PaymentEntryCreateDto

data class CreatePaymentEntryInput(val entry: PaymentEntryCreateDto)

class CreatePaymentEntryUseCase(
    private val repository: PaymentEntryRepository
) : UseCase<CreatePaymentEntryInput, Unit>() {
    override suspend fun useCaseFunction(input: CreatePaymentEntryInput) {
        repository.createPaymentEntry(input.entry)
    }
}
