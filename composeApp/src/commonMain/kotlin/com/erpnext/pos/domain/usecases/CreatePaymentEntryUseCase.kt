package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.PaymentEntryRepository
import com.erpnext.pos.remoteSource.dto.PaymentEntryCreateDto

data class CreatePaymentEntryInput(val entry: PaymentEntryCreateDto)

class CreatePaymentEntryUseCase(
    private val repository: PaymentEntryRepository
) : UseCase<CreatePaymentEntryInput, String>() {
    override suspend fun useCaseFunction(input: CreatePaymentEntryInput): String {
        return repository.createPaymentEntry(input.entry)
    }
}
