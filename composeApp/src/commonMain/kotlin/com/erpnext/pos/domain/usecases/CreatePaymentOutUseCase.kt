package com.erpnext.pos.domain.usecases

import com.erpnext.pos.data.repositories.PaymentOutRepository
import com.erpnext.pos.remoteSource.dto.PaymentOutCreateDto
import com.erpnext.pos.remoteSource.dto.PaymentOutSubmitDto

data class CreatePaymentOutInput(
    val clientRequestId: String,
    val payload: PaymentOutCreateDto
)

class CreatePaymentOutUseCase(
    private val repository: PaymentOutRepository
) : UseCase<CreatePaymentOutInput, PaymentOutSubmitDto>() {
    override suspend fun useCaseFunction(input: CreatePaymentOutInput): PaymentOutSubmitDto {
        return repository.createPaymentOut(input.clientRequestId, input.payload)
    }
}
